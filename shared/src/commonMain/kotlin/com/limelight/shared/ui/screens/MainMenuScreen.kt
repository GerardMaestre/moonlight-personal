package com.limelight.shared.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.shared.ui.theme.MoonlightColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    onNavigate: (AppScreen) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "PANEL DE CONTROL",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: account action */ }) {
                        Icon(
                            imageVector = Icons.Default.Computer, // Placeholder, use proper icon later
                            contentDescription = "Account",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: tune action */ }) {
                        Icon(
                            imageVector = Icons.Default.Computer, // Placeholder
                            contentDescription = "Tune",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                    titleContentColor = MoonlightColors.TertiaryContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            // We use an experimental layout or a simple adaptive column/row based on screen width.
            // A BoxWithConstraints allows us to switch from Column to Row.
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                if (maxWidth > 600.dp) {
                    // Desktop / Landscape mode: Side by side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MenuCard(
                            title = "Moonlight",
                            subtitle = "Streaming de Juegos",
                            icon = Icons.Default.Games,
                            onClick = { onNavigate(AppScreen.MOONLIGHT) },
                            modifier = Modifier.weight(1f).padding(8.dp)
                        )
                        MenuCard(
                            title = "Mi PC",
                            subtitle = "Control de Energía",
                            icon = Icons.Default.Computer,
                            onClick = { onNavigate(AppScreen.POWER_CONTROL) },
                            modifier = Modifier.weight(1f).padding(8.dp)
                        )
                        MenuCard(
                            title = "Fotos",
                            subtitle = "Servidor Multimedia",
                            icon = Icons.Default.PhotoAlbum,
                            onClick = { onNavigate(AppScreen.PHOTO_SERVER) },
                            modifier = Modifier.weight(1f).padding(8.dp)
                        )
                    }
                } else {
                    // Mobile / Portrait mode: Stacked and Fixed
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        MenuCard(
                            title = "Moonlight",
                            subtitle = "Streaming de Juegos",
                            icon = Icons.Default.Games,
                            onClick = { onNavigate(AppScreen.MOONLIGHT) },
                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        MenuCard(
                            title = "Mi PC",
                            subtitle = "Control de Energía",
                            icon = Icons.Default.Computer,
                            onClick = { onNavigate(AppScreen.POWER_CONTROL) },
                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        MenuCard(
                            title = "Fotos",
                            subtitle = "Servidor Multimedia",
                            icon = Icons.Default.PhotoAlbum,
                            onClick = { onNavigate(AppScreen.PHOTO_SERVER) },
                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                        )
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MoonlightColors.Secondary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(36.dp),
                    tint = MoonlightColors.Secondary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
