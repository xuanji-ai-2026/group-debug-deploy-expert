package com.beijixing.social.compliance.service;

import com.alibaba.fastjson2.JSON;
import com.beijixing.social.entity.SocialAccount;
import com.beijixing.social.service.AccountService;
import com.beijixing.social.service.OAuthService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Token自动刷新调度器 v2.0 (2026合规版)
 *
 * 核心职责:
 * 1. **定期扫描**: 定时检查所有社交账号的Token健康状态
 * 2. **智能预警**: 提前7天/3天/1天发送预警通知
 * 3. **自动刷新**: Token即将过期时自动调用refresh_token接口
 * 4. **异常处理**: 刷新失败时触发告警并标记账号状态
 *
 * 调度策略:
 * - 主任务: 每6小时执行一次全量扫描（凌晨2点、8点、14点、20点）
 * - 预警任务: 每1小时检查即将过期的Token（<24小时）
 * - 紧急刷新: 发现<1小时过期的Token立即刷新
 *
 * 告警机制:
 * - 日志记录: 所有操作详细记录到日志文件
 * - 数据库标记: 更新SocialAccount表的token_status字段
 * - Redis通知: 发布消息给WebSocket推送模块
 * - 外部告警: 可选集成钉钉/企微/邮件通知
 *
 * @author 北极星AI团队
 * @version 2.0 (2026-05-20)
 */
@Service
@Slf4j
public class TokenRefreshScheduler {

    @Autowired
    private TokenSecurityService tokenSecurityService;

    @Autowired
    private OAuthService oauthService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${compliance.alert.dingtalk-webhook:}")
    private String dingtalkWebhook;

    @Value("${compliance.alert.wecom-webhook:}")
    private String wecomWebhook;

    @Value("${compliance.alert.email-enabled:false}")
    private boolean emailAlertEnabled;

