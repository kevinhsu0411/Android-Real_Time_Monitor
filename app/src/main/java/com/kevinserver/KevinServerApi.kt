package com.kevinserver

import android.util.Log
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Streaming

interface KevinServerApi {

    @Streaming
    @GET("api/audio")
    fun getAudioStream():Call<ResponseBody>


    @Streaming
    @GET("api/preview")
    fun getCameraStream():Call<ResponseBody>

    companion object {
        @Volatile
        private var instance: KevinServerApi? = null

        fun getInstance(ip: String): KevinServerApi = instance?: synchronized(KevinServerApi::class.java){
            instance?:Retrofit.Builder()
            .baseUrl("http://$ip:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KevinServerApi::class.java)
            .also {
                if ( it.getCameraStream().execute().code() == 200 ) {
                    Log.d("kk", "KevinServerApi Retrofit2 = 200")
                    instance = it
                }
            }
        }
    }
}