package com.kevinserver

import android.app.Activity
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.Camera
import android.hardware.Camera.PictureCallback
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kevinserver.databinding.FragmentCameraBinding
import java.io.*
import java.lang.Exception
import java.util.*

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class CameraFragment : Fragment(), SurfaceHolder.Callback, Camera.PreviewCallback {


    companion object {
        var imgRow: ByteArray ?= null
        var camera: Camera? = null
    }

    private lateinit var mSurfaceHolder: SurfaceHolder
    private lateinit var mSurfaceView: SurfaceView

    private var _binding: FragmentCameraBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }

        activity?.let { showWifiIp(it) }
        initView(view)
    }

    private fun initView(view: View) {
        mSurfaceView = view.findViewById<View>(R.id.svPreview) as SurfaceView
        mSurfaceHolder = mSurfaceView.getHolder()
        mSurfaceHolder.addCallback(this)
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        mSurfaceView.setOnClickListener(View.OnClickListener {
            binding.coverImage.visibility = View.VISIBLE
        })
        binding.buttonTakePic.setOnClickListener {
            takePic()
        }

        binding.coverImage.setOnClickListener {
            it.isVisible = false
        }
    }


    fun takePic() {
        camera?.takePicture(null, null, picture)
    }

    override fun onResume() {
        Log.d("kk", "CAMERA onResume ++")
        super.onResume()
        mSurfaceHolder.addCallback(this)
        camera?.setPreviewCallback(this)
        camera?.startPreview()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        camera = Camera.open()
        Log.d("kk", "CAMERA OPEN +++  surfaceCreated")

//        if (Build.VERSION.SDK_INT >= 8) { //轉90度
//            camera.setDisplayOrientation(90)
//        }

        try {
            camera?.setPreviewDisplay(holder)
        } catch (exception: IOException) {
            camera?.release()
        }
    }



    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) { // 取得相機參數
        val parameters = camera?.getParameters()

        val sizes = parameters?.supportedPictureSizes
        if (sizes != null) {
            for (size in sizes) {
                Log.i("KK", "Available resolution: ${size.width} , ${size.height}")
            }
        }

        parameters?.pictureFormat = ImageFormat.JPEG
        parameters?.previewFormat = ImageFormat.NV21
        parameters?.setPreviewSize(640, 480)

        camera?.setParameters(parameters)
        camera?.setPreviewCallback(this)
        camera?.startPreview()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d("kk", "CAMERA CLOSE +++  surfaceDestroyed ---")
        mSurfaceHolder.removeCallback(this)
        camera?.stopPreview()
    }

    var picture = PictureCallback { data, camera ->
        val pictureFile = getOutputMediaFile()
        pictureFile?.let {
            val fos = FileOutputStream(it)
            fos.write(data)
            fos.close()
        }
        camera.startPreview()
    }


    private fun getOutputMediaFile(): File? {
        val mediaStorageDir = File(MainActivity.ROOT_DIR_PATH, "camera")
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null
            }
        }
        val mediaFile = File(mediaStorageDir.path + File.separator + "IMG_Kevin.jpg")
        return mediaFile
    }

    override fun onPreviewFrame(data: ByteArray, camera: Camera?) {
        imgRow = data
    }


    fun showWifiIp(activity: Activity) {
        val wifiManager = activity.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ip = wifiInfo.ipAddress
        val ip_String =
            (ip and 0xff).toString() + "." + (ip shr 8 and 0xff) + "." + (ip shr 16 and 0xff) + "." + (ip shr 24 and 0xff)
        if (ip == 0) {
            binding.wifiIp.setText(R.string.not_wifi_ip)
        } else {
            binding.wifiIp.setText("http://" + ip_String + ":8080")
        }
    }

}