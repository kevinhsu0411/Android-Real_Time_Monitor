package com.kevinserver

import android.app.Activity
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import com.lidroid.xutils.util.MimeTypeUtils
import elonen.NanoHTTPD
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.io.*
import java.lang.Exception
import java.util.concurrent.TimeUnit


class WebServer(val activity: Activity, port: Int) : NanoHTTPD(port) {

    val MIME_HTML = "text/html"
    val MIME_JSON = "application/json"
    val MIME_MPEG = "audio/mpeg"

    private var mRecorder: MediaRecorder ?= null
    private val audioFileName = MainActivity.ROOT_DIR_PATH + "/kevinAudio.mp3"

    private lateinit var mAudioDisposable: Disposable
    init {
        Log.d("kk", "NanoHttpd init...")

        prepareMic()

    }

    fun prepareMic() {
        Thread {
            mRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFileName)
                try {
                    prepare()
                } catch (e: IOException) {
                    Log.e("kk", "prepare() failed")
                }
                start()

            }
        }.start()
    }

    fun startMic() {
        try {
            prepareMic()
        } catch (e: Exception) {

        }
    }

    fun stopMic() {
        try {
            mRecorder?.apply {
                stop()
                release()
            }
            mRecorder = null
        } catch (e: Exception) {

        }
    }

    override fun serve(session: IHTTPSession): Response {
        Log.d("KK", "serve uri: ${session.uri}")

        val uri = session.uri
        try {

            if (uri.startsWith("/api")) {


                if (uri.endsWith("pic")) {
                    //可呼叫相機拍照  等callback 再response
                    val target = File(MainActivity.ROOT_DIR_PATH + "/camera/IMG_Kevin.jpg")
                    val mimeType = MimeTypeUtils.getMimeType(target.getName())
                    val fis = FileInputStream(target)
                    Log.d("kk", "pic mimeType= $mimeType")
                    return newChunkedResponse(Response.Status.OK, mimeType, fis)
                }


                if (uri.endsWith("preview")) {
                    CameraFragment.queryPreviewIndex = 0
                    if (CameraFragment.imgRow == null) {
                        CameraFragment.camera?.startPreview()
                    } else {
                        val imgC = YuvImage(CameraFragment.imgRow, ImageFormat.NV21, 640, 480, null)
                        val outStream = ByteArrayOutputStream()
                        imgC.compressToJpeg(Rect(0, 0, 640, 480), 80, outStream)
                        try {
                            outStream.flush()
                            outStream.close()
                        } catch (e: IOException) {
                            // TODO Auto-generated catch block
                            e.printStackTrace()
                        }

                        val image_bytes: ByteArray = outStream.toByteArray()
                        val img_base64 = "data:image/jpeg;base64,${Base64.encodeToString(image_bytes, 2)}"
                        return newFixedLengthResponse(Response.Status.OK, MIME_JSON, " \"$img_base64\" "
                        )
                    }
                }

                if (uri.endsWith("audio_Play")) {
                    val target = File(audioFileName)
                    val mimeType = MimeTypeUtils.getMimeType(target.getName())
                    val fileLength = target.length()
                    val fis = FileInputStream(target)

                    Log.d("kk", "audio mimeType= $mimeType")
                    Log.d("kk", "AudioFis= $target")
                    return newChunkedResponse(Response.Status.OK, mimeType, fis)
                }

                if (uri.endsWith("audio_Start")) {
                    mAudioDisposable = Observable.interval(5, TimeUnit.SECONDS).subscribe({
                       stopMic()
                        Completable.timer(300, TimeUnit.MILLISECONDS).subscribe {
                            startMic()
                        }
                    }, {
                        Log.e("kk", "Error ${it.printStackTrace()}")
                    })
                    return newFixedLengthResponse(Response.Status.OK,"", "")
                }

                if (uri.endsWith("audio_Stop")) {
                    mAudioDisposable.dispose()
                    //startMic()
                    return newFixedLengthResponse(Response.Status.OK, "", "")
                }


            }

            return if (uri.endsWith("favicon.ico")) {
                newChunkedResponse(Response.Status.OK, MIME_HTML, null)
            } else {
                var filename = uri
                if (filename == "/") filename = "/index.html"

                var inputStream: InputStream? = null

                try {
                    inputStream = activity.getAssets().open("web" + filename)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                newChunkedResponse(Response.Status.OK, MIME_HTML, inputStream)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ResponseException) {
            e.printStackTrace()
        }
        return repons404(session, uri)
    }

    private fun repons404(session: IHTTPSession?, url: String?): Response {
        return newFixedLengthResponse("<!DOCTYPE html><html><body>Sorry, We Can't Find $url !</body></html>\n")
    }

}