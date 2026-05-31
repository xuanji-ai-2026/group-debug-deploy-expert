package com.beijixing.social.crawl.service;

import com.beijixing.social.crawl.adapter.PlatformCommentAdapter;
import com.beijixing.social.crawl.entity.CrawlTask;
import com.beijixing.social.crawl.mapper.CrawlTaskMapper;
import com.beijixing.social.crawl.model.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一评论抓取服务 (Facade Pattern)
 *
 * 核心职责:
 * 1. **统一入口**: 提供平台无关的评论抓取API
 * 2. **适配器路由**: 根据目标URL/ID自动选择合适的平台适配器
 * 3. **自定义池集成**: 与CustomTargetPoolService深度整合
 * 4. **批量处理**: 支持多目标并行抓取
 * 5. **结果聚合**: 统一返回格式，便于前端展示和后续AI分析
 *
 * 使用示例:
 * <pre>
 * // 方式1: 通过URL抓取
 * CommentCrawlResult result = service.crawlFromUrl("https://v.douyin.com/i2eABC123/", userId);
 *
 * // 方式2: 指定平台+ID抓取
 * CommentCrawlResult result = service.crawlComments("DOUYIN", "7234567890123456789", userId);
 *
 * // 方式3: 批量抓取（自定义池）
 * BatchCrawlResult batchResult = service.batchCrawl(targets, userId);
 * </pre>
 *
 * @author 北极星AI团队
 * @version 2.0 (2026-05-20)
 */
@Service
@Slf4j
public class CommentCrawlService {

    @Autowired
    private CustomTargetPoolService targetPoolService;

    @Autowired
    private CrawlTaskMapper crawlTaskMapper;

    private final Map<String, PlatformCommentAdapter> adapterMap = new ConcurrentHashMap<>();

    /**
     * 注册平台适配器 (Spring自动注入)
     */
    @Autowired(required = false)
    public void setAdapters(List<PlatformCommentAdapter> adapters) {
        if (adapters != null) {
            for (PlatformCommentAdapter adapter : adapters) {
                adapterMap.put(adapter.getPlatformCode().toUpperCase(), adapter);
                log.info("📌 注册平台适配器: {} - {}",
                        adapter.getPlatformCode(), adapter.getPlatformName());
            }
        }
        log.info("✅ 已注册{}个平台适配器", adapterMap.size());
    }

    /**
     * 从URL抓取评论 (核心方法)
     *
     * 流程:
     * 1. URL解析 → 识别平台 + 提取真实ID
     * 2. 添加到自定义目标池 (去重)
     * 3. 获取用户Token (从数据库/Redis)
     * 4. 调用对应平台的适配器进行抓取
     * 5. 更新目标状态和结果
     * 6. 返回统一格式的结果
     *
     * @param url 目标URL (支持抖音/小红书/快手/微博/B站等)
     * @param userId 用户ID (用于获取Token)
     * @param options 抓取配置 (可选, 默认使用标准配置)
     * @return 评论抓取结果
     */
    public CommentCrawlResult crawlFromUrl(String url, String userId, CrawlOptions options) {
        log.info("\n🚀 [评论抓取] 开始处理URL: {} | 用户: {}", url, userId);

        long startTime = System.currentTimeMillis();

        try {
            if (options == null) {
                options = CrawlOptions.defaults();
            }

            // Step 1: 解析URL → 识别平台 + ID
            CustomTargetPoolService.ParsedTarget parsed = targetPoolService.parseAndResolve(url);
            if (parsed == null) {
                return createErrorResult(url, "UNSUPPORTED_URL",
                        "无法识别的URL格式。支持的平台:\n" +
                        "• 抖音: v.douyin.com / douyin.com/video/\n" +
                        "• 小红书: xhslink.com / xiaohongshu.com/explore/\n" +
                        "• 快手: v.kuaishou.com\n" +
                        "• 微博: weibo.com\n" +
                        "• B站: bilibili.com/video/BV");
            }

            log.info("  📍 平台: {} | 解析ID: {}", parsed.getPlatform(), parsed.getResolvedId());

            // Step 2: 添加到目标池
            CustomTargetPoolService.CrawlTarget target = targetPoolService.addTarget(url, userId);

            // Step 3: 更新状态为"抓取中"
            targetPoolService.updateTargetStatus(target.getTargetId(),
                    CustomTargetPoolService.TargetStatus.CRAWLING);

            // Step 4: 获取用户Token (模拟，实际应从数据库查询)
            String accessToken = getUserAccessToken(userId, parsed.getPlatform());

            if (accessToken == null) {
                if ("BILIBILI".equalsIgnoreCase(parsed.getPlatform())) {
                    log.info("B站游客模式抓取: platform={}, targetId={}", parsed.getPlatform(), parsed.getResolvedId());
                } else {
                    log.warn("⚠️ 平台 {} 无AccessToken，部分API可能无法访问，尝试公开端点: targetId={}",
                            parsed.getPlatform(), parsed.getResolvedId());
                }
            }

            PlatformCommentAdapter adapter = adapterMap.get(parsed.getPlatform().toUpperCase());
            if (adapter == null) {
                throw new RuntimeException("未找到平台适配器: " + parsed.getPlatform() +
                        "\n已注册的平台: " + String.join(", ", adapterMap.keySet()));
            }

            CommentCrawlResult result = adapter.crawlComments(
                    parsed.getResolvedId(),
                    accessToken,
                    options
            );

            // Step 6: 更新目标状态
            if (result.isSuccess()) {
                targetPoolService.updateTargetStatus(target.getTargetId(),
                        CustomTargetPoolService.TargetStatus.COMPLETED);
                result.setTargetTitle(result.getTargetUrl());  // 可选：获取标题
            } else {
                targetPoolService.updateTargetStatus(target.getTargetId(),
                        CustomTargetPoolService.TargetStatus.FAILED);
            }
            targetPoolService.updateTargetResult(target.getTargetId(), result);

            log.info("🎉 [评论抓取] 完成 | {}", result.getSummary());

            return result;

        } catch (Exception e) {
            log.error("❌ [评论抓取] 异常: {}", e.getMessage(), e);
            return createErrorResult(url, "CRAWL_EXCEPTION", e.getMessage());
        }
    }

