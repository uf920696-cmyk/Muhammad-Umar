package com.jarvis.voice.buffer

import com.jarvis.voice.pipeline.AudioFrame
import java.util.ArrayDeque

/**
 * PRE-WAKE PRIVACY BUFFER:
 * Enforces local-only storage before wake.
 * - Maximum ring buffer capacity: exactly 1000 ms (1 second).
 * - Forward pre-roll window: default 300 ms, maximum 500 ms.
 * - STRICT INVARIANT: Never outputs any audio frame older than:
 *     wakeFrameTimestamp - preRollMs
 */
class PreWakeRingBuffer(
    private val maxBufferDurationMs: Long = 1000L,
    val defaultPreRollMs: Long = 300L,
    val maxPreRollMs: Long = 500L
) {
    private val buffer = ArrayDeque<AudioFrame>()
    private val lock = Any()

    /**
     * Appends an audio frame and purges any frame older than maxBufferDurationMs (1s).
     */
    fun push(frame: AudioFrame) {
        synchronized(lock) {
            buffer.addLast(frame)
            val cutoff = frame.timestampMs - maxBufferDurationMs
            while (buffer.isNotEmpty() && buffer.first.timestampMs < cutoff) {
                buffer.removeFirst()
            }
        }
    }

    /**
     * Extracts ONLY frames within the approved pre-roll window:
     *   [wakeTimestamp - clampedPreRollMs, wakeTimestamp]
     * Never returns any frame older than (wakeTimestamp - clampedPreRollMs).
     */
    fun extractPreRoll(wakeTimestampMs: Long, requestedPreRollMs: Long = defaultPreRollMs): List<AudioFrame> {
        val clampedPreRoll = requestedPreRollMs.coerceIn(0L, maxPreRollMs)
        val strictCutoff = wakeTimestampMs - clampedPreRoll

        synchronized(lock) {
            return buffer.filter { frame ->
                frame.timestampMs >= strictCutoff && frame.timestampMs <= wakeTimestampMs
            }.toList()
        }
    }

    /**
     * Clears all buffered frames.
     */
    fun clear() {
        synchronized(lock) {
            buffer.clear()
        }
    }

    val currentDurationMs: Long
        get() = synchronized(lock) {
            if (buffer.isEmpty()) 0L
            else buffer.last.timestampMs - buffer.first.timestampMs
        }

    val size: Int
        get() = synchronized(lock) { buffer.size }
}
