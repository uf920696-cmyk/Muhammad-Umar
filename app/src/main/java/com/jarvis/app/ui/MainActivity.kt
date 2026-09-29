package com.jarvis.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.jarvis.app.service.JarvisForegroundService
import com.jarvis.core.common.JarvisState
import com.jarvis.core.security.SecureStorageManager
import com.jarvis.device.apps.AppLauncher
import com.jarvis.device.nlp.CommandProcessor
import com.jarvis.device.torch.TorchController
import com.jarvis.voice.speech.JarvisTtsEngine
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val requestAudioLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            JarvisForegroundService.startService(this)
        } else {
            Toast.makeText(this, "Microphone permission is required for JARVIS", Toast.LENGTH_LONG).show()
        }
    }

    private val requestNotificationsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Handled */ }

    // Google Voice Input Launcher (Rock solid on ALL Android phones)
    private val speechInputLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!spokenMatches.isNullOrEmpty()) {
                val commandText = spokenMatches[0]
                executeSpokenCommand(commandText)
            }
        }
    }

    private lateinit var torchController: TorchController
    private lateinit var appLauncher: AppLauncher
    private lateinit var secureStorageManager: SecureStorageManager
    private lateinit var commandProcessor: CommandProcessor
    private lateinit var ttsEngine: JarvisTtsEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        torchController = TorchController(this)
        appLauncher = AppLauncher(this)
        secureStorageManager = SecureStorageManager(this)
        commandProcessor = CommandProcessor(this)
        ttsEngine = JarvisTtsEngine(this)

        // Automatically start 24/7 background service on launch if permission allowed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            JarvisForegroundService.startService(this)
        } else {
            requestAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            JarvisAppRoot(
                secureStorageManager = secureStorageManager,
                onActivate = { JarvisForegroundService.activate(this) },
                onDeactivate = { JarvisForegroundService.deactivate(this) },
                onStartListening = { checkAndStartService() },
                onStopListening = { JarvisForegroundService.stopService(this) },
                onTriggerVoiceCommand = {
                    openDirectGoogleSpeechInput()
                },
                onToggleTorch = { enabled ->
                    lifecycleScope.launch {
                        torchController.setTorch(enabled)
                    }
                },
                onTestAppLaunch = { pkg ->
                    appLauncher.launchAppByPackage(pkg)
                },
                onRequestMicrophone = {
                    requestAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onRequestNotifications = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestNotificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
        }
    }

    private fun checkAndStartService() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            JarvisForegroundService.startService(this)
        }
    }

    /**
     * Rock-Solid Direct Voice Input Dialog:
     * Never times out abruptly. Opens Google's standard speech recognizer UI.
     */
    private fun openDirectGoogleSpeechInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "JARVIS is listening... Speak your command")
        }
        try {
            speechInputLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Speech recognition error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Executes the spoken command immediately on device and speaks back!
     */
    private fun executeSpokenCommand(commandText: String) {
        lifecycleScope.launch {
            Toast.makeText(this@MainActivity, "Heard: $commandText", Toast.LENGTH_SHORT).show()
            val result = commandProcessor.processCommand(commandText)
            Toast.makeText(this@MainActivity, "JARVIS: ${result.responseSpeech}", Toast.LENGTH_LONG).show()
            ttsEngine.speak(result.responseSpeech)
        }
    }

    override fun onDestroy() {
        ttsEngine.release()
        super.onDestroy()
    }
}

@Composable
fun JarvisAppRoot(
    secureStorageManager: SecureStorageManager,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onTriggerVoiceCommand: () -> Unit,
    onToggleTorch: (Boolean) -> Unit,
    onTestAppLaunch: (String) -> Unit,
    onRequestMicrophone: () -> Unit,
    onRequestNotifications: () -> Unit
) {
    val systemState by JarvisForegroundService.systemState.collectAsState()
    val isJarvisActive by JarvisForegroundService.isJarvisActive.collectAsState()
    val lastTranscript by JarvisForegroundService.lastSpokenTranscript.collectAsState()
    val lastReply by JarvisForegroundService.lastAssistantReply.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var isFlashlightOn by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090C))
    ) {
        // Futuristic Arc Reactor HUD Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111116))
                .border(width = 1.dp, color = Color(0xFF22222B), shape = RoundedCornerShape(0.dp))
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "J.A.R.V.I.S.",
                        color = Color(0xFFFF2A42),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 3.sp
                    )
                    Text(
                        text = "CORE HUD // FULL NATIVE VOICE AI",
                        color = Color(0xFF888899),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Reactor Status Dot
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(
                            when {
                                !isJarvisActive -> Color(0xFFFF9100)
                                systemState is JarvisState.Listening -> Color(0xFF00E676)
                                systemState is JarvisState.LocalWake -> Color(0xFF00E676)
                                systemState is JarvisState.Speaking -> Color(0xFFFFD600)
                                systemState is JarvisState.Error -> Color(0xFFFF5252)
                                else -> Color(0xFF00E676)
                            },
                            CircleShape
                        )
                )
            }
        }

        // 4 Primary Navigation Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF111116),
            contentColor = Color(0xFFFF2A42)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("HUD", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("PERMISSIONS", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("CAPABILITIES", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("SETTINGS", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }

        when (selectedTab) {
            0 -> {
                HUDContent(
                    state = systemState,
                    isJarvisActive = isJarvisActive,
                    lastTranscript = lastTranscript,
                    lastReply = lastReply,
                    isFlashlightOn = isFlashlightOn,
                    onActivate = onActivate,
                    onDeactivate = onDeactivate,
                    onStartListening = onStartListening,
                    onStopListening = onStopListening,
                    onTriggerVoiceCommand = onTriggerVoiceCommand,
                    onToggleTorch = {
                        isFlashlightOn = !isFlashlightOn
                        onToggleTorch(isFlashlightOn)
                    },
                    onTestAppLaunch = onTestAppLaunch
                )
            }
            1 -> {
                PermissionCenterScreen(
                    onRequestMicrophone = onRequestMicrophone,
                    onRequestNotifications = onRequestNotifications
                )
            }
            2 -> {
                CapabilityStatusScreen(
                    capabilities = listOf(
                        CapabilityItem(
                            name = "Voice Recognition (STT)",
                            description = "Google Speech Dialog & Native Background engine",
                            state = CapabilityState.AVAILABLE,
                            note = "English & Urdu commands supported"
                        ),
                        CapabilityItem(
                            name = "Text-to-Speech (TTS)",
                            description = "Native vocal feedback engine speaking JARVIS confirmation replies",
                            state = CapabilityState.AVAILABLE
                        ),
                        CapabilityItem(
                            name = "Torch Hardware Control",
                            description = "Voice control: 'Turn on flashlight' / 'Turn off flashlight'",
                            state = CapabilityState.AVAILABLE
                        ),
                        CapabilityItem(
                            name = "Volume Management",
                            description = "Voice control: 'Full volume', 'Mute volume', 'Increase volume'",
                            state = CapabilityState.AVAILABLE
                        ),
                        CapabilityItem(
                            name = "App Launcher",
                            description = "Voice control: 'Open YouTube', 'Open WhatsApp', 'Open Camera'",
                            state = CapabilityState.AVAILABLE
                        ),
                        CapabilityItem(
                            name = "WhatsApp Calling",
                            description = "Voice control: 'Call Ali on WhatsApp' or 'Video call Ali'",
                            state = CapabilityState.BEST_EFFORT
                        )
                    )
                )
            }
            3 -> {
                SettingsScreen(secureStorageManager = secureStorageManager)
            }
        }
    }
}
