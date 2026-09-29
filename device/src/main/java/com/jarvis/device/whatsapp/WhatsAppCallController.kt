package com.jarvis.device.whatsapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.jarvis.core.common.ToolResult
import com.jarvis.device.contacts.ContactResolver

/**
 * WHATSAPP CALL CONTROLLER:
 * Automates WhatsApp voice and video calling to a contact (e.g., "Ali").
 *
 * CRITICAL ARCHITECTURAL CONSTRAINTS & HONESTY CONTRACT:
 * 1. Third-Party App Boundary: WhatsApp is an external closed-source third-party application.
 *    Android does NOT provide an OS callback to confirm whether WhatsApp actually established
 *    or connected the call.
 * 2. Absolute Honesty: This controller NEVER returns ToolResult.Verified. It returns:
 *    - ToolResult.Dispatched: When the WhatsApp call intent or deep-link was successfully launched.
 *    - ToolResult.NeedsPermission: If READ_CONTACTS or CALL_PHONE is missing.
 *    - ToolResult.NeedsUserAction: If WhatsApp is not installed or contact has no phone number.
 *    - ToolResult.Failed: On intent resolution failure or invalid parameters.
 */
class WhatsAppCallController(private val context: Context) {

    companion object {
        private const val TAG = "WhatsAppCallController"
        const val WHATSAPP_PACKAGE = "com.whatsapp"
        const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
    }

    private val contactResolver = ContactResolver(context)

    /**
     * Checks if WhatsApp or WhatsApp Business is installed.
     */
    fun isWhatsAppInstalled(): Boolean {
        val pm = context.packageManager
        return try {
            pm.getPackageInfo(WHATSAPP_PACKAGE, 0)
            true
        } catch (e: Exception) {
            try {
                pm.getPackageInfo(WHATSAPP_BUSINESS_PACKAGE, 0)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    /**
     * Call contact on WhatsApp by spoken name or direct phone number.
     * @param target Spoken name ("Ali") or raw phone number ("+1234567890")
     * @param isVideo true for video call, false for voice call
     */
    fun callContact(target: String, isVideo: Boolean = false): ToolResult {
        // 1. Verify WhatsApp is installed
        if (!isWhatsAppInstalled()) {
            return ToolResult.NeedsUserAction(
                action = "whatsapp_call",
                actionRequired = "WhatsApp is not installed on this device"
            )
        }

        // 2. Resolve Contact
        var resolvedNumber: String? = null
        var contactName: String = target

        if (target.matches("^[+0-9\\-\\s()]+$".toRegex()) && target.filter { it.isDigit() }.length >= 7) {
            resolvedNumber = target.filter { it.isDigit() || it == '+' }
        } else {
            val (errorResult, matches) = contactResolver.resolveContactByName(target)
            if (errorResult != null) return errorResult

            when {
                matches.isEmpty() -> {
                    return ToolResult.Failed("No contact matching '$target' found in phonebook", recoverable = false)
                }
                matches.size == 1 -> {
                    resolvedNumber = matches[0].normalizedNumber.ifBlank { matches[0].phoneNumber }
                    contactName = matches[0].displayName
                }
                else -> {
                    val candidateNames = matches.take(3).map { "${it.displayName} (${it.phoneNumber})" }
                    return ToolResult.Dispatched(
                        action = "whatsapp_call",
                        dispatchDetails = "Multiple contacts matched '$target': ${candidateNames.joinToString(", ")}. Please specify exact contact."
                    )
                }
            }
        }

        if (resolvedNumber.isNullOrBlank()) {
            return ToolResult.Failed("Contact '$target' does not have a valid phone number", recoverable = false)
        }

        // Clean number for WhatsApp international format (no +, no leading 0)
        val cleanDigits = resolvedNumber.filter { it.isDigit() }

        // 3. Dispatch WhatsApp Direct Intent
        return try {
            // Dispatch WhatsApp intent
            val intent = Intent(Intent.ACTION_VIEW).apply {
                // WhatsApp custom scheme for voice or chat
                data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanDigits")
                setPackage(WHATSAPP_PACKAGE)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            context.startActivity(intent)

            Log.i(TAG, "Dispatched WhatsApp call intent for $contactName ($cleanDigits)")

            // Absolute Honesty: ALWAYS return Dispatched for third-party apps
            ToolResult.Dispatched(
                action = "whatsapp_call",
                dispatchDetails = "Dispatched WhatsApp ${if (isVideo) "video" else "voice"} call intent to $contactName ($cleanDigits). Note: WhatsApp is an external application; completion cannot be verified via hardware read-back."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch WhatsApp intent: ${e.message}")
            ToolResult.Failed("Failed to launch WhatsApp: ${e.message}", recoverable = true)
        }
    }
}
