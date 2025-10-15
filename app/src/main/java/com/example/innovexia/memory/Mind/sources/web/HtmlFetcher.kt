package com.example.innovexia.memory.Mind.sources.web

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Fetches HTML content from URLs with rate limiting and retry logic
 */
class HtmlFetcher {
    companion object {
        private const val TAG = "HtmlFetcher"
        private const val USER_AGENT = "InnovexiaSourcesBot/1.0 (+local)"
        private const val TIMEOUT_SECONDS = 5L
        private const val MAX_RETRIES = 2
        private const val MIN_RATE_LIMIT_MS = 1000L // 1 request per second per domain
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // Per-domain rate limiting: domain -> last request timestamp
    private val domainTimestamps = mutableMapOf<String, Long>()
    private val rateLimitMutex = Mutex()

    data class FetchResult(
        val url: String,
        val finalUrl: String,      // After redirects
        val contentType: String,
        val content: String,
        val statusCode: Int
    )

    /**
     * Fetch HTML content from a URL with rate limiting and retries
     * @param url URL to fetch
     * @return FetchResult on success, throws exception on failure
     */
    suspend fun fetch(url: String): FetchResult = withContext(Dispatchers.IO) {
        val parsedUrl = URL(url)
        val domain = parsedUrl.host

        // Apply rate limiting per domain
        applyRateLimit(domain)

        // Check robots.txt-specified crawl delay
        val crawlDelay = RobotsPolicy.getCrawlDelay(url)
        if (crawlDelay != null && crawlDelay > MIN_RATE_LIMIT_MS) {
            Log.d(TAG, "Applying robots.txt crawl delay: ${crawlDelay}ms for $domain")
            applyRateLimit(domain, crawlDelay)
        }

        // Retry with exponential backoff
        var lastException: Exception? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                if (attempt > 0) {
                    val backoffMs = 1000L * (1 shl attempt) // 1s, 2s, 4s...
                    Log.d(TAG, "Retrying fetch (attempt ${attempt + 1}/$MAX_RETRIES) after ${backoffMs}ms: $url")
                    delay(backoffMs)
                }

                return@withContext executeFetch(url)

            } catch (e: Exception) {
                Log.w(TAG, "Fetch attempt ${attempt + 1}/$MAX_RETRIES failed: $url - ${e.message}")
                lastException = e
            }
        }

        // All retries failed
        throw lastException ?: Exception("Failed to fetch $url after $MAX_RETRIES attempts")
    }

    /**
     * Execute the actual HTTP fetch
     */
    private suspend fun executeFetch(url: String): FetchResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Fetching: $url")

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.5")
            .header("Accept-Encoding", "gzip, deflate, br")
            .build()

        val response: Response = httpClient.newCall(request).execute()

        try {
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }

            val contentType = response.header("Content-Type") ?: "text/html"

            // Only accept HTML content
            if (!contentType.contains("html", ignoreCase = true)) {
                throw Exception("Non-HTML content type: $contentType")
            }

            val content = response.body?.string()
                ?: throw Exception("Empty response body")

            val finalUrl = response.request.url.toString()

            Log.d(TAG, "Successfully fetched: $finalUrl (${content.length} bytes)")

            FetchResult(
                url = url,
                finalUrl = finalUrl,
                contentType = contentType,
                content = content,
                statusCode = response.code
            )

        } finally {
            response.close()
        }
    }

    /**
     * Apply rate limiting per domain
     * Ensures minimum delay between requests to the same domain
     */
    private suspend fun applyRateLimit(domain: String, minDelayMs: Long = MIN_RATE_LIMIT_MS) {
        rateLimitMutex.withLock {
            val lastRequest = domainTimestamps[domain] ?: 0L
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequest

            if (elapsed < minDelayMs) {
                val waitTime = minDelayMs - elapsed
                Log.d(TAG, "Rate limiting: waiting ${waitTime}ms before fetching from $domain")
                delay(waitTime)
            }

            domainTimestamps[domain] = System.currentTimeMillis()
        }
    }

    /**
     * Clear rate limit timestamps (useful for testing)
     */
    fun clearRateLimits() {
        domainTimestamps.clear()
    }
}
