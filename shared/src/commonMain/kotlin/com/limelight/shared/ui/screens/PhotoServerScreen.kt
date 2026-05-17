package com.limelight.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import com.limelight.shared.platform.PhotoServerActions
import com.limelight.shared.platform.PhotoServerState
import com.limelight.shared.platform.PhotoServerStatus
import com.limelight.shared.platform.PreviewPhotoServerActions
import com.limelight.shared.ui.components.*
import com.limelight.shared.ui.theme.MoonlightColors
import kotlinx.coroutines.launch

import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import com.limelight.shared.data.immich.ImmichPhotoAsset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoServerScreen(
    state: PhotoServerState,
    actions: PhotoServerActions = PreviewPhotoServerActions,
    onBack: () -> Unit,
    onOpenImmich: (left: Int, top: Int, width: Int, height: Int) -> Unit,
) {
    var selectedAssetId by remember { mutableStateOf<String?>(null) }
    val timelineAssets = remember(state.timelineUiModel.sections) {
        state.timelineUiModel.sections.flatMap { section -> section.items.map { item -> item.asset } }
    }

    AetherisScreen(primaryGlowAlignment = Alignment.TopStart, secondaryGlowAlignment = Alignment.BottomEnd) {
        Scaffold(topBar = { HomeHubTopBar(title = "Ajustes de galería") }, containerColor = Color.Transparent) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item {
                    Text("Servidor Multimedia", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 42.sp), color = MoonlightColors.OnSurface, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    Text("Control centralizado para iniciar y conectar la galería multimedia nativa de Immich.", style = MaterialTheme.typography.bodyLarge, color = MoonlightColors.OnSurfaceVariant, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }

                item {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        if (maxWidth > 840.dp) {
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.weight(1f)) {
                                    ConnectionCard(state, actions)
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.weight(1f)) {
                                    ControlCard(state, actions, onOpenImmich = onOpenImmich)
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxWidth()) {
                                ConnectionCard(state, actions)
                                ControlCard(state, actions, onOpenImmich = onOpenImmich)
                            }
                        }
                    }
                }

                if (state.recentLogs.isNotEmpty()) {
                    item {
                        LogsCard(logs = state.recentLogs)
                    }
                }
                if (timelineAssets.isNotEmpty()) {
                    item {
                        GalleryPreview(assets = timelineAssets, config = state.connectionConfig, onAssetClick = { selectedAssetId = it })
                    }
                }

                item { Spacer(Modifier.height(56.dp)) }
            }
        }
    }
    selectedAssetId?.let { selectedId ->
        FullscreenAssetViewer(
            assets = timelineAssets,
            selectedAssetId = selectedId,
            config = state.connectionConfig,
            onDismiss = { selectedAssetId = null }
        )
    }
    DisposableEffect(selectedAssetId) {
        state.isFullscreenViewerOpen = selectedAssetId != null
        onDispose {
            state.isFullscreenViewerOpen = false
        }
    }
}

@Composable
private fun GalleryPreview(assets: List<ImmichPhotoAsset>, config: com.limelight.shared.data.immich.ImmichConnectionConfig, onAssetClick: (String) -> Unit) {
    var gridColumns by remember { mutableStateOf(3) }
    GlassCard(contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
        Text("Vista rápida", style = MaterialTheme.typography.titleMedium, color = MoonlightColors.OnSurface)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Pellizca para cambiar tamaño (${gridColumns} columnas)",
            style = MaterialTheme.typography.bodySmall,
            color = MoonlightColors.OnSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            modifier = Modifier
                .height(320.dp)
                .pointerInput(gridColumns) {
                    detectTransformGestures { _, _, zoom, _ ->
                        val next = if (zoom > 1.04f) {
                            (gridColumns - 1).coerceAtLeast(2)
                        } else if (zoom < 0.96f) {
                            (gridColumns + 1).coerceAtMost(6)
                        } else {
                            gridColumns
                        }
                        if (next != gridColumns) {
                            gridColumns = next
                        }
                    }
                },
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = assets.take(30), key = { it.id }) { asset ->
                Box(modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(10.dp)).clickable { onAssetClick(asset.id) }) {
                    ThumbnailImage(asset.id, asset.name, config, 160.dp, modifier = Modifier.fillMaxSize(), cornerRadius = 10.dp)
                }
            }
        }
    }
}

