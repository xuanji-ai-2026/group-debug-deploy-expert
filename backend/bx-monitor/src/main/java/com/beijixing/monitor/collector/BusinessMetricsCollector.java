package com.beijixing.monitor.collector;

import com.beijixing.monitor.entity.MetricsData;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class BusinessMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();

    public BusinessMetricsCollector(MeterRegistry meterRegistry,
                                     StringRedisTemplate redisTemplate) {
        this.meterRegistry = meterRegistry;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        // 注册业务计数器
        registerCounter("business.login.count");
        registerCounter("business.order.count");
        registerCounter("business.api.call.count");
        registerCounter("business.register.count");
    }

    private void registerCounter(String name) {
        Counter.builder(name).description(name).register(meterRegistry);
        counters.put(name, new AtomicLong(0));
    }

    public void incrementCounter(String name) {
        counters.computeIfAbsent(name, k -> {
            Counter.builder(k).description(k).register(meterRegistry);
            return new AtomicLong(0);
        }).incrementAndGet();
    }

    @Scheduled(fixedRateString = "${monitor.collect.business-interval:60}000")
    public List<MetricsData> collect() {
        List<MetricsData> metrics = new ArrayList<>();
        try {
            // 从Redis获取业务指标
            Long loginCount = redisTemplate.opsForValue().increment("bx:business:login:daily");
            Long orderCount = redisTemplate.opsForValue().increment("bx:business:order:daily");
            Long apiCallCount = redisTemplate.opsForValue().increment("bx:business:api:daily");
            Long registerCount = redisTemplate.opsForValue().increment("bx:business:register:daily");

            metrics.add(buildMetric("business.login.daily", loginCount != null ? loginCount : 0, "count"));
            metrics.add(buildMetric("business.order.daily", orderCount != null ? orderCount : 0, "count"));
            metrics.add(buildMetric("business.api.call.daily", apiCallCount != null ? apiCallCount : 0, "count"));
            metrics.add(buildMetric("business.register.daily", registerCount != null ? registerCount : 0, "count"));

            // 从内存计数器获取实时增量
            counters.forEach((name, value) -> {
                long v = value.getAndSet(0);
                if (v > 0) {
                    metrics.add(buildMetric(name + ".increment", v, "count"));
                }
            });

            log.debug("Business metrics collected: {} metrics", metrics.size());
        } catch (Exception e) {
            log.warn("Failed to collect business metrics from Redis, using defaults: {}", e.getMessage());
            // 写入默认值
            metrics.add(buildMetric("business.login.daily", 0L, "count"));
            metrics.add(buildMetric("business.order.daily", 0L, "count"));
            metrics.add(buildMetric("business.api.call.daily", 0L, "count"));
        }
        return metrics;
    }

    private MetricsData buildMetric(String name, Long value, String unit) {
        return MetricsData.builder()
                .metricName(name)
                .metricType("business")
                .serviceName("bx-monitor")
                .value(value.doubleValue())
                .unit(unit)
                .collectTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .build();
    }
}
