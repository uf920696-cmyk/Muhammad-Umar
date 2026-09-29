package com.jarvis.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.jarvis.app.ui.MainActivity
import com.jarvis.core.common.JarvisState
import com.jarvis.device.nlp.CommandProcessor
import com.jarvis.voice.ack.LocalAckPlayer
import com.jarvis.voice.speech.JarvisSpeechRecognizer
import com.jarvis.voice.speech.JarvisTtsEngine
import com.jarvis.wakeword.NativeContinuousWakeEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 24/7 BACKGROUND COMPANION & CONTINUOUS CONVERSATION SERVICE:
 * - 100% Silent: Zero annoying beeps/chimes.
 * - Persistent Active Mode: Stays continuously active and listening until explicitly deactivated!
 * - Works over ANY open app (YouTube, Camera, WhatsApp, Browser, Home Screen).
 * - Full AI Knowledge & Hardware Controls.
 */
class JarvisForegroundService : Service() {

    companion object {
        private const val TAG = "JarvisService"
        private const val CHANNEL_ID = "jarvis_voice_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.jarvis.action.START"
        const val ACTION_STOP = "com.jarvis.action.STOP"
        const val ACTION_ACTIVATE = "com.jarvis.action.ACTIVATE"
        const val ACTION_DEACTIVATE = "com.jarvis.action.DEACTIVATE"
        const val ACTION_TRIGGER_VOICE_COMMAND = "com.jarvis.action.TRIGGER_VOICE_COMMAND"

        private val _systemState = MutableStateFlow<JarvisState>(JarvisState.Idle)
        val systemState: StateFlow<JarvisState> = _systemState.asStateFlow()

        private val _isJarvisActive = MutableStateFlow<Boolean>(true)
        val isJarvisActive: StateFlow<Boolean> = _isJarvisActive.asStateFlow()

        private val _lastSpokenTranscript = MutableStateFlow<String>("")
        val lastSpokenTranscript: StateFlow<String> = _lastSpokenTranscript.asStateFlow()

        private val _lastAssistantReply = MutableStateFlow<String>("")
        val lastAssistantReply: StateFlow<String> = _lastAssistantReply.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, JarvisForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, JarvisForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun activate(context: Context) {
            val intent = Intent(context, JarvisForegroundService::class.java).apply {
                action = ACTION_ACTIVATE
            }
            context.startService(intent)
        }

        fun deactivate(context: Context) {
            val intent = Intent(context, JarvisForegroundService::class.java).apply {
                action = ACTION_DEACTIVATE
            }
            context.startService(intent)
        }

        fun triggerListening(context: Context) {
            val intent = Intent(context, JarvisForegroundService::class.java).apply {
                action = ACTION_TRIGGER_VOICE_COMMAND
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var ackPlayer: LocalAckPlayer
    private var nativeWakeEngine: NativeContinuousWakeEngine? = null

    private lateinit var ttsEngine: JarvisTtsEngine
    private lateinit var speechRecognizer: JarvisSpeechRecognizer
    private lateinit var commandProcessor: CommandProcessor

    override fun onCreate() {
        super.onCreate()
        ackPlayer = LocalAckPlayer(this)
        ttsEngine = JarvisTtsEngine(this)
        commandProcessor = CommandProcessor(this)

        speechRecognizer = JarvisSpeechRecognizer(
            context = this,
            onResult = { query, candidates ->
                onSpeechRecognized(query, candidates)
            },
            onError = { error ->
                Log.w(TAG, "SpeechRecognizer status: $error")
                handleListeningEnded()
            }
        )

        // Native Continuous Zero-Beep Wake Engine for Standby Mode
        nativeWakeEngine = NativeContinuousWakeEngine(this) { directCommand ->
            onWakeDetected(directCommand)
        }

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                shutdown()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_ACTIVATE -> {
                activateJarvis("Manual activate button")
            }
            ACTION_DEACTIVATE -> {
                deactivateJarvis("Jarvis deactivate kar diya gaya hai. Main standby par hoon.")
            }
            ACTION_TRIGGER_VOICE_COMMAND -> {
                activateJarvis("Manual voice trigger")
            }
            ACTION_START, null -> {
                startForegroundWithNotification()
                // By default start in continuous active mode so user can immediately talk
                activateJarvis("Service started")
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification(if (_isJarvisActive.value) "JARVIS ACTIVE — Continuous Listening..." else "JARVIS Standby — 'JARVIS' bolein")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun activateJarvis(source: String, speakPrompt: Boolean = false) {
        Log.i(TAG, "Activating JARVIS (Source: $source) - Will stay active until explicitly deactivated!")
        _isJarvisActive.value = true
        nativeWakeEngine?.pause()
        updateNotification("JARVIS ACTIVE — Boliye, sun rahi hoon...")

        if (speakPrompt) {
            ttsEngine.speak("Ji, boliye?") {
                serviceScope.launch {
                    delay(150)
                    triggerVoiceInput()
                }
            }
        } else {
            triggerVoiceInput()
        }
    }

    private fun deactivateJarvis(goodbyeText: String) {
        Log.i(TAG, "Explicitly deactivating JARVIS -> Switching to Standby")
        _isJarvisActive.value = false
        speechRecognizer.stopListening()
        _systemState.value = JarvisState.LocalWake
        updateNotification("JARVIS Standby (Deactivated) — 'JARVIS' bolein ya Activate karein")

        ttsEngine.speak(goodbyeText) {
            resumeStandbyWakeListener()
        }
    }

    private fun onWakeDetected(directCommand: String?) {
        Log.i(TAG, "Wake word detected! directCommand='$directCommand'")
        ackPlayer.playAck() // 100% silent per requirement

        if (!directCommand.isNullOrBlank()) {
            _isJarvisActive.value = true
            nativeWakeEngine?.pause()
            onSpeechRecognized(directCommand, listOf(directCommand))
        } else {
            activateJarvis("Wake word detection", speakPrompt = true)
        }
    }

    /**
     * Starts listening for user's command
     */
    private fun triggerVoiceInput() {
        if (!_isJarvisActive.value) return
        serviceScope.launch {
            Log.i(TAG, "Continuous active listening window opened")
            _systemState.value = JarvisState.Listening(System.currentTimeMillis())
            updateNotification("JARVIS ACTIVE — Sun rahi hoon...")

            nativeWakeEngine?.pause()
            delay(100)
            speechRecognizer.startListening()
        }
    }

    /**
     * Handles recognized text, executes action, and CONTINUES listening without deactivating!
     */
    private fun onSpeechRecognized(text: String, candidates: List<String>) {
        val clean = text.trim().lowercase()

        // Re-wake words in active mode
        if (clean == "jarvis" || clean == "hey jarvis") {
            ttsEngine.speak("Ji, boliye?") {
                serviceScope.launch {
                    delay(100)
                    triggerVoiceInput()
                }
            }
            return
        }

        _lastSpokenTranscript.value = text
        _systemState.value = JarvisState.Thinking(querySummary = text)
        updateNotification("AMAL HO RAHA HAI: '$text'")

        serviceScope.launch {
            val result = commandProcessor.processCommand(text, candidates)
            _lastAssistantReply.value = result.responseSpeech
            _systemState.value = JarvisState.Speaking()
            updateNotification("JARVIS: ${result.responseSpeech}")

            if (result.isDeactivateRequest) {
                deactivateJarvis(result.responseSpeech)
                return@launch
            }

            // Speak response and automatically continue listening (Continuous Active Loop)
            ttsEngine.speak(result.responseSpeech) {
                if (_isJarvisActive.value) {
                    serviceScope.launch {
                        delay(250)
                        triggerVoiceInput()
                    }
                } else {
                    resumeStandbyWakeListener()
                }
            }
        }
    }

    private fun handleListeningEnded() {
        if (_isJarvisActive.value) {
            // EXACT USER REQUIREMENT:
            // "ek dafa activate krne ke bad yeh activate hi rhe jab tk isko deactivate na kiya jaye"
            // Re-arm immediately, NO timeout deactivation!
            serviceScope.launch {
                delay(400)
                if (_isJarvisActive.value) {
                    triggerVoiceInput()
                }
            }
        } else {
            resumeStandbyWakeListener()
        }
    }

    private fun resumeStandbyWakeListener() {
        serviceScope.launch {
            delay(300)
            _systemState.value = JarvisState.LocalWake
            updateNotification("JARVIS Standby — 'JARVIS' bolein ya Activate dabayein")
            nativeWakeEngine?.start()
        }
    }

    private fun shutdown() {
        _isJarvisActive.value = false
        nativeWakeEngine?.stop()
        speechRecognizer.release()
        ttsEngine.release()
        ackPlayer.release()
        _systemState.value = JarvisState.Idle
    }

    override fun onDestroy() {
        shutdown()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JARVIS Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "JARVIS 24/7 Continuous Hands-Free Assistant"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Action button to Toggle Activate/Deactivate directly from the notification
        val toggleActionIntent = Intent(this, JarvisForegroundService::class.java).apply {
            action = if (_isJarvisActive.value) ACTION_DEACTIVATE else ACTION_ACTIVATE
        }
        val pendingToggleIntent = PendingIntent.getService(
            this,
            1,
            toggleActionIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val actionTitle = if (_isJarvisActive.value) "DEACTIVATE JARVIS" else "ACTIVATE JARVIS"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("J.A.R.V.I.S. Assistant ${if (_isJarvisActive.value) "(ACTIVE)" else "(STANDBY)"}")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_play, actionTitle, pendingToggleIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(statusText))
    }
}
