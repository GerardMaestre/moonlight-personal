package com.limelight.shared.sync

class ImmichSyncOrchestrator(
    private val scheduler: BackgroundSyncScheduler,
    private val syncStateFlow: SyncStateFlow = DefaultSyncStateFlow(),
) {
    fun enableBackgroundSync(
        allowMeteredNetwork: Boolean,
        requiresBatteryNotLow: Boolean = true,
        requiresCharging: Boolean = false,
    ) {
        scheduler.schedule(
            BackgroundSyncConfig(
                allowMeteredNetwork = allowMeteredNetwork,
                requiresBatteryNotLow = requiresBatteryNotLow,
                requiresCharging = requiresCharging,
            ),
        )
        syncStateFlow.update(SyncState.Idle)
    }

    fun disableBackgroundSync() {
        scheduler.cancelAll()
        syncStateFlow.update(SyncState.Idle)
    }
}
