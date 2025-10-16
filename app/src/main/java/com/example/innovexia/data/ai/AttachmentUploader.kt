package com.example.innovexia.data.ai

import android.net.Uri
import com.example.innovexia.data.models.AttachmentKind
import com.example.innovexia.data.models.AttachmentMeta
import com.example.innovexia.data.models.AttachmentStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * Handles uploading attachments to Firebase Storage
 * and saving metadata to Firestore
 */
class AttachmentUploader(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    /**
     * Upload an attachment to Firebase Storage and save metadata
     *
     * @param attachment The attachment to upload
     * @param chatId The chat ID this attachment belongs to
     * @return Updated AttachmentMeta with Firebase URL
     */
    suspend fun uploadToFirebase(
        attachment: AttachmentMeta,
        chatId: String
    ): AttachmentMeta {
        val userId = auth.currentUser?.uid
            ?: throw IllegalStateException("User not authenticated")

        // Skip if incognito or local-only
        if (attachment.localOnly) {
            return attachment
        }

        // Get file extension
        val extension = when (attachment.kind) {
            AttachmentKind.PHOTO -> "jpg"
            AttachmentKind.PDF -> "pdf"
            AttachmentKind.FILE -> attachment.displayName.substringAfterLast('.', "bin")
        }

        // Storage path: users/{uid}/chats/{chatId}/{attachmentId}.{ext}
        val storagePath = "users/$userId/chats/$chatId/${attachment.id}.$extension"
        val storageRef = storage.reference.child(storagePath)

        // Get file from URI
        val file = File(attachment.localUri?.path ?: throw IllegalArgumentException("Invalid URI"))
        if (!file.exists()) {
            throw IllegalArgumentException("File does not exist: ${file.absolutePath}")
        }

        // Upload file
        val uploadTask = storageRef.putFile(Uri.fromFile(file))
        uploadTask.await()

        // Get download URL
        val downloadUrl = storageRef.downloadUrl.await().toString()

        // Save metadata to Firestore
        val metadata = mapOf(
            "id" to attachment.id,
            "kind" to attachment.kind.name,
            "mime" to attachment.mime,
            "displayName" to attachment.displayName,
            "sizeBytes" to attachment.sizeBytes,
            "width" to attachment.width,
            "height" to attachment.height,
            "firebaseUrl" to downloadUrl,
            "uploadedAt" to System.currentTimeMillis()
        )

        firestore.collection("users")
            .document(userId)
            .collection("chats")
            .document(chatId)
            .collection("attachments")
            .document(attachment.id)
            .set(metadata)
            .await()

        // Return updated attachment with Firebase URL
        return attachment.copy(
            storagePath = downloadUrl,
            status = AttachmentStatus.READY
        )
    }

    /**
     * Delete attachment from Firebase Storage and Firestore
     */
    suspend fun deleteFromFirebase(
        attachmentId: String,
        chatId: String
    ) {
        val userId = auth.currentUser?.uid ?: return

        try {
            // Get metadata to find storage path
            val doc = firestore.collection("users")
                .document(userId)
                .collection("chats")
                .document(chatId)
                .collection("attachments")
                .document(attachmentId)
                .get()
                .await()

            val kind = doc.getString("kind")
            val extension = when (kind) {
                "PHOTO" -> "jpg"
                "PDF" -> "pdf"
                else -> "bin"
            }

            // Delete from Storage
            val storagePath = "users/$userId/chats/$chatId/$attachmentId.$extension"
            storage.reference.child(storagePath).delete().await()

            // Delete metadata from Firestore
            doc.reference.delete().await()

        } catch (e: Exception) {
            // Silent fail - attachment might already be deleted
        }
    }
}
