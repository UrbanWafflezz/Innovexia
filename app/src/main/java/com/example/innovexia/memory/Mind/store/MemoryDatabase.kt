package com.example.innovexia.memory.Mind.store

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.innovexia.memory.Mind.store.dao.MemoryDao
import com.example.innovexia.memory.Mind.store.dao.MemoryFtsDao
import com.example.innovexia.memory.Mind.store.dao.VectorDao
import com.example.innovexia.memory.Mind.store.entities.MemoryEntity
import com.example.innovexia.memory.Mind.store.entities.MemoryFtsEntity
import com.example.innovexia.memory.Mind.store.entities.MemoryVectorEntity

/**
 * Room database for persona memories
 */
@Database(
    entities = [
        MemoryEntity::class,
        MemoryFtsEntity::class,
        MemoryVectorEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MemoryDatabase : RoomDatabase() {

    abstract fun memoryDao(): MemoryDao
    abstract fun memoryFtsDao(): MemoryFtsDao
    abstract fun vectorDao(): VectorDao

    companion object {
        @Volatile
        private var INSTANCE: MemoryDatabase? = null

        /**
         * Migration from version 1 to 2: Clear old 256-dim vectors, keep memories
         * This allows the system to regenerate 768-dim vectors on next ingestion
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                android.util.Log.i("MemoryDatabase", "Migrating from v1 to v2: Clearing old vectors (dimension change 256->768)")
                // Delete all old vectors - memories and FTS entries are preserved
                // New vectors will be generated automatically on next chat
                database.execSQL("DELETE FROM memory_vectors")
            }
        }

        fun getInstance(context: Context): MemoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MemoryDatabase::class.java,
                    "memory_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration() // If migration fails, recreate DB
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
