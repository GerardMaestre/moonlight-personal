package io.flutter.embedding.engine

import android.content.Context
import io.flutter.embedding.engine.dart.DartExecutor

class FlutterEngine(context: Context) {
    val dartExecutor: DartExecutor = DartExecutor()
}
