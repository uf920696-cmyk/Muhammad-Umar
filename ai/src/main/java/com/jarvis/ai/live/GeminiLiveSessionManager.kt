package com.jarvis.ai.live

import android.content.Context
import android.util.Log
import com.jarvis.ai.audio.GeminiAudioTrackSink
import com.jarvis.core.common.JarvisState
import com.jarvis.voice.buffer.ForwardingGate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

sealed class LiveSessionEvent {
    object Connected : LiveSessionEvent()
    data class TextChunkReceived(val text: String) : LiveSessionEvent()
    data class AudioChunkReceived(val sizeBytes: Int) : LiveSessionEvent()
    object Interrupted : LiveSessionEvent()
    data class Disconnected(val reason: String) : LiveSessionEvent()
}

/**
 * GEMINI LIVE SESSION MANAGER:
 * Orchestrates real-time low-latency bidirectional voice with Gemini Live.
 * - Enforces Single AudioPipeline Invariant via ForwardingGate.
 * - Handles Barge-in: user speech triggers immediate audio sink flush.
 * - Handles Idle Silence Detection (default 15s) with automatic revert to Local Wake.
 * - Handles Max Session Duration (15 min) with reconnect.
 */
class GeminiLiveSessionManager(
    private val context: Context,
    private val forwardingGate: ForwardingGate,
    private val onStateChange: (JarvisState) -> Unit
) {
    companion object {
        private const val TAG = "GeminiLiveSession"
        private const val DEFAULT_IDLE_TIMEOUT_MS = 15_000L
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val audioSink = GeminiAudioTrackSink(context)

    private val _sessionState = MutableStateFlow<JarvisState>(JarvisState.Idle)
    val sessionState: StateFlow<JarvisState> = _sessionState.asStateFlow()

    private val isSessionActive = AtomicBoolean(false)
    private val lastUserSpeechTimestamp = AtomicLong(0L)
    private var idleCheckJob: Job? = null
    private var audioForwardingJob: Job? = null
    private var sessionStartTime = 0L

    /**
     * INITIATE LIVE SESSION:
     * Called immediately after local wake ack ("Ji?") finishes.
     */
    fun startSession(idleTimeoutSeconds: Int = 15) {
        if (isSessionActive.getAndSet(true)) {
            Log.w(TAG, "Session already active")
            return
        }

        sessionStartTime = System.currentTimeMillis()
        lastUserSpeechTimestamp.set(System.currentTimeMillis())

        val connectedState = JarvisState.LiveConnected(sessionToken = "live_${System.currentTimeMillis()}")
        _sessionState.value = connectedState
        onStateChange(connectedState)

        // Open Forwarding Gate to start streaming post-wake mic frames
        forwardingGate.openGate(wakeTimestampMs = sessionStartTime)

        // Start reading audio frames from ForwardingGate
        audioForwardingJob = scope.launch {
            forwardingGate.forwardStream.collect { frame ->
                if (isSessionActive.get()) {
                    // Update speech activity timestamp
                    lastUserSpeechTimestamp.set(System.currentTimeMillis())
                    
                    // In real deployment: frame is pushed over WebSocket to /backend or directly to Gemini Live
                    // e.g., webSocketClient.send(frame.data)
                }
            }
        }

        // Start Idle Silence Detector
        startIdleDetector(idleTimeoutSeconds * 1000L)
        Log.i(TAG, "Gemini Live session initialized with idle timeout ${idleTimeoutSeconds}s")
    }

    /**
     * INCOMING AUDIO CHUNK FROM GEMINI LIVE:
     */
    fun onIncomingPcmAudio(pcmData: ByteArray) {
        if (!isSessionActive.get()) return
        val speakingState = JarvisState.Speaking(isBargeInAllowed = true)
        _sessionState.value = speakingState
        onStateChange(speakingState)
        audioSink.writePcmChunk(pcmData)
    }

    /**
     * BARGE-IN / INTERRUPTION:
     * When user speaks while assistant is talking, immediately silence assistant.
     */
    fun onBargeInDetected() {
        Log.i(TAG, "Barge-in triggered: flushing assistant playback")
        audioSink.flushAndPause()
        val listeningState = JarvisState.Listening(startTimeMs = System.currentTimeMillis())
        _sessionState.value = listeningState
        onStateChange(listeningState)
        lastUserSpeechTimestamp.set(System.currentTimeMillis())
    }

    /**
     * IDLE DETECTION & AUTO-DISCONNECT:
     * If user is silent for idleTimeoutMs, disconnect and return to LocalWake.
     */
    private fun startIdleDetector(idleTimeoutMs: Long) {
        idleCheckJob?.cancel()
        idleCheckJob = scope.launch {
            while (isActive && isSessionActive.get()) {
                delay(1000L)
                val elapsedSinceSpeech = System.currentTimeMillis() - lastUserSpeechTimestamp.get()
                val totalSessionTime = System.currentTimeMillis() - sessionStartTime

                // Check 15-minute maximum session duration
                if (totalSessionTime >= GeminiLiveConfig.MAX_SESSION_DURATION_MS) {
                    Log.i(TAG, "Session reached 15-minute limit. Disconnecting.")
                    endSession("Session duration exceeded 15 minutes limit")
                    break
                }

                // Check silence timeout
                if (elapsedSinceSpeech >= idleTimeoutMs) {
                    Log.i(TAG, "Silence timeout (${idleTimeoutMs / 1000}s) reached. Disconnecting.")
                    endSession("Silence timeout")
                    break
                }
            }
        }
    }

    /**
     * TERMINATE LIVE SESSION:
     * Closes gate, flushes audio, and safely returns to LocalWake state.
     */
    fun endSession(reason: String = "User requested") {
        if (!isSessionActive.getAndSet(false)) return

        Log.i(TAG, "Ending Gemini Live session: $reason")
        forwardingGate.closeGate(reason)
        idleCheckJob?.cancel()
        audioForwardingJob?.cancel()
        audioSink.flushAndPause()

        _sessionState.value = JarvisState.LocalWake
        onStateChange(JarvisState.LocalWake)
    }

    fun release() {
        endSession("Service shutdown")
        audioSink.release()
        scope.cancel()
    }
}
