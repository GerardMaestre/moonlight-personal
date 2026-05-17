package com.limelight.ui.premium

import android.content.Context
import android.content.Intent
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor

/**
 * Helper class that isolates all direct references to Flutter SDK classes.
 * This prevents Dalvik/ART class verification issues at runtime if the Flutter SDK
 * is compileOnly and not present in the runtime classpath.
 */
object ImmichFlutterEngineWrapper {
    
    fun preWarm(context: Context) {
        val flutterEngine = FlutterEngine(context)
        flutterEngine.dartExecutor.executeDartEntrypoint(
            DartExecutor.DartEntrypoint.createDefault()
        )
        FlutterEngineCache.getInstance().put("immich_engine", flutterEngine)
    }

    fun getLaunchIntent(context: Context): Intent {
        return io.flutter.embedding.android.FlutterActivity
            .withCachedEngine("immich_engine")
            .build(context)
    }
}
