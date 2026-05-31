package com.beijixing.app.ui.acquire

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AcquireTaskViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AcquireTaskUiState())
    val uiState: StateFlow<AcquireTaskUiState> = _uiState.asStateFlow()
}

data class AcquireTaskUiState(
    val tasks: List<Any> = emptyList(),
    val isLoading: Boolean = false,
    val errorMsg: String? = null
)
