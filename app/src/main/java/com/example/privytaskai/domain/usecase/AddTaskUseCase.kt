package com.example.privytaskai.domain.usecase

import com.example.privytaskai.domain.model.Task
import com.example.privytaskai.domain.repository.TaskRepository
import com.example.privytaskai.data.local.LocalEmbeddingService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val embeddingService: LocalEmbeddingService
) {
    suspend operator fun invoke(title: String, description: String): Result<Long> {
        return try {
            val embedding = embeddingService.generateEmbedding("$title\n$description")
            val task = Task(
                title = title,
                description = description,
                embedding = embedding
            )
            val id = repository.insertTask(task)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}