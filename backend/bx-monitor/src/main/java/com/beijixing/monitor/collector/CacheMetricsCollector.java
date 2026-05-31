package com.beijixing.monitor.collector;

import com.beijixing.monitor.entity.MetricsData;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class CacheMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final StringRedisTemplate redisTemplate;
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);

    public CacheMetricsCollector(MeterRegistry meterRegistry, StringRedisTemplate redisTemplate) {
        this.meterRegistry = meterRegistry;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        Gauge.builder("redis.hit.count", hitCount, AtomicLong::get).register(meterRegistry);
        Gauge.builder("redis.miss.count", missCount, AtomicLong::get).register(meterRegistry);
    }

    @Scheduled(fixedRateString = "${monitor.collect.cache-interval:30}000")
    public List<MetricsData> collect() {
        List<MetricsData> metrics = new ArrayList<>();
        try {
            RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
            if (factory == null) {
                log.warn("Redis connection factory is null, skipping cache metrics");
                return metrics;
            }

            RedisConnection connection = factory.getConnection();
            try {
                Properties memInfo = connection.serverCommands().info("memory");
                Properties statsInfo = connection.serverCommands().info("stats");
                Properties keyspaceInfo = connection.serverCommands().info("keyspace");

                // 内存使用
                if (memInfo != null) {
                    String usedMemory = memInfo.getProperty("used_memory");
                    String usedMemoryPeak = memInfo.getProperty("used_memory_peak");
                    String maxMemory = memInfo.getProperty("maxmemory");
                    String memFragmentationRatio = memInfo.getProperty("mem_fragmentation_ratio");

                    if (usedMemory != null) {
                        double usedMb = Long.parseLong(usedMemory) / 1024.0 / 1024.0;
                        metrics.add(buildMetric("redis.memory.used", usedMb, "MB"));
                    }
                    if (usedMemoryPeak != null) {
                        double peakMb = Long.parseLong(usedMemoryPeak) / 1024.0 / 1024.0;
                        metrics.add(buildMetric("redis.memory.peak", peakMb, "MB"));
                    }
                    if (maxMemory != null && !"0".equals(maxMemory)) {
                        double usage = (Long.parseLong(usedMemory) / Double.parseDouble(maxMemory)) * 100;
                        metrics.add(buildMetric("redis.memory.usage", usage, "%"));
                    }
                    if (memFragmentationRatio != null) {
                        metrics.add(buildMetric("redis.memory.fragmentation", Double.parseDouble(memFragmentationRatio), "ratio"));
                    }
                }

                // 命中率
                if (statsInfo != null) {
                    String keyspaceHits = statsInfo.getProperty("keyspace_hits");
                    String keyspaceMisses = statsInfo.getProperty("keyspace_misses");
                    if (keyspaceHits != null && keyspaceMisses != null) {
                        long hits = Long.parseLong(keyspaceHits);
                        long misses = Long.parseLong(keyspaceMisses);
                        long total = hits + misses;
                        hitCount.set(hits);
                        missCount.set(misses);
                        double hitRate = total > 0 ? (hits * 100.0 / total) : 0;
                        metrics.add(buildMetric("redis.hit.count", hits, "count"));
                        metrics.add(buildMetric("redis.miss.count", misses, "count"));
                        metrics.add(buildMetric("redis.hit.rate", hitRate, "%"));
                    }
                }

                // 连接数
                Properties clientsInfo = connection.serverCommands().info("clients");
                if (clientsInfo != null) {
                    String connectedClients = clientsInfo.getProperty("connected_clients");
                    if (connectedClients != null) {
                        metrics.add(buildMetric("redis.clients.connected", Long.parseLong(connectedClients), "count"));
                    }
                }

                // 键数量
                if (keyspaceInfo != null) {
                    keyspaceInfo.stringPropertyNames().forEach(key -> {
                        String value = keyspaceInfo.getProperty(key);
                        metrics.add(buildMetric("redis.keys." + key.replace(":", "."), value, ""));
                    });
                }

                // 指令统计
                if (statsInfo != null) {
                    String totalCommands = statsInfo.getProperty("total_commands_processed");
                    String totalConnections = statsInfo.getProperty("total_connections_received");
                    if (totalCommands != null) {
                        metrics.add(buildMetric("redis.commands.total", Long.parseLong(totalCommands), "count"));
                    }
                    if (totalConnections != null) {
                        metrics.add(buildMetric("redis.connections.total", Long.parseLong(totalConnections), "count"));
                    }
                }
            } finally {
                connection.close();
            }

            log.debug("Cache metrics collected: {} metrics", metrics.size());
        } catch (Exception e) {
            log.warn("Failed to collect cache metrics: {}", e.getMessage());
        }
        return metrics;
    }

    private MetricsData buildMetric(String name, String value, String unit) {
        double numValue;
        try {
            numValue = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            numValue = 0.0;
        }
        return buildMetric(name, numValue, unit);
    }

    private MetricsData buildMetric(String name, double value, String unit) {
        return MetricsData.builder()
                .metricName(name)
                .metricType("cache")
                .serviceName("bx-monitor")
                .value(value)
                .unit(unit)
                .collectTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .build();
    }
}
