package com.beijixing.risk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 风控配置类
 *
 * @author 林超 (EMP-SEC-001)
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "risk")
public class RiskConfig {

    /**
     * 规则引擎配置
     */
    private RuleConfig rule = new RuleConfig();

    /**
     * 评分引擎配置
     */
    private ScoreConfig score = new ScoreConfig();

    /**
     * 决策引擎配置
     */
    private DecisionConfig decision = new DecisionConfig();

    /**
     * 操作频率配置
     */
    private FrequencyConfig frequency = new FrequencyConfig();

    @Data
    public static class RuleConfig {
        /**
         * 规则缓存时间（秒）
         */
        private int cacheTtl = 300;
        /**
         * 最大规则数量
         */
        private int maxRules = 1000;
    }

    @Data
    public static class ScoreConfig {
        /**
         * 评分阈值配置
         */
        private Map<String, Integer> thresholds;

        public int getLowThreshold() {
            return thresholds != null ? thresholds.getOrDefault("low", 80) : 80;
        }

        public int getMediumThreshold() {
            return thresholds != null ? thresholds.getOrDefault("medium", 60) : 60;
        }

        public int getHighThreshold() {
            return thresholds != null ? thresholds.getOrDefault("high", 40) : 40;
        }

        public int getCriticalThreshold() {
            return thresholds != null ? thresholds.getOrDefault("critical", 20) : 20;
        }
    }

    @Data
    public static class DecisionConfig {
        /**
         * 决策超时时间（毫秒）
         */
        private long timeoutMs = 1000;
        /**
         * 最大重试次数
         */
        private int maxRetries = 3;
    }

    @Data
    public static class FrequencyConfig {
        /**
         * 发布频率限制（次/天）
         */
        private int publishLimit = 10;
        /**
         * 私信频率限制（次/天）
         */
        private int messageLimit = 50;
        /**
         * 关注频率限制（次/天）
         */
        private int followLimit = 100;
        /**
         * 评论频率限制（次/天）
         */
        private int commentLimit = 200;

        public int getLimitByType(String operationType) {
            return switch (operationType) {
                case "publish" -> publishLimit;
                case "message" -> messageLimit;
                case "follow" -> followLimit;
                case "comment" -> commentLimit;
                default -> 100;
            };
        }
    }
}
