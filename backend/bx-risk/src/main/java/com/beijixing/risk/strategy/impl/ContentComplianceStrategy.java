package com.beijixing.risk.strategy.impl;

import com.beijixing.risk.dto.RiskCheckRequest;
import com.beijixing.risk.strategy.RiskStrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 内容合规策略 - 违禁词检测、敏感内容过滤
 *
 * @author 林超 (EMP-SEC-001)
 * 检测项：
 * 1. 政治敏感词
 * 2. 违禁行业词
 * 3. 违禁行为词
 * 4. 平台违规词
 */
@Slf4j
@Component
public class ContentComplianceStrategy implements RiskStrategyHandler {

    /**
     * 违禁词库
     */
    private static final Set<String> FORBIDDEN_WORDS = new HashSet<>(Arrays.asList(
            "政治敏感词1", "政治敏感词2",
            "违禁行业词1", "违禁行业词2"
    ));

    /**
     * 联系方式正则
     */
    private static final Pattern[] CONTACT_PATTERNS = {
            Pattern.compile("[1-9]\\d{10}"),  // 手机号
            Pattern.compile("\\w+@\\w+\\.\\w+"),  // 邮箱
            Pattern.compile("[微V信][\\s:：]*\\d+")  // 微信号
    };

    @Override
    public String getStrategyType() {
        return "content_compliance";
    }

    @Override
    public boolean supports(RiskCheckRequest request) {
        return "CONTENT".equals(request.getRiskType());
    }

    @Override
    public StrategyResult execute(RiskCheckRequest request) {
        String content = request.getContent();
        if (content == null || content.isEmpty()) {
            return StrategyResult.pass();
        }

        // 检测违禁词
        for (String word : FORBIDDEN_WORDS) {
            if (content.contains(word)) {
                log.warn("检测到违禁词: {}", word);
                return StrategyResult.block(20, "检测到违禁词: " + word);
            }
        }

        // 检测联系方式
        if (hasContactInfo(content)) {
            log.warn("检测到联系方式");
            return StrategyResult.block(40, "检测到联系方式，禁止发布");
        }

        return StrategyResult.pass();
    }

    /**
     * 检测联系方式
     */
    private boolean hasContactInfo(String content) {
        for (Pattern pattern : CONTACT_PATTERNS) {
            if (pattern.matcher(content).find()) {
                return true;
            }
        }
        return false;
    }
}
