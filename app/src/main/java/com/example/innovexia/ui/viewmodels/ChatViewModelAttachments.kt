package com.example.innovexia.ui.viewmodels

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.innovexia.data.ai.AttachmentUploader
import com.example.innovexia.data.ai.FileProcessor
import com.example.innovexia.data.ai.ImageProcessor
import com.example.innovexia.data.models.AttachmentKind
import com.example.innovexia.data.models.AttachmentMeta
import com.example.innovexia.data.models.AttachmentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Extension of ChatViewModel for attachment handling
 * Manages attachment state and processing
 */
class AttachmentManager(
    private val context: Context,
    private val chatId: String,
    private val isIncognito: () -> Boolean,
    private val hasCloudBackup: () -> Boolean
) {
    private val imageProcessor by lazy { ImageProcessor(context.cacheDir) }
    private val fileProcessor by lazy { FileProcessor(context.cacheDir) }
    private val uploader by lazy { AttachmentUploader() }

    // Attachments for current message being composed
    private val _attachments = MutableStateFlow<List<AttachmentMeta>>(emptyList())
    val attachments: StateFlow<List<AttachmentMeta>> = _attachments.asStateFlow()

    /**
     * Process selected URIs and add as attachments
     */
    suspend fun processUris(uris: List<Uri>, resolver: ContentResolver) = withContext(Dispatchers.IO) {
        uris.forEach { uri ->
            try {
                val mime = resolver.getType(uri) ?: guessMime(uri)
                when {
                    mime.startsWith("image/") -> processImage(uri, mime, resolver)
                    mime == "application/pdf" -> processPdf(uri, resolver)
                    else -> {
                        // Unsupported type - could show a toast/snackbar
                        android.util.Log.w("AttachmentManager", "Unsupported file type: $mime")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AttachmentManager", "Error processing URI: $uri", e)
            }
        }
    }

    /**
     * Process an image attachment
     */
    private suspend fun processImage(uri: Uri, mime: String, resolver: ContentResolver) {
        // Create pending attachment
        val attachment = AttachmentMeta(
            name = "image_${System.currentTimeMillis()}.jpg",
            mime = mime,
            kind = AttachmentKind.PHOTO,
            localUri = uri,
            status = AttachmentStatus.PREPPING,
            localOnly = isIncognito() || !hasCloudBackup()
        )
        _attachments.value += attachment

        try {
            // Process image (downscale, convert, strip EXIF)
            val processed = imageProcessor.prepareForGemini(resolver, uri)

            // Update attachment with processed data
            val updated = attachment.copy(
                name = processed.displayName,
                mime = "image/jpeg",
                bytes = processed.size,
                width = processed.width,
                height = processed.height,
                localUri = processed.uri,
                status = if (attachment.localOnly) AttachmentStatus.READY else AttachmentStatus.UPLOADING
            )
            _attachments.value = _attachments.value.map { if (it.id == attachment.id) updated else it }

            // Upload to Firebase if not incognito and cloud backup enabled
            if (!updated.localOnly) {
                val uploaded = uploader.uploadToFirebase(updated, chatId)
                _attachments.value = _attachments.value.map { if (it.id == attachment.id) uploaded else it }
            }

        } catch (e: Exception) {
            // Mark as failed
            val failed = attachment.copy(
                status = AttachmentStatus.FAILED,
                errorMessage = e.message
            )
            _attachments.value = _attachments.value.map { if (it.id == attachment.id) failed else it }
        }
    }

    /**
     * Process a PDF attachment
     */
    private suspend fun processPdf(uri: Uri, resolver: ContentResolver) {
        // Create pending attachment
        val attachment = AttachmentMeta(
            name = "document_${System.currentTimeMillis()}.pdf",
            mime = "application/pdf",
            kind = AttachmentKind.PDF,
            localUri = uri,
            status = AttachmentStatus.PREPPING,
            localOnly = isIncognito() || !hasCloudBackup()
        )
        _attachments.value += attachment

        try {
            // Copy to cache and validate size
            val processed = fileProcessor.copyToCache(resolver, uri, enforceMaxMB = 30)

            // Update attachment with processed data
            val updated = attachment.copy(
                name = processed.displayName,
                bytes = processed.size,
                localUri = processed.uri,
                status = if (attachment.localOnly) AttachmentStatus.READY else AttachmentStatus.UPLOADING
            )
            _attachments.value = _attachments.value.map { if (it.id == attachment.id) updated else it }

            // Upload to Firebase if not incognito and cloud backup enabled
            if (!updated.localOnly) {
                val uploaded = uploader.uploadToFirebase(updated, chatId)
                _attachments.value = _attachments.value.map { if (it.id == attachment.id) uploaded else it }
            }

        } catch (e: Exception) {
            // Mark as failed
            val failed = attachment.copy(
                status = AttachmentStatus.FAILED,
                errorMessage = e.message
            )
            _attachments.value = _attachments.value.map { if (it.id == attachment.id) failed else it }
        }
    }

    /**
     * Remove an attachment
     */
    fun removeAttachment(attachmentId: String) {
        val attachment = _attachments.value.find { it.id == attachmentId }

        // Clean up local cache file if it exists
        attachment?.localUri?.path?.let { path ->
            try {
                File(path).delete()
            } catch (e: Exception) {
                // Ignore
            }
        }

        _attachments.value = _attachments.value.filter { it.id != attachmentId }
    }

    /**
     * Get ready attachments for sending
     */
    fun getReadyAttachments(): List<AttachmentMeta> {
        return _attachments.value.filter { it.status == AttachmentStatus.READY }
    }

    /**
     * Clear all attachments
     */
    fun clearAttachments() {
        // Clean up local cache files
        _attachments.value.forEach { attachment ->
            attachment.localUri?.path?.let { path ->
                try {
                    File(path).delete()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        _attachments.value = emptyList()
    }

    /**
     * Guess MIME type from URI
     */
    private fun guessMime(uri: Uri): String {
        val path = uri.path ?: return "application/octet-stream"
        return when (path.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "heic", "heif" -> "image/heic"
            "pdf" -> "application/pdf"
            else -> "application/octet-stream"
        }
    }
}
