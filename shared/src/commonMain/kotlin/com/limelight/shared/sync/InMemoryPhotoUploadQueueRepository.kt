package com.limelight.shared.sync

class InMemoryPhotoUploadQueueRepository : PhotoUploadQueueRepository {
    private data class QueueItem(
        val task: PhotoUploadTask,
        val uploadedRemoteId: String? = null,
        val failedReason: String? = null,
        val cancelled: Boolean = false,
    )

    private val storage = linkedMapOf<String, QueueItem>()

    override suspend fun enqueueIfMissing(task: PhotoUploadTask): Boolean {
        if (storage.containsKey(task.localId)) {
            return false
        }
        storage[task.localId] = QueueItem(task = task)
        return true
    }

    override suspend fun nextBatch(limit: Int): List<PhotoUploadTask> {
        val sanitizedLimit = limit.coerceAtLeast(1)
        return storage.values.asSequence()
            .filter { item -> item.uploadedRemoteId == null && item.failedReason == null && !item.cancelled }
            .map { item -> item.task }
            .take(sanitizedLimit)
            .toList()
    }

    override suspend fun markUploaded(localId: String, remoteId: String) {
        val current = storage[localId] ?: return
        storage[localId] = current.copy(uploadedRemoteId = remoteId, failedReason = null)
    }

    override suspend fun markFailed(localId: String, reason: String) {
        val current = storage[localId] ?: return
        storage[localId] = current.copy(failedReason = reason)
    }

    override suspend fun cancel(localId: String) {
        val current = storage[localId] ?: return
        storage[localId] = current.copy(cancelled = true)
    }
}
