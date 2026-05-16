package com.limelight.shared.network.immich.auth

import kotlinx.serialization.Serializable

@Serializable
data class ImmichLoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class ImmichLoginResponse(
    val accessToken: String,
    val userId: String,
    val userEmail: String? = null,
    val name: String? = null,
)

@Serializable
data class ImmichSessionUserResponse(
    val id: String,
    val email: String? = null,
    val name: String? = null,
)
