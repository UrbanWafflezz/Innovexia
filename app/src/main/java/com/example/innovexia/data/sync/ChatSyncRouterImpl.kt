package com.example.innovexia.data.sync

import com.example.innovexia.data.local.dao.ChatDao
import com.example.innovexia.data.local.dao.MessageDao
import com.example.innovexia.data.local.entities.ChatEntity
import com.example.innovexia.data.local.entities.MessageEntity
import com.example.innovexia.data.repository.CloudSyncRepository

/**
 * Implementation of ChatSyncRouter that routes messages to local or cloud storage
 * based on chat.isIncognito flag.
 */
class ChatSyncRouterImpl(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val cloudSyncRepo: CloudSyncRepository?
) : ChatSyncRouter {

    override suspend fun saveMessage(chat: ChatEntity, msg: MessageEntity) {
        if (chat.isIncognito) {
            // Incognito mode: save locally only, do not sync to cloud
            messageDao.insert(msg.copy(localOnly = true))
            chatDao.updateTimestamp(chat.id, System.currentTimeMillis())
            return
        }

        // Normal mode: save locally and enqueue for cloud sync
        messageDao.insert(msg.copy(localOnly = false))
        chatDao.updateTimestamp(chat.id, System.currentTimeMillis())

        // Sync to cloud if available (using existing sync methods)
        cloudSyncRepo?.syncChat(chat.id)
        cloudSyncRepo?.syncMessage(chat.id, msg.id)
    }

    override suspend fun moveChatToCloud(chatId: String): Result<Unit> = runCatching {
        val chat = chatDao.getById(chatId) ?: throw IllegalArgumentException("Chat not found")

        if (!chat.isIncognito) {
            throw IllegalStateException("Chat is not in incognito mode")
        }

        // Get all messages for this chat
        val messages = messageDao.forChatSync(chatId)

        // Update chat: disable incognito first
        val updatedChat = chat.copy(
            isIncognito = false,
            updatedAt = System.currentTimeMillis()
        )
        chatDao.update(updatedChat)

        // Mark all messages as no longer local-only
        messages.forEach { msg ->
            messageDao.update(msg.copy(localOnly = false))
        }

        // Now sync to cloud using existing methods
        cloudSyncRepo?.syncChat(chatId)
        messages.forEach { msg ->
            cloudSyncRepo?.syncMessage(chatId, msg.id)
        }
    }
}
