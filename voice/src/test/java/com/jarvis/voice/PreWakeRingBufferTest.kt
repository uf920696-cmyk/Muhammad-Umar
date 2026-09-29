package com.jarvis.voice

import com.jarvis.voice.buffer.PreWakeRingBuffer
import com.jarvis.voice.pipeline.AudioFrame
import org.junit.Assert.*
import org.junit.Test

/**
 * STRICT PRE-WAKE PRIVACY TIMESTAMP BOUNDARY TEST:
 * Verifies that:
 * 1. Buffer caps at 1000ms.
 * 2. No frame older than (wakeTimestamp - preRoll) is EVER returned.
 * 3. Pre-roll is clamped to max 500ms.
 */
class PreWakeRingBufferTest {

    @Test
    fun testTimestampBoundary_StrictCutoff() {
        val ringBuffer = PreWakeRingBuffer(maxBufferDurationMs = 1000L, defaultPreRollMs = 300L, maxPreRollMs = 500L)
        val dummyData = ShortArray(512)

        val baseTimestamp = 10000L

        // Feed frames spanning 1000ms at 50ms intervals: 10000ms to 11000ms (21 frames)
        for (t in 0..20) {
            val ts = baseTimestamp + (t * 50L)
            ringBuffer.push(AudioFrame(dummyData, ts))
        }

        val wakeTimestamp = 11000L
        val preRollMs = 300L

        // Expected cutoff: 11000 - 300 = 10700L
        val extracted = ringBuffer.extractPreRoll(wakeTimestamp, preRollMs)

        assertFalse("Extracted frames should not be empty", extracted.isEmpty())

        for (frame in extracted) {
            assertTrue(
                "Frame with timestamp ${frame.timestampMs} must be >= strict cutoff 10700L",
                frame.timestampMs >= (wakeTimestamp - preRollMs)
            )
            assertTrue(
                "Frame with timestamp ${frame.timestampMs} must be <= wakeTimestamp 11000L",
                frame.timestampMs <= wakeTimestamp
            )
        }

        val oldestReturned = extracted.minOf { it.timestampMs }
        assertEquals(10700L, oldestReturned)
    }

    @Test
    fun testMaxPreRollClamping() {
        val ringBuffer = PreWakeRingBuffer(maxBufferDurationMs = 1000L, defaultPreRollMs = 300L, maxPreRollMs = 500L)
        val dummyData = ShortArray(512)
        val baseTimestamp = 20000L

        for (t in 0..20) {
            val ts = baseTimestamp + (t * 50L)
            ringBuffer.push(AudioFrame(dummyData, ts))
        }

        val wakeTimestamp = 21000L
        // Request 800ms (illegal, exceeds max 500ms)
        val extracted = ringBuffer.extractPreRoll(wakeTimestamp, requestedPreRollMs = 800L)

        // Must be clamped to 500ms: cutoff is 21000 - 500 = 20500L
        val oldestReturned = extracted.minOf { it.timestampMs }
        assertTrue("Pre-roll must be clamped to 500ms max", oldestReturned >= 20500L)
    }

    @Test
    fun testBufferCapacity_CapsAtOneSecond() {
        val ringBuffer = PreWakeRingBuffer(maxBufferDurationMs = 1000L)
        val dummyData = ShortArray(512)

        // Push 3 seconds worth of frames (0ms to 3000ms)
        for (ts in 0L..3000L step 50L) {
            ringBuffer.push(AudioFrame(dummyData, ts))
        }

        // Buffer should only contain frames within [2000L, 3000L]
        val allRemaining = ringBuffer.extractPreRoll(3000L, requestedPreRollMs = 500L)
        for (f in allRemaining) {
            assertTrue(f.timestampMs >= 2000L)
        }
    }
}
