package com.jarvis.app.ui

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class CapabilityState(val label: String, val color: Color) {
    AVAILABLE("AVAILABLE", Color(0xFF00E676)),
    LIMITED_BY_ANDROID("LIMITED BY ANDROID", Color(0xFFFFB300)),
    BEST_EFFORT("BEST-EFFORT", Color(0xFFFF9100)),
    NEEDS_PERMISSION("NEEDS PERMISSION", Color(0xFFFF5252)),
    NEEDS_USER_ACTION("NEEDS USER ACTION", Color(0xFF40C4FF)),
    NOT_IMPLEMENTED("NOT IMPLEMENTED", Color(0xFF757575)),
    UNAVAILABLE_ON_THIS_DEVICE("UNAVAILABLE ON THIS DEVICE", Color(0xFFD50000))
}

data class CapabilityItem(
    val name: String,
    val description: String,
    val state: CapabilityState,
    val note: String = ""
)

@Composable
fun CapabilityStatusScreen(
    capabilities: List<CapabilityItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D11))
            .padding(16.dp)
    ) {
        Text(
            text = "SYSTEM CAPABILITY STATUS",
            color = Color(0xFFFF2A42),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Text(
            text = "Real hardware and OS access integrity matrix",
            color = Color(0xFF888899),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(capabilities) { item ->
                CapabilityCard(item)
            }
        }
    }
}

@Composable
fun CapabilityCard(item: CapabilityItem) {
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
                    fontSize = 15.sp
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
                fontSize = 13.sp
            )

            if (item.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.note,
                    color = Color(0xFF707088),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
