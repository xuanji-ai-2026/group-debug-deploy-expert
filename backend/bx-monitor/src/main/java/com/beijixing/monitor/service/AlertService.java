package com.beijixing.monitor.service;

import com.beijixing.monitor.alarm.AlarmNotifier;
import com.beijixing.monitor.config.AlertConfig;
import com.beijixing.monitor.entity.AlertRecord;
import com.beijixing.monitor.entity.AlertRule;
import com.beijixing.monitor.entity.MetricsData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@SuppressWarnings("nullness")
public class AlertService {

    private final AlarmNotifier alarmNotifier;
    private final AlertConfig alertConfig;
    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<String, AlertRule> rules = new ConcurrentHashMap<>();

    public AlertService(AlarmNotifier alarmNotifier, AlertConfig alertConfig,
                        StringRedisTemplate redisTemplate) {
        this.alarmNotifier = alarmNotifier;
        this.alertConfig = alertConfig;
        this.redisTemplate = redisTemplate;
        initDefaultRules();
    }

    private void initDefaultRules() {
        // 默认告警规则
        List<AlertRule> defaultRules = List.of(
                AlertRule.builder().ruleId("rule.cpu.high").ruleName("CPU使用率过高")
                        .metricName("system.cpu.usage").metricType("system")
                        .alertType("threshold").operator("gt").threshold(90.0)
                        .alertLevel("P2").enabled(true).evalInterval(60).cooldownMinutes(10)
                        .channels("dingtalk,wecom").build(),
                AlertRule.builder().ruleId("rule.memory.high").ruleName("内存使用率过高")
                        .metricName("system.memory.usage").metricType("system")
                        .alertType("threshold").operator("gt").threshold(85.0)
                        .alertLevel("P2").enabled(true).evalInterval(60).cooldownMinutes(10)
                        .channels("dingtalk,wecom").build(),
                AlertRule.builder().ruleId("rule.jvm.heap.high").ruleName("JVM堆内存过高")
                        .metricName("jvm.memory.heap.used").metricType("app")
                        .alertType("threshold").operator("gt").threshold(2048.0)
                        .alertLevel("P3").enabled(true).evalInterval(120).cooldownMinutes(15)
                        .channels("dingtalk").build(),
                AlertRule.builder().ruleId("rule.redis.hit.low").ruleName("Redis命中率低")
                        .metricName("redis.hit.rate").metricType("cache")
                        .alertType("threshold").operator("lt").threshold(50.0)
                        .alertLevel("P3").enabled(true).evalInterval(300).cooldownMinutes(30)
                        .channels("dingtalk").build(),
                AlertRule.builder().ruleId("rule.db.connections.high").ruleName("数据库连接数过高")
                        .metricName("db.connections.active").metricType("database")
                        .alertType("threshold").operator("gt").threshold(20.0)
                        .alertLevel("P1").enabled(true).evalInterval(30).cooldownMinutes(5)
                        .channels("dingtalk,wecom,email").build()
        );

        defaultRules.forEach(rule -> rules.put(rule.getRuleId(), rule));
    }

    /**
     * 评估告警规则
     */
    public void evaluate(MetricsData metric) {
        List<AlertRule> matchedRules = rules.values().stream()
                .filter(AlertRule::getEnabled)
                .filter(r -> r.getMetricName().equals(metric.getMetricName()))
                .filter(r -> r.getMetricType().equals(metric.getMetricType()))
                .toList();

        for (AlertRule rule : matchedRules) {
            if (shouldAlert(rule, metric)) {
                fireAlert(rule, metric);
            }
        }
    }

    private boolean shouldAlert(AlertRule rule, MetricsData metric) {
        // 冷却检查
        String cooldownKey = "bx:alert:cooldown:" + rule.getRuleId() + ":" + metric.getHost();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            return false;
        }

        double value = metric.getValue();
        double threshold = rule.getThreshold();
        String operator = rule.getOperator();

