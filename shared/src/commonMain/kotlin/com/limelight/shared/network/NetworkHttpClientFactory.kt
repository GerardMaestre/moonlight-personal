package com.limelight.shared.network

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

expect fun platformNetworkHttpClient(json: Json): HttpClient

internal fun defaultNetworkHttpClient(json: Json): HttpClient = platformNetworkHttpClient(json)
