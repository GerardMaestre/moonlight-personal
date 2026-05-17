package com.limelight.shared.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.shared.ui.screens.AppScreen
import com.limelight.shared.ui.theme.MoonlightColors
import com.limelight.shared.platform.PhotoServerState

@Composable
fun BottomNavBar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    photoServerState: PhotoServerState? = null,
    onRefreshImmich: (() -> Unit)? = null
) {
    val isVisible = photoServerState?.activeDetailAssetId == null

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it * 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 14.dp)
    ) {
        val darkObsidianGlass = Color(0xFF0A0C10).copy(alpha = 0.92f)
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(darkObsidianGlass)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.16f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(8.dp)
                .animateContentSize()
        ) {
            // 1. Expanded Panel (Slide up with settings options) - Shown ONLY when inside Immich Home and isBarExpanded is true
            if (currentScreen == AppScreen.IMMICH_HOME && photoServerState != null && photoServerState.isBarExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ajustes de Galería",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MoonlightColors.Tertiary
                        )
                        IconButton(
                            onClick = { photoServerState.isBarExpanded = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White)
                        }
                    }

                    // Interactive Column count chips selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Columnas:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.weight(1f)
                        )
                        listOf(2, 3, 4, 5).forEach { cols ->
                            val isSelected = photoServerState.gridColumnCount == cols
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MoonlightColors.Tertiary else Color.White.copy(alpha = 0.05f))
                                    .clickable { photoServerState.gridColumnCount = cols }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$cols Col",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    // Quick action utility buttons (Refresh & Exit Home)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                photoServerState.isBarExpanded = false
                                onRefreshImmich?.invoke()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.07f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Sync, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sincronizar", style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = {
                                photoServerState.isBarExpanded = false
                                onNavigate(AppScreen.PHOTO_SERVER)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MoonlightColors.Tertiary.copy(alpha = 0.2f),
                                contentColor = MoonlightColors.Tertiary
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Home, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Volver a Casa", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.08f))
                )
            }

            // 2. Base Bar Layout: 2x4 Grid when inside Immich, 1x4 Row for other screens
            if (currentScreen == AppScreen.IMMICH_HOME && photoServerState != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Row 1: General App Navigation Items (Inicio, Juegos, Energía, Servidor)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        GeneralTabItem(
                            label = "Inicio",
                            icon = Icons.Default.Home,
                            selected = false,
                            onClick = { onNavigate(AppScreen.MAIN_MENU) }
                        )
                        GeneralTabItem(
                            label = "Juegos",
                            icon = Icons.Default.SportsEsports,
                            selected = false,
                            onClick = { onNavigate(AppScreen.MOONLIGHT) }
                        )
                        GeneralTabItem(
                            label = "Energía",
                            icon = Icons.Default.PowerSettingsNew,
                            selected = false,
                            onClick = { onNavigate(AppScreen.POWER_CONTROL) }
                        )
                        GeneralTabItem(
                            label = "Servidor",
                            icon = Icons.Default.PhotoAlbum,
                            selected = true,
                            onClick = { onNavigate(AppScreen.PHOTO_SERVER) }
                        )
                    }

                    // Row 2: Immich Specific Navigation Items (Fotos, Álbumes, Favoritos, Opciones)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        TabItem(
                            label = "Fotos",
                            icon = Icons.Default.PhotoLibrary,
                            selected = photoServerState.currentTab == "fotos",
                            onClick = { photoServerState.currentTab = "fotos" }
                        )
                        TabItem(
                            label = "Álbumes",
                            icon = Icons.Default.Folder,
                            selected = photoServerState.currentTab == "albums",
                            onClick = { photoServerState.currentTab = "albums" }
                        )
                        TabItem(
                            label = "Favoritos",
                            icon = Icons.Default.Favorite,
                            selected = photoServerState.currentTab == "favoritos",
                            onClick = { photoServerState.currentTab = "favoritos" }
                        )
                        TabItem(
                            label = "Opciones",
                            icon = if (photoServerState.isBarExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.Settings,
                            selected = photoServerState.isBarExpanded,
                            onClick = { photoServerState.isBarExpanded = !photoServerState.isBarExpanded }
                        )
                    }
                }
            } else {
                // Default Single Row for other screens
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    GeneralTabItem(
                        label = "Inicio",
                        icon = Icons.Default.Home,
                        selected = currentScreen == AppScreen.MAIN_MENU,
                        onClick = { onNavigate(AppScreen.MAIN_MENU) }
                    )
                    GeneralTabItem(
                        label = "Juegos",
                        icon = Icons.Default.SportsEsports,
                        selected = currentScreen == AppScreen.MOONLIGHT || currentScreen == AppScreen.GAME_LIST,
                        onClick = { onNavigate(AppScreen.MOONLIGHT) }
                    )
                    GeneralTabItem(
                        label = "Energía",
                        icon = Icons.Default.PowerSettingsNew,
                        selected = currentScreen == AppScreen.POWER_CONTROL,
                        onClick = { onNavigate(AppScreen.POWER_CONTROL) }
                    )
                    GeneralTabItem(
                        label = "Servidor",
                        icon = Icons.Default.PhotoAlbum,
                        selected = currentScreen == AppScreen.PHOTO_SERVER || currentScreen == AppScreen.IMMICH_HOME,
                        onClick = { onNavigate(AppScreen.PHOTO_SERVER) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp)) // 🛸 RIPPLE CONSTRAINT: Confine raw clicks to a gorgeous rounded pill!
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MoonlightColors.Tertiary else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (selected) MoonlightColors.Tertiary else Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun RowScope.GeneralTabItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp)) // 🛸 RIPPLE CONSTRAINT: Confine general clicks to a rounded pill as well!
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MoonlightColors.Primary else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (selected) MoonlightColors.Primary else Color.White.copy(alpha = 0.4f)
            )
        }
    }
}
