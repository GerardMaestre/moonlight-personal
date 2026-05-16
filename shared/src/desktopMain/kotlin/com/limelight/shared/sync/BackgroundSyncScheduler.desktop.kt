package com.limelight.shared.sync

actual class BackgroundSyncScheduler actual constructor(platformContext: Any?) {
    actual fun schedule(config: BackgroundSyncConfig) = Unit
    actual fun cancelAll() = Unit
}
