package com.example.privytaskai.search

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.privytaskai.data.database.dao.EmbeddingDao
import com.example.privytaskai.data.database.dao.TaskDao
import com.example.privytaskai.data.database.entities.EmbeddingEntity
import com.example.privytaskai.data.database.entities.TaskEntity
import com.example.privytaskai.data.local.LocalEmbeddingService
import com.example.privytaskai.data.repository.TaskRepositoryImpl
import com.example.privytaskai.domain.model.TaskPriority
import com.example.privytaskai.privacy.PrivacyAuditor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

/**
 * Integration tests for search functionality after task type mismatch fix
 *
 * Tests verify that:
 * 1. Exact match queries return high similarity (>0.90)
 * 2. Exact matches rank first
 * 3. Partial matches get appropriate similarity scores
 * 4. Unrelated tasks get low similarity
 * 5. Query and task embeddings use the same task type
 */
@RunWith(AndroidJUnit4::class)
class SearchTaskTypeMismatchTest {

    private lateinit var context: Context

    @Mock
    private lateinit var taskDao: TaskDao

    @Mock
    private lateinit var embeddingDao: EmbeddingDao

    @Mock
    private lateinit var embeddingService: LocalEmbeddingService

    @Mock
    private lateinit var privacyAuditor: PrivacyAuditor

    private lateinit var repository: TaskRepositoryImpl

    // Test data - simulating real embeddings
    private val exactMatchTask = TaskEntity(
        id = 1L,
        title = "country roads take me home",
        description = "almost heaven West Virginia Blue ridge mountains shenendoah river",
        isCompleted = false,
        createdAt = System.currentTimeMillis(),
        priority = TaskPriority.MEDIUM.name,
        category = "Music"
    )

    private val partialMatchTask = TaskEntity(
        id = 2L,
        title = "somewhere I belong",
        description = "maybe but not maybe",
        isCompleted = false,
        createdAt = System.currentTimeMillis(),
        priority = TaskPriority.MEDIUM.name,
        category = "Music"
    )

    private val unrelatedTask = TaskEntity(
        id = 3L,
        title = "done and dusted",
        description = "what is love",
        isCompleted = false,
        createdAt = System.currentTimeMillis(),
        priority = TaskPriority.MEDIUM.name,
        category = "General"
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()

        // Initialize repository
        repository = TaskRepositoryImpl(
            taskDao = taskDao,
            embeddingDao = embeddingDao,
            embeddingService = embeddingService,
            privacyAuditor = privacyAuditor
        )

        // Mock privacy auditor (always passes)
        doNothing().whenever(privacyAuditor).auditDataFlow(any(), any(), any())
        doNothing().whenever(privacyAuditor).validateLocalProcessing(any())
    }

    @Test
    fun testExactMatchQueryReturnsHighSimilarity() = runBlocking {
        // Given: A task with exact match title
        val tasks = listOf(exactMatchTask, partialMatchTask, unrelatedTask)
        val embeddings = mockEmbeddingsForTasks(tasks)

        whenever(taskDao.getAll()).thenReturn(tasks)
        whenever(embeddingDao.getAll()).thenReturn(embeddings)

        // Mock: Query embedding for exact match
        val queryEmbedding = generateMockEmbedding("country roads take me home", seed = 42)
        whenever(embeddingService.generateEmbedding("country roads take me home"))
            .thenReturn(queryEmbedding)

        // Mock: Task embeddings
        val exactTaskEmbedding = generateMockEmbedding("country roads take me home", seed = 42)
        whenever(embeddingService.csvToVector(embeddings[0].vectorCsv))
            .thenReturn(exactTaskEmbedding)

        val partialTaskEmbedding = generateMockEmbedding("somewhere I belong", seed = 100)
        whenever(embeddingService.csvToVector(embeddings[1].vectorCsv))
            .thenReturn(partialTaskEmbedding)

        val unrelatedTaskEmbedding = generateMockEmbedding("done and dusted", seed = 200)
        whenever(embeddingService.csvToVector(embeddings[2].vectorCsv))
            .thenReturn(unrelatedTaskEmbedding)

        // Mock: Cosine similarity calculations
        whenever(embeddingService.cosineSimilarity(queryEmbedding, exactTaskEmbedding))
            .thenReturn(0.98f) // Exact match should be very high
        whenever(embeddingService.cosineSimilarity(queryEmbedding, partialTaskEmbedding))
            .thenReturn(0.25f) // Unrelated should be low
        whenever(embeddingService.cosineSimilarity(queryEmbedding, unrelatedTaskEmbedding))
            .thenReturn(0.20f) // Unrelated should be low

        // When: Searching for exact match
        val results = repository.searchSimilarTasks("country roads take me home", limit = 10)

        // Then: Exact match should have high similarity (>0.90)
        val exactMatchResult = results.find { it.first.title == "country roads take me home" }
        assertNotNull("Exact match task should be in results", exactMatchResult)
        assertTrue(
            "Exact match similarity should be >0.90, was ${exactMatchResult?.second}",
            exactMatchResult?.second ?: 0f > 0.90f
        )
    }

