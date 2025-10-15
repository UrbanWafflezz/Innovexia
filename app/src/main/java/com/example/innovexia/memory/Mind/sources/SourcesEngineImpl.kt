package com.example.innovexia.memory.Mind.sources

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.example.innovexia.memory.Mind.di.MindModule
import com.example.innovexia.memory.Mind.sources.api.SourcesEngine
// import com.example.innovexia.memory.Mind.sources.indexer.DocumentIngest  // DISABLED: POI requires API 26+
import com.example.innovexia.memory.Mind.sources.indexer.IndexPdfWork
import com.example.innovexia.memory.Mind.sources.indexer.PdfIngest
import com.example.innovexia.memory.Mind.sources.indexer.TextIngest
import com.example.innovexia.memory.Mind.sources.retrieve.SourcesRetriever
import com.example.innovexia.memory.Mind.sources.web.UrlIngest
import com.example.innovexia.memory.Mind.sources.web.WebIndexerWork
import com.example.innovexia.memory.Mind.sources.store.SourcesDatabase
import com.example.innovexia.memory.Mind.sources.store.entities.SourceChunkEntity
import com.example.innovexia.memory.Mind.sources.store.entities.SourceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Implementation of SourcesEngine
 */
class SourcesEngineImpl(
    private val context: Context,
    private val database: SourcesDatabase,
    private val pdfIngest: PdfIngest,
    private val textIngest: TextIngest,
    // private val documentIngest: DocumentIngest,  // DISABLED: POI requires API 26+
    private val config: SourcesConfig
) : SourcesEngine {

    companion object {
        private const val TAG = "SourcesEngine"
    }

    private val workManager = WorkManager.getInstance(context)
    private val urlIngest = UrlIngest(context)

    // Lazy-init retriever with embedder
    private val retriever: SourcesRetriever by lazy {
        val embedder = MindModule.provideEmbedder(config.dim, context)
        SourcesRetriever(database.chunkDao(), embedder)
    }

    override suspend fun addPdfFromUri(personaId: String, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                // Get display name from URI
                val displayName = getDisplayName(uri) ?: "document.pdf"

                Log.d(TAG, "Adding PDF: $displayName for persona: $personaId")

                // Ingest the PDF (copy, extract metadata, generate thumbnail)
                val result = pdfIngest.ingestPdf(personaId, uri, displayName)

                if (result.isFailure) {
                    return@withContext Result.failure(
                        result.exceptionOrNull() ?: Exception("Failed to ingest PDF")
                    )
                }

                val source = result.getOrNull()
                    ?: return@withContext Result.failure(Exception("Failed to get source entity"))

                // Save to database
                database.sourceDao().insert(source)

                // Enqueue indexing work
                val workRequest = IndexPdfWork.createWorkRequest(source.id, personaId)
                workManager.enqueueUniqueWork(
                    IndexPdfWork.getWorkName(source.id),
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )

                Log.d(TAG, "Successfully added PDF: ${source.id}")
                Result.success(source.id)

            } catch (e: Exception) {
                Log.e(TAG, "Error adding PDF", e)
                Result.failure(e)
            }
        }

    override suspend fun addFileFromUri(personaId: String, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                // Get display name from URI
                val displayName = getDisplayName(uri) ?: "document"

                Log.d(TAG, "Adding file: $displayName for persona: $personaId")

                // Determine file type from extension
                val extension = displayName.substringAfterLast('.', "").lowercase()

                val result = when {
                    extension == "pdf" -> {
                        pdfIngest.ingestPdf(personaId, uri, displayName)
                    }
                    TextIngest.SUPPORTED_TYPES.containsKey(extension) -> {
                        textIngest.ingestTextFile(personaId, uri, displayName)
                    }
                    // TEMPORARILY DISABLED: DocumentIngest requires Apache POI which needs API 26+
                    // DocumentIngest.SUPPORTED_TYPES.containsKey(extension) -> {
                    //     documentIngest.ingestDocument(personaId, uri, displayName)
                    // }
                    else -> {
                        return@withContext Result.failure(
                            Exception("Unsupported file type: .$extension (Office documents temporarily disabled)")
                        )
                    }
                }

                if (result.isFailure) {
                    return@withContext Result.failure(
                        result.exceptionOrNull() ?: Exception("Failed to ingest file")
                    )
                }

                val source = result.getOrNull()
                    ?: return@withContext Result.failure(Exception("Failed to get source entity"))

                // Save to database
                database.sourceDao().insert(source)

                // Enqueue indexing work
                val workRequest = IndexPdfWork.createWorkRequest(source.id, personaId)
                workManager.enqueueUniqueWork(
                    IndexPdfWork.getWorkName(source.id),
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )

                Log.d(TAG, "Successfully added file: ${source.id}")
                Result.success(source.id)

            } catch (e: Exception) {
                Log.e(TAG, "Error adding file", e)
                Result.failure(e)
            }
        }

    override suspend fun addUrlSource(
        personaId: String,
        url: String,
        maxDepth: Int,
        maxPages: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Adding URL: $url for persona: $personaId")

            // Ingest the URL (validate and create entity)
            val result = urlIngest.ingestUrl(personaId, url)

            if (result.isFailure) {
                return@withContext Result.failure(
                    result.exceptionOrNull() ?: Exception("Failed to ingest URL")
                )
            }

            val source = result.getOrNull()
                ?: return@withContext Result.failure(Exception("Failed to get source entity"))

            // Save to database
            database.sourceDao().insert(source)

            // Enqueue web indexing work
            val workRequest = WebIndexerWork.createWorkRequest(
                sourceId = source.id,
                personaId = personaId,
                url = url,
                maxDepth = maxDepth,
                maxPages = maxPages
            )
            workManager.enqueueUniqueWork(
                WebIndexerWork.getWorkName(source.id),
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            Log.d(TAG, "Successfully added URL: ${source.id}")
            Result.success(source.id)

        } catch (e: Exception) {
            Log.e(TAG, "Error adding URL", e)
            Result.failure(e)
        }
    }

    override fun observeSource(personaId: String, sourceId: String): Flow<SourceEntity?> {
        return database.sourceDao().observe(personaId, sourceId)
    }

    override fun observeSources(personaId: String): Flow<List<SourceEntity>> {
        return database.sourceDao().observeByPersona(personaId)
    }

    override suspend fun listChunks(sourceId: String): List<SourceChunkEntity> =
        withContext(Dispatchers.IO) {
            database.chunkDao().getBySource(sourceId)
        }

    override suspend fun reindex(sourceId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val source = database.sourceDao().getById(sourceId)
                    ?: return@withContext Result.failure(Exception("Source not found"))

                Log.d(TAG, "Reindexing source: $sourceId")

                // Delete existing chunks
                database.chunkDao().deleteBySource(sourceId)

                // Reset status to NOT_INDEXED
                database.sourceDao().updateStatus(sourceId, "NOT_INDEXED", null)

                // Enqueue indexing work based on source type
                if (source.type == "URL") {
                    // For URLs, use WebIndexerWork
                    val workRequest = WebIndexerWork.createWorkRequest(
                        sourceId = source.id,
                        personaId = source.personaId,
                        url = source.storagePath, // URL is stored in storagePath
                        maxDepth = source.depth ?: 2,
                        maxPages = 10
                    )
                    workManager.enqueueUniqueWork(
                        WebIndexerWork.getWorkName(source.id),
                        ExistingWorkPolicy.REPLACE,
                        workRequest
                    )
                } else {
                    // For files (PDF, TEXT, DOCUMENT), use IndexPdfWork
                    val workRequest = IndexPdfWork.createWorkRequest(source.id, source.personaId)
                    workManager.enqueueUniqueWork(
                        IndexPdfWork.getWorkName(source.id),
                        ExistingWorkPolicy.REPLACE,
                        workRequest
                    )
                }

                Result.success(Unit)

            } catch (e: Exception) {
                Log.e(TAG, "Error reindexing source", e)
                Result.failure(e)
            }
        }

    override suspend fun removeSource(sourceId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val source = database.sourceDao().getById(sourceId)
                    ?: return@withContext Result.failure(Exception("Source not found"))

                Log.d(TAG, "Removing source: $sourceId")

                // Cancel any ongoing indexing work based on source type
                if (source.type == "URL") {
                    workManager.cancelUniqueWork(WebIndexerWork.getWorkName(sourceId))
                } else {
                    workManager.cancelUniqueWork(IndexPdfWork.getWorkName(sourceId))
                }

                // Delete chunks (cascade will handle this, but being explicit)
                database.chunkDao().deleteBySource(sourceId)

                // Delete files (only for non-URL sources)
                if (source.type != "URL") {
                    pdfIngest.deleteSourceFiles(source)
                }

                // Delete from database
                database.sourceDao().deleteById(sourceId)

                Result.success(Unit)

            } catch (e: Exception) {
                Log.e(TAG, "Error removing source", e)
                Result.failure(e)
            }
        }

    override suspend fun getStorageUsed(personaId: String): Long =
        withContext(Dispatchers.IO) {
            database.sourceDao().getTotalBytes(personaId) ?: 0L
        }

    override suspend fun getSourceCount(personaId: String): Int =
        withContext(Dispatchers.IO) {
            database.sourceDao().getCount(personaId)
        }

    override suspend fun searchChunks(
        personaId: String,
        query: String,
        limit: Int
    ): List<SourceChunkEntity> =
        withContext(Dispatchers.IO) {
            try {
                retriever.search(personaId, query, limit)
            } catch (e: Exception) {
                Log.e(TAG, "Error searching chunks", e)
                emptyList()
            }
        }

    override suspend fun getContextForQuery(
        personaId: String,
        query: String,
        limit: Int
    ): String =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "getContextForQuery called - personaId: $personaId, query: $query, limit: $limit")
                val chunks = retriever.search(personaId, query, limit)
                Log.d(TAG, "Found ${chunks.size} chunks")
                chunks.forEachIndexed { i, chunk ->
                    Log.d(TAG, "Chunk $i: sourceId=${chunk.sourceId}, pages=${chunk.pageStart}-${chunk.pageEnd}, text=${chunk.text.take(100)}...")
                }
                val context = retriever.formatContext(chunks)
                Log.d(TAG, "Formatted context: ${context.length} chars")
                context
            } catch (e: Exception) {
                Log.e(TAG, "Error getting context for query", e)
                ""
            }
        }

    /**
     * Get display name from content URI
     */
    private fun getDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting display name", e)
            null
        }
    }
}
