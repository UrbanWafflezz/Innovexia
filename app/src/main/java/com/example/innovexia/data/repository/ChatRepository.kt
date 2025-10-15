package com.example.innovexia.data.repository

import com.example.innovexia.core.auth.ProfileId
import com.example.innovexia.core.chat.TitleNamer
import com.example.innovexia.data.ai.ChatNamingService
import com.example.innovexia.data.ai.shouldUpdateChatTitle
import com.example.innovexia.data.local.AppDatabase
import com.example.innovexia.data.local.dao.ChatDao
import com.example.innovexia.data.local.dao.MessageDao
import com.example.innovexia.data.local.entities.ChatEntity
import com.example.innovexia.data.local.entities.MessageEntity
import com.example.innovexia.data.local.entities.MsgStatus
import com.example.innovexia.data.models.AttachmentMeta
import com.example.innovexia.data.models.AttachmentMetaSerializer
import com.example.innovexia.data.preferences.UserPreferences
import com.example.innovexia.ui.models.Persona
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.util.UUID

/**
 * Repository for chat and message operations.
 * Handles local storage and AI-powered title generation.
 * Now uses ProfileScopedRepository for auth-aware data scoping.
 */
class ChatRepository(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    database: AppDatabase? = null,
    private val context: android.content.Context? = null,
    private val userPreferences: UserPreferences? = null
) {
    private val chatNamingService = ChatNamingService()

    // Profile-scoped repository for auth-aware operations
    private val profileRepo: ProfileScopedRepository? = database?.let {
        ProfileScopedRepository(it, chatDao, messageDao)
    }

    // Cloud sync repository for Firebase operations
    private val cloudSyncRepo: CloudSyncRepository? = if (context != null && database != null) {
        CloudSyncRepository(context, chatDao, messageDao)
    } else null

    /**
     * Observe all recent chats, ordered by most recently updated.
     * Uses profile-scoped queries when available.
     */
    fun observeAllRecentChats(): Flow<List<ChatEntity>> {
        return profileRepo?.chats() ?: chatDao.observeAllRecent()
    }

    /**
     * Observe recent chats (not archived, not deleted), with pinned chats first.
     * Automatically updates when auth state changes.
     */
    fun observeRecentChats(): Flow<List<ChatEntity>> {
        return if (profileRepo != null) {
            profileRepo.profile.flatMapLatest { profileId ->
                val ownerId = profileId.toOwnerId()
                // Chats are cached by Room and only reload when database changes
                chatDao.observeRecentChats(ownerId)
            }
        } else {
            chatDao.observeRecentChats(ProfileId.GUEST_OWNER_ID)
        }
    }

    /**
     * Observe archived chats.
     * Automatically updates when auth state changes.
     */
    fun observeArchivedChats(): Flow<List<ChatEntity>> {
        return if (profileRepo != null) {
            profileRepo.profile.flatMapLatest { profileId ->
                chatDao.observeArchivedChats(profileId.toOwnerId())
            }
        } else {
            chatDao.observeArchivedChats(ProfileId.GUEST_OWNER_ID)
        }
    }

    /**
     * Observe deleted (trash) chats.
     * Automatically updates when auth state changes.
     */
    fun observeDeletedChats(): Flow<List<ChatEntity>> {
        return if (profileRepo != null) {
            profileRepo.profile.flatMapLatest { profileId ->
                chatDao.observeDeletedChats(profileId.toOwnerId())
            }
        } else {
            chatDao.observeDeletedChats(ProfileId.GUEST_OWNER_ID)
        }
    }

    /**
     * Get the profile-scoped repository for auth-aware operations.
     */
    fun getProfileRepository(): ProfileScopedRepository? = profileRepo

    /**
     * Observe a specific chat by ID.
     */
    fun observeChatById(chatId: String): Flow<ChatEntity?> {
        return kotlinx.coroutines.flow.flow {
            while (true) {
                emit(chatDao.getById(chatId))
                kotlinx.coroutines.delay(500) // Poll every 500ms
            }
        }
    }

    /**
     * Observe messages for a specific chat.
     */
    fun messagesForChat(chatId: String): Flow<List<MessageEntity>> {
        return messageDao.forChat(chatId)
    }

    /**
     * Get the last N messages for a chat (for previews)
     */
    suspend fun getLastMessages(chatId: String, limit: Int): List<MessageEntity> {
        return messageDao.lastN(chatId, limit)
    }

    /**
     * Get a specific message by ID
     */
    suspend fun getMessageById(messageId: String): MessageEntity? {
        val allMessages = messageDao.forChatSync("")
        return allMessages.find { it.id == messageId }
    }

    /**
     * Update a message
     */
    suspend fun updateMessage(message: MessageEntity) {
        messageDao.update(message)
        chatDao.updateTimestamp(message.chatId, System.currentTimeMillis())
    }

    /**
     * Start a new chat with the first user message.
     * Returns the generated chat ID.
     */
    suspend fun startChat(firstMessage: String, persona: Persona?, isIncognito: Boolean = false, attachments: List<AttachmentMeta> = emptyList()): String {
        val chatId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val ownerId = profileRepo?.getCurrentProfile()?.toOwnerId() ?: ProfileId.GUEST_OWNER_ID

        // Create chat entity
        val chat = ChatEntity(
            id = chatId,
            ownerId = ownerId,
            title = generateTitle(firstMessage),
            createdAt = now,
            updatedAt = now,
            personaName = persona?.name,
            personaInitial = persona?.initial,
            personaColor = persona?.colorHex,
            isIncognito = isIncognito
        )
        chatDao.insert(chat)

        // Add first user message
        val userMessage = MessageEntity(
            id = UUID.randomUUID().toString(),
            ownerId = ownerId,
            chatId = chatId,
            role = "user",
            text = firstMessage,
            createdAt = now,
            streamed = true,
            attachmentsJson = if (attachments.isNotEmpty()) {
                AttachmentMetaSerializer.toJson(attachments)
            } else null
        )
        messageDao.insert(userMessage)

        return chatId
    }

    /**
     * Append a user message to an existing chat.
     */
    suspend fun appendUserMessage(chatId: String, text: String, attachments: List<AttachmentMeta> = emptyList()) {
        val ownerId = profileRepo?.getCurrentProfile()?.toOwnerId() ?: ProfileId.GUEST_OWNER_ID

        val message = MessageEntity(
            id = UUID.randomUUID().toString(),
            ownerId = ownerId,
            chatId = chatId,
            role = "user",
            text = text,
            createdAt = System.currentTimeMillis(),
            streamed = true,
            attachmentsJson = if (attachments.isNotEmpty()) {
                AttachmentMetaSerializer.toJson(attachments)
            } else null
        )
        messageDao.insert(message)
        chatDao.updateTimestamp(chatId, System.currentTimeMillis())
    }

    /**
     * Append or update a model message token.
     * If messageId is null, creates a new message.
     */
    suspend fun appendModelToken(
        chatId: String,
        messageId: String?,
        token: String,
        isFinal: Boolean,
        groundingMetadata: com.example.innovexia.data.ai.GroundingMetadata? = null,
        groundingStatus: com.example.innovexia.data.ai.GroundingStatus? = null
    ): String {
        val now = System.currentTimeMillis()
        val ownerId = profileRepo?.getCurrentProfile()?.toOwnerId() ?: ProfileId.GUEST_OWNER_ID

        if (messageId == null) {
            // Create new model message
            val newId = UUID.randomUUID().toString()
            val message = MessageEntity(
                id = newId,
                ownerId = ownerId,
                chatId = chatId,
                role = "model",
                text = token,
                createdAt = now,
                streamed = isFinal,
                groundingJson = groundingMetadata?.let {
                    com.example.innovexia.data.models.GroundingMetadataSerializer.toJson(it)
                },
                groundingStatus = groundingStatus?.name ?: "NONE"
            )
            messageDao.insert(message)
            chatDao.updateTimestamp(chatId, now)
            return newId
        } else {
            // Update existing message - query directly by ID for efficiency
            val existing = messageDao.getById(messageId)
            if (existing != null && existing.chatId == chatId) {
                // Update grounding status immediately when provided (even during streaming)
                // This ensures the UI can switch to GroundingSearchBubble as soon as we know it's a grounded response
                val newGroundingStatus = if (groundingStatus != null) {
                    android.util.Log.d("ChatRepository", "Updating grounding status for message $messageId: ${groundingStatus.name}")
                    groundingStatus.name
                } else {
                    existing.groundingStatus
                }

                val updated = existing.copy(
                    text = existing.text + token,
                    streamed = isFinal,
                    // Only update grounding if provided and final
                    groundingJson = if (isFinal && groundingMetadata != null) {
                        val json = com.example.innovexia.data.models.GroundingMetadataSerializer.toJson(groundingMetadata)
                        android.util.Log.d("ChatRepository", "Writing grounding JSON to DB for message $messageId (length: ${json.length} chars)")
                        json
                    } else {
                        existing.groundingJson
                    },
                    // Update grounding status immediately when provided (not just on final)
                    groundingStatus = newGroundingStatus
                )
                messageDao.update(updated)
                chatDao.updateTimestamp(chatId, now)

                // Log what was saved
                if (isFinal && groundingMetadata != null) {
                    android.util.Log.d("ChatRepository", "Successfully wrote grounding metadata to DB for message $messageId")
                }
            } else {
                android.util.Log.w("ChatRepository", "Message $messageId not found in chat $chatId - cannot update grounding data")
            }
            return messageId
        }
    }

    /**
     * Update chat title if it's still the default generated one.
     * For auto-naming: accepts pre-formatted title (already limited to 10 chars).
     */
    suspend fun updateTitleIfNeeded(chatId: String, suggestion: String) {
        val chat = chatDao.getById(chatId)
        if (chat != null && chat.title.length <= 32) {
            // Only update if current title is auto-generated (short)
            chatDao.updateTitle(chatId, suggestion)
        }
    }

    /**
     * Update chat title if user did not manually rename.
     * Specifically for auto-naming after first AI response.
     */
    suspend fun updateTitleIfUserDidNotRename(chatId: String, title: String) {
        updateTitleIfNeeded(chatId, title)
    }

    /**
     * Generate and update chat title using AI based on conversation context.
     * Should be called periodically as conversation evolves.
     *
     * @param chatId The chat ID
     * @return true if title was updated, false otherwise
     */
    suspend fun generateAndUpdateAITitle(chatId: String): Boolean {
        return try {
            val chat = chatDao.getById(chatId) ?: return false

            // Don't update if user manually renamed (title > 32 chars or doesn't look auto-generated)
            if (chat.title.length > 32) {
                return false
            }

            // Get messages for context
            val messages = messageDao.forChatSync(chatId)

            // Only generate title if we have enough messages
            if (messages.size < 2) {
                return false
            }

            // Check if should update based on message count
            if (!shouldUpdateChatTitle(messages.size)) {
                return false
            }

            // Generate AI title
            val result = chatNamingService.generateChatTitle(messages)
            result.getOrNull()?.let { aiTitle ->
                chatDao.updateTitle(chatId, aiTitle)
                true
            } ?: false

        } catch (e: Exception) {
            // Silent fail - don't crash if title generation fails
            false
        }
    }

    /**
     * Check if a chat should have its title updated based on message count.
     */
    suspend fun shouldUpdateTitle(chatId: String): Boolean {
        val messages = messageDao.forChatSync(chatId)
        return shouldUpdateChatTitle(messages.size)
    }

    /**
     * Delete a chat and all its messages.
     */
    suspend fun deleteChat(chatId: String) {
        val chat = chatDao.getById(chatId)
        if (chat != null) {
            chatDao.delete(chat)
        }
    }

    /**
     * Delete all chats and messages.
     */
    suspend fun deleteAllHistory() {
        messageDao.deleteAll()
        chatDao.deleteAll()
    }

    /**
     * Toggle pin status for a chat.
     */
    suspend fun togglePin(chatId: String) {
        chatDao.togglePin(chatId)
    }

    /**
     * Archive a chat (move to archived section).
     */
    suspend fun archiveChat(chatId: String) {
        chatDao.archive(chatId)
    }

    /**
     * Restore a chat from archive (move back to recent).
     */
    suspend fun restoreFromArchive(chatId: String) {
        chatDao.restoreFromArchive(chatId)
    }

    /**
     * Move a chat to trash (soft delete locally).
     * Also soft deletes from Firebase if cloud delete is enabled.
     */
    suspend fun moveToTrash(chatId: String) {
        // Soft delete locally
        chatDao.moveToTrash(chatId)

        // Also soft delete from Firebase if enabled
        if (context != null && cloudSyncRepo != null && userPreferences != null) {
            try {
                val cloudDeleteEnabled = userPreferences.cloudDeleteEnabled.first()

                if (cloudDeleteEnabled) {
                    cloudSyncRepo.softDeleteChatFromCloud(chatId)
                    android.util.Log.d("ChatRepository", "Soft deleted chat $chatId from cloud")
                }
            } catch (e: Exception) {
                // Log but don't throw - local delete succeeded
                android.util.Log.e("ChatRepository", "Failed to soft delete chat $chatId from cloud", e)
            }
        }
    }

    /**
     * Restore a chat from trash.
     */
    suspend fun restoreFromTrash(chatId: String) {
        chatDao.restoreFromTrash(chatId)
    }

    /**
     * Permanently delete all chats in trash.
     * Also permanently deletes from Firebase if cloud delete is enabled.
     */
    suspend fun emptyTrash() {
        val ownerId = profileRepo?.getCurrentProfile()?.toOwnerId() ?: ProfileId.GUEST_OWNER_ID

        // Get all chats in trash before deleting
        val allChats = chatDao.getAllFor(ownerId)
        val chatIds = allChats.filter { it.deletedLocally }.map { it.id }

        // Delete locally
        chatDao.emptyTrash(ownerId)

        // Also delete from Firebase if enabled
        if (context != null && cloudSyncRepo != null && userPreferences != null && chatIds.isNotEmpty()) {
            try {
                val cloudDeleteEnabled = userPreferences.cloudDeleteEnabled.first()

                if (cloudDeleteEnabled) {
                    cloudSyncRepo.batchDeleteChatsFromCloud(chatIds)
                    android.util.Log.d("ChatRepository", "Permanently deleted ${chatIds.size} chats from cloud")
                }
            } catch (e: Exception) {
                // Log but don't throw - local delete succeeded
                android.util.Log.e("ChatRepository", "Failed to delete chats from cloud", e)
            }
        }
    }

    /**
     * Permanently delete a specific chat from trash (delete forever).
     * Also permanently deletes from Firebase if cloud delete is enabled.
     */
    suspend fun deleteForever(chatId: String) {
        // Delete locally
        chatDao.deleteForever(chatId)

        // Also delete from Firebase if enabled
        if (context != null && cloudSyncRepo != null && userPreferences != null) {
            try {
                val cloudDeleteEnabled = userPreferences.cloudDeleteEnabled.first()

                if (cloudDeleteEnabled) {
                    cloudSyncRepo.permanentlyDeleteChatFromCloud(chatId)
                    android.util.Log.d("ChatRepository", "Permanently deleted chat $chatId from cloud")
                }
            } catch (e: Exception) {
                // Log but don't throw - local delete succeeded
                android.util.Log.e("ChatRepository", "Failed to permanently delete chat $chatId from cloud", e)
            }
        }
    }

    /**
     * Generate a simple title from the first user message.
     * Takes first 3-5 words, capitalizes, max 32 chars.
     */
    private fun generateTitle(firstMessage: String): String {
        val words = firstMessage.trim().split(Regex("\\s+")).take(5)
        val title = words.joinToString(" ")
        return if (title.length > 32) {
            title.take(29) + "..."
        } else {
            title.replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Insert a message directly (used for edit-resend)
     */
    suspend fun insertMessage(message: MessageEntity) {
        messageDao.insert(message)
        chatDao.updateTimestamp(message.chatId, System.currentTimeMillis())
    }

    /**
     * Update message status
     */
    suspend fun updateMessageStatus(messageId: String, status: MsgStatus) {
        val messages = messageDao.forChatSync("")
        val message = messages.find { it.id == messageId }
        if (message != null) {
            val updated = message.withStatus(status)
            messageDao.update(updated)
        }
    }

    /**
     * Soft delete a message
     */
    suspend fun softDeleteMessage(messageId: String) {
        val messages = messageDao.forChatSync("")
        val message = messages.find { it.id == messageId }
        if (message != null) {
            val updated = message.copy(deletedAt = System.currentTimeMillis())
            messageDao.update(updated)
        }
    }

    /**
     * Toggle incognito mode for a chat
     */
    suspend fun toggleIncognito(chatId: String, enabled: Boolean) {
        chatDao.setIncognito(chatId, enabled, System.currentTimeMillis())
    }

    /**
     * Update message stream state (for regeneration)
     */
    suspend fun updateMessageStreamState(messageId: String, state: String, now: Long) {
        messageDao.updateStreamState(messageId, state, now)
    }

    /**
     * Overwrite message text (for streaming regeneration)
     */
    suspend fun overwriteMessageText(messageId: String, text: String, now: Long) {
        messageDao.overwriteText(messageId, text, now)
    }

    /**
     * Increment regeneration count
     */
    suspend fun bumpRegenCount(messageId: String, now: Long) {
        messageDao.bumpRegen(messageId, now)
    }

    /**
     * Mark message with error
     */
    suspend fun markMessageError(messageId: String, error: String?, state: String, now: Long) {
        messageDao.markError(messageId, error, state, now)
    }

    /**
     * Move incognito chat to cloud
     */
    suspend fun moveChatToCloud(chatId: String): Result<Unit> = runCatching {
        val chat = chatDao.getById(chatId) ?: throw IllegalArgumentException("Chat not found")

        if (!chat.isIncognito) {
            throw IllegalStateException("Chat is not in incognito mode")
        }

        // Get all messages for this chat
        val messages = messageDao.forChatSync(chatId)

        // For now, just disable incognito mode
        // TODO: Implement actual cloud upload when CloudSyncRepository has the methods
        val updatedChat = chat.copy(
            isIncognito = false,
            updatedAt = System.currentTimeMillis()
        )
        chatDao.update(updatedChat)

        // Mark all messages as no longer local-only
        messages.forEach { msg ->
            messageDao.update(msg.copy(localOnly = false))
        }
    }
}
