package com.example.privytaskai.domain.usecase

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.privytaskai.data.pdf.DocumentChunker
import com.example.privytaskai.data.pdf.PDFReader
import com.example.privytaskai.domain.model.Document
import com.example.privytaskai.domain.model.DocumentChunk
import com.example.privytaskai.domain.model.DocumentMetadata
import com.example.privytaskai.domain.model.DocumentType
import com.example.privytaskai.domain.repository.DocumentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for indexing SEBI PDF documents into the RAG system
 *
 * Flow:
 * 1. Parse PDF → Extract text
 * 2. Chunk document → 100-600 token chunks with overlap
 * 3. Generate embeddings → EmbeddingGemma for each chunk
 * 4. Store in database → For vector search
 */
class IndexDocumentUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pdfReader: PDFReader,
    private val documentChunker: DocumentChunker,
    private val documentRepository: DocumentRepository
) {
    companion object {
        private const val TAG = "IndexDocumentUseCase"
    }

    /**
     * Index a PDF document from URI
     *
     * @param uri Content URI of PDF file
     * @param title Document title
     * @param documentType Type of document
     * @return Result with document ID
     */
    suspend fun execute(
        uri: Uri,
        title: String,
        documentType: DocumentType = DocumentType.SEBI_CIRCULAR
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting document indexing: $title")

            // Step 1: Extract PDF text
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Cannot open URI: $uri")
                )

            val pdfResult = pdfReader.extractText(inputStream)
            if (pdfResult.isFailure) {
                return@withContext Result.failure(
                    pdfResult.exceptionOrNull() ?: Exception("PDF extraction failed")
                )
            }

            val pdfDocument = pdfResult.getOrThrow()
            Log.d(TAG, "Extracted ${pdfDocument.text.length} chars from PDF")

            // Step 2: Chunk document
            val chunkMetadata = mapOf(
                "title" to title,
                "documentType" to documentType.name
            )

            val chunks = documentChunker.chunkDocument(
                text = pdfDocument.text,
                metadata = chunkMetadata
            )

            Log.d(TAG, "Created ${chunks.size} chunks")

            // Step 3 & 4: Repository will generate embeddings and store
            val documentId = documentRepository.indexDocument(
                title = title,
                content = pdfDocument.text,
                documentType = documentType,
                metadata = DocumentMetadata(
                    author = pdfDocument.metadata.author,
                    subject = pdfDocument.metadata.subject,
                    creationDate = pdfDocument.metadata.creationDate,
                    numberOfPages = pdfDocument.metadata.numberOfPages,
                    sourceUri = uri.toString()
                ),
                chunks = chunks.map { chunk ->
                    DocumentChunk(
                        documentId = 0, // Will be set by repository
                        chunkIndex = chunk.chunkIndex,
                        text = chunk.text,
                        startIndex = chunk.startIndex,
                        endIndex = chunk.endIndex,
                        metadata = chunk.metadata
                    )
                }
            )

            Log.d(TAG, "Document indexed successfully: ID=$documentId")
            Result.success(documentId)

        } catch (e: Exception) {
            Log.e(TAG, "Document indexing failed", e)
            Result.failure(e)
        }
    }
}
