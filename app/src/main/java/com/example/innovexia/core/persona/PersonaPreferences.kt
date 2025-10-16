package com.example.innovexia.core.persona

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.personaDataStore: DataStore<Preferences> by preferencesDataStore(name = "persona_prefs")

class PersonaPreferences(
    private val context: Context
) {
    private val ACTIVE_PERSONA_KEY = stringPreferencesKey("active_persona_id")

    /**
     * Get the active persona ID for the current user/guest.
     * If no persona is explicitly set, defaults to Inno (the default AI companion).
     */
    fun getActivePersonaId(ownerId: String): Flow<String?> {
        val key = stringPreferencesKey("active_persona_${ownerId}")
        return context.personaDataStore.data.map { prefs ->
            // Return stored persona ID, or default to per-user Inno if not set
            prefs[key] ?: InnoPersonaDefaults.getInnoPersonaId(ownerId)
        }
    }

    /**
     * Get the active persona ID without falling back to Inno.
     * Returns null if no persona is explicitly set.
     */
    fun getActivePersonaIdRaw(ownerId: String): Flow<String?> {
        val key = stringPreferencesKey("active_persona_${ownerId}")
        return context.personaDataStore.data.map { prefs ->
            prefs[key]
        }
    }

    /**
     * Set the active persona ID for the current user/guest.
     * Pass null to clear the selection (will default to Inno).
     */
    suspend fun setActivePersonaId(ownerId: String, personaId: String?) {
        val key = stringPreferencesKey("active_persona_${ownerId}")
        context.personaDataStore.edit { prefs ->
            if (personaId != null) {
                prefs[key] = personaId
            } else {
                prefs.remove(key)
            }
        }
    }

    /**
     * Check if Inno is the active persona (either explicitly set or by default).
     */
    fun isInnoActive(ownerId: String): Flow<Boolean> {
        return getActivePersonaId(ownerId).map { activeId ->
            activeId == InnoPersonaDefaults.getInnoPersonaId(ownerId)
        }
    }

    /**
     * Clear the active persona selection for a specific user/guest.
     * Used when signing out to reset persona state.
     */
    suspend fun clearForOwner(ownerId: String) {
        val key = stringPreferencesKey("active_persona_${ownerId}")
        context.personaDataStore.edit { prefs ->
            prefs.remove(key)
        }
    }

    /**
     * Clear all persona preferences (for complete reset).
     * WARNING: This clears preferences for ALL users.
     */
    suspend fun clearAll() {
        context.personaDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
