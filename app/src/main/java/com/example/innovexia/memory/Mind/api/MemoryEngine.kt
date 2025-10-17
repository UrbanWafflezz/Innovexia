package com.example.innovexia.memory.Mind.api

import kotlinx.coroutines.flow.Flow

/**
 * Main memory engine interface
 */
interface MemoryEngine {

    // Per-persona enable/disable
    suspend fun enable(personaId: String, enabled: Boolean)
    suspend fun isEnabled(personaId: String): Boolean

    // Ingestion
    suspend fun ingest(turn: ChatTurn, personaId: String, incognito: Boolean)

    // Context building for LLM
    suspend fun contextFor(message: String, personaId: String, chatId: String): ContextBundle

    // UI queries
    fun observeCounts(personaId: String, userId: String): Flow<List<CategoryCount>>
    fun feed(personaId: String, userId: String, kind: MemoryKind?, query: String?): Flow<List<MemoryHit>>

    // Management
    suspend fun delete(memoryId: String)
    suspend fun deleteAll(personaId: String)

    // Stats
    suspend fun getCount(personaId: String, userId: String): Int
}
