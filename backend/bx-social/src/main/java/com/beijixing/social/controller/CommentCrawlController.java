package com.beijixing.social.controller;

import com.beijixing.social.crawl.model.*;
import com.beijixing.social.crawl.service.CommentCrawlService;
import com.beijixing.social.crawl.service.CustomTargetPoolService;
import com.beijixing.social.vo.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评论抓取API控制器
 *
 * 提供统一的RESTful接口，支持多平台评论抓取
 *
 * 核心API:
 * - POST /api/crawl/url - 从URL抓取评论 (自动识别平台)
 * - POST /api/crawl/platform/{platform} - 指定平台+ID抓取
 * - POST /api/crawl/batch - 批量抓取多个目标
 * - POST /api/crawl/import/csv - CSV导入并抓取
 * - GET /api/crawl/platforms - 获取支持的平台列表
 * - GET /api/crawl/history - 获取抓取历史
 *
 * @author 北极星AI团队
 * @version 2.0 (2026-05-20)
 */
@RestController
@RequestMapping("/crawl")
@Tag(name = "评论抓取API", description = "支持抖音/小红书/快手/微博/B站等10+平台的评论抓取")
@Slf4j
public class CommentCrawlController {

    @Autowired
    private CommentCrawlService crawlService;

    @Autowired
    private CustomTargetPoolService targetPoolService;

