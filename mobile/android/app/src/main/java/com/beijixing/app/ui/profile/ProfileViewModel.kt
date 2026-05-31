package com.beijixing.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beijixing.app.data.model.UpdateProfileRequest
import com.beijixing.app.data.model.User
import com.beijixing.app.data.model.UserBalance
import com.beijixing.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadUserInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val result = userRepository.getUserInfo()
                
                result.onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = user
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMsg = error.message ?: "获取用户信息失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMsg = "异常：${e.message}"
                )
            }
        }
    }

    fun loadBalance() {
        viewModelScope.launch {
            try {
                val result = userRepository.getUserBalance()
                
                result.onSuccess { balance ->
                    _uiState.value = _uiState.value.copy(balance = balance)
                }.onFailure { error ->
                    // 静默失败，不显示错误
                }
            } catch (e: Exception) {
                // 静默失败
            }
        }
    }

    fun updateProfile(nickname: String, email: String? = null) {
        viewModelScope.launch {
            try {
                val userId = _uiState.value.user?.userId ?: return@launch
                val request = UpdateProfileRequest(
                    nickname = nickname.ifBlank { null },
                    email = email?.ifBlank { null }
                )
                val result = userRepository.updateProfile(userId, request)
                
                result.onSuccess { updatedUser ->
                    _uiState.value = _uiState.value.copy(
                        user = updatedUser,
                        successMsg = "资料更新成功"
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(errorMsg = error.message ?: "更新失败")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMsg = "异常：${e.message}")
            }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            try {
                val userId = _uiState.value.user?.userId ?: return@launch
                val result = userRepository.changePassword(userId, oldPassword, newPassword)
                
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(successMsg = "密码修改成功，请重新登录")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(errorMsg = error.message ?: "修改失败")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMsg = "异常：${e.message}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                userRepository.logout()
                _uiState.value = ProfileUiState()
            } catch (e: Exception) {
                // 忽略错误
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMsg = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMsg = null)
    }
}

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val balance: UserBalance? = null,
    val errorMsg: String? = null,
    val successMsg: String? = null
)
