package com.example.innovexia.memory.Mind.sources.web

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Handles robots.txt parsing and caching
 * Respects website crawling policies
 */
object RobotsPolicy {
    private const val TAG = "RobotsPolicy"
    private const val USER_AGENT = "InnovexiaSourcesBot/1.0 (+local)"
    private const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours

    // Cache: host -> RobotRules
    private val cache = mutableMapOf<String, CachedRules>()
    private val mutex = Mutex()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    data class CachedRules(
        val rules: RobotRules,
        val timestamp: Long
    )

    data class RobotRules(
        val disallowedPaths: List<String>,
        val allowedPaths: List<String>,
        val crawlDelay: Long? = null
    ) {
        fun isAllowed(path: String): Boolean {
            // Check explicit allows first (more specific)
            if (allowedPaths.any { path.startsWith(it) }) {
                return true
            }

            // Check disallows
            if (disallowedPaths.any { path.startsWith(it) }) {
                return false
            }

            // Default allow if no rules match
            return true
        }
    }

    /**
     * Check if a URL is allowed to be crawled based on robots.txt
     * @param url Full URL to check
     * @return true if allowed, false if disallowed
     */
    suspend fun isAllowed(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val parsedUrl = URL(url)
            val host = parsedUrl.host
            val path = parsedUrl.path.ifEmpty { "/" }

            val rules = getRulesForHost(host)
            val allowed = rules.isAllowed(path)

            Log.d(TAG, "URL check: $url -> ${if (allowed) "ALLOWED" else "DISALLOWED"}")
            allowed

        } catch (e: Exception) {
            Log.e(TAG, "Error checking robots.txt for $url", e)
            // Default to allowed if we can't fetch/parse robots.txt
            true
        }
    }

    /**
     * Get crawl delay for a host (in milliseconds)
     * @return delay in ms, or null if no delay specified
     */
    suspend fun getCrawlDelay(url: String): Long? = withContext(Dispatchers.IO) {
        try {
            val host = URL(url).host
            val rules = getRulesForHost(host)
            rules.crawlDelay
        } catch (e: Exception) {
            Log.e(TAG, "Error getting crawl delay for $url", e)
            null
        }
    }

    /**
     * Get or fetch rules for a host
     */
    private suspend fun getRulesForHost(host: String): RobotRules {
        mutex.withLock {
            // Check cache
            val cached = cache[host]
            if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_DURATION_MS) {
                Log.d(TAG, "Using cached robots.txt for $host")
                return cached.rules
            }

            // Fetch and parse robots.txt
            val rules = fetchAndParseRobotsTxt(host)
            cache[host] = CachedRules(rules, System.currentTimeMillis())

            return rules
        }
    }

    /**
     * Fetch and parse robots.txt for a host
     */
    private suspend fun fetchAndParseRobotsTxt(host: String): RobotRules =
        withContext(Dispatchers.IO) {
            try {
                val robotsUrl = "https://$host/robots.txt"
                Log.d(TAG, "Fetching robots.txt from $robotsUrl")

                val request = Request.Builder()
                    .url(robotsUrl)
                    .header("User-Agent", USER_AGENT)
                    .build()

                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.w(TAG, "robots.txt not found for $host (${response.code}), allowing all")
                    return@withContext RobotRules(emptyList(), emptyList())
                }

                val content = response.body?.string() ?: ""
                parseRobotsTxt(content)

            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch robots.txt for $host, allowing all: ${e.message}")
                // Default to allow all if robots.txt is unreachable
                RobotRules(emptyList(), emptyList())
            }
        }

    /**
     * Parse robots.txt content
     * Simplified parser that looks for our user agent or "*"
     */
    private fun parseRobotsTxt(content: String): RobotRules {
        val disallowedPaths = mutableListOf<String>()
        val allowedPaths = mutableListOf<String>()
        var crawlDelay: Long? = null

        var relevantSection = false
        val lines = content.lines()

        for (line in lines) {
            val trimmed = line.trim()

            // Skip comments and empty lines
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            // Check for User-agent directive
            if (trimmed.startsWith("User-agent:", ignoreCase = true)) {
                val agent = trimmed.substringAfter(":").trim()
                relevantSection = agent == "*" || agent.contains("InnovexiaSourcesBot", ignoreCase = true)
                continue
            }

            // Only process rules in relevant section
            if (!relevantSection) continue

            when {
                trimmed.startsWith("Disallow:", ignoreCase = true) -> {
                    val path = trimmed.substringAfter(":").trim()
                    if (path.isNotEmpty()) {
                        disallowedPaths.add(path)
                    }
                }

                trimmed.startsWith("Allow:", ignoreCase = true) -> {
                    val path = trimmed.substringAfter(":").trim()
                    if (path.isNotEmpty()) {
                        allowedPaths.add(path)
                    }
                }

                trimmed.startsWith("Crawl-delay:", ignoreCase = true) -> {
                    val delay = trimmed.substringAfter(":").trim().toDoubleOrNull()
                    if (delay != null) {
                        crawlDelay = (delay * 1000).toLong() // Convert to milliseconds
                    }
                }
            }
        }

        Log.d(TAG, "Parsed robots.txt: disallow=${disallowedPaths.size}, allow=${allowedPaths.size}, delay=$crawlDelay")
        return RobotRules(disallowedPaths, allowedPaths, crawlDelay)
    }

    /**
     * Clear the cache (useful for testing)
     */
    suspend fun clearCache() {
        mutex.withLock {
            cache.clear()
        }
    }
}
