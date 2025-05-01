package com.example.autorecorder.common.recorder

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val MIME_TYPE_AAC = MediaFormat.MIMETYPE_AUDIO_AAC
private const val SAMPLE_RATE = 44_100
private const val BIT_RATE = 64 * 1024 // 64kbps
private const val TAG = "AudioEncoder"

@SuppressLint("MissingPermission")
class AudioEncoder(
    mediaProjection: MediaProjection,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val callback: EncoderCallback
) : Encoder {
    private val releaseLock = Any() // Thread-safe cleanup
    private val scope = CoroutineScope(CoroutineName(TAG) + dispatcher)

    // Moved playbackConfig creation outside to avoid recreation
    private val playbackConfig by lazy {
        AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()
    }

    private val channelCount = 1
    private val channelMask = AudioFormat.CHANNEL_IN_MONO

    private val audioBufferSizeInBytes by lazy {
       val size = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            channelMask,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (size <= 0) {
            Log.e(TAG, "Invalid buffer size: $size")
            throw IllegalStateException("Cannot init AudioRecord")
        }
        size
    }

    private val audioPlayback by lazy {
        AudioRecord.Builder()
            .setAudioPlaybackCaptureConfig(playbackConfig)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(channelMask)
                    .build()
            )
            .setBufferSizeInBytes(audioBufferSizeInBytes)
            .build().also {
                if (it.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioRecord not initialized!")
                }
            }
    }

    private val format by lazy {
        MediaFormat.createAudioFormat(MIME_TYPE_AAC, SAMPLE_RATE, channelCount).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectHE)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, audioBufferSizeInBytes)
        }
    }

    private val codec by lazy { MediaCodec.createEncoderByType(MIME_TYPE_AAC) }

    private var isEncoding = false
    private var recordJob: Job? = null
    private var drainJob: Job? = null

    override fun prepare() {
        synchronized(releaseLock) {
            codec.reset()
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }
    }

    override fun start() {
        synchronized(releaseLock) {
            if (isEncoding) {
                Log.w(TAG, "start() called but already started. Ignoring.")
                return
            }
            try {
                isEncoding = true
                audioPlayback.startRecording()
                codec.start()
                recordJob = record()
                drainJob = drain()
            } catch (e: Exception) {
                Log.e(TAG, "Error starting AudioEncoder", e)
                isEncoding = false
                // Make sure if error happens, we clean up
                try {
                    audioPlayback.stop()
                } catch (_: Exception) { }
                try {
                    codec.stop()
                } catch (_: Exception) { }
            }
        }
    }

    private fun record() = scope.launch {
        val byteArray = ByteArray(audioBufferSizeInBytes)
        try {
            while (isActive && isEncoding) {
                val readBytes = audioPlayback.read(byteArray, 0, byteArray.size)
                if (readBytes < 0) {
                    Log.e(TAG, "AudioRecord read error: $readBytes")
                    break
                }
                val inputBufferId = codec.dequeueInputBuffer(-1)
                if (inputBufferId >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferId)!!
                    inputBuffer.clear()
                    inputBuffer.put(byteArray, 0, readBytes)
                    codec.queueInputBuffer(
                        inputBufferId, 0, readBytes, System.nanoTime() / 1000, 0
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in record loop", e)
        } finally {
            ensureSignalEOS()
        }
    }

    private fun drain() = scope.launch {
        val bufferInfo = MediaCodec.BufferInfo()
        try {
            while (isActive && isEncoding) {
                val outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 10_000) // 10ms timeout
                when {
                    outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER -> continue
                    outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        Log.d(TAG, "Output format changed")
                        callback.onFormatChanged(codec.outputFormat, EncoderType.Audio)
                    }
                    outputBufferId >= 0 -> {
                        try {
                            val encodedData = codec.getOutputBuffer(outputBufferId)
                                ?: throw RuntimeException("encodedData is null")
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                                bufferInfo.size = 0
                            }
                            if (bufferInfo.size > 0) {
                                callback.onEncoded(encodedData, bufferInfo, EncoderType.Audio)
                            }
                            codec.releaseOutputBuffer(outputBufferId, false)

                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                Log.d(TAG, "EOS")
                                break
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error while processing output buffer", e)
                            codec.releaseOutputBuffer(outputBufferId, false)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in drain loop", e)
        } finally {
            synchronized(releaseLock) {
                try {
                    codec.stop()
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping codec", e)
                }
                isEncoding = false
            }
        }
    }

    override fun stop() {
        synchronized(releaseLock) {
            try {
                isEncoding = false
                audioPlayback.stop()
                recordJob?.cancel()
                drainJob?.cancel()

                ensureSignalEOS()
            } catch (e: Exception) {
                Log.e(TAG, "Error in stop()", e)
            }
        }
    }

    override fun release() {
        synchronized(releaseLock) {
            isEncoding = false
            recordJob?.cancel()
            drainJob?.cancel()

            try {
                audioPlayback.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing audioPlayback", e)
            }

            try {
                codec.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing codec", e)
            }

            scope.cancel()
        }
    }

    private fun ensureSignalEOS() {
        try {
            val id = codec.dequeueInputBuffer(0)
            if (id >= 0) {
                codec.queueInputBuffer(id, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error signaling EOS", e)
        }
    }
}