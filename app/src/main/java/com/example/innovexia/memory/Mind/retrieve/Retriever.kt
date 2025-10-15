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
     * Recall memories for a query using hybrid ranking
     */
    suspend fun recall(
        personaId: String,
        query: String,
        k: Int = config.kReturn
    ): List<MemoryHit> {
        val normalized = Normalizers.normalize(query)
        val now = System.currentTimeMillis()

        // Get candidates from FTS
        val ftsIds = try {
            ftsDao.search(personaId, normalized, config.kFts)
        } catch (e: Exception) {
            emptyList()
        }

        // Get candidates from vector similarity
        val vectorIds = getVectorCandidates(personaId, normalized, config.kVec)

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
     * Get vector similarity candidates
     */
    private suspend fun getVectorCandidates(
        personaId: String,
        query: String,
        k: Int
    ): List<String> {
        try {
            val queryEmbedding = embedder.embed(query)
            val (queryQ8, queryScale) = Quantizer.quantize(queryEmbedding)

            val allVectors = vectorDao.getAllByPersona(personaId)
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
