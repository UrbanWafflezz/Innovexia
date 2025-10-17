package com.example.innovexia.memory.Mind.ingest

/**
 * Text normalization utilities
 */
object Normalizers {

    /**
     * Normalize text for embedding
     */
    fun normalize(text: String): String {
        return text
            .trim()
            .replace(Regex("\\s+"), " ") // collapse whitespace
            .replace(Regex("[\\x00-\\x1F\\x7F]"), "") // remove control chars
            .take(2000) // limit length
    }

    /**
     * Create a deduplication key from text
     */
    fun dedupKey(text: String): String {
        return text
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(200)
    }

    /**
     * Check if text is too short to be meaningful
     * Changed from 3 words to 2 words minimum to be less strict
     */
    fun isTooShort(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return true
        val wordCount = trimmed.split("\\s+".toRegex()).size
        return wordCount < 2 // Changed from 3 to 2
    }

    /**
     * Check if text is just a greeting/farewell
     * Only filters out single-word greetings/simple responses
     */
    fun isGreeting(text: String): Boolean {
        val trimmed = text.trim()
        val lower = trimmed.lowercase()
        val wordCount = trimmed.split("\\s+".toRegex()).size

        // Only filter single-word simple greetings
        if (wordCount > 1) return false

        val greetings = setOf(
            "hi", "hello", "hey", "goodbye", "bye", "thanks",
            "ok", "okay", "sure", "yes", "no"
        )
        return greetings.contains(lower)
    }
}
