package com.limelight.shared.network

import com.limelight.shared.network.defaultNetworkHttpClient
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
        return runBlocking(Dispatchers.IO) {
            try {
                runScriptInternal(folder, file)
            } catch (e: Exception) {
                log("Error: ${e.javaClass.simpleName}: ${e.message}")
                false
            }
        }
    }

    private suspend fun runScriptInternal(folder: String, file: String): Boolean {
        val endpoint = normalizeBaseUrl(baseUrl) + "/api/run-script"
        log("Calling $endpoint with folder=$folder, file=$file")

        return defaultNetworkHttpClient(json).use { client ->
            val response = client.post(endpoint) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(ScriptRequest(folder, file))
            }

            val responseBody = try {
                response.bodyAsText()
            } catch (e: Exception) {
                "Could not read response"
            }

            log("Response: ${response.status.value} - $responseBody")
            response.status == HttpStatusCode.OK
        }
    }

    private fun normalizeBaseUrl(baseUrl: String): String {
        return baseUrl.trim().let {
            val normalized = if (!it.startsWith("http")) "http://$it" else it
            normalized.trimEnd('/')
        }
    }

    private fun log(msg: String) {
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

