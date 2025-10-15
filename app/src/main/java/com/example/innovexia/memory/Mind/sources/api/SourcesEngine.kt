package com.example.innovexia.memory.Mind.sources.api

import android.net.Uri
import com.example.innovexia.memory.Mind.sources.store.entities.SourceChunkEntity
import com.example.innovexia.memory.Mind.sources.store.entities.SourceEntity
import kotlinx.coroutines.flow.Flow

/**
 * Public interface for the Sources engine
 */
interface SourcesEngine {

    /**
     * Add a PDF from URI
     * @param personaId ID of the persona to associate the PDF with
     * @param uri Content URI of the PDF file
     * @return Result with source ID on success, error on failure
     */
    suspend fun addPdfFromUri(personaId: String, uri: Uri): Result<String>

    /**
     * Add any supported file from URI (auto-detects type)
     * Supports: PDF, .kt, .md, .json, .xml, .kts, .txt, .java, .py, and more
     * @param personaId ID of the persona to associate the file with
     * @param uri Content URI of the file
     * @return Result with source ID on success, error on failure
     */
    suspend fun addFileFromUri(personaId: String, uri: Uri): Result<String>

    /**
     * Add a URL source
     * @param personaId ID of the persona to associate the URL with
     * @param url URL to index
     * @param maxDepth Maximum crawl depth (default 2)
     * @param maxPages Maximum pages to index (default 10)
     * @return Result with source ID on success, error on failure
     */
    suspend fun addUrlSource(
        personaId: String,
        url: String,
        maxDepth: Int = 2,
        maxPages: Int = 10
    ): Result<String>

    /**
     * Observe a single source
     */
    fun observeSource(personaId: String, sourceId: String): Flow<SourceEntity?>

    /**
     * Observe all sources for a persona
     */
    fun observeSources(personaId: String): Flow<List<SourceEntity>>

    /**
     * List all chunks for a source
     */
    suspend fun listChunks(sourceId: String): List<SourceChunkEntity>

    /**
     * Reindex a source (re-extract and re-embed)
     */
    suspend fun reindex(sourceId: String): Result<Unit>

    /**
     * Remove a source and all its chunks
     */
    suspend fun removeSource(sourceId: String): Result<Unit>

    /**
     * Get total storage used by a persona's sources
     */
    suspend fun getStorageUsed(personaId: String): Long

    /**
     * Get count of sources for a persona
     */
    suspend fun getSourceCount(personaId: String): Int

    /**
     * Search for relevant chunks based on query
     * @param personaId Persona to search within
     * @param query User's query text
     * @param limit Maximum number of chunks to return
     * @return List of chunks ranked by similarity
     */
    suspend fun searchChunks(
        personaId: String,
        query: String,
        limit: Int = 5
    ): List<SourceChunkEntity>

    /**
     * Get formatted context from query
     * @param personaId Persona to search within
     * @param query User's query text
     * @param limit Maximum number of chunks to return
     * @return Formatted context string ready to inject into chat
     */
    suspend fun getContextForQuery(
        personaId: String,
        query: String,
        limit: Int = 5
    ): String
}
