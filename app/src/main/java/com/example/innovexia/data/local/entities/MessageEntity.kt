package com.example.innovexia.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.innovexia.data.models.AttachmentMeta
import com.example.innovexia.data.models.AttachmentMetaSerializer

/**
 * Message status enum for tracking message lifecycle
 */
enum class MsgStatus {
    SENDING,  // Message is being sent
    SENT,     // Message successfully sent
    FAILED    // Message failed to send
}

/**
 * Stream state enum for assistant messages
 */
enum class StreamState {
    IDLE,      // Not currently streaming
    STREAMING, // Currently streaming tokens
    ERROR      // Stream failed with error
}

/**
 * Room entity representing a message in a chat.
 * role: "user" | "model" | "system"
 * streamed: true when token stream is complete
 * Version 8: Added modelUsed field for provenance tracking
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chatId"), Index("ownerId"), Index("createdAt")]
)
data class MessageEntity(
    @PrimaryKey
    val id: String, // UUID
    val ownerId: String, // "guest" or Firebase UID
    val chatId: String,
    val role: String, // "user" | "model" | "system"
    val text: String, // Full text (local)
    val createdAt: Long,
    val streamed: Boolean = true, // true when stream is finished
    // Cloud sync fields
    val updatedAt: Long = createdAt, // Last update timestamp
    val textHead: String? = null, // First ~12KB of text (for Firestore)
    val hasChunks: Boolean = false, // True if overflow stored in Storage
    val attachmentsJson: String? = null, // JSON array of AttachmentMeta
    val replyToId: String? = null, // Optional reply-to message ID
    // Soft-delete support
    val deletedAt: Long? = null, // Timestamp when soft-deleted in cloud (null = not deleted)
    // UserBubbleV2 fields
    val status: String = "SENT", // "SENDING" | "SENT" | "FAILED" (stored as String for Room compatibility)
    val editedAt: Long? = null, // Timestamp when edited
    val supersedesMessageId: String? = null, // Points to prior user msg if this is an edit-resend
    val replacedAssistantId: String? = null, // Optional: the assistant msg this edit supersedes
    // Incognito mode
    val localOnly: Boolean = false, // True if message is local-only (incognito chat)
    // In-place regeneration fields
    val streamState: String = "IDLE", // "IDLE" | "STREAMING" | "ERROR" (stored as String for Room compatibility)
    val regenCount: Int = 0, // Increments each regeneration
    val error: String? = null, // Error message if streamState == ERROR
    // Model used to generate this message (for provenance tracking)
    val modelUsed: String = "gemini-2.5-flash",
    // Grounding metadata (JSON-encoded GroundingMetadata)
    val groundingJson: String? = null,
    // Grounding status ("NONE" | "SEARCHING" | "SUCCESS" | "FAILED")
    val groundingStatus: String = "NONE"
) {
    /**
     * Parse attachments from JSON
     */
    fun attachments(): List<AttachmentMeta> {
        return AttachmentMetaSerializer.fromJson(attachmentsJson)
    }

    /**
     * Helper to create updated entity with attachments
     */
    fun withAttachments(attachments: List<AttachmentMeta>): MessageEntity {
        return copy(attachmentsJson = AttachmentMetaSerializer.toJson(attachments))
    }

    /**
     * Get message status as enum (renamed to avoid conflict with auto-generated getStatus())
     */
    fun getStatusEnum(): MsgStatus {
        return try {
            MsgStatus.valueOf(status)
        } catch (e: Exception) {
            MsgStatus.SENT
        }
    }

    /**
     * Helper to create updated entity with status
     */
    fun withStatus(newStatus: MsgStatus): MessageEntity {
        return copy(status = newStatus.name)
    }

    /**
     * Get stream state as enum
     */
    fun getStreamStateEnum(): StreamState {
        return try {
            StreamState.valueOf(streamState)
        } catch (e: Exception) {
            StreamState.IDLE
        }
    }

    /**
     * Helper to create updated entity with stream state
     */
    fun withStreamState(newState: StreamState): MessageEntity {
        return copy(streamState = newState.name)
    }

    /**
     * Parse grounding metadata from JSON
     */
    fun groundingMetadata(): com.example.innovexia.data.ai.GroundingMetadata? {
        return if (groundingJson != null) {
            try {
                com.example.innovexia.data.models.GroundingMetadataSerializer.fromJson(groundingJson)
            } catch (e: Exception) {
                android.util.Log.e("MessageEntity", "Failed to parse grounding metadata: ${e.message}")
                null
            }
        } else null
    }

    /**
     * Helper to create updated entity with grounding metadata
     */
    fun withGroundingMetadata(metadata: com.example.innovexia.data.ai.GroundingMetadata?): MessageEntity {
        return copy(groundingJson = if (metadata != null) {
            com.example.innovexia.data.models.GroundingMetadataSerializer.toJson(metadata)
        } else null)
    }

    /**
     * Get grounding status as enum
     */
    fun getGroundingStatusEnum(): com.example.innovexia.data.ai.GroundingStatus {
        return try {
            com.example.innovexia.data.ai.GroundingStatus.valueOf(groundingStatus)
        } catch (e: Exception) {
            com.example.innovexia.data.ai.GroundingStatus.NONE
        }
    }

    /**
     * Helper to create updated entity with grounding status
     */
    fun withGroundingStatus(status: com.example.innovexia.data.ai.GroundingStatus): MessageEntity {
        return copy(groundingStatus = status.name)
    }
}
