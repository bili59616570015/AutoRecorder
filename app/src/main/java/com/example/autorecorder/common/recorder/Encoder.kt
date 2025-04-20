package com.example.autorecorder.common.recorder

import android.media.MediaCodec
import android.media.MediaFormat
import java.nio.ByteBuffer

interface Encoder {
    fun prepare()
    fun start()
    fun stop()
    fun release()
}

enum class EncoderType { Video, Audio }

interface EncoderCallback {
    fun onFormatChanged(format: MediaFormat, type: EncoderType)
    fun onEncoded(buffer: ByteBuffer, info: MediaCodec.BufferInfo, type: EncoderType)
}