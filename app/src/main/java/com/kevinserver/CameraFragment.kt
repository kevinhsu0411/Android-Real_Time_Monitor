package com.kevinserver

import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.hardware.Camera
import android.hardware.Camera.AutoFocusCallback
import android.hardware.Camera.PictureCallback
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
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
        lateinit var imgRow: ByteArray
    }

    private lateinit var mSurfaceHolder: SurfaceHolder
    private lateinit var mSurfaceView: SurfaceView
    private lateinit var camera: Camera
    private var is_Keep_Capture = false


    private var _binding: FragmentCameraBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentCameraBinding.inflate(inflater, container, false)

//        val Databinding = activity?.let { DataBindingUtil.setContentView<FragmentCameraBinding>(
//            it.parent, R.layout.fragment_camera) }
//        Databinding?.data = this

        return binding?.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
            is_Keep_Capture = false
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }


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
            Log.d("kk", "isVisible: ${it.isVisible}")
            it.isVisible = false
        }
    }


    fun takePic() {
        Log.d("KK", " +++  Capture ")
        camera.takePicture(null, null, picture)
    }
    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        camera = Camera.open()
        Log.d("kk", "CAMERA OPEN +++  surfaceCreated")

//        if (Build.VERSION.SDK_INT >= 8) { //轉90度
//            camera.setDisplayOrientation(90)
//        }

        try {
            camera.setPreviewDisplay(holder)
        } catch (exception: IOException) {
            camera.release()
        }
    }



    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) { // 取得相機參數
        Log.d("kk", "CAMERA OPEN +++  surfaceChanged")
        val parameters = camera.getParameters()

        var sizes = parameters.supportedPictureSizes

        // Iterate through all available resolutions and choose one.
        // The chosen resolution will be stored in mSize.
        for (size in sizes) {
            Log.i("KK", "Available resolution: ${size.width} , ${size.height}")
        }

        parameters.pictureFormat = ImageFormat.JPEG
        parameters.previewFormat = ImageFormat.NV21
        parameters.setPreviewSize(640, 480)

        camera.setParameters(parameters)
        camera.startPreview()
        camera.setPreviewCallback(this)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d("kk", "CAMERA CLOSE +++  surfaceDestroyed ---")
        camera.stopPreview()
        camera.release()
    }

    var picture = PictureCallback { data, camera ->
        Log.d("kk", "+onPictureTaken")

        val pictureFile = getOutputMediaFile()
        if (pictureFile == null) {
            Log.d("kk", "pictureFile = null")
            return@PictureCallback
        }
        try {
            //write the file
            val fos = FileOutputStream(pictureFile)
            fos.write(data)
            fos.close()
        } catch (e: FileNotFoundException) {
            Log.d("kk", "FileNotFoundException:$e")
        } catch (e: IOException) {
            Log.d("kk", "IOException:$e")
        }
        camera.startPreview()
    }


    private fun getOutputMediaFile(): File? {
        val mediaStorageDir = File(MainActivity.ROOT_DIR_PATH, "camera")
        //if this "JCGCamera folder does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null
            }
        }

        //take the current timeStamp
        val mediaFile = File(mediaStorageDir.path + File.separator + "IMG_Kevin.jpg")
        Log.d("kk", "mediaFile :" + mediaFile.absolutePath)
        return mediaFile
    }

    override fun onPreviewFrame(data: ByteArray, camera: Camera?) {
        imgRow = data
    }


}