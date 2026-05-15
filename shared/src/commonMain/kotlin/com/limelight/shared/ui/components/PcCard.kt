package com.limelight.shared.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopWindows
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
    onPair: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isOnline = computer.isOnline
    
    val statusColor = if (isOnline) MoonlightColors.Secondary else MaterialTheme.colorScheme.onSurfaceVariant
    val containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isOnline) 1f else 0.6f)
    
    val glowBrush = if (isOnline) {
        Brush.linearGradient(
            colors = listOf(MoonlightColors.PrimaryContainer, MoonlightColors.TertiaryContainer)
        )
    } else null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .then(
                if (glowBrush != null) Modifier.border(2.dp, glowBrush, RoundedCornerShape(24.dp))
                else Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            )
            .clickable(enabled = isOnline && computer.isPaired) { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        if (isOnline) statusColor.copy(alpha = 0.1f) 
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (computer.isStreaming) Icons.Default.PlayArrow else Icons.Default.DesktopWindows,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = computer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                val statusText = if (isOnline && !computer.isPaired) "Needs pairing" else computer.statusLabel
                val statusTextColor = if (isOnline && !computer.isPaired) MoonlightColors.Error else statusColor
                
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusTextColor,
                    maxLines = 1
                )
                if (computer.localAddress != null) {
                    Text(
                        text = computer.localAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
            }

            // Buttons / Status
            if (isOnline && !computer.isPaired && onPair != null) {
                Button(
                    onClick = onPair,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MoonlightColors.PrimaryContainer.copy(alpha = 0.2f),
                        contentColor = MoonlightColors.PrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("PAIR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            } else {
                val chipBg = if (isOnline) statusColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                val chipText = if (isOnline) "ONLINE" else "WAKE"
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = chipBg,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .then(
                            if (!isOnline && onWakeOnLan != null) Modifier.clickable { onWakeOnLan() }
                            else Modifier
                        )
                ) {
                    Text(
                        text = chipText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
