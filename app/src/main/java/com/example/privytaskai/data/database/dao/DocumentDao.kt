package com.example.privytaskai.data.database.dao

import androidx.room.*
import com.example.privytaskai.data.database.entities.DocumentEntity
import com.example.privytaskai.data.database.entities.DocumentChunkEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for document operations
 */
@Dao
interface DocumentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<DocumentChunkEntity>)

    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun observeAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): DocumentEntity?

    @Query("SELECT * FROM document_chunks WHERE documentId = :documentId ORDER BY chunkIndex")
    suspend fun getChunksByDocumentId(documentId: Long): List<DocumentChunkEntity>

    @Query("SELECT * FROM document_chunks")
    suspend fun getAllChunks(): List<DocumentChunkEntity>

    @Query("SELECT COUNT(*) FROM document_chunks")
    suspend fun getChunkCount(): Int

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("DELETE FROM document_chunks WHERE documentId = :documentId")
    suspend fun deleteChunksByDocumentId(documentId: Long)

    @Transaction
    suspend fun deleteDocumentWithChunks(documentId: Long) {
        getDocumentById(documentId)?.let { document ->
            deleteDocument(document)
            deleteChunksByDocumentId(documentId)
        }
    }

    @Query("SELECT * FROM documents WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    suspend fun searchDocuments(query: String): List<DocumentEntity>
}

