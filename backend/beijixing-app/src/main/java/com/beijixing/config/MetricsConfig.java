package com.beijixing.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Configuration
@ConditionalOnClass(MeterRegistry.class)
public class MetricsConfig {

    private final MeterRegistry meterRegistry;

    public MetricsConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        log.info("[MetricsConfig] 监控指标配置初始化完成 - Micrometer Registry已注册");
        initBusinessMetrics();
    }

    private final ConcurrentMap<String, AtomicLong> businessGauges = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> businessCounters = new ConcurrentHashMap<>();

    private void initBusinessMetrics() {
        registerGauge("bx.order.pending.count", "待处理订单数量", 0L);
        registerGauge("bx.user.online.count", "在线用户数量", 0L);
        registerGauge("bx.lead.today.count", "今日新增商机数", 0L);
        registerGauge("bx.ai.task.queue.size", "AI任务队列长度", 0L);
        log.info("[MetricsConfig] 业务指标Gauge注册完成: {}个", businessGauges.size());
    }

    public void registerGauge(String metricName, String description, long initialValue) {
        AtomicLong value = new AtomicLong(initialValue);
        businessGauges.put(metricName, value);
        Gauge.builder(metricName, value, AtomicLong::get)
                .description(description)
                .tag("application", "beijixing-ai")
                .register(meterRegistry);
    }

    public void setGaugeValue(String metricName, long value) {
        AtomicLong gauge = businessGauges.get(metricName);
        if (gauge != null) {
            gauge.set(value);
        }
    }

    public void incrementGauge(String metricName) {
        AtomicLong gauge = businessGauges.get(metricName);
        if (gauge != null) {
            gauge.incrementAndGet();
        }
    }

    public void decrementGauge(String metricName) {
        AtomicLong gauge = businessGauges.get(metricName);
        if (gauge != null) {
            gauge.decrementAndGet();
        }
    }

    public Counter getOrRegisterCounter(String metricName, String description) {
        return businessCounters.computeIfAbsent(metricName, key ->
                Counter.builder(key)
                        .description(description)
                        .tag("application", "beijixing-ai")
                        .register(meterRegistry));
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordTimer(Timer.Sample sample, String metricName, String description) {
        sample.stop(Timer.builder(metricName)
                .description(description)
                .tag("application", "beijixing-ai")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(meterRegistry));
    }

    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
}
