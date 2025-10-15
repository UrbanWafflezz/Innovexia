package com.example.innovexia.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.innovexia.data.local.dao.ChatDao
import com.example.innovexia.data.local.dao.MessageDao
import com.example.innovexia.data.local.dao.MemChunkDao
import com.example.innovexia.data.local.dao.HealthCheckDao
import com.example.innovexia.data.local.dao.IncidentDao
import com.example.innovexia.data.local.dao.PersonaDao
import com.example.innovexia.data.local.dao.SessionDao
import com.example.innovexia.data.local.dao.SubscriptionDao
import com.example.innovexia.data.local.dao.UsageDao
import com.example.innovexia.data.local.entities.ChatEntity
import com.example.innovexia.data.local.entities.MessageEntity
import com.example.innovexia.data.local.entities.MemChunkEntity
import com.example.innovexia.data.local.entities.HealthCheckEntity
import com.example.innovexia.data.local.entities.IncidentEntity
import com.example.innovexia.data.local.entities.PersonaEntity
import com.example.innovexia.data.local.entities.SessionEntity
import com.example.innovexia.data.local.entities.SubscriptionEntity
import com.example.innovexia.data.local.entities.UsageEntity
import com.example.innovexia.data.local.entities.DailyUsageEntity
import com.example.innovexia.data.local.migrations.MIGRATION_4_5
import com.example.innovexia.data.local.migrations.MIGRATION_5_6
import com.example.innovexia.data.local.migrations.MIGRATION_6_7
import com.example.innovexia.data.local.migrations.MIGRATION_7_8
import com.example.innovexia.data.local.migrations.MIGRATION_8_9
import com.example.innovexia.data.local.migrations.MIGRATION_9_10
import com.example.innovexia.data.local.migrations.MIGRATION_10_11
import com.example.innovexia.data.local.migrations.MIGRATION_11_12
import com.example.innovexia.data.local.migrations.MIGRATION_12_13
import com.example.innovexia.data.local.migrations.MIGRATION_13_14
import com.example.innovexia.data.local.migrations.MIGRATION_14_15
import com.example.innovexia.data.local.migrations.MIGRATION_15_16
import com.example.innovexia.data.local.migrations.MIGRATION_16_17
import com.example.innovexia.data.local.migrations.MIGRATION_17_18
import com.example.innovexia.data.local.migrations.MIGRATION_18_19
import com.example.innovexia.data.local.migrations.MIGRATION_19_20
import com.example.innovexia.data.local.migrations.MIGRATION_20_21

/**
 * Room database for local chat storage.
 * Version 8: Added local chat management fields (pinned, archived, deletedLocally) to chats.
 * Version 9: Added personas table for local-first persona storage.
 * Version 10: Added subscription and usage tracking tables.
 * Version 11: Added extendedSettings column to personas table for Persona 2.0.
 * Version 12: Added UserBubbleV2 fields (status, editedAt, supersedesMessageId, replacedAssistantId) to messages.
 * Version 13: Added Incognito mode fields (isIncognito, cloudId to chats; localOnly to messages).
 * Version 14: Added in-place regeneration fields (streamState, regenCount, error) to messages.
 * Version 15: Diagnostic migration with logging and performance indexes.
 * Version 16: Added model tracking fields (currentModel to chats, modelUsed to messages) for Gemini 2.5 family.
 * Version 17: Added medical disclaimer support (hasMedicalDisclaimer to messages) - REVERTED in v18.
 * Version 18: Removed medical disclaimer support (removed hasMedicalDisclaimer from messages).
 * Version 19: Added lastUsedAt field to personas table for tracking persona usage.
 * Version 20: Added groundingJson field to messages table for storing Google Search grounding metadata.
 * Version 21: Added groundingStatus field to messages table for tracking grounding search state.
 */
@TypeConverters(AppDatabase.Converters::class)
@Database(
    entities = [
        ChatEntity::class,
        MessageEntity::class,
        MemChunkEntity::class,
        HealthCheckEntity::class,
        IncidentEntity::class,
        SessionEntity::class,
        PersonaEntity::class,
        SubscriptionEntity::class,
        UsageEntity::class,
        DailyUsageEntity::class
    ],
    version = 21,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun memChunkDao(): MemChunkDao
    abstract fun healthCheckDao(): HealthCheckDao
    abstract fun incidentDao(): IncidentDao
    abstract fun sessionDao(): SessionDao
    abstract fun personaDao(): PersonaDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun usageDao(): UsageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "innovexia_db"
                )
                    .addMigrations(
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        MIGRATION_15_16,
                        MIGRATION_16_17,
                        MIGRATION_17_18,
                        MIGRATION_18_19,
                        MIGRATION_19_20,
                        MIGRATION_20_21
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Type converters for complex fields
     */
    class Converters {
        @TypeConverter
        fun fromStringList(value: List<String>?): String? {
            return value?.joinToString(",")
        }

        @TypeConverter
        fun toStringList(value: String?): List<String>? {
            return value?.split(",")?.filter { it.isNotEmpty() }
        }
    }
}
