package com.beijixing.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beijixing.app.data.model.AddAccountRequest
import com.beijixing.app.data.model.SocialAccount
import com.beijixing.app.data.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    fun loadAccounts(platform: String? = null, status: Int? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null)
            
            try {
                val result = accountRepository.getAccounts(platform, status)
                
                result.onSuccess { accounts ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        accounts = accounts
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

    fun addAccount(platform: String, accountName: String, accountNo: String) {
        viewModelScope.launch {
            try {
                val request = AddAccountRequest(
                    platform = platform.uppercase(),
                    accountName = accountName,
                    accountNo = accountNo
                )
                val result = accountRepository.addAccount(request)
                
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(successMsg = "账号添加成功")
                    loadAccounts()
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(errorMsg = error.message ?: "添加失败")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMsg = "异常：${e.message}")
            }
        }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            try {
                val result = accountRepository.deleteAccount(accountId)
                
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        accounts = _uiState.value.accounts.filter { it.id != accountId },
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

    fun checkHealth(accountId: Long) {
        viewModelScope.launch {
            try {
                val result = accountRepository.checkAccountHealth(accountId)
                
                result.onSuccess { healthResult ->
                    val message = if (healthResult.isHealthy) 
                        "账号状态正常" 
                        else "账号异常：${healthResult.message}"
                    _uiState.value = _uiState.value.copy(successMsg = message)
                    
                    loadAccounts()
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(errorMsg = error.message ?: "检测失败")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMsg = "异常：${e.message}")
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

data class AccountUiState(
    val isLoading: Boolean = false,
    val accounts: List<SocialAccount> = emptyList(),
    val errorMsg: String? = null,
    val successMsg: String? = null
)