    /**
     * 从URL抓取评论（核心接口）
     *
     * 自动识别URL所属平台并抓取评论
     *
     * 请求示例:
     * POST /api/crawl/url
     * {
     *   "url": "https://v.douyin.com/i2eABC123/",
     *   "userId": "10001",
     *   "options": {
     *     "pageSize": 20,
     *     "maxPages": 50,
     *     "includeReplies": true,
     *     "sortType": "TIME_DESC"
     *   }
     * }
     */
    @PostMapping("/url")
    @Operation(summary = "从URL抓取评论", description = "自动识别平台并抓取指定链接的评论")
    public ResponseEntity<ApiResponse<CommentCrawlResult>> crawlFromUrl(
            @RequestBody CrawlFromUrlRequest request) {

        log.info("📥 [API] 收到抓取请求 | URL: {} | 用户: {}", request.getUrl(), request.getUserId());

        CommentCrawlResult result = crawlService.crawlFromUrl(
                request.getUrl(),
                request.getUserId(),
                request.getOptions()
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success(result));
        } else {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(result.getErrorCode() != null ? Integer.parseInt(result.getErrorCode()) : 400, result.getErrorMessage()));
        }
    }

    /**
     * 指定平台 + 目标ID 抓取评论
     *
     * 适用于已知平台和目标ID的场景
     *
     * 平台代码: DOUYIN, XIAOHONGSHU, KUAISHOU, WEIBO, BILIBILI
     */
    @PostMapping("/platform/{platform}")
    @Operation(summary = "按平台抓取评论", description = "指定平台代码和目标ID进行评论抓取")
    public ResponseEntity<ApiResponse<CommentCrawlResult>> crawlByPlatform(
            @PathVariable String platform,
            @RequestBody CrawlByPlatformRequest request) {

        CommentCrawlResult result = crawlService.crawlComments(
                platform,
                request.getTargetId(),
                request.getUserId(),
                request.getOptions()
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success(result));
        } else {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(result.getErrorCode() != null ? Integer.parseInt(result.getErrorCode()) : 400, result.getErrorMessage()));
        }
    }

    /**
     * 批量抓取多个目标（并行执行）
     *
     * 支持混合输入不同平台的URL
     */
    @PostMapping("/batch")
    @Operation(summary = "批量抓取", description = "并行抓取多个目标的评论")
    public ResponseEntity<ApiResponse<CommentCrawlService.BatchCrawlResult>> batchCrawl(
            @RequestBody BatchCrawlRequest request) {

        CommentCrawlService.BatchCrawlResult batchResult =
                crawlService.batchCrawl(request.getTargets(), request.getUserId(), request.getOptions());

        return ResponseEntity.ok(ApiResponse.success(batchResult));
    }

    /**
     * CSV导入并批量抓取
     *
     * CSV格式: 每行一个URL或ID
     */
    @PostMapping("/import/csv")
    @Operation(summary = "CSV导入抓取", description = "从CSV内容导入目标列表并批量抓取")
    public ResponseEntity<ApiResponse<CommentCrawlService.BatchCrawlResult>> importAndCrawl(
            @RequestBody CsvImportRequest request) {

        CommentCrawlService.BatchCrawlResult result =
                crawlService.importAndCrawl(request.getCsvContent(), request.getDelimiter(),
                        request.getUserId(), request.getOptions());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取所有支持的平台列表及特性
     */
    @GetMapping("/platforms")
    @Operation(summary = "获取支持的平台列表")
    public ResponseEntity<ApiResponse<List<CommentCrawlService.PlatformInfo>>> getPlatforms() {
        List<CommentCrawlService.PlatformInfo> platforms = crawlService.getSupportedPlatforms();
        return ResponseEntity.ok(ApiResponse.success(platforms));
    }

    /**
     * 获取用户的抓取历史记录
     */
    @GetMapping("/history")
    @Operation(summary = "获取抓取历史")
    public ResponseEntity<ApiResponse<List<?>>> getCrawlHistory(
            @Parameter(description = "用户ID") @RequestParam String userId) {

        var history = crawlService.getCrawlHistory(userId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    /**
     * 清空用户的目标池
     */
    @DeleteMapping("/pool")
    @Operation(summary = "清空目标池")
    public ResponseEntity<ApiResponse<String>> clearPool(
            @RequestParam String userId) {
        crawlService.clearUserPool(userId);
        return ResponseEntity.ok(ApiResponse.success("目标池已清空"));
    }

    // ============================================================
    // Phase 3 增强: 目标池管理API (v3.0)
    // ============================================================

    /**
     * 添加抓取目标到自定义池（支持URL自动解析）
     *
     * 支持格式:
     * - 抖音: https://v.douyin.com/i2eABC123/ 或 video ID
     * - 小红书: http://xhslink.com/a/xxxxx 或 note ID
     * - 快手/微博/B站等
     */
    @PostMapping("/targets/add")
    @Operation(summary = "添加抓取目标", description = "将URL或ID添加到自定义目标池，支持自动识别平台")
    public ResponseEntity<ApiResponse<CustomTargetPoolService.CrawlTarget>> addTarget(
            @RequestBody AddTargetRequest request) {

        log.info("📥 [Phase 3] 添加目标到池 | 输入: {} | 用户: {}", request.getInput(), request.getUserId());

        try {
            CustomTargetPoolService.CrawlTarget target = targetPoolService.addTarget(
                    request.getInput(),
                    request.getUserId()
            );

            return ResponseEntity.ok(ApiResponse.success(target));
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ 添加目标失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.fail(400, e.getMessage()));
        }
    }

    /**
     * 批量添加抓取目标
     *
     * 适用于从Excel/CSV批量导入场景
     */
    @PostMapping("/targets/batch-add")
    @Operation(summary = "批量添加目标", description = "一次添加多个URL或ID到目标池")
    public ResponseEntity<ApiResponse<BatchAddResult>> batchAddTargets(
            @RequestBody BatchAddTargetsRequest request) {

        log.info("📥 [Phase 3] 批量添加目标 | 数量: {} | 用户: {}",
                request.getInputs().size(), request.getUserId());

        BatchAddResult result = new BatchAddResult();
        int successCount = 0;
        int failCount = 0;
        List<String> failedItems = new java.util.ArrayList<>();

        for (String input : request.getInputs()) {
            try {
                CustomTargetPoolService.CrawlTarget target = targetPoolService.addTarget(input, request.getUserId());
                successCount++;
                if (target.getStatus() == CustomTargetPoolService.TargetStatus.SKIPPED) {
                    result.incrementSkipped();
                }
            } catch (Exception e) {
                failCount++;
                failedItems.add(input + " (" + e.getMessage() + ")");
                log.warn("⚠️ 批量添加失败项: {} | 原因: {}", input, e.getMessage());
            }
        }

        result.setTotal(request.getInputs().size());
        result.setSuccessCount(successCount);
        result.setFailCount(failCount);
        result.setFailedItems(failedItems);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 查询用户的目标池列表（分页+筛选）
     */
    @GetMapping("/targets")
    @Operation(summary = "查询目标池", description = "分页查询用户的抓取目标列表，支持状态筛选和平台过滤")
    public ResponseEntity<ApiResponse<TargetPoolPageResult>> getTargets(
            @Parameter(description = "用户ID") @RequestParam String userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "状态筛选") @RequestParam(required = false) String status,
            @Parameter(description = "平台筛选") @RequestParam(required = false) String platform) {

        List<CustomTargetPoolService.CrawlTarget> allTargets = targetPoolService.getUserTargets(userId);

        // 应用筛选条件
        List<CustomTargetPoolService.CrawlTarget> filtered = allTargets;

        if (status != null && !status.isEmpty()) {
            CustomTargetPoolService.TargetStatus targetStatus =
                    CustomTargetPoolService.TargetStatus.valueOf(status.toUpperCase());
            filtered = filtered.stream()
                    .filter(t -> t.getStatus() == targetStatus)
                    .collect(Collectors.toList());
        }

        if (platform != null && !platform.isEmpty()) {
            filtered = filtered.stream()
                    .filter(t -> t.getPlatform().equalsIgnoreCase(platform))
                    .collect(Collectors.toList());
        }

        // 分页处理
        int total = filtered.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);

        List<CustomTargetPoolService.CrawlTarget> pageData =
                (start < total) ? filtered.subList(start, end) : new java.util.ArrayList<>();

        TargetPoolPageResult result = new TargetPoolPageResult();
        result.setRecords(pageData);
        result.setTotal(total);
        result.setCurrent(page);
        result.setSize(size);
        result.setStatusSummary(calculateStatusSummary(allTargets));

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 删除指定目标
     */
    @DeleteMapping("/targets/{targetId}")
    @Operation(summary = "删除目标", description = "从目标池中删除指定目标")
    public ResponseEntity<ApiResponse<String>> deleteTarget(
            @PathVariable String targetId,
            @RequestParam String userId) {

        boolean removed = targetPoolService.removeTarget(targetId, userId);
        if (removed) {
            return ResponseEntity.ok(ApiResponse.success("目标已删除"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取目标详情（含最近抓取结果）
     */
    @GetMapping("/targets/{targetId}/detail")
    @Operation(summary = "目标详情", description = "获取目标的完整信息和最近抓取结果")
    public ResponseEntity<ApiResponse<CustomTargetPoolService.CrawlTarget>> getTargetDetail(
            @PathVariable String targetId) {

        CustomTargetPoolService.CrawlTarget target = targetPoolService.getTarget(targetId);
        if (target != null) {
            return ResponseEntity.ok(ApiResponse.success(target));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取目标池统计信息
     */
    @GetMapping("/targets/stats")
    @Operation(summary = "目标池统计", description = "获取用户目标池的整体统计信息")
    public ResponseEntity<ApiResponse<TargetPoolStats>> getTargetPoolStats(
            @RequestParam String userId) {

        List<CustomTargetPoolService.CrawlTarget> targets = targetPoolService.getUserTargets(userId);

        TargetPoolStats stats = new TargetPoolStats();
        stats.setTotalTargets(targets.size());
        stats.setStatusSummary(calculateStatusSummary(targets));

        Map<String, Long> platformDistribution = targets.stream()
                .collect(Collectors.groupingBy(CustomTargetPoolService.CrawlTarget::getPlatform, Collectors.counting()));
        stats.setPlatformDistribution(platformDistribution);

        long todayCrawled = targets.stream()
                .filter(t -> t.getLastCrawledAt() != null &&
                        t.getLastCrawledAt().toLocalDate().equals(java.time.LocalDate.now()))
                .count();
        stats.setTodayCrawled(todayCrawled);

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    private Map<String, Integer> calculateStatusSummary(List<CustomTargetPoolService.CrawlTarget> targets) {
        return targets.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getStatus().name(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
    }

    // ====== Phase 3 数据模型 ======

    @lombok.Data
    public static class AddTargetRequest {
        private String input;       // URL或平台ID (必填)
        private String userId;      // 用户ID (必填)
    }

    @lombok.Data
    public static class BatchAddTargetsRequest {
        private List<String> inputs;   // URL/ID列表 (必填)
        private String userId;          // 用户ID (必填)
    }

    @lombok.Data
    public static class BatchAddResult {
        private int total;
        private int successCount;
        private int failCount;
        private int skippedCount;
        private List<String> failedItems;

        public void incrementSkipped() { this.skippedCount++; }
    }

    @lombok.Data
    public static class TargetPoolPageResult {
        private List<CustomTargetPoolService.CrawlTarget> records;
        private int total;
        private int current;
        private int size;
        private Map<String, Integer> statusSummary;
    }

    @lombok.Data
    public static class TargetPoolStats {
        private int totalTargets;
        private Map<String, Integer> statusSummary;
        private Map<String, Long> platformDistribution;
        private long todayCrawled;
    }

    // ====== 请求数据模型 ======

    @lombok.Data
    public static class CrawlFromUrlRequest {
        private String url;                      // 目标URL (必填)
        private String userId;                   // 用户ID (必填)
        private CrawlOptions options;             // 抓取配置 (可选)
    }

    @lombok.Data
    public static class CrawlByPlatformRequest {
        private String targetId;                 // 目标ID (必填)
        private String userId;                   // 用户ID (必填)
        private CrawlOptions options;             // 抓取配置 (可选)
    }

    @lombok.Data
    public static class BatchCrawlRequest {
        private List<String> targets;            // URL/ID列表 (必填)
        private String userId;                   // 用户ID (必填)
        private CrawlOptions options;             // 抓取配置 (可选)
    }

    @lombok.Data
    public static class CsvImportRequest {
        private String csvContent;               // CSV内容 (必填)
        private String delimiter;                 // 分隔符 (默认逗号)
        private String userId;                   // 用户ID (必填)
        private CrawlOptions options;             // 抓取配置 (可选)
    }
}
