package com.jarvis.device.display

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.jarvis.core.common.ToolResult

/**
 * BRIGHTNESS CONTROLLER WITH HARDWARE READ-BACK:
 * - Reads and adjusts screen brightness (0 to 255).
 * - Enforces Settings.System.canWrite() check.
 * - Absolute honesty: Returns ToolResult.NeedsUserAction if WRITE_SETTINGS permission is missing.
 * - Reads back Settings.System.SCREEN_BRIGHTNESS to confirm actual applied value.
 */
class BrightnessController(private val context: Context) {

    companion object {
        private const val TAG = "BrightnessController"
    }

    /**
     * Sets screen brightness to a target percentage (0 to 100).
     */
    fun setBrightnessPercent(percent: Int): ToolResult {
        // 1. Check special permission
        if (!Settings.System.canWrite(context)) {
            Log.w(TAG, "Cannot write settings: WRITE_SETTINGS permission not granted")
            return ToolResult.NeedsUserAction(
                action = "set_brightness",
                actionRequired = "Grant WRITE_SETTINGS permission in system settings to allow brightness control"
            )
        }

        val clampedPercent = percent.coerceIn(0, 100)
        val targetRaw = (clampedPercent * 255) / 100

        try {
            // Disable auto brightness if set to manual
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )

            // Write target brightness
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                targetRaw
            )

            // Independent Hardware Read-back
            val actualRaw = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
            val actualPercent = (actualRaw * 100) / 255

            Log.i(TAG, "Requested brightness: $clampedPercent% ($targetRaw), Read-back brightness: $actualPercent% ($actualRaw)")

            return if (actualRaw == targetRaw) {
                ToolResult.Verified(
                    action = "set_brightness",
                    verifiedState = mapOf(
                        "percent" to actualPercent,
                        "rawBrightness" to actualRaw
                    )
                )
            } else {
                ToolResult.Dispatched(
                    action = "set_brightness",
                    dispatchDetails = "Dispatched brightness $clampedPercent%, but hardware reported $actualPercent%"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set brightness: ${e.message}")
            return ToolResult.Failed("Failed to set brightness: ${e.message}", recoverable = true)
        }
    }

    fun getBrightnessPercent(): Int {
        return try {
            val raw = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
            (raw * 100) / 255
        } catch (e: Exception) {
            0
        }
    }
}
