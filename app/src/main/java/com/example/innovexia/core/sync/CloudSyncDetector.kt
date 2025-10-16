package com.example.innovexia.core.sync

import android.content.Context
import android.util.Log
import com.example.innovexia.core.auth.FirebaseAuthManager
import com.example.innovexia.data.local.AppDatabase
import com.example.innovexia.data.preferences.UserPreferences
import kotlinx.coroutines.flow.first

/**
 * Detects scenarios where user should be prompted to restore from cloud.
 * Used on app launch to determine if user has cloud backups they might want to restore.
 */
object CloudSyncDetector {

    private const val TAG = "CloudSyncDetector"

    /**
     * Check if we should prompt the user to restore from cloud.
     *
     * Conditions:
     * - User is signed in (not guest)
     * - User hasn't seen the prompt before
     * - Local database is empty or nearly empty (fresh install or data clear)
     * - Firebase has chats available for this user
     *
     * @return true if we should show restore prompt
     */
    suspend fun shouldPromptRestore(context: Context, userPreferences: UserPreferences): Boolean {
        try {
            // Check if user has already seen and dismissed the prompt
            if (userPreferences.hasSeenRestorePrompt.first()) {
                Log.d(TAG, "User has already seen restore prompt - skipping")
                return false
            }

            // Check if user is signed in
            val user = FirebaseAuthManager.currentUser()
            if (user == null) {
                Log.d(TAG, "User not signed in - skipping restore prompt")
                return false
            }

            // Check if local database is empty (fresh install scenario)
            val db = AppDatabase.getInstance(context)
            val localChatCount = db.chatDao().getAllSync().count()

            if (localChatCount > 5) {
                // User has substantial local data - not a fresh install
                Log.d(TAG, "Local has $localChatCount chats - not a fresh install, skipping prompt")
                return false
            }

            // Check if Firebase has chats for this user
            val cloudChatCount = getCloudChatCount(user.uid)

            if (cloudChatCount == 0) {
                Log.d(TAG, "No cloud chats found - skipping prompt")
                return false
            }

            Log.d(TAG, "Should prompt restore: localChats=$localChatCount, cloudChats=$cloudChatCount")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Error checking if should prompt restore", e)
            return false
        }
    }

    /**
     * Get the count of chats available in cloud for the current user.
     *
     * @param uid User ID to fetch chats for
     * @return Number of chats in cloud (including deleted ones)
     */
    suspend fun getCloudChatCount(uid: String): Int {
        return try {
            val engine = CloudSyncEngine()
            val chatIds = engine.fetchChatIds(uid)
            Log.d(TAG, "Found ${chatIds.size} chats in cloud for user $uid")
            chatIds.size
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cloud chat count", e)
            0
        }
    }

    /**
     * Get the count of active (non-deleted) chats in cloud.
     *
     * @param uid User ID to fetch chats for
     * @return Number of active chats in cloud
     */
    suspend fun getActiveCloudChatCount(uid: String): Int {
        return try {
            val engine = CloudSyncEngine()
            val chats = engine.fetchCloudChats(uid, includeDeleted = false)
            Log.d(TAG, "Found ${chats.size} active chats in cloud for user $uid")
            chats.size
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get active cloud chat count", e)
            0
        }
    }

    /**
     * Mark that the user has seen the restore prompt.
     * This prevents the prompt from showing again.
     */
    suspend fun markRestorePromptSeen(userPreferences: UserPreferences) {
        userPreferences.setHasSeenRestorePrompt(true)
        Log.d(TAG, "Marked restore prompt as seen")
    }
}
