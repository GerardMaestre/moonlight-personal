package com.limelight.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.shared.ui.screens.AppScreen
import com.limelight.shared.ui.theme.MoonlightColors

@Composable
fun BottomNavBar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit
) {
    // Only show on mobile / narrow screens typically, but for simplicity we render it.
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavBarItem(
                icon = Icons.Default.Dashboard,
                label = "Control",
                isSelected = currentScreen == AppScreen.MAIN_MENU,
                onClick = { onNavigate(AppScreen.MAIN_MENU) },
                modifier = Modifier.weight(1f)
            )
            NavBarItem(
                icon = Icons.Default.SportsEsports,
                label = "Gaming",
                isSelected = currentScreen == AppScreen.MOONLIGHT || currentScreen == AppScreen.GAME_LIST,
                onClick = { onNavigate(AppScreen.MOONLIGHT) },
                modifier = Modifier.weight(1f)
            )
            NavBarItem(
                icon = Icons.Default.PowerSettingsNew,
                label = "Power",
                isSelected = currentScreen == AppScreen.POWER_CONTROL,
                onClick = { onNavigate(AppScreen.POWER_CONTROL) },
                modifier = Modifier.weight(1f)
            )
            NavBarItem(
                icon = Icons.Default.PhotoLibrary,
                label = "Media",
                isSelected = currentScreen == AppScreen.PHOTO_SERVER,
                onClick = { onNavigate(AppScreen.PHOTO_SERVER) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NavBarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val contentColor = if (isSelected) MoonlightColors.Secondary else MaterialTheme.colorScheme.onSurfaceVariant
    val backgroundColor = if (isSelected) MoonlightColors.Secondary.copy(alpha = 0.1f) else Color.Transparent

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}
