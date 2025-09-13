package com.example.privytaskai.data.database.dao

import androidx.room.*
import com.example.privytaskai.data.database.entities.EmbeddingEntity

@Dao
interface EmbeddingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(embedding: EmbeddingEntity)

    @Query("SELECT * FROM embeddings WHERE taskId IN (:ids)")
    suspend fun getForTasks(ids: List<Long>): List<EmbeddingEntity>

    @Query("SELECT * FROM embeddings")
    suspend fun getAll(): List<EmbeddingEntity>

    @Query("DELETE FROM embeddings WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: Long)
}