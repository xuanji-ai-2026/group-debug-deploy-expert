package com.beijixing.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime

@Parcelize
data class CrawlTask(
    val id: Long? = null,
    val taskName: String? = null,
    val taskType: String? = null,
    val platformCode: String? = null,
    val targetId: String? = null,
    val keywords: String? = null,
    val maxCrawlCount: Int? = null,
    val crawlIntervalSeconds: Int? = null,
    val totalCommentsFound: Int? = null,
    val highIntentCount: Int? = null,
    val leadsGenerated: Int? = null,
    val messagesSent: Int? = null,
    val status: Int? = null,
    val progressPercent: Int? = null,
    val errorMessage: String? = null,
    val startTime: LocalDateTime? = null,
    val endTime: LocalDateTime? = null,
    val createTime: LocalDateTime? = null,
    val updateTime: LocalDateTime? = null
) : Parcelable

@Parcelize
data class SocialComment(
    val id: Long? = null,
    val commentId: String? = null,
    val crawlTaskId: Long? = null,
    val platformCode: String? = null,
    val contentId: String? = null,
    val authorId: String? = null,
    val authorName: String? = null,
    val authorAvatar: String? = null,
    val authorBio: String? = null,
    val userFollowerCount: Long? = null,
    val userFollowingCount: Long? = null,
    val userVerified: Boolean? = null,
    val commentText: String? = null,
    val likeCount: Int? = null,
    val replyCount: Int? = null,
    val publishTime: LocalDateTime? = null,
    val extractedPhone: String? = null,
    val extractedWechat: String? = null,
    val hasPhoneContact: Boolean? = false,
    val hasWechatContact: Boolean? = false,
    val aiIntentScore: Int? = null,
    val aiIntentLevel: String? = null,
    val isHighIntent: Boolean? = false,
    val aiAnalysisResult: String? = null,
    val leadGenerated: Boolean? = false,
    val generatedLeadId: Long? = null,
    val messageSent: Boolean? = false,
    val messageSentTime: LocalDateTime? = null,
    val deleted: Int? = 0,
    val createTime: LocalDateTime? = null,
    val updateTime: LocalDateTime? = null
) : Parcelable

@Parcelize
data class MessageTemplate(
    val id: Long? = null,
    val templateName: String? = null,
    val platformCode: String? = null,
    val intentLevel: String? = null,
    val templateContent: String? = null,
    val templateVariables: String? = null,
    val aiGenerated: Boolean? = false,
    val isDefault: Boolean? = false,
    val useCount: Int? = null,
    val successRate: Double? = null,
    val replyRate: Double? = null,
    val status: Int? = 1,
    val createTime: LocalDateTime? = null,
    val updateTime: LocalDateTime? = null
) : Parcelable

@Parcelize
data class MessageResult(
    val success: Boolean = false,
    val commentId: Long? = null,
    val platformCode: String? = null,
    val authorId: String? = null,
    val authorName: String? = null,
    val messageContent: String? = null,
    val templateId: Long? = null,
    val templateName: String? = null,
    val errorMessage: String? = null,
    val sendTime: LocalDateTime? = null
) : Parcelable

@Parcelize
data class LeadResult(
    val success: Boolean = false,
    val generatedCount: Int = 0,
    val skippedCount: Int = 0,
    val errors: Map<Long, String>? = null,
    val generatedLeads: List<CrawlLead>? = null
) : Parcelable

@Parcelize
data class CrawlLead(
    val id: Long? = null,
    val title: String? = null,
    val source: String? = null,
    val channel: String? = null,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val level: String? = null,
    val intentScore: Int? = null,
    val requirementDesc: String? = null,
    val status: String? = null,
    val createTime: LocalDateTime? = null
) : Parcelable

data class SendMessageRequest(
    val commentId: Long,
    val templateId: Long?,
    val content: String
)

data class BatchSendMessageRequest(
    val commentIds: List<Long>,
    val templateId: Long?,
    val maxConcurrent: Int = 5,
    val intervalMs: Int = 30000
)

data class GenerateLeadsRequest(
    val minScore: Int? = null,
    val requiredLevels: List<String>? = null,
    val requireContactInfo: Boolean = false,
    val minFollowerCount: Int? = null,
    val autoAssign: Boolean = true,
    val generateFollowUpTask: Boolean = true
)

data class BatchGenerateLeadsRequest(
    val commentIds: List<Long>,
    val autoAssign: Boolean = true,
    val generateFollowUpTask: Boolean = true
)
