package com.example.autorecorder.common.recorder

import android.annotation.SuppressLint
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.example.autorecorder.common.FILES_FOLDER
import com.example.autorecorder.common.Utils.getDir
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date

class QueuedMuxer(dstPrefix: String) {
    private var mVideoFormat: MediaFormat? = null
    private var mAudioFormat: MediaFormat? = null
    private var mVideoTrackIndex = -1
    private var mAudioTrackIndex = -1
    private var mStarted = false
    private var mPendingSamples = mutableListOf<SampleInfo>()
    private var mPendingBuffer: ByteBuffer? = null
    private var mLastAudioTimestampUs: Long = 0
    private var mLastVideoTimestampUs: Long = 0
    private var currentMuxer: MediaMuxer? = null

    init {
        currentMuxer?.stop()
        currentMuxer?.release()
        currentMuxer = MediaMuxer(getDst(dstPrefix), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    @SuppressLint("SimpleDateFormat")
    private fun getDst(prefix: String): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss")
        val date = dateFormat.format(Date())
        val filesDir = getDir(FILES_FOLDER)
        return filesDir.absolutePath + "/${prefix}_$date.mp4"
    }

    @Synchronized
    fun setOutputFormat(sampleType: SampleType, format: MediaFormat) {
        when (sampleType) {
            SampleType.VIDEO -> {
                if (mVideoFormat != null) return
                mVideoFormat = format
            }
            SampleType.AUDIO -> {
                if (mAudioFormat != null) return
                mAudioFormat = format
            }
        }
        tryStartMuxer()
    }

    @Synchronized
    private fun tryStartMuxer() {
        if (mStarted || mVideoFormat == null || mAudioFormat == null) return

        mVideoTrackIndex = currentMuxer!!.addTrack(mVideoFormat!!)
        mAudioTrackIndex = currentMuxer!!.addTrack(mAudioFormat!!)
        currentMuxer!!.start()
        mStarted = true
        writePendingSamples()
    }

    @Synchronized
    fun writeSampleData(sampleType: SampleType, byteBuf: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        // Ensure timestamps are increasing (from second version)
        when (sampleType) {
            SampleType.AUDIO -> {
                if (bufferInfo.presentationTimeUs <= mLastAudioTimestampUs) {
                    bufferInfo.presentationTimeUs = mLastAudioTimestampUs + 1
                }
                mLastAudioTimestampUs = bufferInfo.presentationTimeUs
            }
            SampleType.VIDEO -> {
                if (bufferInfo.presentationTimeUs <= mLastVideoTimestampUs) {
                    bufferInfo.presentationTimeUs = mLastVideoTimestampUs + 1
                }
                mLastVideoTimestampUs = bufferInfo.presentationTimeUs
            }
        }
        if (!mStarted) {
            queueSample(sampleType, byteBuf, bufferInfo)
            return
        }

        val trackIndex = when (sampleType) {
            SampleType.VIDEO -> mVideoTrackIndex
            SampleType.AUDIO -> mAudioTrackIndex
        }

        if (trackIndex == -1) {
            Log.w(TAG, "Track not initialized for $sampleType, queuing sample")
            queueSample(sampleType, byteBuf, bufferInfo)
            return
        }

        try {
            byteBuf.position(bufferInfo.offset)
            byteBuf.limit(bufferInfo.offset + bufferInfo.size)
            currentMuxer?.writeSampleData(trackIndex, byteBuf, bufferInfo)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Muxer not started or already stopped", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write sample data", e)
            throw e
        }
    }

