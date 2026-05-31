package com.beijixing.monitor.collector;

import com.beijixing.monitor.entity.MetricsData;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class SystemMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final AtomicReference<Double> cpuUsage = new AtomicReference<>(0.0);
    private final AtomicReference<Double> memoryUsage = new AtomicReference<>(0.0);
    private final AtomicReference<Double> diskUsage = new AtomicReference<>(0.0);
    private final AtomicReference<Long> networkBytesIn = new AtomicReference<>(0L);
    private final AtomicReference<Long> networkBytesOut = new AtomicReference<>(0L);

    public SystemMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        Gauge.builder("system.cpu.usage", cpuUsage, AtomicReference::get).register(meterRegistry);
        Gauge.builder("system.memory.usage", memoryUsage, AtomicReference::get).register(meterRegistry);
        Gauge.builder("system.disk.usage", diskUsage, AtomicReference::get).register(meterRegistry);
        Gauge.builder("system.network.bytes.in", networkBytesIn, AtomicReference::get).register(meterRegistry);
        Gauge.builder("system.network.bytes.out", networkBytesOut, AtomicReference::get).register(meterRegistry);
    }

    @Scheduled(fixedRateString = "${monitor.collect.system-interval:30}000")
    public List<MetricsData> collect() {
        List<MetricsData> metrics = new ArrayList<>();
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            Runtime runtime = Runtime.getRuntime();

            // CPU使用率（使用Java 14+推荐方式）
            double cpu = -1;
            try {
                com.sun.management.OperatingSystemMXBean sunOsBean =
                    (com.sun.management.OperatingSystemMXBean) osBean;
                // getCpuLoad()是getSystemCpuLoad()的替代方案（Java 14+）
                cpu = sunOsBean.getCpuLoad() * 100;
            } catch (Exception e) {
                // fallback to memory-based calculation
            }
            if (cpu < 0) cpu = (1 - runtime.freeMemory() / (double) runtime.maxMemory()) * 100;
            cpuUsage.set(cpu);
            metrics.add(buildMetric("system.cpu.usage", cpu, "%"));

            // 内存使用率
            double totalMem = runtime.totalMemory();
            double freeMem = runtime.freeMemory();
            double usedMem = totalMem - freeMem;
            double memUsage = (usedMem / totalMem) * 100;
            memoryUsage.set(memUsage);
            metrics.add(buildMetric("system.memory.usage", memUsage, "%"));
            metrics.add(buildMetric("system.memory.used", usedMem / 1024 / 1024 / 1024, "GB"));
            metrics.add(buildMetric("system.memory.total", totalMem / 1024 / 1024 / 1024, "GB"));

            // 线程数
            int threadCount = ManagementFactory.getThreadMXBean().getThreadCount();
            metrics.add(buildMetric("system.threads", (double) threadCount, "count"));

            // 加载类数
            int loadedClassCount = ManagementFactory.getClassLoadingMXBean().getLoadedClassCount();
            metrics.add(buildMetric("system.classes.loaded", (double) loadedClassCount, "count"));

            log.debug("System metrics collected: {} metrics", metrics.size());
        } catch (Exception e) {
            log.error("Failed to collect system metrics", e);
        }
        return metrics;
    }

    private MetricsData buildMetric(String name, Double value, String unit) {
        return MetricsData.builder()
                .metricName(name)
                .metricType("system")
                .serviceName("bx-monitor")
                .host(getHostName())
                .value(value)
                .unit(unit)
                .collectTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .build();
    }

    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
