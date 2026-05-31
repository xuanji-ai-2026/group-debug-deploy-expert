package com.beijixing.app.ui.lead

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beijixing.app.data.model.Lead
import com.beijixing.app.data.model.PageResult
import com.beijixing.app.data.repository.LeadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LeadViewModel @Inject constructor(
    private val leadRepository: LeadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeadUiState())
    val uiState: StateFlow<LeadUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private var currentKeyword: String? = null
    private var currentStatus: String? = null

    fun loadLeads(page: Int = 1) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null)
            
            try {
                val result = leadRepository.getLeads(
                    page = page,
                    size = 20,
                    keyword = currentKeyword,
                    status = currentStatus
                )
                
                result.onSuccess { pageResult ->
                    currentPage = page
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        leads = pageResult.records,
                        hasMore = !pageResult.isLastPage,
                        totalCount = pageResult.total.toInt()
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

    fun searchLeads(keyword: String) {
        currentKeyword = keyword.ifBlank { null }
        loadLeads(1)
    }

    fun filterByStatus(status: String) {
        currentStatus = status.ifBlank { null }
        loadLeads(1)
    }

    fun deleteLead(leadId: Long) {
        viewModelScope.launch {
            try {
                val result = leadRepository.deleteLead(leadId)
                
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        leads = _uiState.value.leads.filter { it.id != leadId },
                        successMsg = "删除成功"
                    )
                    loadLeads(currentPage)
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(errorMsg = error.message ?: "删除失败")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMsg = "删除异常：${e.message}")
            }
        }
    }

    fun addFollowRecord(leadId: Long, content: String) {
        viewModelScope.launch {
            try {
                val result = leadRepository.addFollowRecord(leadId, content)
                
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(successMsg = "跟进记录已添加")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(errorMsg = error.message ?: "添加失败")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMsg = "异常：${e.message}")
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.hasMore && !_uiState.value.isLoading) {
            loadLeads(currentPage + 1)
        }
    }

    fun refresh() {
        loadLeads(1)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMsg = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMsg = null)
    }
}

data class LeadUiState(
    val isLoading: Boolean = false,
    val leads: List<Lead> = emptyList(),
    val hasMore: Boolean = false,
    val totalCount: Int = 0,
    val errorMsg: String? = null,
    val successMsg: String? = null
)
