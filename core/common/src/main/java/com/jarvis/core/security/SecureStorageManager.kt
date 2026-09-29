package com.jarvis.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * SECURE STORAGE MANAGER:
 * Uses Android Keystore + MasterKey (AES256_GCM / AES256_SIV) via EncryptedSharedPreferences.
 * Stores sensitive credentials locally on-device:
 * - Picovoice Porcupine AccessKey
 * - Per-Device Client Secret for /backend communication
 * 
 * NEVER logs keys or includes them in backups.
 */
class SecureStorageManager(context: Context) {

    companion object {
        private const val TAG = "SecureStorageManager"
        private const val PREFS_FILE_NAME = "jarvis_secure_prefs"

        private const val KEY_PICOVOICE_ACCESS_KEY = "picovoice_access_key"
        private const val KEY_DEVICE_SECRET = "device_client_secret"
        private const val KEY_LIVE_IDLE_TIMEOUT_SEC = "live_idle_timeout_sec"
        private const val KEY_MEMORY_ENABLED = "memory_enabled"
        private const val KEY_BACKEND_URL = "backend_url"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_AI_MODEL = "ai_model"

        const val DEFAULT_BACKEND_URL = "https://ais-dev-4lhpii67foqhnl7455let4-401772682672.asia-southeast1.run.app"
        const val DEFAULT_AI_MODEL = "gemini-3.8-flash"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getPicovoiceAccessKey(): String? = securePrefs.getString(KEY_PICOVOICE_ACCESS_KEY, null)

    fun setPicovoiceAccessKey(key: String) {
        securePrefs.edit().putString(KEY_PICOVOICE_ACCESS_KEY, key.trim()).apply()
        Log.i(TAG, "Picovoice AccessKey updated in encrypted storage")
    }

    fun clearPicovoiceAccessKey() {
        securePrefs.edit().remove(KEY_PICOVOICE_ACCESS_KEY).apply()
        Log.i(TAG, "Picovoice AccessKey cleared from encrypted storage")
    }

    fun getDeviceClientSecret(): String {
        var secret = securePrefs.getString(KEY_DEVICE_SECRET, null)
        if (secret.isNullOrBlank()) {
            secret = java.util.UUID.randomUUID().toString() + "-" + System.currentTimeMillis()
            securePrefs.edit().putString(KEY_DEVICE_SECRET, secret).apply()
        }
        return secret
    }

    fun getLiveIdleTimeoutSec(): Int = securePrefs.getInt(KEY_LIVE_IDLE_TIMEOUT_SEC, 20)

    fun setLiveIdleTimeoutSec(seconds: Int) {
        val clamped = seconds.coerceIn(10, 60)
        securePrefs.edit().putInt(KEY_LIVE_IDLE_TIMEOUT_SEC, clamped).apply()
    }

    fun isMemoryEnabled(): Boolean = securePrefs.getBoolean(KEY_MEMORY_ENABLED, true)

    fun setMemoryEnabled(enabled: Boolean) {
        securePrefs.edit().putBoolean(KEY_MEMORY_ENABLED, enabled).apply()
    }

    fun getBackendUrl(): String {
        val stored = securePrefs.getString(KEY_BACKEND_URL, null)
        return if (!stored.isNullOrBlank()) stored else DEFAULT_BACKEND_URL
    }

    fun setBackendUrl(url: String) {
        val clean = url.trim().trimEnd('/')
        securePrefs.edit().putString(KEY_BACKEND_URL, clean).apply()
    }

    fun getGeminiApiKey(): String? = securePrefs.getString(KEY_GEMINI_API_KEY, null)

    fun setGeminiApiKey(key: String) {
        securePrefs.edit().putString(KEY_GEMINI_API_KEY, key.trim()).apply()
    }

    fun clearGeminiApiKey() {
        securePrefs.edit().remove(KEY_GEMINI_API_KEY).apply()
    }

    fun getAiModel(): String {
        val stored = securePrefs.getString(KEY_AI_MODEL, null)
        return if (!stored.isNullOrBlank()) stored else DEFAULT_AI_MODEL
    }

    fun setAiModel(model: String) {
        securePrefs.edit().putString(KEY_AI_MODEL, model.trim()).apply()
    }
}
