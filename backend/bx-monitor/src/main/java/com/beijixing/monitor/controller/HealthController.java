package com.beijixing.monitor.controller;

import com.beijixing.monitor.entity.HealthStatus;
import com.beijixing.monitor.service.HealthCheckService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/health")
public class HealthController {

    private final HealthCheckService healthCheckService;

    public HealthController(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    /**
     * 健康检查
     */
    @GetMapping
    public Map<String, Object> health() {
        HealthStatus status = healthCheckService.checkHealth();
        return Map.of(
                "code", 0,
                "status", status.getStatus(),
                "serviceName", status.getServiceName(),
                "healthyCount", status.getHealthyCount(),
                "totalCount", status.getTotalCount(),
                "components", status.getComponents(),
                "checkTime", status.getCheckTime()
        );
    }

    /**
     * 简要状态
     */
    @GetMapping("/status")
    public Map<String, Object> status() {
        HealthStatus status = healthCheckService.checkHealth();
        String statusText = status.getStatus().equals("UP") ? "healthy" : "unhealthy";
        return Map.of("status", statusText, "timestamp", status.getCheckTime());
    }

    /**
     * 获取所有服务健康状态
     */
    @GetMapping("/services")
    public Map<String, Object> getAllServiceHealth() {
        Map<String, HealthStatus> allHealth = healthCheckService.getAllServiceHealth();
        return Map.of("code", 0, "data", allHealth);
    }

    /**
     * 注册外部服务健康检查
     */
    @PostMapping("/services/register")
    public Map<String, Object> registerService(@RequestParam String serviceName,
                                                 @RequestParam String healthUrl) {
        healthCheckService.registerService(serviceName, healthUrl);
        return Map.of("code", 0, "message", "服务已注册健康检查");
    }
}
