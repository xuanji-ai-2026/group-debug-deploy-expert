package com.beijixing.monitor.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRecord {

    private Long id;
    private String alertId;
    private String alertName;
    private String alertLevel;        // P1, P2, P3, P4
    private String metricName;
    private String alertType;          // threshold, trend, compared
    private String serviceName;
    private String host;
    private Double currentValue;
    private Double thresholdValue;
    private String message;
    private String status;            // firing, resolved
    private LocalDateTime fireTime;
    private LocalDateTime resolveTime;
    private LocalDateTime createTime;
}
