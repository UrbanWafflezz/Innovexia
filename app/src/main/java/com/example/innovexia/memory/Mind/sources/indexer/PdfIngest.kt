package com.example.innovexia.memory.Mind.sources.indexer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.innovexia.memory.Mind.sources.SourcesConfig
import com.example.innovexia.memory.Mind.sources.store.entities.SourceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Handles PDF file ingestion: copying, metadata extraction, thumbnail generation
 */
class PdfIngest(
    private val context: Context,
    private val config: SourcesConfig
) {

    companion object {
        private const val TAG = "PdfIngest"
    }

    /**
     * Ingest a PDF from URI
     * @return SourceEntity with initial metadata, or null on error
     */
    suspend fun ingestPdf(
        personaId: String,
        uri: Uri,
        displayName: String
    ): Result<SourceEntity> = withContext(Dispatchers.IO) {
        try {
            // Validate MIME type
            val mimeType = context.contentResolver.getType(uri)
            if (mimeType != "application/pdf") {
                return@withContext Result.failure(
                    Exception("Invalid file type: $mimeType. Only PDF files are supported.")
                )
            }

            val sourceId = UUID.randomUUID().toString()

            // Create storage directories
            val sourceDir = getPersonaSourcesDir(personaId)
            sourceDir.mkdirs()

            val pdfFile = File(sourceDir, "$sourceId.pdf")
            val thumbFile = File(sourceDir, "$sourceId-thumb.png")

            // Copy PDF to internal storage
            var fileSize = 0L
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(pdfFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        fileSize += read
                    }
                }
            } ?: return@withContext Result.failure(Exception("Failed to open PDF file"))

            // Check file size
            val maxBytes = config.maxPdfMB * 1024 * 1024L
            if (fileSize > maxBytes) {
                pdfFile.delete()
                return@withContext Result.failure(
                    Exception("PDF too large: ${fileSize / 1024 / 1024}MB (max: ${config.maxPdfMB}MB)")
                )
            }

            // Extract metadata and generate thumbnail
            var pageCount = 0
            var thumbnailGenerated = false

            try {
                ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                    PdfRenderer(fd).use { renderer ->
                        pageCount = renderer.pageCount

                        // Check page count
                        if (pageCount > config.maxPages) {
                            pdfFile.delete()
                            return@withContext Result.failure(
                                Exception("PDF has too many pages: $pageCount (max: ${config.maxPages})")
                            )
                        }

                        // Generate thumbnail from first page
                        if (pageCount > 0) {
                            renderer.openPage(0).use { page ->
                                val aspectRatio = page.width.toFloat() / page.height.toFloat()
                                val thumbWidth = config.thumbnailWidth
                                val thumbHeight = (thumbWidth / aspectRatio).toInt()

                                val bitmap = Bitmap.createBitmap(
                                    thumbWidth,
                                    thumbHeight,
                                    Bitmap.Config.ARGB_8888
                                )

                                page.render(
                                    bitmap,
                                    null,
                                    null,
                                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                                )

                                // Save thumbnail
                                FileOutputStream(thumbFile).use { out ->
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                                }
                                bitmap.recycle()
                                thumbnailGenerated = true
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting PDF metadata", e)
                pdfFile.delete()
                thumbFile.delete()

                // Check if it's an encrypted PDF
                if (e.message?.contains("password", ignoreCase = true) == true ||
                    e.message?.contains("encrypted", ignoreCase = true) == true
                ) {
                    return@withContext Result.failure(
                        Exception("Encrypted PDF not supported. Please provide an unencrypted version.")
                    )
                }

                return@withContext Result.failure(
                    Exception("Failed to process PDF: ${e.message}")
                )
            }

            // Create SourceEntity
            val entity = SourceEntity(
                id = sourceId,
                personaId = personaId,
                type = "PDF",
                displayName = displayName.substringBeforeLast('.'),
                fileName = "$sourceId.pdf",
                mime = "application/pdf",
                bytes = fileSize,
                pageCount = pageCount,
                addedAt = System.currentTimeMillis(),
                lastIndexedAt = null,
                status = "NOT_INDEXED",
                errorMsg = null,
                storagePath = pdfFile.absolutePath,
                thumbPath = if (thumbnailGenerated) thumbFile.absolutePath else null
            )

            Result.success(entity)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ingest PDF", e)
            Result.failure(e)
        }
    }

    /**
     * Get persona-specific sources directory
     */
    private fun getPersonaSourcesDir(personaId: String): File {
        return File(context.filesDir, "sources/$personaId")
    }

    /**
     * Delete source files
     */
    suspend fun deleteSourceFiles(source: SourceEntity) = withContext(Dispatchers.IO) {
        try {
            File(source.storagePath).delete()
            source.thumbPath?.let { File(it).delete() }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting source files", e)
        }
    }
}
