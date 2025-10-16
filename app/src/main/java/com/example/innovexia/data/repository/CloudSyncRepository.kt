package com.example.innovexia.data.repository

import android.content.Context
import android.util.Log
import com.example.innovexia.core.auth.FirebaseAuthManager
import com.example.innovexia.core.sync.CloudSyncEngine
import com.example.innovexia.core.sync.CloudSyncSettings
import com.example.innovexia.data.local.dao.ChatDao
import com.example.innovexia.data.local.dao.MessageDao
import kotlinx.coroutines.flow.first

/**
 * Repository layer for cloud sync operations.
 *
 * Orchestrates bidirectional sync between local Room DB and Firebase.
 * Only syncs when:
 * - User is signed in (not guest)
 * - Cloud sync is enabled in settings
 */
class CloudSyncRepository(
    private val context: Context,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) {
    private val engine = CloudSyncEngine()
    private val settings = CloudSyncSettings(context)

    companion object {
        private const val TAG = "CloudSyncRepository"
    }

    /**
     * Check if cloud sync is currently enabled and user is signed in.
     */
    suspend fun isSyncEnabled(): Boolean {
        val signedIn = FirebaseAuthManager.currentUser() != null
        val enabled = settings.cloudSyncEnabled.first()
        return signedIn && enabled
    }

    /**
     * Enable cloud sync and trigger initial upload.
     * Returns true if successfully enabled, false if user not signed in.
     */
    suspend fun enableSync(): Boolean {
        val uid = FirebaseAuthManager.currentUser()?.uid
        if (uid == null) {
            Log.w(TAG, "Cannot enable sync: user not signed in")
            return false
        }

        settings.set(true)
        Log.d(TAG, "Cloud sync enabled for user $uid")
        return true
    }

    /**
     * Disable cloud sync (does not delete cloud data).
     */
    suspend fun disableSync() {
        settings.set(false)
        Log.d(TAG, "Cloud sync disabled")
    }

    /**
     * Sync a single chat to cloud.
     * Only syncs if sync is enabled.
     * Skips incognito chats (local-only).
     */
    suspend fun syncChat(chatId: String) {
        if (!isSyncEnabled()) return

        val uid = FirebaseAuthManager.currentUser()?.uid ?: return
        val chat = chatDao.getById(chatId) ?: return

        // Skip incognito chats (local-only)
        if (chat.isIncognito) {
            Log.d(TAG, "Skipping incognito chat $chatId (local-only)")
            return
        }

        try {
            engine.upsertChat(uid, chat)
            Log.d(TAG, "Synced chat $chatId to cloud")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync chat $chatId", e)
        }
    }

    /**
     * Sync a single message to cloud.
     * Only syncs if sync is enabled.
     * Skips messages from incognito chats (local-only).
     */
    suspend fun syncMessage(chatId: String, messageId: String) {
        if (!isSyncEnabled()) return

        val uid = FirebaseAuthManager.currentUser()?.uid ?: return
        val chat = chatDao.getById(chatId) ?: return

        // Skip incognito chats (local-only)
        if (chat.isIncognito) {
            Log.d(TAG, "Skipping message $messageId from incognito chat $chatId (local-only)")
            return
        }

        val message = messageDao.forChatSync(chatId).find { it.id == messageId } ?: return

        try {
            engine.upsertMessage(uid, chatId, message)
            Log.d(TAG, "Synced message $messageId to cloud")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync message $messageId", e)
        }
    }

    /**
     * Perform full upload of all local chats and messages to cloud.
     * Typically called when sync is first enabled.
     * Uses batching to avoid overwhelming Firestore.
     * Skips incognito chats (local-only).
     */
    suspend fun performInitialUpload(onProgress: (Int, Int) -> Unit = { _, _ -> }) {
        if (!isSyncEnabled()) return

        val uid = FirebaseAuthManager.currentUser()?.uid ?: return

        try {
            // Get all chats for current user (get all and filter by owner)
            val allChats = chatDao.getAllSync()
            // Filter out incognito chats (local-only)
            val chats = allChats.filter { it.ownerId == uid && !it.isIncognito }
            Log.d(TAG, "Starting initial upload: ${chats.size} chats (excluding incognito)")

            chats.forEachIndexed { index, chat ->
                // Upload chat
                engine.upsertChat(uid, chat)

                // Upload all messages in chat
                val messages = messageDao.forChatSync(chat.id)
                messages.forEach { message ->
                    engine.upsertMessage(uid, chat.id, message)
                }

                onProgress(index + 1, chats.size)
                Log.d(TAG, "Uploaded chat ${chat.id} with ${messages.size} messages")
            }

            // Update sync statistics
            val totalMessages = chats.sumOf { chat ->
                messageDao.forChatSync(chat.id).size
            }
            settings.updateLastSync(chats.size, totalMessages)

            Log.d(TAG, "Initial upload complete")
        } catch (e: Exception) {
            Log.e(TAG, "Initial upload failed", e)
            throw e
        }
    }

    /**
     * Delete all cloud data for the current user.
     * Does not affect local data.
     */
    suspend fun deleteCloudData() {
        val uid = FirebaseAuthManager.currentUser()?.uid ?: return

        try {
            engine.deleteAllUserData(uid)
            Log.d(TAG, "Deleted all cloud data for user $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete cloud data", e)
            throw e
        }
    }

    /**
     * Download and restore all chats from cloud to local DB.
     * WARNING: This will overwrite local data.
     */
    suspend fun downloadAllFromCloud(onProgress: (Int, Int) -> Unit = { _, _ -> }) {
        val uid = FirebaseAuthManager.currentUser()?.uid ?: return

        try {
            val chatIds = engine.fetchChatIds(uid)
            Log.d(TAG, "Downloading ${chatIds.size} chats from cloud")

            chatIds.forEachIndexed { index, chatId ->
                // TODO: Implement chat download and restoration
                // This would fetch Firestore documents and reassemble messages
                onProgress(index + 1, chatIds.size)
            }

            Log.d(TAG, "Cloud download complete")
        } catch (e: Exception) {
            Log.e(TAG, "Cloud download failed", e)
            throw e
        }
    }

    /**
     * List all cloud chats for restore UI.
     */
    suspend fun listCloudChats(includeDeleted: Boolean = true): List<com.example.innovexia.core.sync.CloudChatItem> {
        val uid = FirebaseAuthManager.currentUser()?.uid
            ?: throw IllegalStateException("User not signed in")

        return engine.fetchCloudChats(uid, includeDeleted)
    }

    /**
     * Restore selected chats from cloud to local DB.
     */
    suspend fun restoreChatsFromCloud(
        chatIds: List<String>,
        includeDeletedMessages: Boolean = false,
        forceOverwrite: Boolean = false,
        reviveInCloud: Boolean = false,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): RestoreResult {
        val uid = FirebaseAuthManager.currentUser()?.uid
            ?: throw IllegalStateException("User not signed in")

        val results = mutableListOf<ChatRestoreResult>()

        chatIds.forEachIndexed { index, chatId ->
            try {
                val result = restoreSingleChat(
                    uid = uid,
                    chatId = chatId,
                    includeDeletedMessages = includeDeletedMessages,
                    forceOverwrite = forceOverwrite,
                    reviveInCloud = reviveInCloud
                )
                results.add(result)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore chat $chatId", e)
                results.add(ChatRestoreResult(chatId, ChatRestoreStatus.ERROR, e.message))
            }
            onProgress(index + 1, chatIds.size)
        }

        return RestoreResult(results)
    }

    /**
     * Restore a single chat with all messages.
     */
    private suspend fun restoreSingleChat(
        uid: String,
        chatId: String,
        includeDeletedMessages: Boolean,
        forceOverwrite: Boolean,
        reviveInCloud: Boolean
    ): ChatRestoreResult {
        // Fetch cloud chat
        val cloudChat = engine.fetchChat(uid, chatId)
            ?: return ChatRestoreResult(chatId, ChatRestoreStatus.SKIPPED, "Chat not found in cloud")

        // Check for local conflict
        val localChat = chatDao.getById(chatId)
        if (localChat != null && !forceOverwrite) {
            // Use last-write-wins by updatedAt
            if (localChat.updatedAt >= cloudChat.updatedAt) {
                return ChatRestoreResult(chatId, ChatRestoreStatus.SKIPPED, "Local chat is newer")
            }
        }

        // Create/Update local chat
        // Use current timestamp for updatedAt to ensure it appears at the top of recent chats
        val now = System.currentTimeMillis()
        val restoredChat = com.example.innovexia.data.local.entities.ChatEntity(
            id = cloudChat.id,
            ownerId = uid,
            title = cloudChat.title,
            createdAt = cloudChat.createdAt,
            updatedAt = now, // Use current time so it appears at top
            personaName = cloudChat.persona,
            personaInitial = cloudChat.persona?.firstOrNull()?.toString(),
            personaColor = null,
            memoryEnabled = cloudChat.memoryEnabled,
            summary = "",
            summaryUpdatedAt = 0L,
            lastMsgAt = cloudChat.lastMsgAt,
            msgCount = cloudChat.msgCount,
            summaryHead = cloudChat.summaryHead,
            summaryHasChunks = cloudChat.summaryHasChunks,
            deletedAt = null // Revive locally
        )

        chatDao.insert(restoredChat)

        // Restore messages with pagination
        var messageCount = 0
        var lastDoc: com.google.firebase.firestore.DocumentSnapshot? = null

        do {
            val page = engine.fetchMessages(uid, chatId, limit = 100, startAfterDoc = lastDoc)

            for (cloudMsg in page.messages) {
                // Skip deleted messages if not included
                if (!includeDeletedMessages && cloudMsg.deletedAt != null) {
                    continue
                }

                // Reassemble full text
                val fullText = if (cloudMsg.hasChunks) {
                    engine.loadMessageText(uid, chatId, cloudMsg.id, cloudMsg.textHead, hasChunks = true)
                } else {
                    cloudMsg.textHead
                }

                // Create local message
                val localMessage = com.example.innovexia.data.local.entities.MessageEntity(
                    id = cloudMsg.id,
                    ownerId = uid,
                    chatId = chatId,
                    role = cloudMsg.role,
                    text = fullText,
                    createdAt = cloudMsg.createdAt,
                    streamed = true,
                    updatedAt = cloudMsg.updatedAt,
                    textHead = cloudMsg.textHead,
                    hasChunks = cloudMsg.hasChunks,
                    attachmentsJson = com.example.innovexia.data.models.AttachmentMetaSerializer.toJson(cloudMsg.attachments),
                    replyToId = cloudMsg.replyToId,
                    deletedAt = null // Revive locally
                )

                messageDao.insert(localMessage)
                messageCount++
            }

            lastDoc = page.lastDoc
        } while (lastDoc != null && page.messages.isNotEmpty())

        // Optionally revive in cloud
        if (reviveInCloud && cloudChat.deletedAt != null) {
            engine.reviveChatInCloud(uid, chatId)
        }

        Log.d(TAG, "Restored chat $chatId with $messageCount messages")
        return ChatRestoreResult(chatId, ChatRestoreStatus.RESTORED, "$messageCount messages restored")
    }

    /**
     * Soft delete a chat from cloud (marks as deleted but keeps data).
     */
    suspend fun softDeleteChatFromCloud(chatId: String) {
        val uid = FirebaseAuthManager.currentUser()?.uid ?: return

        try {
            engine.softDeleteChatInCloud(uid, chatId)
            Log.d(TAG, "Soft deleted chat $chatId from cloud")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to soft delete chat $chatId from cloud", e)
            throw e
        }
    }

    /**
     * Permanently delete a chat from cloud (removes all data).
     */
    suspend fun permanentlyDeleteChatFromCloud(chatId: String) {
        val uid = FirebaseAuthManager.currentUser()?.uid ?: return

        try {
            engine.permanentlyDeleteChat(uid, chatId)
            Log.d(TAG, "Permanently deleted chat $chatId from cloud")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to permanently delete chat $chatId from cloud", e)
            throw e
        }
    }

    /**
     * Batch delete multiple chats from cloud (permanent deletion).
     * Used when emptying trash.
     */
    suspend fun batchDeleteChatsFromCloud(chatIds: List<String>) {
        val uid = FirebaseAuthManager.currentUser()?.uid ?: return

        try {
            engine.batchDeleteChats(uid, chatIds)
            Log.d(TAG, "Batch deleted ${chatIds.size} chats from cloud")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to batch delete chats from cloud", e)
            throw e
        }
    }
}

/**
 * Result of restore operation.
 */
data class RestoreResult(
    val chatResults: List<ChatRestoreResult>
) {
    val restoredCount: Int get() = chatResults.count { it.status == ChatRestoreStatus.RESTORED }
    val skippedCount: Int get() = chatResults.count { it.status == ChatRestoreStatus.SKIPPED }
    val errorCount: Int get() = chatResults.count { it.status == ChatRestoreStatus.ERROR }
}

/**
 * Result for a single chat restore.
 */
data class ChatRestoreResult(
    val chatId: String,
    val status: ChatRestoreStatus,
    val message: String? = null
)

enum class ChatRestoreStatus {
    RESTORED,
    SKIPPED,
    ERROR
}
