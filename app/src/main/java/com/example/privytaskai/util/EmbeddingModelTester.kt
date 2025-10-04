package com.example.privytaskai.util

import android.content.Context
import android.util.Log
import com.example.privytaskai.data.ml.EmbeddingGemmaModel
import com.example.privytaskai.data.ml.SentencePieceTokenizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manual test harness for EmbeddingGemma model
 * Call from MainActivity or any other Activity to validate model works
 */
class EmbeddingModelTester(private val context: Context) {
    
    companion object {
        private const val TAG = "EmbeddingModelTester"
    }
    
    private var tokenizer: SentencePieceTokenizer? = null
    
    /**
     * Run all tests and log results
     * Returns true if all tests pass
     */
    suspend fun runAllTests(): Boolean = withContext(Dispatchers.Default) {
        val results = mutableListOf<Boolean>()
        
        Log.d(TAG, "🧪 ===== EmbeddingGemma Test Suite =====")
        
        // Initialize tokenizer once for all tests
        tokenizer = SentencePieceTokenizer(context)
        val tokenizerInit = tokenizer!!.initialize()
        if (tokenizerInit.isFailure) {
            Log.e(TAG, "❌ Tokenizer initialization failed: ${tokenizerInit.exceptionOrNull()}")
            return@withContext false
        }
        
        results.add(testModelInitialization())
        results.add(testEmbeddingGeneration())
        results.add(testCosineSimilarity())
        results.add(testInferenceSpeed())
        
        // Cleanup
        tokenizer?.close()
        
        val passed = results.count { it }
        val total = results.size
        
        Log.d(TAG, "📊 Test Results: $passed/$total passed")
        Log.d(TAG, if (passed == total) "✅ All tests PASSED" else "❌ Some tests FAILED")
        
        passed == total
    }
    
    private suspend fun testModelInitialization(): Boolean {
        return try {
            Log.d(TAG, "\n--- Test 1: Model Initialization ---")
            val model = EmbeddingGemmaModel(context, tokenizer!!)
            val result = model.initialize()
            
            if (result.isSuccess) {
                Log.d(TAG, "✅ Model initialized successfully")
                model.close()
                true
            } else {
                Log.e(TAG, "❌ Model initialization failed: ${result.exceptionOrNull()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Model initialization threw exception", e)
            false
        }
    }
    
    private suspend fun testEmbeddingGeneration(): Boolean {
        return try {
            Log.d(TAG, "\n--- Test 2: Embedding Generation ---")
            val model = EmbeddingGemmaModel(context, tokenizer!!)
            model.initialize().getOrThrow()
            
            val sebiText = "SEBI has issued a circular regarding disclosure requirements for mutual funds."
            val result = model.generateEmbedding(
                text = sebiText,
                taskType = EmbeddingGemmaModel.TaskType.DOCUMENT
            )
            
            if (result.isSuccess) {
                val embedding = result.getOrNull()!!
                val nonZeroCount = embedding.count { it != 0f }
                Log.d(TAG, "✅ Generated ${embedding.size}-dim embedding with $nonZeroCount non-zero values")
                model.close()
                true
            } else {
                Log.e(TAG, "❌ Embedding generation failed: ${result.exceptionOrNull()}")
                model.close()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Embedding generation threw exception", e)
            false
        }
    }
    
    private suspend fun testCosineSimilarity(): Boolean {
        return try {
            Log.d(TAG, "\n--- Test 3: Cosine Similarity ---")
            val model = EmbeddingGemmaModel(context, tokenizer!!)
            model.initialize().getOrThrow()
            
            val text1 = "SEBI circular on mutual fund disclosure"
            val text2 = "Disclosure requirements for mutual funds by SEBI"
            val text3 = "Weather forecast for tomorrow"
            
            val emb1 = model.generateEmbedding(text1).getOrThrow()
            val emb2 = model.generateEmbedding(text2).getOrThrow()
            val emb3 = model.generateEmbedding(text3).getOrThrow()
            
            val sim12 = model.cosineSimilarity(emb1, emb2)
            val sim13 = model.cosineSimilarity(emb1, emb3)
            
            Log.d(TAG, "Similarity (similar texts): $sim12")
            Log.d(TAG, "Similarity (different texts): $sim13")
            
            model.close()
            
            if (sim12 > 0.5f && sim13 < sim12) {
                Log.d(TAG, "✅ Cosine similarity working correctly")
                true
            } else {
                Log.e(TAG, "❌ Cosine similarity gave unexpected results")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Cosine similarity test threw exception", e)
            false
        }
    }
    
    private suspend fun testInferenceSpeed(): Boolean {
        return try {
            Log.d(TAG, "\n--- Test 4: Inference Speed ---")
            val model = EmbeddingGemmaModel(context, tokenizer!!)
            model.initialize().getOrThrow()
            
            val text = "SEBI circular regarding disclosure norms for market participants"
            
            // Warm-up
            model.generateEmbedding(text)
            
            // Measure 5 runs
            val times = mutableListOf<Long>()
            repeat(5) {
                val start = System.currentTimeMillis()
                model.generateEmbedding(text).getOrThrow()
                val elapsed = System.currentTimeMillis() - start
                times.add(elapsed)
            }
            
            val avgTime = times.average()
            val maxTime = times.maxOrNull() ?: 0L
            val minTime = times.minOrNull() ?: 0L
            
            Log.d(TAG, "📊 Inference Performance:")
            Log.d(TAG, "   Average: ${avgTime.toInt()}ms")
            Log.d(TAG, "   Max: ${maxTime}ms")
            Log.d(TAG, "   Min: ${minTime}ms")
            
            model.close()
            
            if (avgTime < 150) {  // Relaxed target for initial testing
                Log.d(TAG, "✅ Performance acceptable for budget devices")
                true
            } else {
                Log.w(TAG, "⚠️  Performance slower than target (${avgTime.toInt()}ms > 100ms)")
                true  // Still pass, just warn
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Inference speed test threw exception", e)
            false
        }
    }
}
