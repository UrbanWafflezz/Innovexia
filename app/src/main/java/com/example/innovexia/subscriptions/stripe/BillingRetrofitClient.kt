package com.example.innovexia.subscriptions.stripe

import android.os.Build
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit client configuration for billing API
 */
object BillingRetrofitClient {

    // For Android emulator accessing localhost
    private const val EMULATOR_BASE_URL = "http://10.0.2.2:4242/"

    // For physical device on same WiFi network (replace with your computer's IP)
    private const val PHYSICAL_DEVICE_BASE_URL = "http://10.0.0.53:4242/"

    // For production
    private const val PRODUCTION_BASE_URL = "https://your-server.com/"

    /**
     * Auto-detect if running on emulator or physical device
     */
    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || "google_sdk" == Build.PRODUCT)
    }

    // Automatically choose the right URL based on device type
    private val BASE_URL = if (isEmulator()) EMULATOR_BASE_URL else PHYSICAL_DEVICE_BASE_URL

    private val gson = GsonBuilder()
        .setLenient()
        .create()

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
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val api: BillingApi = retrofit.create(BillingApi::class.java)
}
