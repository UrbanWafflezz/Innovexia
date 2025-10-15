package com.example.innovexia.data.local.dao

import androidx.room.*
import com.example.innovexia.data.local.entities.ChatEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for chat operations.
 * All queries are scoped by ownerId to isolate guest and user data.
 */
@Dao
interface ChatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chat: ChatEntity)

    @Update
    suspend fun update(chat: ChatEntity)

    @Query("UPDATE chats SET title = :title WHERE id = :chatId")
    suspend fun updateTitle(chatId: String, title: String)

    @Query("UPDATE chats SET updatedAt = :timestamp WHERE id = :chatId")
    suspend fun updateTimestamp(chatId: String, timestamp: Long)

    /**
     * Observe all chats for a specific owner (guest or user UID).
     */
    @Query("SELECT * FROM chats WHERE ownerId = :owner ORDER BY updatedAt DESC")
    fun observeChats(owner: String): Flow<List<ChatEntity>>

    /**
     * Legacy method - use observeChats(owner) instead.
     * @deprecated Use observeChats(owner) with ProfileId
     */
    @Deprecated("Use observeChats(owner) with ProfileId")
    @Query("SELECT * FROM chats ORDER BY updatedAt DESC")
    fun observeAllRecent(): Flow<List<ChatEntity>>

    /**
     * Get all chats for a specific owner synchronously.
     */
    @Query("SELECT * FROM chats WHERE ownerId = :owner ORDER BY updatedAt DESC")
    suspend fun getAllFor(owner: String): List<ChatEntity>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getById(chatId: String): ChatEntity?
    
    /**
     * Get all chats synchronously (for cloud sync operations).
     */
    @Query("SELECT * FROM chats ORDER BY updatedAt DESC")
    suspend fun getAllSync(): List<ChatEntity>

    @Delete
    suspend fun delete(chat: ChatEntity)

    /**
     * Delete all chats for a specific owner.
     */
    @Query("DELETE FROM chats WHERE ownerId = :owner")
    suspend fun deleteAllFor(owner: String)

    @Query("DELETE FROM chats")
    suspend fun deleteAll()

    @Query("UPDATE chats SET summary = :summary, summaryUpdatedAt = :timestamp WHERE id = :chatId")
    suspend fun updateSummary(chatId: String, summary: String, timestamp: Long)

    @Query("UPDATE chats SET memoryEnabled = :enabled WHERE id = :chatId")
    suspend fun setMemoryEnabled(chatId: String, enabled: Boolean)

    /**
     * Check if there are any chats for the guest owner.
     * Used to determine if we should show the "Import Guest chats" option.
     */
    @Query("SELECT COUNT(*) FROM chats WHERE ownerId = 'guest'")
    suspend fun countGuestChats(): Int

    /**
     * Observe recent chats (not archived, not deleted locally) for a specific owner,
     * with pinned chats first, then sorted by updatedAt.
     */
    @Query("""
        SELECT * FROM chats
        WHERE ownerId = :owner
        AND archived = 0
        AND deletedLocally = 0
        ORDER BY pinned DESC, updatedAt DESC
    """)
    fun observeRecentChats(owner: String): Flow<List<ChatEntity>>

    /**
     * Observe archived chats for a specific owner.
     */
    @Query("""
        SELECT * FROM chats
        WHERE ownerId = :owner
        AND archived = 1
        AND deletedLocally = 0
        ORDER BY updatedAt DESC
    """)
    fun observeArchivedChats(owner: String): Flow<List<ChatEntity>>

    /**
     * Observe deleted (trash) chats for a specific owner.
     */
    @Query("""
        SELECT * FROM chats
        WHERE ownerId = :owner
        AND deletedLocally = 1
        ORDER BY updatedAt DESC
    """)
    fun observeDeletedChats(owner: String): Flow<List<ChatEntity>>

    /**
     * Toggle pin status for a chat.
     */
    @Query("UPDATE chats SET pinned = NOT pinned WHERE id = :chatId")
    suspend fun togglePin(chatId: String)

    /**
     * Set pin status for a chat.
     */
    @Query("UPDATE chats SET pinned = :pinned WHERE id = :chatId")
    suspend fun setPin(chatId: String, pinned: Boolean)

    /**
     * Archive a chat (move to archived section).
     */
    @Query("UPDATE chats SET archived = 1, pinned = 0 WHERE id = :chatId")
    suspend fun archive(chatId: String)

    /**
     * Restore a chat from archive (move back to recent).
     */
    @Query("UPDATE chats SET archived = 0 WHERE id = :chatId")
    suspend fun restoreFromArchive(chatId: String)

    /**
     * Move a chat to trash (soft delete locally).
     */
    @Query("UPDATE chats SET deletedLocally = 1, pinned = 0, archived = 0 WHERE id = :chatId")
    suspend fun moveToTrash(chatId: String)

    /**
     * Restore a chat from trash.
     */
    @Query("UPDATE chats SET deletedLocally = 0 WHERE id = :chatId")
    suspend fun restoreFromTrash(chatId: String)

    /**
     * Permanently delete all chats in trash for a specific owner.
     */
    @Query("DELETE FROM chats WHERE ownerId = :owner AND deletedLocally = 1")
    suspend fun emptyTrash(owner: String)

    /**
     * Permanently delete a specific chat (from trash).
     */
    @Query("DELETE FROM chats WHERE id = :chatId AND deletedLocally = 1")
    suspend fun deleteForever(chatId: String)

    /**
     * Toggle incognito mode for a chat.
     */
    @Query("UPDATE chats SET isIncognito = :enabled, updatedAt = :timestamp WHERE id = :chatId")
    suspend fun setIncognito(chatId: String, enabled: Boolean, timestamp: Long)

    /**
     * Set cloud ID for a chat (used after uploading to cloud).
     */
    @Query("UPDATE chats SET cloudId = :cloudId WHERE id = :chatId")
    suspend fun setCloudId(chatId: String, cloudId: String)
}
