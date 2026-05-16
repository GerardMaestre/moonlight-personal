package com.limelight.shared.data.session

import com.limelight.platform.StorageManager
import com.limelight.shared.domain.SessionRepository
import com.limelight.shared.domain.SessionState
import com.limelight.shared.domain.model.Session
import com.limelight.shared.network.immich.auth.ImmichAuthApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionRepositoryImpl(
    private val storageManager: StorageManager,
    private val authApi: ImmichAuthApi,
) : SessionRepository {
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Unauthenticated)
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    override suspend fun loginWithPassword(serverUrl: String, email: String, password: String): Session {
        _sessionState.value = SessionState.Authenticating
        return runCatching {
            val response = authApi.loginWithPassword(serverUrl, email, password)
            Session(serverUrl = serverUrl, userId = response.userId, accessToken = response.accessToken)
        }.onSuccess { saveSession(it) }
            .onFailure { _sessionState.value = SessionState.Error(it.message ?: "Authentication failed") }
            .getOrThrow()
    }

    override suspend fun validateApiKey(serverUrl: String, apiKey: String): Session {
        _sessionState.value = SessionState.Authenticating
        return runCatching {
            val user = authApi.validateSession(serverUrl = serverUrl, apiKey = apiKey)
            Session(serverUrl = serverUrl, userId = user.id, apiKey = apiKey)
        }.onSuccess { saveSession(it) }
            .onFailure { _sessionState.value = SessionState.Error(it.message ?: "API key validation failed") }
            .getOrThrow()
    }

    override suspend fun getActiveSession(): Session? {
        val session = loadSession()
        _sessionState.value = session?.let { SessionState.Authenticated(it) } ?: SessionState.Unauthenticated
        return session
    }

    override suspend fun clearSession() {
        storageManager.putString(KEY_SERVER_URL, "")
        storageManager.putString(KEY_USER_ID, "")
        storageManager.putString(KEY_ACCESS_TOKEN, "")
        storageManager.putString(KEY_API_KEY, "")
        storageManager.putString(KEY_EXPIRES_AT, "")
        _sessionState.value = SessionState.Unauthenticated
    }

    override suspend fun refreshIfNeeded(): Session? {
        val session = loadSession() ?: run {
            _sessionState.value = SessionState.Unauthenticated
            return null
        }
        if (session.expiresAt != null && session.expiresAt <= nowMillis()) {
            _sessionState.value = SessionState.Expired(session)
            return null
        }

        return runCatching {
            authApi.validateSession(session.serverUrl, accessToken = session.accessToken, apiKey = session.apiKey)
            _sessionState.value = SessionState.Authenticated(session)
            session
        }.getOrElse {
            _sessionState.value = SessionState.Expired(session)
            null
        }
    }

    private fun saveSession(session: Session) {
        storageManager.putString(KEY_SERVER_URL, session.serverUrl)
        storageManager.putString(KEY_USER_ID, session.userId)
        storageManager.putString(KEY_ACCESS_TOKEN, session.accessToken.orEmpty())
        storageManager.putString(KEY_API_KEY, session.apiKey.orEmpty())
        storageManager.putString(KEY_EXPIRES_AT, session.expiresAt?.toString().orEmpty())
        _sessionState.value = SessionState.Authenticated(session)
    }

    private fun loadSession(): Session? {
        val serverUrl = storageManager.getString(KEY_SERVER_URL).orEmpty()
        val userId = storageManager.getString(KEY_USER_ID).orEmpty()
        if (serverUrl.isBlank() || userId.isBlank()) return null

        return Session(
            serverUrl = serverUrl,
            userId = userId,
            accessToken = storageManager.getString(KEY_ACCESS_TOKEN).orEmpty().ifBlank { null },
            apiKey = storageManager.getString(KEY_API_KEY).orEmpty().ifBlank { null },
            expiresAt = storageManager.getString(KEY_EXPIRES_AT)?.toLongOrNull(),
        )
    }

    private fun nowMillis(): Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()

    private companion object {
        const val KEY_SERVER_URL = "immich.session.server_url"
        const val KEY_USER_ID = "immich.session.user_id"
        const val KEY_ACCESS_TOKEN = "immich.session.access_token"
        const val KEY_API_KEY = "immich.session.api_key"
        const val KEY_EXPIRES_AT = "immich.session.expires_at"
    }
}
