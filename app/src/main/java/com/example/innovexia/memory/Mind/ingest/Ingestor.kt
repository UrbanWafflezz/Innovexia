package com.example.innovexia.memory.Mind.ingest

import com.example.innovexia.memory.Mind.api.ChatTurn
import com.example.innovexia.memory.Mind.embed.Embedder
import com.example.innovexia.memory.Mind.embed.Quantizer
import com.example.innovexia.memory.Mind.store.dao.MemoryDao
import com.example.innovexia.memory.Mind.store.dao.MemoryFtsDao
import com.example.innovexia.memory.Mind.store.dao.VectorDao
import com.example.innovexia.memory.Mind.store.entities.MemoryEntity
import com.example.innovexia.memory.Mind.store.entities.MemoryFtsEntity
import com.example.innovexia.memory.Mind.store.entities.MemoryVectorEntity
import java.util.UUID

/**
 * Handles ingestion of chat turns into memory
 */
class Ingestor(
    private val memoryDao: MemoryDao,
    private val ftsDao: MemoryFtsDao,
    private val vectorDao: VectorDao,
    private val embedder: Embedder
) {

    /**
     * Ingest a chat turn (user + assistant messages)
     */
    suspend fun ingest(turn: ChatTurn, personaId: String, incognito: Boolean) {
        android.util.Log.d("Ingestor", "Starting ingestion for persona=$personaId, incognito=$incognito")
        val now = System.currentTimeMillis()
        val memories = mutableListOf<MemoryEntity>()
        val vectors = mutableListOf<MemoryVectorEntity>()

        // Process user message (save ALL messages, no filtering)
        android.util.Log.d("Ingestor", "User message: '${turn.userMessage}' (length=${turn.userMessage.length})")
        if (turn.userMessage.isNotBlank()) {
            val userMem = createMemory(
                text = turn.userMessage,
                role = "user",
                personaId = personaId,
                userId = turn.userId,
                chatId = turn.chatId,
                now = now
            )
            memoryDao.insert(userMem)
            ftsDao.insert(MemoryFtsEntity(id = userMem.id, text = userMem.text))

            val userVec = createVector(turn.userMessage, userMem.id)
            vectorDao.insert(userVec)
            android.util.Log.d("Ingestor", "Saved user memory: id=${userMem.id}, dim=${userVec.dim}")
        } else {
            android.util.Log.d("Ingestor", "User message is blank, skipping")
        }

        // Process assistant message if present (save ALL messages, no filtering)
        turn.assistantMessage?.let { assistantMsg ->
            if (assistantMsg.isNotBlank()) {
                val assistantMem = createMemory(
                    text = assistantMsg,
                    role = "model",
                    personaId = personaId,
                    userId = turn.userId,
                    chatId = turn.chatId,
                    now = now
                )
                memoryDao.insert(assistantMem)
                ftsDao.insert(MemoryFtsEntity(id = assistantMem.id, text = assistantMem.text))

                val assistantVec = createVector(assistantMsg, assistantMem.id)
                vectorDao.insert(assistantVec)
                android.util.Log.d("Ingestor", "Saved assistant memory: id=${assistantMem.id}, dim=${assistantVec.dim}")
            } else {
                android.util.Log.d("Ingestor", "Assistant message is blank, skipping")
            }
        }
        android.util.Log.d("Ingestor", "Ingestion complete")
    }

    /**
     * Create a memory entity from text
     */
    private fun createMemory(
        text: String,
        role: String,
        personaId: String,
        userId: String,
        chatId: String,
        now: Long
    ): MemoryEntity {
        val normalized = Normalizers.normalize(text)
        val kind = Heuristics.classifyKind(normalized)
        val emotion = Heuristics.detectEmotion(normalized)
        val importance = Heuristics.calculateImportance(normalized, kind, emotion)

        return MemoryEntity(
            id = UUID.randomUUID().toString(),
            personaId = personaId,
            userId = userId,
            chatId = chatId,
            role = role,
            text = normalized,
            kind = kind.name,
            emotion = emotion?.name,
            importance = importance,
            createdAt = now,
            lastAccessed = now
        )
    }

    /**
     * Create vector entity from text
     */
    private suspend fun createVector(text: String, memoryId: String): MemoryVectorEntity {
        val normalized = Normalizers.normalize(text)
        val embedding = embedder.embed(normalized)
        val (q8, scale) = Quantizer.quantize(embedding)

        return MemoryVectorEntity(
            memoryId = memoryId,
            dim = embedding.size,
            q8 = q8,
            scale = scale
        )
    }
}
