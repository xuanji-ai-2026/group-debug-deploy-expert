package com.beijixing.monitor.service;

import com.beijixing.monitor.entity.MetricsData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@SuppressWarnings({"nullness", "unused"})
public class MetricsService {

    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<String, LinkedList<Double>> slidingWindows = new ConcurrentHashMap<>();
    private static final int WINDOW_SIZE = 60; // 60个数据点窗口

    public MetricsService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 保存指标到Redis
     */
    @SuppressWarnings("nullness")
    public void saveMetric(MetricsData metric) {
        try {
            String key = String.format("bx:monitor:metrics:%s:%s",
                    metric.getMetricType(), metric.getMetricName());

            Map<String, String> data = new HashMap<>();
            data.put("value", String.valueOf(metric.getValue()));
            data.put("service", metric.getServiceName());
            data.put("host", metric.getHost() != null ? metric.getHost() : "");
            data.put("unit", metric.getUnit() != null ? metric.getUnit() : "");
            data.put("time", metric.getCollectTime().toString());

            redisTemplate.opsForHash().putAll(key, data);
            redisTemplate.expire(key, Duration.ofMinutes(10));

            // 维护滑动窗口
            updateSlidingWindow(metric.getMetricName(), metric.getValue());

            log.debug("Metric saved: {} = {}", metric.getMetricName(), metric.getValue());
        } catch (Exception e) {
            log.error("Failed to save metric {}: {}", metric.getMetricName(), e.getMessage());
        }
    }

    /**
     * 查询指标列表
     */
    public List<MetricsData> queryMetrics(String metricType, String metricName,
                                          LocalDateTime start, LocalDateTime end) {
        List<MetricsData> results = new ArrayList<>();
        try {
            String pattern = "bx:monitor:metrics:" +
                    (metricType != null ? metricType : "*") + ":" +
                    (metricName != null ? metricName : "*");

            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null) {
                for (String key : keys) {
                    Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
                    if (!data.isEmpty()) {
                        MetricsData m = MetricsData.builder()
                                .metricName(key.substring(key.lastIndexOf(':') + 1))
                                .metricType(key.split(":")[3])
                                .value(Double.parseDouble((String) data.get("value")))
                                .serviceName((String) data.get("service"))
                                .host((String) data.get("host"))
                                .unit((String) data.get("unit"))
                                .collectTime(LocalDateTime.parse((String) data.get("time")))
                                .build();
                        results.add(m);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to query metrics: {}", e.getMessage());
        }
        return results;
    }

    /**
     * 获取指标统计
     */
    public Map<String, Double> getMetricStats(String metricName) {
        LinkedList<Double> window = slidingWindows.get(metricName);
        if (window == null || window.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Double> stats = new HashMap<>();
        double sum = 0;
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;

        for (Double v : window) {
            sum += v;
            max = Math.max(max, v);
            min = Math.min(min, v);
        }

        int count = window.size();
        stats.put("count", (double) count);
        stats.put("sum", sum);
        stats.put("avg", sum / count);
        stats.put("max", max);
        stats.put("min", min);

        return stats;
    }

    /**
     * 获取所有指标名
     */
    public List<String> listMetricNames(String metricType) {
        try {
            String pattern = "bx:monitor:metrics:" + (metricType != null ? metricType : "*") + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys == null) return Collections.emptyList();

            return keys.stream()
                    .map(k -> k.substring(k.lastIndexOf(':') + 1))
                    .distinct()
                    .sorted()
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list metric names: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 趋势分析 - 与上一周期比较
     */
    public double compareWithPrevious(String metricName) {
        LinkedList<Double> window = slidingWindows.get(metricName);
        if (window == null || window.size() < 2) {
            return 0;
        }
        Double current = window.peekLast();
        Double previous = window.get(window.size() - 2);
        if (previous == 0) return 0;
        return ((current - previous) / previous) * 100;
    }

    private void updateSlidingWindow(String metricName, Double value) {
        LinkedList<Double> window = slidingWindows.computeIfAbsent(metricName, k -> new LinkedList<>());
        synchronized (window) {
            window.addLast(value);
            while (window.size() > WINDOW_SIZE) {
                window.removeFirst();
            }
        }
    }
}
