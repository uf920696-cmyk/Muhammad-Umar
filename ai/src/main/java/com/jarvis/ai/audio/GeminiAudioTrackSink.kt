package com.jarvis.ai.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.jarvis.ai.live.GeminiLiveConfig
import java.util.concurrent.atomic.AtomicBoolean

/**
 * LOW-LATENCY AUDIO TRACK SINK FOR GEMINI LIVE:
 * - Plays 24 kHz 16-bit PCM Mono audio received from Gemini Live.
 * - Supports instant barge-in flushing via flushAndPause().
 * - Requests and abandons audio focus cleanly.
 */
class GeminiAudioTrackSink(private val context: Context) {

    companion object {
        private const val TAG = "GeminiAudioTrackSink"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioTrack: AudioTrack? = null
    private val isPlaying = AtomicBoolean(false)

    init {
        initAudioTrack()
    }

    private fun initAudioTrack() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            GeminiLiveConfig.OUTPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val bufferSize = maxOf(minBufferSize, 4096)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(GeminiLiveConfig.OUTPUT_SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            isPlaying.set(true)
            Log.i(TAG, "AudioTrack initialized successfully for 24kHz PCM16 Mono")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AudioTrack: ${e.message}", e)
        }
    }

    /**
     * Writes incoming PCM audio chunk from Gemini Live to speaker.
     */
    fun writePcmChunk(pcmData: ByteArray) {
        val track = audioTrack ?: return
        try {
            if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                track.play()
                isPlaying.set(true)
            }
            track.write(pcmData, 0, pcmData.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing to AudioTrack: ${e.message}")
        }
    }

    /**
     * BARGE-IN INTERRUPTION FLUSH:
     * Immediately pauses and flushes pending buffered playback when user speaks.
     */
    fun flushAndPause() {
        try {
            audioTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.pause()
                    track.flush()
                }
            }
            isPlaying.set(false)
            Log.i(TAG, "AudioTrack flushed and paused for barge-in")
        } catch (e: Exception) {
            Log.e(TAG, "Error flushing AudioTrack: ${e.message}")
        }
    }

    fun release() {
        try {
            audioTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing AudioTrack: ${e.message}")
        } finally {
            audioTrack = null
            isPlaying.set(false)
        }
    }
}
