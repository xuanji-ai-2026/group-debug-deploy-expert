package com.beijixing.app.ui.crawl.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beijixing.app.data.model.*
import com.beijixing.app.data.repository.CrawlRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CrawlViewModel @Inject constructor(
    private val crawlRepository: CrawlRepository
) : ViewModel() {

    private val _tasks = MutableLiveData<List<CrawlTask>>()
    val tasks: LiveData<List<CrawlTask>> = _tasks
    
    private val _comments = MutableLiveData<List<SocialComment>>()
    val comments: LiveData<List<SocialComment>> = _comments
    
    private val _templates = MutableLiveData<List<MessageTemplate>>()
    val templates: LiveData<List<MessageTemplate>> = _templates
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _taskResult = MutableLiveData<Boolean?>()
    val taskResult: LiveData<Boolean?> = _taskResult
    
    private val _messageResult = MutableLiveData<MessageResult?>()
    val messageResult: LiveData<MessageResult?> = _messageResult
    
    private val _leadResult = MutableLiveData<LeadResult?>()
    val leadResult: LiveData<LeadResult?> = _leadResult

    fun loadTasks() {
        viewModelScope.launch {
            try {
                _loading.value = true
                
                crawlRepository.getCrawlTasks()
                    .onSuccess { tasks ->
                        _tasks.value = tasks
                    }
                    .onFailure { e ->
                        _error.value = "加载任务失败: ${e.message}"
                    }
            } catch (e: Exception) {
                _error.value = "网络错误: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun createTask(request: CreateTaskRequest) {
        viewModelScope.launch {
            try {
                _loading.value = true
                
                crawlRepository.createCrawlTask(request)
                    .onSuccess { task ->
                        _taskResult.value = true
                        loadTasks()
                    }
                    .onFailure { e ->
                        _error.value = "创建任务失败: ${e.message}"
                        _taskResult.value = false
                    }
            } catch (e: Exception) {
                _error.value = "创建任务失败: ${e.message}"
                _taskResult.value = false
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadComments(taskId: Long, page: Int = 1, size: Int = 20) {
        viewModelScope.launch {
            try {
                _loading.value = true
                
                crawlRepository.getTaskComments(
                    taskId = taskId,
                    page = page,
                    size = size,
                    onlyHighIntent = false,
                    onlyWithContact = false
                )
                    .onSuccess { result ->
                        _comments.value = result.comments ?: emptyList()
                    }
                    .onFailure { e ->
                        _error.value = "加载评论失败: ${e.message}"
                    }
            } catch (e: Exception) {
                _error.value = "加载评论失败: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun filterComments(
        taskId: Long,
        intentLevel: String? = null,
        minScore: Int? = null,
        onlyWithContact: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                _loading.value = true
                
                crawlRepository.getTaskComments(
                    taskId = taskId,
                    page = 1,
                    size = 100,
                    minScore = minScore,
                    level = intentLevel,
                    onlyHighIntent = true,
                    onlyWithContact = onlyWithContact
                )
                    .onSuccess { result ->
                        _comments.value = result.comments ?: emptyList()
                    }
                    .onFailure { e ->
                        _error.value = "筛选评论失败: ${e.message}"
                    }
            } catch (e: Exception) {
                _error.value = "筛选评论失败: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun analyzeComments(taskId: Long) {
        viewModelScope.launch {
            try {
                crawlRepository.analyzeComments(taskId)
                    .onSuccess {
                        _error.value = null
                        loadComments(taskId)
                    }
                    .onFailure { e ->
                        _error.value = "AI分析启动失败: ${e.message}"
                    }
            } catch (e: Exception) {
                _error.value = "分析失败: ${e.message}"
            }
        }
    }

    fun generateLeadsFromTask(taskId: Long, minScore: Int, autoAssign: Boolean) {
        viewModelScope.launch {
            try {
                _loading.value = true
                
                val request = GenerateLeadsRequest(
                    minScore = minScore,
                    autoAssign = autoAssign,
                    generateFollowUpTask = true
                )
                
                crawlRepository.generateLeads(taskId, request)
                    .onSuccess { result ->
                        _leadResult.value = result
                        loadComments(taskId)
                    }
                    .onFailure { e ->
                        _error.value = "商机生成失败: ${e.message}"
                    }
            } catch (e: Exception) {
                _error.value = "商机生成失败: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun sendMessage(commentId: Long, templateId: Long?, content: String) {
        viewModelScope.launch {
            try {
                _loading.value = true
                
                val request = SendMessageRequest(
                    commentId = commentId,
                    templateId = templateId,
                    content = content
                )
                
                crawlRepository.sendMessage(request)
                    .onSuccess { result ->
                        _messageResult.value = result
                    }
                    .onFailure { e ->
                        _messageResult.value = MessageResult(
                            success = false,
                            errorMessage = "发送失败: ${e.message}"
                        )
                    }
            } catch (e: Exception) {
                _messageResult.value = MessageResult(
                    success = false,
                    errorMessage = "网络错误: ${e.message}"
                )
            } finally {
                _loading.value = false
            }
        }
    }

    fun batchSendMessage(commentIds: List<Long>, templateId: Long?) {
        viewModelScope.launch {
            try {
                _loading.value = true
                
                val request = BatchSendMessageRequest(
                    commentIds = commentIds,
                    templateId = templateId,
                    maxConcurrent = 5,
                    intervalMs = 30000
                )
                
                crawlRepository.batchSendMessage(request)
                    .onSuccess { result ->
                        _messageResult.value = result
                    }
                    .onFailure { e ->
                        _error.value = "批量发送失败: ${e.message}"
                    }
            } catch (e: Exception) {
                _error.value = "批量发送失败: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun batchGenerateLeads(commentIds: LongArray) {
        viewModelScope.launch {
            try {
                _loading.value = true
                
                val request = BatchGenerateLeadsRequest(
                    commentIds = commentIds.toList(),
                    autoAssign = true,
                    generateFollowUpTask = true
                )
                
                crawlRepository.generateLeads(0L, GenerateLeadsRequest())
                    .onSuccess { result ->
                        _leadResult.value = result
                    }
                    .onFailure { e ->
                        _error.value = "批量生成商机失败: ${e.message}"
                    }
            } catch (e: Exception) {
                _error.value = "批量生成商机失败: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun pauseTask(taskId: Long) {
        viewModelScope.launch {
            try {
                crawlRepository.pauseTask(taskId)
                    .onSuccess {
                        loadTasks()
                    }
                    .onFailure { e ->
                        _error.value = "暂停失败: ${e.message}"
                    }
            } catch (e: Exception) {
                _error.value = "暂停失败: ${e.message}"
            }
        }
    }

    fun stopTask(taskId: Long) {
        viewModelScope.launch {
            try {
                crawlRepository.stopTask(taskId)
                    .onSuccess {
                        loadTasks()
                    }
                    .onFailure { e ->
                        _error.value = "停止失败: ${e.message}"
                    }
            } catch (e: Exception) {
                _error.value = "停止失败: ${e.message}"
            }
        }
    }

    fun loadTemplates(platformCode: String? = null, intentLevel: String? = null) {
        viewModelScope.launch {
            try {
                crawlRepository.getMessageTemplates(platformCode, intentLevel)
                    .onSuccess { templates ->
                        _templates.value = templates
                    }
                    .onFailure { e ->
                        _error.value = "加载模板失败: ${e.message}"
                    }
            } catch (e: Exception) {
                _error.value = "加载模板失败: ${e.message}"
            }
        }
    }
}
