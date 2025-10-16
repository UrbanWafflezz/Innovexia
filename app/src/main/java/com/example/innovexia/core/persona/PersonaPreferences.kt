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
            // Return stored persona ID, or default to Inno if not set
            prefs[key] ?: InnoPersonaDefaults.INNO_PERSONA_ID
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
            activeId == InnoPersonaDefaults.INNO_PERSONA_ID
        }
    }
}
