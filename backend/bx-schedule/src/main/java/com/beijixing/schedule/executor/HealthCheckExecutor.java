package com.beijixing.schedule.executor;

import com.beijixing.social.compliance.service.AlertNotificationService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.net.InetAddress;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 健康检查任务执行器
 * 负责检查系统各组件的健康状态
 */
@Slf4j
@Component
public class HealthCheckExecutor extends BaseExecutor {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private AlertNotificationService alertNotificationService;

    @Value("${xxl.job.admin.addresses:http://localhost:8080/xxl-job-admin}")
    private String xxlJobAdminAddress;

    @Value("${ai.service.endpoints:}")
    private String aiServiceEndpoints;

    @XxlJob("healthCheckJob")
    public void xxlExecute() {
        String params = XxlJobHelper.getJobParam();
        execute(params);
    }

    @Override
    protected String doExecute(String params) {
        log.info("开始执行健康检查任务");
        XxlJobHelper.log("开始执行健康检查任务");

        Map<String, Object> healthReport = new LinkedHashMap<>();
        healthReport.put("timestamp", LocalDateTime.now().toString());
        healthReport.put("host", getHostAddress());

        int totalChecks = 0;
        int passed = 0;

        totalChecks++;
        boolean mysqlOk = checkMysql();
        healthReport.put("mysql", mysqlOk ? "UP" : "DOWN");
        if (mysqlOk) passed++;

        totalChecks++;
        boolean redisOk = checkRedis();
        healthReport.put("redis", redisOk ? "UP" : "DOWN");
        if (redisOk) passed++;

        totalChecks++;
        boolean xxlJobOk = checkXxlJobAdmin();
        healthReport.put("xxl-job-admin", xxlJobOk ? "UP" : "DOWN");
        if (xxlJobOk) passed++;

        totalChecks++;
        boolean aiServiceOk = checkAiServices();
        healthReport.put("ai-services", aiServiceOk ? "UP" : "DOWN");
        if (aiServiceOk) passed++;

        totalChecks++;
        boolean diskOk = checkDiskSpace();
        healthReport.put("disk", diskOk ? "UP" : "DOWN");
        if (diskOk) passed++;

        totalChecks++;
        boolean memoryOk = checkMemory();
        healthReport.put("memory", memoryOk ? "UP" : "DOWN");
        if (memoryOk) passed++;

        healthReport.put("status", passed == totalChecks ? "ALL_HEALTHY" : "DEGRADED");
        healthReport.put("summary", String.format("%d/%d 服务正常", passed, totalChecks));

        if (passed < totalChecks) {
            sendHealthAlert(healthReport);
        }

        return toJson(healthReport);
    }

    @Override
    protected String getJobName() {
        return "健康检查任务";
    }

    @Override
    protected String getJobType() {
        return "health_check";
    }

    @Override
    protected int getTimeoutSeconds() {
        return 60;
    }

