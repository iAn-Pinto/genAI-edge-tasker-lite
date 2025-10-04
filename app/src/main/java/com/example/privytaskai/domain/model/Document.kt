package com.example.privytaskai.domain.model

/**
 * Domain model for a SEBI circular document
 */
data class Document(
    val id: Long = 0,
    val title: String,
    val content: String,
    val documentType: DocumentType = DocumentType.SEBI_CIRCULAR,
    val metadata: DocumentMetadata,
    val chunks: List<DocumentChunk> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Individual chunk of a document for RAG retrieval
 */
data class DocumentChunk(
    val id: Long = 0,
    val documentId: Long,
    val chunkIndex: Int,
    val text: String,
    val startIndex: Int,
    val endIndex: Int,
    val embedding: List<Float>? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Document metadata extracted from PDF
 */
data class DocumentMetadata(
    val author: String? = null,
    val subject: String? = null,
    val creationDate: String? = null,
    val numberOfPages: Int = 0,
    val fileSize: Long = 0,
    val sourceUri: String? = null
)

/**
 * Document types supported
 */
enum class DocumentType {
    SEBI_CIRCULAR,
    SEBI_NOTIFICATION,
    SEBI_GUIDANCE,
    SEBI_PRESS_RELEASE,
    OTHER
}

/**
 * Search result with relevance score
 */
data class SearchResult(
    val chunk: DocumentChunk,
    val document: Document,
    val score: Float,
    val rank: Int
)