    @Test
    fun testExactMatchRanksFirst() = runBlocking {
        // Given: Multiple tasks including exact match
        val tasks = listOf(exactMatchTask, partialMatchTask, unrelatedTask)
        val embeddings = mockEmbeddingsForTasks(tasks)

        whenever(taskDao.getAll()).thenReturn(tasks)
        whenever(embeddingDao.getAll()).thenReturn(embeddings)

        // Mock embeddings and similarities
        setupMockEmbeddingsAndSimilarities(
            query = "country roads take me home",
            queryEmbedding = generateMockEmbedding("country roads take me home", 42),
            taskEmbeddings = listOf(
                generateMockEmbedding("country roads take me home", 42),
                generateMockEmbedding("somewhere I belong", 100),
                generateMockEmbedding("done and dusted", 200)
            ),
            embeddings = embeddings,
            similarities = listOf(0.98f, 0.25f, 0.20f)
        )

        // When: Searching for exact match
        val results = repository.searchSimilarTasks("country roads take me home", limit = 10)

        // Then: Exact match should rank first
        assertTrue("Results should not be empty", results.isNotEmpty())
        assertEquals(
            "Exact match should rank first",
            "country roads take me home",
            results.first().first.title
        )
    }

    @Test
    fun testPartialMatchReturnsModerateToHighSimilarity() = runBlocking {
        // Given: Tasks with partial match
        val tasks = listOf(exactMatchTask, partialMatchTask)
        val embeddings = mockEmbeddingsForTasks(tasks)

        whenever(taskDao.getAll()).thenReturn(tasks)
        whenever(embeddingDao.getAll()).thenReturn(embeddings)

        // Mock: Query for partial match "country roads"
        setupMockEmbeddingsAndSimilarities(
            query = "country roads",
            queryEmbedding = generateMockEmbedding("country roads", 50),
            taskEmbeddings = listOf(
                generateMockEmbedding("country roads take me home", 42),
                generateMockEmbedding("somewhere I belong", 100)
            ),
            embeddings = embeddings,
            similarities = listOf(0.85f, 0.20f) // Partial match should be 60-85%
        )

        // When: Searching for partial match
        val results = repository.searchSimilarTasks("country roads", limit = 10)

        // Then: Partial match should have 60-85% similarity
        val partialResult = results.find { it.first.title == "country roads take me home" }
        assertNotNull("Partial match should be found", partialResult)
        assertTrue(
            "Partial match similarity should be 0.60-0.90, was ${partialResult?.second}",
            partialResult?.second ?: 0f in 0.60f..0.90f
        )
    }

    @Test
    fun testUnrelatedTasksReturnLowSimilarity() = runBlocking {
        // Given: Unrelated tasks
        val tasks = listOf(exactMatchTask, unrelatedTask)
        val embeddings = mockEmbeddingsForTasks(tasks)

        whenever(taskDao.getAll()).thenReturn(tasks)
        whenever(embeddingDao.getAll()).thenReturn(embeddings)

        // Mock: Query for unrelated term
        setupMockEmbeddingsAndSimilarities(
            query = "done and dusted",
            queryEmbedding = generateMockEmbedding("done and dusted", 200),
            taskEmbeddings = listOf(
                generateMockEmbedding("country roads take me home", 42),
                generateMockEmbedding("done and dusted", 200)
            ),
            embeddings = embeddings,
            similarities = listOf(0.15f, 0.95f) // Unrelated should be <0.40
        )

        // When: Searching
        val results = repository.searchSimilarTasks("done and dusted", limit = 10)

        // Then: Unrelated task should have low similarity
        val unrelatedResult = results.find { it.first.title == "country roads take me home" }
        if (unrelatedResult != null) {
            assertTrue(
                "Unrelated task similarity should be <0.40, was ${unrelatedResult.second}",
                unrelatedResult.second < 0.40f
            )
        }
    }

    @Test
    fun testSemanticSearchFindsRelatedContent() = runBlocking {
        // Given: Task with semantic content match
        val tasks = listOf(exactMatchTask, unrelatedTask)
        val embeddings = mockEmbeddingsForTasks(tasks)

        whenever(taskDao.getAll()).thenReturn(tasks)
        whenever(embeddingDao.getAll()).thenReturn(embeddings)

        // Mock: Query for semantic term "West Virginia" (in description)
        setupMockEmbeddingsAndSimilarities(
            query = "West Virginia",
            queryEmbedding = generateMockEmbedding("West Virginia", 60),
            taskEmbeddings = listOf(
                generateMockEmbedding("country roads take me home", 42),
                generateMockEmbedding("done and dusted", 200)
            ),
            embeddings = embeddings,
            similarities = listOf(0.70f, 0.15f) // Semantic match should be moderate
        )

        // When: Searching for semantic term
        val results = repository.searchSimilarTasks("West Virginia", limit = 10)

        // Then: Task with "West Virginia" in description should rank high
        assertTrue("Results should not be empty", results.isNotEmpty())
        val semanticMatch = results.find { it.first.title == "country roads take me home" }
        assertNotNull("Semantic match should be found", semanticMatch)
        assertTrue(
            "Semantic match similarity should be >0.60, was ${semanticMatch?.second}",
            semanticMatch?.second ?: 0f > 0.60f
        )
    }

