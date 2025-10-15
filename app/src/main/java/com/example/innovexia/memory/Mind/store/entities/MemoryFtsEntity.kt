package com.example.innovexia.memory.Mind.store.entities

import androidx.room.Entity
import androidx.room.Fts4

/**
 * FTS4 table for full-text search on memory text
 * Using FTS4 instead of FTS5 for better Android compatibility
 * Note: We manually sync this with MemoryEntity instead of using contentEntity
 */
@Fts4
@Entity(tableName = "memories_fts")
data class MemoryFtsEntity(
    val id: String,
    val text: String
)
