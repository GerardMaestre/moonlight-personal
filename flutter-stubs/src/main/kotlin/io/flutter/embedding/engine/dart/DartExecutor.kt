package io.flutter.embedding.engine.dart

class DartExecutor {
    fun executeDartEntrypoint(entrypoint: DartEntrypoint) {}

    class DartEntrypoint {
        companion object {
            @JvmStatic
            fun createDefault(): DartEntrypoint = DartEntrypoint()
        }
    }
}
