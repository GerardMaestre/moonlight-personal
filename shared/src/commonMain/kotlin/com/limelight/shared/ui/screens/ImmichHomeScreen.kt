package com.limelight.shared.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.shared.ui.components.*
import com.limelight.shared.ui.theme.MoonlightColors
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import coil3.compose.LocalPlatformContext
import com.limelight.shared.platform.PhotoServerState
import com.limelight.shared.platform.PhotoServerActions
import com.limelight.shared.data.immich.ImmichGalleryState
import com.limelight.shared.platform.PhotoServerStatus
import com.limelight.shared.platform.TimelineSection

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

private data class TimelineSectionWithMonth(
    val section: TimelineSection,
    val monthHeader: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmichHomeScreen(
    state: PhotoServerState,
    actions: PhotoServerActions,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    val selectedTabId = immichTabs.getOrNull(selectedTabIndex)?.id ?: defaultImmichTabId
    val scope = rememberCoroutineScope()

    AetherisScreen {
        Scaffold(
            topBar = {
                ImmichTopBar(
                    onOpenSettings = onOpenSettings,
                    onBack = onBack
                )
            },
            bottomBar = {
                ImmichBottomNav(
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it }
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (selectedTabId) {
                    "fotos" -> ImmichPhotosTab(state, actions)
                    "buscar" -> ImmichSearchTab()
                    "compartido" -> ImmichSharedTab()
                    "biblioteca" -> ImmichLibraryTab()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImmichTopBar(
    onOpenSettings: () -> Unit,
    onBack: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            brush = Brush.sweepGradient(
                                listOf(Color(0xFFE53935), Color(0xFFFFB300), Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFFE53935))
                            ),
                            shape = CircleShape
                        )
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "immich",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        actions = {
            IconButton(onClick = { /* Sync Status */ }) {
                Icon(Icons.Default.CloudDone, null, tint = Color.White.copy(alpha = 0.7f))
            }
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFB300))
                    .clickable { onOpenSettings() },
                contentAlignment = Alignment.Center
            ) {
                Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(10.dp)
                        .background(Color(0xFF1E88E5), CircleShape)
                        .border(1.dp, Color.Black, CircleShape)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF111318),
            titleContentColor = Color.White
        ),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ChevronLeft, null, tint = Color.White)
            }
        }
    )
}

