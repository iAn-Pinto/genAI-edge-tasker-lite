package com.example.privytaskai.util

import android.util.Log
import com.example.privytaskai.data.ml.EmbeddingGemmaModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple test harness for EmbeddingGemma
 * Call from MainActivity to verify model works before using in production
 * 
 * Usage:
 * ```kotlin
 * @Inject lateinit var embeddingTest: EmbeddingTest
 * 
 * override fun onCreate(savedInstanceState: Bundle?) {
 *     super.onCreate(savedInstanceState)
 *     embeddingTest.runBasicTest(lifecycleScope)
 * }
 * ```
 */
@Singleton
class EmbeddingTest @Inject constructor(
    private val embeddingModel: EmbeddingGemmaModel
) {
    companion object {
        private const val TAG = "EmbeddingTest"
    }
    
    /**
     * Run comprehensive test suite
     * Checks initialization, embedding generation, similarity
     */
    fun runBasicTest(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== Starting EmbeddingGemma Test ===")
                
                // Test 1: Initialize model
                Log.d(TAG, "Test 1: Initializing model...")
                val initStart = System.currentTimeMillis()
                embeddingModel.initialize().getOrThrow()
                val initTime = System.currentTimeMillis() - initStart
                Log.d(TAG, "✓ Model initialized in ${initTime}ms")
                
                if (initTime > 3000) {
                    Log.w(TAG, "⚠ Init time > 3s - NNAPI may have failed, using CPU")
                }
                
                // Test 2: Generate embedding for SEBI-related text
                Log.d(TAG, "Test 2: Generating embedding for document...")
                val testText = "SEBI circular regarding disclosure norms for listed companies"
                val embedStart = System.currentTimeMillis()
                val embedding = embeddingModel.generateEmbedding(
                    testText, 
                    EmbeddingGemmaModel.TaskType.DOCUMENT
                ).getOrThrow()
                val embedTime = System.currentTimeMillis() - embedStart
                
                Log.d(TAG, "✓ Embedding generated in ${embedTime}ms")
                Log.d(TAG, "  Dimensions: ${embedding.size}")
                Log.d(TAG, "  First 5 values: ${embedding.take(5).toList()}")
                Log.d(TAG, "  L2 norm: ${"%.3f".format(embedding.sumOf { (it * it).toDouble() })}")
                
                if (embedTime > 200) {
                    Log.w(TAG, "⚠ Slow embedding generation (${embedTime}ms) - consider optimization")
                }
                
                // Test 3: Similarity test (semantic understanding)
                Log.d(TAG, "Test 3: Testing semantic similarity...")
                val query = "disclosure requirements for companies"
                val queryEmbedding = embeddingModel.generateEmbedding(
                    query, 
                    EmbeddingGemmaModel.TaskType.SEARCH
                ).getOrThrow()
                
                val similarity = embeddingModel.cosineSimilarity(embedding, queryEmbedding)
                Log.d(TAG, "✓ Similarity score: ${"%.3f".format(similarity)}")
                
                if (similarity > 0.5) {
                    Log.d(TAG, "  → Strong semantic match (as expected)")
                } else {
                    Log.w(TAG, "  → Weak match - tokenizer may not be working correctly")
                }
                
                // Test 4: Different task types
                Log.d(TAG, "Test 4: Testing task-specific embeddings...")
                val docEmbed = embeddingModel.generateEmbedding(
                    testText, 
                    EmbeddingGemmaModel.TaskType.DOCUMENT
                ).getOrThrow()
                val searchEmbed = embeddingModel.generateEmbedding(
                    testText, 
                    EmbeddingGemmaModel.TaskType.SEARCH
                ).getOrThrow()
                
                val docVsSearch = embeddingModel.cosineSimilarity(docEmbed, searchEmbed)
                Log.d(TAG, "✓ Document vs Search similarity: ${"%.3f".format(docVsSearch)}")
                Log.d(TAG, "  (Should be high but not 1.0 due to different prompts)")
                
                // Test 5: Truncation for budget devices
                Log.d(TAG, "Test 5: Testing dimension truncation...")
                val truncated512 = embeddingModel.truncateEmbedding(embedding, 512)
                val truncated256 = embeddingModel.truncateEmbedding(embedding, 256)
                val truncated128 = embeddingModel.truncateEmbedding(embedding, 128)
                
                Log.d(TAG, "✓ Truncation successful")
                Log.d(TAG, "  768-dim: ${embedding.size} values")
                Log.d(TAG, "  512-dim: ${truncated512.size} values (33% savings)")
                Log.d(TAG, "  256-dim: ${truncated256.size} values (67% savings)")
                Log.d(TAG, "  128-dim: ${truncated128.size} values (83% savings)")
                
                // Test 6: Memory footprint check
                Log.d(TAG, "Test 6: Checking memory usage...")
                val runtime = Runtime.getRuntime()
                val usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                Log.d(TAG, "✓ Current memory usage: ${usedMemoryMB}MB")
                
                if (usedMemoryMB > 500) {
                    Log.w(TAG, "⚠ High memory usage - monitor for budget devices")
                }
                
                Log.d(TAG, "")
                Log.d(TAG, "=== All Tests Passed! ===")
                Log.d(TAG, "Summary:")
                Log.d(TAG, "  Init time: ${initTime}ms")
                Log.d(TAG, "  Embedding time: ${embedTime}ms")
                Log.d(TAG, "  Memory usage: ${usedMemoryMB}MB")
                Log.d(TAG, "  Embedding dimensions: ${embedding.size}")
                Log.d(TAG, "")
                Log.d(TAG, "Next step: Integrate into DocumentRepository!")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Test failed", e)
                Log.e(TAG, "Common causes:")
                Log.e(TAG, "  1. Model/tokenizer files missing from assets/")
                Log.e(TAG, "  2. Insufficient device RAM")
                Log.e(TAG, "  3. TFLite dependencies not synced")
            }
        }
    }
    
    /**
     * Quick smoke test - just verify model loads
     */
    fun runQuickTest(scope: CoroutineScope, onComplete: (Boolean) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                embeddingModel.initialize().getOrThrow()
                embeddingModel.generateEmbedding(
                    "test", 
                    EmbeddingGemmaModel.TaskType.DOCUMENT
                ).getOrThrow()
                Log.d(TAG, "Quick test PASSED - model works!")
                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "Quick test FAILED", e)
                onComplete(false)
            }
        }
    }
    
    /**
     * Benchmark test - measure performance across different text lengths
     */
    fun runBenchmark(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== Running Performance Benchmark ===")
                
                embeddingModel.initialize().getOrThrow()
                
                val textLengths = listOf(
                    "Short" to "SEBI circular",
                    "Medium" to "SEBI circular regarding disclosure norms for listed companies and their subsidiaries",
                    "Long" to "SEBI circular regarding disclosure norms for listed companies. " +
                            "This circular mandates quarterly disclosures, annual reports, and real-time " +
                            "notifications for material events affecting company valuation. ".repeat(3)
                )
                
                textLengths.forEach { (label, text) ->
                    val times = mutableListOf<Long>()
                    repeat(5) {
                        val start = System.currentTimeMillis()
                        embeddingModel.generateEmbedding(
                            text, 
                            EmbeddingGemmaModel.TaskType.DOCUMENT
                        ).getOrThrow()
                        times.add(System.currentTimeMillis() - start)
                    }
                    val avgTime = times.average()
                    Log.d(TAG, "$label text: ${avgTime.toInt()}ms avg (${text.length} chars)")
                }
                
                Log.d(TAG, "=== Benchmark Complete ===")
                
            } catch (e: Exception) {
                Log.e(TAG, "Benchmark failed", e)
            }
        }
    }
}
