package io.flutter.embedding.engine

class FlutterEngineCache private constructor() {
    companion object {
        private val instance = FlutterEngineCache()
        
        @JvmStatic
        fun getInstance(): FlutterEngineCache = instance
    }

    fun put(key: String, engine: FlutterEngine?) {}
    fun get(key: String): FlutterEngine? = null
}
