package com.kevinserver

import android.app.Activity
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.Camera
import android.util.Base64
import android.util.Log
import com.lidroid.xutils.util.MimeTypeUtils
import elonen.NanoHTTPD
import java.io.*
import java.lang.Exception


class WebServer(val activity: Activity, port: Int) : NanoHTTPD(port) {

    val MIME_HTML = "text/html"
    val MIME_JSON = "application/json"

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
                    return newFixedLengthResponse(Response.Status.OK, mimeType, fis, 100000000)
                }


                if (uri.endsWith("preview")) {
                    if (CameraFragment.imgRow == null) {
                        CameraFragment.camera = Camera.open()
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
            }


            var filename = uri
            if (filename == "/") filename = "/index.html"

            var inputStream: InputStream? = null

            try {
                inputStream = activity.getAssets().open("web" + filename)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return newChunkedResponse(Response.Status.OK, MIME_HTML, inputStream)
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