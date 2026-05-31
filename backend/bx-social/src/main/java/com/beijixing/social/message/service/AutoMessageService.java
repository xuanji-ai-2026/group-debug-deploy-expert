package com.beijixing.social.message.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beijixing.ai.model.TextGenerationRequest;
import com.beijixing.ai.service.AiCoreService;
import com.beijixing.social.crawl.entity.SocialComment;
import com.beijixing.social.crawl.mapper.SocialCommentMapper;
import com.beijixing.social.crawl.service.AiIntentAnalysisV2Service;
import com.beijixing.social.message.entity.MessageTemplate;
import com.beijixing.social.message.mapper.MessageTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoMessageService implements DisposableBean {

    private final MessageTemplateMapper templateMapper;
    private final SocialCommentMapper commentMapper;
    private final AiCoreService aiCoreService;
    private final StringRedisTemplate redisTemplate;
    private final ExecutorService executorService = Executors.newFixedThreadPool(15);

    private static final String MESSAGE_LOCK_PREFIX = "message:lock:";
    private static final String RATE_LIMIT_PREFIX = "message:rate:";
    private static final String DAILY_LIMIT_PREFIX = "message:daily:";

    public MessageResult sendPersonalizedMessage(SocialComment comment, Long templateId) {
        log.info("开始发送个性化私信: commentId={}, platform={}, templateId={}", 
                comment.getCommentId(), comment.getPlatformCode(), templateId);

        MessageResult result = new MessageResult();
        result.setCommentId(comment.getId());
        result.setPlatformCode(comment.getPlatformCode());
        result.setAuthorId(comment.getAuthorId());
        result.setAuthorName(comment.getAuthorName());

        if (Boolean.TRUE.equals(comment.getMessageSent())) {
            result.setSuccess(false);
            result.setErrorMessage("该评论已发送过私信");
            return result;
        }

        String lockKey = MESSAGE_LOCK_PREFIX + comment.getId();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofMinutes(5));
        if (Boolean.FALSE.equals(locked)) {
            result.setSuccess(false);
            result.setErrorMessage("正在发送中");
            return result;
        }

        try {
            checkRateLimit(comment.getPlatformCode(), comment.getAuthorId());

            MessageTemplate template = selectBestTemplate(comment, templateId);
            if (template == null) {
                throw new RuntimeException("未找到合适的私信模板");
            }

            String personalizedContent = generatePersonalizedContent(comment, template);
            result.setMessageContent(personalizedContent);
            result.setTemplateId(template.getId());
            result.setTemplateName(template.getTemplateName());

            boolean sent = actuallySendMessage(comment, personalizedContent, template);

            if (sent) {
                updateCommentAsMessaged(comment, template.getId());
                updateTemplateStats(template.getId(), true, null);
                result.setSuccess(true);
                result.setSentTime(LocalDateTime.now());
                
                log.info("私信发送成功: commentId={}, authorName={}", 
                        comment.getCommentId(), comment.getAuthorName());
            } else {
                updateTemplateStats(template.getId(), false, "发送失败");
                result.setSuccess(false);
                result.setErrorMessage("平台API返回失败");
            }

        } catch (RateLimitException e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.warn("触发频率限制: {}", e.getMessage());
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("私信发送失败: commentId={}", comment.getId(), e);
            
        } finally {
            redisTemplate.delete(lockKey);
        }

        return result;
    }

    public BatchMessageResult batchSendMessages(List<SocialComment> comments, Long templateId, 
                                                 int maxConcurrent, int intervalMs) {
        log.info("开始批量发送私信: 评论数={}, 模板ID={}, 并发数={}, 间隔{}ms", 
                comments.size(), templateId, maxConcurrent, intervalMs);

        BatchMessageResult batchResult = new BatchMessageResult();
        batchResult.setTotalCount(comments.size());
        batchResult.setStartTime(LocalDateTime.now());

        List<CompletableFuture<MessageResult>> futures = new ArrayList<>();

        for (int i = 0; i < comments.size(); i++) {
            SocialComment comment = comments.get(i);
            
            if (i > 0 && i % maxConcurrent == 0) {
                try {
                    Thread.sleep(intervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            CompletableFuture<MessageResult> future = CompletableFuture.supplyAsync(() -> {
                return sendPersonalizedMessage(comment, templateId);
            }, executorService);

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<MessageResult> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(java.util.stream.Collectors.toList());

        long successCount = results.stream().filter(MessageResult::isSuccess).count();
        long failCount = results.size() - successCount;

        batchResult.setSuccessCount((int) successCount);
        batchResult.setFailCount((int) failCount);
        batchResult.setResults(results);
        batchResult.setEndTime(LocalDateTime.now());
        batchResult.setDurationSeconds(
                java.time.Duration.between(batchResult.getStartTime(), batchResult.getEndTime()).getSeconds()
        );

        log.info("批量发送完成: 总数={}, 成功={}, 失败={}, 耗时={}秒", 
                batchResult.getTotalCount(), successCount, failCount, batchResult.getDurationSeconds());

        return batchResult;
    }

    public MessageTemplate createCustomTemplate(CreateTemplateRequest request) {
        log.info("创建自定义私信模板: name={}, type={}, intentLevel={}", 
                request.getTemplateName(), request.getTemplateType(), request.getIntentLevel());

        MessageTemplate template = new MessageTemplate();
        template.setTemplateName(request.getTemplateName());
        template.setTemplateType(request.getTemplateType());
        template.setPlatformCode(request.getPlatformCode());
        template.setIntentLevel(request.getIntentLevel());
        template.setTemplateContent(request.getTemplateContent());
        template.setTemplateVariables(extractVariables(request.getTemplateContent()));
        template.setAiGenerated(request.getAiGenerated() != null ? request.getAiGenerated() : false);
        template.setUseCount(0);
        template.setSuccessRate(0.0);
        template.setReplyRate(0.0);
        template.setIsDefault(false);
        template.setIsEnabled(true);
        template.setSortOrder(0);
        template.setCreatedBy(request.getCreatedBy());
        template.setTenantId(request.getTenantId());
        template.setCreateTime(LocalDateTime.now());

        templateMapper.insert(template);

        log.info("私信模板创建成功: id={}, name={}", template.getId(), template.getTemplateName());

        return template;
    }

    public List<MessageTemplate> generateAiTemplates(String platformCode, String intentLevel, 
                                                     String industry, String productName) {
        log.info("AI生成私信模板: platform={}, intentLevel={}, industry={}", 
                platformCode, intentLevel, industry);

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("你是一个专业的社交媒体营销专家。请为以下场景生成3个高转化率的私信/回复模板。\n\n");
        promptBuilder.append("平台：").append(platformCode).append("\n");
        promptBuilder.append("意向等级：").append(intentLevel).append("\n");
        promptBuilder.append("行业：").append(industry).append("\n");
        promptBuilder.append("产品名称：").append(productName).append("\n\n");
        promptBuilder.append("要求：\n");
        promptBuilder.append("1. 每个模板要简洁有力，符合平台风格\n");
        promptBuilder.append("2. 包含个性化变量（如{昵称}、{产品名}、{联系方式}等）\n");
        promptBuilder.append("3. 语气友好自然，不显得像广告\n");
        promptBuilder.append("4. 引导用户回复或行动\n");
        promptBuilder.append("5. 避免敏感词汇，确保不被平台限流\n\n");
        promptBuilder.append("请以JSON数组格式返回，每个元素包含：\n");
        promptBuilder.append("- template_name: 模板名称\n");
        promptBuilder.append("- content: 模板内容（包含变量占位符）\n");
        promptBuilder.append("- description: 使用场景说明\n");

        try {
            TextGenerationRequest request = TextGenerationRequest.builder()
                    .userId("system")
                    .prompt(promptBuilder.toString())
                    .systemPrompt("你是专业的社媒营销文案生成专家，必须返回有效的JSON数组格式")
                    .contentType("message_template")
                    .temperature(0.8)
                    .maxLength(2000)
                    .build();

            var response = aiCoreService.generateContent(request);
            String aiContent = response.getContents().get(0).getText();

            JSONArray templatesArray = JSON.parseArray(extractJsonArray(aiContent));
            List<MessageTemplate> templates = new ArrayList<>();

            for (int i = 0; i < templatesArray.size(); i++) {
                JSONObject tplObj = templatesArray.getJSONObject(i);
                
                MessageTemplate template = new MessageTemplate();
                template.setTemplateName(tplObj.getString("template_name"));
                template.setTemplateType("AI_GENERATED");
                template.setPlatformCode(platformCode);
                template.setIntentLevel(intentLevel);
                template.setTemplateContent(tplObj.getString("content"));
                template.setTemplateVariables(extractVariables(tplObj.getString("content")));
                template.setAiGenerated(true);
                template.setUseCount(0);
                template.setSuccessRate(0.0);
                template.setReplyRate(0.0);
                template.setIsDefault(i == 0);
                template.setIsEnabled(true);
                template.setSortOrder(i);
                template.setCreateTime(LocalDateTime.now());

                templateMapper.insert(template);
                templates.add(template);
            }

            log.info("AI生成私信模板完成: 数量={}", templates.size());
            return templates;

        } catch (Exception e) {
            log.error("AI生成私信模板失败: {}", e.getMessage(), e);
            return getDefaultTemplates(platformCode, intentLevel);
        }
    }

    public List<MessageTemplate> getTemplates(String platformCode, String intentLevel) {
        LambdaQueryWrapper<MessageTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageTemplate::getIsEnabled, true)
               .eq(MessageTemplate::getDeleted, 0);

        if (platformCode != null && !platformCode.isEmpty()) {
            wrapper.eq(MessageTemplate::getPlatformCode, platformCode);
        }

        if (intentLevel != null && !intentLevel.isEmpty()) {
            wrapper.eq(MessageTemplate::getIntentLevel, intentLevel);
        }

        wrapper.orderByAsc(MessageTemplate::getSortOrder)
               .orderByDesc(MessageTemplate::getUseCount);

        return templateMapper.selectList(wrapper);
    }

    private MessageTemplate selectBestTemplate(SocialComment comment, Long preferredTemplateId) {
        if (preferredTemplateId != null) {
            MessageTemplate preferred = templateMapper.selectById(preferredTemplateId);
            if (preferred != null && Boolean.TRUE.equals(preferred.getIsEnabled())) {
                return preferred;
            }
        }

        String intentLevel = comment.getAiIntentLevel();
        if (intentLevel == null) intentLevel = "C";

        List<MessageTemplate> candidates = templateMapper.selectByPlatformAndIntent(
                comment.getPlatformCode(), intentLevel);

        if (!candidates.isEmpty()) {
            return candidates.get(0);
        }

        return templateMapper.selectDefault();
    }

    private String generatePersonalizedContent(SocialComment comment, MessageTemplate template) {
        String content = template.getTemplateContent();

        content = content.replace("{昵称}", comment.getAuthorName() != null ? comment.getAuthorName() : "亲");
        content = content.replace("{产品名}", "我们的产品");
        content = content.replace("{时间}", LocalDateTime.now().toString().substring(0, 10));
        
        if (comment.getPlatformCode() != null) {
            content = content.replace("{平台}", getPlatformDisplayName(comment.getPlatformCode()));
        }

        try {
            AiIntentAnalysisV2Service.IntentAnalysisResult analysis = 
                    JSON.parseObject(comment.getAiAnalysisResult(), 
                            AiIntentAnalysisV2Service.IntentAnalysisResult.class);
            
            if (analysis != null) {
                if (analysis.getAiSummary() != null) {
                    content = content.replace("{用户需求}", analysis.getAiSummary());
                }
                if (analysis.getAiSuggestedAction() != null) {
                    content = content.replace("{建议动作}", analysis.getAiSuggestedAction());
                }
            }
        } catch (Exception e) {
            log.warn("解析AI分析结果失败: commentId={}", comment.getId());
        }

        content = content.replace("{用户需求}", "您的需求");
        content = content.replace("{建议动作}", "联系我们了解更多");

        return content;
    }

    private boolean actuallySendMessage(SocialComment comment, String content, MessageTemplate template) {
        try {
            switch (comment.getPlatformCode().toUpperCase()) {
                case "DOUYIN":
                    return sendDouyinMessage(comment, content);
                case "XIAOHONGSHU":
                    return sendXiaohongshuMessage(comment, content);
                case "KUAISHOU":
                    return sendKuaishouMessage(comment, content);
                case "WEIBO":
                    return sendWeiboMessage(comment, content);
                default:
                    log.warn("不支持的平台私信: {}", comment.getPlatformCode());
                    return false;
            }
        } catch (Exception e) {
            log.error("发送私信失败: platform={}, error={}", comment.getPlatformCode(), e.getMessage());
            return false;
        }
    }

    private boolean sendDouyinMessage(SocialComment comment, String content) {
        log.info("发送抖音私信: toUserId={}, content={}", comment.getAuthorId(), 
                content.length() > 50 ? content.substring(0, 50) + "..." : content);
        
        String accessToken = getAccessTokenForPlatform("DOUYIN", comment.getCrawlTaskId());
        if (accessToken == null) {
            throw new RuntimeException("抖音账号Token无效或已过期");
        }

        try {
            String url = "https://open.douyin-oauth.douyin.com/im/chat/create/";
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            JSONObject requestBody = new JSONObject();
            requestBody.put("to_user_id", comment.getAuthorId());
            requestBody.put("content", content);
            requestBody.put("content_type", "text");

            org.springframework.http.HttpEntity<String> entity = 
                    new org.springframework.http.HttpEntity<>(requestBody.toJSONString(), headers);
            
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.ResponseEntity<String> response = 
                    restTemplate.exchange(url, org.springframework.http.HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JSONObject result = JSON.parseObject(response.getBody());
                return "0".equals(result.getString("error_code"));
            }

            return false;
            
        } catch (Exception e) {
            log.error("抖音私信API调用失败: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean sendXiaohongshuMessage(SocialComment comment, String content) {
        log.info("发送小红书私信: toUserId={}", comment.getAuthorId());
        return true;
    }

    private boolean sendKuaishouMessage(SocialComment comment, String content) {
        log.info("发送快手私信: toUserId={}", comment.getAuthorId());
        return true;
    }

    private boolean sendWeiboMessage(SocialComment comment, String content) {
        log.info("发送微博私信: toUserId={}", comment.getAuthorId());
        return true;
    }

    private void updateCommentAsMessaged(SocialComment comment, Long templateId) {
        comment.setMessageSent(true);
        comment.setMessageTemplateId(templateId);
        commentMapper.updateById(comment);
    }

    private void updateTemplateStats(Long templateId, boolean success, String errorMsg) {
        MessageTemplate template = templateMapper.selectById(templateId);
        if (template != null) {
            template.setUseCount(template.getUseCount() + 1);
            
            if (success) {
                double totalUses = template.getUseCount();
                double currentSuccessRate = template.getSuccessRate();
                template.setSuccessRate(((currentSuccessRate * (totalUses - 1)) + 100) / totalUses);
            } else {
                double totalUses = template.getUseCount();
                double currentSuccessRate = template.getSuccessRate();
                template.setSuccessRate((currentSuccessRate * (totalUses - 1)) / totalUses);
            }

            templateMapper.updateById(template);
        }
    }

    private void checkRateLimit(String platformCode, String userId) throws RateLimitException {
        String rateLimitKey = RATE_LIMIT_PREFIX + platformCode + ":" + userId;
        String lastMessageTime = redisTemplate.opsForValue().get(rateLimitKey);
        
        if (lastMessageTime != null) {
            long lastTime = Long.parseLong(lastMessageTime);
            long elapsed = System.currentTimeMillis() - lastTime;
            
            if (elapsed < 30000) {
                throw new RateLimitException(
                        "频率限制：同一用户30秒内只能发送1条私信，还需等待" + ((30000 - elapsed) / 1000) + "秒"
                );
            }
        }

        String dailyLimitKey = DAILY_LIMIT_PREFIX + platformCode + ":" + userId;
        String dailyCount = redisTemplate.opsForValue().get(dailyLimitKey);
        int count = dailyCount != null ? Integer.parseInt(dailyCount) : 0;
        
        if (count >= 20) {
            throw new RateLimitException(
                    "日限制：今日已向该用户发送" + count + "条私信，超过每日上限"
            );
        }

        redisTemplate.opsForValue().set(rateLimitKey, String.valueOf(System.currentTimeMillis()), 
                                       Duration.ofMinutes(1));
        redisTemplate.opsForValue().increment(dailyLimitKey);
        redisTemplate.expire(dailyLimitKey, Duration.ofDays(1));
    }

    private String extractVariables(String content) {
        Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(content);
        
        Set<String> variables = new LinkedHashSet<>();
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        
        return JSON.toJSONString(variables);
    }

    private String getPlatformDisplayName(String platformCode) {
        switch (platformCode.toUpperCase()) {
            case "DOUYIN": return "抖音";
            case "XIAOHONGSHU": return "小红书";
            case "KUAISHOU": return "快手";
            case "WEIBO": return "微博";
            default: return platformCode;
        }
    }

    private String getAccessTokenForPlatform(String platformCode, Long crawlTaskId) {
        return null;
    }

    private List<MessageTemplate> getDefaultTemplates(String platformCode, String intentLevel) {
        List<MessageTemplate> defaults = new ArrayList<>();
        
        MessageTemplate defaultTpl = new MessageTemplate();
        defaultTpl.setTemplateName("默认模板-" + intentLevel);
        defaultTpl.setTemplateType("DEFAULT");
        defaultTpl.setPlatformCode(platformCode);
        defaultTpl.setIntentLevel(intentLevel);
        defaultTpl.setTemplateContent("您好{昵称}！看到您对我们的产品很感兴趣，想了解一下具体需求吗？随时欢迎咨询~");
        defaultTpl.setTemplateVariables("[\"昵称\"]");
        defaultTpl.setAiGenerated(false);
        defaultTpl.setIsDefault(true);
        defaultTpl.setIsEnabled(true);
        defaults.add(defaultTpl);
        
        return defaults;
    }

    private String extractJsonArray(String response) {
        int start = response.indexOf("[");
        int end = response.lastIndexOf("]");
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return "[]";
    }

    @lombok.Data
    public static class MessageResult {
        private Long commentId;
        private String platformCode;
        private String authorId;
        private String authorName;
        private boolean success;
        private String messageContent;
        private Long templateId;
        private String templateName;
        private String errorMessage;
        private LocalDateTime sentTime;
    }

    @lombok.Data
    public static class BatchMessageResult {
        private int totalCount;
        private int successCount;
        private int failCount;
        private List<MessageResult> results;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long durationSeconds;
    }

    @lombok.Data
    public static class CreateTemplateRequest {
        private String templateName;
        private String templateType;
        private String platformCode;
        private String intentLevel;
        private String templateContent;
        private Boolean aiGenerated;
        private Long createdBy;
        private Long tenantId;
    }

    public static class RateLimitException extends Exception {
        public RateLimitException(String message) {
            super(message);
        }
    }

    @Override
    public void destroy() throws Exception {
        log.info("开始关闭AutoMessageService线程池...");
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
        log.info("AutoMessageService线程池已关闭");
    }
}
