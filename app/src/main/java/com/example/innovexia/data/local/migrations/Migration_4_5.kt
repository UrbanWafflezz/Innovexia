package com.example.innovexia.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.innovexia.core.auth.ProfileId

/**
 * Migration from version 4 to 5:
 * Adds ownerId field to chats and messages tables to support Guest Mode
 * and user-scoped local storage.
 *
 * Backfills all existing rows with ownerId = "guest".
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add ownerId column to chats table with default value "guest"
        db.execSQL("""
            ALTER TABLE chats
            ADD COLUMN ownerId TEXT NOT NULL DEFAULT '${ProfileId.GUEST_OWNER_ID}'
        """.trimIndent())

        // Add ownerId column to messages table with default value "guest"
        db.execSQL("""
            ALTER TABLE messages
            ADD COLUMN ownerId TEXT NOT NULL DEFAULT '${ProfileId.GUEST_OWNER_ID}'
        """.trimIndent())

        // Create indices for ownerId columns for better query performance
        db.execSQL("CREATE INDEX IF NOT EXISTS index_chats_ownerId ON chats(ownerId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_chats_updatedAt ON chats(updatedAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_ownerId ON messages(ownerId)")
    }
}
