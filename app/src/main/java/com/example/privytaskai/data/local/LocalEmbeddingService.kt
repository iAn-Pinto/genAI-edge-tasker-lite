@file:Suppress("UNUSED_PARAMETER")

package com.example.privytaskai.data.local

import android.util.Log
import com.example.privytaskai.data.ml.EmbeddingGemmaModel
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local embedding service using EmbeddingGemma for semantic embeddings
 * 
 * Provides:
 * - 768-dimensional semantic embeddings
 * - Sub-70ms inference on CPU
 * - Backward compatibility with existing repository code
 */
@Singleton
class LocalEmbeddingService @Inject constructor(
    private val embeddingModel: EmbeddingGemmaModel
) {
    
    companion object {
        private const val TAG = "LocalEmbeddingService"
        private const val EMBEDDING_DIM = 768
    }
    
    /**
     * Generate semantic embedding using EmbeddingGemma
     * 
     * @param text Input text (automatically truncated to 512 tokens)
     * @param dimensions Ignored (EmbeddingGemma always produces 768-dim)
     * @return List<Float> of 768 dimensions
     */
    fun generateEmbedding(text: String, dimensions: Int = EMBEDDING_DIM): List<Float> {
        if (text.isEmpty()) {
            Log.w(TAG, "Empty text provided, returning zero vector")
            return FloatArray(EMBEDDING_DIM).toList()
        }
        
        return try {
            // Use runBlocking since Room/Repository calls are already in coroutine scope
            // and this method signature is synchronous for backward compatibility
            val result = runBlocking {
                embeddingModel.generateEmbedding(
                    text = text,
                    taskType = EmbeddingGemmaModel.TaskType.DOCUMENT
                )
            }
            
            result.getOrElse { error ->
                Log.e(TAG, "Embedding generation failed", error)
                FloatArray(EMBEDDING_DIM)
            }.toList()
            
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during embedding generation", e)
            FloatArray(EMBEDDING_DIM).toList()
        }
    }
    
    /**
     * Generate embedding for search queries
     * Uses QUESTION_ANSWERING task type for better retrieval
     */
    fun generateQueryEmbedding(query: String): List<Float> {
        if (query.isEmpty()) {
            return FloatArray(EMBEDDING_DIM).toList()
        }
        
        return try {
            val result = runBlocking {
                embeddingModel.generateEmbedding(
                    text = query,
                    taskType = EmbeddingGemmaModel.TaskType.QUESTION_ANSWERING
                )
            }
            
            result.getOrElse { error ->
                Log.e(TAG, "Query embedding generation failed", error)
                FloatArray(EMBEDDING_DIM)
            }.toList()
            
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during query embedding", e)
            FloatArray(EMBEDDING_DIM).toList()
        }
    }
    
    /**
     * Convert vector to CSV string for Room database storage
     */
    fun vectorToCsv(vector: List<Float>): String {
        return vector.joinToString(",")
    }
    
    /**
     * Parse CSV string to vector
     */
    fun csvToVector(csv: String): List<Float> {
        return if (csv.isBlank()) {
            FloatArray(EMBEDDING_DIM).toList()
        } else {
            try {
                csv.split(',').map { it.toFloat() }
            } catch (e: NumberFormatException) {
                Log.e(TAG, "Failed to parse CSV vector", e)
                FloatArray(EMBEDDING_DIM).toList()
            }
        }
    }
    
    /**
     * Calculate cosine similarity between two embeddings
     * 
     * @return Similarity score between 0 and 1 (1 = identical)
     */
    fun cosineSimilarity(a: List<Float>, b: List<Float>): Float {
        return try {
            embeddingModel.cosineSimilarity(
                a.toFloatArray(),
                b.toFloatArray()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Cosine similarity calculation failed", e)
            0f
        }
    }
    
    /**
     * Truncate embedding to lower dimensions
     * Useful for memory optimization (Matryoshka representation)
     */
    fun truncateEmbedding(embedding: List<Float>, targetDim: Int): List<Float> {
        require(targetDim in listOf(128, 256, 512, 768)) {
            "Target dimension must be 128, 256, 512, or 768"
        }
        
        return try {
            embeddingModel.truncateEmbedding(
                embedding.toFloatArray(),
                targetDim
            ).toList()
        } catch (e: Exception) {
            Log.e(TAG, "Embedding truncation failed", e)
            embedding.take(targetDim)
        }
    }
}
