package com.example.innovexia.subscriptions.mock

import android.content.Context
import com.example.innovexia.memory.Mind.store.MemoryDatabase
import com.example.innovexia.memory.Mind.sources.store.SourcesDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Repository for aggregating real usage data across all personas
 * Memory and sources data stays local (Room), but counts are aggregated for display
 */
class UsageDataRepository(
    private val context: Context,
    private val ownerId: String
) {

    private val memoryDatabase: MemoryDatabase by lazy {
        MemoryDatabase.getInstance(context)
    }

    private val sourcesDatabase: SourcesDatabase by lazy {
        SourcesDatabase.getInstance(context)
    }

    /**
     * Observe total memory count across all personas for this user
     * Memory data stays local - we only count it
     */
    fun observeTotalMemoryCount(): Flow<Int> {
        return memoryDatabase.memoryDao().observeTotalCount()
    }

    /**
     * Observe total sources count across all personas for this user
     * Sources data stays local - we only count it
     */
    fun observeTotalSourcesCount(): Flow<Int> {
        return sourcesDatabase.sourceDao().observeTotalCount()
    }

    /**
     * Observe combined usage data
     */
    fun observeUsageData(): Flow<UsageData> {
        return combine(
            observeTotalMemoryCount(),
            observeTotalSourcesCount()
        ) { memoryCount, sourcesCount ->
            UsageData(
                memoryEntriesCount = memoryCount,
                sourcesCount = sourcesCount
            )
        }
    }

    /**
     * Get current memory count (suspend version)
     */
    suspend fun getTotalMemoryCount(): Int {
        return memoryDatabase.memoryDao().getTotalCount()
    }

    /**
     * Get current sources count (suspend version)
     */
    suspend fun getTotalSourcesCount(): Int {
        return sourcesDatabase.sourceDao().getTotalCount()
    }
}

/**
 * Real usage data aggregated from local databases
 */
data class UsageData(
    val memoryEntriesCount: Int = 0,
    val sourcesCount: Int = 0
)
