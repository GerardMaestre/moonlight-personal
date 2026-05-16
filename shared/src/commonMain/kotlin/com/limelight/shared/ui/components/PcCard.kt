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
    val iconBackground = if (isOnline) MoonlightColors.Primary.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f)
    
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = isOnline && computer.isPaired) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (computer.isStreaming) Icons.Default.PlayArrow else Icons.Default.DesktopWindows,
                    contentDescription = null,
                    tint = if (isOnline) MoonlightColors.Primary else statusColor,
                    modifier = Modifier.size(28.dp)
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
                    color = MoonlightColors.OnSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOnline && !computer.isPaired) "Needs pairing" else computer.statusLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isOnline && !computer.isPaired) MoonlightColors.Error else statusColor,
                        maxLines = 1
                    )
                }
            }

            // Action Button
            if (isOnline && !computer.isPaired && onPair != null) {
                Button(
                    onClick = onPair,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MoonlightColors.Primary,
                        contentColor = MoonlightColors.OnPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("PAIR", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                IconButton(
                    onClick = { if (!isOnline) onWakeOnLan?.invoke() else onClick() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    Icon(
                        imageVector = if (isOnline) Icons.Default.PlayArrow else Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = if (isOnline) MoonlightColors.Primary else MoonlightColors.OnSurfaceVariant
                    )
                }
            }
        }
    }
}
