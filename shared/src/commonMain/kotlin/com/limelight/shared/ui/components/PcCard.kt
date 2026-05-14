package com.limelight.shared.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.limelight.shared.model.ComputerInfo
import com.limelight.shared.model.ComputerStatus
import com.limelight.shared.ui.theme.MoonlightColors

@Composable
fun PcCard(
    computer: ComputerInfo,
    onClick: () -> Unit,
    onWakeOnLan: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = when (computer.status) {
            ComputerStatus.ONLINE -> MoonlightColors.Green
            ComputerStatus.OFFLINE -> MoonlightColors.Red
            ComputerStatus.UNKNOWN -> MoonlightColors.Outline
        },
        animationSpec = tween(500)
    )

    val glowAlpha = if (computer.isOnline) 0.08f else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = computer.isOnline) { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MoonlightColors.SurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (computer.isOnline) 8.dp else 2.dp
        )
    ) {
        Box {
            // Subtle gradient glow for online PCs
            if (computer.isOnline) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MoonlightColors.Purple.copy(alpha = 0.6f),
                                    MoonlightColors.Cyan.copy(alpha = 0.6f)
                                )
                            )
                        )
                )
            }

            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator circle with icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (computer.isStreaming) Icons.Default.PlayArrow
                        else Icons.Default.Monitor,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = computer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = computer.statusLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor.copy(alpha = 0.9f)
                    )
                    if (computer.localAddress != null) {
                        Text(
                            text = computer.localAddress,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                // Status badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.12f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = when (computer.status) {
                            ComputerStatus.ONLINE -> "ONLINE"
                            ComputerStatus.OFFLINE -> "OFFLINE"
                            ComputerStatus.UNKNOWN -> "..."
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                // WOL button for offline PCs
                if (!computer.isOnline && onWakeOnLan != null && computer.macAddress != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onWakeOnLan) {
                        Icon(
                            Icons.Default.PowerSettingsNew,
                            contentDescription = "Wake on LAN",
                            tint = MoonlightColors.Amber
                        )
                    }
                }
            }
        }
    }
}
