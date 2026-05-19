package com.limelight.shared.network

import kotlinx.coroutines.flow.StateFlow

data class DiscoveredHost(
    val id: String, // IP Address
    val name: String,
    val port: Int,
    val uuid: String
)

interface MultiplatformDiscoveryManager {
    val discoveredHosts: StateFlow<List<DiscoveredHost>>
    
    fun startDiscovery()
    fun stopDiscovery()
}

/**
 * Platform-specific Multicast DNS (mDNS) listener interface.
 */
expect class PlatformMdnsScanner(onHostDiscovered: (DiscoveredHost) -> Unit) {
    fun startScanning()
    fun stopScanning()
}
