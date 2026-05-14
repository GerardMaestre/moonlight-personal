package com.limelight.ui.premium

import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Client for the UpSnap Wake-on-LAN API.
 * 
 * Handles authentication via PocketBase JWT tokens and device wake requests.
 * Supports self-signed certificates for local/Tailscale networks.
 */
class UpSnapClient(
    private val serverUrl: String,
    private val username: String,
    private val password: String
) {
    companion object {
        private const val TAG = "UpSnapClient"
        private const val CONNECT_TIMEOUT = 10_000 // 10 seconds
        private const val READ_TIMEOUT = 15_000    // 15 seconds
    }

    /**
     * Result of a wake operation.
     */
    sealed class WakeResult {
        data object Success : WakeResult()
        data class Error(val message: String) : WakeResult()
    }

    /**
     * Authenticate and wake the device in a single call.
     * This method is blocking and should be called from a background thread.
     */
    fun wakeDevice(deviceId: String): WakeResult {
        return try {
            // Step 1: Authenticate to get JWT token (only if credentials provided)
            val token = if (username.isNotBlank()) {
                authenticate()
            } else {
                ""
            }
            
            if (username.isNotBlank() && token.isEmpty()) {
                return WakeResult.Error("Error de autenticación. Verifica usuario y contraseña.")
            }

            // Step 2: Send wake request
            sendWakeRequest(deviceId, token)
        } catch (e: java.net.ConnectException) {
            WakeResult.Error("No se puede conectar al servidor UpSnap. ¿Está encendido?")
        } catch (e: java.net.SocketTimeoutException) {
            WakeResult.Error("Timeout al conectar con UpSnap. Verifica la red.")
        } catch (e: Exception) {
            Log.e(TAG, "Wake failed", e)
            WakeResult.Error("Error: ${e.localizedMessage ?: "desconocido"}")
        }
    }

    /**
     * Lists available devices on the UpSnap server.
     * Returns a list of pairs (deviceId, deviceName).
     */
    fun listDevices(): List<Pair<String, String>> {
        return try {
            val token = if (username.isNotBlank()) authenticate() else ""
            if (username.isNotBlank() && token.isEmpty()) return emptyList()
            
            val base = serverUrl.trim().let { 
                if (!it.startsWith("http")) "http://$it" else it 
            }.trimEnd('/')
            val url = URL("$base/api/collections/devices/records")
            val conn = createConnection(url)
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.connectTimeout = CONNECT_TIMEOUT
            conn.readTimeout = READ_TIMEOUT

            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(body)
                val items = json.getJSONArray("items")
                val result = mutableListOf<Pair<String, String>>()
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    result.add(Pair(item.getString("id"), item.getString("name")))
                }
                result
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "listDevices failed", e)
            emptyList()
        }
    }

    private fun authenticate(url: URL? = null): String {
        val authUrl = try {
            val base = serverUrl.trim().let { 
                if (!it.startsWith("http")) "http://$it" else it 
            }.trimEnd('/')
            url ?: URL("$base/api/collections/users/auth-with-password")
        } catch (e: Exception) {
            Log.e(TAG, "Invalid URL: $serverUrl", e)
            return ""
        }
        
        val conn = createConnection(authUrl)
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT

        val body = JSONObject().apply {
            if (authUrl.toString().contains("/admins/")) {
                put("email", username.trim())
            } else {
                put("identity", username.trim())
            }
            put("password", password)
        }

        try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body.toString())
                writer.flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write body to $authUrl", e)
            return ""
        }

        return if (conn.responseCode == 200) {
            val response = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).readText()
            JSONObject(response).optString("token", "")
        } else if (conn.responseCode == 400 && authUrl.toString().contains("/collections/users/")) {
            // Try modern admin auth fallback (PocketBase v0.23+)
            Log.i(TAG, "User auth 400, trying _superusers auth...")
            val superUserUrl = URL(authUrl.toString().replace("/collections/users/", "/collections/_superusers/"))
            return authenticate(superUserUrl)
        } else if (conn.responseCode == 404 && authUrl.toString().contains("/collections/_superusers/")) {
            // Try legacy admin auth fallback (PocketBase < v0.23)
            Log.i(TAG, "Superusers auth 404, trying legacy admins auth...")
            val adminUrl = URL(authUrl.toString().replace("/collections/_superusers/", "/admins/"))
            return authenticate(adminUrl)
        } else {
            val errorBody = try {
                conn.errorStream?.bufferedReader()?.readText() ?: "No error body"
            } catch (_: Exception) { "Error reading error body" }
            Log.w(TAG, "Auth failed (${conn.responseCode}) at $authUrl: $errorBody")
            ""
        }
    }

    private fun sendWakeRequest(deviceId: String, token: String): WakeResult {
        val url = URL("$serverUrl/api/upsnap/wake/$deviceId")
        val conn = createConnection(url)
        conn.requestMethod = "GET"
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT

        return when (conn.responseCode) {
            200, 204 -> WakeResult.Success
            401 -> WakeResult.Error("Contraseña incorrecta o usuario no válido.")
            403 -> WakeResult.Error("No tienes permiso para despertar este dispositivo.")
            404 -> WakeResult.Error("ID de dispositivo no encontrado. Prueba a buscarlo de nuevo.")
            else -> {
                val errorBody = try {
                    conn.errorStream?.bufferedReader()?.readText() ?: ""
                } catch (_: Exception) { "" }
                WakeResult.Error("Error del servidor (${conn.responseCode}): $errorBody")
            }
        }
    }

    /**
     * Creates an HTTP(S) connection that trusts self-signed certificates.
     * This is safe because the connection goes through Tailscale's encrypted tunnel.
     */
    private fun createConnection(url: URL): HttpURLConnection {
        val conn = url.openConnection() as HttpURLConnection

        // For HTTPS connections over Tailscale with self-signed certs
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
