package com.example.innovexia.data.models

import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

/**
 * Type of attachment
 */
enum class AttachmentKind {
    PHOTO,
    FILE,
    PDF
}

/**
 * Status of attachment processing/upload
 */
enum class AttachmentStatus {
    PENDING,      // Not yet processed
    PREPPING,     // Being processed (downsized, converted, etc.)
    UPLOADING,    // Uploading to cloud/Gemini
    READY,        // Ready to be sent
    FAILED        // Processing or upload failed
}

/**
 * Metadata for message attachments.
 * Stored as JSON in MessageEntity and mirrored to Firestore.
 *
 * Extended to support rich attachment handling for Gemini integration
 */
data class AttachmentMeta(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val mime: String,
    val bytes: Long = 0,
    val width: Int? = null,
    val height: Int? = null,
    val kind: AttachmentKind = AttachmentKind.FILE,
    @Transient val localUri: Uri? = null,           // Transient - not serialized to JSON
    val geminiFileId: String? = null,                // after upload via File API (if used)
    val storagePath: String? = null,                 // Cloud Storage path (for synced attachments)
    val localOnly: Boolean = false,                  // incognito or no-cloud
    @Transient val status: AttachmentStatus = AttachmentStatus.READY, // Transient - runtime only
    @Transient val errorMessage: String? = null      // Transient - runtime only
) {
    // For backwards compatibility
    val displayName: String get() = name
    val sizeBytes: Long get() = bytes
    val firebaseUrl: String? get() = storagePath
}

/**
 * Helper to serialize/deserialize list of attachments to JSON
 */
object AttachmentMetaSerializer {
    private val gson = Gson()
    private val listType = object : TypeToken<List<AttachmentMeta>>() {}.type

    fun toJson(attachments: List<AttachmentMeta>): String {
        return gson.toJson(attachments)
    }

    fun fromJson(json: String?): List<AttachmentMeta> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            gson.fromJson(json, listType)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
