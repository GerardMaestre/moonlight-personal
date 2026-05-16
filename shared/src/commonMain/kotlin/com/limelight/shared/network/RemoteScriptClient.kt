package com.limelight.shared.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
private data class ScriptRequest(val carpeta: String, val archivo: String)

/**
 * Client for triggering remote scripts on the Stream Deck PC.
 */
class RemoteScriptClient(private val baseUrl: String, private val token: String) {
    companion object {
        private const val TAG = "RemoteScriptClient"
        private val json = Json { ignoreUnknownKeys = true }
    }

    fun runScript(folder: String, file: String): Boolean {
        return try {
            val endpoint = "$baseUrl/api/run-script"
            log("Calling $endpoint with folder=$folder, file=$file")
            
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.connectTimeout = 10000
            conn.readTimeout = 30000  // Scripts can take time to respond

            val body = json.encodeToString(ScriptRequest.serializer(), ScriptRequest(folder, file))
            log("Sending body: $body")
            conn.outputStream.bufferedWriter().use { it.write(body) }

            try {
                val code = conn.responseCode
                val responseBody = try {
                    if (code == 200) {
                        conn.inputStream.bufferedReader().readText()
                    } else {
                        conn.errorStream?.bufferedReader()?.readText() ?: "No response body"
                    }
                } catch (e: Exception) {
                    "Could not read response"
                }

                log("Response: $code - $responseBody")
                code == 200
            } finally {
                conn.disconnect()
            }
        } catch (e: java.net.SocketTimeoutException) {
            log("TIMEOUT connecting to StreamDeck server at $baseUrl")
            false
        } catch (e: java.net.ConnectException) {
            log("CONNECTION REFUSED at $baseUrl - is StreamDeck running?")
            false
        } catch (e: Exception) {
            log("Error: ${e.javaClass.simpleName}: ${e.message}")
            false
        }
    }
    
    private fun log(msg: String) {
        // Use both println and Android Log for maximum visibility
        println("[$TAG] $msg")
        try {
            val logClass = Class.forName("android.util.Log")
            val method = logClass.getMethod("d", String::class.java, String::class.java)
            method.invoke(null, TAG, msg)
        } catch (_: Exception) {
            // Not on Android, println is enough
        }
    }
}

