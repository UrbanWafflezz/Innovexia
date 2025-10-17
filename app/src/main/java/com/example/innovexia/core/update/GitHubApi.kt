package com.example.innovexia.core.update

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import com.example.innovexia.BuildConfig
import java.util.concurrent.TimeUnit

/**
 * GitHub Releases API interface
 */
interface GitHubApi {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<GitHubRelease>

    companion object {
        private const val BASE_URL = "https://api.github.com/"

        // Optional: Add your GitHub Personal Access Token here to avoid rate limits
        // Create one at: https://github.com/settings/tokens (no scopes needed for public repos)
        // For production, store this in local.properties or Firebase Remote Config
        private const val GITHUB_TOKEN = "" // Leave empty for anonymous access

        fun create(): GitHubApi {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .apply {
                    // Add authentication header if token is provided
                    if (GITHUB_TOKEN.isNotBlank()) {
                        addInterceptor(Interceptor { chain ->
                            val request = chain.request().newBuilder()
                                .addHeader("Authorization", "Bearer $GITHUB_TOKEN")
                                .addHeader("Accept", "application/vnd.github+json")
                                .addHeader("X-GitHub-Api-Version", "2022-11-28")
                                .build()
                            chain.proceed(request)
                        })
                    } else {
                        // Add standard GitHub API headers for anonymous access
                        addInterceptor(Interceptor { chain ->
                            val request = chain.request().newBuilder()
                                .addHeader("Accept", "application/vnd.github+json")
                                .addHeader("X-GitHub-Api-Version", "2022-11-28")
                                .build()
                            chain.proceed(request)
                        })
                    }

                    // Add HTTP logging in debug builds
                    if (BuildConfig.DEBUG) {
                        val loggingInterceptor = HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        }
                        addInterceptor(loggingInterceptor)
                    }

                    // Add retry interceptor for transient failures
                    addInterceptor(RetryInterceptor(maxRetries = 3))
                }
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GitHubApi::class.java)
        }
    }
}

/**
 * Interceptor to retry failed requests with exponential backoff
 */
private class RetryInterceptor(private val maxRetries: Int = 3) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        var attempt = 0
        var lastException: Exception? = null

        while (attempt < maxRetries) {
            try {
                val response = chain.proceed(chain.request())

                // Retry on 5xx server errors or 429 rate limit
                if (response.code in 500..599 || response.code == 429) {
                    response.close()
                    attempt++

                    if (attempt < maxRetries) {
                        // Exponential backoff: 1s, 2s, 4s
                        val backoffMillis = (1000L * (1 shl (attempt - 1))).coerceAtMost(5000L)
                        Thread.sleep(backoffMillis)
                        continue
                    }
                }

                return response
            } catch (e: Exception) {
                lastException = e
                attempt++

                if (attempt < maxRetries) {
                    // Exponential backoff for network errors
                    val backoffMillis = (1000L * (1 shl (attempt - 1))).coerceAtMost(5000L)
                    Thread.sleep(backoffMillis)
                } else {
                    throw e
                }
            }
        }

        throw lastException ?: Exception("Max retries exceeded")
    }
}
