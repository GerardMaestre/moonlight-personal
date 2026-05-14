package com.limelight.shared.model

/**
 * Platform-independent representation of a gaming PC.
 * Maps to ComputerDetails on Android, uses mock data on Desktop for previews.
 */
data class ComputerInfo(
    val id: String,
    val name: String,
    val status: ComputerStatus,
    val localAddress: String? = null,
    val remoteAddress: String? = null,
    val macAddress: String? = null,
    val isPaired: Boolean = false,
    val runningGameId: Int = 0,
    val isNvidiaServer: Boolean = true
) {
    val isOnline: Boolean get() = status == ComputerStatus.ONLINE
    val isStreaming: Boolean get() = runningGameId != 0
    val statusLabel: String get() = when {
        isStreaming -> "Streaming"
        isOnline -> "Ready to stream"
        status == ComputerStatus.OFFLINE -> "Offline"
        else -> "Unknown"
    }
}

enum class ComputerStatus {
    ONLINE, OFFLINE, UNKNOWN
}
