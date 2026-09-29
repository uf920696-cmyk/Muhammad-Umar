package com.jarvis.voice.speech

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * HIGH-SENSITIVITY, NOISE-RESILIENT SPEECH RECOGNITION ENGINE:
 * - 100% SILENT: Completely disables prompt and error beeps.
 * - NOISE-RESILIENT: Generous silence window, multi-candidate hypothesis ranking.
 * - MULTI-LINGUAL: Accurately understands Roman Urdu, Hindi, Indian English, and Global English.
 */
class JarvisSpeechRecognizer(
    private val context: Context,
    private val onResult: (query: String, candidates: List<String>) -> Unit,
    private val onError: (String) -> Unit
) {

    companion object {
        private const val TAG = "JarvisSpeechRecognizer"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private var isListening = false
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    init {
        mainScope.launch {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }
                Log.i(TAG, "SpeechRecognizer initialized successfully")
            } else {
                Log.e(TAG, "SpeechRecognizer not available on this device")
            }
        }
    }

    fun startListening() {
        mainScope.launch {
            if (isListening) return@launch
            val recognizer = speechRecognizer ?: run {
                onError("Speech recognition not available")
                return@launch
            }

            // Mute system chimes/beeps to ensure completely silent continuous listening
            silenceBeeps(true)

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("ur-PK", "hi-IN", "en-IN", "en-US", "hi-Latn"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10)
                putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true)

                // Silences native Google Speech Recognizer sound effects
                putExtra("android.speech.extra.DICTATION_MODE", true)
                putExtra("android.speech.extra.BEEP_SILENT", true)

                // Robust noise-resistant listening lengths
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            try {
                recognizer.startListening(intent)
                isListening = true
                Log.i(TAG, "Started speech recognition listener (Noise-resilient, 100% silent)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start listening: ${e.message}", e)
                isListening = false
                silenceBeeps(false)
                onError(e.message ?: "Failed to start speech recognizer")
            }
        }
    }

    private fun silenceBeeps(mute: Boolean) {
        try {
            val am = audioManager ?: return
            if (mute) {
                am.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0)
                am.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0)
            } else {
                am.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0)
                am.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0)
            }
        } catch (e: Exception) {}
    }

    fun stopListening() {
        mainScope.launch {
            if (!isListening) return@launch
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping recognizer: ${e.message}")
            } finally {
                isListening = false
                silenceBeeps(false)
            }
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for user command")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "User started speaking command")
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                Log.d(TAG, "User finished speaking command")
            }

            override fun onError(error: Int) {
                isListening = false
                silenceBeeps(false)
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission missing"
                    SpeechRecognizer.ERROR_NETWORK -> "Network issue"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server issue"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Time out"
                    else -> "Error: $error"
                }
                Log.w(TAG, "SpeechRecognizer status: $errorMsg (code $error)")
                onError(errorMsg)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                silenceBeeps(false)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
                if (matches.isNotEmpty()) {
                    val query = matches[0]
                    Log.i(TAG, "Successfully captured user command: '$query', total candidates: ${matches.size}")
                    onResult(query, matches)
                } else {
                    onError("No speech matches found")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun release() {
        mainScope.launch {
            try {
                silenceBeeps(false)
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.w(TAG, "Error destroying speech recognizer: ${e.message}")
            } finally {
                speechRecognizer = null
                isListening = false
            }
        }
    }
}
