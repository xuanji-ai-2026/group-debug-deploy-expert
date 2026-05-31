package com.beijixing.monitor.alarm;

import com.beijixing.monitor.entity.AlertRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmsAlarmSender implements AlarmSender {

    public SmsAlarmSender() {
    }

    @Override
    public boolean send(AlertRecord alert) {
        // SMS集成示例（阿里云/腾讯云短信）
        // 此处为占位实现，实际使用时需要配置短信网关
        try {
            String message = buildSmsContent(alert);
            log.info("SMS alert would be sent: {}", message);
            // 实际调用短信网关 API
            // webClient.post().uri(smsGatewayUrl).bodyValue(...)
            return true;
        } catch (Exception e) {
            log.error("Failed to send SMS alert: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getType() {
        return "sms";
    }

    @Override
    public boolean isEnabled() {
        return false; // 需要配置SMS网关才启用
    }

    private String buildSmsContent(AlertRecord alert) {
        return String.format("[%s]%s 指标:%s 当前值:%.2f 阈值:%.2f",
                alert.getAlertLevel(), alert.getAlertName(),
                alert.getMetricName(), alert.getCurrentValue(), alert.getThresholdValue());
    }
}