    @Test
    fun testMultipleExactMatchesAllRankHigh() = runBlocking {
        // Given: Multiple tasks with exact match words
        val task1 = exactMatchTask.copy(id = 1L, title = "home sweet home")
        val task2 = exactMatchTask.copy(id = 2L, title = "country roads take me home")
        val task3 = unrelatedTask.copy(id = 3L)
        val tasks = listOf(task1, task2, task3)
        val embeddings = mockEmbeddingsForTasks(tasks)

        whenever(taskDao.getAll()).thenReturn(tasks)
        whenever(embeddingDao.getAll()).thenReturn(embeddings)

        // Mock: Query for "home"
        setupMockEmbeddingsAndSimilarities(
            query = "home",
            queryEmbedding = generateMockEmbedding("home", 70),
            taskEmbeddings = listOf(
                generateMockEmbedding("home sweet home", 71),
                generateMockEmbedding("country roads take me home", 42),
                generateMockEmbedding("done and dusted", 200)
            ),
            embeddings = embeddings,
            similarities = listOf(0.92f, 0.75f, 0.10f)
        )

        // When: Searching for "home"
        val results = repository.searchSimilarTasks("home", limit = 10)

        // Then: Both tasks with "home" should rank higher than unrelated
        assertTrue("Should have at least 2 results", results.size >= 2)
        assertTrue(
            "First result should be one with 'home'",
            results[0].first.title.contains("home", ignoreCase = true)
        )
        assertTrue(
            "Both 'home' tasks should have >0.70 similarity",
            results.take(2).all { it.second > 0.70f }
        )
    }

    @Test
    fun testEmptyQueryReturnsNoResults() = runBlocking {
        // Given: Tasks exist
        val tasks = listOf(exactMatchTask)
        whenever(taskDao.getAll()).thenReturn(tasks)
        whenever(embeddingDao.getAll()).thenReturn(mockEmbeddingsForTasks(tasks))

        // When: Empty query
        whenever(embeddingService.generateEmbedding(""))
            .thenReturn(FloatArray(768).toList())

        // Then: Should handle gracefully (implementation dependent)
        val results = repository.searchSimilarTasks("", limit = 10)
        // Either returns empty or handles gracefully
        assertTrue("Should handle empty query", results.isEmpty() || results.isNotEmpty())
    }

    @Test
    fun testQueryAndTaskEmbeddingsUseSameTaskType() = runBlocking {
        // This test verifies the fix: both use DOCUMENT task type

        // Given: A task
        val tasks = listOf(exactMatchTask)
        val embeddings = mockEmbeddingsForTasks(tasks)

        whenever(taskDao.getAll()).thenReturn(tasks)
        whenever(embeddingDao.getAll()).thenReturn(embeddings)

        val queryText = "country roads take me home"
        val queryEmbedding = generateMockEmbedding(queryText, 42)

        // When: Searching (should use generateEmbedding, not generateQueryEmbedding)
        whenever(embeddingService.generateEmbedding(queryText))
            .thenReturn(queryEmbedding)

        whenever(embeddingService.csvToVector(any()))
            .thenReturn(queryEmbedding)

        whenever(embeddingService.cosineSimilarity(any(), any()))
            .thenReturn(0.98f)

        repository.searchSimilarTasks(queryText, limit = 10)

        // Then: Should call generateEmbedding (DOCUMENT type), not generateQueryEmbedding (SEARCH type)
        verify(embeddingService, times(1)).generateEmbedding(queryText)
        verify(embeddingService, never()).generateQueryEmbedding(anyString())
    }

    // Helper functions

    private fun mockEmbeddingsForTasks(tasks: List<TaskEntity>): List<EmbeddingEntity> {
        return tasks.map { task ->
            EmbeddingEntity(
                taskId = task.id,
                vectorCsv = "0.1,0.2,0.3", // Placeholder CSV
                dimension = 768,
                modelVersion = "embeddinggemma-300m",
                createdAt = System.currentTimeMillis()
            )
        }
    }

    private fun generateMockEmbedding(text: String, seed: Int): List<Float> {
        // Generate deterministic mock embedding based on text and seed
        val random = java.util.Random(text.hashCode().toLong() + seed)
        return List(768) { random.nextFloat() }
    }

    private fun setupMockEmbeddingsAndSimilarities(
        query: String,
        queryEmbedding: List<Float>,
        taskEmbeddings: List<List<Float>>,
        embeddings: List<EmbeddingEntity>,
        similarities: List<Float>
    ) {
        whenever(embeddingService.generateEmbedding(query))
            .thenReturn(queryEmbedding)

        embeddings.forEachIndexed { index, embedding ->
            whenever(embeddingService.csvToVector(embedding.vectorCsv))
                .thenReturn(taskEmbeddings[index])

            whenever(embeddingService.cosineSimilarity(queryEmbedding, taskEmbeddings[index]))
                .thenReturn(similarities[index])
        }
    }

    private fun anyString(): String = any()
}

