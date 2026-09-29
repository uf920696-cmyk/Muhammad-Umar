package com.jarvis.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.core.security.SecureStorageManager

@Composable
fun SettingsScreen(
    secureStorageManager: SecureStorageManager,
    modifier: Modifier = Modifier
) {
    var accessKey by remember { mutableStateOf(secureStorageManager.getPicovoiceAccessKey() ?: "") }
    var idleTimeout by remember { mutableStateOf(secureStorageManager.getLiveIdleTimeoutSec().toString()) }
    var memoryEnabled by remember { mutableStateOf(secureStorageManager.isMemoryEnabled()) }
    var backendUrl by remember { mutableStateOf(secureStorageManager.getBackendUrl()) }
    var geminiApiKey by remember { mutableStateOf(secureStorageManager.getGeminiApiKey() ?: "") }
    var aiModel by remember { mutableStateOf(secureStorageManager.getAiModel()) }
    var savedMessage by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF09090C))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "HARDWARE & SECURITY SETTINGS",
            color = Color(0xFFFF2A42),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Text(
            text = "All secrets encrypted using Android Keystore (AES-256)",
            color = Color(0xFF888899),
            fontSize = 12.sp
        )

        // Picovoice AccessKey Input Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF22222E), RoundedCornerShape(8.dp))
                .background(Color(0xFF14141B), RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Picovoice Porcupine AccessKey",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Required for on-device local wake word detection ('Jarvis'). Stored in EncryptedSharedPreferences.",
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = accessKey,
                    onValueChange = { accessKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter your Picovoice AccessKey", color = Color(0xFF555566), fontSize = 12.sp) },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF2A42),
                        unfocusedBorderColor = Color(0xFF333344)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            secureStorageManager.setPicovoiceAccessKey(accessKey)
                            savedMessage = "Picovoice key saved securely!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("SAVE KEY", color = Color.Black, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            secureStorageManager.clearPicovoiceAccessKey()
                            accessKey = ""
                            savedMessage = "Picovoice key cleared."
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("CLEAR", color = Color(0xFFFF5252), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
            }
        }

        // Live Session Idle Timeout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF22222E), RoundedCornerShape(8.dp))
                .background(Color(0xFF14141B), RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Live Session Idle Timeout (Seconds)",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Duration of silence before closing Live cloud connection and reverting to local wake mode (10s - 60s).",
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = idleTimeout,
                    onValueChange = {
                        idleTimeout = it
                        it.toIntOrNull()?.let { sec ->
                            secureStorageManager.setLiveIdleTimeoutSec(sec)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF2A42),
                        unfocusedBorderColor = Color(0xFF333344)
                    )
                )
            }
        }

        // AI Brain Backend URL
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF22222E), RoundedCornerShape(8.dp))
                .background(Color(0xFF14141B), RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "AI Brain Backend Server Endpoint",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Base URL of the JARVIS backend proxy (/api/chat). By default points to the live cloud dev service.",
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = backendUrl,
                    onValueChange = {
                        backendUrl = it
                        secureStorageManager.setBackendUrl(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://ais-dev-... / http://10.0.2.2:3000", color = Color(0xFF555566), fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF2A42),
                        unfocusedBorderColor = Color(0xFF333344)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            secureStorageManager.setBackendUrl(backendUrl)
                            savedMessage = "Backend URL updated!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("SAVE URL", color = Color.Black, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            backendUrl = SecureStorageManager.DEFAULT_BACKEND_URL
                            secureStorageManager.setBackendUrl(backendUrl)
                            savedMessage = "Backend URL reset to default."
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("RESET DEFAULT", color = Color(0xFF00E5FF), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
            }
        }

        // Direct Gemini API Key (Optional Fallback)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF22222E), RoundedCornerShape(8.dp))
                .background(Color(0xFF14141B), RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Direct Gemini API Key (Optional)",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Optional direct Gemini API Key fallback. If provided, the app can query Gemini cloud directly if the backend proxy is unreachable.",
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = geminiApiKey,
                    onValueChange = { geminiApiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("AIzaSy... (optional direct key)", color = Color(0xFF555566), fontSize = 12.sp) },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF2A42),
                        unfocusedBorderColor = Color(0xFF333344)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            secureStorageManager.setGeminiApiKey(geminiApiKey)
                            savedMessage = "Direct Gemini API Key saved!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("SAVE API KEY", color = Color.Black, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            secureStorageManager.clearGeminiApiKey()
                            geminiApiKey = ""
                            savedMessage = "Direct Gemini key cleared."
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("CLEAR", color = Color(0xFFFF5252), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
            }
        }

        // Optional Memory Toggle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF22222E), RoundedCornerShape(8.dp))
                .background(Color(0xFF14141B), RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Encrypted Local Memory",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Store local contextual memory across sessions. Disabled by default.",
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp
                    )
                }

                Switch(
                    checked = memoryEnabled,
                    onCheckedChange = {
                        memoryEnabled = it
                        secureStorageManager.setMemoryEnabled(it)
                    }
                )
            }
        }

        if (savedMessage.isNotBlank()) {
            Text(
                text = savedMessage,
                color = Color(0xFF00E676),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
