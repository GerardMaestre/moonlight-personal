package com.limelight.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.stickyHeader
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.shared.data.immich.ImmichGalleryState
import com.limelight.shared.data.immich.ImmichPhotoAsset
import com.limelight.shared.data.immich.ImmichServerSummary
import com.limelight.shared.platform.PhotoServerActions
import com.limelight.shared.platform.TimelineSection
import com.limelight.shared.platform.TimelineUiModel
import com.limelight.shared.platform.PhotoServerState
import com.limelight.shared.platform.PhotoServerStatus
import com.limelight.shared.platform.PreviewPhotoServerActions
import com.limelight.shared.ui.components.*
import kotlin.math.max
import com.limelight.shared.ui.theme.MoonlightColors
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoServerScreen(
    state: PhotoServerState,
    actions: PhotoServerActions = PreviewPhotoServerActions,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    AetherisScreen(primaryGlowAlignment = Alignment.TopStart, secondaryGlowAlignment = Alignment.BottomEnd) {
        Scaffold(topBar = { HomeHubTopBar(onBack = onBack) }, containerColor = Color.Transparent) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Multimedia", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 42.sp), color = MoonlightColors.OnSurface, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Cliente nativo de Immich conectado a la API REST real.", style = MaterialTheme.typography.bodyLarge, color = MoonlightColors.OnSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))

                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    if (maxWidth > 840.dp) {
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.weight(2f)) {
                                ConnectionCard(state, actions)
                                ControlCard(state, actions)
                            }
                            SummaryColumn(state.galleryState, Modifier.weight(1f))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxWidth()) {
                            ConnectionCard(state, actions)
                            ControlCard(state, actions)
                            SummaryColumn(state.galleryState)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                GalleryCard(
                    galleryState = state.galleryState,
                    config = state.connectionConfig,
                    logs = state.recentLogs,
                    timeline = state.timelineUiModel,
                    onRefresh = { scope.launch { actions.refreshImmich() } },
                )
                Spacer(Modifier.height(110.dp))
            }
        }
    }
}

@Composable
private fun ConnectionCard(state: PhotoServerState, actions: PhotoServerActions, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    GlassCard(contentPadding = PaddingValues(22.dp), modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Conexión Immich", style = MaterialTheme.typography.headlineMedium, color = MoonlightColors.OnSurface)
            Text("Introduce la URL de tu instancia y una API Key con permisos asset.read/server.*.", style = MaterialTheme.typography.bodyMedium, color = MoonlightColors.OnSurfaceVariant)
            OutlinedTextField(
                value = state.connectionConfig.baseUrl,
                onValueChange = { state.updateConnection(baseUrl = it, apiKey = state.connectionConfig.apiKey) },
                label = { Text("URL base (https://immich.tu-dominio.com)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.connectionConfig.apiKey,
                onValueChange = { state.updateConnection(baseUrl = state.connectionConfig.baseUrl, apiKey = it) },
                label = { Text("API Key") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryGlassButton(
                text = "Conectar y cargar galería",
                icon = Icons.Default.CloudSync,
                onClick = { scope.launch { actions.startPhotoServer() } },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.status != PhotoServerStatus.Starting && state.galleryState !is ImmichGalleryState.Loading,
            )
        }
    }
}

@Composable
private fun ControlCard(state: PhotoServerState, actions: PhotoServerActions, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val running = state.status is PhotoServerStatus.Running
    val starting = state.status == PhotoServerStatus.Starting
    val accent = when (state.status) {
        PhotoServerStatus.Stopped -> MoonlightColors.Outline
        PhotoServerStatus.Starting -> MoonlightColors.Tertiary
        is PhotoServerStatus.Running -> MoonlightColors.Tertiary
        is PhotoServerStatus.Error -> MoonlightColors.Error
    }

    GlassCard(contentPadding = PaddingValues(28.dp), modifier = modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(132.dp).clip(CircleShape).background(accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(94.dp).clip(CircleShape).background(accent.copy(alpha = 0.22f), CircleShape))
                Icon(if (running) Icons.Default.CloudDone else if (starting) Icons.Default.CloudSync else Icons.Default.CloudOff, null, tint = accent, modifier = Modifier.size(78.dp))
            }
            Spacer(Modifier.height(18.dp))
            StatusPill(if (running) "Immich API" else if (starting) "Conectando" else "Sin sesión", accent)
            Spacer(Modifier.height(12.dp))
            Text(if (running) "Servidor Activo" else "Cliente Multimedia", style = MaterialTheme.typography.headlineLarge, color = MoonlightColors.OnSurface)
            Spacer(Modifier.height(8.dp))
            Text(state.healthMessage, style = MaterialTheme.typography.bodyMedium, color = MoonlightColors.OnSurfaceVariant, textAlign = TextAlign.Center)
            state.lastError?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = MoonlightColors.Error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PrimaryGlassButton(if (running) "Recargar" else "Conectar", Icons.Default.PowerSettingsNew, { scope.launch { actions.startPhotoServer() } }, Modifier.weight(1f), enabled = !starting)
                ErrorGlassButton("Cerrar", Icons.Default.StopCircle, { actions.stopPhotoServer() }, Modifier.weight(1f), enabled = running || state.status is PhotoServerStatus.Error)
            }
            if (running || state.status is PhotoServerStatus.Error) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { scope.launch { actions.restartPhotoServer() } }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(999.dp)) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reconectar")
                }
            }
        }
    }
}

