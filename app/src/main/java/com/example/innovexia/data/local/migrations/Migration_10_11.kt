package com.example.innovexia.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 10 to 11
 * Adds extendedSettings column to personas table for Persona 2.0 extended configuration
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add extendedSettings column to personas table
        db.execSQL("ALTER TABLE personas ADD COLUMN extendedSettings TEXT DEFAULT NULL")
    }
}
