package com.example.innovexia.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 7 to 8.
 * Adds local chat management fields: pinned, archived, and deletedLocally to chats table.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add pinned field to chats table (default false = 0)
        db.execSQL("ALTER TABLE chats ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")

        // Add archived field to chats table (default false = 0)
        db.execSQL("ALTER TABLE chats ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")

        // Add deletedLocally field to chats table (default false = 0)
        db.execSQL("ALTER TABLE chats ADD COLUMN deletedLocally INTEGER NOT NULL DEFAULT 0")
    }
}
