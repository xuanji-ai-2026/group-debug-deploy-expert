package com.beijixing.social.crawl.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.beijixing.ai.service.AiCoreService;
import com.beijixing.ai.model.TextGenerationRequest;
import com.beijixing.social.crawl.entity.SocialComment;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AiIntentAnalysisV2Service implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(AiIntentAnalysisV2Service.class);

    private final AiCoreService aiCoreService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(20);

    private static final Map<String, List<String>> INTENT_KEYWORDS = new HashMap<>();
    
    static {
        INTENT_KEYWORDS.put("HIGH_BUY", Arrays.asList(
            "购买", "报价", "价格", "多少钱", "怎么买", "哪里买", "下单",
            "合作", "需求", "急需", "找供应商", "预算", "采购",
            "想要", "求购", "想买", "准备买", "打算买"
        ));
        
        INTENT_KEYWORDS.put("HIGH_CONTACT", Arrays.asList(
            "加微信", "加V", "+v", "私信我", "联系我", "咨询",
            "联系方式", "电话", "手机号", "微信号",
            "怎么联系", "在哪里", "地址"
        ));
        
        INTENT_KEYWORDS.put("HIGH_COMPETITOR", Arrays.asList(
            "竞品A用了", "竞品B效果", "对比了XX", "之前用的XX",
            "换一个试试", "别的牌子不行", "看了好几家"
        ));
        
        INTENT_KEYWORDS.put("MEDIUM_INTEREST", Arrays.asList(
            "了解", "咨询", "对比", "看看", "感兴趣", "考虑一下",
            "方案", "产品介绍", "演示", "试用", "体验",
            "效果怎么样", "好用吗", "推荐吗"
        ));
        
        INTENT_KEYWORDS.put("MEDIUM_QUESTION", Arrays.asList(
            "是什么", "怎么用", "多少钱", "哪里有",
            "适合什么人", "有什么效果", "成分", "材质",
            "保质期", "发货", "售后"
        ));
        
        INTENT_KEYWORDS.put("LOW_CASUAL", Arrays.asList(
            "随便看看", "了解一下", "问问", "看看再说",
            "路过", "收藏先", "以后再说"
        ));

        INTENT_KEYWORDS.put("NEGATIVE", Arrays.asList(
            "骗人", "假货", "差评", "不好用", "退货",
            "投诉", "举报", "太贵", "不值得", "别买"
        ));
    }

    public IntentAnalysisResult analyzeComment(SocialComment comment) {
        String text = comment.getCommentText();
        if (text == null || text.trim().isEmpty()) {
            return createEmptyResult(comment);
        }

        log.info("开始AI深度意向分析: commentId={}, platform={}, text={}", 
                comment.getCommentId(), comment.getPlatformCode(), 
                text.length() > 50 ? text.substring(0, 50) + "..." : text);

        IntentAnalysisResult result = new IntentAnalysisResult();

        int keywordScore = calculateKeywordScore(text);
        result.setKeywordScore(keywordScore);

        extractContactInfo(text, result);
        detectUrgencySignals(text, result);
        detectPainPoints(text, result);
        detectCompetitorMention(text, result);
        detectPurchaseStage(text, result);
        estimateBudget(text, result);
        analyzeUserAuthority(comment, result);

        if (keywordScore >= 70 || result.isWantsDirectContact() || result.isHasHighUrgency()) {
            CompletableFuture.runAsync(() -> {
                try {
                    IntentRecognitionResponse aiResult = callAiDeepAnalysis(comment, text);
                    synchronized (result) {
                        result.setAiScore(aiResult.getIntentScore());
                        result.setAiConfidence(aiResult.getConfidence());
                        result.setAiTags(aiResult.getTags());
                        result.setAiSummary(aiResult.getSummary());
                        result.setAiSuggestedAction(aiResult.getSuggestedAction());
                        result.setAiRecommendedTemplate(aiResult.getRecommendedTemplate());
                    }
                } catch (Exception e) {
                    log.error("AI深度分析失败: commentId={}", comment.getId(), e);
                }
            }, executorService);
        }

        int finalScore = calculateFinalScore(result);
        result.setFinalScore(finalScore);
        result.setLevel(determineLevel(finalScore));
        result.setHighIntent(finalScore >= 75);

        updateCommentWithResult(comment, result);

        return result;
    }

    private int calculateKeywordScore(String text) {
        String lowerText = text.toLowerCase();
        int score = 40;

        for (Map.Entry<String, List<String>> entry : INTENT_KEYWORDS.entrySet()) {
            String category = entry.getKey();
            List<String> keywords = entry.getValue();

            for (String keyword : keywords) {
                if (lowerText.contains(keyword.toLowerCase())) {
                    switch (category) {
                        case "HIGH_BUY":
                            score += 15;
                            break;
                        case "HIGH_CONTACT":
                            score += 12;
                            break;
                        case "HIGH_COMPETITOR":
                            score += 10;
                            break;
                        case "MEDIUM_INTEREST":
                            score += 6;
                            break;
                        case "MEDIUM_QUESTION":
                            score += 4;
                            break;
                        case "LOW_CASUAL":
                            score -= 8;
                            break;
                        case "NEGATIVE":
                            score -= 15;
                            break;
                    }
                }
            }
        }

        if (text.length() > 100) score += 5;
        if (text.length() > 300) score += 5;
        if (containsEmoji(text)) score += 3;
        if (hasMultipleQuestions(text)) score += 4;

        return Math.max(0, Math.min(100, score));
    }

    private void extractContactInfo(String text, IntentAnalysisResult result) {
        Pattern phonePattern = Pattern.compile("1[3-9]\\d{9}");
        Matcher phoneMatcher = phonePattern.matcher(text);
        if (phoneMatcher.find()) {
            result.setExtractedPhone(phoneMatcher.group());
            result.setHasPhoneContact(true);
        }

        Pattern wechatPattern = Pattern.compile("[a-zA-Z][a-zA-Z0-9_-]{5,19}");
        Matcher wechatMatcher = wechatPattern.matcher(text);
        if (wechatMatcher.find() && (text.contains("微信") || text.contains("v") || text.contains("V"))) {
            result.setExtractedWechat(wechatMatcher.group());
            result.setHasWechatContact(true);
        }

        if (text.contains("私信") || text.contains("联系") || text.contains("咨询")) {
            result.setWantsDirectContact(true);
        }
    }

    private void detectUrgencySignals(String text, IntentAnalysisResult result) {
        List<String> urgentWords = Arrays.asList("急", "赶紧", "马上", "今天", "明天", "现在", 
                                                "等不了", "急需", "紧急", "尽快", "立刻");
        for (String word : urgentWords) {
            if (text.contains(word)) {
                result.addUrgencySignal(word);
                result.setHasHighUrgency(true);
            }
        }
    }

    private void detectPainPoints(String text, IntentAnalysisResult result) {
        List<String> painPatterns = Arrays.asList(
            ".*不好.*", ".*问题.*", ".*太贵.*", ".*找不到.*", ".*不知道.*",
            ".*没用过.*", ".*担心.*", ".*怕.*", ".*不满意.*", ".*想换.*"
        );
        
        for (String pattern : painPatterns) {
            if (text.matches(pattern)) {
                result.addPainPoint(pattern.replace(".*", ""));
            }
        }
    }

    private void detectCompetitorMention(String text, IntentAnalysisResult result) {
        List<String> competitors = Arrays.asList("竞品A", "竞品B", "其他品牌", "别家", 
                                                 "以前用的", "之前买的", "别的牌子");
        for (String competitor : competitors) {
            if (text.contains(competitor)) {
                result.setCompetitorMentioned(competitor);
                break;
            }
        }
    }

    private void detectPurchaseStage(String text, IntentAnalysisResult result) {
        if (text.contains("已经买了") || text.contains("刚下单") || text.contains("收到了")) {
            result.setPurchaseStage("POST_PURCHASE");
        } else if (text.contains("准备买") || text.contains("打算") || text.contains("考虑")) {
            result.setPurchaseStage("CONSIDERATION");
        } else if (text.contains("想买") || text.contains("想要") || text.contains("求购")) {
            result.setPurchaseStage("INTENTION");
        } else if (text.contains("看看") || text.contains("了解一下")) {
            result.setPurchaseStage("AWARENESS");
        } else {
            result.setPurchaseStage("UNKNOWN");
        }
    }

    private void estimateBudget(String text, IntentAnalysisResult result) {
        Pattern budgetPattern = Pattern.compile("(\\d+)\\s*(元|块|千|万)");
        Matcher matcher = budgetPattern.matcher(text);
        if (matcher.find()) {
            try {
                double amount = Double.parseDouble(matcher.group(1));
                String unit = matcher.group(2);
                if ("千".equals(unit)) amount *= 1000;
                if ("万".equals(unit)) amount *= 10000;
                
                result.setEstimatedBudget(amount);
                if (amount >= 10000) {
                    result.setBudgetLevel("HIGH");
                } else if (amount >= 1000) {
                    result.setBudgetLevel("MEDIUM");
                } else {
                    result.setBudgetLevel("LOW");
                }
            } catch (NumberFormatException e) {
                log.warn("预算解析失败: {}", text);
            }
        }
    }

    private void analyzeUserAuthority(SocialComment comment, IntentAnalysisResult result) {
        int authorityScore = 0;
        
        if (comment.getUserVerified() != null && comment.getUserVerified()) {
            authorityScore += 20;
        }
        
        if (comment.getUserFollowerCount() != null) {
            if (comment.getUserFollowerCount() > 10000) authorityScore += 15;
            else if (comment.getUserFollowerCount() > 1000) authorityScore += 10;
            else if (comment.getUserFollowerCount() > 100) authorityScore += 5;
        }
        
        if (comment.getLikeCount() != null && comment.getLikeCount() > 10) {
            authorityScore += 5;
        }
        
        if (comment.getCommentText() != null && comment.getCommentText().length() > 50) {
            authorityScore += 5;
        }
        
        result.setUserAuthorityScore(authorityScore);
    }

    private IntentRecognitionResponse callAiDeepAnalysis(SocialComment comment, String text) {
        try {
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("你是一个专业的销售线索分析师。请分析以下社交媒体评论的用户购买意向。\n\n");
            promptBuilder.append("评论内容：").append(text).append("\n\n");
            promptBuilder.append("平台：").append(comment.getPlatformCode()).append("\n");
            promptBuilder.append("用户粉丝数：").append(comment.getUserFollowerCount() != null ? comment.getUserFollowerCount() : "未知").append("\n");
            promptBuilder.append("点赞数：").append(comment.getLikeCount() != null ? comment.getLikeCount() : 0).append("\n\n");
            promptBuilder.append("请以JSON格式返回分析结果，包含以下字段：\n");
            promptBuilder.append("- intent_score: 意向评分(0-100)\n");
            promptBuilder.append("- confidence: 置信度(0.0-1.0)\n");
            promptBuilder.append("- tags: 意向标签数组(如[\"高意向\", \"有预算\", \"急需\"])\n");
            promptBuilder.append("- summary: 一句话总结用户意图\n");
            promptBuilder.append("- suggested_action: 建议的跟进动作(如\"立即联系\",\"发送产品资料\",\"添加到待跟进\")\n");
            promptBuilder.append("- recommended_template: 推荐使用的私信模板ID(1-5)\n");

            TextGenerationRequest request = TextGenerationRequest.builder()
                    .userId("system")
                    .prompt(promptBuilder.toString())
                    .systemPrompt("你是专业的销售线索分析AI助手，必须返回有效的JSON格式结果")
                    .contentType("intent_analysis")
                    .temperature(0.3)
                    .maxLength(500)
                    .build();

            var response = aiCoreService.generateContent(request);
            
            String aiContent = response.getContents().get(0).getText();
            
            JSONObject aiResult = JSON.parseObject(extractJsonFromResponse(aiContent));
            
            return IntentRecognitionResponse.builder()
                    .intentScore(aiResult.getIntValue("intent_score"))
                    .confidence(aiResult.getDouble("confidence"))
                    .tags(parseStringArray(aiResult.getJSONArray("tags")))
                    .summary(aiResult.getString("summary"))
                    .suggestedAction(aiResult.getString("suggested_action"))
                    .recommendedTemplate(aiResult.getIntValue("recommended_template"))
                    .build();
                    
        } catch (Exception e) {
            log.error("AI深度分析调用失败: {}", e.getMessage(), e);
            return IntentRecognitionResponse.builder()
                    .intentScore(calculateKeywordScore(text))
                    .confidence(0.6)
                    .tags(Arrays.asList("AI分析失败，使用规则匹配"))
                    .summary("基于关键词分析的初步判断")
                    .suggestedAction("人工审核")
                    .recommendedTemplate(1)
                    .build();
        }
    }

    private int calculateFinalScore(IntentAnalysisResult result) {
        int score = result.getKeywordScore();

        if (result.isHasPhoneContact()) score += 15;
        if (result.isHasWechatContact()) score += 12;
        if (result.isWantsDirectContact()) score += 8;
        if (result.isHasHighUrgency()) score += 10;
        if ("HIGH".equals(result.getBudgetLevel())) score += 10;
        if ("MEDIUM".equals(result.getBudgetLevel())) score += 5;
        if (result.getCompetitorMentioned() != null) score += 8;
        if ("INTENTION".equals(result.getPurchaseStage())) score += 8;
        if ("CONSIDERATION".equals(result.getPurchaseStage())) score += 5;
        if (result.getUserAuthorityScore() >= 30) score += 5;

        if (result.getAiScore() != null && result.getAiScore() > 0) {
            score = (int)(score * 0.4 + result.getAiScore() * 0.6);
        }

        return Math.max(0, Math.min(100, score));
    }

    private String determineLevel(int score) {
        if (score >= 85) return "A";
        if (score >= 70) return "B";
        if (score >= 55) return "C";
        if (score >= 40) return "D";
        return "E";
    }

    private void updateCommentWithResult(SocialComment comment, IntentAnalysisResult result) {
        comment.setAiIntentScore(result.getFinalScore());
        comment.setAiIntentLevel(result.getLevel());
        comment.setAiIntentTags(JSON.toJSONString(result.getAllTags()));
        comment.setAiAnalysisResult(JSON.toJSONString(result));
        comment.setIsHighIntent(result.isHighIntent());

        if (result.getExtractedPhone() != null) {
            comment.setExtractedPhone(result.getExtractedPhone());
            comment.setHasPhoneContact(true);
        }
        if (result.getExtractedWechat() != null) {
            comment.setExtractedWechat(result.getExtractedWechat());
            comment.setHasWechatContact(true);
        }
    }

    private IntentAnalysisResult createEmptyResult(SocialComment comment) {
        IntentAnalysisResult result = new IntentAnalysisResult();
        result.setFinalScore(0);
        result.setLevel("E");
        result.setHighIntent(false);
        return result;
    }

    private boolean containsEmoji(String text) {
        return text.chars().anyMatch(c -> c > 0x2000);
    }

    private boolean hasMultipleQuestions(String text) {
        return text.chars().filter(c -> c == '？' || c == '?').count() >= 2;
    }

    private List<String> parseStringArray(JSONArray array) {
        if (array == null) return new ArrayList<>();
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            list.add(array.getString(i));
        }
        return list;
    }

    private String extractJsonFromResponse(String response) {
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return "{}";
    }

    public static class IntentAnalysisResult {
        private int keywordScore;
        private Integer aiScore;
        private Double aiConfidence;
        private List<String> aiTags;
        private String aiSummary;
        private String aiSuggestedAction;
        private Integer aiRecommendedTemplate;

        private int finalScore;
        private String level;
        private boolean isHighIntent;

        private String extractedPhone;
        private String extractedWechat;
        private boolean hasPhoneContact;
        private boolean hasWechatContact;
        private boolean wantsDirectContact;

        private List<String> urgencySignals;
        private boolean hasHighUrgency;
        private List<String> painPoints;
        private String competitorMentioned;
        private String purchaseStage;
        private Double estimatedBudget;
        private String budgetLevel;
        private int userAuthorityScore;

        public IntentAnalysisResult() {
            this.urgencySignals = new ArrayList<>();
            this.painPoints = new ArrayList<>();
        }

        public int getKeywordScore() { return keywordScore; }
        public void setKeywordScore(int keywordScore) { this.keywordScore = keywordScore; }
        public Integer getAiScore() { return aiScore; }
        public void setAiScore(Integer aiScore) { this.aiScore = aiScore; }
        public Double getAiConfidence() { return aiConfidence; }
        public void setAiConfidence(Double aiConfidence) { this.aiConfidence = aiConfidence; }
        public List<String> getAiTags() { return aiTags; }
        public void setAiTags(List<String> aiTags) { this.aiTags = aiTags; }
        public String getAiSummary() { return aiSummary; }
        public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }
        public String getAiSuggestedAction() { return aiSuggestedAction; }
        public void setAiSuggestedAction(String aiSuggestedAction) { this.aiSuggestedAction = aiSuggestedAction; }
        public Integer getAiRecommendedTemplate() { return aiRecommendedTemplate; }
        public void setAiRecommendedTemplate(Integer aiRecommendedTemplate) { this.aiRecommendedTemplate = aiRecommendedTemplate; }
        public int getFinalScore() { return finalScore; }
        public void setFinalScore(int finalScore) { this.finalScore = finalScore; }
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public boolean isHighIntent() { return isHighIntent; }
        public void setHighIntent(boolean isHighIntent) { this.isHighIntent = isHighIntent; }
        public String getExtractedPhone() { return extractedPhone; }
        public void setExtractedPhone(String extractedPhone) { this.extractedPhone = extractedPhone; }
        public String getExtractedWechat() { return extractedWechat; }
        public void setExtractedWechat(String extractedWechat) { this.extractedWechat = extractedWechat; }
        public boolean isHasPhoneContact() { return hasPhoneContact; }
        public void setHasPhoneContact(boolean hasPhoneContact) { this.hasPhoneContact = hasPhoneContact; }
        public boolean isHasWechatContact() { return hasWechatContact; }
        public void setHasWechatContact(boolean hasWechatContact) { this.hasWechatContact = hasWechatContact; }
        public boolean isWantsDirectContact() { return wantsDirectContact; }
        public void setWantsDirectContact(boolean wantsDirectContact) { this.wantsDirectContact = wantsDirectContact; }
        public List<String> getUrgencySignals() { return urgencySignals; }
        public void setUrgencySignals(List<String> urgencySignals) { this.urgencySignals = urgencySignals; }
        public boolean isHasHighUrgency() { return hasHighUrgency; }
        public void setHasHighUrgency(boolean hasHighUrgency) { this.hasHighUrgency = hasHighUrgency; }
        public List<String> getPainPoints() { return painPoints; }
        public void setPainPoints(List<String> painPoints) { this.painPoints = painPoints; }
        public String getCompetitorMentioned() { return competitorMentioned; }
        public void setCompetitorMentioned(String competitorMentioned) { this.competitorMentioned = competitorMentioned; }
        public String getPurchaseStage() { return purchaseStage; }
        public void setPurchaseStage(String purchaseStage) { this.purchaseStage = purchaseStage; }
        public Double getEstimatedBudget() { return estimatedBudget; }
        public void setEstimatedBudget(Double estimatedBudget) { this.estimatedBudget = estimatedBudget; }
        public String getBudgetLevel() { return budgetLevel; }
        public void setBudgetLevel(String budgetLevel) { this.budgetLevel = budgetLevel; }
        public int getUserAuthorityScore() { return userAuthorityScore; }
        public void setUserAuthorityScore(int userAuthorityScore) { this.userAuthorityScore = userAuthorityScore; }

        public void addUrgencySignal(String signal) {
            this.urgencySignals.add(signal);
        }

        public void addPainPoint(String painPoint) {
            this.painPoints.add(painPoint);
        }

        public List<String> getAllTags() {
            List<String> allTags = new ArrayList<>();
            if (aiTags != null) allTags.addAll(aiTags);
            if (!urgencySignals.isEmpty()) allTags.add("紧急");
            if (hasPhoneContact) allTags.add("留电话");
            if (hasWechatContact) allTags.add("留微信");
            if (competitorMentioned != null) allTags.add("提及竞品");
            if (hasHighUrgency) allTags.add("高 urgency");
            return allTags;
        }
    }

    public static class IntentRecognitionResponse {
        private int intentScore;
        private double confidence;
        private List<String> tags;
        private String summary;
        private String suggestedAction;
        private int recommendedTemplate;

        public IntentRecognitionResponse() {}

        public int getIntentScore() { return intentScore; }
        public void setIntentScore(int intentScore) { this.intentScore = intentScore; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public String getSuggestedAction() { return suggestedAction; }
        public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }
        public int getRecommendedTemplate() { return recommendedTemplate; }
        public void setRecommendedTemplate(int recommendedTemplate) { this.recommendedTemplate = recommendedTemplate; }

        public static class Builder {
            private IntentRecognitionResponse resp = new IntentRecognitionResponse();
            public Builder intentScore(int score) { resp.intentScore = score; return this; }
            public Builder confidence(double conf) { resp.confidence = conf; return this; }
            public Builder tags(List<String> t) { resp.tags = t; return this; }
            public Builder summary(String s) { resp.summary = s; return this; }
            public Builder suggestedAction(String a) { resp.suggestedAction = a; return this; }
            public Builder recommendedTemplate(int t) { resp.recommendedTemplate = t; return this; }
            public IntentRecognitionResponse build() { return resp; }
        }

        public static Builder builder() { return new Builder(); }
    }

    @Override
    public void destroy() throws Exception {
        log.info("开始关闭AiIntentAnalysisV2Service线程池...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("线程池未能在60秒内优雅关闭，强制终止");
                executorService.shutdownNow();
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.error("线程池未能正常关闭");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
            log.error("等待线程池关闭时被中断");
        }
        log.info("AiIntentAnalysisV2Service线程池已关闭");
    }
}
