package com.beijixing.monitor.controller;

import com.beijixing.monitor.entity.MetricsData;
import com.beijixing.monitor.service.MetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/metrics")
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    /**
     * 查询指标列表
     */
    @GetMapping
    public Map<String, Object> queryMetrics(
            @RequestParam(required = false) String metricType,
            @RequestParam(required = false) String metricName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        List<MetricsData> metrics = metricsService.queryMetrics(metricType, metricName, start, end);
        return Map.of("code", 0, "data", metrics, "total", metrics.size());
    }

    /**
     * 获取指标统计
     */
    @GetMapping("/stats/{metricName}")
    public Map<String, Object> getMetricStats(@PathVariable String metricName) {
        Map<String, Double> stats = metricsService.getMetricStats(metricName);
        double compare = metricsService.compareWithPrevious(metricName);
        return Map.of("code", 0, "metricName", metricName, "stats", stats, "compareWithPrevious", compare);
    }

    /**
     * 获取指标名列表
     */
    @GetMapping("/names")
    public Map<String, Object> listMetricNames(@RequestParam(required = false) String metricType) {
        List<String> names = metricsService.listMetricNames(metricType);
        return Map.of("code", 0, "data", names);
    }

    /**
     * 手动上报指标
     */
    @PostMapping("/report")
    public Map<String, Object> reportMetric(@RequestBody MetricsData metric) {
        metricsService.saveMetric(metric);
        return Map.of("code", 0, "message", "指标已上报");
    }

    /**
     * 批量上报指标
     */
    @PostMapping("/report/batch")
    public Map<String, Object> reportMetricsBatch(@RequestBody List<MetricsData> metrics) {
        metrics.forEach(metricsService::saveMetric);
        return Map.of("code", 0, "message", "批量指标已上报", "count", metrics.size());
    }
}
