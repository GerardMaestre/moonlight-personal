package com.limelight.streaming

object BitratePlanner {
    fun suggestProfile(isWifi: Boolean, preferCodec: CodecPreference = CodecPreference.AUTO): StreamProfile {
        val codec = if (preferCodec == CodecPreference.AUTO) CodecPreference.H264 else preferCodec
        return if (isWifi) {
            StreamProfile(60, 20_000, codec, 1920, 1080)
        } else {
            StreamProfile(30, 8_000, codec, 1280, 720)
        }
    }
}
