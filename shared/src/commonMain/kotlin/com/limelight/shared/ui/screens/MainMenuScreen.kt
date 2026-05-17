package com.limelight.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.shared.ui.components.AetherisScreen
import com.limelight.shared.ui.components.GlassCard
import com.limelight.shared.ui.components.HomeHubTopBar
import com.limelight.shared.ui.theme.MoonlightColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(onNavigate: (AppScreen) -> Unit) {
    AetherisScreen {
        Scaffold(
            topBar = { HomeHubTopBar() },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Home Hub", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 42.sp), color = MoonlightColors.OnSurface, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Control centralizado para gaming, energía y multimedia.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MoonlightColors.OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(30.dp))

                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    if (maxWidth > 700.dp) {
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth()) {
                            MenuCard("Moonlight", "Gaming Nexus", Icons.Default.Games, MoonlightColors.Primary, { onNavigate(AppScreen.MOONLIGHT) }, Modifier.weight(1f))
                            MenuCard("Mi PC", "Core Power", Icons.Default.Computer, MoonlightColors.Tertiary, { onNavigate(AppScreen.POWER_CONTROL) }, Modifier.weight(1f))
                            MenuCard("Multimedia", "Aetheris Media", Icons.Default.PhotoAlbum, MoonlightColors.Secondary, { onNavigate(AppScreen.IMMICH_HOME) }, Modifier.weight(1f))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxWidth()) {
                            MenuCard("Moonlight", "Streaming de juegos", Icons.Default.Games, MoonlightColors.Primary, { onNavigate(AppScreen.MOONLIGHT) })
                            MenuCard("Mi PC", "Control de energía", Icons.Default.Computer, MoonlightColors.Tertiary, { onNavigate(AppScreen.POWER_CONTROL) })
                            MenuCard("Multimedia", "Servidor de fotos Immich", Icons.Default.PhotoAlbum, MoonlightColors.Secondary, { onNavigate(AppScreen.IMMICH_HOME) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .heightIn(min = 178.dp)
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(24.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.18f), Color.Transparent)))
            )
            Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(38.dp)) }
                Spacer(Modifier.height(18.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MoonlightColors.OnSurface)
                Spacer(Modifier.height(6.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MoonlightColors.OnSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}
