package com.example.innovexia.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a persona.
 * Stored locally on-device, can be backed up to Firebase.
 * Public personas are Firebase-only (not stored locally unless imported).
 */
@Entity(
    tableName = "personas",
    indices = [Index("ownerId"), Index("updatedAt"), Index("isDefault")]
)
data class PersonaEntity(
    @PrimaryKey
    val id: String, // UUID
    val ownerId: String, // "guest" or Firebase UID
    val name: String,
    val initial: String, // 1 character uppercase
    val color: Long, // ARGB color
    val summary: String,
    val tags: List<String> = emptyList(), // TypeConverter applied at Database level
    val system: String? = null, // Optional system prompt/instructions
    val createdAt: Long, // Timestamp millis
    val updatedAt: Long, // Timestamp millis
    val lastUsedAt: Long? = null, // Last time persona was selected/used
    val isDefault: Boolean = false, // Auto-select in new chats
    // Persona 2.0 extended settings (JSON)
    val extendedSettings: String? = null, // JSON string of PersonaDraftDto extended fields
    // Cloud sync fields
    val cloudId: String? = null, // Firestore document ID (null if not synced)
    val lastSyncedAt: Long? = null // Last successful sync timestamp
)
