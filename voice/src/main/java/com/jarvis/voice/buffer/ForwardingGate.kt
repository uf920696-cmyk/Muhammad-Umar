package com.jarvis.voice.buffer

import android.util.Log
import com.jarvis.voice.pipeline.AudioFrame
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean

/**
 * FORWARDING GATE & PRIVACY CONTROLLER:
 *
 * Rules:
 * 1. Before wake: ZERO audio leaves the device. Frames only circulate into the local 1s PreWakeRingBuffer.
 * 2. On wake: Gate opens. Only frames within [wakeTimestamp - preRoll, now] are forwarded.
 * 3. While Gemini Live connects: Queue at most 8000 ms (8 seconds) of post-wake audio.
 * 4. If connection fails or drops: Immediately drop all queued audio and report failure.
 * 5. While local "Ji?" plays: Audio forwarding is gated to prevent acoustic feedback into Gemini.
 */
class ForwardingGate(
    private val ringBuffer: PreWakeRingBuffer = PreWakeRingBuffer()
) {
    companion object {
        private const val TAG = "ForwardingGate"
        const val MAX_POST_WAKE_QUEUE_MS = 8000L
    }

    private val isGateOpen = AtomicBoolean(false)
    private val isMutedForAck = AtomicBoolean(false)
    private val postWakeQueue = ArrayDeque<AudioFrame>()
    private val queueLock = Any()

    private val _forwardStream = MutableSharedFlow<AudioFrame>(
        replay = 0,
        extraBufferCapacity = 128
    )
    val forwardStream: SharedFlow<AudioFrame> = _forwardStream.asSharedFlow()

    // Test and LiveConfig alias for short-array consumption
    private val _forwardedAudioFlow = MutableSharedFlow<ShortArray>(
        replay = 0,
        extraBufferCapacity = 128
    )
    val forwardedAudioFlow: SharedFlow<ShortArray> = _forwardedAudioFlow.asSharedFlow()

    fun isOpen(): Boolean = isGateOpen.get()

    /**
     * Ingest incoming frame from single AudioPipeline.
     */
    fun onAudioFrame(frame: AudioFrame) {
        if (!isGateOpen.get()) {
            // Local-only pre-wake buffer
            ringBuffer.push(frame)
        } else {
            // Gate is open (post-wake)
            if (isMutedForAck.get()) {
                // Suppress frame while local "Ji?" is speaking to avoid acoustic feedback
                return
            }

            synchronized(queueLock) {
                postWakeQueue.addLast(frame)
                // Purge if exceeded 8 seconds maximum connection queue
                val oldestAllowed = frame.timestampMs - MAX_POST_WAKE_QUEUE_MS
                while (postWakeQueue.isNotEmpty() && postWakeQueue.first.timestampMs < oldestAllowed) {
                    val dropped = postWakeQueue.removeFirst()
                    Log.w(TAG, "Dropped post-wake frame exceeding 8s limit: timestamp ${dropped.timestampMs}")
                }
            }

            _forwardStream.tryEmit(frame)
            _forwardedAudioFlow.tryEmit(frame.data)
        }
    }

    /**
     * Submit raw PCM shorts directly (used in tests and forward pipelines).
     */
    fun submitAudio(pcmData: ShortArray) {
        val frame = AudioFrame(pcmData.clone(), System.currentTimeMillis())
        onAudioFrame(frame)
    }

    /**
     * Triggered on verified local wake word detection.
     */
    fun openGate(wakeTimestampMs: Long = System.currentTimeMillis(), requestedPreRollMs: Long = ringBuffer.defaultPreRollMs) {
        val preRollFrames = ringBuffer.extractPreRoll(wakeTimestampMs, requestedPreRollMs)
        ringBuffer.clear()

        synchronized(queueLock) {
            postWakeQueue.clear()
            for (frame in preRollFrames) {
                postWakeQueue.addLast(frame)
                _forwardStream.tryEmit(frame)
                _forwardedAudioFlow.tryEmit(frame.data)
            }
        }

        isGateOpen.set(true)
        Log.i(TAG, "ForwardingGate OPENED. Pre-roll forwarded frames: ${preRollFrames.size}")
    }

    /**
     * Suppress microphone forwarding during local acknowledgement playback (~300-400ms).
     */
    fun setMutedForAck(muted: Boolean) {
        isMutedForAck.set(muted)
        Log.d(TAG, "ForwardingGate ack mute set to: $muted")
    }

    /**
     * Connection failed or idle timeout reached: close gate and discard any queued audio.
     */
    fun closeGate(reason: String = "Normal closure") {
        isGateOpen.set(false)
        isMutedForAck.set(false)
        synchronized(queueLock) {
            val droppedCount = postWakeQueue.size
            postWakeQueue.clear()
            Log.i(TAG, "ForwardingGate CLOSED (reason: $reason). Dropped $droppedCount post-wake frames.")
        }
        ringBuffer.clear()
    }
}