        boolean triggered = switch (operator) {
            case "gt" -> value > threshold;
            case "lt" -> value < threshold;
            case "gte" -> value >= threshold;
            case "lte" -> value <= threshold;
            case "eq" -> Math.abs(value - threshold) < 0.001;
            default -> false;
        };

        return triggered;
    }

    private void fireAlert(AlertRule rule, MetricsData metric) {
        AlertRecord alert = AlertRecord.builder()
                .alertId(UUID.randomUUID().toString())
                .alertName(rule.getRuleName())
                .alertLevel(rule.getAlertLevel())
                .metricName(metric.getMetricName())
                .alertType(rule.getAlertType())
                .serviceName(metric.getServiceName())
                .host(metric.getHost())
                .currentValue(metric.getValue())
                .thresholdValue(rule.getThreshold())
                .message(String.format("%s 触发告警：%s = %.2f，阈值 = %.2f",
                        rule.getRuleName(), metric.getMetricName(),
                        metric.getValue(), rule.getThreshold()))
                .status("firing")
                .fireTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .build();

        // 设置冷却
        String cooldownKey = "bx:alert:cooldown:" + rule.getRuleId() + ":" + metric.getHost();
        redisTemplate.opsForValue().set(cooldownKey, "1",
                Duration.ofMinutes(rule.getCooldownMinutes() > 0 ? rule.getCooldownMinutes()
                        : alertConfig.getAlert().getCooldownMinutes()));

        // 发送告警
        alarmNotifier.notify(alert);

        // 保存告警记录
        saveAlertRecord(alert);

        log.warn("ALERT FIRED: {} [{}] {}={} threshold={}",
                alert.getAlertId(), rule.getAlertLevel(),
                metric.getMetricName(), metric.getValue(), rule.getThreshold());
    }

    private void saveAlertRecord(AlertRecord alert) {
        try {
            String key = "bx:alert:records:" + alert.getStatus();
            redisTemplate.opsForHash().put(key, alert.getAlertId(), toJson(alert));
            redisTemplate.expire(key, Duration.ofDays(7));
        } catch (Exception e) {
            log.error("Failed to save alert record: {}", e.getMessage());
        }
    }

    private String toJson(AlertRecord alert) {
        return String.format("{\"alertId\":\"%s\",\"alertName\":\"%s\",\"level\":\"%s\",\"metric\":\"%s\",\"value\":%.2f,\"status\":\"%s\"}",
                alert.getAlertId(), alert.getAlertName(), alert.getAlertLevel(),
                alert.getMetricName(), alert.getCurrentValue(), alert.getStatus());
    }

    // ==================== API Methods ====================

    public List<AlertRecord> getAlertList(String status, int page, int size) {
        List<AlertRecord> results = new ArrayList<>();
        try {
            String key = "bx:alert:records:" + (status != null ? status : "firing");
            Map<Object, Object> records = redisTemplate.opsForHash().entries(key);
            // 简化返回，实际应分页
            records.values().forEach(v -> {
                // 解析并添加到结果
            });
        } catch (Exception e) {
            log.error("Failed to get alert list: {}", e.getMessage());
        }
        return results;
    }

    public AlertRule createRule(AlertRule rule) {
        if (rule.getRuleId() == null) {
            rule.setRuleId(UUID.randomUUID().toString());
        }
        rules.put(rule.getRuleId(), rule);
        return rule;
    }

    public AlertRule updateRule(String ruleId, AlertRule rule) {
        if (rules.containsKey(ruleId)) {
            rule.setRuleId(ruleId);
            rules.put(ruleId, rule);
            return rule;
        }
        return null;
    }

    public boolean deleteRule(String ruleId) {
        return rules.remove(ruleId) != null;
    }

    public List<AlertRule> getAllRules() {
        return new ArrayList<>(rules.values());
    }

    public AlertRule getRule(String ruleId) {
        return rules.get(ruleId);
    }
}