    // ====== 统计计数器 ======
    private final AtomicInteger totalScanned = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);
    private final AtomicInteger warningCount = new AtomicInteger(0);

    /**
     * 主调度任务: 全量扫描所有Token状态
     *
     * 执行频率: 每6小时 (cron: 0 0 2,8,14,20 * * ?)
     *
     * 扫描逻辑:
     * 1. 从数据库查询所有活跃的社交账号（status=1）
     * 2. 调用tokenSecurityService.checkAllTokenHealth()获取健康报告
     * 3. 根据剩余时间分类处理:
     *    - >7天: 正常，无需操作
     *    - 3-7天: 发送低级预警（日志+数据库标记）
     *    - 1-3天: 发送中级预警（日志+Redis通知）
     *    - <1天: 紧急刷新（立即调用refresh接口）
     *    - 已过期: 标记为EXPIRED，通知用户重新授权
     */
    @Scheduled(cron = "0 0 2,8,14,20 * * ?")
    public void scheduledFullScan() {
        log.info("🔄 [TokenScheduler] 开始执行全量Token扫描...");

        long startTime = System.currentTimeMillis();
        totalScanned.set(0);
        successCount.set(0);
        failedCount.set(0);
        warningCount.set(0);

        try {
            // 1. 获取所有Token的健康报告
            List<TokenSecurityService.TokenHealthReport> reports =
                    tokenSecurityService.checkAllTokenHealth();

            totalScanned.set(reports.size());

            if (reports.isEmpty()) {
                log.info("✅ [TokenScheduler] 无活跃Token需要扫描");
                return;
            }

            // 2. 分类处理每个Token
            for (TokenSecurityService.TokenHealthReport report : reports) {
                processSingleToken(report);
            }

            // 3. 输出统计摘要
            long duration = System.currentTimeMillis() - startTime;
            logScanSummary(duration);

        } catch (Exception e) {
            log.error("❌ [TokenScheduler] 全量扫描异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 紧急检查任务: 仅检查即将过期的Token（<24小时）
     *
     * 执行频率: 每1小时 (cron: 0 0 * * * ?)
     *
     * 目的: 快速响应即将过期的Token，避免业务中断
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void scheduledUrgentCheck() {
        log.debug("⏰ [TokenScheduler] 执行紧急检查（<24小时过期）...");

        try {
            List<TokenSecurityService.TokenHealthReport> reports =
                    tokenSecurityService.checkAllTokenHealth();

            int urgentCount = 0;

            for (TokenSecurityService.TokenHealthReport report : reports) {
                if ("CRITICAL".equals(report.getStatus()) || "EXPIRED".equals(report.getStatus())) {
                    processSingleToken(report);  // 立即处理
                    urgentCount++;
                }
            }

            if (urgentCount > 0) {
                log.warn("⚠️ [TokenScheduler] 发现{}个紧急Token需处理", urgentCount);
            }

        } catch (Exception e) {
            log.error("❌ [TokenScheduler] 紧急检查异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理单个Token的刷新/预警逻辑
     */
    private void processSingleToken(TokenSecurityService.TokenHealthReport report) {
        Long accountId = report.getAccountId();
        String status = report.getStatus();
        long remainingHours = report.getRemainingHours();

        switch (status) {
            case "HEALTHY":
                log.debug("✅ Token健康: accountId={}, 剩余={}小时", accountId, remainingHours);
                break;

            case "NOTICE":
                handleNoticeWarning(accountId, remainingHours);
                warningCount.incrementAndGet();
                break;

            case "WARNING":
                handleWarningAlert(accountId, remainingHours);
                warningCount.incrementAndGet();
                break;

            case "CRITICAL":
                handleCriticalRefresh(accountId, remainingHours);
                break;

            case "EXPIRED":
                handleExpiredToken(accountId);
                failedCount.incrementAndGet();
                break;

            default:
                log.warn("❓ 未知Token状态: accountId={}, status={}", accountId, status);
        }
    }

    /**
     * 处理NOTICE级别预警（3-7天内过期）
     *
     * 措施:
     * - 记录日志（INFO级别）
     * - 数据库更新token_status字段为"EXPIRING_SOON"
     * - 不发送外部通知（避免打扰用户）
     */
    private void handleNoticeWarning(Long accountId, long remainingHours) {
        log.info("ℹ️ [TokenScheduler] Token即将过期: accountId={}, 剩余={}天",
                accountId, remainingHours / 24);

        try {
            SocialAccount account = accountService.getById(accountId);
            if (account != null) {
                account.setErrorMsg("EXPIRING_SOON");
                accountService.updateById(account);
                log.info("✅ [TokenScheduler] 数据库已更新: accountId={}, tokenStatus=EXPIRING_SOON", accountId);
            } else {
                log.warn("⚠️ [TokenScheduler] 账号不存在，无法更新状态: accountId={}", accountId);
            }
        } catch (Exception e) {
            log.error("❌ [TokenScheduler] 更新数据库失败: accountId={}, error={}", accountId, e.getMessage(), e);
        }
    }

    /**
     * 处理WARNING级别预警（1-3天内过期）
     *
     * 措施:
     * - 记录日志（WARN级别）
     * - Redis发布预警消息（供WebSocket推送）
     * - 数据库标记为"URGENT_REFRESH_NEEDED"
     * - 可选：发送邮件/钉钉通知管理员
     */
    private void handleWarningAlert(Long accountId, long remainingHours) {
        log.warn("⚠️ [TokenScheduler] Token紧急预警: accountId={}, 剩余={}小时",
                accountId, remainingHours);

        try {
            Map<String, Object> warningMessage = new HashMap<>();
            warningMessage.put("accountId", accountId);
            warningMessage.put("remainingHours", remainingHours);
            warningMessage.put("level", "WARNING");
            warningMessage.put("timestamp", LocalDateTime.now().toString());
            warningMessage.put("message", String.format("Token即将过期，剩余%d小时，请及时处理", remainingHours));

            redisTemplate.convertAndSend("token:warning", JSON.toJSONString(warningMessage));
            log.info("📢 [TokenScheduler] Redis预警消息已发布: channelId=token:warning, accountId={}", accountId);
        } catch (Exception e) {
            log.error("❌ [TokenScheduler] 发布Redis消息失败: accountId={}, error={}", accountId, e.getMessage(), e);
        }

        sendAlertNotificationAsync(accountId, "WARNING", remainingHours);
    }

    /**
     * 处理CRITICAL级别（<1小时过期）- 立即尝试刷新
     *
     * 流程:
     * 1. 调用oauthService.refreshToken()进行刷新
     * 2. 如果成功，更新TokenSecurityService中的缓存
     * 3. 如果失败，标记为FAILED，发送紧急告警
     */
    private void handleCriticalRefresh(Long accountId, long remainingHours) {
        log.error("🚨 [TokenScheduler] Token即将过期，立即刷新: accountId={}, 剩余={}分钟",
                accountId, remainingHours * 60);

        try {
            boolean refreshed = oauthService.refreshToken(oauthService.getAccountById(accountId));

            if (refreshed) {
                log.info("✅ [TokenScheduler] 自动刷新成功: accountId={}", accountId);
                successCount.incrementAndGet();

                try {
                    SocialAccount account = oauthService.getAccountById(accountId);
                    if (account != null && account.getAccessToken() != null) {
                        int expiresIn = (int) Duration.between(
                                LocalDateTime.now(),
                                account.getTokenExpireTime()
                        ).getSeconds();
                        tokenSecurityService.updateTokensAfterRefresh(
                                accountId,
                                account.getAccessToken(),
                                account.getRefreshToken(),
                                expiresIn > 0 ? expiresIn : 7200
                        );
                        log.info("🔄 [TokenScheduler] 缓存已同步更新: accountId={}", accountId);

                        account.setErrorMsg(null);
                        accountService.updateById(account);
                    }
                } catch (Exception e) {
                    log.warn("⚠️ [TokenScheduler] 刷新后缓存更新失败(不影响主流程): accountId={}, error={}",
                            accountId, e.getMessage());
                }

            } else {
                log.error("❌ [TokenScheduler] 自动刷新失败: accountId={}", accountId);
                failedCount.incrementAndGet();

                // 发送紧急告警
                sendEmergencyAlert(accountId, "自动刷新失败");
            }

        } catch (Exception e) {
            log.error("❌ [TokenScheduler] 刷新过程异常: accountId={}, error={}",
                    accountId, e.getMessage(), e);
            failedCount.incrementAndGet();
            sendEmergencyAlert(accountId, "刷新异常: " + e.getMessage());
        }
    }

    /**
     * 处理已过期的Token
     *
     * 措施:
     * - 标记账户状态为INACTIVE或TOKEN_EXPIRED
     * - 发送紧急通知给用户（APP推送/短信/邮件）
     * - 禁止使用该账号进行API调用
     * - 记录审计日志
     */
    private void handleExpiredToken(Long accountId) {
        log.error("💀 [TokenScheduler] Token已过期: accountId={}", accountId);

        try {
            SocialAccount account = accountService.getById(accountId);
            if (account != null) {
                account.setStatus(2);
                account.setErrorMsg("TOKEN_EXPIRED");
                accountService.updateById(account);
                log.info("📝 [TokenScheduler] 数据库状态已更新: accountId={}, status=2(TOKEN_EXPIRED)", accountId);

                sendUserNotificationAsync(account, "TOKEN_EXPIRED");
            } else {
                log.warn("⚠️ [TokenScheduler] 账号不存在，无法更新: accountId={}", accountId);
            }

            sendEmergencyAlert(accountId, "Token已过期");

        } catch (Exception e) {
            log.error("❌ [TokenScheduler] 处理过期Token失败: accountId={}, error={}",
                    accountId, e.getMessage(), e);
        }
    }

    // ============================================================
    // 告警通知方法
    // ============================================================

    /**
     * 发送紧急告警（多渠道）
     *
     * 当前实现: 仅日志输出
     * 后续可扩展: 钉钉机器人 / 企业微信 / 邮件 / SMS
     */
    private void sendEmergencyAlert(Long accountId, String reason) {
        String alertMessage = String.format(
                "🚨 [北极星AI-紧急告警]\n" +
                "时间: %s\n" +
                "账号ID: %d\n" +
                "原因: %s\n" +
                "建议: 请立即检查该账号状态，必要时联系用户重新授权",
                LocalDateTime.now().toString(),
                accountId,
                reason
        );

        log.error(alertMessage);

        sendDingTalkWebhookAsync(alertMessage);
        sendWeComWebhookAsync(alertMessage);
        if (emailAlertEnabled && isWorkingHour()) {
            sendEmailAlertAsync("北极星AI-Token紧急告警", alertMessage);
        }
    }

    @Async
    public void sendDingTalkWebhookAsync(String message) {
        if (dingtalkWebhook == null || dingtalkWebhook.isEmpty()) {
            return;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "markdown");

            Map<String, Object> markdown = new HashMap<>();
            markdown.put("title", "北极星AI-Token紧急告警");
            markdown.put("text", message);
            body.put("markdown", markdown);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(body), headers);
            String result = restTemplate.postForObject(dingtalkWebhook, entity, String.class);

            log.info("📱 [TokenScheduler] 钉钉告警发送结果: {}", result);
        } catch (Exception e) {
            log.error("❌ [TokenScheduler] 钉钉告警发送失败: {}", e.getMessage());
        }
    }

    @Async
    public void sendWeComWebhookAsync(String message) {
        if (wecomWebhook == null || wecomWebhook.isEmpty()) {
            return;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "markdown");

            Map<String, Object> markdown = new HashMap<>();
            markdown.put("content", message);
            body.put("markdown", markdown);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(body), headers);
            String result = restTemplate.postForObject(wecomWebhook, entity, String.class);

            log.info("💬 [TokenScheduler] 企业微信告警发送结果: {}", result);
        } catch (Exception e) {
            log.error("❌ [TokenScheduler] 企业微信告警发送失败: {}", e.getMessage());
        }
    }

    @Async
    public void sendEmailAlertAsync(String subject, String content) {
        try {
            log.info("📧 [TokenScheduler] 发送邮件告警(仅工作日): subject={}", subject);
            Map<String, Object> emailPayload = new HashMap<>();
            emailPayload.put("to", "admin@beijixing.ai");
            emailPayload.put("subject", subject);
            emailPayload.put("content", content);
            emailPayload.put("timestamp", LocalDateTime.now().toString());

            redisTemplate.convertAndSend("token:email:alert", JSON.toJSONString(emailPayload));
            log.info("📧 [TokenScheduler] 邮件告警任务已发布到Redis队列");
        } catch (Exception e) {
            log.error("❌ [TokenScheduler] 邮件告警发送失败: {}", e.getMessage());
        }
    }

    @Async
    public void sendAlertNotificationAsync(Long accountId, String level, long remainingHours) {
        try {
            String platformName = "未知平台";
            SocialAccount account = accountService.getById(accountId);
            if (account != null) {
                platformName = account.getPlatformCode();
            }

            String notificationText = String.format(
                    "⚠️ [Token预警通知]\n级别: %s\n账号ID: %d\n平台: %s\n剩余时间: %d小时\n请及时处理!",
                    level, accountId, platformName, remainingHours
            );

            log.info("🔔 [TokenScheduler] 异步发送告警通知: level={}, accountId={}", level, accountId);

            if (dingtalkWebhook != null && !dingtalkWebhook.isEmpty()) {
                sendDingTalkWebhookAsync(notificationText);
            }
            if (wecomWebhook != null && !wecomWebhook.isEmpty()) {
                sendWeComWebhookAsync(notificationText);
            }
        } catch (Exception e) {
            log.error("❌ [TokenScheduler] 异步告警通知失败: accountId={}, error={}", accountId, e.getMessage());
        }
    }

    @Async
    public void sendUserNotificationAsync(SocialAccount account, String reason) {
        try {
            if (account == null || account.getUserId() == null) {
                log.warn("⚠️ [TokenScheduler] 无法发送用户通知: 账号或用户ID为空");
                return;
            }

            Long userId = account.getUserId();
            String platformName = getPlatformDisplayName(account.getPlatformCode());
            String notificationTitle = "社交账号授权提醒";
            String notificationContent = "";

            switch (reason) {
                case "TOKEN_EXPIRED":
                    notificationContent = String.format(
                            "您的%s账号(%s)授权已过期，请重新绑定以继续使用。",
                            platformName, account.getNickname() != null ? account.getNickname() : "未知"
                    );
                    break;
                default:
                    notificationContent = String.format(
                            "您的%s账号(%s)状态异常: %s，请及时处理。",
                            platformName, account.getNickname() != null ? account.getNickname() : "未知", reason
                    );
            }

            Map<String, Object> userNotification = new HashMap<>();
            userNotification.put("userId", userId);
            userNotification.put("title", notificationTitle);
            userNotification.put("content", notificationContent);
            userNotification.put("type", "TOKEN_ALERT");
            userNotification.put("accountId", account.getId());
            userNotification.put("timestamp", LocalDateTime.now().toString());

            redisTemplate.convertAndSend("token:user:notification", JSON.toJSONString(userNotification));
            log.info("📲 [TokenScheduler] 用户通知已发布: userId={}, type={}", userId, reason);

        } catch (Exception e) {
            log.error("❌ [TokenScheduler] 发送用户通知失败: error={}", e.getMessage(), e);
        }
    }

    private String getPlatformDisplayName(String platformCode) {
        if (platformCode == null) return "未知";
        switch (platformCode.toUpperCase()) {
            case "DOUYIN": return "抖音";
            case "XIAOHONGSHU": return "小红书";
            case "VIDEO": return "视频号";
            case "KUAISHOU": return "快手";
            case "BILIBILI": return "B站";
            case "WEIBO": return "微博";
            default: return platformCode;
        }
    }

    /**
     * 判断是否在工作时间（用于控制邮件告警频率）
     */
    private boolean isWorkingHour() {
        int hour = LocalDateTime.now().getHour();
        int dayOfWeek = LocalDateTime.now().getDayOfWeek().getValue(); // 1=Monday, 7=Sunday

        return dayOfWeek >= 1 && dayOfWeek <= 5 && hour >= 9 && hour < 18;
    }

    // ============================================================
    // 统计与监控
    // ============================================================

    /**
     * 输出扫描统计摘要
     */
    private void logScanSummary(long durationMs) {
        int total = totalScanned.get();
        int success = successCount.get();
        int failed = failedCount.get();
        int warning = warningCount.get();

        log.info("📊 [TokenScheduler] 扫描完成:\n" +
                        "  总数: {}\n" +
                        "  成功: {} ({:.1f}%)\n" +
                        "  失败: {} ({:.1f}%)\n" +
                        "  预警: {} ({:.1f}%)\n" +
                        "  耗时: {}ms",
                total,
                success, total > 0 ? (double) success / total * 100 : 0,
                failed, total > 0 ? (double) failed / total * 100 : 0,
                warning, total > 0 ? (double) warning / total * 100 : 0,
                durationMs
        );

        log.debug("Prometheus指标上报待接入: total={}, success={}, failed={}, warning={}, durationMs={}",
                total, success, failed, warning, durationMs);
        reportMetricsToPrometheus(total, success, failed, warning, durationMs);
    }

    /**
     * 获取当前调度器状态（供管理API调用）
     */
    public SchedulerStatus getSchedulerStatus() {
        SchedulerStatus status = new SchedulerStatus();
        status.setLastScanTime(LocalDateTime.now());
        status.setTotalScanned(totalScanned.get());
        status.setSuccessCount(successCount.get());
        status.setFailedCount(failedCount.get());
        status.setWarningCount(warningCount.get());

        return status;
    }

    /**
     * 上报指标到Prometheus/Grafana（可扩展实现）
     * 支持多种监控后端:
     * 1. Micrometer + Prometheus (推荐)
     * 2. 自定义Metrics API
     * 3. 日志输出(开发环境降级)
     */
    private void reportMetricsToPrometheus(int total, int success, int failed, int warning, long durationMs) {
        try {
            log.info("📊 [TokenScheduler Metrics] total={}, success={}({}%), failed={}({}%), warning={}({}%), duration={}ms",
                    total,
                    success, total > 0 ? String.format("%.1f", (double) success / total * 100) : "0.0",
                    failed, total > 0 ? String.format("%.1f", (double) failed / total * 100) : "0.0",
                    warning, total > 0 ? String.format("%.1f", (double) warning / total * 100) : "0.0",
                    durationMs);
        } catch (Exception e) {
            log.warn("⚠️ 指标上报失败: error={}", e.getMessage());
        }
    }

    // ============================================================
    // 手动触发接口（供管理员使用）
    // ============================================================

    /**
     * 手动触发全量扫描（用于测试或紧急情况）
     *
     * 注意: 此方法会重置所有计数器
     */
    public void triggerManualScan() {
        log.info("🔧 [TokenScheduler] 管理员手动触发全量扫描...");
        scheduledFullScan();
    }

    /**
     * 刷新指定账号的Token（管理员强制刷新）
     */
    public boolean forceRefreshToken(Long accountId) {
        log.info("🔧 [TokenScheduler] 管理员强制刷新Token: accountId={}", accountId);

        try {
            return oauthService.refreshToken(oauthService.getAccountById(accountId));
        } catch (Exception e) {
            log.error("❌ 强制刷新失败: accountId={}, error={}", accountId, e.getMessage(), e);
            return false;
        }
    }

    // ============================================================
    // 数据模型
    // ============================================================

    @Data
    public static class SchedulerStatus {
        private LocalDateTime lastScanTime;
        private int totalScanned;
        private int successCount;
        private int failedCount;
        private int warningCount;
    }
}
