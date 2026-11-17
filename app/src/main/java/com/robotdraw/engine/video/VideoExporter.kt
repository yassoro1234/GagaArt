package com.robotdraw.engine.video

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.Image
import java.nio.ByteBuffer

/**
 * Hardware H.264 encoder + MP4 muxer, for frames given as ARGB bitmaps.
 *
 * Based on the native AndroidVideoEngine idea (MediaCodec encoder, MediaMuxer output),
 * with the parts that were left as TODO filled in:
 *  - frames are pushed with exact presentation times (frame N at N / fps seconds);
 *  - the encoder input uses YUV420 "flexible" images, so it works on devices that do
 *    not support one specific YUV layout;
 *  - the muxer track is added and started when the encoder reports its output format;
 *  - finish() sends end-of-stream, drains the encoder, then closes codec and muxer.
 */
class VideoExporter(
    private val width: Int,
    private val height: Int,
    private val fps: Int,
    bitrate: Int,
    outputPath: String
) {
    private val codec: MediaCodec
    private val muxer: MediaMuxer
    private val info = MediaCodec.BufferInfo()
    private val pixels = IntArray(width * height)
    private var trackIndex = -1
    private var muxerStarted = false
    private var frameIndex = 0L
    private var finished = false

    init {
        require(width > 0 && height > 0 && width % 2 == 0 && height % 2 == 0) { "Video size must be even" }
        val format = MediaFormat.createVideoFormat(MIME, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // one key frame per second
        }
        codec = MediaCodec.createEncoderByType(MIME)
        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: Exception) {
            codec.release()
            throw e
        }
    }

    /** Adds one frame. The bitmap is scaled to the exporter size if needed. */
    fun pushFrame(source: Bitmap) {
        check(!finished) { "Exporter already finished" }
        val frame = if (source.width == width && source.height == height) source
        else Bitmap.createScaledBitmap(source, width, height, true)
        frame.getPixels(pixels, 0, width, 0, 0, width, height)
        val ptsUs = frameIndex * 1_000_000L / fps
        val inIndex = dequeueInput()
        val image = codec.getInputImage(inIndex) ?: error("Encoder gave no input image")
        fillYuv(image)
        codec.queueInputBuffer(inIndex, 0, width * height * 3 / 2, ptsUs, 0)
        frameIndex++
        drain(endOfStream = false)
    }

    /** Ends the video, writes the MP4 file and releases the codec and muxer. */
    fun finish() {
        if (finished) return
        finished = true
        val inIndex = dequeueInput()
        codec.queueInputBuffer(inIndex, 0, 0, frameIndex * 1_000_000L / fps, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        drain(endOfStream = true)
        codec.stop()
        codec.release()
        if (muxerStarted) muxer.stop()
        muxer.release()
    }

    /** Stops without a valid file (used after an error). */
    fun abort() {
        if (finished) return
        finished = true
        runCatching { codec.stop() }
        codec.release()
        runCatching { if (muxerStarted) muxer.stop() }
        muxer.release()
    }

    private fun dequeueInput(): Int {
        while (true) {
            val index = codec.dequeueInputBuffer(10_000)
            if (index >= 0) return index
            drain(endOfStream = false) // encoder is full: write out what it has
        }
    }

    private fun drain(endOfStream: Boolean) {
        while (true) {
            val outIndex = codec.dequeueOutputBuffer(info, 10_000)
            when {
                outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) return
                }
                outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    trackIndex = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    muxerStarted = true
                }
                outIndex >= 0 -> {
                    val buffer: ByteBuffer = codec.getOutputBuffer(outIndex) ?: error("No output buffer")
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                    if (info.size > 0 && muxerStarted) {
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        muxer.writeSampleData(trackIndex, buffer, info)
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
            }
        }
    }

    /** ARGB pixels to YUV420 (BT.601 limited range). Chroma is sampled at the top-left of each 2x2 block. */
    private fun fillYuv(image: Image) {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val yBuf = yPlane.buffer
        val uBuf = uPlane.buffer
        val vBuf = vPlane.buffer
        for (y in 0 until height) {
            for (x in 0 until width) {
                val c = pixels[y * width + x]
                val r = (c shr 16) and 0xff
                val g = (c shr 8) and 0xff
                val b = c and 0xff
                val yv = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                yBuf.put(y * yPlane.rowStride + x * yPlane.pixelStride, yv.toByte())
                if (x and 1 == 0 && y and 1 == 0) {
                    val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    val cy = y shr 1
                    val cx = x shr 1
                    uBuf.put(cy * uPlane.rowStride + cx * uPlane.pixelStride, u.toByte())
                    vBuf.put(cy * vPlane.rowStride + cx * vPlane.pixelStride, v.toByte())
                }
            }
        }
    }

    companion object {
        const val MIME = "video/avc"
    }
}