@Composable
private fun SummaryColumn(galleryState: ImmichGalleryState, modifier: Modifier = Modifier) {
    val summary = (galleryState as? ImmichGalleryState.Success)?.summary ?: ImmichServerSummary()
    Column(verticalArrangement = Arrangement.spacedBy(24.dp), modifier = modifier.fillMaxWidth()) {
        MetricCard("Fotos", summary.images.toString(), "Imágenes", "Total real desde Immich", Icons.Default.PhotoLibrary, progressFrom(summary.images, summary.totalAssets), MoonlightColors.Tertiary)
        MetricCard("Vídeos", summary.videos.toString(), "Clips", "API /search/statistics", Icons.Default.VideoLibrary, progressFrom(summary.videos, summary.totalAssets), MoonlightColors.PrimaryContainer)
        MetricCard("Almacenamiento", formatBytes(summary.quotaUsageBytes), quotaSuffix(summary.quotaSizeBytes), summary.userName ?: "Usuario autenticado", Icons.Default.Storage, quotaProgress(summary), MoonlightColors.Secondary)
    }
}

@Composable
private fun MetricCard(title: String, value: String, suffix: String, footer: String, icon: androidx.compose.ui.graphics.vector.ImageVector, progress: Float, color: Color) {
    GlassCard(contentPadding = PaddingValues(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MoonlightColors.OnSurfaceVariant, letterSpacing = 1.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(value, style = MaterialTheme.typography.headlineMedium, color = MoonlightColors.OnSurface)
                    Text(suffix, style = MaterialTheme.typography.bodyMedium, color = MoonlightColors.Outline)
                }
                Icon(icon, null, tint = color, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(18.dp))
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)).background(MoonlightColors.SurfaceContainerHighest)) {
                Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(999.dp)).background(color))
            }
            Spacer(Modifier.height(8.dp))
            Text(footer, style = MaterialTheme.typography.labelSmall, color = if (progress >= 1f) MoonlightColors.Tertiary else MoonlightColors.Outline, modifier = Modifier.align(Alignment.End))
        }
    }
}

