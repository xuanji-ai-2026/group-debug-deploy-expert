package com.beijixing.app.data.repository

import android.content.Context
import com.beijixing.app.data.model.*
import com.beijixing.app.data.remote.ApiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserRepository @Inject constructor(
    @ApplicationContext context: Context,
    apiClient: ApiClient
) : BaseRepository(context, apiClient) {

    suspend fun login(phone: String, password: String, loginType: String = "password"): Result<LoginResponse> {
        return withIoContext {
            try {
                val deviceId = android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                ) ?: "unknown"

                val response = apiClient.apiService.login(
                    LoginRequest(
                        phone = phone,
                        password = password,
                        deviceId = deviceId,
                        loginType = loginType
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.isSuccess() && body.data != null) {
                        val loginData = body.data
                        apiClient.saveToken(loginData.token)
                        if (!loginData.refreshToken.isNullOrEmpty()) {
                            apiClient.saveRefreshToken(loginData.refreshToken)
                        }
                        Result.success(loginData)
                    } else {
                        Result.failure(Exception("登录失败: ${body.msg}"))
                    }
                } else if (response.code() == 401) {
                    Result.failure(Exception("账号或密码错误"))
                } else if (response.code() == 403) {
                    Result.failure(Exception("账号已被禁用，请联系客服"))
                } else {
                    Result.failure(Exception("网络错误: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                android.util.Log.e("UserRepository", "登录异常", e)
                when (e) {
                    is java.net.SocketTimeoutException -> Result.failure(Exception("连接超时，请检查网络"))
                    is java.net.UnknownHostException -> Result.failure(Exception("无法连接服务器，请检查网络"))
                    is java.net.ConnectException -> Result.failure(Exception("网络连接失败"))
                    else -> Result.failure(Exception("登录失败：${e.message}"))
                }
            }
        }.getOrThrow()
    }

    suspend fun loginWithSmsCode(phone: String, code: String): Result<LoginResponse> {
        return withIoContext {
            try {
                val response = apiClient.apiService.loginWithSmsCode(
                    mapOf(
                        "phone" to phone,
                        "code" to code,
                        "loginType" to "sms_code"
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.isSuccess() && body.data != null) {
                        val loginData = parseLoginResponse(body.data)
                        apiClient.saveToken(loginData.token)
                        if (!loginData.refreshToken.isNullOrEmpty()) {
                            apiClient.saveRefreshToken(loginData.refreshToken)
                        }
                        Result.success(loginData)
                    } else {
                        Result.failure(Exception("登录失败: ${body.msg}"))
                    }
                } else {
                    Result.failure(Exception("网络错误: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("登录失败：${e.message}"))
            }
        }.getOrThrow()
    }

    suspend fun registerAndLogin(phone: String, code: String, password: String): Result<LoginResponse> {
        return withIoContext {
            try {
                val response = apiClient.apiService.registerAndLogin(
                    mapOf(
                        "phone" to phone,
                        "code" to code,
                        "password" to password,
                        "loginType" to "register"
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.isSuccess() && body.data != null) {
                        val loginData = parseLoginResponse(body.data)
                        apiClient.saveToken(loginData.token)
                        if (!loginData.refreshToken.isNullOrEmpty()) {
                            apiClient.saveRefreshToken(loginData.refreshToken)
                        }
                        Result.success(loginData)
                    } else {
                        Result.failure(Exception("注册失败: ${body.msg}"))
                    }
                } else {
                    Result.failure(Exception("网络错误: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("注册失败：${e.message}"))
            }
        }.getOrThrow()
    }

    suspend fun loginWithEmail(email: String, code: String): Result<LoginResponse> {
        return withIoContext {
            try {
                val response = apiClient.apiService.loginWithEmail(
                    mapOf(
                        "email" to email,
                        "code" to code,
                        "loginType" to "email"
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.isSuccess() && body.data != null) {
                        val loginData = parseLoginResponse(body.data)
                        apiClient.saveToken(loginData.token)
                        if (!loginData.refreshToken.isNullOrEmpty()) {
                            apiClient.saveRefreshToken(loginData.refreshToken)
                        }
                        Result.success(loginData)
                    } else {
                        Result.failure(Exception("登录失败: ${body.msg}"))
                    }
                } else {
                    Result.failure(Exception("网络错误: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("登录失败：${e.message}"))
            }
        }.getOrThrow()
    }

    suspend fun sendSmsCode(phone: String): Result<Boolean> {
        return withIoContext {
            try {
                val response = apiClient.apiService.sendSmsCode(mapOf("phone" to phone, "type" to "LOGIN"))

                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "发送验证码失败"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("发送验证码失败：${e.message}"))
            }
        }.getOrThrow()
    }

    suspend fun sendEmailCode(email: String): Result<Boolean> {
        return withIoContext {
            try {
                val response = apiClient.apiService.sendEmailCode(mapOf("email" to email, "type" to "LOGIN"))

                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "发送验证码失败"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("发送验证码失败：${e.message}"))
            }
        }.getOrThrow()
    }

    suspend fun register(phone: String, password: String, code: String?): Result<User> {
        return withIoContext {
            try {
                val deviceId = android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                )
                val response = apiClient.apiService.register(
                    RegisterRequest(
                        phone = phone,
                        password = password,
                        deviceId = deviceId
                    )
                )

                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(response.body()?.data ?: User())
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "注册失败"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("注册失败：${e.message}"))
            }
        }.getOrThrow()
    }

    suspend fun getUserInfo(): Result<User> {
        return withIoContext {
            try {
                val response = apiClient.apiService.getUserInfo()

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.isSuccess() && body.data != null) {
                        Result.success(body.data)
                    } else {
                        Result.failure(Exception("获取用户信息失败: ${body.msg}"))
                    }
                } else if (response.code() == 401) {
                    Result.failure(Exception("登录已过期，请重新登录"))
                } else {
                    Result.failure(Exception("网络错误: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("获取用户信息失败：${e.message}"))
            }
        }.getOrThrow()
    }

    suspend fun getUserBalance(): Result<UserBalance> {
        return withIoContext {
            try {
                val response = apiClient.apiService.getUserBalance()

                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(response.body()?.data ?: UserBalance())
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "获取余额失败"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("获取余额失败：${e.message}"))
            }
        }.getOrThrow()
    }

    suspend fun updateProfile(userId: Long, request: UpdateProfileRequest): Result<User> {
        return withIoContext {
            try {
                val response = apiClient.apiService.updateProfile(userId, request)

                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(response.body()?.data ?: User())
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "更新资料失败"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("更新资料失败：${e.message}"))
            }
        }.getOrThrow()
    }

    suspend fun changePassword(userId: Long, oldPassword: String, newPassword: String): Result<Boolean> {
        return withIoContext {
            try {
                val response = apiClient.apiService.changePassword(
                    userId,
                    ChangePasswordRequest(oldPassword = oldPassword, newPassword = newPassword)
                )

                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "修改密码失败"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("修改密码失败：${e.message}"))
            }
        }.getOrThrow()
    }

    suspend fun logout() {
        try {
            withContext(Dispatchers.IO) {
                apiClient.clearToken()
                apiClient.apiService.logout()
            }
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "登出异常", e)
            apiClient.clearToken()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseLoginResponse(data: Map<String, Any>): LoginResponse {
        val token = data["token"] as? String ?: ""
        val refreshToken = data["refresh_token"] as? String ?: data["refreshToken"] as? String ?: ""
        val isNewUser = data["isNewUser"] as? Boolean ?: false
        val expiresIn = (data["expires_in"] as? Number)?.toLong() ?: 0L

        val userMap = data["user"] as? Map<String, Any>
        val user = if (userMap != null) {
            User(
                userId = (userMap["id"] as? Number)?.toLong() ?: (userMap["userId"] as? Number)?.toLong() ?: 0L,
                phone = userMap["phone"] as? String ?: "",
                nickname = userMap["nickname"] as? String ?: "",
                avatar = userMap["avatar"] as? String,
                role = userMap["roleType"] as? String ?: userMap["role"] as? String,
                email = userMap["email"] as? String,
                realName = userMap["realName"] as? String,
                status = (userMap["status"] as? Number)?.toInt() ?: 1,
                tenantId = (userMap["tenantId"] as? Number)?.toLong()
            )
        } else {
            User()
        }

        return LoginResponse(
            token = token,
            refreshToken = refreshToken,
            expiresIn = expiresIn,
            user = user,
            isNewUser = isNewUser
        )
    }
}
