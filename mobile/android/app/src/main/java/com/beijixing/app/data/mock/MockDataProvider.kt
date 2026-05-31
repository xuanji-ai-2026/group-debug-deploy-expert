package com.beijixing.app.data.mock

import com.beijixing.app.data.model.*
import java.util.concurrent.TimeUnit

object MockDataProvider {

    fun getDashboardData(): DashboardData = DashboardData(
        totalLeads = 128,
        todayNewLeads = 15,
        activeTasks = 6,
        totalAccounts = 8,
        unreadMessages = 3,
        balance = UserBalance(
            balance = 1280.50,
            points = 12800,
            frozen = 200.00,
            totalRecharge = 5000.00
        ),
        recentLeads = getRecentLeads(),
        recentTasks = getRecentTasks(),
        leadTrend = getLeadTrend()
    )

    fun getMockUser(): User = User(
        userId = 1001L,
        tenantId = 1L,
        phone = "138****8888",
        nickname = "张经理",
        avatar = null,
        role = "ADMIN",
        status = 1,
        createTime = "2024-01-15 10:30:00"
    )

    fun getLeadList(): List<Lead> = listOf(
        Lead(
            id = 1L, leadNo = "L20260518001", title = "【截客】竞品A意向客户",
            companyName = "昆明云创科技有限公司",
            name = "李总", phone = "138****1234",
            source = "INTERCEPT", sourceDesc = "同业截客",
            level = "A", levelDesc = "A级-高意向", status = "NEW", statusDesc = "新建",
            needs = "需要一套完整的社媒获客系统，预算充足，希望本月内能上线",
            budget = "5-10万", area = "昆明市",
            intentScore = 85, isIntercept = true,
            assigneeId = 1L, assigneeName = "张经理",
            createTime = "2026-05-18 09:30:00"
        ),
        Lead(
            id = 2L, leadNo = "L20260518002", title = "小红书-美妆行业咨询",
            companyName = "云南电商联盟",
            name = "王经理", phone = "139****5678",
            source = "XIAOHONGSHU", sourceDesc = "小红书",
            level = "B", levelDesc = "B级-中意向", status = "FOLLOWING", statusDesc = "跟进中",
            needs = "咨询抖音截客功能，对比了多家供应商",
            budget = "2-5万", area = "大理市",
            intentScore = 75, isIntercept = false,
            assigneeId = 1L, assigneeName = "张经理",
            followCount = 2, lastFollowTime = "2026-05-17 16:30:00",
            createTime = "2026-05-17 10:15:00"
        ),
        Lead(
            id = 3L, leadNo = "L20260518003", title = "快手-集团采购意向",
            companyName = "成都新零售集团",
            name = "陈总监", phone = "137****9012",
            source = "KUAISHOU", sourceDesc = "快手",
            level = "A", levelDesc = "A级-高意向", status = "FOLLOWING", statusDesc = "跟进中",
            needs = "集团需要为10个子公司部署获客系统，意向很强",
            budget = "20-50万", area = "成都市",
            intentScore = 90, isIntercept = false,
            assigneeId = 2L, assigneeName = "李顾问",
            createTime = "2026-05-16 08:45:00"
        ),
        Lead(
            id = 4L, leadNo = "L20260518004", title = "微博-初步咨询",
            name = "刘老板", phone = "136****3456",
            source = "WEIBO", sourceDesc = "微博",
            level = "C", levelDesc = "C级-低意向", status = "NEW", statusDesc = "新建",
            needs = "简单了解一下，暂时没有明确需求",
            budget = "待定", area = "曲靖市",
            intentScore = 40, isIntercept = false,
            createTime = "2026-05-18 07:20:00"
        ),
        Lead(
            id = 5L, leadNo = "L20260518005", title = "抖音-文旅行业已成交",
            companyName = "贵州文旅科技",
            name = "赵总", phone = "135****7890",
            source = "DOUYIN", sourceDesc = "抖音",
            level = "B", levelDesc = "B级-中意向", status = "DEALED", statusDesc = "已成交",
            needs = "文旅行业获客方案，已成交",
            budget = "8万", area = "贵阳市",
            intentScore = 80, isIntercept = false,
            assigneeId = 1L, assigneeName = "张经理",
            createTime = "2026-05-10 14:30:00"
        )
    )

    fun getRecentLeads(): List<Lead> = getLeadList().take(3)

