package com.example.innovexia.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.innovexia.InnovexiaApplication
import com.example.innovexia.core.auth.FirebaseAuthManager
import com.example.innovexia.core.session.SessionRecorder
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    private val _error = MutableSharedFlow<String?>()
    val error = _error.asSharedFlow()

    private val _signedIn = MutableStateFlow(FirebaseAuthManager.currentUser() != null)
    val signedIn = _signedIn.asStateFlow()

    // Merge guest chats dialog state
    private val _showMergeDialog = MutableStateFlow(false)
    val showMergeDialog = _showMergeDialog.asStateFlow()

    private val _guestChatCount = MutableStateFlow(0)
    val guestChatCount = _guestChatCount.asStateFlow()

    private val repository by lazy {
        (getApplication() as InnovexiaApplication).chatRepository
    }

    private val userPreferences by lazy {
        (getApplication() as InnovexiaApplication).userPreferences
    }

    // Remember me state
    val rememberMe = userPreferences.rememberMe
    val savedEmail = userPreferences.savedEmail

    fun refresh() {
        _signedIn.value = FirebaseAuthManager.currentUser() != null
    }

    fun signIn(email: String, password: String, rememberMe: Boolean = false) = launchBusy {
        val res = FirebaseAuthManager.signIn(email, password)
        if (res.isSuccess) {
            _signedIn.value = true

            // Save remember me preference
            userPreferences.setRememberMe(rememberMe)
            if (rememberMe) {
                userPreferences.setSavedEmail(email)
            } else {
                userPreferences.setSavedEmail(null)
            }

            // Record session on successful sign-in
            SessionRecorder.upsertCurrentSession(getApplication())
            // Notify profile repository of auth change
            repository.getProfileRepository()?.onAuthChanged()

            // Re-seed Inno persona for new user
            seedInnoPersonaForCurrentUser()

            // Check for guest chats to merge
            checkForGuestChats()
        } else {
            _error.emit(mapError(res.exceptionOrNull()))
        }
    }

    fun signUp(username: String, email: String, password: String) = launchBusy {
        val res = FirebaseAuthManager.signUp(email, password, username)
        if (res.isSuccess) {
            _signedIn.value = true
            // Record session on successful sign-up
            SessionRecorder.upsertCurrentSession(getApplication())
            // Notify profile repository of auth change
            repository.getProfileRepository()?.onAuthChanged()

            // Re-seed Inno persona for new user
            seedInnoPersonaForCurrentUser()

            // Check for guest chats to merge
            checkForGuestChats()
        } else {
            _error.emit(mapError(res.exceptionOrNull()))
        }
    }

    fun sendReset(email: String) = launchBusy {
        val res = FirebaseAuthManager.sendReset(email)
        if (res.isSuccess) {
            _error.emit("Reset email sent.")
        } else {
            _error.emit(mapError(res.exceptionOrNull()))
        }
    }

    fun signOut() = viewModelScope.launch {
        // Get current user ID before signing out
        val currentOwnerId = FirebaseAuthManager.currentUser()?.uid
            ?: com.example.innovexia.core.auth.ProfileId.GUEST_OWNER_ID

        android.util.Log.d("AuthViewModel", "Signing out user: $currentOwnerId")

        // Clear persona and memory state for current user
        try {
            val app = getApplication() as InnovexiaApplication

            // Clear active persona selection
            val personaPrefs = com.example.innovexia.core.persona.PersonaPreferences(getApplication())
            personaPrefs.clearForOwner(currentOwnerId)
            android.util.Log.d("AuthViewModel", "Cleared persona preferences for $currentOwnerId")

            // Clear memory preferences
            val memoryEngine = com.example.innovexia.memory.Mind.di.MindModule.provideMemoryEngine(getApplication())
            if (memoryEngine is com.example.innovexia.memory.Mind.MemoryEngineImpl) {
                memoryEngine.clearAllPreferencesForOwner(currentOwnerId)
                android.util.Log.d("AuthViewModel", "Cleared memory preferences for $currentOwnerId")
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "Error clearing user data on logout", e)
            // Continue with logout even if clearing fails
        }

        // Sign out from Firebase
        FirebaseAuthManager.signOut()
        _signedIn.value = false

        // Clear saved email if remember me is not enabled
        val rememberEnabled = userPreferences.rememberMe.first()
        if (!rememberEnabled) {
            userPreferences.setSavedEmail(null)
        }

        // Notify profile repository of auth change
        repository.getProfileRepository()?.onAuthChanged()

        android.util.Log.d("AuthViewModel", "Sign out completed")
    }

    /**
     * Seed Inno persona for the currently signed-in user.
     * This ensures Inno is created immediately after login/signup.
     */
    private fun seedInnoPersonaForCurrentUser() = viewModelScope.launch {
        try {
            val app = getApplication() as InnovexiaApplication
            val ownerId = FirebaseAuthManager.currentUser()?.uid
                ?: com.example.innovexia.core.auth.ProfileId.GUEST_OWNER_ID

            android.util.Log.d("AuthViewModel", "Seeding Inno persona for owner: $ownerId")

            // Ensure Inno exists and is set as default
            app.personaRepository.ensureInnoIsDefault(ownerId)

            android.util.Log.d("AuthViewModel", "Inno persona seeded successfully for owner: $ownerId")
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "Failed to seed Inno persona on login", e)
            // Don't fail login if Inno creation fails
        }
    }

    /**
     * Check if there are guest chats available to merge.
     * Shows merge dialog if chats exist.
     */
    private fun checkForGuestChats() = viewModelScope.launch {
        val profileRepo = repository.getProfileRepository() ?: return@launch
        if (profileRepo.hasGuestChats()) {
            val count = (getApplication() as InnovexiaApplication).database.chatDao().countGuestChats()
            _guestChatCount.value = count
            _showMergeDialog.value = true
        }
    }

    /**
     * Merge guest chats into the current user's account.
     */
    fun mergeGuestChats() = viewModelScope.launch {
        try {
            val profileRepo = repository.getProfileRepository() ?: return@launch
            val count = profileRepo.mergeGuestChatsIntoCurrentUser()
            _showMergeDialog.value = false
            _error.emit("$count chat${if (count > 1) "s" else ""} imported successfully.")
        } catch (e: Exception) {
            _error.emit("Failed to import chats: ${e.localizedMessage}")
        }
    }

    /**
     * Dismiss the merge dialog without merging.
     */
    fun dismissMergeDialog() {
        _showMergeDialog.value = false
    }

    /**
     * Manually trigger merge dialog check (for Settings "Import Guest chats" option).
     */
    fun checkAndShowMergeDialog() = viewModelScope.launch {
        checkForGuestChats()
    }

    private fun mapError(t: Throwable?): String = when (t) {
        is FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
        is FirebaseAuthUserCollisionException -> "Email already in use."
        is FirebaseAuthWeakPasswordException -> "Password is too weak."
        else -> t?.localizedMessage ?: "Something went wrong."
    }

    private fun launchBusy(block: suspend () -> Unit) = viewModelScope.launch {
        if (_busy.value) return@launch
        _busy.value = true
        try {
            block()
        } finally {
            _busy.value = false
        }
    }
}
