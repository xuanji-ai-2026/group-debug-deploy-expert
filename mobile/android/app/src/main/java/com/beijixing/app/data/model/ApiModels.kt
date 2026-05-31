package com.beijixing.app.data.model

import com.google.gson.annotations.SerializedName

data class UserBalance(
    val balance: Double = 0.0,
    val points: Int = 0,
    val frozen: Double = 0.0,
    val totalRecharge: Double = 0.0
)

data class UpdateProfileRequest(
    val nickname: String? = null,
    val avatar: String? = null,
    val email: String? = null,
    val phone: String? = null
)

data class RegisterRequest(
    @SerializedName("phone")
    val phone: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("nick_name")
    val nickName: String? = null,

    @SerializedName("device_id")
    val deviceId: String? = null
)

data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class FollowRecordRequest(
    val content: String,
    val type: String = "NOTE"
)

data class TaskDetail(
    val task: Task? = null,
    val logs: List<TaskLog> = emptyList(),
    val stats: TaskStats? = null
)

data class TaskStats(
    val totalProcessed: Int = 0,
    val successCount: Int = 0,
    val failCount: Int = 0,
    val pendingCount: Int = 0,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val durationSeconds: Long = 0,
    val avgProcessTime: Double = 0.0
)

data class SocialAccount(
    val id: Long = 0,
    val platform: String = "",
    val accountName: String = "",
    val accountNo: String = "",
    val avatar: String? = null,
    val status: String = "ACTIVE",
    val healthStatus: String = "UNKNOWN",
    val lastCheckTime: Long? = null,
    val createTime: Long = System.currentTimeMillis()
) {
    fun getPlatformDisplayName(): String {
        return when (platform) {
            "WECHAT" -> "微信"
            "DOUYIN" -> "抖音"
            "XIAOHONGSHU" -> "小红书"
            "WEIBO" -> "微博"
            "LINKEDIN" -> "领英"
            else -> platform
        }
    }
}

data class AddAccountRequest(
    val platform: String,
    val accountName: String,
    val accountNo: String,
    val password: String? = null,
    val cookie: String? = null,
    val extraInfo: Map<String, Any>? = null
)

data class AccountHealthResult(
    val accountId: Long,
    val isHealthy: Boolean,
    val status: String,
    val message: String,
    val checkTime: Long = System.currentTimeMillis(),
    val details: Map<String, Any>? = null
)

// ==================== 评论抓取相关数据模型 ====================

data class CommentFilterResult(
    val totalCount: Int = 0,
    val filteredCount: Int = 0,
    val comments: List<SocialComment> = emptyList(),
    val statistics: FilterStatistics? = null
)

data class FilterStatistics(
    val totalCount: Int = 0,
    val highIntentCount: Int = 0,
    val withPhoneCount: Int = 0,
    val withWechatCount: Int = 0,
    val avgIntentScore: Double = 0.0,
    val scoreDistribution: Map<String, Int>? = null,
    val levelDistribution: Map<String, Int>? = null,
    val platformDistribution: Map<String, Int>? = null
)

data class CreateTemplateRequest(
    val templateName: String,
    val platformCode: String,
    val intentLevel: String,
    val templateContent: String,
    val isDefault: Boolean = false
)

data class PlatformInfo(
    val code: String,
    val name: String,
    val icon: String? = null,
    val supportedFeatures: List<String> = emptyList(),
    val isActive: Boolean = true
)

data class RechargePackage(
    val id: Long = 0,
    val name: String = "",
    val amount: Double = 0.0,
    val points: Int = 0,
    val bonusPoints: Int = 0,
    val description: String = "",
    val isPopular: Boolean = false,
    val sortOrder: Int = 0
)

data class CreateOrderRequest(
    val packageId: Long,
    val payType: String,
    val couponId: Long? = null
)

data class GeneratedContent(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val type: String = "",
    val templateId: Long? = null,
    val status: String = "DRAFT",
    val createTime: Long = System.currentTimeMillis(),
    val publishPlatforms: List<String> = emptyList()
)

data class GenerateContentRequest(
    val type: String,
    val topic: String,
    val templateId: Long? = null,
    val params: Map<String, Any>? = null
)

data class PublishContentRequest(
    val contentId: Long,
    val platforms: List<String>,
    val publishTime: Long? = null,
    val extraParams: Map<String, Any>? = null
)

data class DashboardData(
    val totalLeads: Int = 0,
    val todayNewLeads: Int = 0,
    val activeTasks: Int = 0,
    val totalAccounts: Int = 0,
    val unreadMessages: Int = 0,
    val balance: UserBalance? = null,
    val recentLeads: List<Lead> = emptyList(),
    val recentTasks: List<Task> = emptyList(),
    val leadTrend: List<TrendData> = emptyList()
)

data class TrendData(
    val date: String = "",
    val value: Int = 0,
    val label: String? = null
)

data class TaskSummary(
    val totalTasks: Int = 0,
    val runningTasks: Int = 0,
    val completedTasks: Int = 0,
    val failedTasks: Int = 0,
    val totalProcessed: Long = 0L,
    val successRate: Double = 0.0
)

// ============================================================
// 养号策略数据模型 (Nurturing Strategy Models)
// ============================================================

data class NurturingStrategy(
    val id: Long = 0,
    val accountId: Long = 0,
    val accountName: String? = null,
    val accountAvatar: String? = null,
    val platform: String? = null,
    val strategyName: String? = null,
    val dailyTargets: DailyTargets? = null,
    val durationDays: Int = 7,
    val riskLevel: String = "LOW",
    val enabled: Int = 0,
    val nurturingStatus: Int = 0,
    val createTime: String? = null,
    val updateTime: String? = null
)

data class DailyTargets(
    val likeCount: Int = 10,
    val commentCount: Int = 5,
    val shareCount: Int = 2,
    val followCount: Int = 3,
    val browseDuration: Int = 30
)

data class NurturingStrategyRequest(
    val accountId: Long,
    val strategyName: String,
    val dailyTargets: DailyTargets,
    val durationDays: Int = 7,
    val riskLevel: String = "LOW"
)

data class NurturingProgress(
    val strategyId: Long = 0,
    val accountId: Long = 0,
    val status: String = "NOT_RUNNING",
    val dailyTargets: Map<String, Int>? = null,
    val completedCounts: Map<String, Int>? = null,
    val progressPercentage: Double = 0.0,
    val startTime: String? = null
)

data class NurturingTemplate(
    val name: String = "",
    val description: String = "",
    val dailyTargets: DailyTargets? = null,
    val duration: Int = 7,
    val riskLevel: String = "LOW"
)

// ============================================================
// 风控数据模型 (Risk Control Models)
// ============================================================

data class RiskCheckRequest(
    val eventType: String,
    val userId: String,
    val content: String? = null,
    val targetId: String? = null,
    val context: Map<String, Any>? = null
)

data class RiskCheckResult(
    val riskLevel: Int = 0,
    val riskScore: Double = 0.0,
    val matchedRules: List<String> = emptyList(),
    val action: Int = 0,
    val message: String? = null
)

data class RiskRule(
    val id: Long = 0,
    val ruleName: String = "",
    val ruleType: Int = 0,
    val ruleConfig: String = "",
    val action: Int = 1,
    val priority: Int = 0,
    val status: Int = 1
)

data class RiskScore(
    val accountId: Long = 0,
    val score: Int = 100,
    val level: String = "LOW",
    val lastCheckTime: String? = null,
    val factors: List<RiskFactor> = emptyList()
)

data class RiskFactor(
    val name: String = "",
    val score: Int = 0,
    val status: String = "NORMAL",
    val message: String? = null
)
