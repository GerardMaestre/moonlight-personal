package com.limelight.shared.model

/**
 * Network streaming profile with optimized settings for different connectivity scenarios.
 */
data class NetworkProfile(
    val id: String,
    val name: String,
    val description: String,
    val resolution: String,
    val fps: Int,
    val bitrateKbps: Int,
    val videoFormat: String,
    val framePacing: String,
    val icon: ProfileIcon
)

enum class ProfileIcon {
    HOME, MOBILE_5G, DATA_SAVER
}

object NetworkProfiles {
    val HOME = NetworkProfile(
        id = "home",
        name = "Casa",
        description = "1080p60 · 50 Mbps · Codec auto · Pacing equilibrado",
        resolution = "1920x1080",
        fps = 60,
        bitrateKbps = 50000,
        videoFormat = "auto",
        framePacing = "balanced",
        icon = ProfileIcon.HOME
    )

    val FIVE_G = NetworkProfile(
        id = "5g",
        name = "5G",
        description = "1080p60 · 30 Mbps · HEVC · Mínima latencia",
        resolution = "1920x1080",
        fps = 60,
        bitrateKbps = 30000,
        videoFormat = "forceh265",
        framePacing = "latency",
        icon = ProfileIcon.MOBILE_5G
    )

    val SAVER = NetworkProfile(
        id = "saver",
        name = "Ahorro",
        description = "720p60 · 10 Mbps · HEVC · Mínima latencia",
        resolution = "1280x720",
        fps = 60,
        bitrateKbps = 10000,
        videoFormat = "forceh265",
        framePacing = "latency",
        icon = ProfileIcon.DATA_SAVER
    )

    val all = listOf(HOME, FIVE_G, SAVER)
}
