package com.jarvis.device.audio

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.jarvis.core.common.ToolResult

/**
 * VOLUME CONTROLLER WITH HARDWARE READ-BACK:
 * - Controls media, ring, call, and alarm volume streams.
 * - Confirms new volume level via audioManager.getStreamVolume() read-back.
 * - Absolute honesty: Returns ToolResult.Verified ONLY when read-back matches requested target.
 */
class VolumeController(private val context: Context) {

    companion object {
        private const val TAG = "VolumeController"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    /**
     * Set volume level for a specific stream (0 to 100 percentage).
     */
    fun setVolumePercent(streamType: Int = AudioManager.STREAM_MUSIC, percent: Int): ToolResult {
        val am = audioManager ?: return ToolResult.Failed(
            error = "AudioManager not available on device",
            recoverable = false
        )

        val clampedPercent = percent.coerceIn(0, 100)
        val maxVolume = am.getStreamMaxVolume(streamType)
        val targetIndex = (clampedPercent * maxVolume) / 100

        try {
            am.setStreamVolume(streamType, targetIndex, AudioManager.FLAG_SHOW_UI)

            // Independent Hardware Read-back
            val actualIndex = am.getStreamVolume(streamType)
            val actualPercent = (actualIndex * 100) / maxVolume

            Log.i(TAG, "Requested volume: $clampedPercent% ($targetIndex), Read-back volume: $actualPercent% ($actualIndex)")

            return if (actualIndex == targetIndex) {
                ToolResult.Verified(
                    action = "set_volume",
                    verifiedState = mapOf(
                        "stream" to streamType,
                        "percent" to actualPercent,
                        "volumeIndex" to actualIndex,
                        "maxIndex" to maxVolume
                    )
                )
            } else {
                ToolResult.Dispatched(
                    action = "set_volume",
                    dispatchDetails = "Dispatched target $clampedPercent%, but hardware reported $actualPercent% (possible DND or OEM constraint)"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting volume: ${e.message}")
            return ToolResult.Failed("Failed to set volume: ${e.message}", recoverable = true)
        }
    }

    fun getVolumePercent(streamType: Int = AudioManager.STREAM_MUSIC): Int {
        val am = audioManager ?: return 0
        val maxVolume = am.getStreamMaxVolume(streamType)
        if (maxVolume <= 0) return 0
        val current = am.getStreamVolume(streamType)
        return (current * 100) / maxVolume
    }
}
