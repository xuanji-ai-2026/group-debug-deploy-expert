package com.beijixing.app.data.remote

import com.beijixing.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<User>>

    @POST("auth/send-code")
    suspend fun sendSmsCode(@Body request: Map<String, String>): Response<ApiResponse<Map<String, Any>>>

    @POST("auth/send-email-code")
    suspend fun sendEmailCode(@Body request: Map<String, String>): Response<ApiResponse<Map<String, Any>>>

    @POST("auth/verify-code")
    suspend fun verifyCode(@Body request: Map<String, String>): Response<ApiResponse<Map<String, Any>>>

    @POST("auth/login-sms")
    suspend fun loginWithSmsCode(@Body request: Map<String, String>): Response<ApiResponse<Map<String, Any>>>

    @POST("auth/login-email")
    suspend fun loginWithEmail(@Body request: Map<String, String>): Response<ApiResponse<Map<String, Any>>>

    @POST("auth/register-login")
    suspend fun registerAndLogin(@Body request: Map<String, String>): Response<ApiResponse<Map<String, Any>>>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<ApiResponse<LoginResponse>>

    @POST("auth/refresh")
    fun refreshTokenSync(@Body request: RefreshTokenRequest): Response<ApiResponse<LoginResponse>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Map<String, Any>>>

    @GET("user/info")
    suspend fun getUserInfo(): Response<ApiResponse<User>>

    // 后端 UserController 使用 PUT /user/{id} 更新用户信息，前端自动从 token 获取 id
    @PUT("user/{id}")
    suspend fun updateProfile(@Path("id") userId: Long, @Body request: UpdateProfileRequest): Response<ApiResponse<User>>

    // 后端 UserController 使用 PUT /user/{id}/password
    @PUT("user/{id}/password")
    suspend fun changePassword(@Path("id") userId: Long, @Body request: ChangePasswordRequest): Response<ApiResponse<Map<String, Any>>>

    // 后端UserController已补齐/balance端点
    @GET("user/balance")
    suspend fun getUserBalance(): Response<ApiResponse<UserBalance>>

    @POST("lead/list")
    suspend fun getLeadList(
        @Body request: LeadListRequest
    ): Response<ApiResponse<PageResult<Lead>>>

    @GET("lead/{id}")
    suspend fun getLeadDetail(@Path("id") leadId: Long): Response<ApiResponse<Lead>>

    @POST("lead")
    suspend fun addLead(@Body request: AddLeadRequest): Response<ApiResponse<Lead>>

    @PUT("lead/{id}")
    suspend fun updateLead(@Path("id") leadId: Long, @Body request: AddLeadRequest): Response<ApiResponse<Lead>>

    @DELETE("lead/{id}")
    suspend fun deleteLead(@Path("id") leadId: Long): Response<ApiResponse<Map<String, Any>>>

    // 后端LeadController已补齐
    @POST("lead/{id}/follow")
    suspend fun addFollowRecord(@Path("id") leadId: Long, @Body request: FollowRecordRequest): Response<ApiResponse<FollowRecord>>

    // 后端LeadController已补齐
    @GET("lead/{id}/follows")
    suspend fun getFollowRecords(@Path("id") leadId: Long): Response<ApiResponse<List<FollowRecord>>>

    @GET("lead/stats")
    suspend fun getLeadStats(): Response<ApiResponse<LeadStats>>

    @GET("tasks/list")
    suspend fun getTaskList(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
        @Query("type") type: String? = null,
        @Query("status") status: String? = null
    ): Response<ApiResponse<PageResult<Task>>>

    @GET("tasks/{id}")
    suspend fun getTaskDetail(@Path("id") taskId: Long): Response<ApiResponse<TaskDetail>>

    @POST("tasks/create-acquire")
    suspend fun createAcquireTask(@Body request: CreateAcquireTaskRequest): Response<ApiResponse<Task>>

    @POST("tasks/create-intercept")
    suspend fun createInterceptTask(@Body request: CreateInterceptTaskRequest): Response<ApiResponse<Task>>

    @POST("tasks/{id}/start")
    suspend fun startTask(@Path("id") taskId: Long): Response<ApiResponse<Map<String, Any>>>

    @POST("tasks/{id}/pause")
    suspend fun pauseTask(@Path("id") taskId: Long): Response<ApiResponse<Map<String, Any>>>

    @POST("tasks/{id}/resume")
    suspend fun resumeTask(@Path("id") taskId: Long): Response<ApiResponse<Map<String, Any>>>

    @POST("tasks/{id}/stop")
    suspend fun stopTask(@Path("id") taskId: Long): Response<ApiResponse<Map<String, Any>>>

    @DELETE("tasks/{id}")
    suspend fun deleteTask(@Path("id") taskId: Long): Response<ApiResponse<Map<String, Any>>>

    @GET("tasks/{id}/logs")
    suspend fun getTaskLogs(
        @Path("id") taskId: Long,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 50
    ): Response<ApiResponse<PageResult<TaskLog>>>

    @GET("tasks/{id}/stats")
    suspend fun getTaskStats(@Path("id") taskId: Long): Response<ApiResponse<TaskStats>>

    // 后端 AccountController GET /account/page
    @GET("account/page")
    suspend fun getAccountList(
        @Query("platformCode") platform: String? = null,
        @Query("status") status: Int? = null,
        @Query("pageNum") pageNum: Long = 1,
        @Query("pageSize") pageSize: Long = 100
    ): Response<ApiResponse<Map<String, Any>>>

    @POST("account/save")
    suspend fun addAccount(@Body request: AddAccountRequest): Response<ApiResponse<SocialAccount>>

    // 后端 AccountController POST /account/{id}/unbind (解绑代替删除)
    @POST("account/{id}/unbind")
    suspend fun deleteAccount(@Path("id") accountId: Long): Response<ApiResponse<Map<String, Any>>>

    // 后端 AccountController 无 health-check，补充到后端
    @POST("account/{id}/health-check")
    suspend fun checkAccountHealth(@Path("id") accountId: Long): Response<ApiResponse<AccountHealthResult>>

    // 后端AccountController已补齐/platforms端点
    @GET("account/platforms")
    suspend fun getSupportedPlatforms(): Response<ApiResponse<List<PlatformInfo>>>

    // 后端 MessageController @RequestMapping("/messages")
    // GET /messages/private/{userId}?currentUserId= &page= &size=
    @GET("messages/private/{userId}")
    suspend fun getMessageList(
        @Path("userId") userId: Long,
        @Query("currentUserId") currentUserId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<PageResult<Message>>>

    // 后端无 /messages/{id} 详情，改为从 private 列表获取（或前端缓存）
    @GET("messages/session/{sessionId}")
    suspend fun getMessageDetail(@Path("sessionId") sessionId: String): Response<ApiResponse<Message>>

    // 后端 PUT /messages/{sessionId}/read?userId=
    @PUT("messages/{sessionId}/read")
    suspend fun markMessageRead(@Path("sessionId") sessionId: String, @Query("userId") userId: Long): Response<ApiResponse<Map<String, Any>>>

    // 后端 DELETE /messages/{messageId}/delete?userId=
    @DELETE("messages/{messageId}/delete")
    suspend fun deleteMessage(@Path("messageId") messageId: String, @Query("userId") userId: Long): Response<ApiResponse<Map<String, Any>>>

    // 后端无 read-all 端点，由后端 TaskController 补充
    @PUT("messages/read-all")
    suspend fun markAllMessagesRead(@Query("userId") userId: Long): Response<ApiResponse<Map<String, Any>>>

    // 后端 GET /messages/unread/count?userId=
    @GET("messages/unread/count")
    suspend fun getUnreadCount(@Query("userId") userId: Long): Response<ApiResponse<Map<String, Int>>>

    // SocialMessageController (@RequestMapping("/message"))
    @POST("message/send")
    suspend fun sendMessage(@Body request: SendMessageRequest): Response<MessageResult>

    @POST("message/batch-send")
    suspend fun batchSendMessage(@Body request: BatchSendMessageRequest): Response<MessageResult>

    @POST("message/batch-generate-leads")
    suspend fun batchGenerateLeads(@Body request: BatchGenerateLeadsRequest): Response<LeadResult>

    @GET("message/templates")
    suspend fun getMessageTemplates(
        @Query("platformCode") platformCode: String? = null,
        @Query("intentLevel") intentLevel: String? = null
    ): Response<List<MessageTemplate>>

    @POST("message/template/create")
    suspend fun createTemplate(@Body request: CreateTemplateRequest): Response<MessageTemplate>

    @DELETE("message/template/{templateId}")
    suspend fun deleteTemplate(@Path("templateId") templateId: Long): Response<ApiResponse<String>>

    // 后端 PackagePurchaseController GET /billing/package/configs
    @GET("billing/package/configs")
    suspend fun getRechargePackages(): Response<ApiResponse<Map<String, Any>>>

    // 后端 BillingOrderController POST /billing/order/recharge
    @POST("billing/order/recharge")
    suspend fun createRechargeOrder(@Query("tenantId") tenantId: Long, @Body request: CreateOrderRequest): Response<ApiResponse<RechargeOrder>>

    // 后端 BillingOrderController GET /billing/order/user/{userId}
    @GET("billing/order/user/{userId}")
    suspend fun getRechargeOrders(
        @Path("userId") userId: Long,
        @Query("orderType") orderType: Int? = null
    ): Response<ApiResponse<List<RechargeOrder>>>

    // 后端 BillingOrderController GET /billing/order/{orderNo}
    @GET("billing/order/{orderNo}")
    suspend fun getOrderDetail(@Path("orderNo") orderNo: String): Response<ApiResponse<RechargeOrder>>

    // 后端 UserController GET /user/balance
    @GET("user/balance")
    suspend fun getBalance(): Response<ApiResponse<UserBalance>>

    // 后端 AiServiceController POST /v1/text/generate
    @POST("v1/text/generate")
    suspend fun generateContent(@Body request: GenerateContentRequest): Response<ApiResponse<GeneratedContent>>

    // 后端 ContentController GET /content (无/templates, 用GET /content代替)
    @GET("content")
    suspend fun getContentTemplates(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<List<ContentTemplate>>>

    // 后端 ContentController POST /content/{id}/publish
    @POST("content/{id}/publish")
    suspend fun publishContent(@Path("id") contentId: Long, @Body request: PublishContentRequest): Response<ApiResponse<Map<String, Any>>>

    // 后端 ContentController GET /content (分页查询内容列表)
    @GET("content")
    suspend fun getContentHistory(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<PageResult<GeneratedContent>>>

    // 后端 DashboardController GET /data/dashboard/operation
    @GET("data/dashboard/operation")
    suspend fun getDashboardData(): Response<ApiResponse<DashboardData>>

    // 后端 TrendController GET /data/trend/{type}?startDate=&endDate=
    @GET("data/trend/LEAD")
    suspend fun getLeadTrend(
        @Query("days") days: Int = 30
    ): Response<ApiResponse<List<TrendData>>>

    // 后端无此端点，由 TaskController 补充
    @GET("tasks/summary")
    suspend fun getTaskSummary(): Response<ApiResponse<TaskSummary>>

    // ============================================================
    // 爬虫管理 API (后端 CrawlController @RequestMapping("/crawl/task"))
    // ============================================================

    @GET("crawl/task/tasks")
    suspend fun getCrawlTasks(): Response<ApiResponse<List<CrawlTask>>>

    @POST("crawl/task/task/create")
    suspend fun createCrawlTask(@Body request: CreateTaskRequest): Response<ApiResponse<CrawlTask>>

    @GET("crawl/task/task/{taskId}")
    suspend fun getCrawlTask(@Path("taskId") taskId: Long): Response<ApiResponse<CrawlTask>>

    @GET("crawl/task/task/{taskId}/comments")
    suspend fun getTaskComments(
        @Path("taskId") taskId: Long,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
        @Query("minScore") minScore: Int? = null,
        @Query("level") level: String? = null,
        @Query("onlyHighIntent") onlyHighIntent: Boolean = false,
        @Query("onlyWithContact") onlyWithContact: Boolean = false
    ): Response<CommentFilterResult>

    @POST("crawl/task/task/{taskId}/analyze")
    suspend fun analyzeComments(@Path("taskId") taskId: Long): Response<ApiResponse<String>>

    @POST("crawl/task/task/{taskId}/generate-leads")
    suspend fun generateLeads(
        @Path("taskId") taskId: Long,
        @Body request: GenerateLeadsRequest
    ): Response<LeadResult>

    @POST("crawl/task/comment/{commentId}/analyze")
    suspend fun generateSingleLead(@Path("commentId") commentId: Long): Response<LeadResult>

    @POST("crawl/task/task/{taskId}/start")
    suspend fun resumeCrawlTask(@Path("taskId") taskId: Long): Response<ApiResponse<String>>

    @POST("crawl/task/task/{taskId}/stop")
    suspend fun stopCrawlTask(@Path("taskId") taskId: Long): Response<ApiResponse<String>>

    @DELETE("crawl/task/task/{taskId}")
    suspend fun deleteCrawlTask(@Path("taskId") taskId: Long): Response<ApiResponse<String>>

    // ============================================================
    // 移动端专用爬虫 API (MobileCrawlController - 轻量级优化)
    // ============================================================

    data class QuickCreateRequest(
        val platformCode: String,
        val targetType: String,
        val targetId: String,
        val maxComments: Int? = 500,
        val includeReply: Boolean = false
    )

    data class MobileTaskResponse(
        val taskId: Long?,
        val status: String?,
        val platformCode: String?,
        val message: String?
    )

    data class MobileProgressResponse(
        val taskId: Long?,
        val status: String?,
        val progress: Double?,
        val totalComments: Int?,
        val fetchedComments: Int?,
        val highIntentCount: Int?,
        val estimatedRemainingMinutes: Long?
    )

    data class QuickGenerateLeadsRequest(
        val minScore: Int? = 70,
        val maxLeads: Int? = 50
    )

    data class MobileLeadResponse(
        val total: Int?,
        val conversionRate: Double?,
        val leads: List<MobileLeadItem>?
    )

    data class MobileLeadItem(
        val id: Long?,
        val customerName: String?,
        val customerPhone: String?,
        val intentionLevel: String?,
        val score: Int?
    )

    /** MOBILE-001: 一键创建抓取任务（简化参数） */
    @POST("crawl/mobile/quick-create")
    suspend fun quickCreateCrawlTask(@Body request: QuickCreateRequest): Response<MobileTaskResponse>

    /** MOBILE-002: 获取任务实时进度（轻量级） */
    @GET("crawl/mobile/task/{taskId}/progress")
    suspend fun getTaskProgress(@Path("taskId") taskId: Long): Response<MobileProgressResponse>

    /** MOBILE-003: 一键生成商机（批量+过滤） */
    @POST("crawl/mobile/task/{taskId}/quick-generate-leads")
    suspend fun quickGenerateLeads(
        @Path("taskId") taskId: Long,
        @Body request: QuickGenerateLeadsRequest = QuickGenerateLeadsRequest()
    ): Response<MobileLeadResponse>

    /** MOBILE-004: 获取我的任务列表（移动端分页优化） */
    @GET("crawl/mobile/my-tasks")
    suspend fun getMyTasks(
        @Query("status") status: String = "RUNNING",
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 10
    ): Response<ApiResponse<Map<String, Any>>>

    // ============================================================
    // 养号策略 API (Nurturing Strategy)
    // ============================================================

    @GET("nurturing/strategies")
    suspend fun getNurturingStrategies(
        @Query("accountId") accountId: Long? = null,
        @Query("enabled") enabled: Int? = null
    ): Response<ApiResponse<List<NurturingStrategy>>>

    @GET("nurturing/strategy/{id}")
    suspend fun getNurturingStrategyDetail(@Path("id") strategyId: Long): Response<ApiResponse<NurturingStrategy>>

    @POST("nurturing/strategy")
    suspend fun createNurturingStrategy(@Body request: NurturingStrategyRequest): Response<ApiResponse<NurturingStrategy>>

    @PUT("nurturing/strategy/{id}")
    suspend fun updateNurturingStrategy(
        @Path("id") strategyId: Long,
        @Body request: NurturingStrategyRequest
    ): Response<ApiResponse<NurturingStrategy>>

    @DELETE("nurturing/strategy/{id}")
    suspend fun deleteNurturingStrategy(@Path("id") strategyId: Long): Response<ApiResponse<Map<String, Any>>>

    @PUT("nurturing/strategy/{id}/status")
    suspend fun toggleNurturingStatus(
        @Path("id") strategyId: Long,
        @Query("status") status: Int
    ): Response<ApiResponse<Boolean>>

    @POST("nurturing/strategy/{id}/start")
    suspend fun startNurturingStrategy(@Path("id") strategyId: Long): Response<ApiResponse<Boolean>>

    @POST("nurturing/strategy/{id}/stop")
    suspend fun stopNurturingStrategy(@Path("id") strategyId: Long): Response<ApiResponse<Boolean>>

    @GET("nurturing/strategy/{id}/progress")
    suspend fun getNurturingProgress(@Path("id") strategyId: Long): Response<ApiResponse<NurturingProgress>>

    @GET("nurturing/account/{accountId}/status")
    suspend fun getAccountNurturingStatus(@Path("accountId") accountId: Long): Response<ApiResponse<Int>>

    @GET("nurturing/templates")
    suspend fun getNurturingTemplates(): Response<ApiResponse<List<NurturingTemplate>>>

    // ============================================================
    // 风控 API (Risk Control)
    // ============================================================

    // 后端 RiskController POST /risk/check (已对齐)
    @POST("risk/check")
    suspend fun performRiskCheck(@Body request: RiskCheckRequest): Response<ApiResponse<RiskCheckResult>>

    // 后端 RiskController GET /risk/records (规则记录)
    @GET("risk/records")
    suspend fun getRiskRules(
        @Query("tenantId") tenantId: Long,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<ApiResponse<List<RiskRule>>>

    // 后端 RiskController GET /risk/score/{accountId} (已对齐)
    @GET("risk/score/{accountId}")
    suspend fun getAccountRiskScore(@Path("accountId") accountId: Long): Response<ApiResponse<RiskScore>>

    // ============================================================
    // 移动端 OAuth API (Mobile OAuth - Deep Link + PKCE)
    // ============================================================

    /**
     * 生成移动端授权URL（含PKCE参数）
     *
     * @param platform 平台代码: DOUYIN/XIAOHONGSHU/KUAISHOU
     * @return AuthorizationUrlResponse (authUrl, state, codeVerifier)
     */
    @POST("mobile/oauth/authorize/{platform}")
    suspend fun generateAuthUrl(
        @Path("platform") platform: String
    ): Response<ApiResponse<AuthorizationUrlResponse>>

    /**
     * 处理OAuth Deep Link回调
     *
     * @param platform 平台代码
     * @param code 授权码
     * @param state 状态参数
     * @param codeVerifier PKCE验证码（可选）
     * @return OAuthSuccessResult (accountId, nickname, avatar, etc.)
     */
    @GET("mobile/oauth/callback/{platform}")
    suspend fun handleOAuthCallback(
        @Path("platform") platform: String,
        @Query("code") code: String,
        @Query("state") state: String,
        @Query("codeVerifier") codeVerifier: String? = null
    ): Response<ApiResponse<OAuthSuccessResult>>

    /**
     * 刷新Token
     */
    @POST("mobile/oauth/refresh/{accountId}")
    suspend fun refreshToken(
        @Path("accountId") accountId: Long
    ): Response<ApiResponse<RefreshTokenResponse>>

    /**
     * 解除绑定社交账号
     */
    @DELETE("mobile/oauth/unbind/{accountId}")
    suspend fun unbindAccount(
        @Path("accountId") accountId: Long
    ): Response<ApiResponse<Void>>

    /**
     * 获取已绑定账号列表
     */
    @GET("mobile/oauth/accounts")
    suspend fun getBoundAccounts(): Response<ApiResponse<List<BoundSocialAccount>>>

    // ============================================================
    // 系统模块
    // ============================================================

    @GET("system/app/version-check")
    suspend fun checkAppVersion(
        @Query("currentVersionCode") currentVersionCode: Int,
        @Query("currentVersionName") currentVersionName: String,
        @Query("platform") platform: String = "android"
    ): Response<ApiResponse<AppVersionInfo>>

    @GET("system/app/download-url")
    suspend fun getDownloadUrl(
        @Query("versionCode") versionCode: Int
    ): Response<ApiResponse<DownloadInfo>>
}
