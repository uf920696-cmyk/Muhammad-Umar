package com.jarvis.voice.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

/**
 * NATURAL HUMAN FEMALE VOICE ENGINE (NO ROBOTIC PITCH):
 * Calibrated for realistic human female conversation in Urdu, Hindi, and English.
 * Uses warm natural pitch (1.05f) and comfortable pacing so she sounds like a real person talking to you.
 */
class JarvisTtsEngine(private val context: Context) {

    companion object {
        private const val TAG = "JarvisTtsEngine"
    }

    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Set language to Indian English / Hindi / Urdu support for realistic pronunciation
                val locale = Locale("hi", "IN")
                val result = tts?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale("en", "IN")
                }

                // Auto-detect realistic human female voice
                try {
                    val voices = tts?.voices
                    val bestFemaleVoice = voices?.firstOrNull { voice ->
                        val name = voice.name.lowercase()
                        (name.contains("female") || name.contains("fem") || name.contains("#female") || name.contains("woman")) &&
                                (voice.locale.language == "hi" || voice.locale.language == "ur" || voice.locale.country == "IN")
                    } ?: voices?.firstOrNull { voice ->
                        val name = voice.name.lowercase()
                        name.contains("female") || name.contains("woman")
                    } ?: voices?.firstOrNull { voice ->
                        voice.locale.language == "hi" || voice.locale.country == "IN"
                    }

                    if (bestFemaleVoice != null) {
                        tts?.voice = bestFemaleVoice
                        Log.i(TAG, "Selected real human female voice: ${bestFemaleVoice.name}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Voice selection: ${e.message}")
                }

                // Natural human conversational pitch & speed:
                // 1.05f is natural human female pitch (not cartoonish/robotic)
                // 1.0f is relaxed natural speaking speed
                tts?.setPitch(1.05f)
                tts?.setSpeechRate(1.0f)
                isReady = true
                Log.i(TAG, "Jarvis Natural Human TTS initialized successfully")
            } else {
                Log.e(TAG, "TextToSpeech init failed with status: $status")
            }
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!isReady || tts == null) {
            Log.w(TAG, "TTS not ready yet: $text")
            onDone?.invoke()
            return
        }

        val utteranceId = "jarvis_utt_${System.currentTimeMillis()}"
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(uttId: String?) {}
            override fun onDone(uttId: String?) {
                if (uttId == utteranceId) onDone?.invoke()
            }
            override fun onError(uttId: String?) {
                if (uttId == utteranceId) onDone?.invoke()
            }
        })

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
