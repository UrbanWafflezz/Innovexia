package com.example.innovexia.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 8 to 9: Add personas table
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create personas table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `personas` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `ownerId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `initial` TEXT NOT NULL,
                `color` INTEGER NOT NULL,
                `summary` TEXT NOT NULL,
                `tags` TEXT NOT NULL,
                `system` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `isDefault` INTEGER NOT NULL DEFAULT 0,
                `cloudId` TEXT,
                `lastSyncedAt` INTEGER
            )
        """.trimIndent())

        // Create indices for better query performance
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_personas_ownerId` ON `personas` (`ownerId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_personas_updatedAt` ON `personas` (`updatedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_personas_isDefault` ON `personas` (`isDefault`)")
    }
}
