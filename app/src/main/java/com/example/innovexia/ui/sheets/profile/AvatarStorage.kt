package com.example.innovexia.ui.sheets.profile

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Handles avatar image uploads to local storage.
 */
class AvatarStorage {
    /**
     * Uploads an avatar image to local storage.
     *
     * @param context Application context
     * @param userId The user's ID
     * @param imageUri The URI of the image to upload
     * @return The URI of the saved image
     */
    suspend fun uploadAvatar(context: Context, userId: String, imageUri: Uri): Uri = withContext(Dispatchers.IO) {
        val avatarsDir = File(context.filesDir, "avatars")
        if (!avatarsDir.exists()) {
            avatarsDir.mkdirs()
        }

        val avatarFile = File(avatarsDir, "$userId.jpg")

        context.contentResolver.openInputStream(imageUri)?.use { input ->
            FileOutputStream(avatarFile).use { output ->
                input.copyTo(output)
            }
        }

        Uri.fromFile(avatarFile)
    }

    /**
     * Deletes an avatar image from local storage.
     *
     * @param context Application context
     * @param userId The user's ID
     */
    suspend fun deleteAvatar(context: Context, userId: String) {
        withContext(Dispatchers.IO) {
            val avatarFile = File(context.filesDir, "avatars/$userId.jpg")
            if (avatarFile.exists()) {
                avatarFile.delete()
            }
        }
    }
}
