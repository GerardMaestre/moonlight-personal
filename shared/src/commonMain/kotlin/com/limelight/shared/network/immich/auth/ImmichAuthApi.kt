package com.limelight.shared.network.immich.auth

import com.limelight.shared.network.immich.executeImmichRequest
import com.limelight.shared.network.immich.normalizedBaseUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ImmichAuthApi(
    private val httpClient: HttpClient,
) {
    suspend fun loginWithPassword(serverUrl: String, email: String, password: String): ImmichLoginResponse = executeImmichRequest {
        httpClient.post("${normalizedBaseUrl(serverUrl)}/api/auth/login") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(ImmichLoginRequest(email, password))
        }.body()
    }

    suspend fun validateSession(serverUrl: String, accessToken: String? = null, apiKey: String? = null): ImmichSessionUserResponse = executeImmichRequest {
        httpClient.get("${normalizedBaseUrl(serverUrl)}/api/users/me") {
            accept(ContentType.Application.Json)
            when {
                !apiKey.isNullOrBlank() -> header("x-api-key", apiKey)
                !accessToken.isNullOrBlank() -> bearerAuth(accessToken)
                else -> error("Missing credentials")
            }
        }.body()
    }
}
