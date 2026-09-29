package com.jarvis.voice.pipeline

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * MANDATORY MICROPHONE OWNERSHIP CONTRACT:
 * Exactly ONE component owns the microphone: AudioPipeline.
 * No other component may instantiate or start an AudioRecord.
 * All Porcupine, Gemini Live, UI, and test listeners MUST subscribe via audioFlow.
 */
class AudioPipeline private constructor() {

    companion object {
        private const val TAG = "AudioPipeline"
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val FRAME_SIZE_SAMPLES = 512 // 32ms per frame @ 16kHz

        @Volatile
        private var instance: AudioPipeline? = null

        fun getInstance(): AudioPipeline =
            instance ?: synchronized(this) {
                instance ?: AudioPipeline().also { instance = it }
            }
    }

    private val isRecording = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Buffer capacity ensures non-blocking fan-out across wake word engine and forwarding gate
    private val _audioFlow = MutableSharedFlow<AudioFrame>(
        replay = 0,
        extraBufferCapacity = 64
    )
    val audioFlow: SharedFlow<AudioFrame> = _audioFlow.asSharedFlow()

    fun isRecordingActive(): Boolean = isRecording.get()

    @SuppressLint("MissingPermission")
    @Synchronized
    fun start(): Boolean {
        if (isRecording.get()) {
            Log.d(TAG, "AudioPipeline already running")
            return true
        }

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "Failed to get valid minBufferSize for 16kHz PCM16 Mono")
            return false
        }

        val bufferSize = maxOf(minBufferSize, FRAME_SIZE_SAMPLES * 2 * 4)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord state not STATE_INITIALIZED. Releasing.")
                audioRecord?.release()
                audioRecord = null
                return false
            }

            // ATTACH HARDWARE DSP FILTERS FOR CRYSTAL-CLEAR SPEECH IN NOISE
            val sessionId = audioRecord?.audioSessionId ?: 0
            if (sessionId != 0) {
                try {
                    if (android.media.audiofx.NoiseSuppressor.isAvailable()) {
                        android.media.audiofx.NoiseSuppressor.create(sessionId)?.apply {
                            enabled = true
                            Log.i(TAG, "Hardware DSP NoiseSuppressor activated")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "NoiseSuppressor error: ${e.message}")
                }

                try {
                    if (android.media.audiofx.AcousticEchoCanceler.isAvailable()) {
                        android.media.audiofx.AcousticEchoCanceler.create(sessionId)?.apply {
                            enabled = true
                            Log.i(TAG, "Hardware DSP AcousticEchoCanceler activated")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "AcousticEchoCanceler error: ${e.message}")
                }

                try {
                    if (android.media.audiofx.AutomaticGainControl.isAvailable()) {
                        android.media.audiofx.AutomaticGainControl.create(sessionId)?.apply {
                            enabled = true
                            Log.i(TAG, "Hardware DSP AutomaticGainControl activated")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "AutomaticGainControl error: ${e.message}")
                }
            }

            audioRecord?.startRecording()
            isRecording.set(true)

            recordingJob = scope.launch {
                val buffer = ShortArray(FRAME_SIZE_SAMPLES)
                while (isActive && isRecording.get()) {
                    val readSamples = audioRecord?.read(buffer, 0, FRAME_SIZE_SAMPLES) ?: -1
                    if (readSamples > 0) {
                        val frameData = buffer.copyOf(readSamples)
                        val frame = AudioFrame(
                            data = frameData,
                            timestampMs = System.currentTimeMillis(),
                            sampleRate = SAMPLE_RATE
                        )
                        _audioFlow.tryEmit(frame)
                    } else if (readSamples < 0) {
                        Log.w(TAG, "AudioRecord.read error: $readSamples")
                        delay(10)
                    }
                }
            }

            Log.i(TAG, "AudioPipeline started successfully (Single AudioRecord owner)")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting AudioPipeline: ${e.message}", e)
            stop()
            return false
        }
    }

    @Synchronized
    fun stop() {
        if (!isRecording.getAndSet(false)) return

        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception releasing AudioRecord: ${e.message}")
        } finally {
            audioRecord = null
        }
        Log.i(TAG, "AudioPipeline stopped and AudioRecord released")
    }
}
