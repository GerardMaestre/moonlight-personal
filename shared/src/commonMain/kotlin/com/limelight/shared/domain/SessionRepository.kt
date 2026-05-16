package com.limelight.shared.domain

import com.limelight.shared.domain.model.Session
import kotlinx.coroutines.flow.StateFlow

sealed interface SessionState {
    data object Unauthenticated : SessionState
    data object Authenticating : SessionState
    data class Authenticated(val session: Session) : SessionState
    data class Expired(val session: Session?) : SessionState
    data class Error(val message: String) : SessionState
}

interface SessionRepository {
    val sessionState: StateFlow<SessionState>

    suspend fun loginWithPassword(serverUrl: String, email: String, password: String): Session
    suspend fun validateApiKey(serverUrl: String, apiKey: String): Session
    suspend fun getActiveSession(): Session?
    suspend fun clearSession()
    suspend fun refreshIfNeeded(): Session?
}
