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
import com.kevinserver.databinding.FragmentClientBinding
import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.media.AudioRecord

import android.media.MediaRecorder

import java.io.*
import android.graphics.BitmapFactory
import android.util.Base64
import io.reactivex.Observable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.net.Socket


class ClientFragment : Fragment() {

    private var mSharedPreferences: SharedPreferences ?= null

    private val mSampleRate: Int = 8000
    private val mChannelConfig: Int = AudioFormat.CHANNEL_CONFIGURATION_MONO
    private val mAudioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
    private val mAudioTrackPlayBufSize: Int = AudioTrack.getMinBufferSize(mSampleRate, mChannelConfig, mAudioFormat)

    //AudioRecord
    private val mAudioSource: Int = MediaRecorder.AudioSource.MIC
    @SuppressLint("MissingPermission")
    val audioRecord = AudioRecord(mAudioSource, mSampleRate, mChannelConfig, mAudioFormat, mAudioTrackPlayBufSize)
    private val mRecordBufSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelConfig, mAudioFormat)
    private val mPCM_FileName = MainActivity.ROOT_DIR_PATH + "/kevinAudio.pcm"
    private var mPCM_thread = Thread{ }

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

        mSharedPreferences = activity?.getSharedPreferences("server", MODE_PRIVATE)

        mSharedPreferences?.getString("ip", "")?.let {
            binding.clientEdIP.setText(it)
        }

        mSharedPreferences?.getString("port_motor", "")?.let {
            binding.clientEdPORT3.setText(it)
        }

        mSharedPreferences?.getString("cameraPORT", "")?.let {
            binding.clientEdPORT.setText(it)
        }

        mSharedPreferences?.getString("cameraPORT2", "")?.let {
            binding.clientEdPORT2.setText(it)
        }

        // =============== button even ===================

        binding.clientPreviewBtn.setOnClickListener {
            Log.d("kevin", "client Preview")
            val cameraPORT = binding.clientEdPORT.text.toString()

            playRealTime_Camera_Stream(cameraPORT)
            setReceivePCM_RefreshInterval(cameraPORT)

            mSharedPreferences?.edit()?.putString("cameraPORT", cameraPORT)?.commit()
        }

        binding.clientPreview2Btn .setOnClickListener {
            Log.d("kevin", "client Preview2")
            val camera2PORT = binding.clientEdPORT2.text.toString()

            playRealTime_Camera_Stream(camera2PORT)
            setReceivePCM_RefreshInterval(camera2PORT)

            mSharedPreferences?.edit()?.putString("cameraPORT2", camera2PORT)?.commit()
        }

        binding.motorOpenButton.setOnClickListener {
            Log.d("kevin", "motorOpenButton")
            val serverIP = binding.clientEdIP.text.toString()
            val motorPORT = binding.clientEdPORT3.text.toString()
            Client(serverIP, motorPORT.toInt(), binding).setMotor(true)

            mSharedPreferences?.edit()?.putString("ip", serverIP)?.commit()
            mSharedPreferences?.edit()?.putString("port_motor", motorPORT)?.commit()
        }

        binding.motorCloseButton.setOnClickListener {
            Log.d("kevin", "motorCloseButton")
            val serverIP = binding.clientEdIP.text.toString()
            val serverPORT = binding.clientEdPORT.text.toString()
            Client(serverIP, serverPORT.toInt(), binding).setMotor(false)

            mSharedPreferences?.edit()?.putString("ip", serverIP)?.commit()
            mSharedPreferences?.edit()?.putString("port_motor", serverPORT)?.commit()
        }
    }

    @SuppressLint("CheckResult")
    private fun playRealTime_Camera_Stream(Port: String) {
        Thread {
            Observable.interval(100, TimeUnit.MILLISECONDS).subscribe ({
                val serverIP = binding.clientEdIP.text.toString()
                var base64 = ""
                val response = KevinServerApi.getInstance(serverIP, Port).getCameraStream().execute()
                response.body()?.let {
                    base64 = it.string()
                }

                if (response.isSuccessful && base64 != "") {
                    val decodedString = Base64.decode(base64.split(",")[1], Base64.DEFAULT)
                    val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                    activity?.runOnUiThread {
                        binding.clientImvPreview.setImageBitmap(decodedByte)
                    }
                }
            }, {
                Log.d("playRealTime_Camera_Stream", "${it}")
            })
        }.start()
    }

    private fun setReceivePCM_RefreshInterval(Port: String) {
        playRealTime_Audio_Stream(Port)
        Observable.interval(30, TimeUnit.SECONDS).subscribe({
            playRealTime_Audio_Stream(Port)
        }, {})
    }

    private fun playRealTime_Audio_Stream(Port: String) {
        mPCM_thread.interrupt()
        mPCM_thread = Thread {
            try {
                val serverIP = binding.clientEdIP.text.toString()
                val audioPlayer = AudioTrackPlayer()
                val response = KevinServerApi.getInstance(serverIP, Port).getAudioStream().execute()
                val inputStream = response.body()?.byteStream()

                if (response.isSuccessful && inputStream != null) {

                    mSharedPreferences?.edit()?.putString("ip", serverIP)?.commit()

                    val pcmStreamBuffer = ByteArray(mAudioTrackPlayBufSize)

                    while (inputStream.read(pcmStreamBuffer) != -1) {
                        audioPlayer.write(pcmStreamBuffer, 0, pcmStreamBuffer.size)
                    }
                    audioPlayer.close()
                }
            } catch (t: Throwable) {
                Log.e("kevin", t.toString())
            }
        }
        mPCM_thread.start()
    }

    private fun record2FileThread() {
        Thread {
            try {
                val pcmFile = File(mPCM_FileName)
                val buffer = ByteArray(mRecordBufSize)
                val bos = BufferedOutputStream(FileOutputStream(pcmFile), mRecordBufSize)
                audioRecord.startRecording()

                val isRecording = true
                while (isRecording) {
                    val bufferReadResult = audioRecord.read(buffer, 0, mRecordBufSize)
                    val tmpBuf = ByteArray(bufferReadResult)
                    System.arraycopy(buffer, 0, tmpBuf, 0, bufferReadResult)
                    bos.write(tmpBuf, 0, tmpBuf.size)
                }

                bos.flush()
                bos.close()
                audioRecord.stop()
            } catch (t: Throwable) { }
        }.start()
    }

    private fun playLocalFileThread() {
        Log.d("kevin", "播放錄音檔")
        val audioTrack = AudioTrackPlayer()
        Thread {
            try {
                val buffer = ByteArray(mAudioTrackPlayBufSize)
                val bis = BufferedInputStream(FileInputStream(mPCM_FileName), 1024)

                while (bis.read(buffer) != -1) {
                    audioTrack.write(buffer, 0, buffer.size)
                }

                audioTrack.close()
                bis.close()
            } catch (t: Throwable) { Log.d("playLocalFileThread", t.toString()) }
        }.start()
    }


    inner class AudioTrackPlayer(
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

    class Client(val address: String, val port: Int, val binding: FragmentClientBinding) {
        private lateinit var message: String

        fun setMotor(state: Boolean) {
            Log.d("kevin", "port = $port")
            GlobalScope.launch {
                try {
                    message = if (state) { "LED+" } else { "LED-" }

                    println("Connect to server at $address on port $port")
                    val client = Socket(address, port)
                    if (client.isConnected) {
                        client.outputStream.write(message.toByteArray())
                        Log.d("kevin","send $message OK")

                        val buffer = ByteArray(1024)
                        val byte = client.getInputStream().read(buffer)
                        if(byte > 0){
                            val finalByte = byte
                            val tmpMeassage = String(buffer,0,finalByte)
                            Log.d("kevin","recv ${tmpMeassage} OK")
                            binding.motorMsg.setText(tmpMeassage)
                        }

                        client.close()
                        Log.d("kevin","client close")
                    }

                } catch (e: Exception) {
                    Log.d("kevin", e.toString())
                }
            }
        }
    }


}

