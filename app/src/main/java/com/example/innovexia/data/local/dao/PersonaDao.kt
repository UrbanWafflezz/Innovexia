package com.example.innovexia.data.local.dao

import androidx.room.*
import com.example.innovexia.data.local.entities.PersonaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonaDao {
    /**
     * Observe all personas for an owner, ordered by most recently updated
     */
    @Query("SELECT * FROM personas WHERE ownerId = :ownerId ORDER BY updatedAt DESC")
    fun observeForOwner(ownerId: String): Flow<List<PersonaEntity>>

    /**
     * Get all personas for an owner (one-shot)
     */
    @Query("SELECT * FROM personas WHERE ownerId = :ownerId ORDER BY updatedAt DESC")
    suspend fun getForOwner(ownerId: String): List<PersonaEntity>

    /**
     * Get default persona for an owner
     */
    @Query("SELECT * FROM personas WHERE ownerId = :ownerId AND isDefault = 1 LIMIT 1")
    suspend fun getDefaultPersona(ownerId: String): PersonaEntity?

    /**
     * Get persona by ID
     */
    @Query("SELECT * FROM personas WHERE id = :id")
    suspend fun getById(id: String): PersonaEntity?

    /**
     * Insert or replace a persona
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(persona: PersonaEntity)

    /**
     * Insert multiple personas
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(personas: List<PersonaEntity>)

    /**
     * Delete persona by ID
     */
    @Query("DELETE FROM personas WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Update persona name and initial
     */
    @Query("UPDATE personas SET name = :name, initial = :initial, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateName(id: String, name: String, initial: String, updatedAt: Long)

    /**
     * Set/unset default persona
     */
    @Query("UPDATE personas SET isDefault = :isDefault, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setDefault(id: String, isDefault: Boolean, updatedAt: Long)

    /**
     * Clear default flag for all personas of an owner
     */
    @Query("UPDATE personas SET isDefault = 0, updatedAt = :updatedAt WHERE ownerId = :ownerId")
    suspend fun clearAllDefaults(ownerId: String, updatedAt: Long)

    /**
     * Get all personas that need cloud sync (not synced or updated after last sync)
     */
    @Query("""
        SELECT * FROM personas
        WHERE ownerId = :ownerId
        AND (cloudId IS NULL OR lastSyncedAt IS NULL OR updatedAt > lastSyncedAt)
    """)
    suspend fun getUnsyncedForOwner(ownerId: String): List<PersonaEntity>

    /**
     * Update cloud sync metadata
     */
    @Query("UPDATE personas SET cloudId = :cloudId, lastSyncedAt = :syncedAt WHERE id = :id")
    suspend fun updateSyncMetadata(id: String, cloudId: String, syncedAt: Long)

    /**
     * Update lastUsedAt timestamp for a persona
     */
    @Query("UPDATE personas SET lastUsedAt = :lastUsedAt WHERE id = :id")
    suspend fun updateLastUsed(id: String, lastUsedAt: Long)

    /**
     * Delete all personas for an owner (for account deletion/data clear)
     */
    @Query("DELETE FROM personas WHERE ownerId = :ownerId")
    suspend fun deleteAllForOwner(ownerId: String)
}
