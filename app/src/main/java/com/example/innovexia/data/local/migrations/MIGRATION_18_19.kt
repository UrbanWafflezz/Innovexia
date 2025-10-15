package com.example.innovexia.data.local.migrations

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 18 to 19
 * Adds lastUsedAt field to personas table for tracking when each persona was last selected/used
 */
val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        Log.d("MIGRATION_18_19", "Starting migration from version 18 to 19")

        try {
            // Add lastUsedAt field to personas table (nullable, defaults to null)
            db.execSQL("ALTER TABLE personas ADD COLUMN lastUsedAt INTEGER")
            Log.d("MIGRATION_18_19", "Added lastUsedAt column to personas")

            // Count affected rows
            val personaCursor = db.query("SELECT COUNT(*) FROM personas")
            if (personaCursor.moveToFirst()) {
                val personaCount = personaCursor.getInt(0)
                Log.d("MIGRATION_18_19", "Updated $personaCount personas with lastUsedAt field")
            }
            personaCursor.close()

            Log.d("MIGRATION_18_19", "Migration completed successfully")

        } catch (e: Exception) {
            Log.e("MIGRATION_18_19", "Error during migration: ${e.message}", e)
            throw e
        }
    }
}
