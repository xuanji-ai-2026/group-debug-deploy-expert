package com.beijixing.monitor.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRule {

    private Long id;
    private String ruleId;
    private String ruleName;
    private String metricName;
    private String metricType;        // system, app, business, database, cache
    private String alertType;         // threshold, trend, compared
    private String operator;          // gt, lt, gte, lte, eq
    private Double threshold;
    private Double trendThreshold;    // 趋势告警阈值
    private Double comparedThreshold; // 同比环比阈值
    private String alertLevel;        // P1, P2, P3, P4
    private Boolean enabled;
    private String serviceName;
    private Integer evalInterval;      // 评估间隔(秒)
    private Integer cooldownMinutes;  // 冷却时间(分钟)
    private String channels;         // email, sms, dingtalk, wecom
}
