package com.example.privytaskai.domain.usecase

import android.util.Log
import com.example.privytaskai.domain.model.SearchResult
import com.example.privytaskai.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for semantic search over SEBI documents
 *
 * Flow:
 * 1. Generate query embedding (uses QUESTION_ANSWERING task type)
 * 2. Vector search → Find top-k similar chunks
 * 3. Rank by relevance → Return sorted results
 */
class SearchDocumentsUseCase @Inject constructor(
    private val documentRepository: DocumentRepository
) {
    companion object {
        private const val TAG = "SearchDocumentsUseCase"
        private const val DEFAULT_TOP_K = 10
        private const val MIN_SIMILARITY_THRESHOLD = 0.5f
    }

    /**
     * Execute a semantic search
     *
     * @param query Search query text
     * @param topK Number of results to return
     * @param minScore Minimum similarity score (0-1)
     * @return List of search results sorted by relevance
     */
    @Suppress("unused") // Public API for search functionality
    suspend fun execute(
        query: String,
        topK: Int = DEFAULT_TOP_K,
        minScore: Float = MIN_SIMILARITY_THRESHOLD
    ): Result<List<SearchResult>> = withContext(Dispatchers.Default) {
        try {
            if (query.isBlank()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Query cannot be empty")
                )
            }

            Log.d(TAG, "Searching: '$query' (top-$topK)")

            // Perform vector search
            val results = documentRepository.searchSimilar(
                query = query,
                limit = topK
            )

            // Filter by minimum score
            val filtered = results.filter { it.score >= minScore }

            Log.d(TAG, "Found ${filtered.size} results (${results.size} before filtering)")

            Result.success(filtered)

        } catch (e: Exception) {
            Log.e(TAG, "Search failed", e)
            Result.failure(e)
        }
    }

    /**
     * Get search suggestions based on query
     * Returns document titles that match
     */
    @Suppress("unused") // Public API for search suggestions
    suspend fun getSuggestions(
        query: String,
        limit: Int = 5
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val documents = documentRepository.searchByKeyword(query)
            val titles = documents.take(limit).map { it.title }
            Result.success(titles)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get suggestions", e)
            Result.failure(e)
        }
    }
}
