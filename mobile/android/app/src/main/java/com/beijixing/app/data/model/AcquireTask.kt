package com.beijixing.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * 获客任务数据模型
 *
 * 产品需求对应：7.4.2 获客任务页
 * 获客任务用于主动获取目标用户群体
 *
 * @property taskId         任务ID
 * @property tenantId       租户ID
 * @property name           任务名称
 * @property status         任务状态：PENDING/RUNNING/PAUSED/COMPLETED/FAILED
 * @property channel        获客渠道：SEARCH(搜索)/TOPIC(话题)/LOCATION(同城)/RECOMMEND(推荐)
 * @property targetPlatforms 目标平台列表
 * @property keywords       关键词列表
 * @property targetAudience 目标人群配置
 * @property contentTemplate 内容模板
 * @property dailyLimit     每日获客上限
 * @property todayCount     今日获客数
 * @property totalCount     总获客数
 * @property qualifiedCount 优质线索数
 * @property leadQualityScore 线索质量评分(0-100)
 * @property acquiredLeads  已获客线索列表
 * @property progress       任务进度(0-100)
 * @property startTime      开始时间
 * @property endTime        结束时间
 * @property createTime     创建时间
 * @property updateTime     更新时间
 */
data class AcquireTask(
    @SerializedName("task_id")
    val taskId: Long = 0L,

    @SerializedName("tenant_id")
    val tenantId: Long = 0L,

    @SerializedName("name")
    val name: String = "",

    @SerializedName("status")
    val status: String = "PENDING",

    @SerializedName("channel")
    val channel: String = "SEARCH",

    @SerializedName("target_platforms")
    val targetPlatforms: List<String> = emptyList(),

    @SerializedName("keywords")
    val keywords: List<String> = emptyList(),

    @SerializedName("target_audience")
    val targetAudience: TargetAudience? = null,

    @SerializedName("content_template")
    val contentTemplate: ContentTemplate? = null,

    @SerializedName("daily_limit")
    val dailyLimit: Int = 100,

    @SerializedName("today_count")
    val todayCount: Int = 0,

    @SerializedName("total_count")
    val totalCount: Int = 0,

    @SerializedName("qualified_count")
    val qualifiedCount: Int = 0,

    @SerializedName("lead_quality_score")
    val leadQualityScore: Int = 0,

    @SerializedName("acquired_leads")
    val acquiredLeads: List<AcquiredLead> = emptyList(),

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
     * 获取渠道显示文本
     */
    fun getChannelText(): String = when (channel) {
        "SEARCH" -> "搜索获客"
        "TOPIC" -> "话题获客"
        "LOCATION" -> "同城获客"
        "RECOMMEND" -> "推荐获客"
        else -> "未知"
    }

    /**
     * 获取平台显示名称
     */
    fun getPlatformNames(): String {
        return targetPlatforms.joinToString(" · ") { platform ->
            when (platform) {
                "DOUYIN" -> "抖音"
                "XIAOHONGSHU" -> "小红书"
                "KUAISHOU" -> "快手"
                else -> platform
            }
        }
    }

    /**
     * 获取质量评分等级
     */
    fun getQualityLevel(): String = when {
        leadQualityScore >= 80 -> "优质"
        leadQualityScore >= 60 -> "良好"
        leadQualityScore >= 40 -> "一般"
        else -> "待优化"
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
 * 目标人群配置
 */
data class TargetAudience(
    @SerializedName("age_range")
    val ageRange: AgeRange? = null,

    @SerializedName("gender")
    val gender: String? = null,

    @SerializedName("locations")
    val locations: List<String> = emptyList(),

    @SerializedName("interests")
    val interests: List<String> = emptyList(),

    @SerializedName("behavior_tags")
    val behaviorTags: List<String> = emptyList()
)

/**
 * 年龄范围
 */
data class AgeRange(
    @SerializedName("min")
    val min: Int = 18,

    @SerializedName("max")
    val max: Int = 60
)

/**
 * 内容模板
 */
data class ContentTemplate(
    @SerializedName("template_id")
    val templateId: String = "",

    @SerializedName("title")
    val title: String = "",

    @SerializedName("content")
    val content: String = "",

    @SerializedName("images")
    val images: List<String> = emptyList()
)

/**
 * 已获客线索
 */
data class AcquiredLead(
    @SerializedName("lead_id")
    val leadId: Long = 0L,

    @SerializedName("platform_user_id")
    val platformUserId: String = "",

    @SerializedName("nickname")
    val nickname: String = "",

    @SerializedName("avatar")
    val avatar: String? = null,

    @SerializedName("source_channel")
    val sourceChannel: String = "",

    @SerializedName("acquire_time")
    val acquireTime: String = "",

    @SerializedName("quality_score")
    val qualityScore: Int = 0,

    @SerializedName("contact_status")
    val contactStatus: String = "PENDING"
) {
    /**
     * 获取质量等级颜色
     */
    fun getQualityLevel(): String = when {
        qualityScore >= 80 -> "优质"
        qualityScore >= 60 -> "良好"
        qualityScore >= 40 -> "一般"
        else -> "待优化"
    }
}

/**
 * 创建获客任务请求
 */
data class CreateAcquireTaskRequest(
    @SerializedName("name")
    val name: String,

    @SerializedName("channel")
    val channel: String,

    @SerializedName("target_platforms")
    val targetPlatforms: List<String>,

    @SerializedName("keywords")
    val keywords: List<String>,

    @SerializedName("target_audience")
    val targetAudience: TargetAudience? = null,

    @SerializedName("content_template")
    val contentTemplate: ContentTemplate? = null,

    @SerializedName("daily_limit")
    val dailyLimit: Int = 100,

    @SerializedName("start_time")
    val startTime: String? = null
)

/**
 * 获客任务列表响应
 */
data class AcquireTaskListResponse(
    @SerializedName("list")
    val list: List<AcquireTask>,

    @SerializedName("total")
    val total: Int,

    @SerializedName("running_count")
    val runningCount: Int,

    @SerializedName("today_acquired")
    val todayAcquired: Int,

    @SerializedName("avg_quality_score")
    val avgQualityScore: Double
)

/**
 * 获客统计数据
 */
data class AcquireTaskStats(
    @SerializedName("task_id")
    val taskId: Long,

    @SerializedName("total_acquired")
    val totalAcquired: Int,

    @SerializedName("qualified_count")
    val qualifiedCount: Int,

    @SerializedName("conversion_rate")
    val conversionRate: Double,

    @SerializedName("channel_distribution")
    val channelDistribution: Map<String, Int>,

    @SerializedName("daily_trend")
    val dailyTrend: List<DailyTrendItem>
)

/**
 * 日趋势数据
 */
data class DailyTrendItem(
    @SerializedName("date")
    val date: String,

    @SerializedName("count")
    val count: Int,

    @SerializedName("qualified_count")
    val qualifiedCount: Int
)