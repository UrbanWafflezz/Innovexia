package com.example.innovexia.memory.Mind.sources.indexer

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.innovexia.memory.Mind.di.MindModule
import com.example.innovexia.memory.Mind.sources.SourcesConfig
import com.example.innovexia.memory.Mind.sources.store.SourcesDatabase
import com.example.innovexia.memory.Mind.sources.store.entities.SourceChunkEntity
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * WorkManager job for indexing PDF documents in the background
 */
class IndexPdfWork(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "IndexPdfWork"
        private const val KEY_SOURCE_ID = "source_id"
        private const val KEY_PERSONA_ID = "persona_id"

        /**
         * Create work request for indexing a PDF
         */
        fun createWorkRequest(sourceId: String, personaId: String): OneTimeWorkRequest {
            val data = Data.Builder()
                .putString(KEY_SOURCE_ID, sourceId)
                .putString(KEY_PERSONA_ID, personaId)
                .build()

            return OneTimeWorkRequestBuilder<IndexPdfWork>()
                .setInputData(data)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
        }

        /**
         * Get unique work name for a source
         */
        fun getWorkName(sourceId: String): String = "index_pdf_$sourceId"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val sourceId = inputData.getString(KEY_SOURCE_ID)
                ?: return@withContext Result.failure()
            val personaId = inputData.getString(KEY_PERSONA_ID)
                ?: return@withContext Result.failure()

            Log.d(TAG, "Starting PDF indexing: $sourceId")

            // Initialize PDFBox
            PDFBoxResourceLoader.init(applicationContext)

            // Get database and DAOs
            val database = SourcesDatabase.getInstance(applicationContext)
            val sourceDao = database.sourceDao()
            val chunkDao = database.chunkDao()

            // Get source entity
            val source = sourceDao.getById(sourceId)
                ?: return@withContext Result.failure()

            // Update status to INDEXING
            sourceDao.updateStatus(sourceId, "INDEXING", null)
            setProgress(Data.Builder().putInt("progress", 0).build())

            try {
                // Extract text based on source type
                val pages = when (source.type) {
                    "PDF" -> extractPdfText(File(source.storagePath))
                    "TEXT" -> extractTextFile(File(source.storagePath))
                    "DOCUMENT" -> {
                        val extension = source.fileName.substringAfterLast('.', "")
                        extractDocument(File(source.storagePath), extension)
                    }
                    else -> {
                        sourceDao.updateStatus(
                            sourceId,
                            "ERROR",
                            "Unsupported source type: ${source.type}"
                        )
                        return@withContext Result.failure()
                    }
                }
                setProgress(Data.Builder().putInt("progress", 30).build())

                if (pages.isEmpty()) {
                    sourceDao.updateStatus(
                        sourceId,
                        "ERROR",
                        "No text found in ${source.type}"
                    )
                    return@withContext Result.failure()
                }

                // Chunk the text
                val config = SourcesConfig.DEFAULT
                val chunker = Chunker(config)
                val chunks = chunker.chunk(pages)
                setProgress(Data.Builder().putInt("progress", 50).build())

                if (chunks.isEmpty()) {
                    sourceDao.updateStatus(
                        sourceId,
                        "ERROR",
                        "Failed to chunk PDF text"
                    )
                    return@withContext Result.failure()
                }

                // Embed and quantize chunks
                val memoryEngine = MindModule.provideMemoryEngine(applicationContext)
                val embedder = MindModule.provideEmbedder(config.dim)
                val embedPipe = EmbedPipe(embedder)

                val chunkEntities = mutableListOf<SourceChunkEntity>()
                val totalChunks = chunks.size

                // Process chunks in batches with progress updates
                chunks.chunked(10).forEachIndexed { batchIndex, batch ->
                    val batchEntities = embedPipe.embedChunksBatch(sourceId, personaId, batch)
                    chunkEntities.addAll(batchEntities)

                    // Update progress
                    val progress = 50 + ((batchIndex + 1) * 40 / ((totalChunks + 9) / 10))
                    setProgress(Data.Builder().putInt("progress", progress).build())
                }

                // Delete old chunks and insert new ones
                chunkDao.deleteBySource(sourceId)
                chunkDao.insertAll(chunkEntities)

                // Mark as READY
                sourceDao.updateIndexed(
                    sourceId,
                    "READY",
                    System.currentTimeMillis()
                )
                setProgress(Data.Builder().putInt("progress", 100).build())

                Log.d(TAG, "Successfully indexed PDF: $sourceId (${chunkEntities.size} chunks)")
                Result.success()

            } catch (e: Exception) {
                Log.e(TAG, "Error indexing PDF: $sourceId", e)
                sourceDao.updateStatus(
                    sourceId,
                    "ERROR",
                    e.message ?: "Unknown error during indexing"
                )
                Result.failure()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in IndexPdfWork", e)
            Result.failure()
        }
    }

    /**
     * Extract text from PDF using PDFBox
     */
    private suspend fun extractPdfText(pdfFile: File): List<PageText> =
        withContext(Dispatchers.IO) {
            val pages = mutableListOf<PageText>()

            try {
                PDDocument.load(pdfFile).use { document ->
                    if (document.isEncrypted) {
                        throw Exception("Encrypted PDF not supported")
                    }

                    val stripper = PDFTextStripper()
                    val totalPages = document.numberOfPages

                    for (pageNum in 1..totalPages) {
                        stripper.startPage = pageNum
                        stripper.endPage = pageNum

                        val text = stripper.getText(document)
                        if (text.isNotBlank()) {
                            pages.add(PageText(pageNum, text))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting PDF text", e)
                throw e
            }

            pages
        }

    /**
     * Extract text from a text file
     * Splits into "pages" of ~100 lines each for consistent chunking
     */
    private suspend fun extractTextFile(textFile: File): List<PageText> =
        withContext(Dispatchers.IO) {
            val pages = mutableListOf<PageText>()

            try {
                val lines = textFile.readLines()
                val linesPerPage = 100 // Treat every 100 lines as a "page"

                lines.chunked(linesPerPage).forEachIndexed { index, chunk ->
                    val text = chunk.joinToString("\n")
                    if (text.isNotBlank()) {
                        pages.add(PageText(index + 1, text))
                    }
                }

                // If file is empty or very small, at least create one page
                if (pages.isEmpty()) {
                    val text = textFile.readText()
                    if (text.isNotBlank()) {
                        pages.add(PageText(1, text))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting text file", e)
                throw e
            }

            pages
        }

    /**
     * Extract text from a document file
     * TEMPORARILY DISABLED - DocumentExtractor requires Apache POI (API 26+)
     */
    private suspend fun extractDocument(file: File, extension: String): List<PageText> =
        withContext(Dispatchers.IO) {
            throw Exception("Document extraction temporarily disabled (requires Apache POI / API 26+)")
            // try {
            //     val config = SourcesConfig.DEFAULT
            //     val extractor = DocumentExtractor(config)
            //     val result = extractor.extractText(file.absolutePath, extension)
            //
            //     result.getOrNull() ?: throw Exception("Failed to extract document text")
            // } catch (e: Exception) {
            //     Log.e(TAG, "Error extracting document", e)
            //     throw e
            // }
        }
}
