package com.beijixing.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.beijixing.app.R
import com.beijixing.app.ui.main.MainActivity

/**
 * 通知Service
 *
 * 功能说明：
 * - 管理所有APP内推送通知
 * - 创建和管理通知渠道（Android 8.0+）
 * - 提供商机预警、任务进度、积分变动等通知能力
 *
 * 产品需求对应：
 * - 5.1节 通知权限 - 消息推送
 * - 7.1.3节 高意向用户筛选 - 商机预警通知
 * - 7.3.3节 跟进提醒 - 超24小时未跟进自动提醒
 */
class NotificationService {

    companion object {
        // 通知渠道定义
        const val CHANNEL_ALERT = "beijixing_alert_channel"     // 商机预警渠道
        const val CHANNEL_TASK = "beijixing_task_channel"       // 任务进度渠道
        const val CHANNEL_POINTS = "beijixing_points_channel"   // 积分变动渠道
        const val CHANNEL_SYSTEM = "beijixing_system_channel"   // 系统通知渠道

        // 通知ID基数（避免不同类型通知ID冲突）
        const val ID_ALERT_BASE = 2000
        const val ID_TASK_BASE = 3000
        const val ID_POINTS_BASE = 4000
        const val ID_SYSTEM_BASE = 5000

        @Volatile
        private var instance: NotificationService? = null

        fun getInstance(): NotificationService {
            return instance ?: synchronized(this) {
                instance ?: NotificationService().also { instance = it }
            }
        }
    }

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    /**
     * 初始化（应在Application中调用）
     */
    fun init(context: Context) {
        this.context = context.applicationContext
        this.notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        createNotificationChannels()
    }

    /**
     * 创建所有通知渠道（Android 8.0+必须）
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                // 商机预警渠道 - 高优先级
                NotificationChannel(
                    CHANNEL_ALERT,
                    "商机预警",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "高意向客户、商机预警提醒"
                    enableVibration(true)
                },

                // 任务进度渠道 - 中等优先级
                NotificationChannel(
                    CHANNEL_TASK,
                    "任务进度",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "截客任务、获客任务执行状态"
                    enableVibration(true)
                },

                // 积分变动渠道 - 低优先级
                NotificationChannel(
                    CHANNEL_POINTS,
                    "积分变动",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "积分消耗、充值到账通知"
                },

                // 系统通知渠道 - 低优先级
                NotificationChannel(
                    CHANNEL_SYSTEM,
                    "系统通知",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "系统公告、更新提醒"
                }
            )

            channels.forEach { notificationManager.createNotificationChannel(it) }
        }
    }

    /**
     * 发送商机预警通知
     *
     * @param leadId 商机ID（用于点击跳转）
     * @param title 通知标题
     * @param content 通知内容
     */
    fun showLeadAlertNotification(leadId: Long, title: String, content: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("action", "lead_detail")
            putExtra("lead_id", leadId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context, leadId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_alert)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // 点击后自动消失
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(ID_ALERT_BASE + leadId.toInt(), notification)
        } catch (e: SecurityException) {
            // 用户未授予通知权限，静默处理
        }
    }

    /**
     * 发送任务进度通知
     *
     * @param taskId 任务ID
     * @param title 任务名称
     * @param progress 进度百分比
     * @param status 任务状态
     */
    fun showTaskProgressNotification(taskId: Long, title: String, progress: Int, status: String) {
        val notificationId = ID_TASK_BASE + taskId.toInt()

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("action", "task_detail")
            putExtra("task_id", taskId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val content = when (status) {
            "COMPLETED" -> "任务已完成！成功: $progress%"
            "FAILED" -> "任务执行失败，请检查配置"
            else -> "任务进行中: $progress%"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_TASK)
            .setSmallIcon(R.drawable.ic_task)
            .setContentTitle(title)
            .setContentText(content)
            .setProgress(100, progress, false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // 用户未授予通知权限
        }
    }

    /**
     * 发送积分变动通知
     *
     * @param type 变动类型：RECHARGE / CONSUME
     * @param points 变动积分
     * @param balance 变动后余额
     */
    fun showPointsNotification(type: String, points: Long, balance: Long) {
        val notificationId = if (type == "RECHARGE") ID_POINTS_BASE else ID_POINTS_BASE + 1000

        val (title, content) = if (type == "RECHARGE") {
            "积分充值成功" to "+$points 积分已到账，当前余额: $balance"
        } else {
            "积分消耗提醒" to "-$points 积分，当前余额: $balance"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("action", "points")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_POINTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // 用户未授予通知权限
        }
    }

    /**
     * 取消指定通知
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    /**
     * 取消所有通知
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}
