package com.beijixing.risk.strategy.impl;

import com.beijixing.risk.dto.RiskCheckRequest;
import com.beijixing.risk.strategy.RiskStrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 反AI检测策略 - 检测内容是否为AI生成，并提供优化建议
 *
 * @author 林超 (EMP-SEC-001)
 * 检测特征：
 * 1. 语言风格一致性（过于规整）
 * 2. 缺乏口语化表达
 * 3. 段落结构过于规整
 * 4. 缺少个人化内容
 * 5. 重复模式检测
 */
@Slf4j
@Component
public class AntiAIDetectionStrategy implements RiskStrategyHandler {

    /**
     * AI生成内容的典型特征模式
     */
    private static final List<Pattern> AI_PATTERNS = Arrays.asList(
        // 过度使用连接词
        Pattern.compile("首先、其次、最后"),
        Pattern.compile("第一、第二、第三"),
        Pattern.compile("一方面、另一方面"),
        Pattern.compile("首先\\s*[，。]\\s*其次\\s*[，。]\\s*最后"),
        // 固定开头
        Pattern.compile("^(当然|毫无疑问|毫无疑问地说)"),
        // 过度使用书面语
        Pattern.compile("因此|由此可见|综上所述|不言而喻"),
        // 无实际意义的填充
        Pattern.compile("值得注意的是|需要指出的是|应当指出"),
        // 固定段落结构
        Pattern.compile("^\\d+[.、]\\s*[^\n]+[。]$"),
        // 重复开头
        Pattern.compile("^(亲爱的|尊敬的|各位)\\s*观众|用户|粉丝"),
        // 过于完美的排比
        Pattern.compile("[^。]{10,}[，][^。]{10,}[，][^。]{10,}[。]")
    );

    /**
     * 口语化词汇列表（AI内容通常缺乏）
     */
    private static final List<String> CASUAL_WORDS = Arrays.asList(
        "哈哈", "嘿嘿", "哎", "呀", "啦", "嘛", "吧", "哦", "嗯",
        "其实", "感觉", "觉得", "好像", "大概", "差不多", "还行"
    );

    @Override
    public String getStrategyType() {
        return "anti_ai";
    }

    @Override
    public boolean supports(RiskCheckRequest request) {
        Map<String, Object> params = request.getRequestParams();
        if (params == null) {
            return false;
        }
        Object content = params.get("content");
        return content != null && ("publish".equals(request.getOperationType()) ||
                                  "comment".equals(request.getOperationType()));
    }

    @Override
    public StrategyResult execute(RiskCheckRequest request) {
        Map<String, Object> params = request.getRequestParams();
        String content = params.get("content").toString();

        int aiScore = calculateAIScore(content);
        List<String> detectedPatterns = detectAIPatterns(content);

        // 分数越高越可能是AI生成（>80分高度疑似）
        if (aiScore >= 80) {
            log.info("高度疑似AI内容检测: score={}, patterns={}", aiScore, detectedPatterns);
            return StrategyResult.warn(20,
                "内容高度疑似AI生成，建议优化后发布",
                "建议添加口语化表达、打乱段落结构、增加个人经历");
        } else if (aiScore >= 60) {
            return StrategyResult.warn(50,
                "内容可能为AI生成，建议优化",
                generateOptimizationSuggestion(detectedPatterns));
        } else if (aiScore >= 40) {
            return StrategyResult.warn(70,
                "内容有轻微AI特征",
                "建议适当增加口语化表达");
        }

        return StrategyResult.pass();
    }

    /**
     * 计算AI生成可能性评分（0-100，越高越可能是AI）
     */
    private int calculateAIScore(String content) {
        int score = 0;

        // 1. 检测固定模式 (权重: 30分)
        for (Pattern pattern : AI_PATTERNS) {
            if (pattern.matcher(content).find()) {
                score += 15;
            }
        }

        // 2. 检查口语化词汇密度 (权重: 25分)
        int casualWordCount = 0;
        for (String word : CASUAL_WORDS) {
            if (content.contains(word)) {
                casualWordCount++;
            }
        }
        double casualDensity = (double) casualWordCount / (content.length() / 100.0);
        if (casualDensity < 0.5) {
            score += 25;  // 缺乏口语化表达
        } else if (casualDensity < 1.0) {
            score += 10;
        }

        // 3. 检查段落结构规整度 (权重: 20分)
        if (isOverStructured(content)) {
            score += 20;
        }

        // 4. 检查句子长度一致性 (权重: 15分)
        if (hasConsistentSentenceLength(content)) {
            score += 15;
        }

        // 5. 检查是否有过长连续段落 (权重: 10分)
        if (hasLongConsecutiveParagraphs(content)) {
            score += 10;
        }

        return Math.min(100, score);
    }

