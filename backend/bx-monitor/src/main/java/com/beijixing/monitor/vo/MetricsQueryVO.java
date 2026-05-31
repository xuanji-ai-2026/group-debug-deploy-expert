package com.beijixing.monitor.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsQueryVO {

    private String metricType;
    private String metricName;
    private String serviceName;
    private String host;
    private Long startTime;
    private Long endTime;
    private Integer page;
    private Integer size;
}
