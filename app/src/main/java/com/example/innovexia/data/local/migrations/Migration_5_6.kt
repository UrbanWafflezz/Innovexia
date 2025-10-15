package com.example.innovexia.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 5 to 6.
 * Adds cloud sync fields to chats and messages tables.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add cloud sync fields to chats table
        db.execSQL("ALTER TABLE chats ADD COLUMN lastMsgAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE chats ADD COLUMN msgCount INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE chats ADD COLUMN summaryHead TEXT")
        db.execSQL("ALTER TABLE chats ADD COLUMN summaryHasChunks INTEGER NOT NULL DEFAULT 0")

        // Create index on lastMsgAt for sorting
        db.execSQL("CREATE INDEX IF NOT EXISTS index_chats_lastMsgAt ON chats(lastMsgAt)")

        // Add cloud sync fields to messages table
        db.execSQL("ALTER TABLE messages ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE messages ADD COLUMN textHead TEXT")
        db.execSQL("ALTER TABLE messages ADD COLUMN hasChunks INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE messages ADD COLUMN attachmentsJson TEXT")
        db.execSQL("ALTER TABLE messages ADD COLUMN replyToId TEXT")

        // Migrate existing data: set updatedAt = createdAt for existing messages
        db.execSQL("UPDATE messages SET updatedAt = createdAt WHERE updatedAt = 0")

        // Migrate existing data: set lastMsgAt = updatedAt for existing chats
        db.execSQL("UPDATE chats SET lastMsgAt = updatedAt WHERE lastMsgAt = 0")
    }
}
