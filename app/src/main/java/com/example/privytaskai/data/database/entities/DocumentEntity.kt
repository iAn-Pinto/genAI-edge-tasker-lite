package com.example.privytaskai.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Document entity for Room database
 */
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val documentType: String,
    val author: String?,
    val subject: String?,
    val creationDate: String?,
    val numberOfPages: Int,
    val fileSize: Long,
    val sourceUri: String?,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Document chunk entity with embedding storage
 */
@Entity(
    tableName = "document_chunks",
    indices = [Index(value = ["documentId"])]
)
data class DocumentChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val chunkIndex: Int,
    val text: String,
    val startIndex: Int,
    val endIndex: Int,
    val embeddingCsv: String,  // Comma-separated vector (768 dimensions)
    val metadataJson: String = "{}"  // JSON serialized metadata
)

