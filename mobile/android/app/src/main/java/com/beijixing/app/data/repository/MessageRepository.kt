package com.beijixing.app.data.repository

import android.content.Context
import com.beijixing.app.data.model.*
import com.beijixing.app.data.remote.ApiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MessageRepository @Inject constructor(
    @ApplicationContext context: Context,
    apiClient: ApiClient
) : BaseRepository(context, apiClient) {

    suspend fun getMessages(
        userId: Long,
        currentUserId: Long,
        page: Int = 0,
        size: Int = 20
    ): Result<PageResult<Message>> {
        return withIoContext {
            try {
                val response = apiClient.apiService.getMessageList(userId, currentUserId, page, size)
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.isSuccess()) {
                        Result.success(body.data ?: PageResult())
                    } else {
                        Result.failure(Exception("获取消息列表失败: ${body.msg}"))
                    }
                } else {
                    Result.failure(Exception("Network error: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                android.util.Log.e("MessageRepository", "Get messages error", e)
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun getMessageDetail(sessionId: String): Result<Message> {
        return withIoContext {
            try {
                val response = apiClient.apiService.getMessageDetail(sessionId)
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(response.body()?.data ?: Message())
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "获取消息详情失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun markAsRead(sessionId: String, userId: Long): Result<Boolean> {
        return withIoContext {
            try {
                val response = apiClient.apiService.markMessageRead(sessionId, userId)
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "标记已读失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun deleteMessage(messageId: String, userId: Long): Result<Boolean> {
        return withIoContext {
            try {
                val response = apiClient.apiService.deleteMessage(messageId, userId)
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "删除消息失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun markAllAsRead(userId: Long): Result<Boolean> {
        return withIoContext {
            try {
                val response = apiClient.apiService.markAllMessagesRead(userId)
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "全部标记已读失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun getUnreadCount(userId: Long): Result<Int> {
        return withIoContext {
            try {
                val response = apiClient.apiService.getUnreadCount(userId)
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(response.body()?.data?.get("count") ?: 0)
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "获取未读数失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }
}
