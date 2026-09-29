package com.jarvis.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.core.common.JarvisState

@Composable
fun HUDContent(
    state: JarvisState,
    isJarvisActive: Boolean,
    lastTranscript: String,
    lastReply: String,
    isFlashlightOn: Boolean,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onTriggerVoiceCommand: () -> Unit,
    onToggleTorch: () -> Unit,
    onTestAppLaunch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF09090C))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Continuous Active Mode Indicator & Toggle Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = if (isJarvisActive) Color(0xFF00E676) else Color(0xFFFF9100),
                    shape = RoundedCornerShape(10.dp)
                )
                .background(if (isJarvisActive) Color(0xFF0A2416) else Color(0xFF26190A), RoundedCornerShape(10.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(if (isJarvisActive) Color(0xFF00E676) else Color(0xFFFF9100), CircleShape)
                    )
                    Text(
                        text = if (isJarvisActive) "JARVIS IS CONTINUOUSLY ACTIVE" else "JARVIS IS IN STANDBY (DEACTIVATED)",
                        color = if (isJarvisActive) Color(0xFF00E676) else Color(0xFFFFB74D),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    )
                }

                Text(
                    text = if (isJarvisActive) 
                        "Active hands-free listening enabled. Speak any command without repeating wake words! (To stop, say 'Deactivate' or tap below)" 
                        else "Standing by silently. Say 'JARVIS' or tap 'Activate' to start continuous conversation mode.",
                    color = Color(0xFFCCCCCC),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Button(
                    onClick = if (isJarvisActive) onDeactivate else onActivate,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isJarvisActive) Color(0xFFD32F2F) else Color(0xFF00E676)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isJarvisActive) "DEACTIVATE JARVIS (Switch to Standby)" else "ACTIVATE JARVIS (Continuous Hands-Free)",
                        color = if (isJarvisActive) Color.White else Color.Black,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Status Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF22222E), RoundedCornerShape(8.dp))
                .background(Color(0xFF14141B), RoundedCornerShape(8.dp))
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "HARDWARE & VOICE ENGINE STATE",
                    color = Color(0xFF888899),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (state) {
                        is JarvisState.Idle -> "IDLE (START SERVICE TO ACTIVATE)"
                        is JarvisState.LocalWake -> if (isJarvisActive) "CONTINUOUS ACTIVE LISTENING" else "STANDBY (Say 'Jarvis' to activate)"
                        is JarvisState.Listening -> "LISTENING TO VOICE (Noise-Filtered)..."
                        is JarvisState.Thinking -> "PROCESSING COMMAND & INTENT..."
                        is JarvisState.Speaking -> "JARVIS SPEAKING REPLY..."
                        is JarvisState.Error -> "ERROR: ${state.reason}"
                        else -> state.javaClass.simpleName.uppercase()
                    },
                    color = when (state) {
                        is JarvisState.Listening -> Color(0xFF00E676)
                        is JarvisState.LocalWake -> if (isJarvisActive) Color(0xFF00E676) else Color(0xFF40C4FF)
                        is JarvisState.Thinking -> Color(0xFFFF9100)
                        is JarvisState.Speaking -> Color(0xFFFFD600)
                        is JarvisState.Error -> Color(0xFFFF5252)
                        else -> Color(0xFFAAAAAA)
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Live Voice & Conversation Log Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF22222E), RoundedCornerShape(8.dp))
                .background(Color(0xFF14141B), RoundedCornerShape(8.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "VOICE CONVERSATION // TRANSCRIPT",
                    color = Color(0xFFFF2A42),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = if (lastTranscript.isNotBlank()) "YOU: \"$lastTranscript\"" else "YOU: (Say 'Turn on flashlight', 'Call Ali', etc.)",
                    color = Color(0xFF00E676),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = if (lastReply.isNotBlank()) "JARVIS: \"$lastReply\"" else "JARVIS: \"Standing by for your command, Sir.\"",
                    color = Color(0xFF40C4FF),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Big Push-To-Talk Voice Command Button
        Button(
            onClick = onTriggerVoiceCommand,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2A42)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "TAP TO SPEAK COMMAND",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        // Service Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onStartListening,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("START SERVICE", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }

            Button(
                onClick = onStopListening,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333344)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("STOP SERVICE", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }

        // Quick Hardware Actions
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF22222E), RoundedCornerShape(8.dp))
                .background(Color(0xFF14141B), RoundedCornerShape(8.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "HARDWARE TOOLS MANUAL TEST",
                    color = Color(0xFF888899),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onToggleTorch,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFlashlightOn) Color(0xFFFFD600) else Color(0xFF222230)
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isFlashlightOn) "TORCH ON" else "TORCH OFF",
                            color = if (isFlashlightOn) Color.Black else Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = { onTestAppLaunch("com.google.android.youtube") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222230)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "OPEN YOUTUBE",
                            color = Color(0xFF40C4FF),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
