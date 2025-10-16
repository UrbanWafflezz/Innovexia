package com.example.innovexia.data.local.migrations

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 19 to 20
 * Adds groundingJson field to messages table for storing Google Search grounding metadata
 */
val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        Log.d("MIGRATION_19_20", "Starting migration from version 19 to 20")

        try {
            // Check if groundingJson column already exists
            val cursor = db.query("PRAGMA table_info(messages)")
            var columnExists = false
            while (cursor.moveToNext()) {
                val columnName = cursor.getString(cursor.getColumnIndex("name"))
                if (columnName == "groundingJson") {
                    columnExists = true
                    break
                }
            }
            cursor.close()

            if (!columnExists) {
                // Add groundingJson field to messages table (nullable, defaults to null)
                db.execSQL("ALTER TABLE messages ADD COLUMN groundingJson TEXT")
                Log.d("MIGRATION_19_20", "Added groundingJson column to messages")
            } else {
                Log.d("MIGRATION_19_20", "groundingJson column already exists, skipping")
            }

            // Count affected rows
            val messageCursor = db.query("SELECT COUNT(*) FROM messages")
            if (messageCursor.moveToFirst()) {
                val messageCount = messageCursor.getInt(0)
                Log.d("MIGRATION_19_20", "Migration completed for $messageCount messages")
            }
            messageCursor.close()

            Log.d("MIGRATION_19_20", "Migration completed successfully")

        } catch (e: Exception) {
            Log.e("MIGRATION_19_20", "Error during migration: ${e.message}", e)
            throw e
        }
    }
}
