package com.example.autorecorder.common.recorder

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.projection.MediaProjectionManager
import android.view.Surface
import com.example.autorecorder.common.SharedPreferencesHelper
import java.nio.ByteBuffer

class Recorder(
    val context: Context,
    resultData: Intent,
    resultCode: Int,
    streamerName: String,
    val width: Int = SharedPreferencesHelper.quality.width,
    val height: Int = SharedPreferencesHelper.quality.height,
) : EncoderCallback {
    private val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    private val mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)

//    private val muxerManager = QueuedMuxerManager(streamerName)
    private val queuedMuxer: QueuedMuxerCallback = if (SharedPreferencesHelper.splitFile) {
        QueuedMuxerManager(streamerName)
    } else {
        QueuedMuxer(streamerName)
    }

    private val audioEncoder = AudioEncoder(mediaProjection, callback = this)
    private val videoEncoder = VideoEncoder(width = width, height = height, callback = this)

    private val surface: Surface get() = videoEncoder.inputSurface

    private var virtualDisplay: VirtualDisplay? = null

    override fun onFormatChanged(format: MediaFormat, type: EncoderType) {
        queuedMuxer.setOutputFormat(
            if (type == EncoderType.Video) QueuedMuxer.SampleType.VIDEO else QueuedMuxer.SampleType.AUDIO,
            format
        )
    }

    override fun onEncoded(buffer: ByteBuffer, info: MediaCodec.BufferInfo, type: EncoderType) {
        queuedMuxer.writeSampleData(
            if (type == EncoderType.Video) QueuedMuxer.SampleType.VIDEO else QueuedMuxer.SampleType.AUDIO,
            buffer,
            info
        )
    }

    fun start() {
        queuedMuxer.start()
        audioEncoder.prepare()
        videoEncoder.prepare()
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "Projection",
            width,
            height,
            context.resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface,
            null,
            null
        )
        audioEncoder.start()
        videoEncoder.start()
    }

    fun stop() {
        audioEncoder.stop()
        videoEncoder.stop()
        queuedMuxer.stop()

        audioEncoder.release()
        videoEncoder.release()
        queuedMuxer.release()
        virtualDisplay?.release()
        virtualDisplay = null
    }
}