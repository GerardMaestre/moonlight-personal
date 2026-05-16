package com.limelight.shared.sync

import kotlinx.coroutines.flow.StateFlow

expect class BackgroundSyncScheduler(platformContext: Any?) {
    fun schedule(config: BackgroundSyncConfig)
    fun cancelAll()
}

data class BackgroundSyncConfig(
    val allowMeteredNetwork: Boolean = false,
    val requiresBatteryNotLow: Boolean = true,
    val requiresCharging: Boolean = false,
)

data class PhotoUploadTask(
    val localId: String,
    val fileName: String,
    val mimeType: String,
    val localUri: String,
    val hash: String,
    val createdAt: Long,
)

interface PhotoUploadQueueRepository {
    suspend fun enqueueIfMissing(task: PhotoUploadTask): Boolean
    suspend fun nextBatch(limit: Int): List<PhotoUploadTask>
    suspend fun markUploaded(localId: String, remoteId: String)
    suspend fun markFailed(localId: String, reason: String)
    suspend fun cancel(localId: String)
}

sealed interface SyncState { data object Idle : SyncState; data class Scanning(val lastTimestamp: Long) : SyncState; data class Uploading(val localId: String, val progress: Float) : SyncState; data class Backoff(val attempt: Int, val nextDelayMs: Long) : SyncState; data class Conflict(val localId: String, val reason: String) : SyncState; data class Failed(val reason: String) : SyncState }
interface SyncStateFlow { val state: StateFlow<SyncState>; fun update(next: SyncState) }
