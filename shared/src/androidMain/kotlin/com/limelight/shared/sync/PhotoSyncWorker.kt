package com.limelight.shared.sync

import android.content.Context
import android.provider.MediaStore
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

class PhotoSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val queue: PhotoUploadQueueRepository = InMemoryPhotoUploadQueueRepository()
    private val syncState = DefaultSyncStateFlow()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            enqueueIncrementalScan()
            syncState.update(SyncState.Idle)
            Result.success()
        } catch (e: Throwable) {
            syncState.update(SyncState.Failed(e.message ?: "sync error"))
            Result.retry()
        }
    }

    private suspend fun enqueueIncrementalScan() {
        val prefs = applicationContext.getSharedPreferences("photo_sync", Context.MODE_PRIVATE)
        val lastTimestamp = prefs.getLong("last_timestamp", 0L)
        val lastId = prefs.getLong("last_id", 0L)
        syncState.update(SyncState.Scanning(lastTimestamp))

        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_MODIFIED, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.MIME_TYPE)
        val selection = "${MediaStore.Images.Media.DATE_MODIFIED} > ? OR (${MediaStore.Images.Media.DATE_MODIFIED} = ? AND ${MediaStore.Images.Media._ID} > ?)"
        val args = arrayOf(lastTimestamp.toString(), lastTimestamp.toString(), lastId.toString())
        applicationContext.contentResolver.query(uri, projection, selection, args, "${MediaStore.Images.Media.DATE_MODIFIED} ASC, ${MediaStore.Images.Media._ID} ASC")?.use { c ->
            var maxTimestamp = lastTimestamp
            var maxId = lastId
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val modified = c.getLong(1)
                val name = c.getString(2) ?: "$id.jpg"
                val mime = c.getString(3) ?: "image/jpeg"
                val localUri = "$uri/$id"
                val hash = sha256("$id:$modified:$name")
                queue.enqueueIfMissing(PhotoUploadTask(localId = id.toString(), fileName = name, mimeType = mime, localUri = localUri, hash = hash, createdAt = modified))
                if (modified > maxTimestamp || (modified == maxTimestamp && id > maxId)) {
                    maxTimestamp = modified
                    maxId = id
                }
            }
            prefs.edit().putLong("last_timestamp", maxTimestamp).putLong("last_id", maxId).apply()
        }
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object { const val UNIQUE_WORK_NAME = "photo_sync_worker" }
}

class InMemoryPhotoUploadQueueRepository : PhotoUploadQueueRepository {
    private val byLocalId = ConcurrentHashMap<String, PhotoUploadTask>()
    private val byHash = ConcurrentHashMap<String, String>()
    override suspend fun enqueueIfMissing(task: PhotoUploadTask): Boolean {
        if (byLocalId.containsKey(task.localId)) return false
        if (byHash.containsKey(task.hash)) return false
        byLocalId[task.localId] = task
        byHash[task.hash] = task.localId
        return true
    }
    override suspend fun nextBatch(limit: Int): List<PhotoUploadTask> = byLocalId.values.take(limit)
    override suspend fun markUploaded(localId: String, remoteId: String) { byLocalId.remove(localId)?.also { byHash.remove(it.hash) } }
    override suspend fun markFailed(localId: String, reason: String) = Unit
    override suspend fun cancel(localId: String) { byLocalId.remove(localId)?.also { byHash.remove(it.hash) } }
}
