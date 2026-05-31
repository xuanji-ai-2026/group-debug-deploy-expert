package com.beijixing.system.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统监控控制器
 *
 * 功能：SM-005 系统监控（服务状态、资源监控）
 *
 * @author bx-system
 */
@Slf4j
@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class MonitorController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * SM-005-01: 健康检查
     * GET /api/v1/monitor/health
     */
    @GetMapping("/monitor/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "success");
        result.put("status", "UP");
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }

    /**
     * SM-005-02: 服务基本信息
     * GET /api/v1/monitor/info
     */
    @GetMapping("/monitor/info")
    public ResponseEntity<Map<String, Object>> info() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();

        Map<String, Object> info = new HashMap<>();
        info.put("appName", "bx-system");
        info.put("appVersion", "1.0.0");
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("javaVendor", System.getProperty("java.vendor"));
        info.put("osName", System.getProperty("os.name"));
        info.put("osVersion", System.getProperty("os.version"));
        info.put("osArch", System.getProperty("os.arch"));
        info.put("startTime", formatUptime(runtimeBean.getStartTime()));
        info.put("uptime", formatUptimeMs(runtimeBean.getUptime()));
        info.put("serverTime", LocalDateTime.now().format(FORMATTER));

        return successData(info);
    }

    /**
     * SM-005-03: JVM 内存监控
     * GET /api/v1/monitor/jvm-memory
     */
    @GetMapping("/monitor/jvm-memory")
    public ResponseEntity<Map<String, Object>> jvmMemory() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

        Map<String, Object> memory = new HashMap<>();

        // 堆内存
        Map<String, Object> heap = new HashMap<>();
        heap.put("init", formatBytes(heapUsage.getInit()));
        heap.put("used", formatBytes(heapUsage.getUsed()));
        heap.put("committed", formatBytes(heapUsage.getCommitted()));
        heap.put("max", formatBytes(heapUsage.getMax()));
        heap.put("usagePercent", heapUsage.getMax() > 0 ? (heapUsage.getUsed() * 100 / heapUsage.getMax()) : 0);
        memory.put("heap", heap);

        // 非堆内存
        Map<String, Object> nonHeap = new HashMap<>();
        nonHeap.put("init", formatBytes(nonHeapUsage.getInit()));
        nonHeap.put("used", formatBytes(nonHeapUsage.getUsed()));
        nonHeap.put("committed", formatBytes(nonHeapUsage.getCommitted()));
        nonHeap.put("max", formatBytes(nonHeapUsage.getMax()));
        memory.put("nonHeap", nonHeap);

        return successData(memory);
    }

    /**
     * SM-005-04: 系统资源监控
     * GET /api/v1/monitor/system
     */
    @GetMapping("/monitor/system")
    public ResponseEntity<Map<String, Object>> systemMetrics() {
        Runtime runtime = Runtime.getRuntime();

        Map<String, Object> system = new HashMap<>();
        system.put("availableProcessors", runtime.availableProcessors());
        system.put("freeMemory", formatBytes(runtime.freeMemory()));
        system.put("totalMemory", formatBytes(runtime.totalMemory()));
        system.put("maxMemory", formatBytes(runtime.maxMemory()));
        system.put("usedMemory", formatBytes(runtime.totalMemory() - runtime.freeMemory()));

        // 操作系统信息
        Map<String, Object> os = new HashMap<>();
        os.put("availableProcessors", ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors());

        // 线程信息
        Map<String, Object> threadInfo = new HashMap<>();
        threadInfo.put("count", ManagementFactory.getThreadMXBean().getThreadCount());
        threadInfo.put("peak", ManagementFactory.getThreadMXBean().getPeakThreadCount());
        threadInfo.put("daemon", ManagementFactory.getThreadMXBean().getDaemonThreadCount());

        system.put("os", os);
        system.put("threads", threadInfo);

        return successData(system);
    }

    /**
     * SM-005-05: 自定义指标
     * GET /api/v1/monitor/metrics
     */
    @GetMapping("/monitor/metrics")
    public ResponseEntity<Map<String, Object>> metrics(
            @RequestParam(required = false) String name) {
        Map<String, Object> metrics = new HashMap<>();

        // 假数据示例，实际可对接 Micrometer、Prometheus 等
        metrics.put("custom_metric_1", 100);
        metrics.put("custom_metric_2", "healthy");
        metrics.put("custom_metric_3", System.currentTimeMillis());

        return successData(metrics);
    }

    // ==================== 辅助方法 ====================

    private ResponseEntity<Map<String, Object>> successData(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "success");
        result.put("data", data);
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }

    /**
     * 格式化启动时间
     */
    private String formatUptime(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(FORMATTER);
    }

    /**
     * 格式化运行时长
     */
    private String formatUptimeMs(long uptimeMs) {
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        return String.format("%d天 %d小时 %d分钟 %d秒", days, hours % 24, minutes % 60, seconds % 60);
    }

    /**
     * 格式化字节数
     */
    private String formatBytes(long bytes) {
        if (bytes < 0) {
            return "unknown";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        int unit = 0;
        double value = bytes;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        while (value >= 1024 && unit < units.length - 1) {
            value /= 1024;
            unit++;
        }
        return String.format("%.2f %s", value, units[unit]);
    }
}
