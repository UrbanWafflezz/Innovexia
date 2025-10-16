package com.example.innovexia.ui.viewmodels

/**
 * Stream collector that aggregates tokens and ensures final flush.
 * Prevents tail text from being lost when streaming completes.
 */
class StreamCollector(
    private val onUpdate: (String) -> Unit,
    private val onDone: (String) -> Unit
) {
    private val sb = StringBuilder()

    /**
     * Append a token to the accumulated text.
     * Updates UI periodically (every 8 chars or on newline).
     */
    fun onToken(token: String) {
        sb.append(token)
        // Update UI every 8 characters or on newline for smooth streaming
        if (sb.length % 8 == 0 || token.contains('\n')) {
            onUpdate(sb.toString())
        }
    }

    /**
     * Complete the stream - ensures final flush so no text is lost.
     * MUST be called when streaming completes successfully.
     */
    fun complete() {
        onUpdate(sb.toString())  // Final flush
        onDone(sb.toString())
    }

    /**
     * Get current accumulated text without triggering updates.
     */
    fun current(): String = sb.toString()
}
