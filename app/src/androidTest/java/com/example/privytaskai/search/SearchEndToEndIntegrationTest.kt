package com.example.privytaskai.search

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.privytaskai.data.database.database.TaskDatabase
import com.example.privytaskai.data.local.LocalEmbeddingService
import com.example.privytaskai.data.ml.EmbeddingGemmaModel
import com.example.privytaskai.data.ml.SentencePieceTokenizer
import com.example.privytaskai.data.repository.TaskRepositoryImpl
import com.example.privytaskai.domain.model.Task
import com.example.privytaskai.domain.model.TaskPriority
import com.example.privytaskai.privacy.PrivacyAuditor
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * End-to-end integration tests for search functionality
 *
 * These tests use the REAL EmbeddingGemma model to validate:
 * 1. Exact matches get high similarity scores (>0.90)
 * 2. Exact matches rank first in results
 * 3. Partial matches work correctly
 * 4. Semantic search works (finding related content)
 * 5. Query and task embeddings are in the same space
 *
 * Tests multiple real-world scenarios with actual ML model inference.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SearchEndToEndIntegrationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var database: TaskDatabase

    @Inject
    lateinit var embeddingService: LocalEmbeddingService

    @Inject
    lateinit var repository: TaskRepositoryImpl

    @Inject
    lateinit var privacyAuditor: PrivacyAuditor

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()

        // Clear database before each test
        runBlocking {
            database.clearAllTables()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            database.clearAllTables()
        }
        database.close()
    }

    @Test
    fun testExactMatchQuery_CountryRoads_RanksFirst() = runBlocking {
        // Given: Create tasks with real embeddings
        val task1 = createTaskWithTitle(
            title = "country roads take me home",
            description = "almost heaven West Virginia Blue ridge mountains shenandoah river"
        )
        val task2 = createTaskWithTitle(
            title = "somewhere I belong",
            description = "maybe but not maybe"
        )
        val task3 = createTaskWithTitle(
            title = "done and dusted",
            description = "what is love"
        )

        repository.insertTask(task1)
        repository.insertTask(task2)
        repository.insertTask(task3)

        // When: Search for exact match
        val results = repository.searchSimilarTasks("country roads take me home", limit = 10)

        // Then: Exact match should rank first with high similarity
        assertTrue("Should have results", results.isNotEmpty())
        assertEquals(
            "Exact match should rank first",
            "country roads take me home",
            results.first().first.title
        )

        val topSimilarity = results.first().second
        assertTrue(
            "Exact match similarity should be >0.85, was $topSimilarity",
            topSimilarity > 0.85f
        )

        // Other tasks should have lower similarity
        val otherTasks = results.drop(1)
        otherTasks.forEach { (task, similarity) ->
            assertTrue(
                "Unrelated task '${task.title}' should have <0.50 similarity, was $similarity",
                similarity < 0.50f
            )
        }
    }

    @Test
    fun testPartialMatchQuery_CountryRoads_FindsFullTitle() = runBlocking {
        // Given: Tasks
        val task1 = createTaskWithTitle(
            title = "country roads take me home",
            description = "John Denver song about West Virginia"
        )
        val task2 = createTaskWithTitle(
            title = "city streets",
            description = "urban environment"
        )

        repository.insertTask(task1)
        repository.insertTask(task2)

        // When: Search with partial match
        val results = repository.searchSimilarTasks("country roads", limit = 10)

        // Then: Task with matching words should rank first
        assertTrue("Should have results", results.isNotEmpty())
        assertEquals(
            "Partial match should rank first",
            "country roads take me home",
            results.first().first.title
        )

        val similarity = results.first().second
        assertTrue(
            "Partial match should have >0.70 similarity, was $similarity",
            similarity > 0.70f
        )
    }

    @Test
    fun testSemanticSearch_WestVirginia_FindsCountryRoads() = runBlocking {
        // Given: Task with semantic content
        val task1 = createTaskWithTitle(
            title = "country roads take me home",
            description = "almost heaven West Virginia Blue ridge mountains shenandoah river"
        )
        val task2 = createTaskWithTitle(
            title = "done and dusted",
            description = "what is love"
        )

        repository.insertTask(task1)
        repository.insertTask(task2)

        // When: Search for semantic term in description
        val results = repository.searchSimilarTasks("West Virginia", limit = 10)

        // Then: Task mentioning West Virginia should rank high
        assertTrue("Should have results", results.isNotEmpty())

        val countryRoadsResult = results.find {
            it.first.title == "country roads take me home"
        }
        assertNotNull("Should find country roads task", countryRoadsResult)

        val similarity = countryRoadsResult?.second ?: 0f
        assertTrue(
            "Semantic match should have >0.60 similarity, was $similarity",
            similarity > 0.60f
        )
    }

    @Test
    fun testMultipleKeywordSearch_Home_RanksAllWithHome() = runBlocking {
        // Given: Multiple tasks with "home"
        val task1 = createTaskWithTitle(
            title = "home sweet home",
            description = "there's no place like home"
        )
        val task2 = createTaskWithTitle(
            title = "country roads take me home",
            description = "West Virginia mountain mama"
        )
        val task3 = createTaskWithTitle(
            title = "work assignment",
            description = "office project"
        )

        repository.insertTask(task1)
        repository.insertTask(task2)
        repository.insertTask(task3)

        // When: Search for "home"
        val results = repository.searchSimilarTasks("home", limit = 10)

        // Then: Both tasks with "home" should rank higher than unrelated
        assertTrue("Should have at least 2 results", results.size >= 2)

        val topTwo = results.take(2)
        topTwo.forEach { (task, similarity) ->
            assertTrue(
                "Task with 'home' should have >0.65 similarity, '${task.title}' had $similarity",
                similarity > 0.65f
            )
            assertTrue(
                "Top results should contain 'home' in title",
                task.title.contains("home", ignoreCase = true)
            )
        }

        // Work assignment should rank low
        val workTask = results.find { it.first.title == "work assignment" }
        if (workTask != null) {
            assertTrue(
                "Unrelated task should have <0.50 similarity, was ${workTask.second}",
                workTask.second < 0.50f
            )
        }
    }

    @Test
    fun testSingleWordQuery_Country_FindsCountryRoads() = runBlocking {
        // Given: Tasks
        val task1 = createTaskWithTitle(
            title = "country roads take me home",
            description = "rural song"
        )
        val task2 = createTaskWithTitle(
            title = "city life",
            description = "urban living"
        )

        repository.insertTask(task1)
        repository.insertTask(task2)

        // When: Search with single word
        val results = repository.searchSimilarTasks("country", limit = 10)

        // Then: Should find country roads
        assertTrue("Should have results", results.isNotEmpty())

        val countryRoadsTask = results.find {
            it.first.title.contains("country")
        }
        assertNotNull("Should find task with 'country'", countryRoadsTask)
        assertTrue(
            "Single word match should have >0.70 similarity, was ${countryRoadsTask?.second}",
            countryRoadsTask?.second ?: 0f > 0.70f
        )
    }

    @Test
    fun testLongPhraseQuery_MatchesExactly() = runBlocking {
        // Given: Task with long description
        val task1 = createTaskWithTitle(
            title = "country roads take me home",
            description = "almost heaven West Virginia Blue ridge mountains shenandoah river life is old there older than the trees"
        )
        val task2 = createTaskWithTitle(
            title = "random task",
            description = "random description"
        )

        repository.insertTask(task1)
        repository.insertTask(task2)

        // When: Search with long phrase from description
        val results = repository.searchSimilarTasks(
            "almost heaven West Virginia Blue ridge mountains",
            limit = 10
        )

        // Then: Should find the matching task first
        assertTrue("Should have results", results.isNotEmpty())
        assertEquals(
            "Task with matching description should rank first",
            "country roads take me home",
            results.first().first.title
        )

        assertTrue(
            "Long phrase match should have >0.80 similarity, was ${results.first().second}",
            results.first().second > 0.80f
        )
    }

    @Test
    fun testCaseInsensitiveSearch_UPPERCASE_FindsLowercase() = runBlocking {
        // Given: Task with lowercase title
        val task = createTaskWithTitle(
            title = "country roads take me home",
            description = "song lyrics"
        )
        repository.insertTask(task)

        // When: Search with UPPERCASE
        val results = repository.searchSimilarTasks("COUNTRY ROADS TAKE ME HOME", limit = 10)

        // Then: Should still find the task
        assertTrue("Should have results", results.isNotEmpty())
        assertEquals(
            "Case insensitive search should work",
            "country roads take me home",
            results.first().first.title
        )
        assertTrue(
            "Case insensitive match should have >0.85 similarity, was ${results.first().second}",
            results.first().second > 0.85f
        )
    }

    @Test
    fun testSymbolsAndPunctuation_StillFindsMatches() = runBlocking {
        // Given: Task
        val task = createTaskWithTitle(
            title = "country roads take me home",
            description = "test"
        )
        repository.insertTask(task)

        // When: Search with punctuation
        val results = repository.searchSimilarTasks("country roads, take me home!", limit = 10)

        // Then: Should still match despite punctuation
        assertTrue("Should have results", results.isNotEmpty())
        assertEquals(
            "Should find task despite punctuation",
            "country roads take me home",
            results.first().first.title
        )
    }

    @Test
    fun testRankingOrder_HigherSimilarityFirst() = runBlocking {
        // Given: Multiple tasks with varying similarity
        val tasks = listOf(
            createTaskWithTitle("country roads take me home", "exact match"),
            createTaskWithTitle("country roads", "partial match"),
            createTaskWithTitle("roads", "single word match"),
            createTaskWithTitle("unrelated task", "completely different")
        )
        tasks.forEach { repository.insertTask(it) }

        // When: Search
        val results = repository.searchSimilarTasks("country roads take me home", limit = 10)

        // Then: Results should be in descending similarity order
        assertTrue("Should have multiple results", results.size >= 2)

        for (i in 0 until results.size - 1) {
            assertTrue(
                "Similarity should be in descending order: ${results[i].second} >= ${results[i + 1].second}",
                results[i].second >= results[i + 1].second
            )
        }
    }

    @Test
    fun testTopKLimit_ReturnsCorrectNumber() = runBlocking {
        // Given: 5 tasks
        repeat(5) { i ->
            repository.insertTask(
                createTaskWithTitle("task $i", "description $i")
            )
        }

        // When: Search with limit=3
        val results = repository.searchSimilarTasks("task", limit = 3)

        // Then: Should return exactly 3 results
        assertEquals("Should return limited number of results", 3, results.size)
    }

    @Test
    fun testNoMatches_ReturnsLowSimilarityTasks() = runBlocking {
        // Given: Tasks completely unrelated to query
        val task1 = createTaskWithTitle(
            title = "quantum physics",
            description = "particle behavior"
        )
        val task2 = createTaskWithTitle(
            title = "cooking recipes",
            description = "baking instructions"
        )
        repository.insertTask(task1)
        repository.insertTask(task2)

        // When: Search for completely unrelated term
        val results = repository.searchSimilarTasks("country roads take me home", limit = 10)

        // Then: Should return results but with low similarity
        assertTrue("Should return some results", results.isNotEmpty())
        results.forEach { (task, similarity) ->
            assertTrue(
                "Unrelated tasks should have <0.40 similarity, '${task.title}' had $similarity",
                similarity < 0.40f
            )
        }
    }

    @Test
    fun testDifferentCategories_StillSearchable() = runBlocking {
        // Given: Tasks in different categories
        val task1 = createTaskWithTitle(
            title = "country roads take me home",
            description = "music",
            category = "Entertainment"
        )
        val task2 = createTaskWithTitle(
            title = "country roads maintenance",
            description = "infrastructure",
            category = "Work"
        )
        repository.insertTask(task1)
        repository.insertTask(task2)

        // When: Search
        val results = repository.searchSimilarTasks("country roads", limit = 10)

        // Then: Both should be found regardless of category
        assertEquals("Should find both tasks", 2, results.size)
        assertTrue(
            "Both should have >0.70 similarity",
            results.all { it.second > 0.70f }
        )
    }

    // Helper function to create task with embedding
    private fun createTaskWithTitle(
        title: String,
        description: String,
        category: String = "General"
    ): Task {
        val embedding = embeddingService.generateEmbedding("$title\n$description")
        return Task(
            title = title,
            description = description,
            embedding = embedding,
            priority = TaskPriority.MEDIUM,
            category = category
        )
    }
}

