package com.example.privytaskai.data.pdf

import android.util.Log

/**
 * Intelligent document chunking for RAG
 *
 * Strategy:
 * - Chunk size: 100-600 tokens (optimal for retrieval)
 * - Overlap: 20% (100-120 tokens) to preserve context
 * - Sentence-aware splitting (don't break mid-sentence)
 *
 * Based on RAG best practices:
 * https://www.pinecone.io/learn/chunking-strategies/
 */
class DocumentChunker(
    private val minChunkSize: Int = 100,
    private val maxChunkSize: Int = 600,
    private val overlapTokens: Int = 100
) {

    companion object {
        private const val TAG = "DocumentChunker"
        private const val AVG_TOKEN_LENGTH = 4 // Average chars per token
    }

    /**
     * Document chunk with metadata
     */
    data class Chunk(
        val text: String,
        val startIndex: Int,
        val endIndex: Int,
        val chunkIndex: Int,
        val metadata: Map<String, String> = emptyMap()
    )

    /**
     * Chunk document text into overlapping segments
     *
     * @param text Full document text
     * @param metadata Optional metadata to attach to each chunk
     * @return List of chunks with overlap
     */
    fun chunkDocument(
        text: String,
        metadata: Map<String, String> = emptyMap()
    ): List<Chunk> {
        if (text.isBlank()) {
            Log.w(TAG, "Empty text provided for chunking")
            return emptyList()
        }

        Log.d(TAG, "Chunking document: ${text.length} chars")

        // Split into sentences first
        val sentences = splitIntoSentences(text)

        val chunks = mutableListOf<Chunk>()
        var currentChunk = StringBuilder()
        var currentStartIndex = 0
        var chunkIndex = 0

        var i = 0
        while (i < sentences.size) {
            val sentence = sentences[i]
            val potentialLength = currentChunk.length + sentence.length

            // Estimate tokens (rough approximation)
            val estimatedTokens = potentialLength / AVG_TOKEN_LENGTH

            when {
                // Chunk is too large, save it and start new one
                estimatedTokens > maxChunkSize && currentChunk.isNotEmpty() -> {
                    val chunkText = currentChunk.toString().trim()
                    chunks.add(
                        Chunk(
                            text = chunkText,
                            startIndex = currentStartIndex,
                            endIndex = currentStartIndex + chunkText.length,
                            chunkIndex = chunkIndex++,
                            metadata = metadata
                        )
                    )

                    // Start new chunk with overlap
                    currentChunk = StringBuilder()
                    currentStartIndex += chunkText.length

                    // Add overlap sentences
                    val overlapSentences = getOverlapSentences(sentences, i, overlapTokens)
                    currentChunk.append(overlapSentences.joinToString(" "))

                    // Don't increment i, reprocess this sentence in new chunk
                }

                // Add sentence to current chunk
                else -> {
                    if (currentChunk.isNotEmpty()) {
                        currentChunk.append(" ")
                    }
                    currentChunk.append(sentence)
                    i++
                }
            }
        }

        // Add final chunk if not empty
        if (currentChunk.isNotEmpty()) {
            val chunkText = currentChunk.toString().trim()
            if (chunkText.length / AVG_TOKEN_LENGTH >= minChunkSize) {
                chunks.add(
                    Chunk(
                        text = chunkText,
                        startIndex = currentStartIndex,
                        endIndex = currentStartIndex + chunkText.length,
                        chunkIndex = chunkIndex,
                        metadata = metadata
                    )
                )
            }
        }

        Log.d(TAG, "Created ${chunks.size} chunks")
        return chunks
    }

    /**
     * Split text into sentences (basic implementation)
     */
    private fun splitIntoSentences(text: String): List<String> {
        // Simple sentence splitting (can be improved with NLP library)
        return text.split(Regex("[.!?]+\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    /**
     * Get sentences for overlap window
     */
    private fun getOverlapSentences(
        sentences: List<String>,
        currentIndex: Int,
        targetTokens: Int
    ): List<String> {
        val overlap = mutableListOf<String>()
        var tokenCount = 0
        var i = currentIndex - 1

        while (i >= 0 && tokenCount < targetTokens) {
            val sentence = sentences[i]
            overlap.add(0, sentence)
            tokenCount += sentence.length / AVG_TOKEN_LENGTH
            i--
        }

        return overlap
    }

    /**
     * Chunk by paragraph instead of sentences
     * Useful for documents with clear paragraph structure
     */
    fun chunkByParagraph(
        text: String,
        metadata: Map<String, String> = emptyMap()
    ): List<Chunk> {
        val paragraphs = text.split(Regex("\n\n+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val chunks = mutableListOf<Chunk>()
        var currentStartIndex = 0

        paragraphs.forEachIndexed { index, paragraph ->
            val estimatedTokens = paragraph.length / AVG_TOKEN_LENGTH

            if (estimatedTokens >= minChunkSize) {
                chunks.add(
                    Chunk(
                        text = paragraph,
                        startIndex = currentStartIndex,
                        endIndex = currentStartIndex + paragraph.length,
                        chunkIndex = index,
                        metadata = metadata
                    )
                )
            }

            currentStartIndex += paragraph.length
        }

        return chunks
    }
}

