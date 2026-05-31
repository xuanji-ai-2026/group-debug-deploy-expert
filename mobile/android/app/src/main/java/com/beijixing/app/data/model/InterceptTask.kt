package com.beijixing.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * 截客任务数据模型
 *
 * 产品需求对应：7.4.1 截客任务页
 * 截客任务用于从竞争对手的评论区/粉丝列表中截获潜在客户
 *
 * @property taskId         任务ID
 * @property tenantId       租户ID
 * @property name           任务名称
 * @property status         任务状态：PENDING/RUNNING/PAUSED/COMPLETED/FAILED
 * @property targetPlatform 目标平台：DOUYIN/XIAOHONGSHU/KUAISHOU
 * @property targetType     截客类型：COMMENT(评论区)/FAN(粉丝列表)/SEARCH(搜索结果)
 * @property keywords       关键词列表
 * @property competitorAccounts 竞品账号列表
 * @property filterRules    过滤规则配置
 * @property dailyLimit     每日截客上限
 * @property todayCount     今日已截客数
 * @property totalCount     总截客数
 * @property successCount   成功触达数
 * @property interceptedLeads 截获的线索列表
 * @property progress       任务进度(0-100)
 * @property startTime      开始时间
 * @property endTime        结束时间
 * @property createTime     创建时间
 * @property updateTime     更新时间
 */
data class InterceptTask(
    @SerializedName("task_id")
    val taskId: Long = 0L,

    @SerializedName("tenant_id")
    val tenantId: Long = 0L,

    @SerializedName("name")
    val name: String = "",

    @SerializedName("status")
    val status: String = "PENDING",

    @SerializedName("target_platform")
    val targetPlatform: String = "DOUYIN",

    @SerializedName("target_type")
    val targetType: String = "COMMENT",

    @SerializedName("keywords")
    val keywords: List<String> = emptyList(),

    @SerializedName("competitor_accounts")
    val competitorAccounts: List<String> = emptyList(),

    @SerializedName("filter_rules")
    val filterRules: InterceptFilterRules? = null,

    @SerializedName("daily_limit")
    val dailyLimit: Int = 100,

    @SerializedName("today_count")
    val todayCount: Int = 0,

    @SerializedName("total_count")
    val totalCount: Int = 0,

    @SerializedName("success_count")
    val successCount: Int = 0,

    @SerializedName("intercepted_leads")
    val interceptedLeads: List<InterceptedLead> = emptyList(),

    @SerializedName("progress")
    val progress: Int = 0,

    @SerializedName("start_time")
    val startTime: String? = null,

    @SerializedName("end_time")
    val endTime: String? = null,

    @SerializedName("create_time")
    val createTime: String = "",

    @SerializedName("update_time")
    val updateTime: String = ""
) {
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
     * 获取平台显示名称
     */
    fun getPlatformText(): String = when (targetPlatform) {
        "DOUYIN" -> "抖音"
        "XIAOHONGSHU" -> "小红书"
        "KUAISHOU" -> "快手"
        else -> targetPlatform
    }

    /**
     * 获取截客类型显示文本
     */
    fun getTargetTypeText(): String = when (targetType) {
        "COMMENT" -> "评论区截客"
        "FAN" -> "粉丝截客"
        "SEARCH" -> "搜索截客"
        else -> "未知"
    }

    /**
     * 判断是否可暂停
     */
    fun canPause(): Boolean = status == "RUNNING"

    /**
     * 判断是否可恢复
     */
    fun canResume(): Boolean = status == "PAUSED"

    /**
     * 判断是否可停止
     */
    fun canStop(): Boolean = status in listOf("PENDING", "RUNNING", "PAUSED")
}

/**
 * 截客过滤规则
 */
data class InterceptFilterRules(
    @SerializedName("min_fans")
    val minFans: Int? = null,

    @SerializedName("max_fans")
    val maxFans: Int? = null,

    @SerializedName("gender")
    val gender: String? = null,

    @SerializedName("location")
    val location: String? = null,

    @SerializedName("recent_active")
    val recentActive: Boolean = true,

    @SerializedName("exclude_keywords")
    val excludeKeywords: List<String> = emptyList()
)

/**
 * 截获的线索
 */
data class InterceptedLead(
    @SerializedName("lead_id")
    val leadId: Long = 0L,

    @SerializedName("platform_user_id")
    val platformUserId: String = "",

    @SerializedName("nickname")
    val nickname: String = "",

    @SerializedName("avatar")
    val avatar: String? = null,

    @SerializedName("source_type")
    val sourceType: String = "",

    @SerializedName("source_content")
    val sourceContent: String = "",

    @SerializedName("intercept_time")
    val interceptTime: String = "",

    @SerializedName("contact_status")
    val contactStatus: String = "PENDING"
)

/**
 * 创建截客任务请求
 */
data class CreateInterceptTaskRequest(
    @SerializedName("name")
    val name: String,

    @SerializedName("target_platform")
    val targetPlatform: String,

    @SerializedName("target_type")
    val targetType: String,

    @SerializedName("keywords")
    val keywords: List<String>,

    @SerializedName("competitor_accounts")
    val competitorAccounts: List<String>,

    @SerializedName("filter_rules")
    val filterRules: InterceptFilterRules? = null,

    @SerializedName("daily_limit")
    val dailyLimit: Int = 100,

    @SerializedName("start_time")
    val startTime: String? = null
)

/**
 * 截客任务列表响应
 */
data class InterceptTaskListResponse(
    @SerializedName("list")
    val list: List<InterceptTask>,

    @SerializedName("total")
    val total: Int,

    @SerializedName("running_count")
    val runningCount: Int,

    @SerializedName("today_intercepted")
    val todayIntercepted: Int
)

/**
 * 截客实时数据
 */
data class InterceptRealTimeData(
    @SerializedName("task_id")
    val taskId: Long,

    @SerializedName("today_count")
    val todayCount: Int,

    @SerializedName("total_count")
    val totalCount: Int,

    @SerializedName("success_count")
    val successCount: Int,

    @SerializedName("current_rate")
    val currentRate: Double,

    @SerializedName("last_intercept_time")
    val lastInterceptTime: String? = null
)