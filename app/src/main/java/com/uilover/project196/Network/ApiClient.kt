package com.uilover.project196.Network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    
    // Backend URL - Now using VPS hosting! 🎉
    private const val USE_LOCAL_DEVELOPMENT = false // Set to true for local development, false for production VPS
    private const val VPS_IP = "139.162.13.11" // Your VPS IP address
    private const val LOCAL_IP = "192.168.56.1" // Your computer's local IP address (for development)
    
    private val BASE_URL = if (USE_LOCAL_DEVELOPMENT) {
        "http://$LOCAL_IP:8080/" // For local development
    } else {
        "http://$VPS_IP:8080/" // For production VPS
    }
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val authService: AuthApiService = retrofit.create(AuthApiService::class.java)
} 