package com.limelight.shared.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MoonlightColors.Purple
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
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
                    // Mobile / Portrait mode: Stacked
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MenuCard(
                            title = "Moonlight",
                            subtitle = "Streaming de Juegos",
                            icon = Icons.Default.Games,
                            onClick = { onNavigate(AppScreen.MOONLIGHT) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        MenuCard(
                            title = "Mi PC",
                            subtitle = "Control de Energía",
                            icon = Icons.Default.Computer,
                            onClick = { onNavigate(AppScreen.POWER_CONTROL) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        MenuCard(
                            title = "Fotos",
                            subtitle = "Servidor Multimedia",
                            icon = Icons.Default.PhotoAlbum,
                            onClick = { onNavigate(AppScreen.PHOTO_SERVER) },
                            modifier = Modifier.fillMaxWidth()
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
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(64.dp),
                tint = MoonlightColors.Green
            )
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
