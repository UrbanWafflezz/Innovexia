package com.example.innovexia.memory.Mind.sources.indexer

import com.example.innovexia.memory.Mind.sources.SourcesConfig
import kotlin.math.min

/**
 * Data class representing a page of text
 */
data class PageText(
    val pageNumber: Int,  // 1-based
    val text: String
)

/**
 * Data class representing a chunk of text
 */
data class TextChunk(
    val pageStart: Int,   // 1-based
    val pageEnd: Int,     // 1-based, inclusive
    val text: String
)

/**
 * Chunks text content with overlap for better context preservation
 */
class Chunker(private val config: SourcesConfig) {

    /**
     * Chunk a list of pages into overlapping text chunks
     */
    fun chunk(pages: List<PageText>): List<TextChunk> {
        if (pages.isEmpty()) return emptyList()

        val chunks = mutableListOf<TextChunk>()
        val sb = StringBuilder()
        var startPage = pages.first().pageNumber
        var currentPage = startPage
        var currentLength = 0

        for (page in pages) {
            var content = normalizeText(page.text)
            currentPage = page.pageNumber

            while (content.isNotEmpty()) {
                val spaceLeft = config.chunkChars - currentLength
                val toTake = min(spaceLeft, content.length)

                // Add text to current chunk
                if (currentLength > 0 && sb.isNotEmpty()) {
                    sb.append(" ")
                    currentLength++
                }
                sb.append(content.substring(0, toTake))
                currentLength += toTake
                content = content.substring(toTake)

                // If chunk is full, save it and start a new one
                if (currentLength >= config.chunkChars) {
                    val chunkText = sb.toString().trim()
                    if (chunkText.isNotEmpty()) {
                        chunks.add(
                            TextChunk(
                                pageStart = startPage,
                                pageEnd = currentPage,
                                text = chunkText
                            )
                        )
                    }

                    // Reset for next chunk with overlap
                    sb.clear()
                    currentLength = 0
                    startPage = currentPage

                    // Add overlap from the previous chunk
                    if (chunks.isNotEmpty() && config.chunkOverlap > 0) {
                        val prevText = chunks.last().text
                        val overlapText = prevText.takeLast(config.chunkOverlap)
                        sb.append(overlapText)
                        currentLength = overlapText.length
                    }
                }
            }
        }

        // Add final chunk if there's remaining text
        if (sb.isNotEmpty()) {
            val chunkText = sb.toString().trim()
            if (chunkText.isNotEmpty()) {
                chunks.add(
                    TextChunk(
                        pageStart = startPage,
                        pageEnd = currentPage,
                        text = chunkText
                    )
                )
            }
        }

        return chunks
    }

    /**
     * Normalize text by removing excessive whitespace and cleaning up
     */
    private fun normalizeText(text: String): String {
        return text
            .replace(Regex("\\s+"), " ")  // Collapse whitespace
            .replace(Regex("[\\x00-\\x1F]"), " ")  // Remove control characters
            .trim()
    }
}
