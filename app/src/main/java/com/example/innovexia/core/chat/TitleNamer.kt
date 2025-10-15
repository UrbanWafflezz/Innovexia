package com.example.innovexia.core.chat

/**
 * Auto-generates chat titles from first user/model messages.
 * Titles are limited to 25 characters (concise, uses marquee scrolling in UI).
 */
object TitleNamer {
    private const val MAX_LENGTH = 25

    /**
     * Generate a title from the first user and/or model message.
     * @param firstUser First user message text
     * @param firstModel First model response text
     * @return Title limited to 25 characters (concise, full text shows with marquee)
     */
    fun from(firstUser: String?, firstModel: String?): String {
        val raw = when {
            !firstUser.isNullOrBlank() -> firstUser
            !firstModel.isNullOrBlank() -> firstModel
            else -> "Chat"
        }.trim()

        val normalized = raw
            .replace(Regex("[\\n\\r]+"), " ")
            .replace(Regex("\\s+"), " ")
            .removePrefix("Innovexia:")
            .removePrefix("Assistant:")
            .removePrefix("User:")
            .trim()

        // No ellipsis - let marquee handle overflow in UI
        return normalized.take(MAX_LENGTH)
    }
}
