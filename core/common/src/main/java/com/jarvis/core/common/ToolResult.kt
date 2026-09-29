package com.jarvis.core.common

/**
 * Absolute Honesty Tool Result Contract.
 * Never turn Dispatched into Verified without independent confirmation.
 */
sealed class ToolResult {
    /** Action completed and verified by reading back real device state */
    data class Verified(
        val action: String = "",
        val verifiedState: Map<String, Any?> = emptyMap(),
        val detail: String = "",
        val timestampMs: Long = System.currentTimeMillis()
    ) : ToolResult()

    /** Accepted/dispatched to Android system, but final outcome not independently confirmed */
    data class Dispatched(
        val action: String = "",
        val dispatchDetails: String = "",
        val detail: String = "",
        val timestampMs: Long = System.currentTimeMillis()
    ) : ToolResult()

    /** Requires manual user intervention on the device screen */
    data class NeedsUserAction(
        val action: String = "",
        val actionRequired: String = "",
        val what: String = ""
    ) : ToolResult()

    /** Blocked on runtime permission or special Android access */
    data class NeedsPermission(
        val action: String = "",
        val permission: String = "",
        val settingsAction: String = ""
    ) : ToolResult()

    /** High-risk action requiring explicit user voice/UI confirmation */
    data class NeedsConfirmation(
        val summary: String,
        val executionId: String = ""
    ) : ToolResult()

    /** Multiple candidates exist (e.g. contacts with same name) */
    data class Ambiguous(
        val candidates: List<String> = emptyList()
    ) : ToolResult()

    /** Not supported on this Android OS version or OEM hardware */
    data class Unsupported(
        val reason: String
    ) : ToolResult()

    /** Execution failed due to runtime error or timeout */
    data class Failed(
        val reason: String = "",
        val error: String = "",
        val recoverable: Boolean = false
    ) : ToolResult() {
        val errorMessage: String
            get() = reason.ifBlank { error }
    }
}
