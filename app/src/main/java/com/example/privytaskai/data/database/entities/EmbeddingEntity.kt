package com.example.privytaskai.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "embeddings")
data class EmbeddingEntity(
    @PrimaryKey 
    val taskId: Long,
    val vectorCsv: String
)