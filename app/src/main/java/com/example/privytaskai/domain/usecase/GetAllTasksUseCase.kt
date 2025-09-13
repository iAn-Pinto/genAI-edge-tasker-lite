package com.example.privytaskai.domain.usecase

import com.example.privytaskai.domain.model.Task
import com.example.privytaskai.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetAllTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(): Flow<List<Task>> {
        return repository.getAllTasks()
    }
}