package com.limelight.shared.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.limelight.sync.PhotoSyncWorker
import java.util.concurrent.TimeUnit

actual class BackgroundSyncScheduler actual constructor(platformContext: Any?) {
    private val context: Context = requireNotNull(platformContext as? Context) { "BackgroundSyncScheduler requires Android Context" }

    actual fun schedule(config: BackgroundSyncConfig) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (config.allowMeteredNetwork) NetworkType.CONNECTED else NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(config.requiresBatteryNotLow)
            .setRequiresCharging(config.requiresCharging)
            .build()
        val request = PeriodicWorkRequestBuilder<PhotoSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PhotoSyncWorker.UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    actual fun cancelAll() { WorkManager.getInstance(context).cancelUniqueWork(PhotoSyncWorker.UNIQUE_WORK_NAME) }
}
