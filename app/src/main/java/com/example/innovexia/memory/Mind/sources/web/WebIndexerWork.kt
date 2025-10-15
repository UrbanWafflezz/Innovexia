package com.example.innovexia.memory.Mind.sources.web

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.innovexia.memory.Mind.di.MindModule
import com.example.innovexia.memory.Mind.sources.SourcesConfig
import com.example.innovexia.memory.Mind.sources.indexer.Chunker
import com.example.innovexia.memory.Mind.sources.indexer.EmbedPipe
import com.example.innovexia.memory.Mind.sources.indexer.PageText
import com.example.innovexia.memory.Mind.sources.store.SourcesDatabase
import com.example.innovexia.memory.Mind.sources.store.entities.SourceChunkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager job for indexing web URLs in the background
 */
class WebIndexerWork(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "WebIndexerWork"
        private const val KEY_SOURCE_ID = "source_id"
        private const val KEY_PERSONA_ID = "persona_id"
        private const val KEY_URL = "url"
        private const val KEY_MAX_DEPTH = "max_depth"
        private const val KEY_MAX_PAGES = "max_pages"

        /**
         * Create work request for indexing a URL
         */
        fun createWorkRequest(
            sourceId: String,
            personaId: String,
            url: String,
            maxDepth: Int = 2,
            maxPages: Int = 10
        ): OneTimeWorkRequest {
            val data = Data.Builder()
                .putString(KEY_SOURCE_ID, sourceId)
                .putString(KEY_PERSONA_ID, personaId)
                .putString(KEY_URL, url)
                .putInt(KEY_MAX_DEPTH, maxDepth)
                .putInt(KEY_MAX_PAGES, maxPages)
                .build()

            return OneTimeWorkRequestBuilder<WebIndexerWork>()
                .setInputData(data)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
        }

        /**
         * Get unique work name for a source
         */
        fun getWorkName(sourceId: String): String = "index_web_$sourceId"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val sourceId = inputData.getString(KEY_SOURCE_ID)
                ?: return@withContext Result.failure()
            val personaId = inputData.getString(KEY_PERSONA_ID)
                ?: return@withContext Result.failure()
            val url = inputData.getString(KEY_URL)
                ?: return@withContext Result.failure()
            val maxDepth = inputData.getInt(KEY_MAX_DEPTH, 2)
            val maxPages = inputData.getInt(KEY_MAX_PAGES, 10)

            Log.d(TAG, "Starting URL indexing: $sourceId -> $url")

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
                // Expand links to discover subpages
                Log.d(TAG, "Expanding links from: $url (depth=$maxDepth, maxPages=$maxPages)")
                val linkExpander = LinkExpander(maxDepth, maxPages)
                val pagesToFetch = linkExpander.expand(url, 0)
                setProgress(Data.Builder().putInt("progress", 10).build())

                Log.d(TAG, "Discovered ${pagesToFetch.size} pages to fetch")

                // Fetch and parse all pages
                val fetcher = HtmlFetcher()
                val parser = HtmlParser()
                val allPageTexts = mutableListOf<PageText>()

                var firstPageTitle: String? = null
                var firstPageDesc: String? = null

                pagesToFetch.forEachIndexed { index, pageToFetch ->
                    try {
                        Log.d(TAG, "Fetching page ${index + 1}/${pagesToFetch.size}: ${pageToFetch.url}")

                        val fetchResult = fetcher.fetch(pageToFetch.url)
                        val pageData = parser.parse(fetchResult.content, fetchResult.finalUrl)

                        // Save metadata from first page
                        if (index == 0) {
                            firstPageTitle = pageData.title
                            firstPageDesc = pageData.description
                        }

                        // Add text as a "page"
                        if (pageData.text.isNotBlank()) {
                            allPageTexts.add(PageText(index + 1, pageData.text))
                        }

                        // Update progress
                        val progress = 10 + ((index + 1) * 30 / pagesToFetch.size)
                        setProgress(Data.Builder().putInt("progress", progress).build())

                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to fetch page: ${pageToFetch.url} - ${e.message}")
                        // Continue with other pages
                    }
                }

                if (allPageTexts.isEmpty()) {
                    sourceDao.updateStatus(
                        sourceId,
                        "ERROR",
                        "No text content found in any page"
                    )
                    return@withContext Result.failure()
                }

                Log.d(TAG, "Successfully fetched ${allPageTexts.size} pages")
                setProgress(Data.Builder().putInt("progress", 40).build())

                // Chunk the text
                val config = SourcesConfig.DEFAULT
                val chunker = Chunker(config)
                val chunks = chunker.chunk(allPageTexts)
                setProgress(Data.Builder().putInt("progress", 50).build())

                if (chunks.isEmpty()) {
                    sourceDao.updateStatus(
                        sourceId,
                        "ERROR",
                        "Failed to chunk text"
                    )
                    return@withContext Result.failure()
                }

                Log.d(TAG, "Created ${chunks.size} chunks from ${allPageTexts.size} pages")

                // Embed and quantize chunks
                val embedder = MindModule.provideEmbedder(config.dim, applicationContext)
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

                // Verify chunks were inserted
                val insertedChunks = chunkDao.getBySource(sourceId)
                Log.d(TAG, "Verified ${insertedChunks.size} chunks inserted for sourceId: $sourceId")
                insertedChunks.forEachIndexed { i, chunk ->
                    Log.d(TAG, "Inserted chunk $i: id=${chunk.id}, personaId=${chunk.personaId}, text=${chunk.text.take(50)}...")
                }

                // Calculate total bytes (approximate)
                val totalBytes = allPageTexts.sumOf { it.text.length }.toLong()

                // Update source with metadata and mark as READY
                val updatedSource = source.copy(
                    status = "READY",
                    lastIndexedAt = System.currentTimeMillis(),
                    errorMsg = null,
                    pageCount = allPageTexts.size,
                    bytes = totalBytes,
                    metaTitle = firstPageTitle,
                    metaDesc = firstPageDesc,
                    pagesIndexed = pagesToFetch.size
                )
                sourceDao.update(updatedSource)

                // Verify source was updated
                val verifiedSource = sourceDao.getById(sourceId)
                Log.d(TAG, "Verified source: id=$sourceId, personaId=${verifiedSource?.personaId}, status=${verifiedSource?.status}, title=${verifiedSource?.metaTitle}")

                // Log all chunks for this persona to debug retrieval
                val allPersonaChunks = chunkDao.getByPersona(personaId)
                Log.d(TAG, "Total chunks for personaId=$personaId: ${allPersonaChunks.size}")

                setProgress(Data.Builder().putInt("progress", 100).build())

                Log.d(TAG, "Successfully indexed URL: $sourceId (${chunkEntities.size} chunks, ${pagesToFetch.size} pages)")
                Result.success()

            } catch (e: Exception) {
                Log.e(TAG, "Error indexing URL: $sourceId", e)
                sourceDao.updateStatus(
                    sourceId,
                    "ERROR",
                    e.message ?: "Unknown error during indexing"
                )
                Result.failure()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in WebIndexerWork", e)
            Result.failure()
        }
    }
}
