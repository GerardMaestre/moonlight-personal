package com.limelight.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.shared.model.ComputerInfo
import com.limelight.shared.ui.theme.MoonlightColors

@Composable
fun PcCard(
    computer: ComputerInfo,
    onClick: () -> Unit,
    onWakeOnLan: (() -> Unit)? = null,
    onPair: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isOnline = computer.isOnline
    val statusColor = when {
        isOnline && !computer.isPaired -> MoonlightColors.Error
        isOnline -> MoonlightColors.TertiaryFixed
        else -> MoonlightColors.Outline
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 176.dp)
            .clickable(enabled = isOnline && computer.isPaired) { onClick() },
        contentPadding = PaddingValues(24.dp)
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            listOf(MoonlightColors.Primary.copy(alpha = if (isOnline) 0.16f else 0.04f), Color.Transparent)
                        )
                    )
            )
            Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isOnline) MoonlightColors.Primary.copy(alpha = 0.13f) else Color.White.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (computer.isStreaming) Icons.Default.PlayArrow else Icons.Default.DesktopWindows,
                                contentDescription = null,
                                tint = if (isOnline) MoonlightColors.Primary else MoonlightColors.Outline,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(computer.name, style = MaterialTheme.typography.headlineMedium, color = if (isOnline) MoonlightColors.OnSurface else MoonlightColors.OnSurfaceVariant, maxLines = 1)
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (isOnline && !computer.isPaired) "Needs pairing" else computer.statusLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = statusColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    if (computer.isStreaming) {
                        StatusPill("Streaming", MoonlightColors.Tertiary, leadingDot = false)
                    }
                }

                Spacer(Modifier.height(26.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    when {
                        isOnline && !computer.isPaired && onPair != null -> PrimaryGlassButton("Pair", Icons.Default.Lock, onPair, Modifier.weight(1f))
                        isOnline -> PrimaryGlassButton("Resume Stream", Icons.Default.PlayArrow, onClick, Modifier.weight(1f))
                        else -> PrimaryGlassButton("Wake PC", Icons.Default.PowerSettingsNew, { onWakeOnLan?.invoke() }, Modifier.weight(1f), enabled = onWakeOnLan != null)
                    }
                }
            }
        }
    }
}
