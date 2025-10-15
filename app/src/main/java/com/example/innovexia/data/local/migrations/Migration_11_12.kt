package com.example.innovexia.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 11 to 12
 * Adds UserBubbleV2 fields to messages table:
 * - status: message send status (SENDING, SENT, FAILED)
 * - editedAt: timestamp when message was edited
 * - supersedesMessageId: reference to previous message if this is an edit-resend
 * - replacedAssistantId: reference to assistant message superseded by this edit
 */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add new columns to messages table with default values
        db.execSQL("ALTER TABLE messages ADD COLUMN status TEXT NOT NULL DEFAULT 'SENT'")
        db.execSQL("ALTER TABLE messages ADD COLUMN editedAt INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE messages ADD COLUMN supersedesMessageId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE messages ADD COLUMN replacedAssistantId TEXT DEFAULT NULL")
    }
}
