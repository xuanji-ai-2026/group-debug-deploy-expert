package com.beijixing.app.data.repository

import android.content.Context
import com.beijixing.app.data.model.*
import com.beijixing.app.data.remote.ApiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AccountRepository @Inject constructor(
    @ApplicationContext context: Context,
    apiClient: ApiClient
) : BaseRepository(context, apiClient) {

    @Suppress("UNCHECKED_CAST")
    suspend fun getAccounts(platform: String? = null, status: Int? = null): Result<List<SocialAccount>> {
        return withIoContext {
            try {
                val response = apiClient.apiService.getAccountList(platform, status)
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.isSuccess()) {
                        // 后端返回 PageVO 格式，提取 records 列表
                        val pageData = body.data
                        val records = if (pageData is Map<*, *>) {
                            (pageData["records"] as? List<*>)?.filterIsInstance<SocialAccount>() ?: emptyList()
                        } else {
                            emptyList()
                        }
                        Result.success(records)
                    } else {
                        Result.failure(Exception("获取账号列表失败: ${body.msg}"))
                    }
                } else {
                    Result.failure(Exception("Network error: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                android.util.Log.e("AccountRepository", "Get accounts error", e)
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun addAccount(request: AddAccountRequest): Result<SocialAccount> {
        return withIoContext {
            try {
                val response = apiClient.apiService.addAccount(request)
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(response.body()?.data ?: SocialAccount())
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "添加账号失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun deleteAccount(accountId: Long): Result<Boolean> {
        return withIoContext {
            try {
                val response = apiClient.apiService.deleteAccount(accountId)
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "删除账号失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun checkAccountHealth(accountId: Long): Result<AccountHealthResult> {
        return withIoContext {
            try {
                val response = apiClient.apiService.checkAccountHealth(accountId)
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(response.body()?.data ?: AccountHealthResult(
                        accountId = accountId,
                        isHealthy = false,
                        status = "UNKNOWN",
                        message = "检测失败"
                    ))
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "健康检测失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun getSupportedPlatforms(): Result<List<PlatformInfo>> {
        return withIoContext {
            try {
                val response = apiClient.apiService.getSupportedPlatforms()
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(response.body()?.data ?: emptyList())
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "获取平台列表失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }
}
