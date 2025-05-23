package com.example.autorecorder.common.recorder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import com.example.autorecorder.common.SharedPreferencesHelper

private const val I_FRAME_INTERVAL_SEC = 1.0f
private const val REPEAT_FRAMES_AFTER_MICRO_SEC = 1_000_000L / 30

private const val TAG = "VideoEncoder"

class VideoEncoder(width: Int, height: Int, private val callback: EncoderCallback) :
    Encoder, MediaCodec.Callback() {
    /** inputSurface is created in prepare() */
    lateinit var inputSurface: Surface
        private set

    private val mime = MediaFormat.MIMETYPE_VIDEO_AVC
    private val format: MediaFormat =
        MediaFormat.createVideoFormat(mime, width, height).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
            setInteger(MediaFormat.KEY_FRAME_RATE, SharedPreferencesHelper.frameRate)
            setInteger(MediaFormat.KEY_BIT_RATE, SharedPreferencesHelper.bitrate * 1024 * 1024)
            setFloat(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL_SEC)
//            setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, REPEAT_FRAMES_AFTER_MICRO_SEC)
        }
    private val codec = MediaCodec.createEncoderByType(mime).apply {
        setCallback(this@VideoEncoder)
    }

    override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
    }

    override fun onOutputBufferAvailable(
        codec: MediaCodec,
        index: Int,
        info: MediaCodec.BufferInfo
    ) {
        val buffer =
            codec.getOutputBuffer(index) ?: throw RuntimeException("Output buffer is null")
        callback.onEncoded(buffer, info, EncoderType.Video)
        codec.releaseOutputBuffer(index, false)
    }

    override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
        Log.e(TAG, e.toString())
    }

    override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
        Log.d(TAG, "onOutputFormatChanged: ${format.getString(MediaFormat.KEY_MIME)}")
        callback.onFormatChanged(format, EncoderType.Video)
    }

    override fun prepare() {
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = codec.createInputSurface()
    }

    override fun start() {
        codec.start()
    }

    override fun stop() {
        codec.stop()
    }

    override fun release() {
        codec.setCallback(null)
        codec.release()
    }
}