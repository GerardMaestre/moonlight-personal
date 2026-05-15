package com.limelight.streaming

data class StreamProfile(
    val fps: Int,
    val bitrateKbps: Int,
    val codec: CodecPreference,
    val width: Int,
    val height: Int,
)
