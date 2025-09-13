package com.example.privytaskai.data.database.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.privytaskai.data.database.entities.TaskEntity
import com.example.privytaskai.data.database.entities.EmbeddingEntity
import com.example.privytaskai.data.database.dao.TaskDao
import com.example.privytaskai.data.database.dao.EmbeddingDao

@Database(
    entities = [TaskEntity::class, EmbeddingEntity::class], 
    version = 1,
    exportSchema = false
)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun embeddingDao(): EmbeddingDao
}