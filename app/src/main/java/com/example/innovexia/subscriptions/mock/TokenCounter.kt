package com.example.innovexia.subscriptions.mock

/**
 * Token counting utilities
 * Provides accurate token estimation for usage tracking
 */
object TokenCounter {

    /**
     * Estimate tokens from text
     * Uses a more accurate algorithm than simple char/4
     *
     * Based on common tokenization patterns:
     * - Average English word: ~1.3 tokens
     * - Punctuation: usually 1 token
     * - Numbers: 1-2 tokens depending on length
     * - Whitespace: not counted
     */
    fun estimateTokens(text: String): Long {
        if (text.isBlank()) return 0L

        // Split into words (including punctuation)
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }

        var tokenCount = 0L

        for (word in words) {
            tokenCount += when {
                // Very short words (1-2 chars) are usually 1 token
                word.length <= 2 -> 1

                // Common words (3-6 chars) are typically 1 token
                word.length in 3..6 -> 1

                // Medium words (7-12 chars) are often 1-2 tokens
                word.length in 7..12 -> {
                    // Check if it has punctuation
                    if (word.any { !it.isLetterOrDigit() }) 2 else 1
                }

                // Longer words get split into subword tokens
                // Roughly 1 token per 5-6 characters
                else -> (word.length / 5.5).toLong().coerceAtLeast(1)
            }

            // Count punctuation separately if at end of word
            if (word.lastOrNull()?.let { !it.isLetterOrDigit() } == true) {
                tokenCount += 1
            }
        }

        return tokenCount
    }

    /**
     * Estimate input tokens (user message)
     * Slightly more conservative estimate
     */
    fun estimateInputTokens(text: String): Long {
        return estimateTokens(text)
    }

    /**
     * Estimate output tokens from streamed response
     * Accumulates as tokens stream in
     */
    fun estimateOutputTokens(text: String): Long {
        return estimateTokens(text)
    }

    /**
     * Format token count for display
     */
    fun formatTokenCount(tokens: Long): String {
        return when {
            tokens >= 1_000_000 -> String.format("%.1fM", tokens / 1_000_000.0)
            tokens >= 1_000 -> String.format("%.1fK", tokens / 1_000.0)
            else -> tokens.toString()
        }
    }
}
