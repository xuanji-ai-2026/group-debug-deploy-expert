package com.beijixing.app.data.repository

import android.content.Context
import com.beijixing.app.data.model.*
import com.beijixing.app.data.remote.ApiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LeadRepository @Inject constructor(
    @ApplicationContext context: Context,
    apiClient: ApiClient
) : BaseRepository(context, apiClient) {

    suspend fun getLeads(
        page: Int = 1,
        size: Int = 20,
        keyword: String? = null,
        status: String? = null,
        level: String? = null
    ): Result<PageResult<Lead>> {
        return withIoContext {
            try {
                val request = LeadListRequest(
                    keyword = keyword,
                    status = status,
                    level = level,
                    pageNum = page,
                    pageSize = size
                )
                val response = apiClient.apiService.getLeadList(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.isSuccess()) {
                        Result.success(body.data ?: PageResult())
                    } else {
                        Result.failure(Exception("获取商机列表失败: ${body.msg}"))
                    }
                } else {
                    Result.failure(Exception("Network error: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                android.util.Log.e("LeadRepository", "Get leads error", e)
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun getLeadDetail(leadId: Long): Result<Lead> {
        return withIoContext {
            try {
                val response = apiClient.apiService.getLeadDetail(leadId)
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(response.body()?.data ?: Lead())
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "获取商机详情失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun addLead(request: AddLeadRequest): Result<Lead> {
        return withIoContext {
            try {
                val response = apiClient.apiService.addLead(request)
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(response.body()?.data ?: Lead())
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "添加商机失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun updateLead(leadId: Long, request: AddLeadRequest): Result<Lead> {
        return withIoContext {
            try {
                val response = apiClient.apiService.updateLead(leadId, request)
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(response.body()?.data ?: Lead())
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "更新商机失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun deleteLead(leadId: Long): Result<Boolean> {
        return withIoContext {
            try {
                val response = apiClient.apiService.deleteLead(leadId)
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "删除商机失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun addFollowRecord(leadId: Long, content: String): Result<FollowRecord> {
        return withIoContext {
            try {
                val response = apiClient.apiService.addFollowRecord(
                    leadId,
                    FollowRecordRequest(content = content, type = "NOTE")
                )
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(response.body()?.data ?: FollowRecord())
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "添加跟进记录失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun getFollowRecords(leadId: Long): Result<List<FollowRecord>> {
        return withIoContext {
            try {
                val response = apiClient.apiService.getFollowRecords(leadId)
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(response.body()?.data ?: emptyList())
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "获取跟进记录失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun getLeadStats(): Result<LeadStats> {
        return withIoContext {
            try {
                val response = apiClient.apiService.getLeadStats()
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(response.body()?.data ?: LeadStats())
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "获取统计数据失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }
}
