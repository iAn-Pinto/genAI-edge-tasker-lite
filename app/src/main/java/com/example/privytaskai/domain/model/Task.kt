package com.example.privytaskai.domain.model

data class Task(
    val id: Long = 0,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val category: String? = null,
    val embedding: List<Float>? = null
)

enum class TaskPriority {
    LOW, MEDIUM, HIGH, URGENT
}