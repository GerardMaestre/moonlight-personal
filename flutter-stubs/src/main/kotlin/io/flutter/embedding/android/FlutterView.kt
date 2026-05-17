package io.flutter.embedding.android

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import io.flutter.embedding.engine.FlutterEngine

class FlutterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    
    fun attachToFlutterEngine(flutterEngine: FlutterEngine) {
        // Compile-time stub
    }
}
