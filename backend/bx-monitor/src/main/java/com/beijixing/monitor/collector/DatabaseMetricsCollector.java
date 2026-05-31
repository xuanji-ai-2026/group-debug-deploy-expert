package com.beijixing.monitor.collector;

import com.beijixing.monitor.entity.MetricsData;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class DatabaseMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final DataSource dataSource;
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicInteger idleConnections = new AtomicInteger(0);

    public DatabaseMetricsCollector(MeterRegistry meterRegistry, DataSource dataSource) {
        this.meterRegistry = meterRegistry;
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() {
        Gauge.builder("db.connections.active", activeConnections, AtomicInteger::get).register(meterRegistry);
        Gauge.builder("db.connections.idle", idleConnections, AtomicInteger::get).register(meterRegistry);
    }

    @Scheduled(fixedRateString = "${monitor.collect.database-interval:30}000")
    public List<MetricsData> collect() {
        List<MetricsData> metrics = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            if (dataSource instanceof com.zaxxer.hikari.HikariDataSource hikariDS) {
                var hikariPool = hikariDS.getHikariPoolMXBean();
                int active = hikariPool.getActiveConnections();
                int idle = hikariPool.getIdleConnections();
                int total = hikariPool.getTotalConnections();
                int waiting = hikariPool.getThreadsAwaitingConnection();

                activeConnections.set(active);
                idleConnections.set(idle);

                metrics.add(buildMetric("db.connections.active", active, "count"));
                metrics.add(buildMetric("db.connections.idle", idle, "count"));
                metrics.add(buildMetric("db.connections.total", total, "count"));
                metrics.add(buildMetric("db.connections.waiting", waiting, "count"));
            }

            metrics.add(buildMetric("db.info.version", 0.0, ""));
            metrics.add(buildMetric("db.info.product", metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion(), ""));
            metrics.add(buildMetric("db.slow.queries", 0, "count"));

            log.debug("Database metrics collected: {} metrics", metrics.size());
        } catch (Exception e) {
            log.error("Failed to collect database metrics", e);
        }
        return metrics;
    }

    private MetricsData buildMetric(String name, int value, String unit) {
        return MetricsData.builder()
                .metricName(name)
                .metricType("database")
                .serviceName("bx-monitor")
                .value((double) value)
                .unit(unit)
                .collectTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .build();
    }

    private MetricsData buildMetric(String name, String value, String unit) {
        return MetricsData.builder()
                .metricName(name)
                .metricType("database")
                .serviceName("bx-monitor")
                .value(0.0)
                .unit(unit)
                .collectTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .build();
    }

    private MetricsData buildMetric(String name, double value, String unit) {
        return MetricsData.builder()
                .metricName(name)
                .metricType("database")
                .serviceName("bx-monitor")
                .value(value)
                .unit(unit)
                .collectTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .build();
    }
}
