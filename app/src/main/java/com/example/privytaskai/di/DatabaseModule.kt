package com.example.privytaskai.di

import android.content.Context
import androidx.room.Room
import com.example.privytaskai.data.database.database.TaskDatabase
import com.example.privytaskai.data.database.dao.TaskDao
import com.example.privytaskai.data.database.dao.EmbeddingDao
import com.example.privytaskai.data.database.dao.DocumentDao
import com.example.privytaskai.data.database.migrations.Migrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideTaskDatabase(@ApplicationContext context: Context): TaskDatabase {
        return Room.databaseBuilder(
            context,
            TaskDatabase::class.java,
            "task_database"
        )
        .addMigrations(Migrations.MIGRATION_1_2)  // Add the migration instead of destructive fallback
        .build()
    }
    
    @Provides
    fun provideTaskDao(database: TaskDatabase): TaskDao = database.taskDao()
    
    @Provides
    fun provideEmbeddingDao(database: TaskDatabase): EmbeddingDao = database.embeddingDao()

    @Provides
    fun provideDocumentDao(database: TaskDatabase): DocumentDao = database.documentDao()
}