package com.beijixing.monitor.collector;

import com.beijixing.monitor.entity.MetricsData;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class AppMetricsCollector {

    private final MeterRegistry meterRegistry;

    public AppMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        Timer.builder("http.server.requests")
                .description("HTTP server requests")
                .register(meterRegistry);
    }

    @Scheduled(fixedRateString = "${monitor.collect.app-interval:15}000")
    public List<MetricsData> collect() {
        List<MetricsData> metrics = new ArrayList<>();
        try {
            // JVM堆内存
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

            metrics.add(buildMetric("jvm.memory.heap.used", heapUsage.getUsed() / 1024 / 1024, "MB"));
            metrics.add(buildMetric("jvm.memory.heap.max", heapUsage.getMax() / 1024 / 1024, "MB"));
            metrics.add(buildMetric("jvm.memory.heap.committed", heapUsage.getCommitted() / 1024 / 1024, "MB"));
            metrics.add(buildMetric("jvm.memory.nonheap.used", nonHeapUsage.getUsed() / 1024 / 1024, "MB"));

            // GC
            List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
            long totalGcCount = 0;
            long totalGcTime = 0;
            for (GarbageCollectorMXBean gc : gcBeans) {
                totalGcCount += gc.getCollectionCount();
                totalGcTime += gc.getCollectionTime();
            }
            metrics.add(buildMetric("jvm.gc.count", totalGcCount, "count"));
            metrics.add(buildMetric("jvm.gc.time", totalGcTime, "ms"));

            // 活跃线程
            ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
            while (rootGroup.getParent() != null) {
                rootGroup = rootGroup.getParent();
            }
            int activeThreads = rootGroup.activeCount();
            metrics.add(buildMetric("jvm.threads.active", activeThreads, "count"));

            // 元空间
            metrics.add(buildMetric("jvm.memory.metaspaces.used", nonHeapUsage.getUsed() / 1024 / 1024, "MB"));

            log.debug("App metrics collected: {} metrics", metrics.size());
        } catch (Exception e) {
            log.error("Failed to collect app metrics", e);
        }
        return metrics;
    }

    private MetricsData buildMetric(String name, long value, String unit) {
        return MetricsData.builder()
                .metricName(name)
                .metricType("app")
                .serviceName("bx-monitor")
                .value((double) value)
                .unit(unit)
                .collectTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .build();
    }
}
