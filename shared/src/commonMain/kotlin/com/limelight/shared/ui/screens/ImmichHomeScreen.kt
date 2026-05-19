package com.limelight.shared.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.shared.data.immich.ImmichConnectionConfig
import com.limelight.shared.data.immich.ImmichGalleryState
import com.limelight.shared.data.immich.ImmichPhotoAsset
import com.limelight.shared.platform.PhotoServerActions
import com.limelight.shared.platform.PhotoServerState
import com.limelight.shared.ui.components.*
import com.limelight.shared.ui.theme.MoonlightColors
import com.limelight.shared.data.remote.repository.ImmichAlbumRepository
import com.limelight.shared.network.immich.ImmichApiClient
import com.limelight.shared.network.immich.ImmichPersonResponse
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.TextFieldValue
import com.limelight.shared.data.remote.repository.ImmichSearchRepository
import com.limelight.shared.domain.media.SearchQuery
import com.limelight.shared.network.immich.ImmichPerson
import androidx.compose.ui.layout.ContentScale
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ImmichHomeScreen(
    state: PhotoServerState,
    actions: PhotoServerActions,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onPickAndUploadPhoto: ((onProgress: (String?) -> Unit) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val detailViewModel = remember { AssetDetailViewModel() }
    val detailState = detailViewModel.state
    var uploadProgress by remember { mutableStateOf<String?>(null) }

    val baseUrl = state.connectionConfig.baseUrl.trim()
    val isValidUrl = baseUrl.startsWith("http://") || baseUrl.startsWith("https://")

    // Sync active fullscreen detail state with the shared state for the global bottom bar
    LaunchedEffect(detailState.assetId) {
        state.activeDetailAssetId = detailState.assetId
    }

    // Map navigation states directly to the shared PhotoServerState
    var currentTab by state::currentTab
    var gridColumnCount by state::gridColumnCount
    var isBarExpanded by state::isBarExpanded

    // Pinch-to-zoom gesture state for changing grid column count
    var accumulatedZoom by remember { mutableStateOf(1f) }
    val pinchModifier = Modifier
        .pointerInput(gridColumnCount) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    
                    // Handle mouse wheel scroll with Control key
                    if (event.type == PointerEventType.Scroll) {
                        val isCtrlPressed = event.keyboardModifiers.isCtrlPressed
                        if (isCtrlPressed) {
                            val scrollAmount = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                            if (scrollAmount < 0f) {
                                val next = (gridColumnCount - 1).coerceAtLeast(2)
                                if (next != gridColumnCount) {
                                    gridColumnCount = next
                                }
                            } else if (scrollAmount > 0f) {
                                val next = (gridColumnCount + 1).coerceAtMost(6)
                                if (next != gridColumnCount) {
                                    gridColumnCount = next
                                }
                            }
                        }
                    }

                    val pressedChanges = event.changes.filter { it.pressed }
                    
                    if (pressedChanges.size >= 2) {
                        val first = pressedChanges[0]
                        val second = pressedChanges[1]
                        
                        val prevPos = first.previousPosition
                        val currentPos = first.position
                        val prevPos2 = second.previousPosition
                        val currentPos2 = second.position
                        
                        val dxPrev = prevPos.x - prevPos2.x
                        val dyPrev = prevPos.y - prevPos2.y
                        val prevDistance = kotlin.math.sqrt(dxPrev * dxPrev + dyPrev * dyPrev)
                        
                        val dxCurr = currentPos.x - currentPos2.x
                        val dyCurr = currentPos.y - currentPos2.y
                        val currentDistance = kotlin.math.sqrt(dxCurr * dxCurr + dyCurr * dyCurr)
                        
                        if (prevDistance > 0f && currentDistance > 0f) {
                            val zoomFactor = currentDistance / prevDistance
                            if (kotlin.math.abs(zoomFactor - 1f) > 0.001f) {
                                accumulatedZoom *= zoomFactor
                                
                                event.changes.forEach { it.consume() }
                                
                                if (accumulatedZoom > 1.08f) {
                                    val next = (gridColumnCount - 1).coerceAtLeast(2)
                                    if (next != gridColumnCount) {
                                        gridColumnCount = next
                                    }
                                    accumulatedZoom = 1f
                                } else if (accumulatedZoom < 0.92f) {
                                    val next = (gridColumnCount + 1).coerceAtMost(6)
                                    if (next != gridColumnCount) {
                                        gridColumnCount = next
                                    }
                                    accumulatedZoom = 1f
                                }
                            }
                        }
                    } else {
                        accumulatedZoom = 1f
                    }
                }
            }
        }

    // Search and filtering states
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Advanced search states (contextual)
    var personSearchText by remember { mutableStateOf(TextFieldValue("")) }
    var searchedAssets by remember { mutableStateOf<List<ImmichPhotoAsset>>(emptyList()) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var selectedPersonId by remember { mutableStateOf<String?>(null) }

    // Detected people list & selected person filtering
    var peopleList by remember { mutableStateOf<List<ImmichPerson>>(emptyList()) }
    var selectedPerson by remember { mutableStateOf<ImmichPerson?>(null) }
    var selectedPersonPhotos by remember { mutableStateOf<List<ImmichPhotoAsset>>(emptyList()) }
    var isPersonPhotosLoading by remember { mutableStateOf(false) }

    // Dynamic Lists from Server
    var albumsList by remember { mutableStateOf<List<com.limelight.shared.domain.media.Album>>(emptyList()) }
    var favoritesList by remember { mutableStateOf<List<ImmichPhotoAsset>>(emptyList()) }
    var selectedAlbum by remember { mutableStateOf<com.limelight.shared.domain.media.Album?>(null) }
    var selectedAlbumPhotos by remember { mutableStateOf<List<ImmichPhotoAsset>>(emptyList()) }

    // Loading statuses
    var isAlbumsLoading by remember { mutableStateOf(false) }
    var isFavoritesLoading by remember { mutableStateOf(false) }
    var isAlbumPhotosLoading by remember { mutableStateOf(false) }

    // API Clients
    val client = remember { ImmichApiClient() }
    val albumRepo = remember(state.connectionConfig) { ImmichAlbumRepository(state.connectionConfig) }

    LaunchedEffect(isValidUrl) {
        if (isValidUrl && state.galleryState is ImmichGalleryState.Idle) {
            actions.refreshImmich()
        }
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive && peopleList.isEmpty() && isValidUrl) {
            scope.launch {
                runCatching { client.getImmichPeople(state.connectionConfig) }
                    .onSuccess { peopleList = it }
                    .onFailure { it.printStackTrace() }
            }
        }
    }

    // Refresh albums/favorites and load people faces when selecting tabs
    LaunchedEffect(currentTab, state.connectionConfig) {
        if (isValidUrl) {
            if (currentTab == "albums") {
                scope.launch {
                    isAlbumsLoading = true
                    runCatching { albumRepo.listAlbums() }
                        .onSuccess { albumsList = it }
                        .onFailure { it.printStackTrace() }
                    isAlbumsLoading = false
                }
            } else if (currentTab == "favoritos") {
                scope.launch {
                    isFavoritesLoading = true
                    runCatching { client.getFavorites(state.connectionConfig) }
                        .onSuccess { favoritesList = it }
                        .onFailure { it.printStackTrace() }
                    isFavoritesLoading = false
                }
            } else if (currentTab == "fotos") {
                scope.launch {
                    runCatching { client.getImmichPeople(state.connectionConfig) }
                        .onSuccess { peopleList = it }
                        .onFailure { it.printStackTrace() }
                }
            }
        }
    }

    // Handle back actions: close full-screen media details first, or exit album/person detail view
    PlatformBackHandler(enabled = true) {
        when {
            detailState.assetId != null -> detailViewModel.load("", emptyList(), state.connectionConfig)
            selectedAlbum != null -> {
                selectedAlbum = null
                selectedAlbumPhotos = emptyList()
            }
            selectedPerson != null -> {
                selectedPerson = null
                selectedPersonPhotos = emptyList()
            }
            isSearchActive -> {
                isSearchActive = false
                searchQuery = ""
            }
            isBarExpanded -> {
                isBarExpanded = false
            }
            else -> onBack()
        }
    }

    AetherisScreen(
        primaryGlowAlignment = Alignment.TopEnd,
        secondaryGlowAlignment = Alignment.BottomStart
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!isValidUrl) {
                // Connection configuration visual prompt
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    GlassCard(contentPadding = PaddingValues(24.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Configuración Requerida",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MoonlightColors.OnSurface
                            )
                            Text(
                                text = "Para acceder a la galería multimedia nativa, introduce la dirección base y tu API Key en la pantalla de control del servidor.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MoonlightColors.OnSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 4
                            )
                            Spacer(Modifier.height(8.dp))
                            PrimaryGlassButton(
                                text = "Ir a Configuración",
                                icon = Icons.Default.Settings,
                                onClick = onOpenSettings,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                // Layout with custom glassmorphic top-bar and actual gallery content
                Column(modifier = Modifier.fillMaxSize()) {
                    // Premium Top Bar with integrated Search trigger
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                when {
                                    selectedAlbum != null -> {
                                        selectedAlbum = null
                                        selectedAlbumPhotos = emptyList()
                                    }
                                    selectedPerson != null -> {
                                        selectedPerson = null
                                        selectedPersonPhotos = emptyList()
                                    }
                                    else -> onBack()
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowBack, "Volver", tint = MoonlightColors.OnSurface)
                        }

                        if (isSearchActive) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Buscar por nombre o lugar...", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MoonlightColors.Tertiary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .padding(horizontal = 12.dp)
                            )
                        } else {
                            Text(
                                text = when {
                                    selectedAlbum != null -> selectedAlbum!!.name
                                    selectedPerson != null -> selectedPerson!!.name.ifBlank { "Persona" }
                                    currentTab == "albums" -> "Álbumes"
                                    currentTab == "favoritos" -> "Favoritos"
                                    else -> "Galería Immich"
                                },
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp),
                                color = MoonlightColors.OnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (onPickAndUploadPhoto != null) {
                                IconButton(
                                    onClick = {
                                        onPickAndUploadPhoto { progressText ->
                                            uploadProgress = progressText
                                        }
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.06f))
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                                ) {
                                    Icon(Icons.Default.CloudUpload, "Subir Fotos", tint = MoonlightColors.Tertiary)
                                }
                            }

                            IconButton(
                                onClick = {
                                    isSearchActive = !isSearchActive
                                    if (!isSearchActive) {
                                        searchQuery = ""
                                        personSearchText = TextFieldValue("")
                                        selectedPersonId = null
                                        searchedAssets = emptyList()
                                        searchError = null
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                            ) {
                                Icon(if (isSearchActive) Icons.Default.Close else Icons.Default.Search, "Buscar", tint = MoonlightColors.OnSurface)
                            }

                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                            ) {
                                Icon(Icons.Default.Settings, "Ajustes", tint = MoonlightColors.OnSurface)
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isSearchActive,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            SearchCard(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                personValue = personSearchText,
                                onPersonChange = { personSearchText = it },
                                isSearching = isSearching,
                                searchError = searchError,
                                onSearch = {
                                    val query = searchQuery.trim()
                                    val person = personSearchText.text.trim()
                                    if (query.isBlank() && person.isBlank() && selectedPersonId == null) {
                                        searchedAssets = emptyList()
                                        searchError = null
                                        return@SearchCard
                                    }
                                    isSearching = true
                                    searchError = null
                                    scope.launch {
                                        val result = runCatching {
                                            ImmichSearchRepository(state.connectionConfig).search(
                                                SearchQuery(
                                                    text = query.ifBlank { null },
                                                    person = person.ifBlank { null },
                                                    personId = selectedPersonId,
                                                    size = 100
                                                )
                                            )
                                        }
                                        result.onSuccess { response ->
                                            searchedAssets = response.page.items.map { asset ->
                                                ImmichPhotoAsset(
                                                    id = asset.id,
                                                    name = asset.fileName,
                                                    thumbnailUrl = "${state.connectionConfig.baseUrl.trimEnd('/')}/api/assets/${asset.id}/thumbnail",
                                                    createdAt = asset.createdAtIso,
                                                    location = listOfNotNull(asset.city, asset.country).joinToString(", ").ifBlank { null },
                                                    isFavorite = asset.isFavorite,
                                                    isVideo = asset.type == com.limelight.shared.domain.media.AssetType.VIDEO,
                                                    isAnimated = false
                                                )
                                            }
                                        }.onFailure { error ->
                                            searchedAssets = emptyList()
                                            searchError = error.message ?: "Error en búsqueda"
                                        }
                                        isSearching = false
                                    }
                                },
                                onClear = {
                                    searchQuery = ""
                                    personSearchText = TextFieldValue("")
                                    selectedPersonId = null
                                    searchedAssets = emptyList()
                                    searchError = null
                                },
                                people = peopleList,
                                selectedPersonId = selectedPersonId,
                                isLoadingPeople = false,
                                onLoadPeople = {},
                                onPersonQuickSelect = { selected ->
                                    val isAlreadySelected = selected.id == selectedPersonId
                                    val newPersonId = if (isAlreadySelected) null else selected.id
                                    selectedPersonId = newPersonId
                                    personSearchText = TextFieldValue(if (isAlreadySelected) "" else selected.name)
                                    
                                    val query = searchQuery.trim()
                                    val personName = personSearchText.text.trim()
                                    if (newPersonId == null && query.isBlank() && personName.isBlank()) {
                                        searchedAssets = emptyList()
                                        searchError = null
                                    } else {
                                        isSearching = true
                                        searchError = null
                                        scope.launch {
                                            val result = runCatching {
                                                ImmichSearchRepository(state.connectionConfig).search(
                                                    SearchQuery(
                                                        text = query.ifBlank { null },
                                                        person = personName.ifBlank { null },
                                                        personId = newPersonId,
                                                        size = 100
                                                    )
                                                )
                                            }
                                            result.onSuccess { response ->
                                                searchedAssets = response.page.items.map { asset ->
                                                    ImmichPhotoAsset(
                                                        id = asset.id,
                                                        name = asset.fileName,
                                                        thumbnailUrl = "${state.connectionConfig.baseUrl.trimEnd('/')}/api/assets/${asset.id}/thumbnail",
                                                        createdAt = asset.createdAtIso,
                                                        location = listOfNotNull(asset.city, asset.country).joinToString(", ").ifBlank { null },
                                                        isFavorite = asset.isFavorite,
                                                        isVideo = asset.type == com.limelight.shared.domain.media.AssetType.VIDEO,
                                                        isAnimated = false
                                                    )
                                                }
                                            }.onFailure { error ->
                                                searchedAssets = emptyList()
                                                searchError = error.message ?: "Error en búsqueda"
                                            }
                                            isSearching = false
                                        }
                                    }
                                },
                                config = state.connectionConfig
                            )
                        }
                    }

                    // Content Container
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when {
                            selectedPerson != null -> {
                                // Specific Person Timeline View
                                if (isPersonPhotosLoading) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = MoonlightColors.Tertiary, strokeWidth = 3.dp)
                                    }
                                } else {
                                    // ⚡ OPTIMIZATION: Cache calculated filtered list with remember
                                    val filteredPersonPhotos = remember(selectedPersonPhotos, searchQuery) {
                                        selectedPersonPhotos.filter {
                                            it.name.contains(searchQuery, ignoreCase = true) ||
                                                    (it.location?.contains(searchQuery, ignoreCase = true) == true)
                                        }
                                    }

                                    if (filteredPersonPhotos.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No se encontraron fotos con esta persona.", color = MoonlightColors.OnSurfaceVariant)
                                        }
                                    } else {
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(gridColumnCount),
                                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .then(pinchModifier)
                                        ) {
                                            item(span = { GridItemSpan(maxLineSpan) }) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(bottom = 12.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(MoonlightColors.Tertiary.copy(alpha = 0.1f))
                                                        .border(1.dp, MoonlightColors.Tertiary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "Filtrando por: ${selectedPerson!!.name.ifBlank { "Persona sin nombre" }}",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MoonlightColors.Tertiary
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Limpiar filtro",
                                                        tint = Color.White,
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .clickable {
                                                                selectedPerson = null
                                                                selectedPersonPhotos = emptyList()
                                                            }
                                                    )
                                                }
                                            }
                                            items(filteredPersonPhotos, key = { it.id }) { item ->
                                                PhotoGridItem(item = item, config = state.connectionConfig) {
                                                    detailViewModel.load(item.id, filteredPersonPhotos, state.connectionConfig)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            selectedAlbum != null -> {
                                // Specific Album Gallery Timeline View
                                if (isAlbumPhotosLoading) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = MoonlightColors.Tertiary, strokeWidth = 3.dp)
                                    }
                                } else {
                                    // ⚡ OPTIMIZATION: Cache calculated filtered list with remember
                                    val filteredAlbumPhotos = remember(selectedAlbumPhotos, searchQuery) {
                                        selectedAlbumPhotos.filter {
                                            it.name.contains(searchQuery, ignoreCase = true) ||
                                                    (it.location?.contains(searchQuery, ignoreCase = true) == true)
                                        }
                                    }

                                    if (filteredAlbumPhotos.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No se encontraron fotos en este álbum.", color = MoonlightColors.OnSurfaceVariant)
                                        }
                                    } else {
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(gridColumnCount),
                                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .then(pinchModifier)
                                        ) {
                                            items(filteredAlbumPhotos, key = { it.id }) { item ->
                                                PhotoGridItem(item = item, config = state.connectionConfig) {
                                                    detailViewModel.load(item.id, filteredAlbumPhotos, state.connectionConfig)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            currentTab == "albums" -> {
                                // Albums Tab Layout
                                if (isAlbumsLoading) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = MoonlightColors.Tertiary, strokeWidth = 3.dp)
                                    }
                                } else if (albumsList.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("No se encontraron álbumes creados.", color = MoonlightColors.OnSurfaceVariant)
                                    }
                                } else {
                                    // ⚡ OPTIMIZATION: Cache calculated filtered list with remember
                                    val filteredAlbums = remember(albumsList, searchQuery) {
                                        albumsList.filter { it.name.contains(searchQuery, ignoreCase = true) }
                                    }

                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(140.dp),
                                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(filteredAlbums, key = { it.id }) { album ->
                                            AlbumGridItem(album = album) {
                                                selectedAlbum = album
                                                scope.launch {
                                                    isAlbumPhotosLoading = true
                                                    runCatching { albumRepo.getAlbumAssets(album.id) }
                                                        .onSuccess { selectedAlbumPhotos = it }
                                                        .onFailure { it.printStackTrace() }
                                                    isAlbumPhotosLoading = false
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            currentTab == "favoritos" -> {
                                // Favorites Tab Layout
                                if (isFavoritesLoading) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = MoonlightColors.Tertiary, strokeWidth = 3.dp)
                                    }
                                } else if (favoritesList.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("No tienes ninguna foto marcada como favorita.", color = MoonlightColors.OnSurfaceVariant)
                                    }
                                } else {
                                    // ⚡ OPTIMIZATION: Cache calculated filtered list with remember
                                    val filteredFavorites = remember(favoritesList, searchQuery) {
                                        favoritesList.filter {
                                            it.name.contains(searchQuery, ignoreCase = true) ||
                                                    (it.location?.contains(searchQuery, ignoreCase = true) == true)
                                        }
                                    }

                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(gridColumnCount),
                                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .then(pinchModifier)
                                    ) {
                                        items(filteredFavorites, key = { it.id }) { item ->
                                            PhotoGridItem(item = item, config = state.connectionConfig) {
                                                detailViewModel.load(item.id, filteredFavorites, state.connectionConfig)
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                // Main Timeline Tab Layout
                                when (val gallery = state.galleryState) {
                                    is ImmichGalleryState.Loading -> {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                CircularProgressIndicator(color = MoonlightColors.Tertiary, strokeWidth = 3.dp)
                                                Text("Conectando al servidor nativo...", style = MaterialTheme.typography.bodyMedium, color = MoonlightColors.OnSurfaceVariant)
                                            }
                                        }
                                    }
                                    is ImmichGalleryState.Error -> {
                                        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                                            GlassCard(contentPadding = PaddingValues(24.dp)) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Text("Error de Conexión", style = MaterialTheme.typography.titleMedium, color = MoonlightColors.Error)
                                                    Text(gallery.message, style = MaterialTheme.typography.bodySmall, color = MoonlightColors.OnSurfaceVariant, textAlign = TextAlign.Center)
                                                    Spacer(Modifier.height(8.dp))
                                                    PrimaryGlassButton(
                                                        text = "Reintentar Conexión",
                                                        icon = Icons.Default.Refresh,
                                                        onClick = { scope.launch { actions.refreshImmich() } },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    else -> {
                                        // ⚡ OPTIMIZATION: Cache calculated filtered list with remember
                                        val filteredPhotos = remember(gallery, state.timelineUiModel, searchQuery, searchedAssets) {
                                            if (searchedAssets.isNotEmpty()) {
                                                searchedAssets
                                            } else {
                                                val allPhotos = (gallery as? ImmichGalleryState.Success)?.photos ?: emptyList()
                                                val workingList = allPhotos.ifEmpty { state.timelineUiModel.sections.flatMap { s -> s.items.map { i -> i.asset } } }
                                                if (searchQuery.isBlank()) {
                                                    workingList
                                                } else {
                                                    workingList.filter {
                                                        it.name.contains(searchQuery, ignoreCase = true) ||
                                                                (it.location?.contains(searchQuery, ignoreCase = true) == true)
                                                    }
                                                }
                                            }
                                        }

                                        if (filteredPhotos.isEmpty()) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text("No se encontraron fotos o vídeos.", color = MoonlightColors.OnSurfaceVariant)
                                            }
                                        } else {
                                            // Dynamic Timeline Grid
                                            val groupedTimeline = remember(filteredPhotos) {
                                                com.limelight.shared.platform.TimelineUiModel.fromPhotos(filteredPhotos)
                                            }
                                            LazyVerticalGrid(
                                                columns = GridCells.Fixed(gridColumnCount),
                                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.fillMaxSize().then(pinchModifier)
                                            ) {
                                                // 👥 Face / People Engine recognized circular scrolling Row
                                                if (peopleList.isNotEmpty()) {
                                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                                        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
                                                            Text(
                                                                text = "PERSONAS EN TU GALERÍA",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                                                                color = Color.White.copy(alpha = 0.5f),
                                                                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                                            )
                                                            LazyRow(
                                                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                                                contentPadding = PaddingValues(horizontal = 4.dp),
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                items(peopleList, key = { it.id }) { person ->
                                                                    Column(
                                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                                        modifier = Modifier
                                                                            .width(68.dp)
                                                                            .clickable {
                                                                                selectedPerson = person
                                                                                scope.launch {
                                                                                    isPersonPhotosLoading = true
                                                                                    runCatching { client.getPersonAssets(state.connectionConfig, person.id) }
                                                                                        .onSuccess { selectedPersonPhotos = it }
                                                                                        .onFailure { it.printStackTrace() }
                                                                                    isPersonPhotosLoading = false
                                                                                }
                                                                            }
                                                                    ) {
                                                                        PersonFaceImage(
                                                                            personId = person.id,
                                                                            config = state.connectionConfig,
                                                                            modifier = Modifier.size(56.dp)
                                                                        )
                                                                        Spacer(Modifier.height(4.dp))
                                                                        Text(
                                                                            text = person.name.ifBlank { "Persona" },
                                                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                                            color = Color.White.copy(alpha = 0.7f),
                                                                            maxLines = 1,
                                                                            overflow = TextOverflow.Ellipsis
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            Spacer(Modifier.height(12.dp))
                                                        }
                                                    }
                                                }

                                                groupedTimeline.sections.forEach { section ->
                                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                                        Text(
                                                            text = section.readableDate,
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                                            color = MoonlightColors.OnSurfaceVariant,
                                                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp)
                                                        )
                                                    }
                                                    items(section.items, key = { it.id }) { item ->
                                                        PhotoGridItem(item = item.asset, config = state.connectionConfig) {
                                                            detailViewModel.load(item.asset.id, filteredPhotos, state.connectionConfig)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }



                // 100% Native Fullscreen Media Detail Viewer Overlay with Horizontal Paging
                AnimatedVisibility(
                    visible = detailState.assetId != null,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (detailState.orderedAssets.isNotEmpty()) {
                        // Horizontal Swiping Pager State Controller
                        val pagerState = rememberPagerState(
                            initialPage = detailState.currentIndex,
                            pageCount = { detailState.orderedAssets.size }
                        )
                        var activePageScale by remember { mutableStateOf(1f) }

                        // Synchronize state when scrolling pages
                        LaunchedEffect(pagerState.currentPage) {
                            detailViewModel.setCurrentIndex(pagerState.currentPage)
                        }

                        // Synchronize state when loaded via grid tap
                        LaunchedEffect(detailState.currentIndex) {
                            if (pagerState.currentPage != detailState.currentIndex) {
                                pagerState.scrollToPage(detailState.currentIndex)
                            }
                        }

                        val currentAsset = detailState.currentAsset
                        if (currentAsset != null) {
                            var showInfoSheet by remember { mutableStateOf(false) }
                            var showEditorSheet by remember { mutableStateOf(false) }
                            var showDeleteConfirmation by remember { mutableStateOf(false) }

                            // Photo Editor Dynamic Parameters
                            var brightness by remember { mutableStateOf(1f) }
                            var contrast by remember { mutableStateOf(1f) }
                            var saturation by remember { mutableStateOf(1f) }

                            // Reset editor sliders whenever the page changes
                            LaunchedEffect(pagerState.currentPage) {
                                brightness = 1f
                                contrast = 1f
                                saturation = 1f
                            }

                            // Dynamic Color Filter Matrix
                            val activeColorFilter = remember(brightness, contrast, saturation) {
                                if (brightness == 1f && contrast == 1f && saturation == 1f) {
                                    null
                                } else {
                                    val matrix = ColorMatrix()
                                    matrix.setToSaturation(saturation)
                                    val vals = matrix.values
                                    vals[0] = vals[0] * contrast
                                    vals[6] = vals[6] * contrast
                                    vals[12] = vals[12] * contrast
                                    vals[4] = vals[4] + (brightness - 1f) * 255f
                                    vals[9] = vals[9] + (brightness - 1f) * 255f
                                    vals[14] = vals[14] + (brightness - 1f) * 255f
                                    ColorFilter.colorMatrix(matrix)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black)
                            ) {
                                // Dynamic Pager Container
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize(),
                                    pageSpacing = 12.dp,
                                    userScrollEnabled = activePageScale <= 1f
                                ) { pageIndex ->
                                    val asset = detailState.orderedAssets.getOrNull(pageIndex)
                                    if (asset != null) {
                                        // Pinch-to-zoom & Pan gesture state
                                        var scale by remember { mutableStateOf(1f) }
                                        var offset by remember { mutableStateOf(Offset.Zero) }
                                         // Custom video seek and timeline progress tracking states
                                         var videoDuration by remember(asset.id) { mutableStateOf(0L) }
                                         var videoPosition by remember(asset.id) { mutableStateOf(0L) }
                                         var isScrubbing by remember { mutableStateOf(false) }
                                         var scrubPosition by remember { mutableStateOf(0L) }
                                         var seekTarget by remember(asset.id) { mutableStateOf(-1L) }

                                        // Reset zoom of inactive pages
                                        LaunchedEffect(pagerState.currentPage) {
                                            if (pagerState.currentPage != pageIndex) {
                                                scale = 1f
                                                offset = Offset.Zero
                                            }
                                        }

                                        val stateTransform = rememberTransformableState { zoomChange, offsetChange, _ ->
                                            scale = (scale * zoomChange).coerceIn(1f, 5f)
                                            offset = if (scale > 1f) offset + offsetChange else Offset.Zero
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .pointerInput(Unit) {
                                                    detectTapGestures(
                                                        onDoubleTap = {
                                                            scale = if (scale > 1f) 1f else 2.5f
                                                            offset = Offset.Zero
                                                        }
                                                    )
                                                }
                                                .transformable(state = stateTransform),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (asset.isVideo) {
                                                 val streamUrl = AuthenticatedImageRequestFactory().buildVideoPlaybackUrl(state.connectionConfig.baseUrl, asset.id)
                                                 Box(modifier = Modifier.fillMaxSize()) {
                                                     PlatformVideoPlayer(
                                                         streamingUrl = streamUrl,
                                                         authConfig = state.connectionConfig,
                                                         isPlaying = detailState.isPlaying && pageIndex == pagerState.currentPage,
                                                         onDurationKnown = { videoDuration = it },
                                                         onPositionChanged = { if (!isScrubbing) videoPosition = it },
                                                         seekPosition = seekTarget,
                                                         modifier = Modifier
                                                             .fillMaxSize()
                                                             .graphicsLayer(
                                                                 scaleX = scale,
                                                                 scaleY = scale,
                                                                 translationX = offset.x,
                                                                 translationY = offset.y
                                                             )
                                                     )
                                                     if (pageIndex == pagerState.currentPage) {
                                                         Box(
                                                             modifier = Modifier
                                                                 .align(Alignment.BottomCenter)
                                                                 .padding(bottom = 120.dp, start = 20.dp, end = 20.dp)
                                                         ) {
                                                             GlassCard(
                                                                 modifier = Modifier.fillMaxWidth(),
                                                                 contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                                             ) {
                                                                 Column(
                                                                     modifier = Modifier.fillMaxWidth(),
                                                                     verticalArrangement = Arrangement.spacedBy(8.dp),
                                                                     horizontalAlignment = Alignment.CenterHorizontally
                                                                 ) {
                                                                     Row(
                                                                         modifier = Modifier.fillMaxWidth(),
                                                                         verticalAlignment = Alignment.CenterVertically,
                                                                         horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                                     ) {
                                                                         val currentPosToDisplay = if (isScrubbing) scrubPosition else videoPosition
                                                                         Text(
                                                                             text = formatVideoTime(currentPosToDisplay),
                                                                             style = MaterialTheme.typography.labelSmall,
                                                                             color = Color.White
                                                                         )
                                                                         Slider(
                                                                             value = if (isScrubbing) scrubPosition.toFloat() else videoPosition.toFloat(),
                                                                             onValueChange = {
                                                                                 isScrubbing = true
                                                                                 scrubPosition = it.toLong()
                                                                             },
                                                                             onValueChangeFinished = {
                                                                                 seekTarget = scrubPosition
                                                                                 videoPosition = scrubPosition
                                                                                 isScrubbing = false
                                                                             },
                                                                             valueRange = 0f..(if (videoDuration > 0L) videoDuration.toFloat() else 100f),
                                                                             colors = SliderDefaults.colors(
                                                                                 thumbColor = MoonlightColors.Tertiary,
                                                                                 activeTrackColor = MoonlightColors.Tertiary,
                                                                                 inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                                                                             ),
                                                                             modifier = Modifier.weight(1f)
                                                                         )
                                                                         Text(
                                                                             text = formatVideoTime(videoDuration),
                                                                             style = MaterialTheme.typography.labelSmall,
                                                                             color = Color.White.copy(alpha = 0.6f)
                                                                         )
                                                                     }
                                                                     Row(
                                                                         horizontalArrangement = Arrangement.spacedBy(28.dp),
                                                                         verticalAlignment = Alignment.CenterVertically
                                                                     ) {
                                                                         IconButton(
                                                                             onClick = {
                                                                                 val target = (videoPosition - 10000).coerceAtLeast(0L)
                                                                                 seekTarget = target
                                                                                 videoPosition = target
                                                                             }
                                                                         ) {
                                                                             Icon(
                                                                                 imageVector = Icons.Default.FastRewind,
                                                                                 contentDescription = "Retroceder 10s",
                                                                                 tint = Color.White,
                                                                                 modifier = Modifier.size(28.dp)
                                                                             )
                                                                         }
                                                                         IconButton(
                                                                             onClick = { detailViewModel.playPause() },
                                                                             modifier = Modifier
                                                                                 .size(54.dp)
                                                                                 .clip(CircleShape)
                                                                                 .background(MoonlightColors.Tertiary.copy(alpha = 0.16f))
                                                                                 .border(1.dp, MoonlightColors.Tertiary.copy(alpha = 0.4f), CircleShape)
                                                                         ) {
                                                                             Icon(
                                                                                 imageVector = if (detailState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                                                 contentDescription = if (detailState.isPlaying) "Pausar" else "Reproducir",
                                                                                 tint = MoonlightColors.Tertiary,
                                                                                 modifier = Modifier.size(32.dp)
                                                                             )
                                                                         }
                                                                         IconButton(
                                                                             onClick = {
                                                                                 val target = (videoPosition + 10000).coerceAtMost(videoDuration)
                                                                                 seekTarget = target
                                                                                 videoPosition = target
                                                                             }
                                                                         ) {
                                                                             Icon(
                                                                                 imageVector = Icons.Default.FastForward,
                                                                                 contentDescription = "Adelantar 10s",
                                                                                 tint = Color.White,
                                                                                 modifier = Modifier.size(28.dp)
                                                                             )
                                                                         }
                                                                     }
                                                                 }
                                                             }
                                                         }
                                                     }
                                                 }
                                             } else {
                                                // Animated coil loader with dynamic color adjustment filters applied!
                                                Box(
                                                    modifier = Modifier
                                                        .graphicsLayer(
                                                            scaleX = scale,
                                                            scaleY = scale,
                                                            translationX = offset.x,
                                                            translationY = offset.y
                                                        )
                                                ) {
                                                    val context = coil3.compose.LocalPlatformContext.current
                                                    val factory = remember { AuthenticatedImageRequestFactory() }
                                                    val request = factory.buildOriginalRequest(context, state.connectionConfig, asset.id)
                                                    
                                                    coil3.compose.SubcomposeAsyncImage(
                                                        model = request,
                                                        contentDescription = asset.name,
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                                        colorFilter = if (pageIndex == pagerState.currentPage) activeColorFilter else null,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        loading = {
                                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                                CircularProgressIndicator(color = MoonlightColors.Tertiary, strokeWidth = 2.dp)
                                                            }
                                                        },
                                                        error = {
                                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                                Icon(Icons.Default.BrokenImage, "Error", tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Top Action bar (Back, Deletion, Favorite Toggle, Edit, Info, Close)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.TopCenter)
                                        .padding(top = 48.dp, start = 16.dp, end = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { detailViewModel.load("", emptyList(), state.connectionConfig) },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // ✏️ Edit sliders button
                                        if (!currentAsset.isVideo) {
                                            IconButton(
                                                onClick = {
                                                    showEditorSheet = !showEditorSheet
                                                    showInfoSheet = false
                                                },
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(if (showEditorSheet) MoonlightColors.Tertiary.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f))
                                                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                            ) {
                                                Icon(Icons.Default.Edit, "Editar", tint = Color.White)
                                            }
                                        }

                                        // ⭐ Favorite star toggle
                                        IconButton(
                                            onClick = {
                                                val nextFav = !currentAsset.isFavorite
                                                scope.launch {
                                                    val ok = client.updateAssetFavorite(state.connectionConfig, currentAsset.id, nextFav)
                                                    if (ok) {
                                                        // Dynamically update the local state in-place!
                                                        val updatedAsset = currentAsset.copy(isFavorite = nextFav)
                                                        
                                                        // Update Timeline List in background
                                                        val mainGallery = state.galleryState
                                                        if (mainGallery is ImmichGalleryState.Success) {
                                                            val nextPhotos = mainGallery.photos.map {
                                                                if (it.id == currentAsset.id) updatedAsset else it
                                                            }
                                                            state.updateGallery(ImmichGalleryState.Success(mainGallery.summary, nextPhotos))
                                                        }
                                                        
                                                        // Update Album details list
                                                        selectedAlbumPhotos = selectedAlbumPhotos.map {
                                                            if (it.id == currentAsset.id) updatedAsset else it
                                                        }

                                                        // Update Favorites List dynamically
                                                        if (nextFav) {
                                                            if (favoritesList.none { it.id == currentAsset.id }) {
                                                                favoritesList = favoritesList + updatedAsset
                                                            }
                                                        } else {
                                                            favoritesList = favoritesList.filterNot { it.id == currentAsset.id }
                                                        }

                                                        // Force UI refresh by reloading detail state model
                                                        val currentTimeline = detailState.orderedAssets.map {
                                                            if (it.id == currentAsset.id) updatedAsset else it
                                                        }
                                                        detailViewModel.load(updatedAsset.id, currentTimeline, state.connectionConfig)
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.5f))
                                                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = if (currentAsset.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = "Favorito",
                                                tint = if (currentAsset.isFavorite) Color.Red else Color.White
                                            )
                                        }

                                        // ℹ️ Info Button
                                        IconButton(
                                            onClick = {
                                                showInfoSheet = !showInfoSheet
                                                showEditorSheet = false
                                            },
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(if (showInfoSheet) MoonlightColors.Tertiary.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f))
                                                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Info, "Información", tint = Color.White)
                                        }

                                        // 🗑️ Trash Can Delete Button
                                        IconButton(
                                            onClick = { showDeleteConfirmation = true },
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.5f))
                                                .border(1.dp, MoonlightColors.Error.copy(alpha = 0.4f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Delete, "Eliminar", tint = MoonlightColors.Error)
                                        }

                                        IconButton(
                                            onClick = { detailViewModel.load("", emptyList(), state.connectionConfig) },
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.5f))
                                                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Close, "Cerrar", tint = Color.White)
                                        }
                                    }
                                }

                                // Ambient bottom info overlay
                                if (!showEditorSheet && !showInfoSheet) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                                )
                                            )
                                            .padding(bottom = 36.dp, top = 24.dp)
                                            .padding(horizontal = 24.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = currentAsset.name,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                currentAsset.createdAt?.let { date ->
                                                    Text(
                                                        text = formatAssetDate(date),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color.White.copy(alpha = 0.6f)
                                                    )
                                                }
                                                currentAsset.location?.let { loc ->
                                                    Text(
                                                        text = "•  $loc",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color.White.copy(alpha = 0.7f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Slide-up EXIF and Information Bottom Sheet Modal
                                AnimatedVisibility(
                                    visible = showInfoSheet,
                                    enter = slideInVertically(initialOffsetY = { it }),
                                    exit = slideOutVertically(targetOffsetY = { it }),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                                            .background(Color.Black.copy(alpha = 0.9f))
                                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                                            .padding(24.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Detalles de Archivo", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                            IconButton(onClick = { showInfoSheet = false }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.Close, "Cerrar", tint = Color.White)
                                            }
                                        }
                                        
                                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                            InfoRow(label = "Nombre de Archivo", value = currentAsset.name)
                                            currentAsset.mimeType?.let { InfoRow(label = "Tipo MIME", value = it) }
                                            currentAsset.fileSizeInByte?.let { InfoRow(label = "Tamaño", value = formatBytes(it)) }
                                            if (currentAsset.width != null && currentAsset.height != null) {
                                                InfoRow(label = "Dimensiones", value = formatDimensions(currentAsset.width, currentAsset.height))
                                            }
                                            if (currentAsset.make != null || currentAsset.model != null) {
                                                val cam = listOfNotNull(currentAsset.make, currentAsset.model).joinToString(" ")
                                                InfoRow(label = "Cámara", value = cam)
                                            }
                                            currentAsset.location?.let { InfoRow(label = "Ubicación", value = it) }
                                            currentAsset.createdAt?.let { InfoRow(label = "Capturado el", value = it.replace("T", " ").take(19)) }
                                        }
                                        Spacer(Modifier.height(16.dp))
                                    }
                                }

                                // ✏️ GPU Photo Editor Sliders Drawer Overlay
                                AnimatedVisibility(
                                    visible = showEditorSheet,
                                    enter = slideInVertically(initialOffsetY = { it }),
                                    exit = slideOutVertically(targetOffsetY = { it }),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                                            .background(Color.Black.copy(alpha = 0.9f))
                                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                                            .padding(24.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Ajustes de Imagen (Filtros GPU)",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White
                                            )
                                            IconButton(onClick = { showEditorSheet = false }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.Close, "Cerrar", tint = Color.White)
                                            }
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            // Brightness Slider
                                            Column {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("Brillo", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                                                    Text("${(brightness * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MoonlightColors.Tertiary)
                                                }
                                                Slider(
                                                    value = brightness,
                                                    onValueChange = { brightness = it },
                                                    valueRange = 0.5f..1.5f,
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = MoonlightColors.Tertiary,
                                                        activeTrackColor = MoonlightColors.Tertiary,
                                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                                    )
                                                )
                                            }

                                            // Contrast Slider
                                            Column {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("Contraste", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                                                    Text("${(contrast * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MoonlightColors.Tertiary)
                                                }
                                                Slider(
                                                    value = contrast,
                                                    onValueChange = { contrast = it },
                                                    valueRange = 0.5f..1.5f,
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = MoonlightColors.Tertiary,
                                                        activeTrackColor = MoonlightColors.Tertiary,
                                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                                    )
                                                )
                                            }

                                            // Saturation Slider
                                            Column {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("Saturación", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                                                    Text("${(saturation * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MoonlightColors.Tertiary)
                                                }
                                                Slider(
                                                    value = saturation,
                                                    onValueChange = { saturation = it },
                                                    valueRange = 0f..2f,
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = MoonlightColors.Tertiary,
                                                        activeTrackColor = MoonlightColors.Tertiary,
                                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                                    )
                                                )
                                            }

                                            PrimaryGlassButton(
                                                text = "Restablecer Ajustes",
                                                icon = Icons.Default.Refresh,
                                                onClick = {
                                                    brightness = 1f
                                                    contrast = 1f
                                                    saturation = 1f
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        Spacer(Modifier.height(16.dp))
                                    }
                                }

                                // 🗑️ Elegant Deletion Glassmorphic Confirmation Modal popup
                                if (showDeleteConfirmation) {
                                    AlertDialog(
                                        onDismissRequest = { showDeleteConfirmation = false },
                                        title = { Text("¿Eliminar archivo permanentemente?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                                        text = { Text("Esta acción eliminará el archivo del servidor de Immich de forma irreversible.", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp) },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    showDeleteConfirmation = false
                                                    scope.launch {
                                                        val ok = client.deleteAsset(state.connectionConfig, currentAsset.id)
                                                        if (ok) {
                                                            // Close full-screen viewer
                                                            detailViewModel.load("", emptyList(), state.connectionConfig)
                                                            
                                                            // Delete from primary timeline gallery in background
                                                            val mainGallery = state.galleryState
                                                            if (mainGallery is ImmichGalleryState.Success) {
                                                                val nextPhotos = mainGallery.photos.filterNot { it.id == currentAsset.id }
                                                                state.updateGallery(ImmichGalleryState.Success(mainGallery.summary, nextPhotos))
                                                            }
                                                            
                                                            // Delete from currently selected album photos
                                                            selectedAlbumPhotos = selectedAlbumPhotos.filterNot { it.id == currentAsset.id }

                                                            // Delete from favorites list
                                                            favoritesList = favoritesList.filterNot { it.id == currentAsset.id }

                                                            // Delete from person list filter
                                                            selectedPersonPhotos = selectedPersonPhotos.filterNot { it.id == currentAsset.id }
                                                        }
                                                    }
                                                }
                                            ) {
                                                Text("Eliminar", color = MoonlightColors.Error, fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showDeleteConfirmation = false }) {
                                                Text("Cancelar", color = Color.White)
                                            }
                                        },
                                        containerColor = Color(0xFF1E1E1E),
                                        shape = RoundedCornerShape(24.dp),
                                        modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                                    )
                            }
                        }
                    }
                }
            }

            if (uploadProgress != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 90.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    GlassCard(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(horizontal = 32.dp)
                            .shadow(24.dp, shape = RoundedCornerShape(99.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MoonlightColors.Tertiary,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = uploadProgress!!,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun PersonFaceImage(
    personId: String,
    config: com.limelight.shared.data.immich.ImmichConnectionConfig,
    modifier: Modifier = Modifier
) {
    val context = coil3.compose.LocalPlatformContext.current
    val factory = remember { AuthenticatedImageRequestFactory() }
    val request = remember(personId, config) { factory.buildPeopleFaceRequest(context, config, personId) }

    var isSuccess by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.05f)),
        contentAlignment = Alignment.Center
    ) {
        coil3.compose.AsyncImage(
            model = request,
            contentDescription = "Rostro",
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            onState = { state ->
                isSuccess = state is coil3.compose.AsyncImagePainter.State.Success
                isError = state is coil3.compose.AsyncImagePainter.State.Error
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isSuccess && !isError) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.08f)))
        }

        if (isError) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Face, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun PhotoGridItem(
    item: ImmichPhotoAsset,
    config: com.limelight.shared.data.immich.ImmichConnectionConfig,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        ThumbnailImage(
            assetId = item.id,
            contentDescription = item.name,
            config = config,
            cellSize = 110.dp,
            cornerRadius = 14.dp,
            modifier = Modifier.fillMaxSize()
        )
        
        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun AlbumGridItem(
    album: com.limelight.shared.domain.media.Album,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(0.82f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Elegant Album folder representation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MoonlightColors.Tertiary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MoonlightColors.Tertiary,
                    modifier = Modifier.size(56.dp)
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            Column {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${album.assetCount} elementos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MoonlightColors.OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
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
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White, textAlign = TextAlign.End, modifier = Modifier.weight(1.5f))
    }
}

private fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) "${mb.toString().take(4)} MB" else "${kb.toString().take(4)} KB"
}

private fun formatDimensions(w: Double, h: Double): String {
    val megapixels = (w * h) / 1_000_000.0
    return "${w.toInt()} x ${h.toInt()} (${megapixels.toString().take(3)} MP)"
}

@Composable
private fun SearchCard(
    value: String,
    onValueChange: (String) -> Unit,
    personValue: TextFieldValue,
    onPersonChange: (TextFieldValue) -> Unit,
    isSearching: Boolean,
    searchError: String?,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    people: List<com.limelight.shared.network.immich.ImmichPerson>,
    selectedPersonId: String?,
    isLoadingPeople: Boolean,
    onLoadPeople: () -> Unit,
    onPersonQuickSelect: (com.limelight.shared.network.immich.ImmichPerson) -> Unit,
    config: com.limelight.shared.data.immich.ImmichConnectionConfig
) {
    val context = LocalPlatformContext.current
    val requestFactory = remember { AuthenticatedImageRequestFactory() }

    GlassCard(contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Búsqueda contextual", style = MaterialTheme.typography.titleMedium, color = MoonlightColors.OnSurface)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Buscar en Immich (e.g. playa, gato)...", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MoonlightColors.Tertiary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = personValue,
                onValueChange = onPersonChange,
                placeholder = { Text("Filtrar por nombre...", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MoonlightColors.Tertiary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            if (isLoadingPeople) {
                Box(Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MoonlightColors.Tertiary)
                }
            } else if (people.isNotEmpty()) {
                Text("Filtrar por rostro detectado", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MoonlightColors.OnSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(people) { person ->
                        val isSelected = person.id == selectedPersonId
                        val borderColor = if (isSelected) MoonlightColors.Tertiary else Color.White.copy(alpha = 0.12f)
                        val borderSize = if (isSelected) 3.dp else 1.dp
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(76.dp)
                                .clickable { onPersonQuickSelect(person) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(borderSize, borderColor, CircleShape)
                            ) {
                                SubcomposeAsyncImage(
                                    model = requestFactory.buildPeopleFaceRequest(context, config, person.id),
                                    contentDescription = person.name,
                                    contentScale = ContentScale.Crop,
                                    loading = {
                                        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.08f)))
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = person.name,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                color = if (isSelected) MoonlightColors.Tertiary else MoonlightColors.OnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onLoadPeople,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Cargar personas detectadas")
                }
            }
            
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PrimaryGlassButton(
                    text = if (isSearching) "Buscando..." else "Buscar",
                    icon = Icons.Default.Search,
                    onClick = onSearch,
                    modifier = Modifier.weight(1f),
                    enabled = !isSearching
                )
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f).height(56.dp),
                    enabled = !isSearching,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Limpiar", color = Color.White)
                }
            }
            if (searchError != null) {
                Spacer(Modifier.height(8.dp))
                Text(searchError, color = MoonlightColors.Error, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun formatAssetDate(dateString: String?): String {
    if (dateString == null) return ""
    val isoDate = dateString.take(10)
    val parts = isoDate.split("-")
    if (parts.size != 3) return isoDate
    val year = parts[0]
    val month = parts[1].toIntOrNull() ?: return isoDate
    val day = parts[2].toIntOrNull() ?: return isoDate
    
    val monthName = when (month) {
        1 -> "enero"
        2 -> "febrero"
        3 -> "marzo"
        4 -> "abril"
        5 -> "mayo"
        6 -> "junio"
        7 -> "julio"
        8 -> "agosto"
        9 -> "septiembre"
        10 -> "octubre"
        11 -> "noviembre"
        12 -> "diciembre"
        else -> null
    } ?: return isoDate
    return "$day de $monthName, $year"
}


private fun formatVideoTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}
