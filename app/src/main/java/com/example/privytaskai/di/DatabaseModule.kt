package com.example.privytaskai.di

import android.content.Context
import androidx.room.Room
import com.example.privytaskai.data.database.database.TaskDatabase
import com.example.privytaskai.data.database.dao.TaskDao
import com.example.privytaskai.data.database.dao.EmbeddingDao
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
        ).build()
    }
    
    @Provides
    fun provideTaskDao(database: TaskDatabase): TaskDao = database.taskDao()
    
    @Provides
    fun provideEmbeddingDao(database: TaskDatabase): EmbeddingDao = database.embeddingDao()
}