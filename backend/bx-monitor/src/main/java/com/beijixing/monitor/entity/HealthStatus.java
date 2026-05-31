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
public class HealthStatus {

    private String serviceName;
    private String status;            // UP, DOWN, UNKNOWN
    private Double responseTime;
    private Integer activeConnections;
    private Double cpuUsage;
    private Double memoryUsage;
    private Double diskUsage;
    private Integer threadCount;
    private Integer healthyCount;
    private Integer totalCount;
    private Map<String, ComponentHealth> components;
    private LocalDateTime checkTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentHealth {
        private String name;
        private String status;
        private String message;
        private Long latencyMs;
    }
}
