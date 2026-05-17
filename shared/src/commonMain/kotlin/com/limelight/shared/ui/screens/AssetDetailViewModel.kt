package com.limelight.shared.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.limelight.shared.data.immich.ImmichConnectionConfig
import com.limelight.shared.data.immich.ImmichPhotoAsset

class AssetDetailViewModel {
    var state: AssetDetailState by mutableStateOf(AssetDetailState())
        private set

    fun load(assetId: String, timeline: List<ImmichPhotoAsset>, config: ImmichConnectionConfig) {
        val index = timeline.indexOfFirst { it.id == assetId }.coerceAtLeast(0)
        state = AssetDetailState(assetId = assetId.ifBlank { null }, orderedAssets = timeline, currentIndex = index, config = config)
        prefetchAround()
    }

    fun setCurrentIndex(index: Int) {
        if (index in state.orderedAssets.indices) {
            state = state.copy(currentIndex = index, assetId = state.orderedAssets[index].id)
            prefetchAround()
        }
    }

    fun showMetadata(show: Boolean) { state = state.copy(showMetadata = show) }
    fun playPause() { state = state.copy(isPlaying = !state.isPlaying) }
    fun seekTo(progress: Float) { state = state.copy(playbackProgress = progress.coerceIn(0f, 1f)) }
    fun next() { if (state.currentIndex < state.orderedAssets.lastIndex) { state = state.copy(currentIndex = state.currentIndex + 1); prefetchAround() } }
    fun previous() { if (state.currentIndex > 0) { state = state.copy(currentIndex = state.currentIndex - 1); prefetchAround() } }

    private fun prefetchAround() {
        val i = state.currentIndex
        state = state.copy(prefetchedIds = listOfNotNull(state.orderedAssets.getOrNull(i - 1)?.id, state.orderedAssets.getOrNull(i + 1)?.id))
    }
}

data class AssetDetailState(
    val assetId: String? = null,
    val orderedAssets: List<ImmichPhotoAsset> = emptyList(),
    val currentIndex: Int = 0,
    val config: ImmichConnectionConfig = ImmichConnectionConfig(),
    val isPlaying: Boolean = true,
    val playbackProgress: Float = 0f,
    val showMetadata: Boolean = false,
    val prefetchedIds: List<String> = emptyList(),
) {
    val currentAsset: ImmichPhotoAsset? get() = orderedAssets.getOrNull(currentIndex)
}