@Composable
private fun GalleryCard(galleryState: ImmichGalleryState, config: com.limelight.shared.data.immich.ImmichConnectionConfig, logs: List<String>, timeline: TimelineUiModel, onRefresh: () -> Unit) {
    GlassCard(contentPadding = PaddingValues(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Galería Immich", style = MaterialTheme.typography.headlineMedium, color = MoonlightColors.OnSurface)
                    Spacer(Modifier.height(4.dp))
                    Text("Miniaturas reales cargadas desde /api/assets/{id}/thumbnail", style = MaterialTheme.typography.bodyMedium, color = MoonlightColors.OnSurfaceVariant)
                }
                IconButton(onClick = onRefresh, enabled = galleryState !is ImmichGalleryState.Loading && galleryState !is ImmichGalleryState.Idle) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar galería", tint = MoonlightColors.Primary)
                }
            }
            Spacer(Modifier.height(16.dp))
            when (galleryState) {
                ImmichGalleryState.Idle -> EmptyGalleryMessage("Configura la conexión para cargar fotos reales de Immich.")
                ImmichGalleryState.Loading -> LoadingGallery()
                is ImmichGalleryState.Error -> EmptyGalleryMessage(galleryState.message, isError = true, onRetry = onRefresh)
                is ImmichGalleryState.Success -> PhotoTimeline(timeline = timeline, config = config, onLoadMore = onRefresh)
            }
            if (logs.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                logs.takeLast(3).forEach { Text("> $it", color = MoonlightColors.OnSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
            }
        }
    }
}

@Composable
private fun PhotoTimeline(timeline: TimelineUiModel, config: com.limelight.shared.data.immich.ImmichConnectionConfig, loadingMore: Boolean = false, onLoadMore: () -> Unit) {
    if (timeline.sections.isEmpty()) {
        EmptyGalleryMessage("Immich respondió correctamente, pero no devolvió fotos para esta API Key.")
        return
    }
    val listState = rememberLazyListState()
    val preloadThreshold = 4
    LaunchedEffect(listState, timeline.sections.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .filter { index -> index >= (listState.layoutInfo.totalItemsCount - 1 - preloadThreshold).coerceAtLeast(0) }
            .collect { onLoadMore() }
    }
    var selectedAssetId by remember { mutableStateOf<String?>(null) }
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp), state = listState, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        timeline.sections.forEach { section ->
            stickyHeader(key = "header-${section.dayKey}") { TimelineHeader(section.dayKey) }
            item(key = "grid-${section.dayKey}") { SectionGrid(section = section, config = config, onAssetClick = { selectedAssetId = it }) }
        }
        if (loadingMore) item(key = "loading-more") { LoadingGalleryGrid() }
    }
    selectedAssetId?.let { id ->
        AssetDetailScreen(initialAssetId = id, timeline = timeline.sections.flatMap { it.items }.map { it.asset }, config = config, onDismiss = { selectedAssetId = null })
    }
}

@Composable
private fun TimelineHeader(dayKey: String) {
    Text(
        text = formatDayHeader(dayKey),
        color = MoonlightColors.OnSurface,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.fillMaxWidth().background(MoonlightColors.Surface.copy(alpha = 0.95f)).padding(vertical = 6.dp),
    )
}

@Composable
private fun SectionGrid(section: TimelineSection, config: com.limelight.shared.data.immich.ImmichConnectionConfig, onAssetClick: (String) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Adaptive(148.dp), userScrollEnabled = false, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 10000.dp)) {
        items(section.items, key = { it.id }) { item -> GalleryTile(item.asset, config, onAssetClick) }
    }
}

