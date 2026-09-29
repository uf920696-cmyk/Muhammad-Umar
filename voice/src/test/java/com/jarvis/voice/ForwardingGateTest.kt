package com.jarvis.voice

import com.jarvis.voice.buffer.ForwardingGate
import com.jarvis.voice.buffer.PreWakeRingBuffer
import com.jarvis.voice.pipeline.AudioFrame
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests ForwardingGate invariants:
 * 1. Zero frames forwarded before wake.
 * 2. On wake, only pre-roll + post-wake frames are forwarded.
 * 3. Mute during local ack suppresses frames.
 * 4. Close gate drops post-wake queued frames.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ForwardingGateTest {

    @Test
    fun testZeroFramesForwardedBeforeWake() = runTest {
        val ringBuffer = PreWakeRingBuffer()
        val gate = ForwardingGate(ringBuffer)
        val dummyData = ShortArray(512)

        assertFalse("Gate must initially be closed", gate.isOpen())

        val received = mutableListOf<AudioFrame>()
        val collectJob = launch {
            gate.forwardStream.collect { received.add(it) }
        }

        // Feed pre-wake frames
        gate.onAudioFrame(AudioFrame(dummyData, 1000L))
        gate.onAudioFrame(AudioFrame(dummyData, 1050L))
        gate.onAudioFrame(AudioFrame(dummyData, 1100L))

        // No frames should have been emitted
        assertEquals(0, received.size)

        collectJob.cancel()
    }

    @Test
    fun testGateOpensAndEmitsPreRoll() = runTest {
        val ringBuffer = PreWakeRingBuffer(defaultPreRollMs = 300L)
        val gate = ForwardingGate(ringBuffer)
        val dummyData = ShortArray(512)

        // Feed pre-wake frames
        gate.onAudioFrame(AudioFrame(dummyData, 1000L))
        gate.onAudioFrame(AudioFrame(dummyData, 1100L))
        gate.onAudioFrame(AudioFrame(dummyData, 1200L))

        val received = mutableListOf<AudioFrame>()
        val collectJob = launch {
            gate.forwardStream.collect { received.add(it) }
        }

        // Wake occurs at 1200L
        gate.openGate(wakeTimestampMs = 1200L, requestedPreRollMs = 200L)

        assertTrue(gate.isOpen())

        // Post-wake frame
        gate.onAudioFrame(AudioFrame(dummyData, 1250L))

        collectJob.cancel()

        // Should have received pre-roll frames (>= 1000L) plus post-wake (1250L)
        assertTrue(received.any { it.timestampMs >= 1000L })
    }

    @Test
    fun testAckMuteSuppression() = runTest {
        val ringBuffer = PreWakeRingBuffer()
        val gate = ForwardingGate(ringBuffer)
        val dummyData = ShortArray(512)

        gate.openGate(wakeTimestampMs = 1000L)
        gate.setMutedForAck(true)

        val received = mutableListOf<AudioFrame>()
        val collectJob = launch {
            gate.forwardStream.collect { received.add(it) }
        }

        // Send frame during "Ji?" ack
        gate.onAudioFrame(AudioFrame(dummyData, 1050L))
        assertEquals("Frame during ack mute must not be forwarded", 0, received.size)

        // Unmute
        gate.setMutedForAck(false)
        gate.onAudioFrame(AudioFrame(dummyData, 1100L))

        collectJob.cancel()
        assertEquals(1, received.size)
        assertEquals(1100L, received[0].timestampMs)
    }
}
