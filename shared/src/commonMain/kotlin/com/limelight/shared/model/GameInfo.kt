package com.limelight.shared.model

data class GameInfo(
    val id: Int,
    val name: String,
    val status: String = "Ready",
    val boxArtUrl: String? = null,
    val isHdrSupported: Boolean = false
)
