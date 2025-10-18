package com.example.innovexia.memory.Mind.retrieve

import com.example.innovexia.memory.Mind.api.MemoryConfig
import com.example.innovexia.memory.Mind.api.MemoryHit
import com.example.innovexia.memory.Mind.api.MemoryKind
import com.example.innovexia.memory.Mind.embed.Embedder
import com.example.innovexia.memory.Mind.embed.Quantizer
import com.example.innovexia.memory.Mind.ingest.Normalizers
import com.example.innovexia.memory.Mind.store.dao.MemoryDao
import com.example.innovexia.memory.Mind.store.dao.MemoryFtsDao
import com.example.innovexia.memory.Mind.store.dao.VectorDao
import com.example.innovexia.memory.Mind.store.entities.MemoryEntity

/**
 * Hybrid retrieval using FTS + vector similarity
 */
class Retriever(
    private val memoryDao: MemoryDao,
    private val ftsDao: MemoryFtsDao,
    private val vectorDao: VectorDao,
    private val embedder: Embedder,
    private val config: MemoryConfig
) {

    /**
     * Recall memories for a query using hybrid ranking.
     * Supports temporal queries like "yesterday", "last week", "December 15", etc.
     */
    suspend fun recall(
        personaId: String,
        userId: String,
        query: String,
        k: Int = config.kReturn
    ): List<MemoryHit> {
        val normalized = Normalizers.normalize(query)
        val now = System.currentTimeMillis()

        // Check if this is a temporal query (e.g., "yesterday", "last week", "December 15")
        val temporalQuery = TemporalQueryParser.parse(query)

        if (temporalQuery != null) {
            // TEMPORAL MODE: Query is time-based, prioritize time filtering
            android.util.Log.d("Retriever", "Detected temporal query: ${temporalQuery.description} (${temporalQuery.startTimeMs} to ${temporalQuery.endTimeMs})")
            return recallTemporalMemories(
                personaId = personaId,
                userId = userId,
                query = normalized,
                temporalQuery = temporalQuery,
                now = now,
                k = k
            )
        }

        // STANDARD MODE: Semantic retrieval with recency weighting
        // Get candidates from FTS
        val ftsIds = try {
            ftsDao.search(personaId, userId, normalized, config.kFts)
        } catch (e: Exception) {
            emptyList()
        }

        // Get candidates from vector similarity
        val vectorIds = getVectorCandidates(personaId, userId, normalized, config.kVec)

        // Combine and deduplicate
        val allIds = (ftsIds + vectorIds).distinct()
        if (allIds.isEmpty()) {
            return emptyList()
        }

        // Fetch memories
        val memories = allIds.mapNotNull { memoryDao.getById(it) }

        // Compute query vector for scoring
        val queryEmbedding = embedder.embed(normalized)
        val (queryQ8, queryScale) = Quantizer.quantize(queryEmbedding)

        // Score and rank
        val scored = memories.map { memory ->
            val score = computeScore(
                memory = memory,
                queryQ8 = queryQ8,
                queryScale = queryScale,
                now = now,
                ftsMatch = memory.id in ftsIds,
                vecMatch = memory.id in vectorIds
            )
            MemoryHit(
                memory = memory.toMemory(),
                score = score,
                fromChatTitle = null // TODO: fetch chat title
            )
        }

        // Update last accessed
        val topIds = scored.sortedByDescending { it.score }
            .take(k)
            .map { it.memory.id }
        if (topIds.isNotEmpty()) {
            memoryDao.updateLastAccessed(topIds, now)
        }

        return scored.sortedByDescending { it.score }.take(k)
    }

    /**
     * Recall memories for temporal queries (time-range based retrieval).
     * First filters by time, then applies semantic ranking within that subset.
     */
    private suspend fun recallTemporalMemories(
        personaId: String,
        userId: String,
        query: String,
        temporalQuery: TemporalQueryParser.TemporalQuery,
        now: Long,
        k: Int
    ): List<MemoryHit> {
        // Step 1: Get all memories within the specified time range
        // Use a generous limit since we'll apply semantic ranking afterward
        val timeFilteredMemories = memoryDao.getMemoriesBetweenTimes(
            personaId = personaId,
            userId = userId,
            startTimeMs = temporalQuery.startTimeMs,
            endTimeMs = temporalQuery.endTimeMs,
            limit = 500 // Get up to 500 memories in time range
        )

        if (timeFilteredMemories.isEmpty()) {
            android.util.Log.d("Retriever", "No memories found in time range ${temporalQuery.description}")
            return emptyList()
        }

        android.util.Log.d("Retriever", "Found ${timeFilteredMemories.size} memories in time range ${temporalQuery.description}")

        // Step 2: Apply semantic ranking within the time-filtered results
        // Get FTS candidates (only from time-filtered set)
        val timeFilteredIds = timeFilteredMemories.map { it.id }.toSet()
        val ftsIds = try {
            ftsDao.search(personaId, userId, query, config.kFts).filter { it in timeFilteredIds }
        } catch (e: Exception) {
            emptyList()
        }

        // Get vector candidates
        val queryEmbedding = embedder.embed(query)
        val (queryQ8, queryScale) = Quantizer.quantize(queryEmbedding)

        // Score memories within time range
        val scored = timeFilteredMemories.map { memory ->
            // For temporal queries, we want to:
            // 1. Prioritize semantic match (FTS + vector)
            // 2. Sort by time (chronological) rather than recency score
            // 3. Still consider importance

            var score = 0.0

            // FTS match
            if (memory.id in ftsIds) {
                score += config.w1Bm25
            }

            // Vector similarity
            val memVec = vectorDao.getByMemoryId(memory.id)
            if (memVec != null) {
                try {
                    val cosine = Quantizer.cosineSimilarity(
                        queryQ8, queryScale, memVec.q8, memVec.scale
                    )
                    score += config.w2Cosine * cosine
                } catch (e: Exception) {
                    // Dimension mismatch - skip vector scoring
                }
            }

            // For temporal queries, use chronological ordering instead of recency decay
            // Normalize timestamp to [0, 1] within the time range for consistent scoring
            val timeRangeMs = temporalQuery.endTimeMs - temporalQuery.startTimeMs
            val positionInRange = if (timeRangeMs > 0) {
                (memory.createdAt - temporalQuery.startTimeMs).toDouble() / timeRangeMs
            } else {
                0.5 // Single point in time
            }
            // Give slight preference to more recent memories within the range
            score += config.w3Recency * positionInRange

            // Importance
            score += config.w4Importance * memory.importance

            MemoryHit(
                memory = memory.toMemory(),
                score = score,
                fromChatTitle = null
            )
        }

        // Sort by score (if semantic match) or by time (if no semantic match)
        val sorted = if (ftsIds.isNotEmpty() || scored.any { it.score > 0.0 }) {
            // Has semantic matches - sort by score
            scored.sortedByDescending { it.score }
        } else {
            // No semantic matches - sort chronologically (newest first within time range)
            scored.sortedByDescending { it.memory.createdAt }
        }

        val topMemories = sorted.take(k)

        // Update last accessed
        val topIds = topMemories.map { it.memory.id }
        if (topIds.isNotEmpty()) {
            memoryDao.updateLastAccessed(topIds, now)
        }

        return topMemories
    }

    /**
     * Get vector similarity candidates
     */
    private suspend fun getVectorCandidates(
        personaId: String,
        userId: String,
        query: String,
        k: Int
    ): List<String> {
        try {
            val queryEmbedding = embedder.embed(query)
            val (queryQ8, queryScale) = Quantizer.quantize(queryEmbedding)

            val allVectors = vectorDao.getAllByPersona(personaId, userId)
            if (allVectors.isEmpty()) return emptyList()

            // Filter out vectors with mismatched dimensions (from old schema)
            val expectedDim = queryEmbedding.size
            val validVectors = allVectors.filter { vec ->
                if (vec.dim != expectedDim) {
                    android.util.Log.w("Retriever", "Skipping vector ${vec.memoryId} with dim=${vec.dim}, expected=$expectedDim")
                    false
                } else {
                    true
                }
            }

            return validVectors
                .map { vec ->
                    val sim = Quantizer.cosineSimilarity(queryQ8, queryScale, vec.q8, vec.scale)
                    vec.memoryId to sim
                }
                .sortedByDescending { it.second }
                .take(k)
                .map { it.first }
        } catch (e: Exception) {
            android.util.Log.e("Retriever", "Error getting vector candidates: ${e.message}", e)
            return emptyList()
        }
    }

    /**
     * Compute hybrid score for ranking
     */
    private suspend fun computeScore(
        memory: MemoryEntity,
        queryQ8: ByteArray,
        queryScale: Float,
        now: Long,
        ftsMatch: Boolean,
        vecMatch: Boolean
    ): Double {
        var score = 0.0

        // FTS match score (BM25 approximation)
        if (ftsMatch) {
            score += config.w1Bm25
        }

        // Vector cosine similarity
        if (vecMatch) {
            val memVec = vectorDao.getByMemoryId(memory.id)
            if (memVec != null) {
                try {
                    val cosine = Quantizer.cosineSimilarity(
                        queryQ8, queryScale, memVec.q8, memVec.scale
                    )
                    score += config.w2Cosine * cosine
                } catch (e: Exception) {
                    // Dimension mismatch or other error - skip vector scoring
                    android.util.Log.w("Retriever", "Failed to compute similarity for ${memory.id}: ${e.message}")
                }
            }
        }

        // Recency (exponential decay)
        val ageMs = now - memory.createdAt
        val ageDays = ageMs / (1000.0 * 60 * 60 * 24)
        val recencyScore = Math.exp(-ageDays / 30.0) // decay over 30 days
        score += config.w3Recency * recencyScore

        // Importance
        score += config.w4Importance * memory.importance

        return score
    }

    /**
     * Convert MemoryEntity to Memory
     */
    private fun MemoryEntity.toMemory() = com.example.innovexia.memory.Mind.api.Memory(
        id = id,
        personaId = personaId,
        userId = userId,
        chatId = chatId,
        role = role,
        text = text,
        kind = com.example.innovexia.memory.Mind.api.MemoryKind.valueOf(kind),
        emotion = emotion?.let { com.example.innovexia.memory.Mind.api.EmotionType.valueOf(it) },
        importance = importance,
        createdAt = createdAt,
        lastAccessed = lastAccessed
    )
}
