package com.kevinserver

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.hardware.Camera
import android.hardware.Camera.PictureCallback
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kevinserver.databinding.FragmentCameraBinding
import java.io.*
import android.view.LayoutInflater

import android.content.pm.PackageManager

import android.content.Intent
import io.reactivex.Completable
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class CameraFragment : Fragment() {


    companion object {
        var imgRow: ByteArray ?= null
        var camera: Camera? = null
    }


    private lateinit var mWindowManager: WindowManager
    private var isFloatView = false


    private lateinit var mSurfaceHolder: SurfaceHolder
    private lateinit var mFloatView: SurfaceView
    private lateinit var mSurfaceHandlerCallback: SurfaceHolder.Callback
    private var mPreviewCallback = Camera.PreviewCallback{ data, camera ->
        imgRow = data
    }

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
        timerHomeCheck()
    }

    private fun initView(view: View) {
        mSurfaceHolder = binding.svPreview.getHolder()

        mSurfaceHandlerCallback = object: SurfaceHolder.Callback {
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

            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
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
                camera?.setPreviewCallback(mPreviewCallback)
                camera?.startPreview()
            }

            override fun surfaceDestroyed(p0: SurfaceHolder) {
                Log.d("kk", "CAMERA CLOSE ---  surfaceDestroyed ---")
            }
        }

        mSurfaceHolder.addCallback(mSurfaceHandlerCallback)

        binding.svPreview.setOnClickListener(View.OnClickListener {
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
        Log.d("kk", "CAMERA onResume +++")
        super.onResume()
        mSurfaceHolder.addCallback(mSurfaceHandlerCallback)
        camera?.setPreviewCallback(mPreviewCallback)
        camera?.startPreview()
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

    fun timerHomeCheck() {
        val timerTask = Timer().schedule(object : TimerTask() {
            override fun run() {
                activity?.let { mWindowManager = it.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
                if (isHome()) {
                    if (!isFloatView) {
                        mFloatView = SurfaceView(context)
                        val windowLayout = WindowManager.LayoutParams(
                            200, 200,
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                            PixelFormat.TRANSLUCENT
                        )
                        windowLayout.gravity = Gravity.LEFT or Gravity.TOP
                        activity?.runOnUiThread {
                            mWindowManager.addView(mFloatView, windowLayout)
                        }

                        mSurfaceHolder = mFloatView.getHolder()
                        mSurfaceHolder.addCallback(mSurfaceHandlerCallback)
                        camera?.setPreviewCallback(mPreviewCallback)
                        camera?.startPreview()
                        isFloatView = true
                    }
                } else {
                    if (isFloatView) {
                        isFloatView = false
                        Completable.timer(300, TimeUnit.MILLISECONDS).subscribe {
                            Log.d("kk", "從懸浮視窗回歸")
                            mWindowManager.removeView(mFloatView)
                            mSurfaceHolder = binding.svPreview.getHolder()
                            mSurfaceHolder.addCallback(mSurfaceHandlerCallback)
                            camera?.setPreviewCallback(mPreviewCallback)
                            camera?.startPreview()
                        }
                    }
                }
            }
        }, 100,500)
    }

    fun isHome(): Boolean {
        val mActivityManager = activity?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val rti = mActivityManager.getRunningTasks(1);
        val strs = getHomes()
        if (strs != null && strs.size > 0) {
            return strs.contains(rti.get(0).topActivity?.getPackageName())
        } else {
            return false;
        }
    }

    private fun getHomes(): List<String>? {
        val names: MutableList<String> = ArrayList()
        val packageManager = activity?.getPackageManager()
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager?.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolveInfo != null) {
            for (info in resolveInfo) {
                names.add(info.activityInfo.packageName)
            }
        }
        return names
    }

}