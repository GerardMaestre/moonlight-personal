package com.limelight.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.limelight.shared.ui.components.AetherisScreen
import com.limelight.shared.ui.theme.MoonlightColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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

private data class ImmichHomeUiState(
    val selectedTabIndex: Int = 0,
)

private class ImmichHomeStateHolder(
    initialState: ImmichHomeUiState = ImmichHomeUiState(),
) {
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<ImmichHomeUiState> = _uiState.asStateFlow()

    fun setSelectedTabIndex(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
    }
}

@Composable
fun ImmichHomeScreen(onBack: () -> Unit) {
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    val stateHolder = remember(selectedTabIndex) {
        ImmichHomeStateHolder(
            initialState = ImmichHomeUiState(selectedTabIndex = selectedTabIndex)
        )
    }
    val uiState by stateHolder.uiState.collectAsState()
    val selectedTabId = immichTabs.getOrNull(uiState.selectedTabIndex)?.id ?: defaultImmichTabId

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
                        val tabIndex = immichTabs.indexOf(tab)
                        NavigationBarItem(
                            selected = uiState.selectedTabIndex == tabIndex,
                            onClick = {
                                selectedTabIndex = tabIndex
                                stateHolder.setSelectedTabIndex(tabIndex)
                            },
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
    val photosGridState = rememberLazyGridState()
    val photoItems = remember { List(60) { "Foto #${it + 1}" } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = CircleShape,
                color = MoonlightColors.Surface,
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = MoonlightColors.OnSurfaceVariant
                    )
                    Text(
                        text = "Buscar fotos, videos, personas...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MoonlightColors.OnSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MoonlightColors.Surface)
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Perfil de usuario",
                    tint = MoonlightColors.Primary,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = photosGridState,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(photoItems) { label ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MoonlightColors.Surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MoonlightColors.OnSurfaceVariant
                    )
                }
            }
        }
    }
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
