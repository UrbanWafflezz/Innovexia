package com.example.innovexia.di

import android.content.Context
import com.example.innovexia.core.persona.PersonaRepository
import com.example.innovexia.data.local.AppDatabase
import com.example.innovexia.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database and repository dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideChatDao(database: AppDatabase): ChatDao {
        return database.chatDao()
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: AppDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun provideMemChunkDao(database: AppDatabase): MemChunkDao {
        return database.memChunkDao()
    }

    @Provides
    @Singleton
    fun provideHealthCheckDao(database: AppDatabase): HealthCheckDao {
        return database.healthCheckDao()
    }

    @Provides
    @Singleton
    fun provideIncidentDao(database: AppDatabase): IncidentDao {
        return database.incidentDao()
    }

    @Provides
    @Singleton
    fun provideSessionDao(database: AppDatabase): SessionDao {
        return database.sessionDao()
    }

    @Provides
    @Singleton
    fun providePersonaDao(database: AppDatabase): PersonaDao {
        return database.personaDao()
    }

    @Provides
    @Singleton
    fun provideSubscriptionDao(database: AppDatabase): SubscriptionDao {
        return database.subscriptionDao()
    }

    @Provides
    @Singleton
    fun provideUsageDao(database: AppDatabase): UsageDao {
        return database.usageDao()
    }

    @Provides
    @Singleton
    fun providePersonaRepository(personaDao: PersonaDao): PersonaRepository {
        return PersonaRepository(personaDao)
    }

    @Provides
    @Singleton
    fun provideMemoryEngine(@ApplicationContext context: Context): com.example.innovexia.memory.Mind.api.MemoryEngine {
        return com.example.innovexia.memory.Mind.di.MindModule.provideMemoryEngine(context)
    }

    @Provides
    @Singleton
    fun provideSourcesEngine(@ApplicationContext context: Context): com.example.innovexia.memory.Mind.sources.api.SourcesEngine {
        return com.example.innovexia.memory.Mind.di.MindModule.provideSourcesEngine(context)
    }

    @Provides
    @Singleton
    fun provideGeminiService(database: AppDatabase, @ApplicationContext context: Context): com.example.innovexia.data.ai.GeminiService {
        return com.example.innovexia.data.ai.GeminiService(database, context)
    }
}
