package com.example.innovexia.ui.sheets.profile.tabs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.innovexia.data.local.AppDatabase
import com.example.innovexia.data.local.entities.SessionEntity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SecurityViewModel(
    private val app: Application
) : AndroidViewModel(app) {

    private val sessionDao = AppDatabase.getInstance(app).sessionDao()

    val busy = MutableStateFlow(false)
    val message = MutableSharedFlow<String>(replay = 0)
    val sessions: StateFlow<List<SessionItem>> = sessionDao.getLast30Sessions()
        .map { list -> list.map { it.toSessionItem() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Password / Reset / Sign out / Delete ---

    fun changePassword(
        newPassword: String,
        currentEmail: String,
        currentPassword: String?
    ) = launchBusy {
        val user = Firebase.auth.currentUser
        if (user == null) {
            message.emit("Not signed in.")
            return@launchBusy
        }

        // Reauth if current password provided
        if (currentPassword != null && currentEmail.isNotEmpty()) {
            val cred = EmailAuthProvider.getCredential(currentEmail, currentPassword)
            runCatching { user.reauthenticate(cred).await() }.onFailure {
                message.emit("Reauthentication failed: ${it.localizedMessage}")
                return@launchBusy
            }
        }

        runCatching { user.updatePassword(newPassword).await() }
            .onSuccess { message.emit("Password updated successfully.") }
            .onFailure {
                val errorMsg = when {
                    it.message?.contains("requires-recent-login") == true ->
                        "Please sign in again to change your password."
                    else -> it.localizedMessage ?: "Failed to update password."
                }
                message.emit(errorMsg)
            }
    }

    fun sendPasswordReset() = viewModelScope.launch {
        val email = Firebase.auth.currentUser?.email
        if (email.isNullOrEmpty()) {
            message.emit("No email associated with this account.")
            return@launch
        }

        runCatching { Firebase.auth.sendPasswordResetEmail(email).await() }
            .onSuccess { message.emit("Password reset email sent to $email") }
            .onFailure { message.emit(it.localizedMessage ?: "Failed to send reset email.") }
    }

    fun signOut(onSignedOut: () -> Unit) = launchBusy {
        // Clear local sessions
        sessionDao.deleteAllSessions()
        message.emit("Signed out successfully.")
        // Firebase sign-out is handled by AuthViewModel via the callback
        onSignedOut()
    }

    fun deleteAccount(
        currentEmail: String,
        currentPassword: String?
    ) = launchBusy {
        val user = Firebase.auth.currentUser
        if (user == null) {
            message.emit("Not signed in.")
            return@launchBusy
        }

        // Reauth if current password provided
        if (currentPassword != null && currentEmail.isNotEmpty()) {
            val cred = EmailAuthProvider.getCredential(currentEmail, currentPassword)
            runCatching { user.reauthenticate(cred).await() }.onFailure {
                message.emit("Reauthentication failed: ${it.localizedMessage}")
                return@launchBusy
            }
        }

        runCatching {
            // Delete all local data first
            val db = AppDatabase.getInstance(app)
            db.clearAllTables()
            // Delete Firebase account
            user.delete().await()
        }
            .onSuccess { message.emit("Account deleted successfully.") }
            .onFailure {
                val errorMsg = when {
                    it.message?.contains("requires-recent-login") == true ->
                        "Please sign in again to delete your account."
                    else -> it.localizedMessage ?: "Failed to delete account."
                }
                message.emit(errorMsg)
            }
    }

    // --- Sessions ---

    fun loadSessions() {
        // Sessions are automatically loaded via Flow from DAO
        // This method exists for manual refresh if needed
    }

    // --- Helpers ---

    private fun launchBusy(block: suspend () -> Unit) = viewModelScope.launch {
        if (busy.value) return@launch
        busy.value = true
        try {
            block()
        } finally {
            busy.value = false
        }
    }
}

data class SessionItem(
    val id: String,
    val device: String,
    val createdAt: Long,
    val lastActiveAt: Long,
    val city: String?,
    val region: String?,
    val approxLat: Double?,
    val approxLon: Double?,
    val revoked: Boolean
)

private fun SessionEntity.toSessionItem() = SessionItem(
    id = id,
    device = model,
    createdAt = createdAt,
    lastActiveAt = lastActiveAt,
    city = city,
    region = region,
    approxLat = approxLat,
    approxLon = approxLon,
    revoked = revoked
)
