package com.example.innovexia.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 6 to 7.
 * Adds soft-delete support with deletedAt field to chats and messages tables.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add deletedAt field to chats table for soft-delete support
        db.execSQL("ALTER TABLE chats ADD COLUMN deletedAt INTEGER")

        // Add deletedAt field to messages table for soft-delete support
        db.execSQL("ALTER TABLE messages ADD COLUMN deletedAt INTEGER")
    }
}
