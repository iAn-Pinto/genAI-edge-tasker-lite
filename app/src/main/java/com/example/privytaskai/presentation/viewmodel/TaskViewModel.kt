package com.example.privytaskai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.privytaskai.domain.model.Task
import com.example.privytaskai.domain.usecase.AddTaskUseCase
import com.example.privytaskai.domain.usecase.GetAllTasksUseCase
import com.example.privytaskai.domain.usecase.SearchTasksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val addTaskUseCase: AddTaskUseCase,
    private val getAllTasksUseCase: GetAllTasksUseCase,
    private val searchTasksUseCase: SearchTasksUseCase
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<Pair<Task, Float>>>(emptyList())
    val searchResults: StateFlow<List<Pair<Task, Float>>> = _searchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val tasks = runBlocking {
        getAllTasksUseCase()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    fun addTask(title: String, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            
            addTaskUseCase(title, description)
                .onSuccess {
                    // Task added successfully
                }
                .onFailure { exception ->
                    _error.value = exception.message
                }
            
            _isLoading.value = false
        }
    }

    fun searchTasks(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            
            try {
                val results = searchTasksUseCase(query)
                _searchResults.value = results
            } catch (e: Exception) {
                _error.value = e.message
                _searchResults.value = emptyList()
            }
            
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }
}