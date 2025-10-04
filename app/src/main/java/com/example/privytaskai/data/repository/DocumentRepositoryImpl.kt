package com.example.privytaskai.data.repository

import android.util.Log
import com.example.privytaskai.data.database.dao.DocumentDao
import com.example.privytaskai.data.database.entities.DocumentChunkEntity
import com.example.privytaskai.data.database.entities.DocumentEntity
import com.example.privytaskai.data.local.LocalEmbeddingService
import com.example.privytaskai.domain.model.*
import com.example.privytaskai.domain.repository.DocumentRepository
import com.example.privytaskai.domain.repository.DocumentStats
import com.example.privytaskai.privacy.PrivacyAuditor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of DocumentRepository with RAG capabilities
 */
@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val documentDao: DocumentDao,
    private val embeddingService: LocalEmbeddingService,
    private val privacyAuditor: PrivacyAuditor
) : DocumentRepository {

    companion object {
        private const val TAG = "DocumentRepositoryImpl"
    }

    override suspend fun indexDocument(
        title: String,
        content: String,
        documentType: DocumentType,
        metadata: DocumentMetadata,
        chunks: List<DocumentChunk>
    ): Long {
        privacyAuditor.auditDataFlow("INDEX_DOCUMENT", "DOCUMENT_DATA", "LOCAL_STORAGE")

        Log.d(TAG, "Indexing document: $title (${chunks.size} chunks)")

        // Insert document
        val documentEntity = DocumentEntity(
            title = title,
            content = content,
            documentType = documentType.name,
            author = metadata.author,
            subject = metadata.subject,
            creationDate = metadata.creationDate,
            numberOfPages = metadata.numberOfPages,
            fileSize = metadata.fileSize,
            sourceUri = metadata.sourceUri
        )

        val documentId = documentDao.insertDocument(documentEntity)

        // Generate embeddings for all chunks and insert
        val chunkEntities = chunks.map { chunk ->
            Log.d(TAG, "Generating embedding for chunk ${chunk.chunkIndex}...")

            val embedding = embeddingService.generateEmbedding(chunk.text)
            val embeddingCsv = embeddingService.vectorToCsv(embedding)
            val metadataJson = Json.encodeToString(chunk.metadata)

            DocumentChunkEntity(
                documentId = documentId,
                chunkIndex = chunk.chunkIndex,
                text = chunk.text,
                startIndex = chunk.startIndex,
                endIndex = chunk.endIndex,
                embeddingCsv = embeddingCsv,
                metadataJson = metadataJson
            )
        }

        documentDao.insertChunks(chunkEntities)

        Log.d(TAG, "Document indexed: ID=$documentId, ${chunkEntities.size} chunks")
        return documentId
    }

    override suspend fun searchSimilar(
        query: String,
        limit: Int
    ): List<SearchResult> {
        privacyAuditor.auditDataFlow("SEARCH_DOCUMENTS", "QUERY_EMBEDDING", "LOCAL_PROCESSING")
        privacyAuditor.validateLocalProcessing("VECTOR_SEARCH")

        Log.d(TAG, "Searching: '$query' (limit=$limit)")

        // Generate query embedding
        val queryEmbedding = embeddingService.generateQueryEmbedding(query)

        // Get all chunks with embeddings
        val allChunks = documentDao.getAllChunks()

        Log.d(TAG, "Comparing against ${allChunks.size} chunks")

        // Calculate similarities
        val results = allChunks.mapNotNull { chunkEntity ->
            try {
                val chunkEmbedding = embeddingService.csvToVector(chunkEntity.embeddingCsv)
                val similarity = embeddingService.cosineSimilarity(queryEmbedding, chunkEmbedding)

                // Get document
                val document = documentDao.getDocumentById(chunkEntity.documentId)
                    ?: return@mapNotNull null

                chunkEntity to Pair(document, similarity)

            } catch (_: Exception) {
                null
            }
        }

        // Sort by similarity and take top-k
        val topResults = results
            .sortedByDescending { it.second.second }
            .take(limit)

        // Convert to SearchResult
        return topResults.mapIndexed { index, (chunkEntity, pair) ->
            val (documentEntity, score) = pair

            SearchResult(
                chunk = chunkEntity.toDomain(),
                document = documentEntity.toDomain(emptyList()),
                score = score,
                rank = index + 1
            )
        }
    }

    override suspend fun searchByKeyword(query: String): List<Document> {
        privacyAuditor.auditDataFlow("KEYWORD_SEARCH", "QUERY_TEXT", "LOCAL_STORAGE")

        val documents = documentDao.searchDocuments(query)
        return documents.map { it.toDomain(emptyList()) }
    }

    override fun observeAllDocuments(): Flow<List<Document>> {
        return documentDao.observeAllDocuments().map { entities ->
            entities.map { it.toDomain(emptyList()) }
        }
    }

    override suspend fun getDocumentById(id: Long): Document? {
        privacyAuditor.auditDataFlow("GET_DOCUMENT", "DOCUMENT_DATA", "LOCAL_STORAGE")

        val documentEntity = documentDao.getDocumentById(id) ?: return null
        val chunks = documentDao.getChunksByDocumentId(id)

        return documentEntity.toDomain(chunks.map { it.toDomain() })
    }

    override suspend fun deleteDocument(id: Long) {
        privacyAuditor.auditDataFlow("DELETE_DOCUMENT", "DOCUMENT_DATA", "LOCAL_STORAGE")

        documentDao.deleteDocumentWithChunks(id)
        Log.d(TAG, "Deleted document: ID=$id")
    }

    override suspend fun getStats(): DocumentStats {
        val chunkCount = documentDao.getChunkCount()

        return DocumentStats(
            totalDocuments = 0,
            totalChunks = chunkCount,
            avgChunksPerDocument = 0f
        )
    }

    // Mapping functions
    private fun DocumentEntity.toDomain(chunks: List<DocumentChunk>): Document {
        return Document(
            id = id,
            title = title,
            content = content,
            documentType = DocumentType.valueOf(documentType),
            metadata = DocumentMetadata(
                author = author,
                subject = subject,
                creationDate = creationDate,
                numberOfPages = numberOfPages,
                fileSize = fileSize,
                sourceUri = sourceUri
            ),
            chunks = chunks,
            createdAt = createdAt
        )
    }

    private fun DocumentChunkEntity.toDomain(): DocumentChunk {
        val metadata = try {
            Json.decodeFromString<Map<String, String>>(metadataJson)
        } catch (_: Exception) {
            emptyMap()
        }

        return DocumentChunk(
            id = id,
            documentId = documentId,
            chunkIndex = chunkIndex,
            text = text,
            startIndex = startIndex,
            endIndex = endIndex,
            embedding = embeddingService.csvToVector(embeddingCsv),
            metadata = metadata
        )
    }
}
