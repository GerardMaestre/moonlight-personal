package io.flutter.embedding.android

import android.content.Context
import android.content.Intent

class FlutterActivity {
    companion object {
        @JvmStatic
        fun withCachedEngine(engineId: String): CachedEngineIntentBuilder {
            return CachedEngineIntentBuilder(engineId)
        }
    }

    class CachedEngineIntentBuilder(private val engineId: String) {
        fun build(context: Context): Intent {
            // Under normal compile conditions, this maps to starting FlutterActivity.
            // Since this is a compile-only stub, it resolves the type signature cleanly.
            return Intent(context, FlutterActivity::class.java)
        }
    }
}
