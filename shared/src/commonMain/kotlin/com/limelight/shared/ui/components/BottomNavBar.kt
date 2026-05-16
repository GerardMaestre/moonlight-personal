package com.limelight.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.NetworkCheck
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp),
        color = MoonlightColors.Background.copy(alpha = 0.8f),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Glassy Border Top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.05f))
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavBarItem(
                    icon = Icons.Default.Dashboard,
                    isSelected = currentScreen == AppScreen.MAIN_MENU,
                    onClick = { onNavigate(AppScreen.MAIN_MENU) }
                )
                NavBarItem(
                    icon = Icons.Default.NetworkCheck,
                    isSelected = currentScreen == AppScreen.POWER_CONTROL,
                    onClick = { onNavigate(AppScreen.POWER_CONTROL) }
                )
                NavBarItem(
                    icon = Icons.Default.SportsEsports,
                    isSelected = currentScreen == AppScreen.MOONLIGHT || currentScreen == AppScreen.GAME_LIST,
                    onClick = { onNavigate(AppScreen.MOONLIGHT) }
                )
                NavBarItem(
                    icon = Icons.Default.Settings,
                    isSelected = false,
                    onClick = { /* Settings */ }
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (isSelected) MoonlightColors.OnPrimaryContainer else MoonlightColors.OnSurfaceVariant
    val backgroundColor = if (isSelected) MoonlightColors.PrimaryContainer else Color.Transparent
    
    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.shadow(8.dp, RoundedCornerShape(20.dp), spotColor = MoonlightColors.PrimaryContainer)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
    }
}
