package com.jarvis.core.common

/**
 * Single Authoritative System State for JARVIS.
 * Independent components must never display contradictory states.
 */
sealed class JarvisState {
    object Idle : JarvisState()
    object LocalWake : JarvisState()
    data class Listening(val startTimeMs: Long = System.currentTimeMillis()) : JarvisState()
    data class Thinking(val querySummary: String = "") : JarvisState()
    data class Speaking(
        val isBargeInAllowed: Boolean = true,
        val durationMs: Long = 0L
    ) : JarvisState()
    data class Executing(val actionName: String = "", val executionId: String = "") : JarvisState()
    data class Success(val message: String = "", val executionId: String = "") : JarvisState()
    data class Error(val reason: String = "", val recoverable: Boolean = true) : JarvisState()
    data class NeedsPermission(val permission: String = "", val settingsAction: String = "") : JarvisState()
    data class NeedsConfirmation(val summary: String = "", val executionId: String = "") : JarvisState()
    data class Researching(val topic: String = "") : JarvisState()
    data class LiveSession(val sessionId: String = "", val audioForwardingActive: Boolean = true) : JarvisState()
    data class LiveConnected(val sessionToken: String = "") : JarvisState()
}
