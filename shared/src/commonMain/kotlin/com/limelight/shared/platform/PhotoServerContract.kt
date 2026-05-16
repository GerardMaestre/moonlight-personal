package com.limelight.shared.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.limelight.shared.data.immich.ImmichConnectionConfig
import com.limelight.shared.domain.SessionState
import com.limelight.shared.data.immich.ImmichGalleryState
import com.limelight.shared.network.ImmichHealthChecker
import com.limelight.shared.network.immich.ImmichApiClient
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

sealed interface PhotoServerStatus {
    data object Stopped : PhotoServerStatus
    data object Starting : PhotoServerStatus
    data class Running(val port: Int, val url: String) : PhotoServerStatus
    data class Error(val message: String) : PhotoServerStatus
}

sealed interface StartCommandResult {
    data object Success : StartCommandResult
    data class Failed(val reason: String) : StartCommandResult
}

class PhotoServerState {
    var status: PhotoServerStatus by mutableStateOf(PhotoServerStatus.Stopped)
    var lastError: String? by mutableStateOf(null)
    var lastCommandResult: StartCommandResult? by mutableStateOf(null)
    var healthMessage: String by mutableStateOf("Sin comprobaciones")
    var recentLogs: List<String> by mutableStateOf(emptyList())
    var connectionConfig: ImmichConnectionConfig by mutableStateOf(ImmichConnectionConfig())
    var galleryState: ImmichGalleryState by mutableStateOf(ImmichGalleryState.Idle)
    var timelineUiModel: TimelineUiModel by mutableStateOf(TimelineUiModel())
    var sessionState: SessionState by mutableStateOf(SessionState.Unauthenticated)

    fun updateStatus(next: PhotoServerStatus) {
        status = next
        if (next is PhotoServerStatus.Error) {
            lastError = next.message
        }
    }

    fun updateConnection(baseUrl: String, apiKey: String, bearerToken: String = connectionConfig.bearerToken) {
        connectionConfig = ImmichConnectionConfig(baseUrl = baseUrl, apiKey = apiKey, bearerToken = bearerToken)
    }

    fun updateGallery(next: ImmichGalleryState) {
        galleryState = next
        timelineUiModel = when (next) {
            is ImmichGalleryState.Success -> TimelineUiModel.fromPhotos(next.photos)
            else -> TimelineUiModel()
        }
        if (next is ImmichGalleryState.Error) {
            lastError = next.message
        }
    }
}

data class TimelineUiModel(
    val sections: List<TimelineSection> = emptyList(),
) {
    companion object {
        fun fromPhotos(photos: List<com.limelight.shared.data.immich.ImmichPhotoAsset>): TimelineUiModel {
            val grouped = photos.groupBy { photo ->
                val created = photo.createdAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
                if (created == null) "Sin fecha" else {
                    val local = created.toLocalDateTime(TimeZone.currentSystemDefault()).date
                    "${local.year}-${local.monthNumber.toString().padStart(2, '0')}-${local.dayOfMonth.toString().padStart(2, '0')}"
                }
            }
            val sections = grouped.entries.sortedByDescending { it.key }.map { (dayKey, values) ->
                TimelineSection(dayKey = dayKey, items = values.map { TimelineItem(it.id, it) })
            }
            return TimelineUiModel(sections)
        }
    }
}

data class TimelineSection(val dayKey: String, val items: List<TimelineItem>)

data class TimelineItem(val id: String, val asset: com.limelight.shared.data.immich.ImmichPhotoAsset)

interface PhotoServerActions {
    suspend fun startPhotoServer(): StartCommandResult
    fun stopPhotoServer()
    suspend fun restartPhotoServer(): StartCommandResult {
        stopPhotoServer()
        return startPhotoServer()
    }

    suspend fun refreshImmich()
    fun onUpdateConnection(baseUrl: String, apiKey: String) {}
}

class ImmichPhotoServerActions(
    private val state: PhotoServerState,
    private val client: ImmichApiClient = ImmichApiClient(),
    private val onUpdate: (String, String) -> Unit = { _, _ -> },
) : PhotoServerActions {
    override fun onUpdateConnection(baseUrl: String, apiKey: String) {
        state.updateConnection(baseUrl, apiKey)
        onUpdate(baseUrl, apiKey)
    }
    override suspend fun startPhotoServer(): StartCommandResult {
        state.sessionState = SessionState.Authenticating
        state.updateStatus(PhotoServerStatus.Starting)
        state.healthMessage = "Conectando con Immich real..."
        val health = ImmichHealthChecker.check(state.connectionConfig, client)
        return if (health.isHealthy) {
            val url = state.connectionConfig.baseUrl.trim().trimEnd('/')
            state.healthMessage = health.message
            state.updateStatus(PhotoServerStatus.Running(port = 2283, url = url))
            state.sessionState = SessionState.Authenticated(com.limelight.shared.domain.model.Session(serverUrl = url, userId = "immich"))
            refreshImmich()
            StartCommandResult.Success
        } else {
            state.healthMessage = health.message
            state.updateStatus(PhotoServerStatus.Error(health.message))
            state.sessionState = SessionState.Error(health.message)
            StartCommandResult.Failed(health.message)
        }.also { state.lastCommandResult = it }
    }

    override fun stopPhotoServer() {
        state.updateStatus(PhotoServerStatus.Stopped)
        state.sessionState = SessionState.Unauthenticated
        state.healthMessage = "Conexión cerrada"
        state.updateGallery(ImmichGalleryState.Idle)
    }

    override suspend fun refreshImmich() {
        state.updateGallery(ImmichGalleryState.Loading)
        runCatching { client.loadOverview(state.connectionConfig) }
            .onSuccess { overview ->
                state.updateGallery(ImmichGalleryState.Success(overview.summary, overview.photos))
                state.recentLogs = overview.photos.take(3).map { "${it.name} sincronizada desde Immich" }
            }
            .onFailure { error ->
                val message = error.message ?: "No se pudo cargar la galería de Immich"
                state.updateGallery(ImmichGalleryState.Error(message))
                state.healthMessage = message
            }
    }
}

object PreviewPhotoServerActions : PhotoServerActions {
    override suspend fun startPhotoServer(): StartCommandResult = StartCommandResult.Failed("Configura una instancia real de Immich")
    override fun stopPhotoServer() {}
    override suspend fun refreshImmich() {}
}
