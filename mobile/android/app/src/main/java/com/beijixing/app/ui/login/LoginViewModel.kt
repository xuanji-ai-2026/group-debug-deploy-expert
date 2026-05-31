package com.beijixing.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beijixing.app.data.model.LoginResponse
import com.beijixing.app.data.model.User
import com.beijixing.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * 设置登录模式（sms/password/email）
     */
    fun setLoginMode(mode: String) {
        _uiState.value = _uiState.value.copy(loginMode = mode)
    }

    fun login(phone: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null)

            try {
                val result = userRepository.login(phone = phone, password = password)

                result.onSuccess { loginResponse ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        user = loginResponse.user,
                        token = loginResponse.token,
                        successMsg = "登录成功"
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMsg = error.message ?: "登录失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMsg = "网络异常：${e.message}"
                )
            }
        }
    }

    /**
     * 验证码登录（手机号）- 自动判断注册/已注册用户
     */
    fun loginWithSmsCode(phone: String, code: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null)

            try {
                // 调用后端统一接口，自动判断是新用户还是老用户
                val result = userRepository.loginWithSmsCode(phone, code)

                result.onSuccess { response ->
                    when {
                        response.isNewUser -> {
                            // 新用户，需要设置密码
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                showSetPassword = true,
                                successMsg = "验证成功，请设置密码完成注册"
                            )
                        }
                        else -> {
                            // 已注册用户，直接登录成功
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                user = response.user,
                                token = response.token,
                                successMsg = "登录成功"
                            )
                        }
                    }
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMsg = error.message ?: "验证码登录失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMsg = "网络异常：${e.message}"
                )
            }
        }
    }

    /**
     * 注册并登录（新用户设置密码后自动登录）
     */
    fun registerAndLogin(phone: String, code: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null)

            try {
                val result = userRepository.registerAndLogin(phone, code, password)

                result.onSuccess { loginResponse ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        showSetPassword = false,
                        user = loginResponse.user,
                        token = loginResponse.token,
                        successMsg = "注册并登录成功"
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMsg = error.message ?: "注册失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMsg = "注册异常：${e.message}"
                )
            }
        }
    }

    /**
     * 邮箱验证码登录
     */
    fun loginWithEmail(email: String, code: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null)

            try {
                val result = userRepository.loginWithEmail(email, code)

                result.onSuccess { response ->
                    when {
                        response.isNewUser -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                showSetPassword = true,
                                successMsg = "验证成功，请设置密码完成注册"
                            )
                        }
                        else -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                user = response.user,
                                token = response.token,
                                successMsg = "邮箱登录成功"
                            )
                        }
                    }
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMsg = error.message ?: "邮箱登录失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMsg = "网络异常：${e.message}"
                )
            }
        }
    }

    fun sendSmsCode(phone: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSendingCode = true)

            try {
                val result = userRepository.sendSmsCode(phone)

                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSendingCode = false,
                        successMsg = "验证码已发送",
                        countdownSeconds = 60
                    )

                    startCountdown()
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSendingCode = false,
                        errorMsg = error.message ?: "发送验证码失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSendingCode = false,
                    errorMsg = "发送失败：${e.message}"
                )
            }
        }
    }

    /**
     * 发送邮箱验证码
     */
    fun sendEmailCode(email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSendingCode = true)

            try {
                val result = userRepository.sendEmailCode(email)

                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSendingCode = false,
                        successMsg = "邮箱验证码已发送",
                        countdownSeconds = 60
                    )

                    startCountdown()
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSendingCode = false,
                        errorMsg = error.message ?: "发送邮箱验证码失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSendingCode = false,
                    errorMsg = "发送失败：${e.message}"
                )
            }
        }
    }

    private fun startCountdown() {
        viewModelScope.launch {
            var seconds = 60
            while (seconds > 0) {
                kotlinx.coroutines.delay(1000)
                seconds--
                _uiState.value = _uiState.value.copy(countdownSeconds = seconds)
            }
        }
    }

    /**
     * 传统注册接口（保留兼容）
     */
    fun register(phone: String, password: String, code: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null)

            try {
                val result = userRepository.register(phone, password, code)

                result.onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccessRegister = true,
                        user = user,
                        successMsg = "注册成功，请登录"
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMsg = error.message ?: "注册失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMsg = "注册异常：${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMsg = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMsg = null)
    }

    fun checkAutoLogin() {
        viewModelScope.launch {
            try {
                val result = userRepository.getUserInfo()
                result.onSuccess { user ->
                    if (user.userId > 0) {
                        _uiState.value = _uiState.value.copy(
                            isLoggedIn = true,
                            user = user,
                            successMsg = "自动登录成功"
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.d(TAG, "Auto login failed: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "LoginViewModel"
    }
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isSendingCode: Boolean = false,
    val isSuccessRegister: Boolean = false,
    val loginMode: String = "sms",
    val countdownSeconds: Int = 0,
    val user: User? = null,
    val token: String = "",
    val errorMsg: String? = null,
    val successMsg: String? = null,
    val showSetPassword: Boolean = false
)
