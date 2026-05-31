package com.beijixing.monitor.service;

import com.beijixing.monitor.entity.HealthStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings("nullness")
public class HealthCheckService implements DisposableBean {

    private final StringRedisTemplate redisTemplate;
    private final DataSource dataSource;
    private final RestTemplate restTemplate;
    private final ConcurrentHashMap<String, HealthStatus> serviceHealth = new ConcurrentHashMap<>();
    private volatile boolean running = true;
    private final ConcurrentHashMap<String, Thread> healthCheckThreads = new ConcurrentHashMap<>();

    public HealthCheckService(StringRedisTemplate redisTemplate, DataSource dataSource, RestTemplate restTemplate) {
        this.redisTemplate = redisTemplate;
        this.dataSource = dataSource;
        this.restTemplate = restTemplate;
    }

    /**
     * 健康检查
     */
    public HealthStatus checkHealth() {
        HealthStatus.HealthStatusBuilder builder = HealthStatus.builder()
                .serviceName("bx-monitor")
                .checkTime(LocalDateTime.now());

        Map<String, HealthStatus.ComponentHealth> components = new HashMap<>();

        boolean allHealthy = true;
        int healthyCount = 0;
        int totalCount = 0;

        // 1. 内存检查
        totalCount++;
        HealthStatus.ComponentHealth memHealth = checkMemory();
        components.put("memory", memHealth);
        if ("UP".equals(memHealth.getStatus())) healthyCount++; else allHealthy = false;

        // 2. 数据库检查
        totalCount++;
        HealthStatus.ComponentHealth dbHealth = checkDatabase();
        components.put("database", dbHealth);
        if ("UP".equals(dbHealth.getStatus())) healthyCount++; else allHealthy = false;

        // 3. Redis检查
        totalCount++;
        HealthStatus.ComponentHealth redisHealth = checkRedis();
        components.put("redis", redisHealth);
        if ("UP".equals(redisHealth.getStatus())) healthyCount++; else allHealthy = false;

        // 4. 磁盘检查
        totalCount++;
        HealthStatus.ComponentHealth diskHealth = checkDisk();
        components.put("disk", diskHealth);
        if ("UP".equals(diskHealth.getStatus())) healthyCount++; else allHealthy = false;

        // 5. 线程检查
        totalCount++;
        HealthStatus.ComponentHealth threadHealth = checkThreads();
        components.put("threads", threadHealth);
        if ("UP".equals(threadHealth.getStatus())) healthyCount++; else allHealthy = false;

        builder.components(components)
                .status(allHealthy ? "UP" : "DOWN")
                .healthyCount(healthyCount)
                .totalCount(totalCount);

        return builder.build();
    }

    private HealthStatus.ComponentHealth checkMemory() {
        long start = System.currentTimeMillis();
        try {
            Runtime runtime = Runtime.getRuntime();
            double used = (runtime.totalMemory() - runtime.freeMemory()) / 1024.0 / 1024.0;
            double max = runtime.maxMemory() / 1024.0 / 1024.0;
            double usage = (used / max) * 100;

            long latency = System.currentTimeMillis() - start;
            if (usage > 95) {
                return HealthStatus.ComponentHealth.builder()
                        .name("memory").status("DOWN")
                        .message(String.format("内存使用率%.1f%%", usage))
                        .latencyMs(latency).build();
            } else if (usage > 85) {
                return HealthStatus.ComponentHealth.builder()
                        .name("memory").status("WARN")
                        .message(String.format("内存使用率%.1f%%", usage))
                        .latencyMs(latency).build();
            }
            return HealthStatus.ComponentHealth.builder()
                    .name("memory").status("UP")
                    .message(String.format("内存使用率%.1f%% (%.0fMB/%.0fMB)", usage, used, max))
                    .latencyMs(latency).build();
        } catch (Exception e) {
            return HealthStatus.ComponentHealth.builder()
                    .name("memory").status("DOWN")
                    .message(e.getMessage()).latencyMs(System.currentTimeMillis() - start).build();
        }
    }

