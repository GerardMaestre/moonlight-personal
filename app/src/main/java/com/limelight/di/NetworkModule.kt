package com.limelight.di

import com.limelight.shared.network.UpSnapClient
import org.koin.dsl.module

val networkModule = module {
    single { UpSnapClientFactory() }
}

class UpSnapClientFactory {
    fun create(serverUrl: String, username: String, password: String): UpSnapClient {
        return UpSnapClient(serverUrl, username, password)
    }
}
