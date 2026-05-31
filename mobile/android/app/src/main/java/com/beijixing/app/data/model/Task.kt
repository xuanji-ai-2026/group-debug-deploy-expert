package com.beijixing.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * 任务数据模型
 *
 * 对应后端任务调度系统
 * 产品需求对应：6.5节 自动发布与运营 / 7.2节 主动获客功能
 *
 * @property taskId         任务ID
 * @property tenantId       租户ID
 * @property name           任务名称
 * @property type           任务类型：
 *                          INTERCEPT（同业截客）/ ACTIVE_CAPTURE（主动获客）
 *                          / CONTENT_PUBLISH（内容发布）/ CUSTOM_MESSAGE（批量私信）
 * @property status         任务状态：PENDING（待执行）/ RUNNING（执行中）
 *                          / PAUSED（已暂停）/ COMPLETED（已完成）/ FAILED（失败）
 * @property platforms      执行的平台列表：DOUYIN、KUAISHOU、XIAOHONGSHU等
 * @property keywords       关键词列表（截客/获客任务）
 * @property totalCount     总目标数
 * @property completedCount 已完成数
 * @property successCount   成功数
 * @property failCount      失败数
 * @property progress      进度百分比（0-100）
 * @property startTime      任务开始时间
 * @property endTime        任务结束时间（预计）
 * @property actualEndTime  实际结束时间
 * @property errorMsg       错误信息（任务失败时）
 * @property createTime     创建时间
 * @property updateTime     更新时间
 */
data class Task(
    @SerializedName("task_id")
    val taskId: Long = 0L,

    @SerializedName("tenant_id")
    val tenantId: Long = 0L,

    @SerializedName("name")
    val name: String = "",

    @SerializedName("type")
    val type: String = "",

    @SerializedName("status")
    val status: String = "PENDING",

    @SerializedName("platforms")
    val platforms: List<String>? = null,

    @SerializedName("keywords")
    val keywords: List<String>? = null,

    @SerializedName("total_count")
    val totalCount: Int = 0,

    @SerializedName("completed_count")
    val completedCount: Int = 0,

    @SerializedName("success_count")
    val successCount: Int = 0,

    @SerializedName("fail_count")
    val failCount: Int = 0,

    @SerializedName("progress")
    val progress: Int = 0,

    @SerializedName("start_time")
    val startTime: String? = null,

    @SerializedName("end_time")
    val endTime: String? = null,

    @SerializedName("actual_end_time")
    val actualEndTime: String? = null,

    @SerializedName("error_msg")
    val errorMsg: String? = null,

    @SerializedName("create_time")
    val createTime: String = "",

    @SerializedName("update_time")
    val updateTime: String = ""
) {
    /**
     * 获取任务类型显示文本
     */
    fun getTypeText(): String = when (type) {
        "INTERCEPT" -> "同业截客"
        "ACTIVE_CAPTURE" -> "主动获客"
        "CONTENT_PUBLISH" -> "内容发布"
        "CUSTOM_MESSAGE" -> "批量私信"
        else -> "未知任务"
    }

    /**
     * 获取状态显示文本
     */
    fun getStatusText(): String = when (status) {
        "PENDING" -> "待执行"
        "RUNNING" -> "执行中"
        "PAUSED" -> "已暂停"
        "COMPLETED" -> "已完成"
        "FAILED" -> "执行失败"
        else -> "未知"
    }

    /**
     * 获取平台名称字符串
     */
    fun getPlatformNames(): String {
        return platforms?.joinToString(" · ") { platform ->
            when (platform) {
                "DOUYIN" -> "抖音"
                "KUAISHOU" -> "快手"
                "XIAOHONGSHU" -> "小红书"
                "WEIBO" -> "微博"
                else -> platform
            }
        } ?: ""
    }

    /**
     * 判断任务是否可暂停
     */
    fun canPause(): Boolean = status == "RUNNING"

    /**
     * 判断任务是否可恢复
     */
    fun canResume(): Boolean = status == "PAUSED"

    /**
     * 判断任务是否可取消
     */
    fun canCancel(): Boolean = status in listOf("PENDING", "RUNNING", "PAUSED")
}

/**
 * 任务列表请求参数
 *
 * @property type    任务类型筛选
 * @property status  状态筛选
 * @property page    页码
 * @property pageSize 每页数量
 */
data class TaskListRequest(
    @SerializedName("type")
    val type: String? = null,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("page")
    val page: Int = 1,

    @SerializedName("page_size")
    val pageSize: Int = 20
)

/**
 * 创建截客/获客任务请求
 *
 * @property name       任务名称
 * @property type       任务类型
 * @property platforms  执行平台
 * @property keywords   关键词列表
 * @property startTime  计划开始时间
 * @property endTime    计划结束时间
 */
data class CreateTaskRequest(
    @SerializedName("name")
    val name: String,

    @SerializedName("type")
    val type: String,

    @SerializedName("platforms")
    val platforms: List<String>,

    @SerializedName("keywords")
    val keywords: List<String>,

    @SerializedName("start_time")
    val startTime: String? = null,

    @SerializedName("end_time")
    val endTime: String? = null
)

/**
 * 任务操作请求
 *
 * @property taskId 任务ID
 * @property action  操作类型：PAUSE（暂停）/ RESUME（恢复）/ CANCEL（取消）
 */
data class TaskActionRequest(
    @SerializedName("task_id")
    val taskId: Long,

    @SerializedName("action")
    val action: String
)

/**
 * 首页任务进度展示模型
 * 从完整Task简化，用于首页快速展示
 */
data class TaskProgressItem(
    val taskId: Long,
    val name: String,
    val status: String,
    val progress: Int,
    val completed: Int,
    val total: Int,
    val platforms: List<String>
) {
    companion object {
        /**
         * 从Task转换
         */
        fun from(task: Task): TaskProgressItem = TaskProgressItem(
            taskId = task.taskId,
            name = task.name,
            status = task.status,
            progress = task.progress,
            completed = task.completedCount,
            total = task.totalCount,
            platforms = task.platforms ?: emptyList()
        )
    }
}
