package com.example.innovexia.core.storage

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Handles uploading user avatar images to local storage.
 */
object AvatarStorage {
    /**
     * Uploads an avatar image for the given user to local storage.
     * @param context Application context
     * @param uid The user's Firebase UID
     * @param localUri The local URI of the image to upload
     * @return The URI of the saved image
     */
    suspend fun upload(context: Context, uid: String, localUri: Uri): Uri = withContext(Dispatchers.IO) {
        val avatarsDir = File(context.filesDir, "avatars")
        if (!avatarsDir.exists()) {
            avatarsDir.mkdirs()
        }

        val avatarFile = File(avatarsDir, "$uid.jpg")

        context.contentResolver.openInputStream(localUri)?.use { input ->
            FileOutputStream(avatarFile).use { output ->
                input.copyTo(output)
            }
        }

        Uri.fromFile(avatarFile)
    }

    /**
     * Deletes the user's avatar from local storage.
     * @param context Application context
     * @param uid The user's Firebase UID
     */
    suspend fun delete(context: Context, uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val avatarFile = File(context.filesDir, "avatars/$uid.jpg")
            if (avatarFile.exists()) {
                avatarFile.delete()
            }
        }
    }

    /**
     * Gets the URI of the user's avatar from local storage.
     * @param context Application context
     * @param uid The user's Firebase UID
     * @return The URI of the avatar, or null if not found
     */
    fun getAvatarUri(context: Context, uid: String): Uri? {
        val avatarFile = File(context.filesDir, "avatars/$uid.jpg")
        return if (avatarFile.exists()) {
            Uri.fromFile(avatarFile)
        } else {
            null
        }
    }
}
