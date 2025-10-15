package com.example.innovexia.data.local.migrations

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 16 to 17
 * Adds medical disclaimer support:
 * - hasMedicalDisclaimer to messages: tracks messages that contain medical/health advice
 */
val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        Log.d("MIGRATION_16_17", "Starting migration from version 16 to 17")

        try {
            // Add hasMedicalDisclaimer field to messages table (defaults to false)
            db.execSQL("ALTER TABLE messages ADD COLUMN hasMedicalDisclaimer INTEGER NOT NULL DEFAULT 0")
            Log.d("MIGRATION_16_17", "Added hasMedicalDisclaimer column to messages")

            // Count affected rows
            val msgCursor = db.query("SELECT COUNT(*) FROM messages")
            if (msgCursor.moveToFirst()) {
                val msgCount = msgCursor.getInt(0)
                Log.d("MIGRATION_16_17", "Updated $msgCount messages with default disclaimer flag (false)")
            }
            msgCursor.close()

            Log.d("MIGRATION_16_17", "Migration completed successfully")

        } catch (e: Exception) {
            Log.e("MIGRATION_16_17", "Error during migration: ${e.message}", e)
            throw e
        }
    }
}
