package com.example.innovexia.ui.sheets.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.innovexia.core.auth.FirebaseAuthManager
import com.example.innovexia.core.auth.ProfileId
import com.example.innovexia.core.storage.AvatarStorage
import com.example.innovexia.core.sync.CloudSyncSettings
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Profile tab enum
 */
enum class ProfileTab(val title: String) {
    Profile("Profile"),
    Billing("Billing"),
    Security("Security"),
    CloudSync("Cloud Sync")
}

class ProfileViewModel(private val context: Context) : ViewModel() {
    private val cloudSettings = CloudSyncSettings(context)

    private val _tab = MutableStateFlow(ProfileTab.Profile)
    val tab: StateFlow<ProfileTab> = _tab.asStateFlow()

    private val _user = MutableStateFlow<FirebaseUser?>(null)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    private val _success = MutableSharedFlow<String>()
    val success: SharedFlow<String> = _success.asSharedFlow()

    private val _avatarUri = MutableStateFlow<Uri?>(null)
    val avatarUri: StateFlow<Uri?> = _avatarUri.asStateFlow()

    val cloudSyncEnabled = cloudSettings.cloudSyncEnabled

    fun setTab(t: ProfileTab) {
        _tab.value = t
    }

    /**
     * Check if currently in guest mode.
     */
    fun isGuestMode(): Boolean = ProfileId.current() is ProfileId.Guest

    init {
        refreshUser()
    }

    fun refreshUser() {
        _user.value = FirebaseAuthManager.currentUser()
        _user.value?.uid?.let { uid ->
            _avatarUri.value = AvatarStorage.getAvatarUri(context, uid)
        }
    }

    fun updateDisplayName(name: String) = viewModelScope.launch {
        _busy.value = true
        FirebaseAuthManager.updateDisplayName(name)
            .onSuccess {
                refreshUser()
                _success.emit("Profile updated successfully")
            }
            .onFailure { e ->
                _error.emit(e.message ?: "Update failed")
            }
        _busy.value = false
    }

    fun uploadAvatar(uri: Uri) = viewModelScope.launch {
        _busy.value = true
        val uid = FirebaseAuthManager.currentUser()?.uid
        if (uid == null) {
            _error.emit("No user signed in")
            _busy.value = false
            return@launch
        }

        try {
            val savedUri = AvatarStorage.upload(context, uid, uri)
            _avatarUri.value = savedUri
            _success.emit("Avatar updated successfully")
        } catch (e: Exception) {
            _error.emit(e.message ?: "Failed to upload avatar")
        }
        _busy.value = false
    }

    fun setCloudSync(enabled: Boolean) = viewModelScope.launch {
        // Prevent enabling Cloud Sync in guest mode
        if (enabled && isGuestMode()) {
            _error.emit("Sign in to enable Cloud Sync")
            return@launch
        }

        cloudSettings.set(enabled)
        if (enabled && FirebaseAuthManager.currentUser() != null) {
            // TODO: Trigger initial cloud sync if CloudSyncEngine is wired
            _success.emit("Cloud sync enabled")
        } else if (!enabled) {
            _success.emit("Cloud sync disabled")
        } else {
            _error.emit("Please sign in to enable cloud sync")
        }
    }

    fun sendPasswordReset() = viewModelScope.launch {
        _busy.value = true
        val email = FirebaseAuthManager.currentUser()?.email
        if (email == null) {
            _error.emit("No email associated with this account")
            _busy.value = false
            return@launch
        }

        FirebaseAuthManager.sendReset(email)
            .onSuccess {
                _success.emit("Password reset email sent to $email")
            }
            .onFailure { e ->
                _error.emit(e.message ?: "Failed to send reset email")
            }
        _busy.value = false
    }

    fun changePassword(newPassword: String) = viewModelScope.launch {
        _busy.value = true
        FirebaseAuthManager.updatePassword(newPassword)
            .onSuccess {
                _success.emit("Password changed successfully")
            }
            .onFailure { e ->
                if (e.message?.contains("recent") == true) {
                    _error.emit("Please sign in again to change your password")
                } else {
                    _error.emit(e.message ?: "Failed to change password")
                }
            }
        _busy.value = false
    }

    fun signOut() {
        FirebaseAuthManager.signOut()
        refreshUser()
    }

    fun deleteAccount(onSuccess: () -> Unit) = viewModelScope.launch {
        _busy.value = true
        FirebaseAuthManager.deleteAccount()
            .onSuccess {
                _success.emit("Account deleted successfully")
                onSuccess()
            }
            .onFailure { e ->
                if (e.message?.contains("recent") == true) {
                    _error.emit("Please sign in again to delete your account")
                } else {
                    _error.emit(e.message ?: "Failed to delete account")
                }
            }
        _busy.value = false
    }
}