@Composable
private fun FullscreenAssetViewer(
    assets: List<ImmichPhotoAsset>,
    selectedAssetId: String,
    config: com.limelight.shared.data.immich.ImmichConnectionConfig,
    onDismiss: () -> Unit
) {
    val initialPage = remember(selectedAssetId, assets) { assets.indexOfFirst { it.id == selectedAssetId }.coerceAtLeast(0) }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { assets.size })
    val scope = rememberCoroutineScope()
    val context = LocalPlatformContext.current
    val requestFactory = remember { AuthenticatedImageRequestFactory() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val asset = assets[page]
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
                    SubcomposeAsyncImage(
                        model = requestFactory.buildOriginalRequest(context, config, asset.id),
                        contentDescription = asset.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().background(Color.Black)
                    )
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopStart).padding(20.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Cerrar", tint = Color.White)
            }
            if (assets.size > 1) {
                IconButton(
                    onClick = {
                        val previousPage = (pagerState.currentPage - 1).coerceAtLeast(0)
                        if (previousPage != pagerState.currentPage) {
                            scope.launch {
                                pagerState.animateScrollToPage(previousPage)
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterStart).padding(12.dp)
                ) {
                    Icon(Icons.Default.NavigateBefore, contentDescription = "Anterior", tint = Color.White)
                }
                IconButton(
                    onClick = {
                        val nextPage = (pagerState.currentPage + 1).coerceAtMost(assets.lastIndex)
                        if (nextPage != pagerState.currentPage) {
                            scope.launch {
                                pagerState.animateScrollToPage(nextPage)
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterEnd).padding(12.dp)
                ) {
                    Icon(Icons.Default.NavigateNext, contentDescription = "Siguiente", tint = Color.White)
                }
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
            Text("Introduce la URL de tu instancia y una API Key con permisos de lectura.", style = MaterialTheme.typography.bodyMedium, color = MoonlightColors.OnSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            OutlinedTextField(
                value = state.connectionConfig.baseUrl,
                onValueChange = { actions.onUpdateConnection(it, state.connectionConfig.apiKey) },
                label = { Text("URL base (http://100.67.140.39:2283)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.connectionConfig.apiKey,
                onValueChange = { actions.onUpdateConnection(state.connectionConfig.baseUrl, it) },
                label = { Text("API Key") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryGlassButton(
                text = "Guardar y Verificar",
                icon = Icons.Default.Refresh,
                onClick = { scope.launch { actions.refreshImmich() } },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.status != PhotoServerStatus.Starting,
            )
        }
    }
}

@Composable
private fun ControlCard(
    state: PhotoServerState,
    actions: PhotoServerActions,
    onOpenImmich: (left: Int, top: Int, width: Int, height: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val isOnline = state.status is PhotoServerStatus.Running
    val starting = state.status == PhotoServerStatus.Starting
    var buttonBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val accent = when (state.status) {
        PhotoServerStatus.Stopped -> MoonlightColors.Outline
        PhotoServerStatus.Starting -> MoonlightColors.Tertiary
        is PhotoServerStatus.Running -> MoonlightColors.Tertiary
        is PhotoServerStatus.Error -> MoonlightColors.Error
    }

    GlassCard(contentPadding = PaddingValues(28.dp), modifier = modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(120.dp).clip(CircleShape).background(accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(86.dp).clip(CircleShape).background(accent.copy(alpha = 0.22f), CircleShape))
                Icon(if (isOnline) Icons.Default.CloudDone else if (starting) Icons.Default.CloudSync else Icons.Default.CloudOff, null, tint = accent, modifier = Modifier.size(64.dp))
            }
            Spacer(Modifier.height(18.dp))
            StatusPill(if (isOnline) "Servidor Activo" else if (starting) "Conectando" else "Servidor Apagado", accent)
            Spacer(Modifier.height(12.dp))
            Text(if (isOnline) "En Línea" else "Desconectado", style = MaterialTheme.typography.headlineMedium, color = MoonlightColors.OnSurface)
            Spacer(Modifier.height(8.dp))
            Text(state.healthMessage, style = MaterialTheme.typography.bodyMedium, color = MoonlightColors.OnSurfaceVariant, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
            state.lastError?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = MoonlightColors.Error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PrimaryGlassButton(if (isOnline) "Recargar" else "Conectar", Icons.Default.PowerSettingsNew, { scope.launch { actions.startPhotoServer() } }, Modifier.weight(1f), enabled = !starting)
                ErrorGlassButton("Apagar", Icons.Default.StopCircle, { actions.stopPhotoServer() }, Modifier.weight(1f), enabled = isOnline || state.status is PhotoServerStatus.Error)
            }
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.Button(
                onClick = {
                    val bounds = buttonBounds
                    if (bounds != null) {
                        onOpenImmich(bounds.left.toInt(), bounds.top.toInt(), bounds.width.toInt(), bounds.height.toInt())
                    } else {
                        onOpenImmich(0, 0, 0, 0)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .onGloballyPositioned { coordinates ->
                        val position = coordinates.positionInWindow()
                        val size = coordinates.size
                        buttonBounds = androidx.compose.ui.geometry.Rect(
                            position.x,
                            position.y,
                            position.x + size.width,
                            position.y + size.height
                        )
                    },
                shape = RoundedCornerShape(999.dp),
                enabled = isOnline,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MoonlightColors.Tertiary,
                    disabledContainerColor = MoonlightColors.Tertiary.copy(alpha = 0.35f),
                    contentColor = MoonlightColors.OnPrimary,
                    disabledContentColor = MoonlightColors.OnPrimary.copy(alpha = 0.6f),
                ),
            ) {
                Icon(Icons.Default.PhotoLibrary, null)
                Spacer(Modifier.width(10.dp))
                Text("ABRIR GALERÍA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            }
            if (isOnline || state.status is PhotoServerStatus.Error) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { scope.launch { actions.restartPhotoServer() } }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(999.dp)) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reiniciar Proceso en PC")
                }
            }
        }
    }
}

@Composable
private fun LogsCard(logs: List<String>, modifier: Modifier = Modifier) {
    GlassCard(contentPadding = PaddingValues(22.dp), modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MoonlightColors.Primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Registro de Actividad", style = MaterialTheme.typography.titleMedium, color = MoonlightColors.OnSurface)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                logs.takeLast(6).forEach { log ->
                    Text(log, color = MoonlightColors.OnSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
            }
        }
    }
}
