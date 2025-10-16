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
     */
    fun isTooShort(text: String): Boolean {
        return text.trim().split("\\s+".toRegex()).size < 3
    }

    /**
     * Check if text is just a greeting/farewell
     */
    fun isGreeting(text: String): Boolean {
        val lower = text.lowercase().trim()
        val greetings = setOf(
            "hi", "hello", "hey", "goodbye", "bye", "thanks", "thank you",
            "ok", "okay", "sure", "yes", "no", "got it"
        )
        return greetings.contains(lower)
    }
}
