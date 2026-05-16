package com.limelight.shared.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DefaultSyncStateFlow : SyncStateFlow {
    private val mutable = MutableStateFlow<SyncState>(SyncState.Idle)
    override val state: StateFlow<SyncState> = mutable

    override fun update(next: SyncState) {
        mutable.value = next
    }
}
