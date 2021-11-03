package com.kevinserver

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord

import android.media.MediaRecorder
import android.util.Log
import java.io.IOException
import java.io.InputStream


class AudioInputStream(
    audioSource: Int = MediaRecorder.AudioSource.MIC,
    sampleRate: Int = 8000,
    channelConfig: Int = AudioFormat.CHANNEL_CONFIGURATION_MONO,
    audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    val bufferSizeInBytes: Int = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)) : InputStream() {

    @SuppressLint("MissingPermission")
    private val audioRecord = AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSizeInBytes).apply {
        startRecording()
    }

    @Deprecated("Use read(audioData, offset, length)")
    @Throws(IOException::class)
    override fun read(): Int {
        val tmp = byteArrayOf(0)
        read(tmp, 0, 1)
        return tmp[0].toInt()
    }

    @Throws(IOException::class)
    override fun read(audioData: ByteArray, offset: Int, length: Int): Int {
        try {
            //Log.d("kevin", "audio audioData: $audioData")
            return audioRecord.read(audioData, offset, length)
        } catch (e: Exception) {
            throw IOException(e)
        }
    }

    override fun close() {
        audioRecord.stop()
        audioRecord.release()
        super.close()
    }
}