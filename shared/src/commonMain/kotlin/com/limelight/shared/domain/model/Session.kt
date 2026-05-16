package com.limelight.shared.domain.model

data class Session(
    val serverUrl: String,
    val userId: String,
    val accessToken: String? = null,
    val apiKey: String? = null,
    val expiresAt: Long? = null,
)