    /**
     * 指定平台+ID抓取评论
     */
    public CommentCrawlResult crawlComments(String platform, String targetId,
                                           String userId, CrawlOptions options) {
        log.info("🚀 [评论抓取] 平台:{} | ID:{} | 用户:{}", platform, targetId, userId);

        try {
            platform = platform.toUpperCase();
            PlatformCommentAdapter adapter = adapterMap.get(platform);
            if (adapter == null) {
                return createErrorResult(platform + ":" + targetId,
                        "PLATFORM_NOT_FOUND",
                        "未找到平台适配器: " + platform);
            }

            String accessToken = getUserAccessToken(userId, platform);

            if (accessToken == null) {
                if ("BILIBILI".equalsIgnoreCase(platform)) {
                    log.info("B站游客模式抓取: platform={}, targetId={}", platform, targetId);
                } else {
                    log.warn("⚠️ 平台 {} 无AccessToken，部分API可能无法访问，尝试公开端点: targetId={}",
                            platform, targetId);
                }
            }

            CommentCrawlResult result = adapter.crawlComments(targetId, accessToken,
                    options != null ? options : CrawlOptions.defaults());

            log.info("🎉 [评论抓取] 完成 | {}", result.getSummary());
            return result;

        } catch (Exception e) {
            log.error("❌ [评论抓取] 异常: {}", e.getMessage(), e);
            return createErrorResult(platform + ":" + targetId, "EXCEPTION", e.getMessage());
        }
    }

