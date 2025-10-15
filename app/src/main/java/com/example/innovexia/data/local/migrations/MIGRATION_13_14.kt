package com.example.innovexia.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 13 to 14
 * Adds in-place regeneration fields to messages:
 * - streamState: tracks streaming status (IDLE, STREAMING, ERROR)
 * - regenCount: increments with each regeneration
 * - error: error message if streamState == ERROR
 */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add in-place regeneration fields to messages table
        db.execSQL("ALTER TABLE messages ADD COLUMN streamState TEXT NOT NULL DEFAULT 'IDLE'")
        db.execSQL("ALTER TABLE messages ADD COLUMN regenCount INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE messages ADD COLUMN error TEXT DEFAULT NULL")
    }
}
