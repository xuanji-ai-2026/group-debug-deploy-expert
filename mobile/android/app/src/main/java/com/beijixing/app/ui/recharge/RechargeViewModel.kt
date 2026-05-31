package com.beijixing.app.ui.recharge

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class RechargeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(RechargeUiState())
    val uiState: StateFlow<RechargeUiState> = _uiState.asStateFlow()

    fun selectPackage(pkg: Any) {
        _uiState.value = _uiState.value.copy(selectedPackage = pkg)
    }

    fun selectPayMethod(method: String) {
        _uiState.value = _uiState.value.copy(payMethod = method)
    }
}

data class RechargeUiState(
    val balance: Int = 0,
    val packages: List<Any> = emptyList(),
    val records: List<Any>? = null,
    val selectedPackage: Any? = null,
    val payMethod: String = "WECHAT",
    val isLoading: Boolean = false,
    val orderCreated: Boolean = false,
    val currentOrderNo: String? = null,
    val errorMsg: String? = null
)