    /**
     * 批量抓取多个目标 (并行执行)
     *
     * 支持混合输入:
     * - 不同平台的URL混合
     * - 同一平台的不同ID
     * - 自动去重和错误隔离
     */
    public BatchCrawlResult batchCrawl(List<String> targets, String userId,
                                      CrawlOptions options) {
        log.info("\n📦 [批量抓取] 开始 | 目标数: {} | 用户: {}", targets.size(), userId);

        BatchCrawlResult batchResult = new BatchCrawlResult();
        batchResult.setStartTime(LocalDateTime.now());

        List<CompletableFuture<CommentCrawlResult>> futures = new ArrayList<>();

        for (String target : targets) {
            CompletableFuture<CommentCrawlResult> future = CompletableFuture.supplyAsync(() -> {
                return crawlFromUrl(target, userId, options);
            });
            futures.add(future);
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        for (CompletableFuture<CommentCrawlResult> future : futures) {
            try {
                CommentCrawlResult result = future.get();
                batchResult.addResult(result);
            } catch (Exception e) {
                batchResult.addFailure(e.getMessage());
            }
        }

        batchResult.setEndTime(LocalDateTime.now());
        batchResult.calculateSummary();

        log.info("\n📊 [批量抓取] 完成统计:\n" +
                "  总目标数: {}\n" +
                "  成功数: {} ({:.1f}%)\n" +
                "  失败数: {} ({:.1f}%)\n" +
                "  总评论数: {}\n" +
                "  总耗时: {}ms\n" +
                "  平均耗时: {:.0f}ms/目标",
                batchResult.getTotalTargets(),
                batchResult.getSuccessCount(),
                batchResult.getTotalTargets() > 0 ?
                        (double) batchResult.getSuccessCount() / batchResult.getTotalTargets() * 100 : 0,
                batchResult.getFailureCount(),
                batchResult.getTotalTargets() > 0 ?
                        (double) batchResult.getFailureCount() / batchResult.getTotalTargets() * 100 : 0,
                batchResult.getTotalComments(),
                batchResult.getDurationMs(),
                batchResult.getTotalTargets() > 0 ?
                        (double) batchResult.getDurationMs() / batchResult.getTotalTargets() : 0);

        return batchResult;
    }

    /**
     * 从CSV内容批量导入并抓取
     */
    public BatchCrawlResult importAndCrawl(String csvContent, String delimiter,
                                          String userId, CrawlOptions options) {
        CustomTargetPoolService.BatchAddResult addResult =
                targetPoolService.importFromCsv(csvContent, delimiter, userId);

        if (addResult.getSuccessTargets().isEmpty()) {
            BatchCrawlResult errorResult = new BatchCrawlResult();
            errorResult.setErrorMessage("没有有效的目标可导入");
            return errorResult;
        }

        List<String> urls = new ArrayList<>();
        for (CustomTargetPoolService.CrawlTarget target : addResult.getSuccessTargets()) {
            urls.add(target.getOriginalInput());
        }

        return batchCrawl(urls, userId, options);
    }

    // ====== 辅助方法 ======

    /**
     * 获取用户的Access Token (实际实现应从数据库查询)
     */
    private String getUserAccessToken(String userId, String platform) {
        log.debug("从SocialAccount表/Redis获取用户AccessToken: userId={}, platform={}", userId, platform);

        if ("BILIBILI".equalsIgnoreCase(platform)) {
            log.info("B站支持游客模式，无需AccessToken: userId={}", userId);
            return null;
        }

        String token = resolveAccessTokenFromCache(userId, platform);
        if (token == null) {
            log.warn("⚠️ 未找到用户 {} 在平台 {} 的授权Token，将尝试无Token访问", userId, platform);
        }
        return token;
    }

    /**
     * 从缓存/数据库解析Access Token（降级方案）
     */
    private String resolveAccessTokenFromCache(String userId, String platform) {
        log.warn("⚠️ 未配置平台 {} 的真实AccessToken，请通过管理后台配置用户 {} 的授权信息。" +
                "当前返回null，部分平台可能无法正常访问。", platform, userId);
        return null;
    }

    /**
     * 创建错误结果
     */
    private CommentCrawlResult createErrorResult(String target, String errorCode,
                                                String errorMessage) {
        return CommentCrawlResult.builder()
                .taskId(UUID.randomUUID().toString())
                .targetUrl(target)
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .durationMs(0)
                .build();
    }

    // ====== 查询接口 ======

    /**
     * 获取所有已注册的平台列表
     */
    public List<PlatformInfo> getSupportedPlatforms() {
        List<PlatformInfo> platforms = new ArrayList<>();
        for (Map.Entry<String, PlatformCommentAdapter> entry : adapterMap.entrySet()) {
            PlatformCommentAdapter adapter = entry.getValue();
            platforms.add(PlatformInfo.builder()
                    .code(adapter.getPlatformCode())
                    .name(adapter.getPlatformName())
                    .rateLimitInfo(adapter.getRateLimitInfo())
                    .supportedFeatures(adapter.getSupportedFeatures())
                    .build());
        }
        return platforms;
    }

    /**
     * 获取用户的所有抓取历史
     */
    public List<CustomTargetPoolService.CrawlTarget> getCrawlHistory(String userId) {
        return targetPoolService.getUserTargets(userId);
    }

    /**
     * 清空自定义目标池
     */
    public void clearUserPool(String userId) {
        List<CustomTargetPoolService.CrawlTarget> targets = targetPoolService.getUserTargets(userId);
        for (CustomTargetPoolService.CrawlTarget target : targets) {
            targetPoolService.removeTarget(target.getTargetId(), userId);
        }
        log.info("🗑️ 已清空用户 {} 的目标池", userId);
    }

    /**
     * 创建抓取任务
     */
    public CrawlTask createCrawlTask(CrawlTask task) {
        task.setCreateTime(LocalDateTime.now());
        task.setStatus(0);
        crawlTaskMapper.insert(task);
        log.info("✅ 创建抓取任务: id={}", task.getId());
        return task;
    }

    /**
     * 根据ID获取抓取任务
     */
    public CrawlTask getTaskById(Long taskId) {
        return crawlTaskMapper.selectById(taskId);
    }

    public List<CrawlTask> getTaskList(String platformCode, String status, int page, int size) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CrawlTask> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (platformCode != null && !platformCode.isEmpty()) {
            wrapper.eq(CrawlTask::getPlatformCode, platformCode);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(CrawlTask::getStatus, Integer.parseInt(status));
        }
        wrapper.orderByDesc(CrawlTask::getCreateTime);
        int start = (page - 1) * size;
        wrapper.last("LIMIT " + start + ", " + size);
        return crawlTaskMapper.selectList(wrapper);
    }

