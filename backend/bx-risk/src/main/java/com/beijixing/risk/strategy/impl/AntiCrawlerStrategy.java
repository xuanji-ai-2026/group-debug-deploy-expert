package com.beijixing.risk.strategy.impl;

import com.beijixing.risk.dto.RiskCheckRequest;
import com.beijixing.risk.strategy.RiskStrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 反爬虫策略 - 检测和防护爬虫行为
 *
 * @author 林超 (EMP-SEC-001)
 * 检测项：
 * 1. User-Agent伪装检测
 * 2. 请求频率异常
 * 3. Referer来源异常
 * 4. IP访问集中度
 * 5. 请求参数规律性
 */
@Slf4j
@Component
@SuppressWarnings("nullness")
public class AntiCrawlerStrategy implements RiskStrategyHandler {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 可疑User-Agent列表
     */
    private static final List<String> SUSPICIOUS_UA_PATTERNS = Arrays.asList(
        "python", "curl", "wget", "scrapy", "httpclient", "java/", "go-http-client",
        "node-fetch", "axios", "okhttp", "rookit", "bot", "crawler", "spider"
    );

    /**
     * 正常Referer来源
     */
    private static final List<String> VALID_REFERERS = Arrays.asList(
        "https://www.douyin.com", "https://www.kuaishou.com",
        "https://www.xiaohongshu.com", "https://weixin.qq.com"
    );

    @Override
    public String getStrategyType() {
        return "crawler";
    }

    @Override
    public boolean supports(RiskCheckRequest request) {
        return "access".equals(request.getOperationType()) ||
               "login".equals(request.getOperationType());
    }

    @Override
    public StrategyResult execute(RiskCheckRequest request) {
        Map<String, Object> params = request.getRequestParams();
        if (params == null) {
            return StrategyResult.pass();
        }

        // 1. 检查User-Agent
        String userAgent = (String) params.get("userAgent");
        if (userAgent != null && isSuspiciousUA(userAgent)) {
            log.warn("可疑User-Agent检测: {}", userAgent);
            return StrategyResult.block(30, "检测到可疑User-Agent，可能为爬虫行为");
        }

        // 2. 检查请求频率
        if (isRequestFrequencyAbnormal(request)) {
            return StrategyResult.rateLimit(50, "请求频率异常，可能为爬虫行为");
        }

        // 3. 检查Referer
        String referer = (String) params.get("referer");
        if (referer != null && !isValidReferer(referer)) {
            log.warn("可疑Referer: {}", referer);
            return StrategyResult.warn(60, "Referer来源异常", "建议使用真实浏览器访问");
        }

        // 4. 检查IP访问集中度
        if (isIpAccessConcentrated(request)) {
            return StrategyResult.block(20, "IP访问集中度异常");
        }

        // 5. 检查请求参数规律性
        if (hasRegularPattern(params)) {
            return StrategyResult.rateLimit(40, "请求参数具有明显规律性");
        }

        return StrategyResult.pass();
    }

    /**
     * 判断User-Agent是否可疑
     */
    private boolean isSuspiciousUA(String ua) {
        String lowerUA = ua.toLowerCase();
        for (String pattern : SUSPICIOUS_UA_PATTERNS) {
            if (lowerUA.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断请求频率是否异常
     */
    private boolean isRequestFrequencyAbnormal(RiskCheckRequest request) {
        String ip = request.getIpAddress();
        if (ip == null) {
            return false;
        }

        String key = "risk:crawler:ip:" + ip;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }

        // 1分钟内超过100次请求视为异常
        return count != null && count > 100;
    }

    /**
     * 判断Referer是否有效
     */
    private boolean isValidReferer(String referer) {
        for (String valid : VALID_REFERERS) {
            if (referer.startsWith(valid)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断IP访问是否集中
     */
    private boolean isIpAccessConcentrated(RiskCheckRequest request) {
        String ip = request.getIpAddress();
        if (ip == null) {
            return false;
        }

        // 检查同一IP访问的账号数量
        String accountKey = "risk:crawler:ip:accounts:" + ip;
        Long accountCount = redisTemplate.opsForValue().increment(accountKey);
        if (accountCount != null && accountCount == 1) {
            redisTemplate.expire(accountKey, Duration.ofHours(1));
        }

        // 1小时内访问超过10个不同账号视为集中
        return accountCount != null && accountCount > 10;
    }

    /**
     * 判断请求是否有规律性
     */
    private boolean hasRegularPattern(Map<String, Object> params) {
        // 检测请求间隔是否过于规律（固定间隔）
        String interval = (String) params.get("interval");
        if (interval != null) {
            try {
                int intVal = Integer.parseInt(interval);
                // 间隔在0.5-2秒之间且完全一致，视为有规律
                return intVal >= 500 && intVal <= 2000;
            } catch (NumberFormatException ignored) {
            }
        }

        return false;
    }
}
