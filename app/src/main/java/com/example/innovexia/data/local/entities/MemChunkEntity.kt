package com.example.innovexia.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for semantic memory chunks.
 * Structure prepared for future RAG (embeddings will be added later).
 * Each chunk represents a summarized portion of the conversation.
 */
@Entity(
    tableName = "mem_chunks",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chatId")]
)
data class MemChunkEntity(
    @PrimaryKey
    val id: String, // UUID
    val chatId: String,
    val text: String, // Summarized text snippet
    val startIndex: Int, // Starting message index (inclusive)
    val endIndex: Int // Ending message index (inclusive)
    // Future: val embedding: ByteArray? for vector similarity
)
