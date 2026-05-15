package com.limelight.di

import com.limelight.shared.network.UpSnapClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideUpSnapClientFactory(): UpSnapClientFactory = UpSnapClientFactory()
}

class UpSnapClientFactory {
    fun create(serverUrl: String, username: String, password: String): UpSnapClient {
        return UpSnapClient(serverUrl, username, password)
    }
}