    private HealthStatus.ComponentHealth checkDatabase() {
        long start = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(5);
            long latency = System.currentTimeMillis() - start;
            return HealthStatus.ComponentHealth.builder()
                    .name("database").status(valid ? "UP" : "DOWN")
                    .message(valid ? "数据库连接正常" : "数据库连接失败")
                    .latencyMs(latency).build();
        } catch (Exception e) {
            return HealthStatus.ComponentHealth.builder()
                    .name("database").status("DOWN")
                    .message("数据库连接异常: " + e.getMessage())
                    .latencyMs(System.currentTimeMillis() - start).build();
        }
    }

    private HealthStatus.ComponentHealth checkRedis() {
        long start = System.currentTimeMillis();
        try {
            var connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                return HealthStatus.ComponentHealth.builder()
                        .name("redis").status("DOWN")
                        .message("Redis连接工厂未初始化")
                        .latencyMs(System.currentTimeMillis() - start).build();
            }
            String pong = connectionFactory.getConnection().ping();
            long latency = System.currentTimeMillis() - start;
            boolean healthy = "PONG".equalsIgnoreCase(pong);
            return HealthStatus.ComponentHealth.builder()
                    .name("redis").status(healthy ? "UP" : "DOWN")
                    .message(healthy ? "Redis连接正常" : "Redis连接失败")
                    .latencyMs(latency).build();
        } catch (Exception e) {
            return HealthStatus.ComponentHealth.builder()
                    .name("redis").status("DOWN")
                    .message("Redis连接异常: " + e.getMessage())
                    .latencyMs(System.currentTimeMillis() - start).build();
        }
    }

    private HealthStatus.ComponentHealth checkDisk() {
        long start = System.currentTimeMillis();
        try {
            java.io.File[] roots = java.io.File.listRoots();
            double maxUsage = 0;
            for (java.io.File root : roots) {
                long total = root.getTotalSpace();
                long free = root.getFreeSpace();
                double usage = ((double) (total - free) / total) * 100;
                maxUsage = Math.max(maxUsage, usage);
            }
            long latency = System.currentTimeMillis() - start;
            String status = maxUsage > 95 ? "DOWN" : (maxUsage > 85 ? "WARN" : "UP");
            return HealthStatus.ComponentHealth.builder()
                    .name("disk").status(status)
                    .message(String.format("磁盘使用率%.1f%%", maxUsage))
                    .latencyMs(latency).build();
        } catch (Exception e) {
            return HealthStatus.ComponentHealth.builder()
                    .name("disk").status("DOWN")
                    .message(e.getMessage()).latencyMs(System.currentTimeMillis() - start).build();
        }
    }

    private HealthStatus.ComponentHealth checkThreads() {
        long start = System.currentTimeMillis();
        try {
            int threadCount = ManagementFactory.getThreadMXBean().getThreadCount();
            int peakThreadCount = ManagementFactory.getThreadMXBean().getPeakThreadCount();
            long latency = System.currentTimeMillis() - start;

            String status = threadCount > 500 ? "WARN" : "UP";
            return HealthStatus.ComponentHealth.builder()
                    .name("threads").status(status)
                    .message(String.format("活动线程:%d, 峰值:%d", threadCount, peakThreadCount))
                    .latencyMs(latency).build();
        } catch (Exception e) {
            return HealthStatus.ComponentHealth.builder()
                    .name("threads").status("DOWN")
                    .message(e.getMessage()).latencyMs(System.currentTimeMillis() - start).build();
        }
    }

    /**
     * 获取服务列表的健康状态
     */
    public Map<String, HealthStatus> getAllServiceHealth() {
        return new HashMap<>(serviceHealth);
    }

    /**
     * 注册外部服务健康检查
     */
    @SuppressWarnings("nullness")
    public void registerService(String serviceName, String healthUrl) {
        Thread checkThread = new Thread(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    HealthStatus status = restTemplate.getForObject(healthUrl, HealthStatus.class);
                    if (status != null) {
                        serviceHealth.put(serviceName, status);
                    }
                    TimeUnit.MINUTES.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.info("健康检查线程 {} 被中断，即将退出", serviceName);
                    break;
                } catch (Exception e) {
                    log.warn("Failed to check health of service {}: {}", serviceName, e.getMessage());
                }
            }
        }, "health-check-" + serviceName);

        healthCheckThreads.put(serviceName, checkThread);
        checkThread.start();
        log.info("已注册服务健康检查: {} -> {}", serviceName, healthUrl);
    }

    @Override
    public void destroy() throws Exception {
        log.info("开始停止所有健康检查线程...");
        running = false;

        healthCheckThreads.forEach((serviceName, thread) -> {
            try {
                thread.interrupt();
                thread.join(5000);
                if (thread.isAlive()) {
                    log.warn("健康检查线程 {} 未能在5秒内停止", serviceName);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("等待健康检查线程 {} 停止时被中断", serviceName);
            }
        });

        healthCheckThreads.clear();
        log.info("所有健康检查线程已停止");
    }
}
