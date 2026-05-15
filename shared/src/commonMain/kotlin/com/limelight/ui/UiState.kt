package com.limelight.ui

import com.limelight.connection.ConnectionState
import com.limelight.discovery.PcInfo

data class UiState(
    val servers: List<PcInfo> = emptyList(),
    val selectedServer: PcInfo? = null,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
)
