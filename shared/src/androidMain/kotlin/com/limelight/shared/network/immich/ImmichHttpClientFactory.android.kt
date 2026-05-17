package com.limelight.shared.network.immich

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

/**
 * Para endpoints HTTP (cleartext) de desarrollo en LAN, Android bloquea tráfico no-HTTPS por defecto.
 * Configura un `networkSecurityConfig` solo para debug y limita hosts/IP locales permitidos.
 */
actual fun platformImmichHttpClient(json: Json): HttpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(json)
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 60_000
        connectTimeoutMillis = 30_000
        socketTimeoutMillis = 60_000
    }
    engine {
        config {
            connectTimeout(30, TimeUnit.SECONDS)
            readTimeout(60, TimeUnit.SECONDS)
            writeTimeout(60, TimeUnit.SECONDS)
        }
    }
    defaultRequest {
        header(HttpHeaders.CacheControl, "no-cache")
    }
}
