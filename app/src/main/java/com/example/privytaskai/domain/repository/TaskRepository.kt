package com.example.privytaskai.domain.repository

import com.example.privytaskai.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    suspend fun insertTask(task: Task): Long
    suspend fun getAllTasks(): Flow<List<Task>>
    suspend fun searchSimilarTasks(query: String, limit: Int = 10): List<Pair<Task, Float>>
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(task: Task)
    suspend fun getTaskById(id: Long): Task?
}