    fun getTaskList(): List<Task> = listOf(
        Task(
            taskId = 1L, tenantId = 1L,
            name = "抖音-同业截客任务（昆明区域）",
            type = "INTERCEPT", status = "RUNNING",
            platforms = listOf("DOUYIN"),
            keywords = listOf("获客系统", "AI营销", "智能客服"),
            totalCount = 500, completedCount = 320,
            successCount = 285, failCount = 35,
            progress = 64,
            startTime = "2026-05-18 08:00:00",
            endTime = "2026-05-18 20:00:00",
            createTime = "2026-05-18 07:30:00",
            updateTime = "2026-05-18 12:30:00"
        ),
        Task(
            taskId = 2L, tenantId = 1L,
            name = "小红书-主动获客（美妆行业）",
            type = "ACTIVE_CAPTURE", status = "RUNNING",
            platforms = listOf("XIAOHONGSHU"),
            keywords = listOf("美妆博主", "护肤心得", "种草推荐"),
            totalCount = 300, completedCount = 180,
            successCount = 162, failCount = 18,
            progress = 60,
            startTime = "2026-05-18 09:00:00",
            endTime = "2026-05-18 18:00:00",
            createTime = "2026-05-18 08:20:00",
            updateTime = "2026-05-18 12:15:00"
        ),
        Task(
            taskId = 3L, tenantId = 1L,
            name = "多平台-内容批量发布",
            type = "CONTENT_PUBLISH", status = "PAUSED",
            platforms = listOf("DOUYIN", "KUAISHOU", "XIAOHONGSHU"),
            keywords = listOf(),
            totalCount = 50, completedCount = 20,
            successCount = 18, failCount = 2,
            progress = 40,
            startTime = "2026-05-17 14:00:00",
            errorMsg = "用户暂停",
            createTime = "2026-05-17 13:30:00",
            updateTime = "2026-05-18 10:00:00"
        ),
        Task(
            taskId = 4L, tenantId = 1L,
            name = "快手-私信触达（教育行业）",
            type = "CUSTOM_MESSAGE", status = "PENDING",
            platforms = listOf("KUAISHOU"),
            keywords = listOf("在线教育", "培训课程"),
            totalCount = 200, completedCount = 0,
            successCount = 0, failCount = 0,
            progress = 0,
            startTime = "2026-05-18 15:00:00",
            endTime = "2026-05-18 22:00:00",
            createTime = "2026-05-18 11:00:00",
            updateTime = "2026-05-18 11:00:00"
        ),
        Task(
            taskId = 5L, tenantId = 1L,
            name = "微博-同业截客（电商关键词）",
            type = "INTERCEPT", status = "COMPLETED",
            platforms = listOf("WEIBO"),
            keywords = listOf("电商平台", "开店教程", "货源渠道"),
            totalCount = 400, completedCount = 400,
            successCount = 368, failCount = 32,
            progress = 100,
            startTime = "2026-05-17 10:00:00",
            actualEndTime = "2026-05-17 18:30:00",
            createTime = "2026-05-17 09:00:00",
            updateTime = "2026-05-17 18:30:00"
        )
    )

    fun getRecentTasks(): List<Task> = getTaskList().filter { it.status == "RUNNING" || it.status == "PAUSED" }.take(3)

    fun getLeadTrend(): List<TrendData> = listOf(
        TrendData(date = "05-12", value = 8, label = "周一"),
        TrendData(date = "05-13", value = 12, label = "周二"),
        TrendData(date = "05-14", value = 18, label = "周三"),
        TrendData(date = "05-15", value = 25, label = "周四"),
        TrendData(date = "05-16", value = 22, label = "周五"),
        TrendData(date = "05-17", value = 28, label = "周六"),
        TrendData(date = "05-18", value = 15, label = "今天")
    )

    fun getLeadStats(): LeadStats = LeadStats(
        total = 128,
        newCount = 23,
        followingCount = 45,
        dealedCount = 52,
        highLevelCount = 18,
        alertCount = 5
    )

    fun getTaskSummary(): TaskSummary = TaskSummary(
        totalTasks = 24,
        runningTasks = 6,
        completedTasks = 15,
        failedTasks = 3,
        totalProcessed = 12580L,
        successRate = 89.5
    )

    fun getSocialAccounts(): List<SocialAccount> = listOf(
        SocialAccount(id = 1L, platform = "DOUYIN", accountName = "北极星AI官方", accountNo = "beijixing_ai", avatar = null, status = "ACTIVE", healthStatus = "HEALTHY", lastCheckTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5)),
        SocialAccount(id = 2L, platform = "XIAOHONGSHU", accountName = "北极星获客助手", accountNo = "bjx_helper", avatar = null, status = "ACTIVE", healthStatus = "HEALTHY", lastCheckTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10)),
        SocialAccount(id = 3L, platform = "KUAISHOU", accountName = "北极星运营", accountNo = "bjx_ops", avatar = null, status = "ACTIVE", healthStatus = "WARNING", lastCheckTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)),
        SocialAccount(id = 4L, platform = "WEIBO", accountName = "北极星AI科技", accountNo = "beijixing_tech", avatar = null, status = "BANNED", healthStatus = "ERROR", lastCheckTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24))
    )
}
