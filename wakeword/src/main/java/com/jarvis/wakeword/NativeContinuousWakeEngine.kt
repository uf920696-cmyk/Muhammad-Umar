package com.jarvis.wakeword

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * ROCK-SOLID BACKGROUND WAKE ENGINE:
 * Listens for "JARVIS" cleanly.
 * When "JARVIS" is detected, signals callback so system plays single beep and asks "Ji?".
 */
class NativeContinuousWakeEngine(
    private val context: Context,
    private val onWakeWordDetected: (String?) -> Unit
) {
    companion object {
        private const val TAG = "NativeWakeEngine"
        private val HOTWORDS = listOf(
            "hey jarvis",
            "ok jarvis",
            "hello jarvis",
            "hi jarvis",
            "jarvis"
        )
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isRunning = false
    private var isListening = false
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var restartJob: Job? = null

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun start() {
        if (isRunning) return
        isRunning = true
        Log.i(TAG, "Starting wake engine for 'JARVIS' (100% silent)...")
        initAndListen()
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

    private fun initAndListen() {
        scope.launch {
            if (!isRunning) return@launch
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(createListener())
                    }
                }

                silenceBeeps(true)

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("ur-PK", "hi-IN", "en-IN", "en-US", "hi-Latn"))
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra("android.speech.extra.DICTATION_MODE", true)
                    putExtra("android.speech.extra.BEEP_SILENT", true)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
                }

                isListening = true
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting wake engine: ${e.message}")
                silenceBeeps(false)
                scheduleRestart(1500)
            }
        }
    }

    private fun scheduleRestart(delayMs: Long = 1000) {
        if (!isRunning) return
        restartJob?.cancel()
        restartJob = scope.launch {
            delay(delayMs)
            if (isRunning && !isListening) {
                initAndListen()
            }
        }
    }

    private fun checkHotword(text: String): Boolean {
        val clean = text.lowercase().trim()
        val words = clean.split("\\s+".toRegex())
        return words.contains("jarvis") || 
               clean.contains("hey jarvis") || 
               clean.contains("ok jarvis") || 
               clean.contains("hello jarvis") ||
               clean.contains("hi jarvis") ||
               clean.contains("sunno jarvis") ||
               clean.contains("oye jarvis") ||
               clean.contains("start jarvis") ||
               clean.contains("active jarvis")
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
                silenceBeeps(false)
                scheduleRestart(1200)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                silenceBeeps(false)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    for (match in matches) {
                        val clean = match.lowercase().trim()
                        if (checkHotword(clean)) {
                            Log.i(TAG, ">>> ACCURATE 'JARVIS' HEARD: '$match' <<<")
                            val leftoverCommand = HOTWORDS.fold(clean) { acc, hw -> acc.replace(hw, "") }.trim()
                            onWakeWordDetected(if (leftoverCommand.isNotBlank()) leftoverCommand else null)
                            return
                        }
                    }
                }
                scheduleRestart(1000)
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun pause() {
        isRunning = false
        isListening = false
        silenceBeeps(false)
        restartJob?.cancel()
        scope.launch {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {}
        }
    }

    fun stop() {
        isRunning = false
        isListening = false
        silenceBeeps(false)
        restartJob?.cancel()
        scope.launch {
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {}
            speechRecognizer = null
        }
    }
}
