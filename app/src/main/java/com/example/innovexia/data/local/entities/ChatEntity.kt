package com.example.innovexia.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a chat conversation.
 * Stored locally on-device after user consent.
 * Version 8: Added currentModel field for per-chat model selection
 */
@Entity(
    tableName = "chats",
    indices = [Index("ownerId"), Index("updatedAt"), Index("lastMsgAt")]
)
data class ChatEntity(
    @PrimaryKey
    val id: String, // UUID
    val ownerId: String, // "guest" or Firebase UID
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val personaName: String?,
    val personaInitial: String?,
    val personaColor: Long?,
    val memoryEnabled: Boolean = true,
    val summary: String = "",
    val summaryUpdatedAt: Long = 0,
    // Cloud sync fields
    val lastMsgAt: Long = 0, // Last message timestamp (for Firestore ordering)
    val msgCount: Int = 0, // Total message count
    val summaryHead: String? = null, // First ~12KB of summary
    val summaryHasChunks: Boolean = false, // True if summary stored in Storage
    // Soft-delete support
    val deletedAt: Long? = null, // Timestamp when soft-deleted in cloud (null = not deleted)
    // Local chat management (not synced to cloud initially)
    val pinned: Boolean = false, // True if chat is pinned to top
    val archived: Boolean = false, // True if chat is archived
    val deletedLocally: Boolean = false, // True if deleted locally (in trash, not synced to cloud unless permanently deleted)
    // Incognito mode (local-only, no cloud sync)
    val isIncognito: Boolean = false, // True if chat is in incognito mode (local-only)
    val cloudId: String? = null, // Firestore doc ID if ever uploaded to cloud
    // Model tracking
    val currentModel: String = "gemini-2.5-flash" // Current model for this chat
)
