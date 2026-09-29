package com.jarvis.device.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log

/**
 * 100% REAL HARDWARE SYSTEM & SETTINGS CONTROLLER:
 * - Real Alarms (Sets real clock alarms at requested hours and minutes)
 * - Real Camera (Opens Camera for Photo / Selfie Front & Back)
 * - Real System Settings (WiFi, Bluetooth, Display, Battery, Sound, Accessibility)
 */
class SystemFeatureController(private val context: Context) {

    companion object {
        private const val TAG = "SystemFeatureController"
    }

    /**
     * Sets a real alarm in the Android Clock app.
     * e.g. "alarm set karo 7 baje ka", "alarm lagao 6:30 ka"
     */
    fun setAlarm(hour: Int, minute: Int, message: String = "JARVIS Alarm"): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Log.i(TAG, "Alarm set successfully for $hour:$minute")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set alarm: ${e.message}", e)
            false
        }
    }

    /**
     * Opens Camera and triggers capture mode (Front or Back camera)
     */
    fun openCamera(isFront: Boolean = false): Boolean {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                if (isFront) {
                    // Front camera intent extras for Android cameras
                    putExtra("android.intent.extras.CAMERA_FACING", 1)
                    putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                    putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
                } else {
                    putExtra("android.intent.extras.CAMERA_FACING", 0)
                    putExtra("android.intent.extras.LENS_FACING_BACK", 1)
                    putExtra("android.intent.extra.USE_FRONT_CAMERA", false)
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
            Log.i(TAG, "Camera opened: front=$isFront")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open camera: ${e.message}")
            false
        }
    }

    /**
     * Opens specific system settings page
     */
    fun openSetting(settingType: String): Boolean {
        return try {
            val action = when (settingType.lowercase()) {
                "wifi" -> Settings.ACTION_WIFI_SETTINGS
                "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
                "display", "brightness", "screen" -> Settings.ACTION_DISPLAY_SETTINGS
                "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
                "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
                "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
                "apps" -> Settings.ACTION_APPLICATION_SETTINGS
                "storage" -> Settings.ACTION_INTERNAL_STORAGE_SETTINGS
                else -> Settings.ACTION_SETTINGS
            }
            val intent = Intent(action).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open setting $settingType: ${e.message}")
            false
        }
    }
}
