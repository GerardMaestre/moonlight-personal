package com.limelight.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.limelight.shared.model.NetworkProfile
import com.limelight.shared.model.ProfileIcon
import com.limelight.shared.ui.theme.MoonlightColors

@Composable
fun NetworkProfileCard(
    profile: NetworkProfile,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector = when (profile.icon) {
        ProfileIcon.HOME -> Icons.Default.Home
        ProfileIcon.MOBILE_5G -> Icons.Default.NetworkCell
        ProfileIcon.DATA_SAVER -> Icons.Default.SaveAlt
    }

    val borderGradient = if (isSelected) {
        Brush.horizontalGradient(
            listOf(MoonlightColors.Purple, MoonlightColors.Cyan)
        )
    } else {
        Brush.horizontalGradient(
            listOf(MoonlightColors.SurfaceElevated, MoonlightColors.SurfaceElevated)
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(borderGradient)
            .padding(2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MoonlightColors.SurfaceVariant)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = profile.name,
                tint = if (isSelected) MoonlightColors.Purple else MoonlightColors.OnSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MoonlightColors.Purple
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = profile.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MoonlightColors.Purple.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "ACTIVO",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MoonlightColors.Purple,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
