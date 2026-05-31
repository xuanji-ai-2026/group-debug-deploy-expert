package com.beijixing.app.data.repository

import android.content.Context
import com.beijixing.app.data.model.*
import com.beijixing.app.data.remote.ApiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TaskRepository @Inject constructor(
    @ApplicationContext context: Context,
    apiClient: ApiClient
) : BaseRepository(context, apiClient) {

    suspend fun getTasks(
        page: Int = 1,
        size: Int = 20,
        type: String? = null,
        status: String? = null
    ): Result<PageResult<Task>> {
        return withIoContext {
            try {
                val response = apiClient.apiService.getTaskList(page, size, type, status)
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.isSuccess()) {
                        Result.success(body.data ?: PageResult())
                    } else {
                        Result.failure(Exception("获取任务列表失败: ${body.msg}"))
                    }
                } else {
                    Result.failure(Exception("Network error: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                android.util.Log.e("TaskRepository", "Get tasks error", e)
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun getTaskDetail(taskId: Long): Result<TaskDetail> {
        return withIoContext {
            try {
                val response = apiClient.apiService.getTaskDetail(taskId)
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(response.body()?.data ?: TaskDetail())
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "获取任务详情失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun createAcquireTask(request: CreateAcquireTaskRequest): Result<Task> {
        return withIoContext {
            try {
                val response = apiClient.apiService.createAcquireTask(request)
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(response.body()?.data ?: Task())
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "创建获客任务失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun createInterceptTask(request: CreateInterceptTaskRequest): Result<Task> {
        return withIoContext {
            try {
                val response = apiClient.apiService.createInterceptTask(request)
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(response.body()?.data ?: Task())
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "创建截客任务失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun startTask(taskId: Long): Result<Boolean> {
        return withIoContext {
            try {
                val response = apiClient.apiService.startTask(taskId)
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "启动任务失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun pauseTask(taskId: Long): Result<Boolean> {
        return withIoContext {
            try {
                val response = apiClient.apiService.pauseTask(taskId)
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "暂停任务失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun resumeTask(taskId: Long): Result<Boolean> {
        return withIoContext {
            try {
                val response = apiClient.apiService.resumeTask(taskId)
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "恢复任务失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun stopTask(taskId: Long): Result<Boolean> {
        return withIoContext {
            try {
                val response = apiClient.apiService.stopTask(taskId)
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "停止任务失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun deleteTask(taskId: Long): Result<Boolean> {
        return withIoContext {
            try {
                val response = apiClient.apiService.deleteTask(taskId)
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "删除任务失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun getTaskLogs(taskId: Long, page: Int = 1, size: Int = 50): Result<PageResult<TaskLog>> {
        return withIoContext {
            try {
                val response = apiClient.apiService.getTaskLogs(taskId, page, size)
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(response.body()?.data ?: PageResult())
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "获取任务日志失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun getTaskStats(taskId: Long): Result<TaskStats> {
        return withIoContext {
            try {
                val response = apiClient.apiService.getTaskStats(taskId)
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(response.body()?.data ?: TaskStats())
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "获取任务统计失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }
}
