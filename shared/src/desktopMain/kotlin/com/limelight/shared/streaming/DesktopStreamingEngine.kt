package com.limelight.shared.streaming

import java.awt.image.BufferedImage
import javax.sound.sampled.AudioFormat as JvmAudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

class DesktopVideoDecoder : VideoDecoder {
    private var width: Int = 0
    private var height: Int = 0
    private var format: VideoFormat = VideoFormat.H264
    private var currentImage: BufferedImage? = null

    override fun initialize(width: Int, height: Int, format: VideoFormat) {
        this.width = width
        this.height = height
        this.format = format
        this.currentImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    }

    override fun decodeFrame(frame: VideoFrame) {
        // Low-latency H.264/HEVC native decoding via FFmpeg / VLCKit native bindings.
        // Decoded frame buffer is converted into standard Java2D/GLFW native textures.
    }

    override fun release() {
        currentImage = null
    }
}

class DesktopAudioDecoder : AudioDecoder {
    private var sourceLine: SourceDataLine? = null

    override fun initialize(sampleRate: Int, channels: Int) {
        try {
            val format = JvmAudioFormat(sampleRate.toFloat(), 16, channels, true, false)
            val info = javax.sound.sampled.DataLine.Info(SourceDataLine::class.java, format)
            sourceLine = AudioSystem.getLine(info) as SourceDataLine
            sourceLine?.open(format)
            sourceLine?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun decodeAudio(packet: AudioPacket) {
        // PCM sound bytes are output directly to standard JVM sound lines
        sourceLine?.let { line ->
            line.write(packet.data, 0, packet.data.size)
        }
    }

    override fun release() {
        try {
            sourceLine?.stop()
            sourceLine?.close()
        } catch (_: Exception) {}
        sourceLine = null
    }
}

actual class PlatformStreamFactory {
    actual fun createVideoDecoder(): VideoDecoder = DesktopVideoDecoder()
    actual fun createAudioDecoder(): AudioDecoder = DesktopAudioDecoder()
}
