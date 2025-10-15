package com.example.innovexia.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * DataStore-based user preferences for consent and settings.
 *
 * consentedSaveHistory:
 * - null = not asked yet
 * - true = user allowed local storage
 * - false = user declined (ephemeral mode)
 */
class UserPreferences(private val context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

    companion object {
        val CONSENTED_SAVE_HISTORY = booleanPreferencesKey("consented_save_history")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val MAX_OUTPUT_TOKENS = intPreferencesKey("max_output_tokens")
        val SAFETY_LEVEL = stringPreferencesKey("safety_level")
        val GROUNDING_ENABLED = booleanPreferencesKey("grounding_enabled")
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
        val SAVED_EMAIL = stringPreferencesKey("saved_email")

        // Timezone preferences
        val USER_TIMEZONE_ID = stringPreferencesKey("user_timezone_id")

        // Cloud sync preferences
        val HIDE_CLOUD_RESTORE_BUTTON = booleanPreferencesKey("hide_cloud_restore_button")
        val ALWAYS_SHOW_RESTORE_BUTTON = booleanPreferencesKey("always_show_restore_button")
        val HAS_SEEN_RESTORE_PROMPT = booleanPreferencesKey("has_seen_restore_prompt")
        val CLOUD_DELETE_ENABLED = booleanPreferencesKey("cloud_delete_enabled")
    }

    /**
     * Flow of consent status. Emits null if never asked.
     */
    val consentedSaveHistory: Flow<Boolean?> = context.dataStore.data
        .map { preferences ->
            val value = if (preferences.contains(CONSENTED_SAVE_HISTORY)) {
                preferences[CONSENTED_SAVE_HISTORY]
            } else {
                null
            }
            android.util.Log.d("UserPreferences", "consentedSaveHistory value: $value")
            value
        }

    /**
     * Set user consent for saving chat history locally.
     */
    suspend fun setConsentSaveHistory(consented: Boolean) {
        android.util.Log.d("UserPreferences", "setConsentSaveHistory called with: $consented")
        context.dataStore.edit { preferences ->
            preferences[CONSENTED_SAVE_HISTORY] = consented
            android.util.Log.d("UserPreferences", "Consent saved successfully: $consented")
        }
    }

    /**
     * Flow that emits whenever Firebase auth state changes
     */
    private val authStateFlow: Flow<Boolean> = callbackFlow {
        val auth = Firebase.auth
        val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener {
            trySend(it.currentUser != null)
        }
        auth.addAuthStateListener(listener)

        // Emit initial state
        trySend(auth.currentUser != null)

        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Flow of selected model ID
     * - Guests (not logged in): Always use Gemini 2.5 Flash Lite (cannot change)
     * - Authenticated users: Default to Gemini 2.5 Flash (can change)
     *
     * This Flow is reactive to auth state changes, so logging in/out will immediately
     * switch the model without needing to restart the app.
     */
    val selectedModel: Flow<String> = combine(
        authStateFlow,
        context.dataStore.data
    ) { isAuthenticated, preferences ->
        if (isAuthenticated) {
            // Authenticated users: use their preference or default to Gemini 2.5 Flash
            preferences[SELECTED_MODEL] ?: "gemini-2.5-flash"
        } else {
            // Guests ALWAYS use Gemini 2.5 Flash Lite, regardless of stored preference
            "gemini-2.5-flash-lite"
        }
    }

    /**
     * Save selected model
     * NOTE: This only saves for authenticated users. Guests always use Gemini 2.5 Flash Lite.
     */
    suspend fun setSelectedModel(modelId: String) {
        // Only save if user is authenticated
        val isGuest = com.google.firebase.Firebase.auth.currentUser == null
        if (!isGuest) {
            context.dataStore.edit { preferences ->
                preferences[SELECTED_MODEL] = modelId
            }
        }
    }

    /**
     * Flow of AI temperature setting
     */
    val temperature: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[TEMPERATURE] ?: 0.7f // Default temperature
        }

    /**
     * Save temperature
     */
    suspend fun setTemperature(temp: Float) {
        context.dataStore.edit { preferences ->
            preferences[TEMPERATURE] = temp
        }
    }

    /**
     * Flow of max output tokens
     */
    val maxOutputTokens: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[MAX_OUTPUT_TOKENS] ?: 2048 // Default max tokens
        }

    /**
     * Save max output tokens
     */
    suspend fun setMaxOutputTokens(tokens: Int) {
        context.dataStore.edit { preferences ->
            preferences[MAX_OUTPUT_TOKENS] = tokens
        }
    }

    /**
     * Flow of safety level
     */
    val safetyLevel: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[SAFETY_LEVEL] ?: "Standard" // Default safety level
        }

    /**
     * Save safety level
     */
    suspend fun setSafetyLevel(level: String) {
        context.dataStore.edit { preferences ->
            preferences[SAFETY_LEVEL] = level
        }
    }

    /**
     * Flow of grounding enabled setting
     */
    val groundingEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[GROUNDING_ENABLED] ?: false // Default: disabled
        }

    /**
     * Save grounding enabled setting
     */
    suspend fun setGroundingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GROUNDING_ENABLED] = enabled
        }
    }

    /**
     * Flow of user's timezone ID (e.g., "America/New_York", "Europe/London")
     * Returns system default timezone if not set
     */
    val userTimezoneId: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[USER_TIMEZONE_ID] ?: java.time.ZoneId.systemDefault().id
        }

    /**
     * Save user's timezone ID
     * This should be automatically updated when timezone changes are detected
     */
    suspend fun setUserTimezoneId(timezoneId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_TIMEZONE_ID] = timezoneId
            android.util.Log.d("UserPreferences", "Timezone updated: $timezoneId")
        }
    }

    /**
     * Flow of remember me setting
     */
    val rememberMe: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[REMEMBER_ME] ?: false
        }

    /**
     * Save remember me setting
     */
    suspend fun setRememberMe(remember: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[REMEMBER_ME] = remember
        }
    }

    /**
     * Flow of saved email (for remember me feature)
     */
    val savedEmail: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[SAVED_EMAIL]
        }

    /**
     * Save email for remember me feature
     */
    suspend fun setSavedEmail(email: String?) {
        context.dataStore.edit { preferences ->
            if (email != null) {
                preferences[SAVED_EMAIL] = email
            } else {
                preferences.remove(SAVED_EMAIL)
            }
        }
    }

    /**
     * Clear all user preferences (for testing or factory reset).
     */
    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    // ========== Cloud Sync Preferences ==========

    /**
     * Flow indicating if user has hidden the cloud restore button from side menu
     */
    val hideCloudRestoreButton: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[HIDE_CLOUD_RESTORE_BUTTON] ?: false
        }

    /**
     * Set whether to hide the cloud restore button
     */
    suspend fun setHideCloudRestoreButton(hide: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HIDE_CLOUD_RESTORE_BUTTON] = hide
        }
    }

    /**
     * Flow indicating if restore button should always be shown (even when no cloud chats)
     */
    val alwaysShowRestoreButton: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ALWAYS_SHOW_RESTORE_BUTTON] ?: false
        }

    /**
     * Set whether to always show the restore button
     */
    suspend fun setAlwaysShowRestoreButton(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ALWAYS_SHOW_RESTORE_BUTTON] = show
        }
    }

    /**
     * Flow indicating if user has seen the restore prompt (on fresh install)
     */
    val hasSeenRestorePrompt: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[HAS_SEEN_RESTORE_PROMPT] ?: false
        }

    /**
     * Mark that user has seen the restore prompt
     */
    suspend fun setHasSeenRestorePrompt(seen: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_SEEN_RESTORE_PROMPT] = seen
        }
    }

    /**
     * Flow indicating if cloud delete is enabled (delete from Firebase when deleting locally)
     */
    val cloudDeleteEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[CLOUD_DELETE_ENABLED] ?: true // Default: enabled
        }

    /**
     * Set whether to delete from cloud when deleting locally
     */
    suspend fun setCloudDeleteEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CLOUD_DELETE_ENABLED] = enabled
        }
    }
}
