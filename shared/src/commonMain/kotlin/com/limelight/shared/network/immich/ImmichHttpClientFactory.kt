package com.limelight.shared.network.immich

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

expect fun platformImmichHttpClient(json: Json): HttpClient