    /**
     * 检测具体AI特征模式
     */
    private List<String> detectAIPatterns(String content) {
        List<String> detected = new ArrayList<>();

        for (Pattern pattern : AI_PATTERNS) {
            if (pattern.matcher(content).find()) {
                String patternName = pattern.pattern()
                    .replace("^", "")
                    .replace("$", "");
                detected.add(patternName.length() > 20 ? patternName.substring(0, 20) : patternName);
            }
        }

        // 检查口语化词汇
        long casualCount = CASUAL_WORDS.stream()
            .filter(content::contains)
            .count();
        if (casualCount < 3) {
            detected.add("缺乏口语化表达");
        }

        // 检查段落结构
        if (isOverStructured(content)) {
            detected.add("段落结构过于规整");
        }

        return detected;
    }

    /**
     * 判断段落是否过于规整
     */
    private boolean isOverStructured(String content) {
        String[] paragraphs = content.split("\n");
        if (paragraphs.length < 2) {
            return false;
        }

        int avgLength = 0;
        for (String p : paragraphs) {
            avgLength += p.trim().length();
        }
        avgLength /= paragraphs.length;

        // 检查每段长度是否相近（差异小于20%）
        for (String p : paragraphs) {
            int diff = Math.abs(p.trim().length() - avgLength);
            if (avgLength > 0 && diff > avgLength * 0.2) {
                return false;  // 有段落长度差异较大，说明不规整
            }
        }

        return paragraphs.length >= 3;  // 3段以上且长度相近，视为规整
    }

    /**
     * 判断句子长度是否过于一致
     */
    private boolean hasConsistentSentenceLength(String content) {
        String[] sentences = content.split("[。！？]");
        if (sentences.length < 3) {
            return false;
        }

        int avgLength = 0;
        int count = 0;
        for (String s : sentences) {
            String trimmed = s.trim();
            if (trimmed.length() > 5) {
                avgLength += trimmed.length();
                count++;
            }
        }
        if (count < 3) return false;
        avgLength /= count;

        // 检查长度标准差
        double variance = 0;
        for (String s : sentences) {
            String trimmed = s.trim();
            if (trimmed.length() > 5) {
                double diff = trimmed.length() - avgLength;
                variance += diff * diff;
            }
        }
        variance /= count;

        // 标准差小于平均值的10%视为过于一致
        return Math.sqrt(variance) < avgLength * 0.1;
    }

    /**
     * 判断是否有过长连续段落
     */
    private boolean hasLongConsecutiveParagraphs(String content) {
        String[] paragraphs = content.split("\n");
        int longCount = 0;
        for (String p : paragraphs) {
            if (p.trim().length() > 200) {
                longCount++;
            }
        }
        return longCount >= 2;
    }

    /**
     * 生成优化建议
     */
    private String generateOptimizationSuggestion(List<String> detectedPatterns) {
        if (detectedPatterns.isEmpty()) {
            return "建议添加一些个人化表达和口语化内容";
        }

        StringBuilder sb = new StringBuilder("优化建议：");
        for (String pattern : detectedPatterns) {
            if (pattern.contains("连接词")) {
                sb.append("1. 打乱'首先/其次/最后'等固定结构；");
            }
            if (pattern.contains("缺乏口语")) {
                sb.append("2. 添加'感觉/好像/其实'等口语化表达；");
            }
            if (pattern.contains("规整")) {
                sb.append("3. 调整段落长度，增加变化；");
            }
        }
        sb.append("4. 适当添加语气词和停顿标记");
        return sb.toString();
    }
}
