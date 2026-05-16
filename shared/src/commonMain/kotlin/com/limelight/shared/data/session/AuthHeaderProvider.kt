package com.limelight.shared.data.session

import com.limelight.shared.domain.model.Session

class AuthHeaderProvider {
    fun headersFor(session: Session?): Map<String, String> = when {
        session == null -> emptyMap()
        !session.apiKey.isNullOrBlank() -> mapOf("x-api-key" to session.apiKey)
        !session.accessToken.isNullOrBlank() -> mapOf("Authorization" to "Bearer ${session.accessToken}")
        else -> emptyMap()
    }
}
