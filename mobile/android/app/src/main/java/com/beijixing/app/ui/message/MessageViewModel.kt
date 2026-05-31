package com.beijixing.app.ui.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beijixing.app.data.model.Message
import com.beijixing.app.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageUiState())
    val uiState: StateFlow<MessageUiState> = _uiState.asStateFlow()

    // 从 token/preferences 中获取 userId 的辅助方法
    private var currentUserId: Long = 0L
    private var currentUserOtherId: Long = 0L  // 私聊对方的 userId

    fun init(userId: Long, otherUserId: Long = 0L) {
        currentUserId = userId
        currentUserOtherId = otherUserId
    }

    fun loadMessages(page: Int = 0, type: String? = null, isRead: Boolean? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null)
            
            try {
                val result = messageRepository.getMessages(
                    userId = currentUserOtherId.takeIf { it > 0 } ?: currentUserId,
                    currentUserId = currentUserId,
                    page = page,
                    size = 20
                )
                
                result.onSuccess { pageResult ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        messages = pageResult.records,
                        unreadCount = countUnread(pageResult.records)
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMsg = error.message ?: "加载失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMsg = "加载异常：${e.message}"
                )
            }
        }
    }

    fun markAsRead(sessionId: String) {
        viewModelScope.launch {
            try {
                val result = messageRepository.markAsRead(sessionId, currentUserId)
                
                result.onSuccess {
                    // 标记整个会话的消息为已读
                    val updatedMessages = _uiState.value.messages.map { msg ->
                        msg.copy(isRead = true)
                    }
                    _uiState.value = _uiState.value.copy(
                        messages = updatedMessages,
                        unreadCount = countUnread(updatedMessages),
                        successMsg = "已标记为已读"
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(errorMsg = error.message ?: "操作失败")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMsg = "异常：${e.message}")
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                val result = messageRepository.deleteMessage(messageId, currentUserId)
                
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages.filter { it.messageId.toString() != messageId },
                        successMsg = "删除成功"
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(errorMsg = error.message ?: "删除失败")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMsg = "异常：${e.message}")
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                val result = messageRepository.markAllAsRead(currentUserId)
                
                result.onSuccess {
                    val allRead = _uiState.value.messages.map { it.copy(isRead = true) }
                    _uiState.value = _uiState.value.copy(
                        messages = allRead,
                        unreadCount = 0,
                        successMsg = "全部标记为已读"
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(errorMsg = error.message ?: "操作失败")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMsg = "异常：${e.message}")
            }
        }
    }

    fun refresh() {
        loadMessages(1)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMsg = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMsg = null)
    }
    
    private fun countUnread(messages: List<Message>): Int {
        return messages.count { !it.isRead }
    }
}

data class MessageUiState(
    val isLoading: Boolean = false,
    val messages: List<Message> = emptyList(),
    val unreadCount: Int = 0,
    val errorMsg: String? = null,
    val successMsg: String? = null
)