@Composable
private fun ImmichBottomNav(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF1E1F25),
        contentColor = Color.White,
        tonalElevation = 0.dp
    ) {
        immichTabs.forEachIndexed { index, tab ->
            NavigationBarItem(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = if (tab.id == "biblioteca") Icons.Default.GridView else tab.icon,
                        contentDescription = tab.label
                    )
                },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    indicatorColor = Color(0xFF3F4759),
                    unselectedIconColor = Color.White.copy(alpha = 0.6f),
                    unselectedTextColor = Color.White.copy(alpha = 0.6f)
                )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImmichPhotosTab(
    state: PhotoServerState,
    actions: PhotoServerActions,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val timeline = state.timelineUiModel
    val galleryState = state.galleryState

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF111318))) {
        when (galleryState) {
            is ImmichGalleryState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF1E88E5))
            }
            is ImmichGalleryState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(galleryState.message, color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { scope.launch { actions.refreshImmich() } }) {
                        Text("Reintentar")
                    }
                }
            }
            is ImmichGalleryState.Idle -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Conecta con tu servidor Immich", color = Color.White.copy(alpha = 0.6f))
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { scope.launch { actions.startPhotoServer() } }) {
                        Text("Conectar")
                    }
                }
            }
            is ImmichGalleryState.Success -> {
                var selectedAssetId by remember { mutableStateOf<String?>(null) }
                val gridState = rememberLazyGridState()
                
                val sectionsWithMonth = remember(timeline) {
                    val list = mutableListOf<TimelineSectionWithMonth>()
                    var lastMonth = ""
                    timeline.sections.forEach { section ->
                        val month = section.dayKey.split("-").getOrNull(1)?.let { 
                            when(it) {
                                "01" -> "Enero"
                                "02" -> "Febrero"
                                "03" -> "Marzo"
                                "04" -> "Abril"
                                "05" -> "Mayo"
                                "06" -> "Junio"
                                "07" -> "Julio"
                                "08" -> "Agosto"
                                "09" -> "Septiembre"
                                "10" -> "Octubre"
                                "11" -> "Noviembre"
                                "12" -> "Diciembre"
                                else -> ""
                            }
                        } ?: ""
                        list.add(TimelineSectionWithMonth(section, if (month != lastMonth) month else null))
                        lastMonth = month
                    }
                    list
                }
                
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        sectionsWithMonth.forEach { item ->
                            val section = item.section
                            if (item.monthHeader != null) {
                                item(span = { GridItemSpan(maxLineSpan) }, key = "month_${item.monthHeader}") {
                                    Text(
                                        text = item.monthHeader,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                                    )
                                }
                            }

                            item(span = { GridItemSpan(maxLineSpan) }, key = "day_${section.dayKey}") {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = section.readableDate,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(
                                        Icons.Default.CheckCircleOutline,
                                        null,
                                        tint = Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            
                            items(section.items, key = { it.id }) { gridItem ->
                                val photo = gridItem.asset
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clickable { selectedAssetId = photo.id }
                                ) {
                                    ThumbnailImage(
                                        assetId = photo.id,
                                        contentDescription = photo.name,
                                        config = state.connectionConfig,
                                        cellSize = 120.dp,
                                        modifier = Modifier.fillMaxSize(),
                                        cornerRadius = 0.dp
                                    )
                                    
                                    if (photo.isVideo) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                                .padding(4.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        }
                                    }

                                    Icon(
                                        imageVector = if (photo.id.hashCode() % 2 == 0) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
                                            .size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(6.dp)
                            .padding(vertical = 4.dp, horizontal = 1.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        val scrollFraction = if (gridState.layoutInfo.totalItemsCount > 0) {
                            gridState.firstVisibleItemIndex.toFloat() / gridState.layoutInfo.totalItemsCount.toFloat()
                        } else 0f
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.1f)
                                .offset(y = (scrollFraction * 400).dp)
                                .background(Color.White.copy(alpha = 0.4f), CircleShape)
                        )
                    }
                }

                selectedAssetId?.let { id ->
                    AssetDetailScreen(
                        initialAssetId = id,
                        timeline = timeline.sections.flatMap { it.items }.map { it.asset },
                        config = state.connectionConfig,
                        onDismiss = { selectedAssetId = null }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetDetailScreen(
    initialAssetId: String,
    timeline: List<com.limelight.shared.data.immich.ImmichPhotoAsset>,
    config: com.limelight.shared.data.immich.ImmichConnectionConfig,
    onDismiss: () -> Unit
) {
    val vm = remember { AssetDetailViewModel() }
    LaunchedEffect(initialAssetId) { vm.load(initialAssetId, timeline, config) }
    val st = vm.state
    val asset = st.currentAsset ?: return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
        scrimColor = Color.Black.copy(alpha = 0.9f),
        dragHandle = null,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color.Black)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (asset.isVideo) {
                    PlatformVideoPlayer(
                        streamingUrl = "${config.baseUrl.trimEnd('/')}/api/assets/${asset.id}/original",
                        authConfig = config,
                        isPlaying = true,
                        onDurationKnown = {},
                        onPositionChanged = {},
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    ZoomableThumbnailImage(
                        assetId = asset.id,
                        contentDescription = asset.name,
                        config = config,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)) { 
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White) 
                    }
                    Row {
                        IconButton(onClick = { /* Favorite */ }, modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)) { 
                            Icon(if (asset.isFavorite) Icons.Default.Star else Icons.Default.StarBorder, null, tint = Color.White) 
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { /* Info */ }, modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)) { 
                            Icon(Icons.Default.Info, null, tint = Color.White) 
                        }
                    }
                }
            }
            
            Surface(
                color = Color.Black,
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DetailActionItem(Icons.Default.Share, "Compartir")
                    DetailActionItem(Icons.Default.Edit, "Editar")
                    DetailActionItem(Icons.Default.CenterFocusStrong, "Lens")
                    DetailActionItem(Icons.Default.Delete, "Borrar")
                }
            }
        }
    }
}

@Composable
private fun DetailActionItem(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { /* TODO */ }) { Icon(icon, null, tint = Color.White) }
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ImmichSearchTab() = ImmichTabPlaceholder("Buscar")

@Composable
private fun ImmichSharedTab() = ImmichTabPlaceholder("Compartido")

@Composable
private fun ImmichLibraryTab() = ImmichTabPlaceholder("Biblioteca")

@Composable
private fun ImmichTabPlaceholder(title: String) {
    Box(Modifier.fillMaxSize().background(Color(0xFF111318)), contentAlignment = Alignment.Center) {
        Text(title, color = Color.White)
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
}
