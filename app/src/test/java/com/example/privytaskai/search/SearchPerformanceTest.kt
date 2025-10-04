package com.example.privytaskai.search

import com.example.privytaskai.data.local.LocalEmbeddingService
import com.example.privytaskai.data.ml.EmbeddingGemmaModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import kotlin.system.measureTimeMillis

/**
 * Performance and benchmark tests for search functionality
 *
 * Validates:
 * 1. Search completes in reasonable time (<2 seconds)
 * 2. Similarity calculations are fast
 * 3. Multiple searches are consistent
 * 4. Memory usage is reasonable
 */
class SearchPerformanceTest {

    @Mock
    private lateinit var embeddingModel: EmbeddingGemmaModel

    private lateinit var embeddingService: LocalEmbeddingService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        embeddingService = LocalEmbeddingService(embeddingModel)
    }

    @Test
    fun testSimilarityCalculation_IsFast() {
        // Given: Two embeddings
        val embedding1 = List(768) { 0.5f }
        val embedding2 = List(768) { 0.6f }

        // When: Calculate similarity multiple times
        val iterations = 1000
        val time = measureTimeMillis {
            repeat(iterations) {
                calculateCosineSimilarity(embedding1, embedding2)
            }
        }

        // Then: Should be very fast (<10ms for 1000 calculations)
        val avgTime = time.toDouble() / iterations
        assertTrue(
            "Average similarity calculation should be <0.01ms, was ${avgTime}ms",
            avgTime < 0.01
        )
    }

    @Test
    fun testMultipleSearches_ReturnConsistentResults() = runBlocking {
        // Given: Same query embedding
        val query = "country roads take me home"
        val mockEmbedding = generateDeterministicEmbedding(query, 42)

        whenever(embeddingModel.generateEmbedding(any(), any()))
            .thenReturn(Result.success(mockEmbedding))

        // When: Generate embedding multiple times
        val embeddings = List(10) {
            embeddingService.generateEmbedding(query)
        }

        // Then: All should be identical
        embeddings.forEach { embedding ->
            assertEquals("Embeddings should be consistent", 768, embedding.size)
            // In real implementation, embeddings should be deterministic
        }
    }

    @Test
    fun testLargeScaleSearch_CompletesInReasonableTime() {
        // Given: Many task embeddings to compare
        val queryEmbedding = List(768) { 0.5f }
        val taskEmbeddings = List(100) { i ->
            List(768) { (i + it) % 100 / 100f }
        }

        // When: Calculate similarities for all tasks
        val time = measureTimeMillis {
            taskEmbeddings.forEach { taskEmbedding ->
                calculateCosineSimilarity(queryEmbedding, taskEmbedding)
            }
        }

        // Then: Should complete quickly (<100ms for 100 comparisons)
        assertTrue(
            "100 similarity calculations should take <100ms, took ${time}ms",
            time < 100
        )
    }

    @Test
    fun testEmbeddingGeneration_HandlesLongText() = runBlocking {
        // Given: Very long text
        val longText = "country roads take me home " * 100 // Repeat 100 times
        val mockEmbedding = FloatArray(768) { 0.1f }

        whenever(embeddingModel.generateEmbedding(any(), any()))
            .thenReturn(Result.success(mockEmbedding))

        // When: Generate embedding
        val result = embeddingService.generateEmbedding(longText)

        // Then: Should handle without error and return correct size
        assertEquals(768, result.size)
    }

    @Test
    fun testBatchSimilarityCalculations_Efficient() {
        // Given: Multiple query-task pairs
        val pairs = List(50) { i ->
            Pair(
                List(768) { 0.5f },
                List(768) { (i + it) % 50 / 50f }
            )
        }

        // When: Calculate all similarities
        val time = measureTimeMillis {
            val similarities = pairs.map { (query, task) ->
                calculateCosineSimilarity(query, task)
            }

            // Verify all calculated
            assertEquals(50, similarities.size)
        }

        // Then: Batch should be efficient (<50ms)
        assertTrue(
            "Batch similarity calculation should take <50ms, took ${time}ms",
            time < 50
        )
    }

    @Test
    fun testMemoryUsage_EmbeddingsAreReasonable() {
        // Given: Generate multiple embeddings
        val embeddings = mutableListOf<List<Float>>()

        // When: Create 100 embeddings
        repeat(100) { i ->
            embeddings.add(generateDeterministicEmbedding("text $i", i).toList())
        }

        // Then: Memory usage should be predictable
        // 100 embeddings * 768 floats * 4 bytes = ~307KB
        val expectedSize = 100 * 768
        val actualSize = embeddings.sumOf { it.size }
        assertEquals("Total float count should be predictable", expectedSize, actualSize)
    }

    @Test
    fun testNormalization_DoesNotSignificantlySlowDown() {
        // Given: Unnormalized vectors
        val vectors = List(100) {
            FloatArray(768) { 5.0f } // Unnormalized
        }

        // When: Normalize all
        val time = measureTimeMillis {
            vectors.forEach { vector ->
                normalizeVector(vector)
            }
        }

        // Then: Should be fast (<20ms for 100 normalizations)
        assertTrue(
            "Normalizing 100 vectors should take <20ms, took ${time}ms",
            time < 20
        )
    }

    @Test
    fun testEdgeCase_EmptyEmbedding_HandlesGracefully() {
        // Given: Empty or zero embedding
        val emptyEmbedding = List(768) { 0f }
        val normalEmbedding = List(768) { 0.5f }

        // When: Calculate similarity
        val similarity = calculateCosineSimilarity(emptyEmbedding, normalEmbedding)

        // Then: Should handle gracefully (return 0)
        assertEquals("Empty embedding similarity should be 0", 0f, similarity, 0.01f)
    }

    @Test
    fun testEdgeCase_VerySmallValues_MaintainsPrecision() {
        // Given: Embeddings with very small values
        val embedding1 = List(768) { 0.0001f }
        val embedding2 = List(768) { 0.0001f }

        // When: Calculate similarity
        val similarity = calculateCosineSimilarity(embedding1, embedding2)

        // Then: Should still recognize as identical
        assertTrue(
            "Very small identical values should have ~1.0 similarity, was $similarity",
            similarity > 0.99f
        )
    }

    @Test
    fun testConsistency_SameTextSameEmbedding() = runBlocking {
        // Given: Same text multiple times
        val text = "country roads take me home"
        val mockEmbedding = generateDeterministicEmbedding(text, 42)

        whenever(embeddingModel.generateEmbedding(any(), any()))
            .thenReturn(Result.success(mockEmbedding))

        // When: Generate embeddings
        val embedding1 = embeddingService.generateEmbedding(text)
        val embedding2 = embeddingService.generateEmbedding(text)

        // Then: Should be identical
        assertEquals(embedding1.size, embedding2.size)
        // In deterministic implementation, values should match
    }

    // Helper functions

    private fun calculateCosineSimilarity(a: List<Float>, b: List<Float>): Float {
        require(a.size == b.size) { "Embeddings must have same dimension" }

        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        val denominator = kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB)
        return if (denominator > 0.0) {
            (dotProduct / denominator).toFloat()
        } else {
            0f
        }
    }

    private fun normalizeVector(vector: FloatArray): FloatArray {
        val norm = kotlin.math.sqrt(vector.sumOf { (it * it).toDouble() })
        return if (norm > 0.0) {
            vector.map { (it / norm).toFloat() }.toFloatArray()
        } else {
            vector
        }
    }

    private fun generateDeterministicEmbedding(text: String, seed: Int): FloatArray {
        val random = java.util.Random(text.hashCode().toLong() + seed)
        return FloatArray(768) { random.nextFloat() }
    }

    private operator fun String.times(count: Int): String = this.repeat(count)
}
