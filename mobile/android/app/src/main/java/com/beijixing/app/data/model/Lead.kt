package com.beijixing.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * 商机数据模型
 *
 * 对应后端商机服务（bx-lead）的Lead实体
 * 产品需求对应：第七章 商机获客系统
 *
 * @property leadId         商机ID
 * @property tenantId       租户ID（多租户隔离）
 * @property name           客户姓名
 * @property phone          联系电话
 * @property wechat         微信（可选）
 * @property source         来源平台：DOUYIN（抖音）/ KUAISHOU（快手）/ XIAOHONGSHU（小红书）/ WEIBO（微博）等
 * @property sourceId       来源账号ID（用于追踪截客 7.1.2节）
 * @property level          意向等级：HIGH（高）/ MEDIUM（中）/ LOW（低）
 * @property status         商机状态：NEW（新增）/ FOLLOWING（跟进中）/ DEALED（已成交）/ LOST（已流失）
 * @property needs          需求描述
 * @property budget         预算范围
 * @property area           意向区域
 * @property tags           标签列表（用于人群定向 7.2.1节）
 * @property assigneeId     跟进人ID
 * @property assigneeName   跟进人姓名
 * @property followRecords  跟进记录列表
 * @property createTime     创建时间
 * @property updateTime     更新时间
 */
data class Lead(
    @SerializedName("id")
    val id: Long = 0L,

    @SerializedName("leadNo")
    val leadNo: String = "",

    @SerializedName("title")
    val title: String = "",

    @SerializedName("customerName")
    val name: String = "",

    @SerializedName("customerPhone")
    val phone: String = "",

    @SerializedName("customerEmail")
    val email: String? = null,

    @SerializedName("customerCompany")
    val companyName: String? = null,

    @SerializedName("industry")
    val industry: String? = null,

    @SerializedName("region")
    val area: String? = null,

    @SerializedName("source")
    val source: String = "",

    @SerializedName("sourceDesc")
    val sourceDesc: String? = null,

    @SerializedName("channel")
    val channel: String? = null,

    @SerializedName("level")
    val level: String = "C",

    @SerializedName("levelDesc")
    val levelDesc: String = "",

    @SerializedName("status")
    val status: String = "NEW",

    @SerializedName("statusDesc")
    val statusDesc: String = "",

    @SerializedName("requirementDesc")
    val needs: String = "",

    @SerializedName("budgetAmount")
    val budget: String? = null,

    @SerializedName("intentScore")
    val intentScore: Int = 0,

    @SerializedName("isIntercept")
    val isIntercept: Boolean = false,

    @SerializedName("competitorKeywords")
    val competitorKeywords: String? = null,

    @SerializedName("aiAnalysisResult")
    val aiAnalysisResult: String? = null,

    @SerializedName("ownerId")
    val assigneeId: Long? = null,

    @SerializedName("ownerName")
    val assigneeName: String? = null,

    @SerializedName("followCount")
    val followCount: Int = 0,

    @SerializedName("lastFollowTime")
    val lastFollowTime: String? = null,

    @SerializedName("createTime")
    val createTime: String = "",

    @SerializedName("updateTime")
    val updateTime: String = ""
) {
    /**
     * 获取意向等级显示文本
     */
    fun getLevelText(): String = when (level) {
        "A" -> "A级-高意向"
        "B" -> "B级-中意向"
        "C" -> "C级-低意向"
        "HIGH" -> "A级-高意向"
        "MEDIUM" -> "B级-中意向"
        "LOW" -> "C级-低意向"
        else -> levelDesc.ifEmpty { "未知" }
    }

    /**
     * 获取状态显示文本
     */
    fun getStatusText(): String = when (status) {
        "NEW" -> "新商机"
        "FOLLOWING" -> "跟进中"
        "QUOTED" -> "已报价"
        "DEALED" -> "已成交"
        "LOST" -> "已流失"
        else -> statusDesc.ifEmpty { "未知" }
    }

    /**
     * 获取来源平台显示名称
     */
    fun getSourceName(): String = when (source) {
        "INTERCEPT" -> "同业截客"
        "WEBSITE" -> "官网注册"
        "REFERRAL" -> "客户推荐"
        "QA" -> "问答平台"
        "FORUM" -> "论坛"
        "SOCIAL" -> "社交媒体"
        "DOUYIN" -> "抖音"
        "KUAISHOU" -> "快手"
        "XIAOHONGSHU" -> "小红书"
        "WEIBO" -> "微博"
        "BAIJIA" -> "百家号"
        "WEIXIN_VIDEO" -> "微信视频号"
        "ZHIHU" -> "知乎"
        "BILIBILI" -> "B站"
        else -> sourceDesc ?: source
    }
}

