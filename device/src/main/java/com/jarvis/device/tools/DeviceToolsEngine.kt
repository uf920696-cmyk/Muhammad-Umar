package com.jarvis.device.tools

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.jarvis.core.common.ToolResult
import com.jarvis.device.apps.AppLauncher
import com.jarvis.device.audio.VolumeController
import com.jarvis.device.display.BrightnessController
import com.jarvis.device.torch.TorchController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * UNIFIED DEVICE ACTIONS ENGINE:
 * Maps function calls from Gemini Live to verified Android hardware tools.
 * Enforces Absolute Honesty:
 * - Verified: Hardware state read-back confirmed (e.g., Torch callback, Audio read-back).
 * - Dispatched: Intent sent to external app or broadcast without direct state read-back.
 * - NeedsPermission / NeedsUserAction: Missing runtime or settings permission.
 * - Failed: Hardware exception or unsupported device capability.
 */
class DeviceToolsEngine(private val context: Context) {

    companion object {
        private const val TAG = "DeviceToolsEngine"

        // Tool function identifiers matching Gemini tool declarations
        const val TOOL_SET_TORCH = "set_torch"
        const val TOOL_SET_VOLUME = "set_volume"
        const val TOOL_SET_BRIGHTNESS = "set_brightness"
        const val TOOL_LAUNCH_APP = "launch_app"
        const val TOOL_GET_DEVICE_STATUS = "get_device_status"
        const val TOOL_WHATSAPP_CALL = "whatsapp_call"
    }

    private val torchController = TorchController(context)
    private val volumeController = VolumeController(context)
    private val brightnessController = BrightnessController(context)
    private val appLauncher = AppLauncher(context)
    private val whatsAppCallController = com.jarvis.device.whatsapp.WhatsAppCallController(context)

    /**
     * Dispatches tool invocation with strict argument validation.
     */
    suspend fun executeTool(name: String, args: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        Log.i(TAG, "Executing tool: $name with args: $args")

        when (name) {
            TOOL_SET_TORCH -> {
                val enabled = args["enabled"] as? Boolean ?: (args["state"] as? String)?.equals("on", ignoreCase = true)
                if (enabled == null) {
                    return@withContext ToolResult.Failed("Missing or invalid 'enabled' boolean parameter", recoverable = false)
                }
                torchController.setTorch(enabled)
            }

            TOOL_SET_VOLUME -> {
                val percent = (args["percent"] as? Number)?.toInt()
                    ?: (args["level"] as? Number)?.toInt()
                if (percent == null) {
                    return@withContext ToolResult.Failed("Missing or invalid 'percent' parameter (0-100)", recoverable = false)
                }
                volumeController.setVolumePercent(AudioManager.STREAM_MUSIC, percent)
            }

            TOOL_SET_BRIGHTNESS -> {
                val percent = (args["percent"] as? Number)?.toInt()
                    ?: (args["level"] as? Number)?.toInt()
                if (percent == null) {
                    return@withContext ToolResult.Failed("Missing or invalid 'percent' parameter (0-100)", recoverable = false)
                }
                brightnessController.setBrightnessPercent(percent)
            }

            TOOL_LAUNCH_APP -> {
                val appName = args["app_name"] as? String
                val packageName = args["package_name"] as? String

                if (packageName != null) {
                    appLauncher.launchAppByPackage(packageName)
                } else if (appName != null) {
                    appLauncher.launchAppByName(appName)
                } else {
                    ToolResult.Failed("Either 'app_name' or 'package_name' must be provided", recoverable = false)
                }
            }

            TOOL_GET_DEVICE_STATUS -> {
                val statusMap = mapOf(
                    "torch_on" to torchController.isTorchOn(),
                    "volume_music_percent" to volumeController.getVolumePercent(AudioManager.STREAM_MUSIC),
                    "brightness_percent" to brightnessController.getBrightnessPercent()
                )
                ToolResult.Verified(
                    action = "get_device_status",
                    verifiedState = statusMap
                )
            }

            TOOL_WHATSAPP_CALL -> {
                val target = args["target"] as? String ?: args["contact_name"] as? String
                val isVideo = args["video"] as? Boolean ?: false

                if (target.isNullOrBlank()) {
                    ToolResult.Failed("Missing required 'target' or 'contact_name' parameter", recoverable = false)
                } else {
                    whatsAppCallController.callContact(target, isVideo)
                }
            }

            else -> {
                ToolResult.Failed("Unknown device tool: $name", recoverable = false)
            }
        }
    }

    /**
     * Tool declarations to feed into Gemini Live session setup configuration.
     */
    fun getToolDeclarations(): List<Map<String, Any>> {
        return listOf(
            mapOf(
                "name" to TOOL_SET_TORCH,
                "description" to "Turns device flashlight/torch on or off. Hardware state is confirmed via CameraManager callback.",
                "parameters" to mapOf(
                    "type" to "OBJECT",
                    "properties" to mapOf(
                        "enabled" to mapOf("type" to "BOOLEAN", "description" to "true to turn torch ON, false to turn torch OFF")
                    ),
                    "required" to listOf("enabled")
                )
            ),
            mapOf(
                "name" to TOOL_SET_VOLUME,
                "description" to "Sets device media playback volume percent (0 to 100). Hardware state confirmed via read-back.",
                "parameters" to mapOf(
                    "type" to "OBJECT",
                    "properties" to mapOf(
                        "percent" to mapOf("type" to "INTEGER", "description" to "Target volume level 0 to 100")
                    ),
                    "required" to listOf("percent")
                )
            ),
            mapOf(
                "name" to TOOL_SET_BRIGHTNESS,
                "description" to "Sets screen brightness percent (0 to 100). Requires WRITE_SETTINGS permission.",
                "parameters" to mapOf(
                    "type" to "OBJECT",
                    "properties" to mapOf(
                        "percent" to mapOf("type" to "INTEGER", "description" to "Target brightness level 0 to 100")
                    ),
                    "required" to listOf("percent")
                )
            ),
            mapOf(
                "name" to TOOL_LAUNCH_APP,
                "description" to "Launches an installed Android application by name (e.g. YouTube, Spotify) or package name.",
                "parameters" to mapOf(
                    "type" to "OBJECT",
                    "properties" to mapOf(
                        "app_name" to mapOf("type" to "STRING", "description" to "Common name of the app to launch"),
                        "package_name" to mapOf("type" to "STRING", "description" to "Optional exact package name")
                    )
                )
            ),
            mapOf(
                "name" to TOOL_GET_DEVICE_STATUS,
                "description" to "Returns current hardware status: torch state, media volume percentage, screen brightness percentage.",
                "parameters" to mapOf(
                    "type" to "OBJECT",
                    "properties" to emptyMap<String, Any>()
                )
            ),
            mapOf(
                "name" to TOOL_WHATSAPP_CALL,
                "description" to "Dispatches a WhatsApp voice or video call to a contact (e.g., 'Ali') or phone number. Returns Dispatched (third-party external app boundary).",
                "parameters" to mapOf(
                    "type" to "OBJECT",
                    "properties" to mapOf(
                        "target" to mapOf("type" to "STRING", "description" to "Contact name (e.g., 'Ali') or international phone number"),
                        "video" to mapOf("type" to "BOOLEAN", "description" to "true for video call, false for voice call (default: false)")
                    ),
                    "required" to listOf("target")
                )
            )
        )
    }
}
