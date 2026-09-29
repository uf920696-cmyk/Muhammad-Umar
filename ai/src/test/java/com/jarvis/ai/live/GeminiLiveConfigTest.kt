package com.jarvis.ai.live

import com.jarvis.core.common.JarvisState
import com.jarvis.voice.buffer.ForwardingGate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class GeminiLiveConfigTest {

    @Test
    fun testAudioConfigurationStandards() {
        assertEquals(16000, GeminiLiveConfig.INPUT_SAMPLE_RATE)
        assertEquals(1, GeminiLiveConfig.INPUT_CHANNELS)
        assertEquals(2, GeminiLiveConfig.INPUT_BYTES_PER_SAMPLE)

        assertEquals(24000, GeminiLiveConfig.OUTPUT_SAMPLE_RATE)
        assertEquals(1, GeminiLiveConfig.OUTPUT_CHANNELS)

        assertEquals(512, GeminiLiveConfig.CHUNK_SAMPLES)
        assertEquals(15 * 60 * 1000L, GeminiLiveConfig.MAX_SESSION_DURATION_MS)
    }

    @Test
    fun testForwardingGateLifecycleWithLiveSession() = runTest {
        val gate = ForwardingGate()

        assertFalse("Gate must be initially closed", gate.isOpen())

        gate.openGate()
        assertTrue("Gate must be open during live session", gate.isOpen())

        val testPcm = ShortArray(512) { it.toShort() }
        gate.submitAudio(testPcm)

        val received = gate.forwardedAudioFlow.first()
        assertEquals(512, received.size)

        gate.closeGate()
        assertFalse("Gate must be closed after live session disconnect", gate.isOpen())
    }
}
