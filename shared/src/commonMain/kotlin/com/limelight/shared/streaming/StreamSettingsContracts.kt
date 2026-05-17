package com.limelight.shared.streaming

import com.limelight.streaming.CodecPreference

data class StreamSettingsSnapshot(
    val bitrateKbps: Int = 20000,
    val resolution: String = "1920x1080",
    val fps: Int = 60,
    val codec: CodecPreference = CodecPreference.AUTO,
)

interface StreamSettingsStore {
    suspend fun read(): StreamSettingsSnapshot
    suspend fun write(snapshot: StreamSettingsSnapshot)
}
