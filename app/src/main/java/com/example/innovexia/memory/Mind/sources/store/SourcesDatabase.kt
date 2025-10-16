package com.example.innovexia.memory.Mind.sources.store

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.innovexia.memory.Mind.sources.store.dao.SourceChunkDao
import com.example.innovexia.memory.Mind.sources.store.dao.SourceDao
import com.example.innovexia.memory.Mind.sources.store.entities.SourceChunkEntity
import com.example.innovexia.memory.Mind.sources.store.entities.SourceEntity

/**
 * Room database for persona sources (PDFs, URLs, images)
 */
@Database(
    entities = [
        SourceEntity::class,
        SourceChunkEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SourcesDatabase : RoomDatabase() {

    abstract fun sourceDao(): SourceDao
    abstract fun chunkDao(): SourceChunkDao

    companion object {
        @Volatile
        private var INSTANCE: SourcesDatabase? = null

        /**
         * Migration from version 1 to 2: Add web metadata fields
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new web-specific columns with default null values
                database.execSQL("ALTER TABLE sources ADD COLUMN metaTitle TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE sources ADD COLUMN metaDesc TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE sources ADD COLUMN domain TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE sources ADD COLUMN depth INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE sources ADD COLUMN contentType TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE sources ADD COLUMN pagesIndexed INTEGER DEFAULT NULL")
            }
        }

        fun getInstance(context: Context): SourcesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SourcesDatabase::class.java,
                    "sources_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