    /**
     * 检查MySQL连接
     * 使用DataSource获取连接并验证数据库可用性
     */
    private boolean checkMysql() {
        if (dataSource == null) {
            log.warn("DataSource未注入，跳过MySQL健康检查");
            return true;
        }
        try (Connection conn = dataSource.getConnection()) {
            if (conn == null || conn.isClosed()) {
                log.warn("MySQL连接获取失败或已关闭");
                return false;
            }
            boolean valid = conn.isValid(5);
            if (!valid) {
                log.warn("MySQL连接有效性校验失败(超时5秒)");
            } else {
                try (var stmt = conn.createStatement();
                     var rs = stmt.executeQuery("SELECT 1")) {
                    if (rs.next() && rs.getInt(1) == 1) {
                        log.debug("MySQL健康检查通过");
                    }
                }
            }
            return valid;
        } catch (Exception e) {
            log.warn("MySQL健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查Redis
     */
    private boolean checkRedis() {
        try {
            if (redisTemplate == null) {
                return false;
            }
            var connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                return false;
            }
            String pong = connectionFactory.getConnection().ping();
            return "PONG".equals(pong);
        } catch (Exception e) {
            log.warn("Redis健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查XXL-Job Admin
     * 通过HTTP调用XXL-Job Admin的健康检查接口
     */
    private boolean checkXxlJobAdmin() {
        try {
            String adminUrl = xxlJobAdminAddress.endsWith("/") 
                    ? xxlJobAdminAddress.substring(0, xxlJobAdminAddress.length() - 1) 
                    : xxlJobAdminAddress;
            
            ResponseEntity<String> response = restTemplate.getForEntity(
                    adminUrl + "/health", String.class);
            
            boolean ok = response.getStatusCode().is2xxSuccessful();
            if (ok) {
                log.debug("XXL-Job Admin健康检查通过: status={}", response.getStatusCode());
            } else {
                log.warn("XXL-Job Admin响应异常: status={}", response.getStatusCode());
            }
            return ok;
        } catch (Exception e) {
            log.warn("XXL-Job Admin健康检查失败: address={}, error={}", xxlJobAdminAddress, e.getMessage());
            return false;
        }
    }

    /**
     * 检查各AI模型服务的可用性
     * 多模型健康探测，支持配置多个AI服务端点
     */
    private boolean checkAiServices() {
        if (aiServiceEndpoints == null || aiServiceEndpoints.isEmpty()) {
            log.debug("未配置AI服务端点，跳过AI服务健康检查");
            return true;
        }

        try {
            String[] endpoints = aiServiceEndpoints.split(",");
            int upCount = 0;
            int totalCount = endpoints.length;

            for (String endpoint : endpoints) {
                String url = endpoint.trim();
                if (url.isEmpty()) continue;

                try {
                    ResponseEntity<String> response = restTemplate.getForEntity(
                            url + "/health", String.class);
                    
                    if (response.getStatusCode().is2xxSuccessful()) {
                        upCount++;
                        log.debug("AI服务健康检查通过: endpoint={}", url);
                    } else {
                        log.warn("AI服务异常: endpoint={}, status={}", url, response.getStatusCode());
                    }
                } catch (Exception ex) {
                    log.warn("AI服务不可达: endpoint={}, error={}", url, ex.getMessage());
                }
            }

            boolean allUp = upCount == totalCount;
            log.info("AI服务健康检查结果: {}/{} 服务正常", upCount, totalCount);
            return allUp || upCount > 0;
        } catch (Exception e) {
            log.warn("AI服务健康检查异常: error={}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查磁盘空间
     */
    private boolean checkDiskSpace() {
        try {
            java.io.File root = new java.io.File("/");
            long free = root.getFreeSpace();
            long total = root.getTotalSpace();
            double usedPercent = (double) (total - free) / total * 100;
            return usedPercent < 90;
        } catch (Exception e) {
            log.warn("磁盘空间检查失败: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 检查内存
     */
    private boolean checkMemory() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long total = runtime.totalMemory();
            long free = runtime.freeMemory();
            long used = total - free;
            double usedPercent = (double) used / total * 100;
            return usedPercent < 90;
        } catch (Exception e) {
            log.warn("内存检查失败: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 发送健康告警通知
     * 通过AlertNotificationService发送多渠道告警（钉钉、企业微信、邮件等）
     */
    private void sendHealthAlert(Map<String, Object> healthReport) {
        try {
            if (alertNotificationService == null) {
                log.warn("AlertNotificationService未注入，仅记录日志告警");
                log.warn("健康检查告警: {}", healthReport);
                return;
            }

            String status = (String) healthReport.getOrDefault("status", "UNKNOWN");
            String summary = (String) healthReport.getOrDefault("summary", "");

            StringBuilder detailBuilder = new StringBuilder();
            detailBuilder.append("**系统健康状态**: ").append(status).append("\n\n");
            detailBuilder.append("**汇总**: ").append(summary).append("\n\n");
            detailBuilder.append("| 组件 | 状态 |\n|------|------|\n");

            for (Map.Entry<String, Object> entry : healthReport.entrySet()) {
                String key = entry.getKey();
                if ("timestamp".equals(key) || "host".equals(key) || 
                    "status".equals(key) || "summary".equals(key)) {
                    continue;
                }
                String value = String.valueOf(entry.getValue());
                String statusIcon = "UP".equals(value) ? "✅ 正常" : "❌ 异常";
                detailBuilder.append("| ").append(key).append(" | ").append(statusIcon).append(" |\n");
            }

            detailBuilder.append("\n**检测时间**: ").append(healthReport.get("timestamp"));
            detailBuilder.append("\n**主机**: ").append(healthReport.get("host"));

            AlertNotificationService.AlertLevel level = "ALL_HEALTHY".equals(status)
                    ? AlertNotificationService.AlertLevel.INFO
                    : AlertNotificationService.AlertLevel.ERROR;

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("host", healthReport.get("host"));
            metadata.put("summary", summary);

            alertNotificationService.sendAlert(level,
                    "系统健康检查告警 - " + status,
                    detailBuilder.toString(),
                    metadata);

            log.info("健康告警通知已发送: level={}, summary={}", level, summary);
        } catch (Exception e) {
            log.error("发送健康告警异常", e);
        }
    }

    private String getHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String toJson(Map<String, Object> data) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }
}
