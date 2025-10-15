package com.example.innovexia.memory.Mind.store.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Main memory storage entity
 */
@Entity(
    tableName = "memories",
    indices = [
        Index("personaId"),
        Index("userId"),
        Index("chatId"),
        Index("kind"),
        Index("createdAt"),
        Index("lastAccessed")
    ]
)
data class MemoryEntity(
    @PrimaryKey val id: String,
    val personaId: String,
    val userId: String,
    val chatId: String?,
    val role: String, // "user" | "model"
    val text: String,
    val kind: String, // FACT, EVENT, etc.
    val emotion: String?, // HAPPY, SAD, etc.
    val importance: Double, // 0.0 to 1.0
    val createdAt: Long,
    val lastAccessed: Long
)
