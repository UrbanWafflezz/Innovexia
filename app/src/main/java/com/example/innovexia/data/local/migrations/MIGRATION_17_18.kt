package com.example.innovexia.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 17 to 18
 * Removes medical disclaimer support (hasMedicalDisclaimer column from messages)
 */
val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            android.util.Log.d("MIGRATION_17_18", "Starting migration from 17 to 18")

            // SQLite doesn't support DROP COLUMN directly, so we need to:
            // 1. Create a new table without the hasMedicalDisclaimer column
            // 2. Copy data from old table to new table
            // 3. Drop old table
            // 4. Rename new table to old name

            // Use a transaction for atomicity and speed
            db.beginTransaction()

            try {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS messages_new (
                        id TEXT PRIMARY KEY NOT NULL,
                        ownerId TEXT NOT NULL,
                        chatId TEXT NOT NULL,
                        role TEXT NOT NULL,
                        text TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        streamed INTEGER NOT NULL DEFAULT 1,
                        updatedAt INTEGER NOT NULL DEFAULT 0,
                        textHead TEXT,
                        hasChunks INTEGER NOT NULL DEFAULT 0,
                        attachmentsJson TEXT,
                        replyToId TEXT,
                        deletedAt INTEGER,
                        status TEXT NOT NULL DEFAULT 'SENT',
                        editedAt INTEGER,
                        supersedesMessageId TEXT,
                        replacedAssistantId TEXT,
                        localOnly INTEGER NOT NULL DEFAULT 0,
                        streamState TEXT NOT NULL DEFAULT 'IDLE',
                        regenCount INTEGER NOT NULL DEFAULT 0,
                        error TEXT,
                        modelUsed TEXT NOT NULL DEFAULT 'gemini-2.5-flash'
                    )
                """.trimIndent())

                // Copy data from old table to new table (excluding hasMedicalDisclaimer)
                db.execSQL("""
                    INSERT OR IGNORE INTO messages_new (
                        id, ownerId, chatId, role, text, createdAt, streamed, updatedAt,
                        textHead, hasChunks, attachmentsJson, replyToId, deletedAt,
                        status, editedAt, supersedesMessageId, replacedAssistantId,
                        localOnly, streamState, regenCount, error, modelUsed
                    )
                    SELECT
                        id, ownerId, chatId, role, text, createdAt,
                        COALESCE(streamed, 1),
                        updatedAt,
                        textHead,
                        COALESCE(hasChunks, 0),
                        attachmentsJson, replyToId, deletedAt,
                        status, editedAt, supersedesMessageId, replacedAssistantId,
                        localOnly, streamState, regenCount, error, modelUsed
                    FROM messages
                """.trimIndent())

                // Drop old table
                db.execSQL("DROP TABLE IF EXISTS messages")

                // Rename new table to old name
                db.execSQL("ALTER TABLE messages_new RENAME TO messages")

                // Recreate all required indexes
                db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_chatId ON messages(chatId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_ownerId ON messages(ownerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_createdAt ON messages(createdAt)")

                db.setTransactionSuccessful()
                android.util.Log.d("MIGRATION_17_18", "Successfully removed hasMedicalDisclaimer column from messages table")
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            android.util.Log.e("MIGRATION_17_18", "Migration failed", e)
            throw e
        }
    }
}
