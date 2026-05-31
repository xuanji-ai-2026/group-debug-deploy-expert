package com.beijixing.app.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beijixing.app.data.model.PageResult
import com.beijixing.app.data.model.Task
import com.beijixing.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    private var currentPage = 1

    fun refresh() {
        loadTasks(1)
    }

    fun loadTasks(page: Int = 1, type: String? = null, status: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isRefreshing = page == 1, errorMsg = null)
            
            try {
                val result = taskRepository.getTasks(
                    page = page,
                    size = 20,
                    type = type,
                    status = status
                )
                
                result.onSuccess { pageResult ->
                    currentPage = page
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        tasks = pageResult.records,
                        hasMore = !pageResult.isLastPage,
                        totalCount = pageResult.total.toInt()
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMsg = error.message ?: "加载失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMsg = "加载异常：${e.message}"
                )
            }
        }
    }

    fun pauseTask(taskId: Long) {
        viewModelScope.launch {
            try {
                val result = taskRepository.pauseTask(taskId)
                
                result.onSuccess {
                    updateTaskStatus(taskId, "PAUSED", "已暂停")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(errorMsg = error.message ?: "暂停失败")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMsg = "暂停异常：${e.message}")
            }
        }
    }

    fun resumeTask(taskId: Long) {
        viewModelScope.launch {
            try {
                val result = taskRepository.resumeTask(taskId)
                
                result.onSuccess {
                    updateTaskStatus(taskId, "RUNNING", "已恢复")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(errorMsg = error.message ?: "恢复失败")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMsg = "恢复异常：${e.message}")
            }
        }
    }

    fun stopTask(taskId: Long) {
        viewModelScope.launch {
            try {
                val result = taskRepository.stopTask(taskId)
                
                result.onSuccess {
                    updateTaskStatus(taskId, "STOPPED", "已停止")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(errorMsg = error.message ?: "停止失败")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMsg = "停止异常：${e.message}")
            }
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            try {
                val result = taskRepository.deleteTask(taskId)
                
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        tasks = _uiState.value.tasks.filter { it.taskId != taskId },
                        successMsg = "删除成功"
                    )
                    loadTasks(currentPage)
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(errorMsg = error.message ?: "删除失败")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMsg = "删除异常：${e.message}")
            }
        }
    }

    private fun updateTaskStatus(taskId: Long, status: String, message: String) {
        val updatedTasks = _uiState.value.tasks.map { task ->
            if (task.taskId == taskId) task.copy(status = status) else task
        }
        _uiState.value = _uiState.value.copy(tasks = updatedTasks, successMsg = message)
    }

    fun loadMore() {
        if (_uiState.value.hasMore && !_uiState.value.isLoading) {
            loadTasks(currentPage + 1)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMsg = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMsg = null)
    }
}

data class TaskUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val tasks: List<Task> = emptyList(),
    val hasMore: Boolean = false,
    val totalCount: Int = 0,
    val errorMsg: String? = null,
    val successMsg: String? = null
)
