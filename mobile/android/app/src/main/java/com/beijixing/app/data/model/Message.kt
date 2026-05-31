package com.beijixing.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Message Data Model
 *
 * Corresponds to backend message service (bx-message) Message entity
 */
data class Message(
    @SerializedName("message_id")
    val messageId: Long = 0L,

    @SerializedName("tenant_id")
    val tenantId: Long = 0L,

    @SerializedName("user_id")
    val userId: Long = 0L,

    @SerializedName("title")
    val title: String = "",

    @SerializedName("content")
    val content: String = "",

    @SerializedName("type")
    val type: String = "SYSTEM",

    @SerializedName("sub_type")
    val subType: String? = null,

    @SerializedName("is_read")
    val isRead: Boolean = false,

    @SerializedName("priority")
    val priority: String = "NORMAL",

    @SerializedName("target_id")
    val targetId: Long? = null,

    @SerializedName("target_type")
    val targetType: String? = null,

    @SerializedName("extra_data")
    val extraData: String? = null,

    @SerializedName("create_time")
    val createTime: String = "",

    @SerializedName("read_time")
    val readTime: String? = null
) {
    fun getTypeText(): String = when (type) {
        "SYSTEM" -> "System Notification"
        "LEAD" -> "Lead Alert"
        "TASK" -> "Task Notification"
        else -> "Unknown"
    }

    fun getSubTypeText(): String = when (subType) {
        "LEAD_NEW" -> "New Lead"
        "LEAD_FOLLOW" -> "Follow Up Required"
        "LEAD_EXPIRE" -> "Expiring Soon"
        "TASK_START" -> "Task Started"
        "TASK_COMPLETE" -> "Task Completed"
        "TASK_FAIL" -> "Task Failed"
        "TASK_PAUSE" -> "Task Paused"
        "SYSTEM_MAINTENANCE" -> "System Maintenance"
        "SYSTEM_UPDATE" -> "Version Update"
        "POINTS_LOW" -> "Low Points"
        else -> subType ?: ""
    }

    fun getPriorityText(): String = when (priority) {
        "HIGH" -> "High"
        "NORMAL" -> "Normal"
        "LOW" -> "Low"
        else -> "Normal"
    }

    fun canNavigate(): Boolean = targetId != null && targetType != null
}

data class MessageListRequest(
    @SerializedName("type")
    val type: String? = null,

    @SerializedName("is_read")
    val isRead: Boolean? = null,

    @SerializedName("page")
    val page: Int = 1,

    @SerializedName("page_size")
    val pageSize: Int = 20
)

data class MarkReadRequest(
    @SerializedName("message_ids")
    val messageIds: List<Long>? = null
)

data class MessageStats(
    @SerializedName("total")
    val total: Int = 0,

    @SerializedName("unread_count")
    val unreadCount: Int = 0,

    @SerializedName("system_count")
    val systemCount: Int = 0,

    @SerializedName("lead_count")
    val leadCount: Int = 0,

    @SerializedName("task_count")
    val taskCount: Int = 0
)

data class WebSocketMessage(
    @SerializedName("action")
    val action: String = "",

    @SerializedName("message")
    val message: Message? = null,

    @SerializedName("message_ids")
    val messageIds: List<Long>? = null,

    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
