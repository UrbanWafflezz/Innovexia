package com.example.innovexia.data.repository

import androidx.room.withTransaction
import com.example.innovexia.core.auth.ProfileId
import com.example.innovexia.data.local.AppDatabase
import com.example.innovexia.data.local.dao.ChatDao
import com.example.innovexia.data.local.dao.MessageDao
import com.example.innovexia.data.local.entities.ChatEntity
import com.example.innovexia.data.local.entities.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Profile-scoped repository that automatically switches between Guest and User datasets
 * based on Firebase authentication state.
 *
 * All queries are filtered by the current profile's ownerId.
 */
class ProfileScopedRepository(
    private val database: AppDatabase,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) {
    private val _profile = MutableStateFlow(ProfileId.current())
    val profile = _profile.asStateFlow()

    /**
     * Call this when Firebase auth state changes.
     * Automatically switches the active profile.
     */
    fun onAuthChanged() {
        _profile.value = ProfileId.current()
    }

    /**
     * Observe all chats for the current profile.
     * Automatically updates when auth state changes.
     */
    fun chats(): Flow<List<ChatEntity>> =
        profile.flatMapLatest { profileId ->
            chatDao.observeChats(profileId.toOwnerId())
        }

    /**
     * Observe messages for a specific chat.
     */
    fun messagesForChat(chatId: String): Flow<List<MessageEntity>> =
        messageDao.forChat(chatId)

    /**
     * Create a new chat with the current profile's ownerId.
     */
    suspend fun createChat(chat: ChatEntity): ChatEntity {
        val ownerId = profile.value.toOwnerId()
        val scopedChat = chat.copy(ownerId = ownerId)
        chatDao.insert(scopedChat)
        return scopedChat
    }

    /**
     * Create a new message with the current profile's ownerId.
     */
    suspend fun createMessage(message: MessageEntity): MessageEntity {
        val ownerId = profile.value.toOwnerId()
        val scopedMessage = message.copy(ownerId = ownerId)
        messageDao.insert(scopedMessage)
        return scopedMessage
    }

    /**
     * Delete a chat and its messages.
     */
    suspend fun deleteChat(chatId: String) {
        val chat = chatDao.getById(chatId)
        if (chat != null) {
            chatDao.delete(chat)
        }
    }

    /**
     * Delete all chats and messages for the current profile.
     */
    suspend fun deleteAllForCurrentProfile() {
        val ownerId = profile.value.toOwnerId()
        chatDao.deleteAllFor(ownerId)
        messageDao.deleteAllFor(ownerId)
    }

    /**
     * Check if there are any guest chats available for import.
     * Only relevant when user is signed in.
     */
    suspend fun hasGuestChats(): Boolean {
        return chatDao.countGuestChats() > 0
    }

    /**
     * Merge all guest chats into the current user's account.
     * This is a one-time operation typically performed after first sign-in.
     *
     * @return Number of chats merged
     */
    suspend fun mergeGuestChatsIntoCurrentUser(): Int {
        val currentProfile = profile.value
        if (currentProfile !is ProfileId.User) {
            throw IllegalStateException("Cannot merge guest chats: user not signed in")
        }

        val uid = currentProfile.uid
        val guestOwnerId = ProfileId.GUEST_OWNER_ID

        return database.withTransaction {
            val guestChats = chatDao.getAllFor(guestOwnerId)

            // Reassign each chat and its messages
            guestChats.forEach { chat ->
                // Update chat ownerId
                chatDao.insert(chat.copy(ownerId = uid))

                // Update all messages for this chat
                messageDao.reassignOwner(
                    chatId = chat.id,
                    from = guestOwnerId,
                    to = uid
                )
            }

            guestChats.size
        }
    }

    /**
     * Get the current profile ID.
     */
    fun getCurrentProfile(): ProfileId = profile.value

    /**
     * Check if currently in guest mode.
     */
    fun isGuestMode(): Boolean = profile.value is ProfileId.Guest
}
