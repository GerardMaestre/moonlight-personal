package com.limelight.shared.streaming

import kotlinx.coroutines.flow.StateFlow

enum class StreamState {
    IDLE,
    CONNECTING,
    ACTIVE,
    DISCONNECTED,
    ERROR
}

enum class VideoFormat {
    H264,
    HEVC,
    AV1
}

data class VideoFrame(
    val data: ByteArray,
    val timestamp: Long,
    val isKeyFrame: Boolean
)

data class AudioPacket(
    val data: ByteArray,
    val sequenceNumber: Long
)

interface VideoDecoder {
    fun initialize(width: Int, height: Int, format: VideoFormat)
    fun decodeFrame(frame: VideoFrame)
    fun release()
}

interface AudioDecoder {
    fun initialize(sampleRate: Int, channels: Int)
    fun decodeAudio(packet: AudioPacket)
    fun release()
}

sealed class StreamInputEvent {
    data class MouseMove(val x: Float, val y: Float) : StreamInputEvent()
    data class MouseButton(val button: Int, val isDown: Boolean) : StreamInputEvent()
    data class Key(val keyCode: Int, val isDown: Boolean) : StreamInputEvent()
    data class GamepadAxis(val controllerId: Int, val axis: Int, val value: Float) : StreamInputEvent()
    data class GamepadButton(val controllerId: Int, val button: Int, val isDown: Boolean) : StreamInputEvent()
}

interface StreamSession {
    val state: StateFlow<StreamState>
    val error: StateFlow<String?>
    
    suspend fun start(hostIp: String, settings: StreamSettingsSnapshot)
    suspend fun stop()
    fun sendInput(event: StreamInputEvent)
}

/**
 * Platform factory to instantiate the concrete video/audio decoders.
 */
expect class PlatformStreamFactory {
    fun createVideoDecoder(): VideoDecoder
    fun createAudioDecoder(): AudioDecoder
}
