package com.limelight.shared.sync

import com.limelight.shared.data.remote.repository.ImmichAssetRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.Clock

class ImmichBackgroundUploadCoordinator(
    private val queueRepository: PhotoUploadQueueRepository,
    private val assetRepository: ImmichAssetRepository,
    private val bytesResolver: suspend (localUri: String) -> ByteArray,
    private val syncStateFlow: SyncStateFlow = DefaultSyncStateFlow(),
) {
    suspend fun runUploadCycle(batchSize: Int = 8, maxRetryAttempts: Int = 3) {
        syncStateFlow.update(SyncState.Scanning(lastTimestamp = currentEpochMillis()))
        val batch = queueRepository.nextBatch(limit = batchSize)
        if (batch.isEmpty()) {
            syncStateFlow.update(SyncState.Idle)
            return
        }

        batch.forEach { task ->
            val uploaded = uploadSingleTask(task, maxRetryAttempts)
            if (!uploaded) {
                return@forEach
            }
        }
        syncStateFlow.update(SyncState.Idle)
    }

    private suspend fun uploadSingleTask(task: PhotoUploadTask, maxRetryAttempts: Int): Boolean {
        var attempt = 0
        var completed = false
        while (attempt < maxRetryAttempts && !completed) {
            attempt++
            val result = runCatching {
                val bytes = bytesResolver(task.localUri)
                val createdAtStr = try {
                    kotlinx.datetime.Instant.fromEpochMilliseconds(task.createdAt).toString()
                } catch (e: Exception) {
                    null
                }
                assetRepository
                    .uploadAssetMultipartFlow(task.fileName, bytes, task.mimeType, createdAt = createdAtStr, maxRetries = 0)
                    .collectLatest { progress ->
                        val value = progress.fraction.coerceIn(0f, 1f)
                        syncStateFlow.update(SyncState.Uploading(task.localId, value))
                        if (progress.remoteId != null) {
                            queueRepository.markUploaded(task.localId, progress.remoteId)
                            completed = true
                        }
                    }
            }

            result.exceptionOrNull()?.let { error ->
                val delayMs = attempt * 1000L
                if (attempt < maxRetryAttempts) {
                    syncStateFlow.update(SyncState.Backoff(attempt = attempt, nextDelayMs = delayMs))
                    delay(delayMs)
                } else {
                    val reason = error.message ?: "Error desconocido al subir ${task.fileName}"
                    queueRepository.markFailed(task.localId, reason)
                    syncStateFlow.update(SyncState.Failed(reason))
                    return false
                }
            }
        }
        return completed
    }

    private fun currentEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
