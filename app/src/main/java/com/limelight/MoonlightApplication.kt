package com.limelight

import android.app.Application
import com.limelight.ui.premium.ImmichFlutterEngineWrapper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MoonlightApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Safely pre-warm a global FlutterEngine if the Flutter SDK is packaged/present
        runCatching {
            Class.forName("io.flutter.embedding.engine.FlutterEngine")
            ImmichFlutterEngineWrapper.preWarm(this)
        }.onFailure {
            android.util.Log.i("MoonlightApp", "Flutter SDK no presente en runtime. Se omitirá el precalentamiento.")
        }
    }
}

