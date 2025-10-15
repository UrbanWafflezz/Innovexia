package com.example.innovexia.core.sync

import com.example.innovexia.data.local.entities.ChatEntity
import com.example.innovexia.data.local.entities.MessageEntity
import com.example.innovexia.data.models.AttachmentMeta
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Cloud sync engine for syncing chats and messages to Firebase.
 *
 * Architecture:
 * - Firestore: Structure and metadata (compact docs ≤ 1 MB)
 * - Storage: Large content (message chunks, attachments, summaries)
 *
 * Message chunking:
 * - Head text (≤ 12 KB) stored in Firestore textHead
 * - Overflow stored in Storage at users/{uid}/msgchunks/{chatId}/{messageId}/{seq}.txt
 */
class CloudSyncEngine {

    private val firestore = Firebase.firestore
    private val storage = Firebase.storage

    companion object {
        private const val HEAD_BYTES_LIMIT = 12_000
        private const val CHUNK_SIZE = 12_000
    }

    /**
     * Upload or update a chat document in Firestore.
     */
    suspend fun upsertChat(uid: String, chat: ChatEntity) {
        val chatRef = firestore.collection("users").document(uid)
            .collection("chats").document(chat.id)

        val data = mutableMapOf(
            "title" to chat.title,
            "createdAt" to Timestamp(Date(chat.createdAt)),
            "updatedAt" to Timestamp(Date(chat.updatedAt)),
            "lastMsgAt" to if (chat.lastMsgAt > 0) Timestamp(Date(chat.lastMsgAt)) else FieldValue.serverTimestamp(),
            "msgCount" to chat.msgCount,
            "persona" to chat.personaName,
            "memoryEnabled" to chat.memoryEnabled,
            "summaryHead" to chat.summaryHead,
            "summaryHasChunks" to chat.summaryHasChunks
        )

        // Add soft-delete field if present
        if (chat.deletedAt != null) {
            data["deletedAt"] = Timestamp(Date(chat.deletedAt))
        }

        chatRef.set(data, SetOptions.merge()).await()
    }

    /**
     * Upload or update a message with automatic chunking.
     *
     * Strategy:
     * - Store first ~12 KB in Firestore textHead
     * - Store overflow in Storage as chunks
     * - Update parent chat counters
     */
    suspend fun upsertMessage(uid: String, chatId: String, message: MessageEntity) {
        val text = message.text
        val bytes = text.toByteArray(Charsets.UTF_8)

        // Split text into head and tail
        val (head, tail) = if (bytes.size <= HEAD_BYTES_LIMIT) {
            text to null
        } else {
            val headBytes = bytes.copyOfRange(0, HEAD_BYTES_LIMIT)
            val tailBytes = bytes.copyOfRange(HEAD_BYTES_LIMIT, bytes.size)
            String(headBytes, Charsets.UTF_8) to String(tailBytes, Charsets.UTF_8)
        }

        // Prepare message document
        val messageRef = firestore.collection("users").document(uid)
            .collection("chats").document(chatId)
            .collection("messages").document(message.id)

        val data = mutableMapOf(
            "role" to message.role,
            "createdAt" to Timestamp(Date(message.createdAt)),
            "updatedAt" to Timestamp(Date(message.updatedAt)),
            "textHead" to head,
            "hasChunks" to (tail != null),
            "attachments" to message.attachments().map { it.toMap() }
        )

        message.replyToId?.let { data["replyToId"] = it }

        // Add soft-delete field if present
        if (message.deletedAt != null) {
            data["deletedAt"] = Timestamp(Date(message.deletedAt))
        }

        messageRef.set(data, SetOptions.merge()).await()

        // Upload tail chunks to Storage
        if (tail != null) {
            uploadMessageChunks(uid, chatId, message.id, tail)
        }

        // Update chat counters
        updateChatCounters(uid, chatId)
    }

