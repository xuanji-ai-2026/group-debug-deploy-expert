package com.beijixing.bxlead.service.impl;

import cn.hutool.json.JSONUtil;
import com.beijixing.bxlead.service.AiIntentAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI意向分析服务实现类
 * @author 朱怡
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiIntentAnalysisServiceImpl implements AiIntentAnalysisService {
    
    @Value("${ai.service.url:http://localhost:8080}")
    private String aiServiceUrl;

    @Value("${ai.service.api-key:}")
    private String apiKey;
    
    // 竞品关键词库
    private static final List<String> COMPETITOR_KEYWORDS = Arrays.asList(
            "竞品A", "竞品B", "竞品C", "竞争对手", "其他厂商"
    );
    
    // 高意向关键词
    private static final List<String> HIGH_INTENT_KEYWORDS = Arrays.asList(
            "购买", "报价", "合作", "需求", "急需", "找供应商", "预算",
            "多少钱", "怎么买", "合作意向", "项目", "采购"
    );
    
    // 中意向关键词
    private static final List<String> MEDIUM_INTENT_KEYWORDS = Arrays.asList(
            "了解", "咨询", "对比", "看看", "感兴趣", "考虑一下",
            "方案", "产品介绍", "演示", "试用"
    );
    
    // 低意向关键词
    private static final List<String> LOW_INTENT_KEYWORDS = Arrays.asList(
            "随便看看", "了解一下", "问问", "看看再说"
    );
    
    @Override
    public Integer analyzeIntentScore(String content) {
        if (content == null || content.isEmpty()) {
            return 50;
        }
        
        int score = 50; // 基础分
        String lowerContent = content.toLowerCase();
        
        // 高意向关键词加分
        for (String keyword : HIGH_INTENT_KEYWORDS) {
            if (lowerContent.contains(keyword)) {
                score += 10;
            }
        }
        
        // 中意向关键词加分
        for (String keyword : MEDIUM_INTENT_KEYWORDS) {
            if (lowerContent.contains(keyword)) {
                score += 5;
            }
        }
        
        // 低意向关键词减分
        for (String keyword : LOW_INTENT_KEYWORDS) {
            if (lowerContent.contains(keyword)) {
                score -= 10;
            }
        }
        
        // 内容长度加分（详细说明代表更高意向）
        if (content.length() > 100) {
            score += 5;
        }
        if (content.length() > 300) {
            score += 5;
        }
        
        // 确保在0-100范围内
        return Math.max(0, Math.min(100, score));
    }
    
    @Override
    public String extractKeyInfo(String content) {
        Map<String, Object> result = new HashMap<>();
        
        // 提取联系方式（简单正则）
        String phone = extractPhone(content);
        String email = extractEmail(content);
        String budget = extractBudget(content);
        
        result.put("phone", phone);
        result.put("email", email);
        result.put("budget", budget);
        result.put("intentScore", analyzeIntentScore(content));
        result.put("competitor", detectCompetitor(content));
        
        return JSONUtil.toJsonStr(result);
    }
    
    @Override
    public String detectCompetitor(String content) {
        if (content == null) {
            return null;
        }
        
        for (String competitor : COMPETITOR_KEYWORDS) {
            if (content.contains(competitor)) {
                return competitor;
            }
        }
        return null;
    }
    
    @Override
    public String generateAnalysisResult(String content) {
        Integer score = analyzeIntentScore(content);
        String competitor = detectCompetitor(content);
        
        StringBuilder result = new StringBuilder();
        result.append("AI分析结果：\n");
        result.append("意向评分：").append(score).append("分\n");
        
        if (score >= 80) {
            result.append("意向等级：高意向\n");
        } else if (score >= 60) {
            result.append("意向等级：中意向\n");
        } else {
            result.append("意向等级：低意向\n");
        }
        
        if (competitor != null) {
            result.append("竞品提及：").append(competitor).append("\n");
        }
        
        result.append("建议：");
        if (score >= 80) {
            result.append("高意向客户，建议优先跟进，尽快联系报价。");
        } else if (score >= 60) {
            result.append("有一定意向，建议详细介绍产品方案。");
        } else {
            result.append("意向一般，建议长期培育，定期触达。");
        }
        
        return result.toString();
    }
    
    private String extractPhone(String content) {
        // 简化实现，实际应使用正则
        return null;
    }
    
    private String extractEmail(String content) {
        // 简化实现，实际应使用正则
        return null;
    }
    
    private String extractBudget(String content) {
        // 简化实现，实际应使用NLP
        return null;
    }
}