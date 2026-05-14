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
            // Step 1: Authenticate to get JWT token
            val token = authenticate()
                ?: return WakeResult.Error("Error de autenticación. Verifica usuario y contraseña.")

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
            val token = authenticate() ?: return emptyList()
            
            val url = URL("$serverUrl/api/collections/devices/records")
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

    private fun authenticate(): String? {
        val url = URL("$serverUrl/api/collections/users/auth-with-password")
        val conn = createConnection(url)
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT

        val body = JSONObject().apply {
            put("identity", username)
            put("password", password)
        }

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(body.toString())
            writer.flush()
        }

        return if (conn.responseCode == 200) {
            val response = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).readText()
            JSONObject(response).optString("token", null)
        } else {
            Log.w(TAG, "Auth failed with code ${conn.responseCode}")
            null
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
            401 -> WakeResult.Error("Token expirado. Reintenta.")
            404 -> WakeResult.Error("Dispositivo no encontrado en UpSnap.")
            else -> {
                val errorBody = try {
                    conn.errorStream?.bufferedReader()?.readText() ?: ""
                } catch (_: Exception) { "" }
                WakeResult.Error("Error ${conn.responseCode}: $errorBody")
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
