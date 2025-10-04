package com.example.privytaskai.util

import android.content.Context
import android.util.Log
import com.example.privytaskai.data.database.dao.EmbeddingDao
import com.example.privytaskai.data.database.dao.TaskDao
import com.example.privytaskai.data.database.entities.EmbeddingEntity
import com.example.privytaskai.data.local.LocalEmbeddingService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper to regenerate embeddings for all existing tasks
 *
 * Use this to migrate from old 64-dim embeddings to new 768-dim EmbeddingGemma embeddings
 */
@Singleton
class EmbeddingMigrationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val embeddingDao: EmbeddingDao,
    private val embeddingService: LocalEmbeddingService
) {
    companion object {
        private const val TAG = "EmbeddingMigration"
    }

    /**
     * Regenerate embeddings for all tasks
     * This should be called once after upgrading to EmbeddingGemma
     */
    suspend fun regenerateAllEmbeddings(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Starting embedding regeneration for all tasks...")

            val allTasks = taskDao.getAll()
            var successCount = 0
            var failureCount = 0

            allTasks.forEach { task ->
                try {
                    // Generate new 768-dim embedding
                    val text = "${task.title}\n${task.description}"
                    val embedding = embeddingService.generateEmbedding(text)

                    // Check if embedding is valid (not all zeros)
                    if (embedding.any { it != 0f }) {
                        val embeddingEntity = EmbeddingEntity(
                            taskId = task.id,
                            vectorCsv = embeddingService.vectorToCsv(embedding),
                            dimension = 768,
                            modelVersion = "embeddinggemma-300m",
                            createdAt = System.currentTimeMillis()
                        )

                        embeddingDao.upsert(embeddingEntity)
                        successCount++
                        Log.d(TAG, "✅ Regenerated embedding for: ${task.title}")
                    } else {
                        Log.w(TAG, "⚠️ Got zero embedding for: ${task.title}")
                        failureCount++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to regenerate embedding for: ${task.title}", e)
                    failureCount++
                }
            }

            Log.d(TAG, "✅ Embedding regeneration complete!")
            Log.d(TAG, "   Success: $successCount")
            Log.d(TAG, "   Failures: $failureCount")

            Result.success(successCount)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to regenerate embeddings", e)
            Result.failure(e)
        }
    }

    /**
     * Check if embeddings need migration
     * Returns true if any task has old 64-dim embeddings or no embeddings
     */
    suspend fun needsMigration(): Boolean = withContext(Dispatchers.IO) {
        try {
            val allTasks = taskDao.getAll()
            val embeddings = embeddingDao.getAll().associateBy { it.taskId }

            // Check if any task is missing embeddings
            val missingEmbeddings = allTasks.any { task ->
                embeddings[task.id] == null
            }

            // Check if any embedding is old 64-dim version
            val hasOldEmbeddings = embeddings.values.any { embedding ->
                embedding.dimension != 768 || embedding.modelVersion != "embeddinggemma-300m"
            }

            val needsMigration = missingEmbeddings || hasOldEmbeddings

            Log.d(TAG, "Migration check:")
            Log.d(TAG, "  Total tasks: ${allTasks.size}")
            Log.d(TAG, "  Total embeddings: ${embeddings.size}")
            Log.d(TAG, "  Missing embeddings: $missingEmbeddings")
            Log.d(TAG, "  Has old embeddings: $hasOldEmbeddings")
            Log.d(TAG, "  Needs migration: $needsMigration")

            needsMigration

        } catch (e: Exception) {
            Log.e(TAG, "Failed to check migration status", e)
            true // Assume migration needed on error
        }
    }
}

