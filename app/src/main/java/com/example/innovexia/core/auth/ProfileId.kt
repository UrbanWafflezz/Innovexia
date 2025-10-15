package com.example.innovexia.core.auth

import com.google.firebase.Firebase
import com.google.firebase.auth.auth

/**
 * Represents the current profile scope: Guest or authenticated User.
 * Used to isolate local storage between guest and user accounts.
 */
sealed class ProfileId {
    /**
     * Guest mode - data is stored under "guest" ownerId.
     */
    data object Guest : ProfileId()

    /**
     * Authenticated user - data is stored under the user's Firebase UID.
     */
    data class User(val uid: String) : ProfileId()

    /**
     * Returns the owner ID string for database scoping.
     */
    fun toOwnerId(): String = when (this) {
        is Guest -> GUEST_OWNER_ID
        is User -> uid
    }

    companion object {
        const val GUEST_OWNER_ID = "guest"

        /**
         * Get the current profile ID based on Firebase auth state.
         */
        fun current(): ProfileId {
            val uid = Firebase.auth.currentUser?.uid
            return if (uid != null) User(uid) else Guest
        }
    }
}
