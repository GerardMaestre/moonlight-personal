package com.limelight.shared.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

@Serializable
private data class ScriptRequest(val carpeta: String, val archivo: String)

/**
 * Client for triggering remote scripts on the Stream Deck PC.
 */
class RemoteScriptClient(private val baseUrl: String, private val token: String) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }

    fun runScript(folder: String, file: String): Boolean {
        return try {
            val url = URL("$baseUrl/api/run-script")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val body = json.encodeToString(ScriptRequest.serializer(), ScriptRequest(folder, file))
            conn.outputStream.bufferedWriter().use { it.write(body) }

            conn.responseCode == 200
        } catch (e: Exception) {
            println("[RemoteScriptClient] Error: ${e.message}")
            false
        }
    }
}
