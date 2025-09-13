package com.example.privytaskai.domain.usecase

import com.example.privytaskai.domain.model.Task
import com.example.privytaskai.domain.repository.TaskRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(query: String, limit: Int = 10): List<Pair<Task, Float>> {
        return repository.searchSimilarTasks(query, limit)
    }
}