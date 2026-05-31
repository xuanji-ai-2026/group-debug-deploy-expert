package com.beijixing.app.ui.intercept

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class InterceptTaskViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(InterceptTaskUiState())
    val uiState: StateFlow<InterceptTaskUiState> = _uiState.asStateFlow()
}

data class InterceptTaskUiState(
    val tasks: List<Any> = emptyList(),
    val isLoading: Boolean = false,
    val errorMsg: String? = null
)
