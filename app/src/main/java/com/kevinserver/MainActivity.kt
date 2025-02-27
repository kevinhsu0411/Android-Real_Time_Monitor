package com.kevinserver

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kevinserver.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    //private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    companion object {
        lateinit var ROOT_DIR_PATH: String
    }

    private var mSharedPreferences: SharedPreferences?= null

    private lateinit var pm: PowerManager
    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ROOT_DIR_PATH = this.filesDir.toString()
        Log.d("KK", "rootPath = ${ROOT_DIR_PATH}")

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        navView.setupWithNavController(navController)

        mSharedPreferences = getSharedPreferences("server", MODE_PRIVATE)

        initPowerWakeLock()

        Thread {
            init_web_service()
        }.start()

        if (Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(getApplicationContext())) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(intent);
            }
        }
    }

    override fun onPause() {
        Log.d("kk", "onPause --")
        super.onPause()
    }

    override fun onDestroy() {
        Log.d("kk", "onDestroy --")
        super.onDestroy()
        wakeLock.release()
    }

    fun init_web_service() {
        mSharedPreferences?.getString("Init_Server_port", "8080")?.let {
            Log.d("KK", "init_web_service Server_port : $it")
            WebServer(this, it.toInt()).start()
        }
    }

    @SuppressLint("InvalidWakeLockTag")
    fun initPowerWakeLock() {
        pm = this.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.kevinserver")
        wakeLock.acquire()
    }


}