@Composable
private fun GalleryTile(photo: ImmichPhotoAsset, config: com.limelight.shared.data.immich.ImmichConnectionConfig, onAssetClick: (String) -> Unit) {
    Box(Modifier.aspectRatio(1f).clickable { onAssetClick(photo.id) }.clip(RoundedCornerShape(24.dp)).background(MoonlightColors.SurfaceContainerHighest).border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))) {
        ThumbnailImage(
            assetId = photo.id,
            contentDescription = photo.name,
            config = config,
            cellSize = 148.dp,
            modifier = Modifier.matchParentSize(),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(MoonlightColors.Surface.copy(alpha = 0.82f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (photo.isFavorite) {
                    Icon(Icons.Default.Favorite, null, tint = MoonlightColors.Error, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(photo.name, color = MoonlightColors.OnSurface, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            }
            photo.location?.let { Text(it, color = MoonlightColors.OnSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}


@Composable
private fun AssetDetailScreen(initialAssetId: String, timeline: List<ImmichPhotoAsset>, config: com.limelight.shared.data.immich.ImmichConnectionConfig, onDismiss: () -> Unit) {
    val vm = remember { AssetDetailViewModel() }
    LaunchedEffect(initialAssetId, timeline.size) { vm.load(initialAssetId, timeline, config) }
    val st = vm.state
    val asset = st.currentAsset ?: return
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val maxScale = 4f
    val transform = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, maxScale)
        val limit = max(0f, (scale - 1f) * 300f)
        offset = Offset((offset.x + panChange.x).coerceIn(-limit, limit), (offset.y + panChange.y).coerceIn(-limit, limit))
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.fillMaxWidth().height(360.dp).clip(RoundedCornerShape(16.dp)).background(MoonlightColors.SurfaceContainerHighest), contentAlignment = Alignment.Center) {
                if (asset.name.substringAfterLast('.', "").lowercase() in listOf("mp4","mov","mkv")) {
                    PlatformVideoPlayer(streamingUrl = "${config.baseUrl.trimEnd('/')}/api/assets/${asset.id}/original", authConfig = config, isPlaying = st.isPlaying, onDurationKnown = {}, onPositionChanged = {}, modifier = Modifier.fillMaxSize())
                } else {
                    ThumbnailImage(asset.id, asset.name, config, 600.dp, Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y).transformable(transform), cornerRadius = 0.dp)
                }
            }
            Row { IconButton(onClick = { vm.previous() }) { Icon(Icons.Default.NavigateBefore, null)}; IconButton(onClick = { vm.playPause() }) { Icon(if (st.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null)}; IconButton(onClick = { vm.next() }) { Icon(Icons.Default.NavigateNext, null)} }
            Slider(value = st.playbackProgress, onValueChange = vm::seekTo)
            OutlinedButton(onClick = { vm.showMetadata(!st.showMetadata) }) { Text("Metadatos") }
            if (st.showMetadata) {
                Text("Cámara: —\nUbicación: ${asset.location ?: "—"}\nFecha: ${asset.createdAt ?: "—"}\nTamaño: —\nDimensiones: —\nDuración: —")
            }
        }
    }
}

@Composable
private fun LoadingGallery() {
    LoadingGalleryGrid()
}

@Composable
private fun LoadingGalleryGrid() {
    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MoonlightColors.Primary)
            Spacer(Modifier.height(12.dp))
            Text("Cargando galería real de Immich...", color = MoonlightColors.OnSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmptyGalleryMessage(message: String, isError: Boolean = false, onRetry: (() -> Unit)? = null) {
    Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(24.dp)).background(MoonlightColors.SurfaceContainerHighest), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = if (isError) MoonlightColors.Error else MoonlightColors.OnSurfaceVariant, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(20.dp))
            if (isError && onRetry != null) {
                OutlinedButton(onClick = onRetry) { Text("Reintentar") }
            }
        }
    }
}

private fun progressFrom(value: Int, total: Int): Float = when {
    total <= 0 -> 0f
    else -> (value.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}

private fun quotaProgress(summary: ImmichServerSummary): Float {
    val usage = summary.quotaUsageBytes ?: return 0f
    val size = summary.quotaSizeBytes ?: return 0f
    return when {
        size <= 0L -> 0f
        else -> (usage.toFloat() / size.toFloat()).coerceIn(0f, 1f)
    }
}

private fun quotaSuffix(quotaSizeBytes: Long?): String = quotaSizeBytes?.let { "/ ${formatBytes(it)}" } ?: "Sin cuota"

private fun formatBytes(bytes: Long?): String {
    val value = bytes ?: return "—"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    val sequence = generateSequence(value.toDouble() to 0) { (current, index) ->
        when {
            current >= 1024.0 && index < units.lastIndex -> current / 1024.0 to index + 1
            else -> null
        }
    }.last()
    return "${(sequence.first * 10).toInt() / 10.0} ${units[sequence.second]}"
}

private fun formatDayHeader(dayKey: String): String = runCatching {
    if (dayKey == "Sin fecha") dayKey else {
        val d = LocalDate.parse(dayKey)
        "%02d/%02d/%04d".format(d.dayOfMonth, d.monthNumber, d.year)
    }
}.getOrDefault(dayKey)
