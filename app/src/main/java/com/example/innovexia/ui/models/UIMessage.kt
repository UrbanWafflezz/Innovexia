package com.example.innovexia.ui.models

import com.example.innovexia.data.models.AttachmentMeta

enum class MessageStatus {
    SENDING,     // Waiting for Gemini to start responding
    STREAMING,   // Actively receiving response
    COMPLETE     // Response finished
}

/**
 * UI representation of a chat message.
 * Used for display in the message list.
 */
data class UIMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val attachments: List<AttachmentMeta> = emptyList(),
    val status: MessageStatus = MessageStatus.COMPLETE,
    val modelName: String? = null // Model used to generate this response (null for user messages)
)
