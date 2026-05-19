package com.limelight.shared.streaming

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.AudioTrack
import android.media.AudioFormat as AndroidAudioFormat
import android.media.AudioManager
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer

class AndroidVideoDecoder : VideoDecoder {
    private var mediaCodec: MediaCodec? = null

    override fun initialize(width: Int, height: Int, format: VideoFormat) {
        val mime = when (format) {
            VideoFormat.H264 -> MediaFormat.MIMETYPE_VIDEO_AVC
            VideoFormat.HEVC -> MediaFormat.MIMETYPE_VIDEO_HEVC
            VideoFormat.AV1 -> MediaFormat.MIMETYPE_VIDEO_AV1
        }
        val mediaFormat = MediaFormat.createVideoFormat(mime, width, height)
        
        // Android MediaCodec decodes H.264/HEVC frames on low-latency hardware decoders
        mediaCodec = MediaCodec.createDecoderByType(mime).apply {
            configure(mediaFormat, null, null, 0)
            start()
        }
    }

    override fun decodeFrame(frame: VideoFrame) {
        val codec = mediaCodec ?: return
        val inputIndex = codec.dequeueInputBuffer(10000)
        if (inputIndex >= 0) {
            val inputBuffer = codec.getInputBuffer(inputIndex) ?: return
            inputBuffer.clear()
            inputBuffer.put(frame.data)
            codec.queueInputBuffer(
                inputIndex, 0, frame.data.size, frame.timestamp,
                if (frame.isKeyFrame) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
            )
        }
        
        val bufferInfo = MediaCodec.BufferInfo()
        val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
        if (outputIndex >= 0) {
            codec.releaseOutputBuffer(outputIndex, true) // Releases frame to rendering Surface
        }
    }

    override fun release() {
        try {
            mediaCodec?.stop()
            mediaCodec?.release()
        } catch (_: Exception) {}
        mediaCodec = null
    }
}

class AndroidAudioDecoder : AudioDecoder {
    private var audioTrack: AudioTrack? = null

    override fun initialize(sampleRate: Int, channels: Int) {
        val channelConfig = if (channels == 1) AndroidAudioFormat.CHANNEL_OUT_MONO else AndroidAudioFormat.CHANNEL_OUT_STEREO
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, AndroidAudioFormat.ENCODING_PCM_16BIT)
        
        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            channelConfig,
            AndroidAudioFormat.ENCODING_PCM_16BIT,
            minBufferSize,
            AudioTrack.MODE_STREAM
        ).apply {
            play()
        }
    }

    override fun decodeAudio(packet: AudioPacket) {
        audioTrack?.write(packet.data, 0, packet.data.size)
    }

    override fun release() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}

actual class PlatformStreamFactory {
    actual fun createVideoDecoder(): VideoDecoder = AndroidVideoDecoder()
    actual fun createAudioDecoder(): AudioDecoder = AndroidAudioDecoder()
}
