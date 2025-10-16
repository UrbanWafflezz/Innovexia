package com.example.innovexia.core.sync

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cloud_sync_settings")

/**
 * Manages cloud sync settings using DataStore.
 * Tracks sync status, last sync time, and statistics.
 */
class CloudSyncSettings(private val context: Context) {
    private val CLOUD_SYNC_ENABLED = booleanPreferencesKey("cloud_sync_enabled")
    private val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
    private val LAST_SYNC_CHAT_COUNT = intPreferencesKey("last_sync_chat_count")
    private val LAST_SYNC_MESSAGE_COUNT = intPreferencesKey("last_sync_message_count")

    /**
     * Flow of the cloud sync enabled state.
     */
    val cloudSyncEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[CLOUD_SYNC_ENABLED] ?: false
    }

    /**
     * Flow of the last sync timestamp.
     */
    val lastSyncTime: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[LAST_SYNC_TIME] ?: 0L
    }

    /**
     * Flow of the last sync chat count.
     */
    val lastSyncChatCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[LAST_SYNC_CHAT_COUNT] ?: 0
    }

    /**
     * Flow of the last sync message count.
     */
    val lastSyncMessageCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[LAST_SYNC_MESSAGE_COUNT] ?: 0
    }

    /**
     * Sets the cloud sync enabled state.
     */
    suspend fun set(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[CLOUD_SYNC_ENABLED] = enabled
        }
    }

    /**
     * Gets the current cloud sync enabled state (suspending).
     */
    suspend fun get(): Boolean {
        return context.dataStore.data.map { prefs ->
            prefs[CLOUD_SYNC_ENABLED] ?: false
        }.first()
    }

    /**
     * Updates the last sync timestamp and statistics.
     */
    suspend fun updateLastSync(chatCount: Int, messageCount: Int) {
        context.dataStore.edit { prefs ->
            prefs[LAST_SYNC_TIME] = System.currentTimeMillis()
            prefs[LAST_SYNC_CHAT_COUNT] = chatCount
            prefs[LAST_SYNC_MESSAGE_COUNT] = messageCount
        }
    }
}

// Extension to get first value from Flow
private suspend fun <T> Flow<T>.first(): T {
    var result: T? = null
    collect {
        result = it
        return@collect
    }
    return result!!
}
