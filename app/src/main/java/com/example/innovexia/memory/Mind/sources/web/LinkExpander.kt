package com.example.innovexia.memory.Mind.sources.web

import android.util.Log
import java.net.URL

/**
 * Expands links from a page by discovering subpages
 * Respects depth limits and domain boundaries
 */
class LinkExpander(
    private val maxDepth: Int = 2,
    private val maxPages: Int = 10
) {
    companion object {
        private const val TAG = "LinkExpander"

        // Patterns to skip (login, cart, search, etc.)
        private val SKIP_PATTERNS = listOf(
            "login", "signin", "signup", "register", "auth",
            "cart", "checkout", "payment",
            "search", "query",
            "logout", "signout",
            "admin", "api",
            "download", "pdf", "zip", "doc", "docx"
        )
    }

    data class PageToFetch(
        val url: String,
        val depth: Int
    )

    /**
     * Expand links from a starting URL
     * @param startUrl The initial URL to fetch
     * @param startDepth Starting depth (usually 0)
     * @return List of pages to fetch (including the start URL)
     */
    suspend fun expand(startUrl: String, startDepth: Int = 0): List<PageToFetch> {
        Log.d(TAG, "Expanding links from: $startUrl (maxDepth=$maxDepth, maxPages=$maxPages)")

        val toVisit = mutableListOf(PageToFetch(startUrl, startDepth))
        val visited = mutableSetOf<String>()
        val result = mutableListOf<PageToFetch>()

        val startUrlObj = URL(startUrl)
        val baseDomain = startUrlObj.host
        val basePathPrefix = getPathPrefix(startUrlObj)

        while (toVisit.isNotEmpty() && result.size < maxPages) {
            val current = toVisit.removeAt(0)

            // Skip if already visited
            val normalizedUrl = normalizeUrl(current.url)
            if (normalizedUrl in visited) continue

            // Check robots.txt
            if (!RobotsPolicy.isAllowed(normalizedUrl)) {
                Log.d(TAG, "Skipping (robots.txt): $normalizedUrl")
                continue
            }

            // Add to result
            result.add(current)
            visited.add(normalizedUrl)

            Log.d(TAG, "Added page ${result.size}/$maxPages: $normalizedUrl (depth=${current.depth})")

            // Stop expanding if we've reached max depth or max pages
            if (current.depth >= maxDepth || result.size >= maxPages) {
                continue
            }

            // Fetch and parse the page to discover more links
            try {
                val fetcher = HtmlFetcher()
                val fetchResult = fetcher.fetch(normalizedUrl)

                val parser = HtmlParser()
                val pageData = parser.parse(fetchResult.content, fetchResult.finalUrl)

                // Filter and add child links
                val childLinks = filterLinks(
                    links = pageData.links,
                    baseDomain = baseDomain,
                    basePathPrefix = basePathPrefix,
                    visited = visited
                )

                childLinks.forEach { link ->
                    toVisit.add(PageToFetch(link, current.depth + 1))
                }

                Log.d(TAG, "Discovered ${childLinks.size} valid links from $normalizedUrl")

            } catch (e: Exception) {
                Log.w(TAG, "Failed to expand links from $normalizedUrl: ${e.message}")
            }
        }

        Log.d(TAG, "Link expansion complete: ${result.size} pages to fetch")
        return result
    }

    /**
     * Filter links to only include internal, valid links
     */
    private fun filterLinks(
        links: List<String>,
        baseDomain: String,
        basePathPrefix: String,
        visited: Set<String>
    ): List<String> {
        return links
            .filter { url ->
                try {
                    val parsed = URL(url)

                    // Must be same domain
                    if (parsed.host != baseDomain) {
                        return@filter false
                    }

                    // Must have same path prefix (stay within section)
                    if (!parsed.path.startsWith(basePathPrefix)) {
                        return@filter false
                    }

                    // Skip if already visited
                    if (normalizeUrl(url) in visited) {
                        return@filter false
                    }

                    // Skip patterns (login, cart, etc.)
                    val urlLower = url.lowercase()
                    if (SKIP_PATTERNS.any { urlLower.contains(it) }) {
                        return@filter false
                    }

                    // Must be HTTP/HTTPS
                    parsed.protocol == "http" || parsed.protocol == "https"

                } catch (e: Exception) {
                    false
                }
            }
            .distinct()
    }

    /**
     * Get path prefix from URL (e.g., "/docs/" from "https://site.com/docs/guide")
     * This keeps crawling within the same section of a site
     */
    private fun getPathPrefix(url: URL): String {
        val path = url.path
        if (path.isEmpty() || path == "/") {
            return "/"
        }

        // Get first path segment
        val segments = path.split("/").filter { it.isNotEmpty() }
        return if (segments.isNotEmpty()) {
            "/${segments[0]}/"
        } else {
            "/"
        }
    }

    /**
     * Normalize URL by removing fragment and trailing slash
     */
    private fun normalizeUrl(url: String): String {
        return try {
            val parsed = URL(url)
            val normalized = URL(parsed.protocol, parsed.host, parsed.port, parsed.path).toString()

            // Remove trailing slash (unless it's the root)
            if (normalized.endsWith("/") && normalized.length > 1) {
                normalized.dropLast(1)
            } else {
                normalized
            }
        } catch (e: Exception) {
            url
        }
    }
}
