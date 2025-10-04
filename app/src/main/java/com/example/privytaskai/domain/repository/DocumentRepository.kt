package com.example.privytaskai.domain.repository

import com.example.privytaskai.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for document operations
 */
interface DocumentRepository {

    /**
     * Index a document with its chunks
     * Automatically generates embeddings for all chunks
     *
     * @return Document ID
     */
    suspend fun indexDocument(
        title: String,
        content: String,
        documentType: DocumentType,
        metadata: DocumentMetadata,
        chunks: List<DocumentChunk>
    ): Long

    /**
     * Search documents by semantic similarity
     *
     * @param query Natural language query
     * @param limit Maximum number of results
     * @return List of search results with scores
     */
    suspend fun searchSimilar(
        query: String,
        limit: Int = 10
    ): List<SearchResult>

    /**
     * Search documents by keyword (fallback for exact matches)
     */
    suspend fun searchByKeyword(query: String): List<Document>

    /**
     * Get all documents
     */
    fun observeAllDocuments(): Flow<List<Document>>

    /**
     * Get document by ID with all chunks
     */
    suspend fun getDocumentById(id: Long): Document?

    /**
     * Delete document and all its chunks
     */
    suspend fun deleteDocument(id: Long)

    /**
     * Get statistics
     */
    suspend fun getStats(): DocumentStats
}

/**
 * Document repository statistics
 */
data class DocumentStats(
    val totalDocuments: Int,
    val totalChunks: Int,
    val avgChunksPerDocument: Float
)

