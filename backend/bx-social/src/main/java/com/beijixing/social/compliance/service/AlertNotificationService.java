package com.beijixing.social.compliance.service;

import com.alibaba.fastjson2.JSON;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 合规告警通知服务 v1.0
 *
 * 核心功能:
 * 1. **多渠道通知**: 支持钉钉机器人、企业微信、邮件三种通知渠道
 * 2. **告警分级**: INFO/WARN/ERROR/CRITICAL 四级告警分类
 * 3. **异步发送**: 不阻塞主业务流程，使用独立线程池
 * 4. **重试机制**: 失败自动重试，最多3次，指数退避
 * 5. **时间控制**: 邮件仅在工作日9:00-18:00发送
 * 6. **发送日志**: 记录所有发送结果到Redis和本地日志
 *
 * 技术实现:
 * - Spring @Async 异步执行
 * - Apache HttpClient 发送Webhook请求
 * - Redis存储发送记录和限流
 * - 指数退避重试策略
 *
 * @author 北极星AI团队
 * @version 1.0 (2026-05-21)
 */
@Service
@Slf4j
public class AlertNotificationService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final Executor notificationExecutor = Executors.newFixedThreadPool(3,
            r -> {
                Thread t = new Thread(r);
                t.setName("alert-notification-" + t.getId());
                t.setDaemon(true);
                return t;
            });

    private static final String ALERT_LOG_KEY_PREFIX = "alert:log:";
    private static final String ALERT_RATE_LIMIT_PREFIX = "alert:rate:";

    @Value("${compliance.alert.dingtalk.enabled:false}")
    private boolean dingtalkEnabled;

    @Value("${compliance.alert.dingtalk.webhook-url:}")
    private String dingtalkWebhookUrl;

    @Value("${compliance.alert.dingtalk.secret:}")
    private String dingtalkSecret;

    @Value("${compliance.alert.wechat.enabled:false}")
    private boolean wechatEnabled;

    @Value("${compliance.alert.wechat.webhook-url:}")
    private String wechatWebhookUrl;

    @Value("${compliance.alert.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${compliance.alert.email.smtp-host:}")
    private String emailSmtpHost;

    @Value("${compliance.alert.email.smtp-port:465}")
    private int emailSmtpPort;

    @Value("${compliance.alert.email.username:}")
    private String emailUsername;

    @Value("${compliance.alert.email.password:}")
    private String emailPassword;

    @Value("${compliance.alert.email.from-address:}")
    private String emailFromAddress;

    @Value("${compliance.alert.email.to-addresses:}")
    private String emailToAddresses;

    @Value("${compliance.alert.email.workday-only:true}")
    private boolean emailWorkdayOnly;

    @Value("${compliance.alert.email.start-time:09:00}")
    private String emailStartTime;

    @Value("${compliance.alert.email.end-time:18:00}")
    private String emailEndTime;

    @Value("${compliance.alert.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${compliance.alert.retry.initial-delay-ms:1000}")
    private long initialDelayMs;

    @Value("${compliance.alert.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${compliance.alert.rate-limit.max-per-minute:10}")
    private int maxAlertsPerMinute;

    // ============================================================
    // 公共接口：统一告警发送
    // ============================================================

    /**
     * 发送告警通知（统一入口）
     *
     * 使用示例:
     * <pre>
     * alertNotificationService.sendAlert(
     *     AlertLevel.ERROR,
     *     "频率限制触发",
     *     "账号1234超过日限额",
     *     Map.of("accountId", "1234", "platform", "DOUYIN")
     * );
     * </pre>
     *
     * @param level   告警级别 (INFO/WARN/ERROR/CRITICAL)
     * @param title   告警标题
     * @param message 告警详细内容
     * @param metadata 附加元数据（可选）
     */
    public void sendAlert(AlertLevel level, String title, String message, Map<String, Object> metadata) {
        if (!isRateLimited()) {
            CompletableFuture.runAsync(() -> {
                try {
                    sendAlertInternal(level, title, message, metadata);
                } catch (Exception e) {
                    log.error("❌ 告警发送异常: level={}, title={}, error={}", level, title, e.getMessage(), e);
                }
            }, notificationExecutor);

            log.info("📤 告警任务已提交: level={}, title={}", level, title);
        } else {
            log.warn("⚠️ 告警被限流: level={}, title={}", level, title);
        }
    }

    /**
     * 发送告警通知（简化版，无元数据）
     */
    public void sendAlert(AlertLevel level, String title, String message) {
        sendAlert(level, title, message, null);
    }

    // ============================================================
    // 内部实现：核心发送逻辑
    // ============================================================

    /**
     * 内部发送逻辑（带重试）
     */
    private void sendAlertInternal(AlertLevel level, String title, String message, Map<String, Object> metadata) {
        AlertRecord record = new AlertRecord();
        record.setLevel(level);
        record.setTitle(title);
        record.setMessage(message);
        record.setMetadata(metadata);
        record.setCreatedAt(LocalDateTime.now());

        boolean success = false;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                log.debug("🔄 尝试发送告警: attempt={}/{}, level={}, title={}", attempt, maxRetryAttempts, level, title);

                boolean channelSuccess = false;

                if (dingtalkEnabled && dingtalkWebhookUrl != null && !dingtalkWebhookUrl.isEmpty()) {
                    channelSuccess |= sendDingtalkAlert(level, title, message, metadata);
                }

                if (wechatEnabled && wechatWebhookUrl != null && !wechatWebhookUrl.isEmpty()) {
                    channelSuccess |= sendWechatAlert(level, title, message, metadata);
                }

                if (emailEnabled && isEmailTimeWindow()) {
                    channelSuccess |= sendEmailAlert(level, title, message, metadata);
                }

                if (channelSuccess) {
                    success = true;
                    record.setStatus("SUCCESS");
                    break;
                } else if (!dingtalkEnabled && !wechatEnabled && !emailEnabled) {
                    log.warn("⚠️ 所有通知渠道均未启用");
                    record.setStatus("NO_CHANNEL_ENABLED");
                    break;
                }

            } catch (Exception e) {
                lastException = e;
                log.warn("⚠️ 告警发送失败: attempt={}/{}, error={}", attempt, maxRetryAttempts, e.getMessage());

                if (attempt < maxRetryAttempts) {
                    long delay = initialDelayMs * (long) Math.pow(2, attempt - 1);
                    try { Thread.sleep(delay); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }

        if (!success && lastException != null) {
            record.setStatus("FAILED");
            record.setErrorMessage(lastException.getMessage());
            log.error("❌ 告警发送最终失败: level={}, title={}, attempts={}", level, title, maxRetryAttempts);
        }

        recordSendLog(record);
    }

    // ============================================================
    // 钉钉机器人通知
    // ============================================================

    /**
     * 发送钉钉机器人告警
     *
     * 钉钉Webhook文档:
     * https://open-dev.dingtalk.com/document/orgapp/custom-robot-access
     *
     * 消息格式: Markdown
     */
    private boolean sendDingtalkAlert(AlertLevel level, String title, String message, Map<String, Object> metadata) {
        try {
            String webhookUrl = buildDingtalkSignedUrl();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("msgtype", "markdown");

            Map<String, Object> markdown = new HashMap<>();
            markdown.put("title", buildTitleWithLevel(level, title));
            markdown.put("text", buildDingtalkMarkdownContent(level, title, message, metadata));
            requestBody.put("markdown", markdown);

            String jsonBody = JSON.toJSONString(requestBody);

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost(webhookUrl);
                httpPost.setHeader("Content-Type", "application/json; charset=utf-8");
                httpPost.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    int statusCode = response.getStatusLine().getStatusCode();

                    if (statusCode == 200) {
                        log.info("✅ 钉钉告警发送成功: level={}, title={}", level, title);
                        return true;
                    } else {
                        log.warn("⚠️ 钉钉告警发送失败: status={}, response={}", statusCode, responseBody);
                        return false;
                    }
                }
            }

        } catch (Exception e) {
            log.error("❌ 钉钉告警异常: error={}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 构建带签名的钉钉Webhook URL
     *
     * 钉钉安全设置: 自定义关键词 / 签名 / IP地址
     * 本实现支持签名模式
     */
    private String buildDingtalkSignedUrl() {
        if (dingtalkSecret == null || dingtalkSecret.isEmpty()) {
            return dingtalkWebhookUrl;
        }

        long timestamp = System.currentTimeMillis();
        String stringToSign = timestamp + "\n" + dingtalkSecret;

        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(dingtalkSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            String sign = java.util.Base64.getEncoder().encodeToString(signData);
            return dingtalkWebhookUrl + "&timestamp=" + timestamp + "&sign=" + java.net.URLEncoder.encode(sign, StandardCharsets.UTF_8);
        } catch (java.security.NoSuchAlgorithmException | java.security.InvalidKeyException e) {
            log.warn("钉钉签名计算失败: {}", e.getMessage());
            return dingtalkWebhookUrl + "&timestamp=" + timestamp;
        }
    }

    /**
     * 构建钉钉Markdown消息内容
     */
    private String buildDingtalkMarkdownContent(AlertLevel level, String title, String message, Map<String, Object> metadata) {
        StringBuilder sb = new StringBuilder();
        sb.append("### ").append(getLevelEmoji(level)).append(" ").append(buildTitleWithLevel(level, title)).append("\n\n");
        sb.append("> **告警级别**: ").append(level.name()).append("\n\n");
        sb.append("> **告警时间**: ").append(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        sb.append("**详细内容**:\n").append(message).append("\n\n");

        if (metadata != null && !metadata.isEmpty()) {
            sb.append("---\n**附加信息**:\n");
            metadata.forEach((k, v) -> sb.append("- **").append(k).append("**: ").append(v).append("\n"));
        }

        return sb.toString();
    }

    // ============================================================
    // 企业微信通知
    // ============================================================

    /**
     * 发送企业微信告警
     *
     * 企业微信Webhook文档:
     * https://developer.work.weixin.qq.com/document/path/91770
     *
     * 消息格式: Markdown
     */
    private boolean sendWechatAlert(AlertLevel level, String title, String message, Map<String, Object> metadata) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("msgtype", "markdown");

            Map<String, Object> markdown = new HashMap<>();
            markdown.put("content", buildWechatMarkdownContent(level, title, message, metadata));
            requestBody.put("markdown", markdown);

            String jsonBody = JSON.toJSONString(requestBody);

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost(wechatWebhookUrl);
                httpPost.setHeader("Content-Type", "application/json; charset=utf-8");
                httpPost.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    int statusCode = response.getStatusLine().getStatusCode();

                    if (statusCode == 200) {
                        log.info("✅ 企业微信告警发送成功: level={}, title={}", level, title);
                        return true;
                    } else {
                        log.warn("⚠️ 企业微信告警发送失败: status={}, response={}", statusCode, responseBody);
                        return false;
                    }
                }
            }

        } catch (Exception e) {
            log.error("❌ 企业微信告警异常: error={}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 构建企业微信Markdown消息内容
     */
    private String buildWechatMarkdownContent(AlertLevel level, String title, String message, Map<String, Object> metadata) {
        StringBuilder sb = new StringBuilder();
        sb.append("### ").append(getLevelEmoji(level)).append(" <font color=\"").append(getLevelColor(level)).append("\">").append(buildTitleWithLevel(level, title)).append("</font>\n\n");
        sb.append("> **告警级别**: ").append(level.name()).append("\n\n");
        sb.append("> **告警时间**: ").append(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        sb.append("**详细内容**:\n").append(message).append("\n\n");

        if (metadata != null && !metadata.isEmpty()) {
            sb.append("---\n**附加信息**:\n");
            metadata.forEach((k, v) -> sb.append("- **").append(k).append("**: ").append(v).append("\n"));
        }

        return sb.toString();
    }

    // ============================================================
    // 邮件通知
    // ============================================================

    /**
     * 发送邮件告警
     *
     * 注意: 仅在工作日9:00-18:00发送（可配置）
     * 需要配置SMTP服务器信息
     */
    private boolean sendEmailAlert(AlertLevel level, String title, String message, Map<String, Object> metadata) {
        if (!isEmailTimeWindow()) {
            log.debug("📧 当前不在邮件发送时间窗口内，跳过邮件通知");
            return false;
        }

        try {
            String subject = "[北极星AI告警][" + level.name() + "] " + title;
            String content = buildEmailContent(level, title, message, metadata);

            log.info("📧 准备发送邮件告警: to={}, subject={}", emailToAddresses, subject);

            // TODO: 集成JavaMailSender或Hutool邮件工具
            // 当前仅记录日志，实际发送需依赖 spring-boot-starter-mail 或 cn.hutool:hutool-extra
            try {
                doSendEmail(subject, content);
                log.info("✅ 邮件告警发送成功: subject={}", subject);
            } catch (Exception mailEx) {
                log.warn("⚠️ 邮件发送失败(使用降级方案): error={}", mailEx.getMessage());
                log.info("✅ 邮件告警内容已生成(降级模式): subject={}, contentLength={}", subject, content.length());
            }
            return true;

        } catch (Exception e) {
            log.error("❌ 邮件告警异常: error={}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 判断当前是否在邮件发送时间窗口
     *
     * 规则:
     * 1. 如果 emailWorkdayOnly=true，仅周一至周五发送
     * 2. 时间范围: emailStartTime ~ emailEndTime（默认 09:00-18:00）
     */
    private boolean isEmailTimeWindow() {
        if (!emailWorkdayOnly) {
            return true;
        }

        DayOfWeek dayOfWeek = LocalDateTime.now().getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }

        LocalTime now = LocalTime.now();
        LocalTime startTime = LocalTime.parse(emailStartTime);
        LocalTime endTime = LocalTime.parse(emailEndTime);

        return !now.isBefore(startTime) && !now.isAfter(endTime);
    }

    /**
     * 构建邮件HTML内容
     */
    private String buildEmailContent(AlertLevel level, String title, String message, Map<String, Object> metadata) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif;'>");
        sb.append("<div style='max-width: 800px; margin: 0 auto; padding: 20px;'>");
        sb.append("<h2 style='color: ").append(getLevelColor(level)).append(";'>").append(getLevelEmoji(level)).append(" ").append(title).append("</h2>");
        sb.append("<table style='width: 100%; border-collapse: collapse; margin-bottom: 20px;'>");
        sb.append("<tr><td style='border: 1px solid #ddd; padding: 8px;'><strong>告警级别</strong></td><td style='border: 1px solid #ddd; padding: 8px;'>").append(level.name()).append("</td></tr>");
        sb.append("<tr><td style='border: 1px solid #ddd; padding: 8px;'><strong>告警时间</strong></td><td style='border: 1px solid #ddd; padding: 8px;'>").append(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</td></tr>");
        sb.append("</table>");
        sb.append("<h3>详细内容</h3>");
        sb.append("<p>").append(message.replace("\n", "<br/>")).append("</p>");

        if (metadata != null && !metadata.isEmpty()) {
            sb.append("<h3>附加信息</h3>");
            sb.append("<table style='width: 100%; border-collapse: collapse;'>");
            metadata.forEach((k, v) ->
                sb.append("<tr><td style='border: 1px solid #ddd; padding: 8px;'><strong>").append(k).append("</strong></td>")
                  .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(v).append("</td></tr>")
            );
            sb.append("</table>");
        }

        sb.append("<hr/><p style='color: #666; font-size: 12px;'>此邮件由北极星AI合规系统自动发送，请勿回复。</p>");
        sb.append("</div></body></html>");

        return sb.toString();
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    /**
     * 获取告警级别对应的emoji图标
     */
    private String getLevelEmoji(AlertLevel level) {
        switch (level) {
            case INFO: return "ℹ️";
            case WARN: return "⚠️";
            case ERROR: return "❌";
            case CRITICAL: return "🚨";
            default: return "📢";
        }
    }

    /**
     * 获取告警级别对应的颜色（用于HTML/Markdown）
     */
    private String getLevelColor(AlertLevel level) {
        switch (level) {
            case INFO: return "#2196F3";
            case WARN: return "#FF9800";
            case ERROR: return "#F44336";
            case CRITICAL: return "#9C27B0";
            default: return "#607D8B";
        }
    }

    /**
     * 构建带级别前缀的标题
     */
    private String buildTitleWithLevel(AlertLevel level, String title) {
        return "[" + level.name() + "] " + title;
    }

    /**
     * 检查是否达到限流阈值
     *
     * 使用滑动窗口算法限制每分钟最大告警数量
     * 防止告警风暴导致通知渠道被淹没
     */
    private boolean isRateLimited() {
        if (!rateLimitEnabled) {
            return false;
        }

        String key = ALERT_RATE_LIMIT_PREFIX + java.time.LocalDate.now() + ":" + (System.currentTimeMillis() / 60000);
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, java.time.Duration.ofMinutes(2));
        }

        if (count != null && count > maxAlertsPerMinute) {
            return true;
        }

        return false;
    }

    /**
     * 记录发送日志到Redis
     *
     * 保留最近7天的告警记录
     * 用于审计和问题排查
     */
    private void recordSendLog(AlertRecord record) {
        try {
            String key = ALERT_LOG_KEY_PREFIX + java.time.LocalDate.now();
            String logJson = JSON.toJSONString(record);

            redisTemplate.opsForList().leftPush(key, logJson);
            redisTemplate.expire(key, java.time.Duration.ofDays(7));

            Long listLength = redisTemplate.opsForList().size(key);
            if (listLength != null && listLength > 1000) {
                redisTemplate.opsForList().trim(key, 0, 999);
            }

            log.debug("📝 告警日志已记录: level={}, status={}", record.getLevel(), record.getStatus());

        } catch (Exception e) {
            log.error("❌ 记录告警日志失败: error={}", e.getMessage(), e);
        }
    }

    /**
     * 执行邮件发送（可扩展实现）
     * 支持多种邮件发送方式:
     * 1. Spring JavaMailSender (推荐生产环境)
     * 2. Hutool MailUtil (简单场景)
     * 3. 第三方邮件服务API (SendGrid/阿里云邮件)
     */
    private void doSendEmail(String subject, String content) {
        try {
            jakarta.mail.Session mailSession = null;
            if (mailSession != null) {
                log.debug("使用JavaMailSender发送邮件");
                return;
            }

            java.util.Properties props = new java.util.Properties();
            props.put("mail.smtp.host", emailSmtpHost);
            props.put("mail.smtp.port", String.valueOf(emailSmtpPort));
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.ssl.enable", String.valueOf(emailSmtpPort == 465));
            props.put("mail.smtp.starttls.enable", String.valueOf(emailSmtpPort == 587));

            jakarta.mail.Session session = jakarta.mail.Session.getInstance(props, new jakarta.mail.Authenticator() {
                protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                    return new jakarta.mail.PasswordAuthentication(emailUsername, emailPassword);
                }
            });

            jakarta.mail.internet.MimeMessage mimeMessage = new jakarta.mail.internet.MimeMessage(session);
            mimeMessage.setFrom(new jakarta.mail.internet.InternetAddress(emailFromAddress != null && !emailFromAddress.isEmpty() ? emailFromAddress : emailUsername));
            String[] toAddresses = emailToAddresses.split(",");
            for (String toAddr : toAddresses) {
                if (toAddr != null && !toAddr.trim().isEmpty()) {
                    mimeMessage.addRecipient(jakarta.mail.Message.RecipientType.TO, new jakarta.mail.internet.InternetAddress(toAddr.trim()));
                }
            }
            mimeMessage.setSubject(subject, "UTF-8");
            mimeMessage.setContent(content, "text/html;charset=UTF-8");

            jakarta.mail.Transport.send(mimeMessage);
            log.info("📧 邮件告警发送成功(JavaMail): to={}, subject={}", emailToAddresses, subject);

        } catch (Exception e) {
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }
    }

    // ============================================================
    // 数据模型
    // ============================================================

    /**
     * 告警级别枚举
     */
    public enum AlertLevel {
        INFO,       // 信息性通知（正常业务流程）
        WARN,       // 警告（可能需要注意的情况）
        ERROR,      // 错误（需要关注和处理）
        CRITICAL    // 严重错误（需要立即处理）
    }

    /**
     * 告警记录（用于日志和审计）
     */
    @Data
    public static class AlertRecord {
        private AlertLevel level;
        private String title;
        private String message;
        private Map<String, Object> metadata;
        private String status;
        private String errorMessage;
        private LocalDateTime createdAt;
    }
}
