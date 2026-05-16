package com.limelight.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.limelight.shared.ui.components.AetherisScreen
import com.limelight.shared.ui.theme.MoonlightColors

private data class ImmichTab(
    val id: String,
    val label: String,
    val icon: ImageVector,
)

private val immichTabs = listOf(
    ImmichTab(id = "fotos", label = "Fotos", icon = Icons.Default.PhotoLibrary),
    ImmichTab(id = "buscar", label = "Buscar", icon = Icons.Default.Search),
    ImmichTab(id = "compartido", label = "Compartido", icon = Icons.Default.Share),
    ImmichTab(id = "biblioteca", label = "Biblioteca", icon = Icons.Default.MenuBook),
)

private const val defaultImmichTabId = "fotos"

@Composable
fun ImmichHomeScreen(onBack: () -> Unit) {
    var selectedTabId by rememberSaveable { mutableStateOf(defaultImmichTabId) }

    AetherisScreen {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Immich") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    immichTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTabId == tab.id,
                            onClick = { selectedTabId = tab.id },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        ) { padding ->
            when (selectedTabId) {
                "fotos" -> ImmichPhotosTab(modifier = Modifier.padding(padding))
                "buscar" -> ImmichSearchTab(modifier = Modifier.padding(padding))
                "compartido" -> ImmichSharedTab(modifier = Modifier.padding(padding))
                "biblioteca" -> ImmichLibraryTab(modifier = Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun ImmichPhotosTab(modifier: Modifier = Modifier) {
    ImmichTabPlaceholder(
        title = "Fotos",
        subtitle = "Vista principal de la galería de Immich.",
        modifier = modifier
    )
}

@Composable
private fun ImmichSearchTab(modifier: Modifier = Modifier) {
    ImmichTabPlaceholder(
        title = "Buscar",
        subtitle = "Próximamente: búsqueda avanzada de fotos y videos.",
        modifier = modifier
    )
}

@Composable
private fun ImmichSharedTab(modifier: Modifier = Modifier) {
    ImmichTabPlaceholder(
        title = "Compartido",
        subtitle = "Próximamente: elementos y álbumes compartidos.",
        modifier = modifier
    )
}

@Composable
private fun ImmichLibraryTab(modifier: Modifier = Modifier) {
    ImmichTabPlaceholder(
        title = "Biblioteca",
        subtitle = "Próximamente: organización y gestión de la biblioteca.",
        modifier = modifier
    )
}

@Composable
private fun ImmichTabPlaceholder(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MoonlightColors.OnSurfaceVariant
        )
    }
}
