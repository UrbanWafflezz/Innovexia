package com.example.innovexia.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 9 to 10
 * Adds subscription and usage tracking tables
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Drop existing tables if they exist (in case of previous failed migration)
        db.execSQL("DROP TABLE IF EXISTS subscriptions")
        db.execSQL("DROP TABLE IF EXISTS usage")
        db.execSQL("DROP TABLE IF EXISTS daily_usage")

        // Create subscriptions table
        db.execSQL("""
            CREATE TABLE subscriptions (
                userId TEXT NOT NULL PRIMARY KEY,
                plan TEXT NOT NULL,
                status TEXT NOT NULL,
                currentPeriodStart INTEGER,
                currentPeriodEnd INTEGER,
                cancelAtPeriodEnd INTEGER NOT NULL DEFAULT 0,
                stripeCustomerId TEXT,
                stripeSubscriptionId TEXT,
                trialEnd INTEGER,
                lastSynced INTEGER NOT NULL
            )
        """.trimIndent())

        // Create usage table
        db.execSQL("""
            CREATE TABLE usage (
                id TEXT NOT NULL PRIMARY KEY,
                userId TEXT NOT NULL,
                periodId TEXT NOT NULL,
                tokensIn INTEGER NOT NULL,
                tokensOut INTEGER NOT NULL,
                requests INTEGER NOT NULL,
                attachmentsBytes INTEGER NOT NULL,
                lastUpdated INTEGER NOT NULL,
                lastSynced INTEGER NOT NULL
            )
        """.trimIndent())

        // Create daily_usage table
        db.execSQL("""
            CREATE TABLE daily_usage (
                id TEXT NOT NULL PRIMARY KEY,
                userId TEXT NOT NULL,
                date TEXT NOT NULL,
                tokensIn INTEGER NOT NULL,
                tokensOut INTEGER NOT NULL,
                requests INTEGER NOT NULL,
                lastUpdated INTEGER NOT NULL
            )
        """.trimIndent())

        // Create indices for better query performance
        db.execSQL("CREATE INDEX IF NOT EXISTS index_usage_userId ON usage(userId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_usage_periodId ON usage(periodId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_usage_userId ON daily_usage(userId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_usage_date ON daily_usage(date)")
    }
}
