package com.limelight.shared.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

sealed interface PhotoServerStatus {
    data object Stopped : PhotoServerStatus
    data object Starting : PhotoServerStatus
    data class Running(val port: Int, val url: String) : PhotoServerStatus
    data class Error(val message: String) : PhotoServerStatus
}

class PhotoServerState {
    var status: PhotoServerStatus by mutableStateOf(PhotoServerStatus.Stopped)
    var lastError: String? by mutableStateOf(null)

    fun updateStatus(next: PhotoServerStatus) {
        status = next
        if (next is PhotoServerStatus.Error) {
            lastError = next.message
        }
    }
}

interface PhotoServerActions {
    fun startPhotoServer()
    fun stopPhotoServer()
    fun restartPhotoServer() {
        stopPhotoServer()
        startPhotoServer()
    }
}

object PreviewPhotoServerActions : PhotoServerActions {
    override fun startPhotoServer() {}
    override fun stopPhotoServer() {}
}
