package com.example.privytaskai.data.ml

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

/**
 * EmbeddingGemma 308M model for generating semantic embeddings.
 * 
 * Supports:
 * - 768-dimensional embeddings (can be truncated to 512/256/128)
 * - GPU acceleration with CPU fallback (using NNAPI)
 * - Task-specific prompt templates
 * - Sub-70ms inference on CPU, ~64ms on GPU
 * 
 * Memory usage: ~200MB
 */
class EmbeddingGemmaModel(
    private val context: Context,
    private val tokenizer: SentencePieceTokenizer,
    private val modelPath: String = "embeddinggemma_512.tflite"
) {
    private var interpreter: Interpreter? = null
    private var useAccelerator: Boolean = false

    companion object {
        private const val TAG = "EmbeddingGemma"
        private const val EMBEDDING_DIM = 768
    }
    
    /**
     * Task types for prompt templates
     */
    enum class TaskType {
        SEARCH,              // For queries
        QUESTION_ANSWERING,  // For questions
        DOCUMENT             // For documents
    }
    
    /**
     * Initialize the model with NNAPI acceleration and CPU fallback
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing EmbeddingGemma model...")
            
            // Validate model file exists and has reasonable size
            val modelBuffer = loadModelFile()
            val modelSize = modelBuffer.capacity()

            if (modelSize < 1_000_000) { // Less than 1MB is definitely wrong
                val error = """
                    |❌ Model file is too small (${modelSize / 1024}KB).
                    |
                    |The EmbeddingGemma model should be ~179MB.
                    |
                    |📥 Download the model:
                    |1. Visit: https://huggingface.co/litert-community/embeddinggemma-300m
                    |2. Download 'embeddinggemma_512.tflite' (179MB)
                    |3. Place in: app/src/main/assets/embeddinggemma_512.tflite
                    |
                    |Or use this command:
                    |cd app/src/main/assets
                    |curl -L -o embeddinggemma_512.tflite \
                    |  "https://huggingface.co/litert-community/embeddinggemma-300m/resolve/main/embeddinggemma_512.tflite"
                """.trimMargin()

                Log.e(TAG, error)
                return@withContext Result.failure(
                    IllegalStateException("Model file is invalid or not downloaded. See logs for instructions.")
                )
            }

            Log.d(TAG, "Model file size: ${modelSize / (1024 * 1024)}MB - looks good!")

            val options = Interpreter.Options()

            // Try NNAPI acceleration first (works with GPU/DSP/NPU)
            try {
                options.setUseNNAPI(true)
                useAccelerator = true
                Log.d(TAG, "NNAPI acceleration enabled")
            } catch (e: Exception) {
                Log.w(TAG, "NNAPI failed, using CPU optimization: ${e.message}")
                useAccelerator = false
            }
            
            // Configure CPU optimization
            options.setUseXNNPACK(true)
            options.setNumThreads(4)  // Optimal for most devices

            if (!useAccelerator) {
                Log.d(TAG, "Using CPU with XNNPACK (4 threads)")
            }
            
            // Load model from assets
            interpreter = Interpreter(modelBuffer, options)
            
            Log.d(TAG, "Model initialized successfully (Accelerator: $useAccelerator)")
            Result.success(Unit)
            
        } catch (e: IllegalArgumentException) {
            // This is the "not a valid flatbuffer" error
            val error = """
                |❌ Invalid TensorFlow Lite model file!
                |
                |Error: ${e.message}
                |
                |The model file is corrupted or incomplete.
                |
                |📥 Download the correct model:
                |1. Delete: app/src/main/assets/embeddinggemma_512.tflite
                |2. Visit: https://huggingface.co/litert-community/embeddinggemma-300m
                |3. Download 'embeddinggemma_512.tflite' (179MB)
                |4. Place in: app/src/main/assets/
                |
                |Or use this command in your terminal:
                |cd app/src/main/assets
                |rm embeddinggemma_512.tflite
                |curl -L -o embeddinggemma_512.tflite \
                |  "https://huggingface.co/litert-community/embeddinggemma-300m/resolve/main/embeddinggemma_512.tflite"
            """.trimMargin()

            Log.e(TAG, error, e)
            Result.failure(IllegalStateException("Model file is corrupted. See logs for download instructions.", e))

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize model", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate 768-dimensional embedding for text
     * 
     * @param text Input text (will be truncated to 512 tokens)
     * @param taskType Task type for prompt template
     * @return FloatArray of 768 dimensions
     */
    suspend fun generateEmbedding(
        text: String,
        taskType: TaskType = TaskType.DOCUMENT
    ): Result<FloatArray> = withContext(Dispatchers.Default) {
        try {
            val currentInterpreter = interpreter 
                ?: return@withContext Result.failure(
                    IllegalStateException("Model not initialized")
                )
            
            // Apply task-specific prompt template
            val promptedText = applyPromptTemplate(text, taskType)
            
            // Tokenize using proper SentencePiece tokenizer
            val (inputIds, attentionMask) = tokenizer.encode(promptedText, addSpecialTokens = true)

            // Prepare input tensors as 2D arrays
            val inputIdsArray = Array(1) { inputIds }
            val attentionMaskArray = Array(1) { attentionMask }

            // Prepare output tensor
            val output = Array(1) { FloatArray(EMBEDDING_DIM) }
            
            // Run inference
            val startTime = System.currentTimeMillis()
            currentInterpreter.runForMultipleInputsOutputs(
                arrayOf(inputIdsArray, attentionMaskArray),
                mapOf(0 to output)
            )
            val inferenceTime = System.currentTimeMillis() - startTime
            
            Log.d(TAG, "Inference completed in ${inferenceTime}ms (Accelerator: $useAccelerator)")

            // Normalize embedding
            val normalized = normalizeVector(output[0])
            Result.success(normalized)

        } catch (e: Exception) {
            Log.e(TAG, "Embedding generation failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Apply task-specific prompt template
     * Based on EmbeddingGemma training data
     */
    private fun applyPromptTemplate(text: String, taskType: TaskType): String {
        return when (taskType) {
            TaskType.SEARCH -> "task: search result | query: $text"
            TaskType.QUESTION_ANSWERING -> "task: question answering | query: $text"
            TaskType.DOCUMENT -> "title: none | text: $text"
        }
    }
    
    /**
     * Normalize vector to unit length
     */
    private fun normalizeVector(vector: FloatArray): FloatArray {
        val norm = sqrt(vector.sumOf { (it * it).toDouble() })
        return if (norm > 0.0) {
            vector.map { (it / norm).toFloat() }.toFloatArray()
        } else {
            vector
        }
    }
    
    /**
     * Truncate embedding to lower dimensions (Matryoshka)
     * Useful for memory optimization
     */
    fun truncateEmbedding(embedding: FloatArray, targetDim: Int): FloatArray {
        require(targetDim in listOf(128, 256, 512, 768)) {
            "Target dimension must be 128, 256, 512, or 768"
        }
        
        val truncated = embedding.sliceArray(0 until targetDim)
        
        // Normalize after truncation
        return normalizeVector(truncated)
    }
    
    /**
     * Calculate cosine similarity between two embeddings
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Embeddings must have same dimension" }
        
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator > 0.0) {
            (dotProduct / denominator).toFloat()
        } else {
            0f
        }
    }
    
    /**
     * Load model file from assets
     */
    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    
    /**
     * Release resources
     */
    fun close() {
        interpreter?.close()
        interpreter = null
        Log.d(TAG, "Model resources released")
    }
}
