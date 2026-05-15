package com.limelight.discovery

data class PcInfo(
    val name: String,
    val localAddress: String,
    val remoteAddress: String? = null,
    val macAddress: String? = null,
)
