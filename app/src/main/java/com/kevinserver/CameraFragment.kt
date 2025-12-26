package com.kevinserver

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
import androidx.fragment.app.Fragment
import com.kevinserver.databinding.FragmentCameraBinding
import java.io.*
import android.view.LayoutInflater

import android.content.pm.PackageManager

import android.content.Intent
import android.text.method.ScrollingMovementMethod
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.disposables.Disposable
import java.lang.Exception
import java.text.SimpleDateFormat
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
        var queryPreviewIndex = 0

        lateinit var observableEmitter: ObservableEmitter<Int>
    }


    //懸浮視窗
    private lateinit var mWindowManager: WindowManager
    private var isFloatView = false


    //camera
    private lateinit var mSurfaceHolder: SurfaceHolder
    private lateinit var mFloatView: SurfaceView
    private lateinit var mSurfaceHandlerCallback: SurfaceHolder.Callback
    private var mPreviewCallback = Camera.PreviewCallback{ data, camera ->
        imgRow = data
    }
    private val mIdlePreviewTime = 30

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


        activity?.let { showWifiIp(it) }
        initView(view)
        timerHomeCheck()
        timeOutStopPreview()

        val observable = Observable.create<Int>() {
            observableEmitter = it
        }.subscribe {
            if (it == 1) {
                switchNightMode()
            }
        }
    }

    private fun initView(view: View) {
        mSurfaceHolder = binding.svPreview.getHolder()

        mSurfaceHandlerCallback = object: SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                camera = Camera.open()
                printLog("CAMERA OPEN +++  surfaceCreated")

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

//                val support = parameters?.supportedPreviewSizes
//                support?.let {
//                    for (i in 0..it.size) {
//                        Log.d("surfaceChanged", "supportedPreviewSizes = ${it[i].width} * ${it[i].height}")
//                    }
//                }

                parameters?.pictureFormat = ImageFormat.JPEG
                parameters?.previewFormat = ImageFormat.NV21
                parameters?.setPreviewSize(1280, 720)
                parameters?.focusMode = "continuous-video"
                parameters?.whiteBalance = "twilight"
                //parameters?.exposureCompensation = 12

                camera?.setParameters(parameters)
                camera?.setPreviewCallback(mPreviewCallback)
            }

            override fun surfaceDestroyed(p0: SurfaceHolder) {
                printLog(  "CAMERA CLOSE ---  surfaceDestroyed ---")
            }
        }

        mSurfaceHolder.addCallback(mSurfaceHandlerCallback)

        binding.svPreview.setOnClickListener(View.OnClickListener {
            if (imgRow == null) {
                startPreview()
            } else {
                closePreview()
            }
        })
        binding.buttonTakePic.setOnClickListener {
            switchNightMode()
        }

        binding.tvLog.movementMethod = ScrollingMovementMethod.getInstance()
    }

    private fun startPreview() {
        printLog("start Preview")
        queryPreviewIndex = 0
        try {
            camera?.startPreview()
        } catch (e: Exception) {
            printLog("*** can not startPreview ***")
        }
    }

    private fun closePreview() {
        camera?.stopPreview()
        imgRow = null
    }


    private fun takePic() {
        camera?.takePicture(null, null, picture)
    }

    var switchExposure = false
    fun switchNightMode() {
        val parameters = camera?.getParameters()
        if (!switchExposure) {
            parameters?.maxExposureCompensation?.let {
                parameters?.exposureCompensation = it
                printLog("switch 夜間模式: $it")
            }
            switchExposure = true
        } else {
            printLog("close 夜間模式 ")
            parameters?.exposureCompensation = 0
            switchExposure = false
        }
        camera?.setParameters(parameters)
    }

    override fun onResume() {
        printLog( "Camera Fragment onResume +++")
        super.onResume()
        mSurfaceHolder.addCallback(mSurfaceHandlerCallback)
        camera?.setPreviewCallback(mPreviewCallback)
        Completable.timer(2, TimeUnit.SECONDS).subscribe { startPreview() }
    }

    override fun onDestroy() {
        super.onDestroy()
        printLog( "Camera Fragment onDestroy +++")
        timeOutStopPreviewDisposable?.dispose()
    }

    override fun onPause() {
        super.onPause()
        printLog( "Camera Fragment onPause +++")
    }

    var picture = PictureCallback { data, camera ->
        val pictureFile = getOutputMediaFile()
        pictureFile?.let {
            val fos = FileOutputStream(it)
            fos.write(data)
            fos.close()
        }
        startPreview()
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
                            2, 2,
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
                        Completable.timer(2, TimeUnit.SECONDS).subscribe { startPreview() }
                        isFloatView = true
                    }
                } else {
                    if (isFloatView) {
                        isFloatView = false
                        Completable.timer(300, TimeUnit.MILLISECONDS).subscribe {
                            printLog( "從懸浮視窗回歸")
                            mWindowManager.removeView(mFloatView)
                            mSurfaceHolder = binding.svPreview.getHolder()
                            mSurfaceHolder.addCallback(mSurfaceHandlerCallback)
                            startPreview()
                        }
                    }
                }
            }
        }, 100,500)
    }

    fun isHome(): Boolean {
        try {
            val mActivityManager = activity?.let { it.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager }
            val rti = mActivityManager?.getRunningTasks(1)
            val strs = getHomes()
            if (strs != null && strs.size > 0) {
                return strs.contains(rti?.get(0)?.topActivity?.getPackageName())
            } else {
                return false
            }
        } catch (e: Exception) {
            printLog(e.toString())
        }
        return false;
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

    var timeOutStopPreviewDisposable: Disposable ?= null
    private fun timeOutStopPreview() {
        timeOutStopPreviewDisposable = Observable.interval(1, TimeUnit.SECONDS).subscribe {
            queryPreviewIndex++
            if (queryPreviewIndex >= mIdlePreviewTime) {
                closePreview()
            }
        }
    }

    private fun printLog(msg: String) {
        Log.d("kevin", msg)
        binding.tvLog.post {
            binding.tvLog.let {
                val format = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
                it.append(
                    "${
                        format.format(Date(System.currentTimeMillis()))
                    } ->  $msg\n"
                )
                val offset = it.lineCount * it.lineHeight
                if (offset > it.height) {
                    it.scrollTo(0, offset - it.height)
                }
            }
        }
    }
}