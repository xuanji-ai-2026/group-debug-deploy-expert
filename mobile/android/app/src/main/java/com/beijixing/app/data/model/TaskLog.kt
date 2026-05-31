package com.beijixing.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * 任务执行日志
 *
 * @property logId      日志ID
 * @property taskId     任务ID
 * @property action     操作类型
 * @property message    日志消息
 * @property status     状态：SUCCESS/FAILED/WARNING
 * @property createTime 创建时间
 */
data class TaskLog(
    @SerializedName("log_id")
    val logId: Long = 0L,

    @SerializedName("task_id")
    val taskId: Long = 0L,

    @SerializedName("action")
    val action: String = "",

    @SerializedName("message")
    val message: String = "",

    @SerializedName("status")
    val status: String = "",

    @SerializedName("create_time")
    val createTime: String = ""
)