package com.example.innovexia.data.local.migrations

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 15 to 16
 * Adds model tracking fields for Gemini 2.5 family:
 * - currentModel to chats: tracks the currently selected model for the chat
 * - modelUsed to messages: tracks which model was used to generate each message (provenance)
 */
val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        Log.d("MIGRATION_15_16", "Starting migration from version 15 to 16")

        try {
            // Add currentModel field to chats table (defaults to gemini-2.5-flash)
            db.execSQL("ALTER TABLE chats ADD COLUMN currentModel TEXT NOT NULL DEFAULT 'gemini-2.5-flash'")
            Log.d("MIGRATION_15_16", "Added currentModel column to chats")

            // Add modelUsed field to messages table (defaults to gemini-2.5-flash)
            db.execSQL("ALTER TABLE messages ADD COLUMN modelUsed TEXT NOT NULL DEFAULT 'gemini-2.5-flash'")
            Log.d("MIGRATION_15_16", "Added modelUsed column to messages")

            // Count affected rows
            val chatCursor = db.query("SELECT COUNT(*) FROM chats")
            if (chatCursor.moveToFirst()) {
                val chatCount = chatCursor.getInt(0)
                Log.d("MIGRATION_15_16", "Updated $chatCount chats with default model")
            }
            chatCursor.close()

            val msgCursor = db.query("SELECT COUNT(*) FROM messages")
            if (msgCursor.moveToFirst()) {
                val msgCount = msgCursor.getInt(0)
                Log.d("MIGRATION_15_16", "Updated $msgCount messages with default model")
            }
            msgCursor.close()

            Log.d("MIGRATION_15_16", "Migration completed successfully")

        } catch (e: Exception) {
            Log.e("MIGRATION_15_16", "Error during migration: ${e.message}", e)
            throw e
        }
    }
}