/**
 * 跟进记录数据模型
 *
 * @property recordId    记录ID
 * @property leadId      商机ID
 * @property content     跟进内容
 * @property type        跟进类型：CALL（电话）/ MESSAGE（短信）/ VISIT（上门）/ OTHER（其他）
 * @property nextTime    下次跟进时间
 * @property creatorId   记录创建人ID
 * @property creatorName 记录创建人姓名
 * @property createTime  创建时间
 */
data class FollowRecord(
    @SerializedName("record_id")
    val recordId: Long = 0L,

    @SerializedName("lead_id")
    val leadId: Long = 0L,

    @SerializedName("content")
    val content: String = "",

    @SerializedName("type")
    val type: String = "OTHER",

    @SerializedName("next_time")
    val nextTime: String? = null,

    @SerializedName("creator_id")
    val creatorId: Long = 0L,

    @SerializedName("creator_name")
    val creatorName: String = "",

    @SerializedName("create_time")
    val createTime: String = ""
)

/**
 * 商机列表请求参数
 *
 * @property keyword     搜索关键词（匹配姓名、手机号、需求描述）
 * @property level       意向等级筛选
 * @property status      状态筛选
 * @property source      来源平台筛选
 * @property page        页码
 * @property pageSize    每页数量
 */
data class LeadListRequest(
    @SerializedName("keyword")
    val keyword: String? = null,

    @SerializedName("level")
    val level: String? = null,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("source")
    val source: String? = null,

    @SerializedName("pageNum")
    val pageNum: Int = 1,

    @SerializedName("pageSize")
    val pageSize: Int = 20
)

/**
 * 添加商机请求
 *
 * @property name    客户姓名
 * @property phone   联系电话
 * @property wechat  微信（可选）
 * @property source  来源平台
 * @property level   意向等级
 * @property needs   需求描述
 * @property budget  预算范围
 * @property area    意向区域
 * @property tags    标签
 */
data class AddLeadRequest(
    @SerializedName("name")
    val name: String,

    @SerializedName("phone")
    val phone: String,

    @SerializedName("wechat")
    val wechat: String? = null,

    @SerializedName("source")
    val source: String = "",

    @SerializedName("level")
    val level: String = "MEDIUM",

    @SerializedName("needs")
    val needs: String = "",

    @SerializedName("budget")
    val budget: String? = null,

    @SerializedName("area")
    val area: String? = null,

    @SerializedName("tags")
    val tags: List<String>? = null
)

/**
 * 添加跟进记录请求
 *
 * @property leadId   商机ID
 * @property content 跟进内容
 * @property type    跟进类型
 * @property nextTime 下次跟进时间（可选）
 */
data class AddFollowRequest(
    @SerializedName("lead_id")
    val leadId: Long,

    @SerializedName("content")
    val content: String,

    @SerializedName("type")
    val type: String = "OTHER",

    @SerializedName("next_time")
    val nextTime: String? = null
)

/**
 * 商机统计模型
 *
 * @property total     总商机数
 * @property newCount  新增商机数
 * @property followingCount 跟进中数量
 * @property dealedCount    已成交数量
 * @property highLevelCount 高意向数量
 * @property alertCount     待处理预警数
 */
data class LeadStats(
    @SerializedName("total")
    val total: Int = 0,

    @SerializedName("newToday")
    val newCount: Int = 0,

    @SerializedName("pendingFollowUp")
    val followingCount: Int = 0,

    @SerializedName("convertedThisWeek")
    val dealedCount: Int = 0,

    @SerializedName("highLevelCount")
    val highLevelCount: Int = 0,

    @SerializedName("alertCount")
    val alertCount: Int = 0
)