    public long countTasks(String platformCode, String status) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CrawlTask> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (platformCode != null && !platformCode.isEmpty()) {
            wrapper.eq(CrawlTask::getPlatformCode, platformCode);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(CrawlTask::getStatus, Integer.parseInt(status));
        }
        return crawlTaskMapper.selectCount(wrapper);
    }

    public void startTask(Long taskId) {
        CrawlTask task = crawlTaskMapper.selectById(taskId);
        if (task != null) {
            task.setStatus(1);
            task.setStartTime(LocalDateTime.now());
            task.setErrorMsg(null);
            crawlTaskMapper.updateById(task);
            log.info("▶️ 启动抓取任务: id={}", taskId);
        } else {
            throw new RuntimeException("任务不存在: " + taskId);
        }
    }

    public void stopTask(Long taskId) {
        crawlTaskMapper.updateStatus(taskId, 3, "用户手动停止");
        log.info("⏹️ 停止抓取任务: id={}", taskId);
    }

    public void deleteTask(Long taskId) {
        int result = crawlTaskMapper.deleteById(taskId);
        if (result > 0) {
            log.info("🗑️ 删除抓取任务: id={}", taskId);
        } else {
            throw new RuntimeException("任务不存在或已删除: " + taskId);
        }
    }

    // ====== 数据模型 ======

    @Data
    public static class PlatformInfo {
        private String code;
        private String name;
        private String rateLimitInfo;
        private List<String> supportedFeatures;

        public PlatformInfo() {}

        public static PlatformInfoBuilder builder() {
            return new PlatformInfoBuilder();
        }

        public PlatformInfo(String code, String name, String rateLimitInfo, List<String> supportedFeatures) {
            this.code = code;
            this.name = name;
            this.rateLimitInfo = rateLimitInfo;
            this.supportedFeatures = supportedFeatures;
        }

        public static class PlatformInfoBuilder {
            private PlatformInfo info = new PlatformInfo();

            public PlatformInfoBuilder code(String code) { info.code = code; return this; }
            public PlatformInfoBuilder name(String name) { info.name = name; return this; }
            public PlatformInfoBuilder rateLimitInfo(String rateLimitInfo) { info.rateLimitInfo = rateLimitInfo; return this; }
            public PlatformInfoBuilder supportedFeatures(List<String> supportedFeatures) { info.supportedFeatures = supportedFeatures; return this; }

            public PlatformInfo build() { return info; }
        }
    }

    @Data
    public static class BatchCrawlResult {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int totalTargets;
        private int successCount;
        private int failureCount;
        private long totalComments;
        private long durationMs;
        private String errorMessage;
        private List<CommentCrawlResult> results = new ArrayList<>();
        private List<String> failures = new ArrayList<>();

        public void addResult(CommentCrawlResult result) {
            results.add(result);
            totalTargets++;
            if (result.isSuccess()) {
                successCount++;
                totalComments += result.getStatistics().getTotalComments();
            } else {
                failureCount++;
                failures.add(result.getErrorMessage());
            }
        }

        public void addFailure(String error) {
            failures.add(error);
            failureCount++;
            totalTargets++;
        }

        public void calculateSummary() {
            if (startTime != null && endTime != null) {
                durationMs = java.time.Duration.between(startTime, endTime).toMillis();
            }
        }

        public String getSummary() {
            return String.format("[批量] %d/%d成功 | 总计%d条评论 | 耗时%dms",
                    successCount, totalTargets, totalComments, durationMs);
        }
    }
}
