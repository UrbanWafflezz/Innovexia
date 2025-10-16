package com.example.innovexia.core.health

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * Retrofit interface for health endpoints
 */
interface HealthRetrofitApi {
    /**
     * Get health status from any URL
     * Uses dynamic URL to support multiple services
     */
    @GET
    suspend fun getHealth(@Url fullUrl: String): Response<ResponseBody>
}
