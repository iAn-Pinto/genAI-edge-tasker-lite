package com.example.privytaskai.data.database.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.privytaskai.data.database.entities.TaskEntity
import com.example.privytaskai.data.database.entities.EmbeddingEntity
import com.example.privytaskai.data.database.entities.DocumentEntity
import com.example.privytaskai.data.database.entities.DocumentChunkEntity
import com.example.privytaskai.data.database.dao.TaskDao
import com.example.privytaskai.data.database.dao.EmbeddingDao
import com.example.privytaskai.data.database.dao.DocumentDao

@Database(
    entities = [
        TaskEntity::class,
        EmbeddingEntity::class,
        DocumentEntity::class,
        DocumentChunkEntity::class
    ],
    version = 2,  // Incremented for new tables
    exportSchema = false
)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun embeddingDao(): EmbeddingDao
    abstract fun documentDao(): DocumentDao
}