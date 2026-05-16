package com.limelight.shared.data.remote.repository

import com.limelight.shared.data.immich.ImmichConnectionConfig
import com.limelight.shared.data.remote.dto.SearchAssetsRequestDto
import com.limelight.shared.data.remote.dto.SearchResponseDto
import com.limelight.shared.data.remote.mapper.SearchMapper
import com.limelight.shared.domain.media.SearchQuery
import com.limelight.shared.domain.media.SearchResult
import com.limelight.shared.network.immich.ImmichApiClient
import com.limelight.shared.network.immich.normalizedBaseUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen

class ImmichSearchRepository(
    private val config: ImmichConnectionConfig,
    private val httpClient: HttpClient = ImmichApiClient.defaultHttpClient(),
) {
    suspend fun search(query: SearchQuery): SearchResult {
        val response = httpClient.post(normalizedBaseUrl(config.baseUrl) + "/api/search") {
            applyAuth()
            setBody(
                SearchAssetsRequestDto(
                    page = query.page,
                    size = query.size,
                    query = query.text,
                    takenAfter = query.fromDateIso,
                    takenBefore = query.toDateIso,
                    type = query.type?.name,
                    person = query.person,
                    object = query.objectLabel,
                ),
            )
        }.body<SearchResponseDto>()
        return SearchMapper.toDomain(response)
    }

    fun searchFlow(query: SearchQuery): Flow<SearchResult> = flow {
        emit(search(query))
    }.retryWhen { cause, attempt ->
        if (attempt >= 2) return@retryWhen false
        if (cause is DataError.Network) {
            delay((attempt + 1) * 300)
            true
        } else {
            false
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyAuth() {
        when {
            config.apiKey.isNotBlank() -> header("x-api-key", config.apiKey)
            config.bearerToken.isNotBlank() -> bearerAuth(config.bearerToken)
        }
    }
}
