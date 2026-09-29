package com.jarvis.wakeword

import android.content.Context
import android.util.Log
import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import com.jarvis.voice.pipeline.AudioFrame

/**
 * PICOVOICE PORCUPINE WAKE ENGINE:
 * Local on-device wake-word detection for "JARVIS".
 *
 * Rules:
 * 1. AccessKey is dynamically injected from secure Keystore (never in code or repository).
 * 2. Uses Porcupine.Builder().setKeyword(Porcupine.BuiltInKeyword.JARVIS).
 * 3. Feeds frames directly from the single AudioPipeline via processFrame().
 * 4. Zero audio leaves the device during processing.
 */
class PorcupineWakeEngine(
    private val context: Context,
    private val accessKeyProvider: () -> String?
) : WakeWordEngine {

    companion object {
        private const val TAG = "PorcupineWakeEngine"
    }

    private var porcupine: Porcupine? = null

    override fun initialize(): Boolean {
        val accessKey = accessKeyProvider()
        if (accessKey.isNullOrBlank()) {
            Log.w(TAG, "Porcupine AccessKey not configured in secure storage. Wake engine dormant.")
            return false
        }

        return try {
            porcupine = Porcupine.Builder()
                .setAccessKey(accessKey)
                .setKeyword(Porcupine.BuiltInKeyword.JARVIS)
                .setSensitivity(0.6f)
                .build(context)
            Log.i(TAG, "Porcupine 'JARVIS' wake engine initialized successfully")
            true
        } catch (e: PorcupineException) {
            Log.e(TAG, "Porcupine initialization error: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error initializing Porcupine: ${e.message}", e)
            false
        }
    }

    override fun processFrame(frame: AudioFrame): Boolean {
        val engine = porcupine ?: return false
        return try {
            val keywordIndex = engine.process(frame.data)
            keywordIndex >= 0
        } catch (e: Exception) {
            Log.e(TAG, "Error in Porcupine process(): ${e.message}")
            false
        }
    }

    override fun release() {
        porcupine?.delete()
        porcupine = null
        Log.d(TAG, "Porcupine wake engine released")
    }
}
