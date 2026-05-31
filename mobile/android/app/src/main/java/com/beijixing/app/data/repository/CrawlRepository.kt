package com.beijixing.app.data.repository

import android.content.Context
import com.beijixing.app.data.model.*
import com.beijixing.app.data.remote.ApiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class CrawlRepository @Inject constructor(
    @ApplicationContext context: Context,
    apiClient: ApiClient
) : BaseRepository(context, apiClient) {

    suspend fun getCrawlTasks(): Result<List<CrawlTask>> {
        return withIoContext {
            try {
                val response = apiClient.apiService.getCrawlTasks()
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.isSuccess()) {
                        Result.success(body.data ?: emptyList())
                    } else {
                        Result.failure(Exception("获取抓取任务失败: ${body.msg}"))
                    }
                } else {
                    Result.failure(Exception("Network error: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                android.util.Log.e("CrawlRepository", "Get crawl tasks error", e)
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun createCrawlTask(request: CreateTaskRequest): Result<CrawlTask> {
        return withIoContext {
            try {
                val response = apiClient.apiService.createCrawlTask(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.isSuccess()) {
                        Result.success(body.data ?: CrawlTask())
                    } else {
                        Result.failure(Exception("创建抓取任务失败: ${body.msg}"))
                    }
                } else {
                    Result.failure(Exception("Network error: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                android.util.Log.e("CrawlRepository", "Create crawl task error", e)
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun getTaskComments(
        taskId: Long,
        page: Int = 1,
        size: Int = 20,
        minScore: Int? = null,
        level: String? = null,
        onlyHighIntent: Boolean = false,
        onlyWithContact: Boolean = false
    ): Result<CommentFilterResult> {
        return withIoContext {
            try {
                val response = apiClient.apiService.getTaskComments(
                    taskId, page, size, minScore, level, onlyHighIntent, onlyWithContact
                )
                
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body() ?: CommentFilterResult())
                } else {
                    Result.failure(Exception("获取评论列表失败"))
                }
            } catch (e: Exception) {
                android.util.Log.e("CrawlRepository", "Get comments error", e)
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun analyzeComments(taskId: Long): Result<String> {
        return withIoContext {
            try {
                val response = apiClient.apiService.analyzeComments(taskId)
                
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success(response.body()?.data ?: "分析已启动")
                } else {
                    Result.failure(Exception(response.body()?.msg ?: "启动AI分析失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun generateLeads(taskId: Long, request: GenerateLeadsRequest): Result<LeadResult> {
        return withIoContext {
            try {
                val response = apiClient.apiService.generateLeads(taskId, request)
                
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body() ?: LeadResult())
                } else {
                    Result.failure(Exception("生成商机失败"))
                }
            } catch (e: Exception) {
                android.util.Log.e("CrawlRepository", "Generate leads error", e)
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun sendMessage(request: SendMessageRequest): Result<MessageResult> {
        return withIoContext {
            try {
                val response = apiClient.apiService.sendMessage(request)

                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body() ?: MessageResult())
                } else {
                    Result.failure(Exception("发送私信失败"))
                }
            } catch (e: Exception) {
                android.util.Log.e("CrawlRepository", "Send message error", e)
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun batchSendMessage(request: BatchSendMessageRequest): Result<MessageResult> {
        return withIoContext {
            try {
                val response = apiClient.apiService.batchSendMessage(request)

                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body() ?: MessageResult())
                } else {
                    Result.failure(Exception("批量发送私信失败"))
                }
            } catch (e: Exception) {
                android.util.Log.e("CrawlRepository", "Batch send message error", e)
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun getMessageTemplates(
        platformCode: String? = null,
        intentLevel: String? = null
    ): Result<List<MessageTemplate>> {
        return withIoContext {
            try {
                val response = apiClient.apiService.getMessageTemplates(platformCode, intentLevel)
                
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body() ?: emptyList())
                } else {
                    Result.failure(Exception("获取模板列表失败"))
                }
            } catch (e: Exception) {
                android.util.Log.e("CrawlRepository", "Get templates error", e)
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun pauseTask(taskId: Long): Result<String> {
        return withIoContext {
            try {
                val response = apiClient.apiService.stopCrawlTask(taskId)
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success("任务已暂停")
                } else {
                    Result.failure(Exception("暂停任务失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }

    suspend fun stopTask(taskId: Long): Result<String> {
        return withIoContext {
            try {
                val response = apiClient.apiService.stopCrawlTask(taskId)
                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    Result.success("任务已停止")
                } else {
                    Result.failure(Exception("停止任务失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.getOrThrow()
    }
}
