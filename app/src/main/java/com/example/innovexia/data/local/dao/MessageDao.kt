package com.example.innovexia.data.local.dao

import androidx.room.*
import com.example.innovexia.data.local.entities.MessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for message operations.
 * All queries are scoped by ownerId to isolate guest and user data.
 */
@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt ASC")
    fun forChat(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt ASC")
    suspend fun forChatSync(chatId: String): List<MessageEntity>

    @Update
    suspend fun update(message: MessageEntity)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteForChat(chatId: String)

    /**
     * Delete all messages for a specific owner.
     */
    @Query("DELETE FROM messages WHERE ownerId = :owner")
    suspend fun deleteAllFor(owner: String)

    @Query("DELETE FROM messages")
    suspend fun deleteAll()

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt ASC LIMIT 1")
    suspend fun firstMessage(chatId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun lastN(chatId: String, limit: Int): List<MessageEntity>

    /**
     * Get a specific message by ID
     */
    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getById(messageId: String): MessageEntity?

    /**
     * Reassign all messages from one owner to another.
     * Used when merging Guest chats into a user account.
     */
    @Query("UPDATE messages SET ownerId = :to WHERE ownerId = :from AND chatId = :chatId")
    suspend fun reassignOwner(chatId: String, from: String, to: String)

    /**
     * Update stream state for a message (for in-place regeneration)
     */
    @Query("UPDATE messages SET streamState = :state, updatedAt = :now, error = NULL WHERE id = :id")
    suspend fun updateStreamState(id: String, state: String, now: Long)

    /**
     * Overwrite message text (for streaming updates)
     */
    @Query("UPDATE messages SET text = :text, updatedAt = :now WHERE id = :id")
    suspend fun overwriteText(id: String, text: String, now: Long)

    /**
     * Increment regeneration count
     */
    @Query("UPDATE messages SET regenCount = regenCount + 1, updatedAt = :now WHERE id = :id")
    suspend fun bumpRegen(id: String, now: Long)

    /**
     * Mark message with error
     */
    @Query("UPDATE messages SET error = :error, streamState = :state, updatedAt = :now WHERE id = :id")
    suspend fun markError(id: String, error: String?, state: String, now: Long)
}