    /**
     * Upload message text chunks to Storage.
     */
    private suspend fun uploadMessageChunks(
        uid: String,
        chatId: String,
        messageId: String,
        text: String
    ) {
        val chunks = text.chunked(CHUNK_SIZE)
        chunks.forEachIndexed { index, chunk ->
            val ref = storage.reference
                .child("users/$uid/msgchunks/$chatId/$messageId/$index.txt")
            ref.putBytes(chunk.toByteArray(Charsets.UTF_8)).await()
        }
    }

    /**
     * Download and reassemble message text from Firestore + Storage.
     */
    suspend fun loadMessageText(
        uid: String,
        chatId: String,
        messageId: String,
        textHead: String?,
        hasChunks: Boolean
    ): String {
        val head = textHead ?: ""
        if (!hasChunks) return head

        // Load chunks from Storage
        val baseRef = storage.reference.child("users/$uid/msgchunks/$chatId/$messageId")
        val items = baseRef.listAll().await().items.sortedBy {
            it.name.substringBefore('.').toIntOrNull() ?: 0
        }

        val tail = buildString {
            for (obj in items) {
                val chunkBytes = obj.getBytes(Long.MAX_VALUE).await()
                append(String(chunkBytes, Charsets.UTF_8))
            }
        }

        return head + tail
    }

    /**
     * Upload an attachment to Storage and return metadata.
     */
    suspend fun uploadAttachment(
        uid: String,
        chatId: String,
        messageId: String,
        filename: String,
        data: ByteArray,
        mimeType: String
    ): AttachmentMeta {
        val ref = storage.reference
            .child("users/$uid/attachments/$chatId/$messageId/$filename")

        ref.putBytes(data).await()

        return AttachmentMeta(
            name = filename,
            mime = mimeType,
            bytes = data.size.toLong(),
            storagePath = ref.path
        )
    }

    /**
     * Download attachment bytes from Storage.
     */
    suspend fun downloadAttachment(storagePath: String): ByteArray {
        val ref = storage.getReference(storagePath)
        return ref.getBytes(Long.MAX_VALUE).await()
    }

    /**
     * Update chat counters (msgCount, lastMsgAt, updatedAt).
     */
    private suspend fun updateChatCounters(uid: String, chatId: String) {
        val chatRef = firestore.collection("users").document(uid)
            .collection("chats").document(chatId)

        chatRef.set(
            mapOf(
                "updatedAt" to FieldValue.serverTimestamp(),
                "lastMsgAt" to FieldValue.serverTimestamp(),
                "msgCount" to FieldValue.increment(1)
            ),
            SetOptions.merge()
        ).await()
    }

    /**
     * Delete all cloud data for a user (chats, messages, storage).
     * Implements paginated deletion to avoid overwhelming Firestore.
     */
    suspend fun deleteAllUserData(uid: String) {
        // Delete Firestore data in batches
        val userRef = firestore.collection("users").document(uid)

        // Delete all chats (and cascading messages via Firestore rules or manual deletion)
        val chats = userRef.collection("chats").get().await()
        chats.documents.chunked(500).forEach { batch ->
            val writeBatch = firestore.batch()
            batch.forEach { writeBatch.delete(it.reference) }
            writeBatch.commit().await()
        }

        // Delete user document
        userRef.delete().await()

        // Delete Storage data
        val storageRef = storage.reference.child("users/$uid")
        deleteStorageRecursive(storageRef.path)
    }

    /**
     * Recursively delete all objects under a Storage path.
     */
    private suspend fun deleteStorageRecursive(path: String) {
        val ref = storage.getReference(path)
        try {
            val result = ref.listAll().await()

            // Delete all files
            result.items.forEach { it.delete().await() }

            // Recursively delete all subdirectories
            result.prefixes.forEach { deleteStorageRecursive(it.path) }
        } catch (e: Exception) {
            // Silent fail - path might not exist
        }
    }

    /**
     * Fetch all chat IDs for a user from Firestore.
     */
    suspend fun fetchChatIds(uid: String): List<String> {
        val snapshot = firestore.collection("users").document(uid)
            .collection("chats")
            .get()
            .await()

        return snapshot.documents.map { it.id }
    }

