package com.example.innovexia.subscriptions.stripe

import android.os.Build
import com.example.innovexia.BuildConfig
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
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

    // For production - UPDATE THIS with your Render.com URL after deployment
    // Example: "https://innovexia-stripe-server.onrender.com/"
    private const val PRODUCTION_BASE_URL = "https://your-render-app.onrender.com/"

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

    /**
     * Determine which URL to use based on build type and device
     * - Debug builds: Use local server (emulator or physical device)
     * - Release builds: Use production server
     */
    private val BASE_URL = when {
        BuildConfig.DEBUG && isEmulator() -> {
            println("🔧 Billing: Using EMULATOR local server")
            EMULATOR_BASE_URL
        }
        BuildConfig.DEBUG -> {
            println("🔧 Billing: Using PHYSICAL DEVICE local server")
            PHYSICAL_DEVICE_BASE_URL
        }
        else -> {
            println("🚀 Billing: Using PRODUCTION server")
            PRODUCTION_BASE_URL
        }
    }

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val okHttpClient = OkHttpClient.Builder()
        .apply {
            // Only add logging interceptor in debug builds
            // HttpLoggingInterceptor is not available in release builds
            if (BuildConfig.DEBUG) {
                try {
                    val loggingInterceptor = Class.forName("okhttp3.logging.HttpLoggingInterceptor")
                        .getDeclaredConstructor()
                        .newInstance()
                    val levelMethod = loggingInterceptor.javaClass.getMethod("setLevel", Class.forName("okhttp3.logging.HttpLoggingInterceptor\$Level"))
                    val levelEnum = Class.forName("okhttp3.logging.HttpLoggingInterceptor\$Level")
                        .getField("BODY")
                        .get(null)
                    levelMethod.invoke(loggingInterceptor, levelEnum)
                    addInterceptor(loggingInterceptor as okhttp3.Interceptor)
                } catch (e: Exception) {
                    // Logging interceptor not available, continue without it
                }
            }
        }
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
