package com.example.innovexia.data.local.migrations

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 14 to 15
 * Diagnostic migration to verify database integrity and log data state.
 * This migration doesn't change the schema but adds logging to help diagnose
 * issues with missing chats on certain devices.
 */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        Log.d("MIGRATION_14_15", "Starting migration from version 14 to 15")

        try {
            // Count existing chats
            val chatCursor = db.query("SELECT COUNT(*) FROM chats")
            if (chatCursor.moveToFirst()) {
                val chatCount = chatCursor.getInt(0)
                Log.d("MIGRATION_14_15", "Found $chatCount existing chats")
            }
            chatCursor.close()

            // Count existing messages
            val msgCursor = db.query("SELECT COUNT(*) FROM messages")
            if (msgCursor.moveToFirst()) {
                val msgCount = msgCursor.getInt(0)
                Log.d("MIGRATION_14_15", "Found $msgCount existing messages")
            }
            msgCursor.close()

            // Log distinct owner IDs in chats table
            val ownerCursor = db.query("SELECT DISTINCT ownerId FROM chats")
            val ownerIds = mutableListOf<String>()
            while (ownerCursor.moveToNext()) {
                ownerIds.add(ownerCursor.getString(0))
            }
            ownerCursor.close()
            Log.d("MIGRATION_14_15", "Found owner IDs: ${ownerIds.joinToString(", ")}")

            // Verify tables exist
            val tablesCursor = db.query(
                "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name"
            )
            val tables = mutableListOf<String>()
            while (tablesCursor.moveToNext()) {
                tables.add(tablesCursor.getString(0))
            }
            tablesCursor.close()
            Log.d("MIGRATION_14_15", "Existing tables: ${tables.joinToString(", ")}")

            // No schema changes needed - this is a diagnostic-only migration
            Log.d("MIGRATION_14_15", "Migration completed successfully - no schema changes needed")

        } catch (e: Exception) {
            Log.e("MIGRATION_14_15", "Error during migration: ${e.message}", e)
            throw e
        }
    }
}
