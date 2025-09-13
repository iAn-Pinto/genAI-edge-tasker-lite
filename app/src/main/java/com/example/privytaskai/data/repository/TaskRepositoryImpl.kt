package com.example.privytaskai.data.repository

import com.example.privytaskai.data.database.dao.TaskDao
import com.example.privytaskai.data.database.dao.EmbeddingDao
import com.example.privytaskai.data.database.entities.TaskEntity
import com.example.privytaskai.data.database.entities.EmbeddingEntity
import com.example.privytaskai.data.local.LocalEmbeddingService
import com.example.privytaskai.domain.model.Task
import com.example.privytaskai.domain.model.TaskPriority
import com.example.privytaskai.domain.repository.TaskRepository
import com.example.privytaskai.privacy.PrivacyAuditor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val embeddingDao: EmbeddingDao,
    private val embeddingService: LocalEmbeddingService,
    private val privacyAuditor: PrivacyAuditor
) : TaskRepository {

    override suspend fun insertTask(task: Task): Long {
        privacyAuditor.auditDataFlow("INSERT_TASK", "TASK_DATA", "LOCAL_STORAGE")
        
        val taskEntity = task.toEntity()
        val taskId = taskDao.insertTask(taskEntity)
        
        // Store embedding if available
        task.embedding?.let { embedding ->
            val embeddingEntity = EmbeddingEntity(
                taskId = taskId,
                vectorCsv = embeddingService.vectorToCsv(embedding)
            )
            embeddingDao.upsert(embeddingEntity)
        }
        
        return taskId
    }

    override suspend fun getAllTasks(): Flow<List<Task>> {
        privacyAuditor.auditDataFlow("GET_ALL_TASKS", "TASK_DATA", "LOCAL_STORAGE")
        return taskDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun searchSimilarTasks(query: String, limit: Int): List<Pair<Task, Float>> {
        privacyAuditor.auditDataFlow("SEARCH_TASKS", "QUERY_EMBEDDING", "LOCAL_PROCESSING")
        privacyAuditor.validateLocalProcessing("EMBEDDING_GENERATION")
        
        val queryEmbedding = embeddingService.generateEmbedding(query)
        val allTasks = taskDao.getAll()
        val embeddings = embeddingDao.getAll().associateBy { it.taskId }
        
        return allTasks.mapNotNull { taskEntity ->
            val embeddingEntity = embeddings[taskEntity.id] ?: return@mapNotNull null
            val taskEmbedding = embeddingService.csvToVector(embeddingEntity.vectorCsv)
            val similarity = embeddingService.cosineSimilarity(queryEmbedding, taskEmbedding)
            taskEntity.toDomain() to similarity
        }.sortedByDescending { it.second }.take(limit)
    }

    override suspend fun updateTask(task: Task) {
        privacyAuditor.auditDataFlow("UPDATE_TASK", "TASK_DATA", "LOCAL_STORAGE")
        taskDao.updateTask(task.toEntity())
    }

    override suspend fun deleteTask(task: Task) {
        privacyAuditor.auditDataFlow("DELETE_TASK", "TASK_DATA", "LOCAL_STORAGE")
        taskDao.deleteTask(task.toEntity())
        embeddingDao.deleteByTaskId(task.id)
    }

    override suspend fun getTaskById(id: Long): Task? {
        privacyAuditor.auditDataFlow("GET_TASK_BY_ID", "TASK_DATA", "LOCAL_STORAGE")
        return taskDao.getById(id)?.toDomain()
    }

    // Extension functions for mapping between domain and data models
    private fun Task.toEntity(): TaskEntity {
        return TaskEntity(
            id = id,
            title = title,
            description = description,
            isCompleted = isCompleted,
            createdAt = createdAt,
            priority = priority.name,
            category = category
        )
    }

    private fun TaskEntity.toDomain(): Task {
        return Task(
            id = id,
            title = title,
            description = description,
            isCompleted = isCompleted,
            createdAt = createdAt,
            priority = TaskPriority.valueOf(priority),
            category = category
        )
    }
}