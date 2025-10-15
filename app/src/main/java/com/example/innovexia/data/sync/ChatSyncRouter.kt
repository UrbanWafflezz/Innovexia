package com.example.innovexia.data.sync

import com.example.innovexia.data.local.entities.ChatEntity
import com.example.innovexia.data.local.entities.MessageEntity

/**
 * Router interface that decides storage/write behavior for chats and messages.
 * Routes to local-only storage for incognito chats or cloud sync for normal chats.
 */
interface ChatSyncRouter {
    /**
     * Save a message, routing to local-only or cloud sync based on chat.isIncognito.
     * @param chat The chat entity this message belongs to
     * @param msg The message to save
     */
    suspend fun saveMessage(chat: ChatEntity, msg: MessageEntity)

    /**
     * Move an incognito chat to cloud by uploading all messages and attachments.
     * @param chatId The chat ID to move to cloud
     * @return Result indicating success or failure
     */
    suspend fun moveChatToCloud(chatId: String): Result<Unit>
}
