package com.example.innovexia.memory.Mind.sources.web

import android.util.Log
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.safety.Safelist
import java.net.URL

/**
 * Parses HTML content and extracts text, metadata, and links
 */
class HtmlParser {
    companion object {
        private const val TAG = "HtmlParser"
        private const val MAX_TEXT_LENGTH = 15_000 // Max chars per page
    }

    data class PageData(
        val url: String,
        val title: String,
        val description: String?,
        val text: String,
        val links: List<String>
    )

    /**
     * Parse HTML content and extract readable text, metadata, and links
     * @param html Raw HTML content
     * @param baseUrl Base URL for resolving relative links
     * @return PageData with extracted information
     */
    fun parse(html: String, baseUrl: String): PageData {
        Log.d(TAG, "Parsing HTML from: $baseUrl")

        val doc: Document = Jsoup.parse(html, baseUrl)

        // Extract metadata
        val title = extractTitle(doc)
        val description = extractDescription(doc)

        // Remove unwanted elements
        cleanDocument(doc)

        // Extract readable text
        val text = extractText(doc)

        // Extract links
        val links = extractLinks(doc, baseUrl)

        Log.d(TAG, "Parsed: title='$title', text=${text.length} chars, links=${links.size}")

        return PageData(
            url = baseUrl,
            title = title,
            description = description,
            text = text,
            links = links
        )
    }

    /**
     * Extract page title from <title> tag or <meta property="og:title">
     */
    private fun extractTitle(doc: Document): String {
        // Try <title> tag first
        val titleTag = doc.select("title").firstOrNull()?.text()
        Log.d(TAG, "Title tag: '$titleTag'")
        if (!titleTag.isNullOrBlank()) {
            return titleTag.trim()
        }

        // Try Open Graph title
        val ogTitle = doc.select("meta[property=og:title]").firstOrNull()?.attr("content")
        Log.d(TAG, "OG title: '$ogTitle'")
        if (!ogTitle.isNullOrBlank()) {
            return ogTitle.trim()
        }

        // Try Twitter title
        val twitterTitle = doc.select("meta[name=twitter:title]").firstOrNull()?.attr("content")
        Log.d(TAG, "Twitter title: '$twitterTitle'")
        if (!twitterTitle.isNullOrBlank()) {
            return twitterTitle.trim()
        }

        // Try <h1> as fallback
        val h1 = doc.select("h1").firstOrNull()?.text()
        Log.d(TAG, "H1 title: '$h1'")
        if (!h1.isNullOrBlank()) {
            return h1.trim()
        }

        // Use domain name as last resort
        try {
            val url = URL(doc.baseUri())
            val domain = url.host
            if (!domain.isNullOrBlank()) {
                Log.d(TAG, "Using domain as title: '$domain'")
                return domain
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract domain from baseUri", e)
        }

        // Default
        Log.d(TAG, "No title found, using 'Untitled Page'")
        return "Untitled Page"
    }

    /**
     * Extract page description from <meta name="description">
     */
    private fun extractDescription(doc: Document): String? {
        // Try meta description
        val metaDesc = doc.select("meta[name=description]").firstOrNull()?.attr("content")
        if (!metaDesc.isNullOrBlank()) {
            return metaDesc.trim()
        }

        // Try Open Graph description
        val ogDesc = doc.select("meta[property=og:description]").firstOrNull()?.attr("content")
        if (!ogDesc.isNullOrBlank()) {
            return ogDesc.trim()
        }

        return null
    }

    /**
     * Clean document by removing unwanted elements
     */
    private fun cleanDocument(doc: Document) {
        // Remove script, style, and other non-content elements
        doc.select("script, style, noscript, iframe, embed, object").remove()

        // Remove navigation, headers, footers, ads, etc.
        doc.select("nav, header, footer, aside").remove()
        doc.select("[role=navigation], [role=banner], [role=complementary]").remove()
        doc.select(".nav, .navbar, .navigation, .menu").remove()
        doc.select(".header, .footer, .sidebar, .ad, .advertisement").remove()
        doc.select("#nav, #navbar, #navigation, #menu").remove()
        doc.select("#header, #footer, #sidebar, #ad").remove()

        // Remove hidden elements
        doc.select("[style*=display:none], [style*=visibility:hidden]").remove()
        doc.select("[hidden], [aria-hidden=true]").remove()
    }

    /**
     * Extract readable text from document body
     */
    private fun extractText(doc: Document): String {
        val body = doc.body() ?: return ""

        // Focus on main content areas
        val mainContent = doc.select("main, article, [role=main], .content, .main-content")
            .firstOrNull() ?: body

        // Extract text from content elements
        val textElements = mainContent.select("p, h1, h2, h3, h4, h5, h6, li, td, th, blockquote, pre, code")

        val sb = StringBuilder()
        for (element in textElements) {
            val text = element.text().trim()
            if (text.isNotBlank()) {
                sb.append(text).append("\n\n")
            }
        }

        // Fallback to full body text if nothing found
        var text = if (sb.isEmpty()) {
            mainContent.text()
        } else {
            sb.toString()
        }

        // Normalize whitespace
        text = text.replace(Regex("\\s+"), " ").trim()

        // Limit text length
        if (text.length > MAX_TEXT_LENGTH) {
            text = text.substring(0, MAX_TEXT_LENGTH)
            Log.d(TAG, "Text truncated to $MAX_TEXT_LENGTH chars")
        }

        return text
    }

    /**
     * Extract all links from the document
     * Returns absolute URLs only
     */
    private fun extractLinks(doc: Document, baseUrl: String): List<String> {
        val links = mutableSetOf<String>()

        doc.select("a[href]").forEach { element ->
            try {
                val href = element.attr("abs:href") // Get absolute URL
                if (href.isNotBlank() && isValidHttpUrl(href)) {
                    links.add(href)
                }
            } catch (e: Exception) {
                // Skip invalid URLs
            }
        }

        return links.toList()
    }

    /**
     * Check if URL is a valid HTTP/HTTPS URL
     */
    private fun isValidHttpUrl(url: String): Boolean {
        return try {
            val parsed = URL(url)
            parsed.protocol == "http" || parsed.protocol == "https"
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Strip all HTML tags and return plain text
     * Useful for preview or quick text extraction
     */
    fun stripHtml(html: String): String {
        return Jsoup.clean(html, Safelist.none())
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
