package com.limelight

import android.app.Application
import android.content.Context
import com.limelight.di.networkModule
import com.limelight.platform.StorageManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MoonlightApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        StorageManager.initialize(this)

        startKoin {
            androidContext(this@MoonlightApplication)
            modules(networkModule)
        }
    }
}

