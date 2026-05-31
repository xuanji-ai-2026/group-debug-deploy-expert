package com.beijixing.app.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beijixing.app.data.model.Task
import com.beijixing.app.data.model.TaskLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskDetailViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    var currentTaskName: String = ""
        private set

    fun setTaskInfo(id: Long, @Suppress("UNUSED_PARAMETER") type: String) {
        currentTaskName = "任务 #$id"
    }

    fun loadTaskDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                kotlinx.coroutines.delay(500)
                _uiState.update { it.copy(
                    isLoading = false,
                    task = Task(taskId = 1, name = "示例任务")
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMsg = "加载失败: ${e.message}"
                ) }
            }
        }
    }

    fun loadLogs() {
        viewModelScope.launch {
            _uiState.update { it.copy(logs = emptyList()) }
        }
    }

    fun pauseTask() {
        _uiState.update { it.copy(successMsg = "已暂停") }
    }

    fun resumeTask() {
        _uiState.update { it.copy(successMsg = "已恢复") }
    }

    fun stopTask() {
        _uiState.update { it.copy(successMsg = "已停止") }
    }

    fun updateTaskName(newName: String) {
        currentTaskName = newName
        _uiState.update { it.copy(successMsg = "已更新") }
    }

    fun deleteTask(onDeleted: () -> Unit) {
        onDeleted()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMsg = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMsg = null) }
    }
}

data class TaskDetailUiState(
    val isLoading: Boolean = false,
    val task: Task? = null,
    val logs: List<TaskLog> = emptyList(),
    val errorMsg: String? = null,
    val successMsg: String? = null
)
