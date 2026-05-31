package com.beijixing.monitor.alarm;

import com.beijixing.monitor.config.AlertConfig;
import com.beijixing.monitor.entity.AlertRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@SuppressWarnings("nullness")
public class WebhookAlarmSender implements AlarmSender {

    private final AlertConfig alertConfig;
    private final WebClient webClient = WebClient.builder().build();
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public WebhookAlarmSender(AlertConfig alertConfig) {
        this.alertConfig = alertConfig;
    }

    @Async
    @Override
    public boolean send(AlertRecord alert) {
        boolean success = false;

        // 钉钉Webhook
        if (alertConfig.getAlert().getDingtalkWebhook() != null
                && !alertConfig.getAlert().getDingtalkWebhook().isEmpty()) {
            success = sendDingTalk(alert) || success;
        }

        // 企业微信Webhook
        if (alertConfig.getAlert().getWecomWebhook() != null
                && !alertConfig.getAlert().getWecomWebhook().isEmpty()) {
            success = sendWeCom(alert) || success;
        }

        return success;
    }

    private boolean sendDingTalk(AlertRecord alert) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "markdown");

            Map<String, Object> markdown = new HashMap<>();
            markdown.put("title", buildSubject(alert));

            StringBuilder content = new StringBuilder();
            content.append("### ").append(alert.getAlertName()).append("\n\n");
            content.append("> **告警级别**: ").append(getLevelEmoji(alert.getAlertLevel()))
                    .append(" ").append(alert.getAlertLevel()).append("\n\n");
            content.append("- **指标**: ").append(alert.getMetricName()).append("\n");
            content.append("- **当前值**: ").append(String.format("%.2f", alert.getCurrentValue())).append("\n");
            content.append("- **阈值**: ").append(String.format("%.2f", alert.getThresholdValue())).append("\n");
            content.append("- **服务**: ").append(alert.getServiceName()).append("\n");
            content.append("- **主机**: ").append(alert.getHost()).append("\n");
            content.append("- **时间**: ").append(
                    alert.getFireTime() != null ? alert.getFireTime().format(DF) : "").append("\n\n");
            content.append("> ").append(alert.getMessage());

            markdown.put("text", content.toString());
            body.put("markdown", markdown);

            String result = webClient.post()
                    .uri(alertConfig.getAlert().getDingtalkWebhook())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("DingTalk webhook result: {}", result);
            return result != null && result.contains("\"errcode\":0");
        } catch (Exception e) {
            log.error("Failed to send DingTalk webhook: {}", e.getMessage());
            return false;
        }
    }

    private boolean sendWeCom(AlertRecord alert) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "markdown");

            Map<String, Object> markdown = new HashMap<>();
            StringBuilder content = new StringBuilder();
            content.append(alert.getAlertName()).append("\n");
            content.append("> 告警级别: ").append(getLevelEmoji(alert.getAlertLevel()))
                    .append(" ").append(alert.getAlertLevel()).append("\n");
            content.append("- 指标: ").append(alert.getMetricName()).append("\n");
            content.append("- 当前值: ").append(String.format("%.2f", alert.getCurrentValue())).append("\n");
            content.append("- 阈值: ").append(String.format("%.2f", alert.getThresholdValue())).append("\n");
            content.append("- 服务: ").append(alert.getServiceName()).append("\n");
            content.append("- 主机: ").append(alert.getHost()).append("\n");
            content.append("- 时间: ").append(
                    alert.getFireTime() != null ? alert.getFireTime().format(DF) : "").append("\n");
            content.append("- 详情: ").append(alert.getMessage());

            markdown.put("content", content.toString());
            body.put("markdown", markdown);

            String result = webClient.post()
                    .uri(alertConfig.getAlert().getWecomWebhook())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("WeCom webhook result: {}", result);
            return result != null && result.contains("\"errcode\":0");
        } catch (Exception e) {
            log.error("Failed to send WeCom webhook: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getType() {
        return "webhook";
    }

    @Override
    public boolean isEnabled() {
        return (alertConfig.getAlert().getDingtalkWebhook() != null
                && !alertConfig.getAlert().getDingtalkWebhook().isEmpty())
                || (alertConfig.getAlert().getWecomWebhook() != null
                && !alertConfig.getAlert().getWecomWebhook().isEmpty());
    }

    private String buildSubject(AlertRecord alert) {
        return String.format("[%s] %s - %s", alert.getAlertLevel(), alert.getAlertName(), alert.getMetricName());
    }

    private String getLevelEmoji(String level) {
        return switch (level) {
            case "P1" -> "🔴";
            case "P2" -> "🟠";
            case "P3" -> "🟡";
            default -> "🟢";
        };
    }
}
