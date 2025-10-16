package com.example.innovexia.data.local.migrations

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 20 to 21
 * Adds groundingStatus field to messages table for tracking grounding search state
 */
val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        Log.d("MIGRATION_20_21", "Starting migration from version 20 to 21")

        try {
            // Check if groundingStatus column already exists
            val cursor = db.query("PRAGMA table_info(messages)")
            var columnExists = false
            while (cursor.moveToNext()) {
                val columnName = cursor.getString(cursor.getColumnIndex("name"))
                if (columnName == "groundingStatus") {
                    columnExists = true
                    break
                }
            }
            cursor.close()

            if (!columnExists) {
                // Add groundingStatus field to messages table (defaults to "NONE")
                db.execSQL("ALTER TABLE messages ADD COLUMN groundingStatus TEXT NOT NULL DEFAULT 'NONE'")
                Log.d("MIGRATION_20_21", "Added groundingStatus column to messages")
            } else {
                Log.d("MIGRATION_20_21", "groundingStatus column already exists, skipping")
            }

            // Count affected rows
            val messageCursor = db.query("SELECT COUNT(*) FROM messages")
            if (messageCursor.moveToFirst()) {
                val messageCount = messageCursor.getInt(0)
                Log.d("MIGRATION_20_21", "Migration completed for $messageCount messages")
            }
            messageCursor.close()

            Log.d("MIGRATION_20_21", "Migration completed successfully")

        } catch (e: Exception) {
            Log.e("MIGRATION_20_21", "Error during migration: ${e.message}", e)
            throw e
        }
    }
}
