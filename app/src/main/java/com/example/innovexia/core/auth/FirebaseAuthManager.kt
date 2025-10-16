package com.example.innovexia.core.auth

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.tasks.await

object FirebaseAuthManager {
    private val auth: FirebaseAuth by lazy { Firebase.auth }

    fun currentUser(): FirebaseUser? = auth.currentUser

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> =
        runCatching {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
            auth.currentUser ?: error("Signed in but no user.")
        }

    suspend fun signUp(email: String, password: String, displayName: String?): Result<FirebaseUser> =
        runCatching {
            auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = auth.currentUser ?: error("Created but no user.")
            if (!displayName.isNullOrBlank()) {
                val req = userProfileChangeRequest { this.displayName = displayName.trim() }
                user.updateProfile(req).await()
            }
            user
        }

    suspend fun sendReset(email: String): Result<Unit> =
        runCatching { auth.sendPasswordResetEmail(email.trim()).await() }

    fun signOut() = auth.signOut()

    /**
     * Updates the current user's display name.
     */
    suspend fun updateDisplayName(name: String): Result<Unit> =
        runCatching {
            val user = auth.currentUser ?: error("No user signed in")
            val req = userProfileChangeRequest { displayName = name.trim() }
            user.updateProfile(req).await()
        }

    /**
     * Updates the current user's photo URL.
     */
    suspend fun updatePhoto(url: Uri): Result<Unit> =
        runCatching {
            val user = auth.currentUser ?: error("No user signed in")
            val req = userProfileChangeRequest { photoUri = url }
            user.updateProfile(req).await()
        }

    /**
     * Updates the current user's password.
     * Note: This requires recent authentication. If it fails, the user may need to sign in again.
     */
    suspend fun updatePassword(newPassword: String): Result<Unit> =
        runCatching {
            val user = auth.currentUser ?: error("No user signed in")
            user.updatePassword(newPassword).await()
        }

    /**
     * Deletes the current user's account.
     * Note: This requires recent authentication. If it fails, the user may need to sign in again.
     */
    suspend fun deleteAccount(): Result<Unit> =
        runCatching {
            val user = auth.currentUser ?: error("No user signed in")
            user.delete().await()
        }
}
