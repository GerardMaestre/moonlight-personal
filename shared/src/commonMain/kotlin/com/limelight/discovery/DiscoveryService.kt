package com.limelight.discovery

interface DiscoveryService {
    suspend fun discover(timeoutMs: Long = 3_000): List<PcInfo>
}
