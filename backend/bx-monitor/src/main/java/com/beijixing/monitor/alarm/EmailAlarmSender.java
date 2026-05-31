package com.beijixing.monitor.alarm;

import com.beijixing.monitor.config.AlertConfig;
import com.beijixing.monitor.entity.AlertRecord;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@ConditionalOnProperty(name = "spring.mail.host")
@SuppressWarnings("nullness")
public class EmailAlarmSender implements AlarmSender {

    private final JavaMailSender mailSender;
    private final AlertConfig alertConfig;
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public EmailAlarmSender(JavaMailSender mailSender, AlertConfig alertConfig) {
        this.mailSender = mailSender;
        this.alertConfig = alertConfig;
    }

    @Override
    public boolean send(AlertRecord alert) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(alertConfig.getAlert().getEmailReceivers().toArray(new String[0]));
            helper.setSubject(buildSubject(alert));
            helper.setText(buildHtmlContent(alert), true);

            mailSender.send(message);
            return true;
        } catch (MessagingException e) {
            log.error("Failed to send email alert: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getType() {
        return "email";
    }

    @Override
    public boolean isEnabled() {
        return alertConfig.getAlert().getEmailReceivers() != null
                && !alertConfig.getAlert().getEmailReceivers().isEmpty();
    }

    private String buildSubject(AlertRecord alert) {
        return String.format("[%s] %s - %s", alert.getAlertLevel(), alert.getAlertName(), alert.getMetricName());
    }

    private String buildHtmlContent(AlertRecord alert) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif;'>");
        sb.append("<h2 style='color:").append(getLevelColor(alert.getAlertLevel())).append(";'>")
                .append(alert.getAlertName()).append("</h2>");
        sb.append("<table style='border-collapse: collapse; width: 600px;'>");
        sb.append("<tr><td style='padding: 8px; border: 1px solid #ddd; font-weight: bold;'>告警级别</td>")
                .append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(alert.getAlertLevel()).append("</td></tr>");
        sb.append("<tr><td style='padding: 8px; border: 1px solid #ddd; font-weight: bold;'>指标名称</td>")
                .append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(alert.getMetricName()).append("</td></tr>");
        sb.append("<tr><td style='padding: 8px; border: 1px solid #ddd; font-weight: bold;'>当前值</td>")
                .append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(alert.getCurrentValue()).append("</td></tr>");
        sb.append("<tr><td style='padding: 8px; border: 1px solid #ddd; font-weight: bold;'>阈值</td>")
                .append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(alert.getThresholdValue()).append("</td></tr>");
        sb.append("<tr><td style='padding: 8px; border: 1px solid #ddd; font-weight: bold;'>服务</td>")
                .append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(alert.getServiceName()).append("</td></tr>");
        sb.append("<tr><td style='padding: 8px; border: 1px solid #ddd; font-weight: bold;'>主机</td>")
                .append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(alert.getHost()).append("</td></tr>");
        sb.append("<tr><td style='padding: 8px; border: 1px solid #ddd; font-weight: bold;'>发生时间</td>")
                .append("<td style='padding: 8px; border: 1px solid #ddd;'>")
                .append(alert.getFireTime() != null ? alert.getFireTime().format(DF) : "").append("</td></tr>");
        sb.append("<tr><td style='padding: 8px; border: 1px solid #ddd; font-weight: bold;' colspan='2'>详情</td></tr>");
        sb.append("<tr><td style='padding: 8px; border: 1px solid #ddd;' colspan='2'>").append(alert.getMessage()).append("</td></tr>");
        sb.append("</table></body></html>");
        return sb.toString();
    }

    private String getLevelColor(String level) {
        return switch (level) {
            case "P1" -> "#d32f2f";
            case "P2" -> "#f57c00";
            case "P3" -> "#fbc02d";
            default -> "#388e3c";
        };
    }
}
