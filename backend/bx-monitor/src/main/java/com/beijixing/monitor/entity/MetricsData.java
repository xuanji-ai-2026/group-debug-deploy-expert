package com.beijixing.monitor.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsData {

    private Long id;
    private String metricName;
    private String metricType;       // system, app, business, database, cache
    private String serviceName;
    private String host;
    private Double value;
    private String unit;
    private Map<String, String> tags;
    private LocalDateTime collectTime;
    private LocalDateTime createTime;
}
