package com.kevinserver

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kevinserver.databinding.FragmentClientBinding
import android.annotation.SuppressLint
import android.media.AudioRecord

import android.media.MediaRecorder

import java.io.*


class ClientFragment : Fragment() {

    var isRecording = false //是否正在录音的标记
    var isPlaying = false //是否正在放音的标记

    val sampleRate: Int = 8000
    val channelConfig: Int = AudioFormat.CHANNEL_CONFIGURATION_MONO
    val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
    val bufferSizeInBytes: Int = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    //AudioRecord
    val audioSource: Int = MediaRecorder.AudioSource.MIC
    @SuppressLint("MissingPermission")
    val audioRecord = AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSizeInBytes)
    val recBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    //AudioTrack
    val streamType: Int = AudioManager.STREAM_MUSIC
    //val audioTrack = AudioTrack(streamType, sampleRate, channelConfig, audioFormat, bufferSizeInBytes, AudioTrack.MODE_STREAM)
    val playBufSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    val PCM_FileName = MainActivity.ROOT_DIR_PATH + "/kevinAudio.pcm"

    private var _binding: FragmentClientBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClientBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.clientNextPage.setOnClickListener {
            findNavController().navigate(R.id.ThreedFragment)
        }

        binding.clientAudioRecord.setOnClickListener {
            Log.d("kevin", "clientAudioRecord")
            isRecording = true
            isPlaying = false
            Record2FileThread()
        }

        binding.clientAudioTrack.setOnClickListener {
            Log.d("kevin", "clientAudioTrack")
            isRecording = false
            isPlaying = true

            val audioInputStream = AudioInputStream()
            PlayThread(audioInputStream)
        }

    }


    fun Record2FileThread() {
        Thread {
            try {
                val pcmFile = File(PCM_FileName)

                val buffer = ByteArray(recBufSize)
                val bos = BufferedOutputStream(FileOutputStream(pcmFile), recBufSize)
                audioRecord.startRecording() //开始录制
                while (isRecording) {
                    //从MIC保存数据到缓冲区
                    val bufferReadResult: Int = audioRecord.read(
                        buffer, 0,
                        recBufSize
                    )
                    val tmpBuf = ByteArray(bufferReadResult)
                    System.arraycopy(buffer, 0, tmpBuf, 0, bufferReadResult)
                    //写入数据
                    bos.write(tmpBuf, 0, tmpBuf.size)
                }
                bos.flush()
                bos.close()
                audioRecord.stop()
            } catch (t: Throwable) {
            }
        }.start()
    }

    fun PlayThread(audioInputStream: AudioInputStream) {
        val audioTrack = AudioOutputStream()
        Thread {
            try {
                val buffer = ByteArray(playBufSize)
                val bis = audioInputStream //BufferedInputStream(FileInputStream(PCM_FileName), 1024) //playBufSize or 1024
                while (isPlaying && bis.read(buffer) != -1) {
                    //写入数据即播放
                    audioTrack.write(buffer, 0, buffer.size)
                }
                audioTrack.close()
                bis.close()
            } catch (t: Throwable) {
                Log.d("kevin", t.toString())
            }
        }.start()
    }


    inner class AudioOutputStream(
        streamType: Int = AudioManager.STREAM_MUSIC,
        sampleRate: Int = 8000,
        channelConfig: Int = AudioFormat.CHANNEL_OUT_MONO,
        audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
        bufferSizeInBytes: Int = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)) : OutputStream() {

        private val audioTrack = AudioTrack(streamType, sampleRate, channelConfig, audioFormat, bufferSizeInBytes, AudioTrack.MODE_STREAM).apply {
            play()
            setVolume(16f)
        }

        @Deprecated("Use write(audioData, offset, length)")
        @Throws(IOException::class)
        override fun write(b: Int) {
            val tmp = byteArrayOf(0)
            tmp[0] = b.toByte()
            write(tmp, 0, 1)
        }

        @Throws(IOException::class)
        override fun write(audioData: ByteArray, offset: Int, length: Int) {
            try {
                audioTrack.write(audioData, offset, length)
            } catch (e: Exception) {
                throw IOException(e)
            }
        }
        override fun close() {
            audioTrack.stop()
            audioTrack.release()
            super.close()
        }
    }
}

