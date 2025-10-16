package com.example.innovexia.memory.Mind.di

import android.content.Context
import com.example.innovexia.memory.Mind.MemoryEngineImpl
import com.example.innovexia.memory.Mind.api.MemoryConfig
import com.example.innovexia.memory.Mind.api.MemoryEngine
import com.example.innovexia.memory.Mind.embed.Embedder
import com.example.innovexia.memory.Mind.embed.FakeEmbedder
import com.example.innovexia.memory.Mind.embed.GeminiEmbedder
import com.example.innovexia.memory.Mind.ingest.Ingestor
import com.example.innovexia.memory.Mind.retrieve.ContextBuilder
import com.example.innovexia.memory.Mind.retrieve.Retriever
import com.example.innovexia.memory.Mind.sources.SourcesConfig
import com.example.innovexia.memory.Mind.sources.SourcesEngineImpl
import com.example.innovexia.memory.Mind.sources.api.SourcesEngine
import com.example.innovexia.memory.Mind.sources.indexer.PdfIngest
import com.example.innovexia.memory.Mind.sources.store.SourcesDatabase
import com.example.innovexia.memory.Mind.store.MemoryDatabase

/**
 * Factory for creating MemoryEngine and SourcesEngine (no DI framework)
 */
object MindModule {

    @Volatile
    private var memoryEngine: MemoryEngine? = null

    @Volatile
    private var sourcesEngine: SourcesEngine? = null

    /**
     * Get or create singleton MemoryEngine
     */
    fun provideMemoryEngine(context: Context): MemoryEngine {
        return memoryEngine ?: synchronized(this) {
            memoryEngine ?: buildMemoryEngine(context).also {
                memoryEngine = it
            }
        }
    }

    /**
     * Build MemoryEngine with all dependencies
     */
    private fun buildMemoryEngine(context: Context): MemoryEngine {
        val config = MemoryConfig.DEFAULT
        val database = MemoryDatabase.getInstance(context)
        val embedder = provideEmbedder(config.dim, context)

        val ingestor = Ingestor(
            memoryDao = database.memoryDao(),
            ftsDao = database.memoryFtsDao(),
            vectorDao = database.vectorDao(),
            embedder = embedder
        )

        val retriever = Retriever(
            memoryDao = database.memoryDao(),
            ftsDao = database.memoryFtsDao(),
            vectorDao = database.vectorDao(),
            embedder = embedder,
            config = config
        )

        val contextBuilder = ContextBuilder(
            memoryDao = database.memoryDao(),
            retriever = retriever
        )

        return MemoryEngineImpl(
            context = context.applicationContext,
            database = database,
            ingestor = ingestor,
            retriever = retriever,
            contextBuilder = contextBuilder
        )
    }

    /**
     * Get or create singleton SourcesEngine
     */
    fun provideSourcesEngine(context: Context): SourcesEngine {
        return sourcesEngine ?: synchronized(this) {
            sourcesEngine ?: buildSourcesEngine(context).also {
                sourcesEngine = it
            }
        }
    }

    /**
     * Build SourcesEngine with all dependencies
     */
    private fun buildSourcesEngine(context: Context): SourcesEngine {
        val config = SourcesConfig.DEFAULT
        val database = SourcesDatabase.getInstance(context)
        val pdfIngest = PdfIngest(context, config)
        val textIngest = com.example.innovexia.memory.Mind.sources.indexer.TextIngest(context, config)
        // val documentIngest = com.example.innovexia.memory.Mind.sources.indexer.DocumentIngest(context, config)  // DISABLED: POI requires API 26+

        return SourcesEngineImpl(
            context = context.applicationContext,
            database = database,
            pdfIngest = pdfIngest,
            textIngest = textIngest,
            // documentIngest = documentIngest,  // DISABLED: POI requires API 26+
            config = config
        )
    }

    /**
     * Provide embedder
     * Set USE_REAL_EMBEDDINGS = true to use Gemini embeddings (requires API key)
     * Set USE_REAL_EMBEDDINGS = false to use FakeEmbedder (for testing)
     */
    fun provideEmbedder(dim: Int, context: Context? = null): Embedder {
        val useRealEmbeddings = true // Enable real Gemini embeddings

        return if (useRealEmbeddings && context != null) {
            // Get API key from BuildConfig
            val apiKey = try {
                com.example.innovexia.BuildConfig.GEMINI_API_KEY
            } catch (e: Exception) {
                ""
            }

            if (apiKey.isNotEmpty()) {
                android.util.Log.i("MindModule", "Using GeminiEmbedder (768-dim)")
                GeminiEmbedder(apiKey)
            } else {
                android.util.Log.w("MindModule", "No API key found, using FakeEmbedder")
                FakeEmbedder(dim)
            }
        } else {
            android.util.Log.w("MindModule", "Context is null or real embeddings disabled, using FakeEmbedder")
            FakeEmbedder(dim)
        }
    }

    /**
     * Reset singletons (for testing)
     */
    fun reset() {
        memoryEngine = null
        sourcesEngine = null
        MemoryDatabase.destroyInstance()
        SourcesDatabase.destroyInstance()
    }
}