    /**
     * Fetch all message IDs for a chat from Firestore.
     */
    suspend fun fetchMessageIds(uid: String, chatId: String): List<String> {
        val snapshot = firestore.collection("users").document(uid)
            .collection("chats").document(chatId)
            .collection("messages")
            .get()
            .await()

        return snapshot.documents.map { it.id }
    }

    /**
     * Fetch cloud chat items with optional deleted filter.
     */
    suspend fun fetchCloudChats(uid: String, includeDeleted: Boolean = true): List<CloudChatItem> {
        val query = firestore.collection("users").document(uid)
            .collection("chats")
            .orderBy("lastMsgAt", com.google.firebase.firestore.Query.Direction.DESCENDING)

        val snapshot = query.get().await()

        return snapshot.documents.mapNotNull { doc ->
            val deletedAt = doc.getTimestamp("deletedAt")?.toDate()?.time

            // Filter based on includeDeleted flag
            if (!includeDeleted && deletedAt != null) {
                return@mapNotNull null
            }

            CloudChatItem(
                id = doc.id,
                title = doc.getString("title") ?: "Untitled",
                lastMsgAt = doc.getTimestamp("lastMsgAt")?.toDate()?.time ?: 0L,
                msgCount = doc.getLong("msgCount")?.toInt() ?: 0,
                updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L,
                deletedAt = deletedAt
            )
        }
    }

    /**
     * Fetch a single chat document from cloud.
     */
    suspend fun fetchChat(uid: String, chatId: String): CloudChatDoc? {
        val doc = firestore.collection("users").document(uid)
            .collection("chats").document(chatId)
            .get()
            .await()

        if (!doc.exists()) return null

        return CloudChatDoc(
            id = doc.id,
            title = doc.getString("title") ?: "Untitled",
            createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
            updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L,
            lastMsgAt = doc.getTimestamp("lastMsgAt")?.toDate()?.time ?: 0L,
            msgCount = doc.getLong("msgCount")?.toInt() ?: 0,
            persona = doc.getString("persona"),
            memoryEnabled = doc.getBoolean("memoryEnabled") ?: true,
            summaryHead = doc.getString("summaryHead"),
            summaryHasChunks = doc.getBoolean("summaryHasChunks") ?: false,
            deletedAt = doc.getTimestamp("deletedAt")?.toDate()?.time
        )
    }

    /**
     * Fetch messages for a chat with pagination.
     */
    suspend fun fetchMessages(
        uid: String,
        chatId: String,
        limit: Int = 100,
        startAfterDoc: com.google.firebase.firestore.DocumentSnapshot? = null
    ): MessagePage {
        var query = firestore.collection("users").document(uid)
            .collection("chats").document(chatId)
            .collection("messages")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .limit(limit.toLong())

        if (startAfterDoc != null) {
            query = query.startAfter(startAfterDoc)
        }

        val snapshot = query.get().await()

        val messages = snapshot.documents.mapNotNull { doc ->
            CloudMessageDoc(
                id = doc.id,
                role = doc.getString("role") ?: "user",
                textHead = doc.getString("textHead") ?: "",
                hasChunks = doc.getBoolean("hasChunks") ?: false,
                createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L,
                attachments = parseAttachments(doc.get("attachments")),
                replyToId = doc.getString("replyToId"),
                deletedAt = doc.getTimestamp("deletedAt")?.toDate()?.time
            )
        }

        return MessagePage(
            messages = messages,
            lastDoc = snapshot.documents.lastOrNull()
        )
    }

    /**
     * Clear deletedAt flag from a chat (revive in cloud).
     */
    suspend fun reviveChatInCloud(uid: String, chatId: String) {
        val chatRef = firestore.collection("users").document(uid)
            .collection("chats").document(chatId)

        chatRef.update("deletedAt", FieldValue.delete()).await()
    }

    /**
     * Soft delete a chat in cloud (sets deletedAt timestamp).
     * This marks the chat as deleted but keeps it in Firestore for potential recovery.
     */
    suspend fun softDeleteChatInCloud(uid: String, chatId: String) {
        val chatRef = firestore.collection("users").document(uid)
            .collection("chats").document(chatId)

        chatRef.update("deletedAt", FieldValue.serverTimestamp()).await()
    }

