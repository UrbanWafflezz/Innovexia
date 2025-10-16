package com.example.innovexia.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 12 to 13
 * Adds Incognito mode fields:
 * - chats.isIncognito: boolean flag for incognito mode (local-only)
 * - chats.cloudId: Firestore document ID if chat was uploaded to cloud
 * - messages.localOnly: boolean flag mirroring chat.isIncognito when message was created
 */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add incognito fields to chats table
        db.execSQL("ALTER TABLE chats ADD COLUMN isIncognito INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE chats ADD COLUMN cloudId TEXT DEFAULT NULL")

        // Add localOnly field to messages table
        db.execSQL("ALTER TABLE messages ADD COLUMN localOnly INTEGER NOT NULL DEFAULT 0")
    }
}