    private fun queueSample(sampleType: SampleType, byteBuf: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        byteBuf.limit(bufferInfo.offset + bufferInfo.size)
        byteBuf.position(bufferInfo.offset)

        if (mPendingBuffer == null) {
            mPendingBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE).apply {
                order(ByteOrder.nativeOrder())
            }
        }
        // Check if the buffer has enough space; if not, resize it
        if (mPendingBuffer!!.remaining() < bufferInfo.size) {
            val newCapacity = mPendingBuffer!!.capacity() + bufferInfo.size
            val newBuffer = ByteBuffer.allocateDirect(newCapacity).apply {
                order(ByteOrder.nativeOrder())
            }
            mPendingBuffer!!.flip()
            newBuffer.put(mPendingBuffer!!)
            mPendingBuffer = newBuffer
        }
        mPendingBuffer?.put(byteBuf)
        mPendingSamples.add(SampleInfo(sampleType, bufferInfo.size, bufferInfo))
    }

    private fun writePendingSamples() {
        mPendingBuffer?.flip()
        val bufferInfo = MediaCodec.BufferInfo()
        var offset = 0

        mPendingSamples.forEach { sample ->
            sample.writeToBufferInfo(bufferInfo, offset)
            writeSampleData(sample.mSampleType, mPendingBuffer!!, bufferInfo)
            offset += sample.mSize
        }

        mPendingSamples.clear()
        mPendingBuffer = null
    }

    @Synchronized
    fun release() {
        try {
            if (mStarted) {
                // Write any pending samples before stopping
                if (mPendingSamples.isNotEmpty()) {
                    writePendingSamples()
                }
                currentMuxer?.stop()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping muxer", e)
        } finally {
            currentMuxer?.release()
            mPendingSamples.clear()
            mPendingBuffer = null
            mStarted = false
            mVideoTrackIndex = -1
            mAudioTrackIndex = -1
            mLastVideoTimestampUs = 0
            mLastAudioTimestampUs = 0
        }
    }

    enum class SampleType {
        VIDEO, AUDIO
    }

    private class SampleInfo(
        val mSampleType: SampleType,
        val mSize: Int,
        bufferInfo: MediaCodec.BufferInfo
    ) {
        private val mPresentationTimeUs = bufferInfo.presentationTimeUs
        private val mFlags = bufferInfo.flags

        fun writeToBufferInfo(bufferInfo: MediaCodec.BufferInfo, offset: Int) {
            bufferInfo[offset, mSize, mPresentationTimeUs] = mFlags
        }
    }

    companion object {
        private const val TAG = "QueuedMuxer"
        private const val BUFFER_SIZE = 256 * 1024 // I have no idea whether this value is appropriate or not...
    }
}

class QueuedMuxerManager(
    private val dstPrefix: String,
    private val durationMillis: Long = 60 * 60_000L
) {
    private var currentMuxer: QueuedMuxer? = null
    private var muxerTimerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var videoFormat: MediaFormat? = null
    private var audioFormat: MediaFormat? = null

    private fun createMuxer(): QueuedMuxer {
        val muxer = QueuedMuxer(dstPrefix)
        videoFormat?.let {
            muxer.setOutputFormat(QueuedMuxer.SampleType.VIDEO, it)
        }
        audioFormat?.let {
            muxer.setOutputFormat(QueuedMuxer.SampleType.AUDIO, it)
        }
        return muxer
    }

    fun start() {
        currentMuxer = createMuxer()
        scheduleNextRotation()
    }

    fun stop() {
        muxerTimerJob?.cancel()
        currentMuxer?.release()
        currentMuxer = null
    }

    private fun scheduleNextRotation() {
        muxerTimerJob = scope.launch {
            while (isActive) {
                delay(durationMillis)
                currentMuxer?.release()
                currentMuxer = createMuxer()
            }
        }
    }

    fun setOutputFormat(type: QueuedMuxer.SampleType, format: MediaFormat) {
        when (type) {
            QueuedMuxer.SampleType.VIDEO -> {
                videoFormat = format
            }
            QueuedMuxer.SampleType.AUDIO -> {
                audioFormat = format
            }
        }

        currentMuxer?.setOutputFormat(type, format)
    }

    fun writeSampleData(type: QueuedMuxer.SampleType, buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        currentMuxer?.writeSampleData(type, buffer, info)
    }

    fun release() {
        stop()
        scope.cancel()
    }
}
