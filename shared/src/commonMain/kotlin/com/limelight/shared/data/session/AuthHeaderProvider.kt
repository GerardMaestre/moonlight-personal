package com.limelight.shared.data.session

import com.limelight.shared.data.immich.ImmichConnectionConfig
import com.limelight.shared.domain.model.Session

class AuthHeaderProvider {
    /**
     * Authentication precedence is explicit: API key first, then Bearer token.
     *
     * If both credentials are present and non-blank (after trim), both headers are emitted.
     */
    fun headersFor(config: ImmichConnectionConfig): Map<String, String> = headersFor(config.toSession())

    fun headersFor(session: Session?): Map<String, String> {
        if (session == null) return emptyMap()

        val apiKey = session.apiKey?.trim().orEmpty()
        val accessToken = session.accessToken?.trim().orEmpty()

        val headers = linkedMapOf<String, String>()
        if (apiKey.isNotEmpty()) {
            headers["x-api-key"] = apiKey
        }
        if (accessToken.isNotEmpty()) {
            headers["Authorization"] = "Bearer $accessToken"
        }
        return headers
    }

    private fun ImmichConnectionConfig.toSession(): Session = Session(
        serverUrl = baseUrl,
        userId = "immich",
        accessToken = bearerToken.trim().ifEmpty { null },
        apiKey = apiKey.trim().ifEmpty { null },
    )
}
