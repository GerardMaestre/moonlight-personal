package com.limelight.shared.network

import com.limelight.shared.network.defaultNetworkHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class AuthRequest(val identity: String? = null, val email: String? = null, val password: String)

@Serializable
private data class AuthResponse(val token: String)

@Serializable
private data class DeviceItem(val id: String, val name: String)

@Serializable
private data class DeviceListResponse(val items: List<DeviceItem>)

/**
 * Client for the UpSnap Wake-on-LAN API.
 *
 * Uses Ktor Client for multiplatform HTTP requests.
 */
class UpSnapClient(
    private val serverUrl: String,
    private val username: String,
    private val password: String
) {
    companion object {
        private const val CONNECT_TIMEOUT = 10_000
        private const val READ_TIMEOUT = 15_000
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }

    sealed class WakeResult {
        data object Success : WakeResult()
        data class Error(val message: String) : WakeResult()
    }

    fun wakeDevice(deviceId: String): WakeResult {
        return runBlocking(Dispatchers.IO) {
            try {
                val token = if (username.isNotBlank()) authenticate() else ""
                if (username.isNotBlank() && token.isEmpty()) {
                    return@runBlocking WakeResult.Error("Error de autenticación. Verifica usuario y contraseña.")
                }
                sendWakeRequest(deviceId, token)
            } catch (e: Exception) {
                println("[UpSnapClient] Wake failed: ${e.message}")
                WakeResult.Error("Error: ${e.message ?: "desconocido"}")
            }
        }
    }

    fun listDevices(): List<Pair<String, String>> {
        return runBlocking(Dispatchers.IO) {
            listDevicesInternal()
        }
    }

    private suspend fun listDevicesInternal(): List<Pair<String, String>> {
        println("[UpSnapClient] Listing devices for URL: $serverUrl")
        val token = if (username.isNotBlank()) authenticate() else ""
        val endpoint = normalizeServerUrl(serverUrl) + "/api/collections/devices/records"

        return defaultNetworkHttpClient(json).use { client ->
            val response = client.get(endpoint) {
                if (token.isNotEmpty()) header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                println("[UpSnapClient] Raw response: $body")
                val responseDto = json.decodeFromString<DeviceListResponse>(body)
                responseDto.items.map { it.id to it.name }
            } else {
                val errorBody = readErrorBody(response)
                throw Exception("Error del servidor (${response.status.value}): $errorBody")
            }
        }
    }

    private suspend fun authenticate(urlOverride: String? = null): String {
        val authEndpoint = try {
            val base = normalizeServerUrl(serverUrl)
            urlOverride ?: "$base/api/collections/users/auth-with-password"
        } catch (e: Exception) {
            return ""
        }

        val isSuperUser = authEndpoint.contains("/_superusers/") || authEndpoint.contains("/admins/")
        val bodyPayload = if (isSuperUser) {
            AuthRequest(identity = username.trim(), email = username.trim(), password = password)
        } else {
            AuthRequest(identity = username.trim(), password = password)
        }

        println("[UpSnapClient] Sending auth request to $authEndpoint with identity: ${username.trim()}")

        return defaultNetworkHttpClient(json).use { client ->
            val response = client.post(authEndpoint) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
                setBody(bodyPayload)
            }
            println("[UpSnapClient] Auth response code: ${response.status.value} for $authEndpoint")

            when {
                response.status == HttpStatusCode.OK -> {
                    val responseBody = response.bodyAsText()
                    val token = json.decodeFromString<AuthResponse>(responseBody).token
                    println("[UpSnapClient] Auth success, token length: ${token.length}")
                    token
                }
                response.status == HttpStatusCode.BadRequest && authEndpoint.contains("/collections/users/") -> {
                    println("[UpSnapClient] Trying SuperUser auth fallback...")
                    authenticate(authEndpoint.replace("/collections/users/", "/collections/_superusers/"))
                }
                response.status == HttpStatusCode.NotFound && authEndpoint.contains("/collections/_superusers/") -> {
                    println("[UpSnapClient] Trying Admin auth fallback...")
                    authenticate(authEndpoint.replace("/collections/_superusers/", "/admins/"))
                }
                else -> {
                    val errorMsg = readErrorBody(response)
                    println("[UpSnapClient] Auth failed (${response.status.value}): $errorMsg")
                    throw Exception("Fallo de autenticación (${response.status.value}): $errorMsg")
                }
            }
        }
    }

    private suspend fun sendWakeRequest(deviceId: String, token: String): WakeResult {
        val endpoint = normalizeServerUrl(serverUrl) + "/api/upsnap/wake/$deviceId"

        return defaultNetworkHttpClient(json).use { client ->
            val response = client.get(endpoint) {
                if (token.isNotEmpty()) header(HttpHeaders.Authorization, "Bearer $token")
            }
            when (response.status) {
                HttpStatusCode.OK, HttpStatusCode.NoContent -> WakeResult.Success
                HttpStatusCode.Unauthorized -> WakeResult.Error("Contraseña incorrecta o usuario no válido.")
                HttpStatusCode.Forbidden -> WakeResult.Error("No tienes permiso para despertar este dispositivo.")
                HttpStatusCode.NotFound -> WakeResult.Error("ID de dispositivo no encontrado.")
                else -> WakeResult.Error("Error del servidor (${response.status.value}): ${readErrorBody(response)}")
            }
        }
    }

    private suspend fun readErrorBody(response: HttpResponse): String {
        return try {
            response.bodyAsText().takeIf { it.isNotBlank() } ?: "Código ${response.status.value}"
        } catch (e: Exception) {
            "Código ${response.status.value}"
        }
    }

    private fun normalizeServerUrl(url: String): String = url.trim().let {
        val normalized = if (!it.startsWith("http")) "http://$it" else it
        normalized.trimEnd('/')
    }
}