    /**
     * Permanently delete a chat from cloud (hard delete).
     * This removes the chat document, all its messages, and associated Storage files.
     */
    suspend fun permanentlyDeleteChat(uid: String, chatId: String) {
        // Delete all messages in the chat
        val messagesRef = firestore.collection("users").document(uid)
            .collection("chats").document(chatId)
            .collection("messages")

        val messages = messagesRef.get().await()
        messages.documents.chunked(500).forEach { batch ->
            val writeBatch = firestore.batch()
            batch.forEach { writeBatch.delete(it.reference) }
            writeBatch.commit().await()
        }

        // Delete chat document
        val chatRef = firestore.collection("users").document(uid)
            .collection("chats").document(chatId)
        chatRef.delete().await()

        // Delete Storage files for this chat
        deleteStorageForChat(uid, chatId)
    }

    /**
     * Delete all Storage files associated with a chat.
     * This includes message chunks, attachments, and summaries.
     */
    private suspend fun deleteStorageForChat(uid: String, chatId: String) {
        try {
            // Delete message chunks
            val chunksPath = "users/$uid/msgchunks/$chatId"
            deleteStorageRecursive(chunksPath)

            // Delete attachments
            val attachmentsPath = "users/$uid/attachments/$chatId"
            deleteStorageRecursive(attachmentsPath)

            // Delete summaries if they exist
            val summaryPath = "users/$uid/summaries/$chatId.txt"
            try {
                storage.getReference(summaryPath).delete().await()
            } catch (e: Exception) {
                // File might not exist, ignore
            }
        } catch (e: Exception) {
            // Log but don't throw - Storage deletion is best-effort
            android.util.Log.e("CloudSyncEngine", "Error deleting storage for chat $chatId", e)
        }
    }

    /**
     * Batch delete multiple chats from cloud (hard delete).
     * Used when emptying trash.
     */
    suspend fun batchDeleteChats(uid: String, chatIds: List<String>) {
        chatIds.forEach { chatId ->
            try {
                permanentlyDeleteChat(uid, chatId)
            } catch (e: Exception) {
                // Log error but continue with other chats
                android.util.Log.e("CloudSyncEngine", "Failed to delete chat $chatId", e)
            }
        }
    }

    private fun parseAttachments(data: Any?): List<AttachmentMeta> {
        if (data !is List<*>) return emptyList()
        return data.mapNotNull { item ->
            if (item is Map<*, *>) {
                AttachmentMeta(
                    name = item["name"] as? String ?: return@mapNotNull null,
                    mime = item["mime"] as? String ?: "",
                    bytes = (item["bytes"] as? Number)?.toLong() ?: 0L,
                    storagePath = item["storagePath"] as? String ?: ""
                )
            } else null
        }
    }
}

/**
 * Cloud chat item for listing.
 */
data class CloudChatItem(
    val id: String,
    val title: String,
    val lastMsgAt: Long,
    val msgCount: Int,
    val updatedAt: Long,
    val deletedAt: Long?
)

/**
 * Full cloud chat document.
 */
data class CloudChatDoc(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastMsgAt: Long,
    val msgCount: Int,
    val persona: String?,
    val memoryEnabled: Boolean,
    val summaryHead: String?,
    val summaryHasChunks: Boolean,
    val deletedAt: Long?
)

/**
 * Cloud message document.
 */
data class CloudMessageDoc(
    val id: String,
    val role: String,
    val textHead: String,
    val hasChunks: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val attachments: List<AttachmentMeta>,
    val replyToId: String?,
    val deletedAt: Long?
)

/**
 * Paginated message result.
 */
data class MessagePage(
    val messages: List<CloudMessageDoc>,
    val lastDoc: com.google.firebase.firestore.DocumentSnapshot?
)

/**
 * Extension to convert AttachmentMeta to Firestore map.
 */
private fun AttachmentMeta.toMap(): Map<String, Any?> {
    return mapOf(
        "name" to name,
        "mime" to mime,
        "bytes" to bytes,
        "storagePath" to storagePath
    )
}
