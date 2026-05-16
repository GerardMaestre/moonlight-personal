package com.limelight.shared.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

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
 * Handles authentication via PocketBase JWT tokens and device wake requests.
 * Uses standard Java HttpURLConnection for compatibility across Android and Desktop (JVM).
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
        return try {
            val token = if (username.isNotBlank()) authenticate() else ""
            
            if (username.isNotBlank() && token.isEmpty()) {
                return WakeResult.Error("Error de autenticación. Verifica usuario y contraseña.")
            }

            sendWakeRequest(deviceId, token)
        } catch (e: Exception) {
            println("[UpSnapClient] Wake failed: ${e.message}")
            WakeResult.Error("Error: ${e.message ?: "desconocido"}")
        }
    }

    fun listDevices(): List<Pair<String, String>> {
        println("[UpSnapClient] Listing devices for URL: $serverUrl")
        val token = if (username.isNotBlank()) authenticate() else ""
        
        val base = serverUrl.trim().let { 
            if (!it.startsWith("http")) "http://$it" else it 
        }.trimEnd('/')
        val url = URL("$base/api/collections/devices/records")
        val conn = createConnection(url)
        conn.requestMethod = "GET"
        if (token.isNotEmpty()) conn.setRequestProperty("Authorization", "Bearer $token")
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT

        val responseCode = conn.responseCode
        if (responseCode == 200) {
            val body = conn.inputStream.bufferedReader().readText()
            println("[UpSnapClient] Raw response: $body")
            val response = json.decodeFromString<DeviceListResponse>(body)
            return response.items.map { it.id to it.name }
        } else {
            val errorBody = try {
                conn.errorStream?.bufferedReader()?.readText() ?: "Código $responseCode"
            } catch (ex: Exception) {
                "Código $responseCode"
            }
            throw Exception("Error del servidor ($responseCode): $errorBody")
        }
    }

    private fun authenticate(urlOverride: URL? = null): String {
        val authUrl = try {
            val base = serverUrl.trim().let { 
                if (!it.startsWith("http")) "http://$it" else it 
            }.trimEnd('/')
            urlOverride ?: URL("$base/api/collections/users/auth-with-password")
        } catch (e: Exception) {
            return ""
        }
        
        val conn = createConnection(authUrl)
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT

        val isSuperUser = authUrl.toString().contains("/_superusers/") || authUrl.toString().contains("/admins/")
        val body = if (isSuperUser) {
            // Newer PocketBase versions (0.23+) use identity for superusers too, but older ones used email.
            // We send both for maximum compatibility.
            json.encodeToString(AuthRequest.serializer(), AuthRequest(identity = username.trim(), email = username.trim(), password = password))
        } else {
            json.encodeToString(AuthRequest.serializer(), AuthRequest(identity = username.trim(), password = password))
        }
        
        println("[UpSnapClient] Sending auth request to $authUrl with identity: ${username.trim()}")

        conn.outputStream.bufferedWriter().use { it.write(body) }

        try {
            val responseCode = conn.responseCode
            println("[UpSnapClient] Auth response code: $responseCode for $authUrl")
            return if (responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val token = json.decodeFromString<AuthResponse>(response).token
                println("[UpSnapClient] Auth success, token length: ${token.length}")
                token
            } else if (responseCode == 400 && authUrl.toString().contains("/collections/users/")) {
                println("[UpSnapClient] Trying SuperUser auth fallback...")
                val superUserUrl = URL(authUrl.toString().replace("/collections/users/", "/collections/_superusers/"))
                authenticate(superUserUrl)
            } else if (responseCode == 404 && authUrl.toString().contains("/collections/_superusers/")) {
                println("[UpSnapClient] Trying Admin auth fallback...")
                val adminUrl = URL(authUrl.toString().replace("/collections/_superusers/", "/admins/"))
                authenticate(adminUrl)
            } else {
                val errorMsg = try {
                    conn.errorStream?.bufferedReader()?.readText() ?: "Sin respuesta"
                } catch (ex: Exception) {
                    "Fallo HTTP $responseCode"
                }
                println("[UpSnapClient] Auth failed ($responseCode): $errorMsg")
                throw Exception("Fallo de autenticación ($responseCode): $errorMsg")
            }
        } catch (e: java.net.SocketTimeoutException) {
            println("[UpSnapClient] Auth TIMEOUT for $authUrl")
            throw Exception("Timeout de conexión con el servidor")
        } catch (e: java.net.ConnectException) {
            println("[UpSnapClient] Auth CONNECTION REFUSED for $authUrl")
            throw Exception("Conexión rechazada. ¿Está el servidor encendido?")
        } catch (e: Exception) {
            println("[UpSnapClient] Auth unexpected error: ${e.message}")
            throw e
        }
    }

    private fun sendWakeRequest(deviceId: String, token: String): WakeResult {
        val base = serverUrl.trim().let { 
            if (!it.startsWith("http")) "http://$it" else it 
        }.trimEnd('/')
        val url = URL("$base/api/upsnap/wake/$deviceId")
        val conn = createConnection(url)
        conn.requestMethod = "GET"
        if (token.isNotEmpty()) conn.setRequestProperty("Authorization", "Bearer $token")
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT

        return when (conn.responseCode) {
            200, 204 -> WakeResult.Success
            401 -> WakeResult.Error("Contraseña incorrecta o usuario no válido.")
            403 -> WakeResult.Error("No tienes permiso para despertar este dispositivo.")
            404 -> WakeResult.Error("ID de dispositivo no encontrado.")
            else -> WakeResult.Error("Error del servidor (${conn.responseCode})")
        }
    }

    private fun createConnection(url: URL): HttpURLConnection {
        val conn = url.openConnection() as HttpURLConnection
        if (conn is HttpsURLConnection) {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            })
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            conn.sslSocketFactory = sslContext.socketFactory
            conn.hostnameVerifier = javax.net.ssl.HostnameVerifier { _, _ -> true }
        }
        return conn
    }
}
