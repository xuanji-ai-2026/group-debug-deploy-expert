package com.beijixing.monitor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "monitor")
public class AlertConfig {

    private CollectConfig collect = new CollectConfig();
    private AlertProperties alert = new AlertProperties();
    private RedisProperties redis = new RedisProperties();

    @Data
    public static class CollectConfig {
        private int systemInterval = 30;
        private int appInterval = 15;
        private int businessInterval = 60;
        private int databaseInterval = 30;
        private int cacheInterval = 30;
    }

    @Data
    public static class AlertProperties {
        private int cooldownMinutes = 10;
        private String dingtalkWebhook;
        private String wecomWebhook;
        private List<String> emailReceivers;
    }

    @Data
    public static class RedisProperties {
        private String metricsPrefix = "bx:monitor:metrics:";
        private int metricsTtl = 300;
    }
}
