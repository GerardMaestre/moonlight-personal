package com.limelight.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.limelight.shared.ui.screens.AppScreen
import com.limelight.shared.ui.theme.MoonlightColors

@Composable
fun BottomNavBar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MoonlightColors.SurfaceVariant.copy(alpha = 0.30f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(999.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavBarItem(Icons.Default.Home, currentScreen == AppScreen.MAIN_MENU) { onNavigate(AppScreen.MAIN_MENU) }
            NavBarItem(Icons.Default.SportsEsports, currentScreen == AppScreen.MOONLIGHT || currentScreen == AppScreen.GAME_LIST) { onNavigate(AppScreen.MOONLIGHT) }
            NavBarItem(Icons.Default.NetworkCheck, currentScreen == AppScreen.POWER_CONTROL || currentScreen == AppScreen.PHOTO_SERVER) { onNavigate(AppScreen.POWER_CONTROL) }
            NavBarItem(Icons.Default.Settings, false) { onNavigate(AppScreen.POWER_CONTROL) }
        }
    }
}

@Composable
private fun NavBarItem(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (isSelected) MoonlightColors.PrimaryContainer else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MoonlightColors.OnPrimaryContainer else MoonlightColors.OutlineVariant,
            modifier = Modifier.size(26.dp)
        )
    }
}
