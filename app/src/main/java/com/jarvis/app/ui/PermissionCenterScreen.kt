package com.jarvis.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

enum class PermissionState(val label: String, val color: Color) {
    GRANTED("GRANTED", Color(0xFF00E676)),
    NEEDS_PERMISSION("NEEDS PERMISSION", Color(0xFFFF5252)),
    NEEDS_USER_ACTION("NEEDS USER ACTION", Color(0xFF40C4FF)),
    BLOCKED("BLOCKED", Color(0xFFD50000)),
    UNSUPPORTED("UNSUPPORTED", Color(0xFF757575))
}

data class PermissionItem(
    val name: String,
    val description: String,
    val state: PermissionState,
    val actionLabel: String,
    val onAction: (Context) -> Unit
)

@Composable
fun PermissionCenterScreen(
    onRequestMicrophone: () -> Unit,
    onRequestNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val permissionsList = getPermissionItems(context, onRequestMicrophone, onRequestNotifications)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF09090C))
            .padding(16.dp)
    ) {
        Text(
            text = "SECURITY & PERMISSION CENTER",
            color = Color(0xFFFF2A42),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Text(
            text = "Real runtime access and background execution status",
            color = Color(0xFF888899),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(permissionsList) { item ->
                PermissionCard(item)
            }
        }
    }
}

@Composable
fun PermissionCard(item: PermissionItem) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF22222E), RoundedCornerShape(8.dp))
            .background(Color(0xFF14141B), RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                Text(
                    text = item.state.label,
                    color = item.state.color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.description,
                color = Color(0xFFAAAAAA),
                fontSize = 12.sp
            )

            if (item.state != PermissionState.GRANTED && item.actionLabel.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { item.onAction(context) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222230))
                ) {
                    Text(
                        text = item.actionLabel,
                        color = Color(0xFF40C4FF),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

fun getPermissionItems(
    context: Context,
    onRequestMicrophone: () -> Unit,
    onRequestNotifications: () -> Unit
): List<PermissionItem> {
    val items = mutableListOf<PermissionItem>()

    // 1. Microphone
    val micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    items.add(
        PermissionItem(
            name = "Microphone (RECORD_AUDIO)",
            description = "Mandatory for local wake word engine and Gemini Live streaming.",
            state = if (micGranted) PermissionState.GRANTED else PermissionState.NEEDS_PERMISSION,
            actionLabel = "GRANT MICROPHONE ACCESS",
            onAction = { onRequestMicrophone() }
        )
    )

    // 2. Notifications
    val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else true
    items.add(
        PermissionItem(
            name = "Notifications (POST_NOTIFICATIONS)",
            description = "Required to keep foreground service alive and show real-time status.",
            state = if (notifGranted) PermissionState.GRANTED else PermissionState.NEEDS_PERMISSION,
            actionLabel = "GRANT NOTIFICATION ACCESS",
            onAction = { onRequestNotifications() }
        )
    )

    // 3. Battery Optimization
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val isIgnoringBattery = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    items.add(
        PermissionItem(
            name = "Battery Optimization Whitelist",
            description = "Prevents OEM killers from killing JARVIS while listening.",
            state = if (isIgnoringBattery) PermissionState.GRANTED else PermissionState.NEEDS_USER_ACTION,
            actionLabel = "OPEN BATTERY OPTIMIZATION SETTINGS",
            onAction = { ctx ->
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${ctx.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    ctx.startActivity(intent)
                } catch (e: Exception) {
                    val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    ctx.startActivity(fallbackIntent)
                }
            }
        )
    )

    // 4. Contacts
    val contactsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    items.add(
        PermissionItem(
            name = "Contacts (READ_CONTACTS)",
            description = "Required for resolving Ali or any caller in calls/messages.",
            state = if (contactsGranted) PermissionState.GRANTED else PermissionState.NEEDS_PERMISSION,
            actionLabel = "GRANT CONTACTS PERMISSION",
            onAction = { ctx ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", ctx.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                ctx.startActivity(intent)
            }
        )
    )

    // 5. Phone Calls
    val callGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
    items.add(
        PermissionItem(
            name = "Phone Calls (CALL_PHONE)",
            description = "Allows initiating real phone calls without opening dialer keypad.",
            state = if (callGranted) PermissionState.GRANTED else PermissionState.NEEDS_PERMISSION,
            actionLabel = "GRANT CALL_PHONE PERMISSION",
            onAction = { ctx ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", ctx.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                ctx.startActivity(intent)
            }
        )
    )

    // 6. System Write Settings (Brightness)
    val canWriteSettings = Settings.System.canWrite(context)
    items.add(
        PermissionItem(
            name = "System Settings (WRITE_SETTINGS)",
            description = "Allows adjusting screen brightness directly.",
            state = if (canWriteSettings) PermissionState.GRANTED else PermissionState.NEEDS_USER_ACTION,
            actionLabel = "ALLOW WRITE SETTINGS",
            onAction = { ctx ->
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${ctx.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                ctx.startActivity(intent)
            }
        )
    )

    // 7. Accessibility Service
    items.add(
        PermissionItem(
            name = "Accessibility Service",
            description = "Required for screen inspection, back/home navigation, and UI gestures.",
            state = PermissionState.NEEDS_USER_ACTION,
            actionLabel = "OPEN ACCESSIBILITY SETTINGS",
            onAction = { ctx ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                ctx.startActivity(intent)
            }
        )
    )

    return items
}
