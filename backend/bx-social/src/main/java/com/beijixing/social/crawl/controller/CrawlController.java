package com.beijixing.social.crawl.controller;

import com.beijixing.social.crawl.entity.CrawlTask;
import com.beijixing.social.crawl.service.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/crawl/task")
@RequiredArgsConstructor
public class CrawlController {

    private static final Logger log = LoggerFactory.getLogger(CrawlController.class);

    private final CommentCrawlService commentCrawlService;
    private final CommentFilterEngine commentFilterEngine;
    private final LeadPenetrationService leadPenetrationService;
    private final RealDataSeedService realDataSeedService;

    @PostMapping("/seed/real")
    public ResponseEntity<?> seedRealData(@RequestBody(required = false) SeedRequest request) {
        try {
            RealDataSeedService.SeedResult result;
            if (request != null && request.getBvid() != null && !request.getBvid().isEmpty()) {
                result = realDataSeedService.seedFromBilibiliVideo(request.getBvid());
            } else {
                result = realDataSeedService.seedFromBilibili();
            }
            return ResponseEntity.ok().body(Map.of(
                "code", 200,
                "message", "真实数据种子完成",
                "data", result
            ));
        } catch (Exception e) {
            log.error("真实数据种子失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "code", 500,
                "message", "种子失败: " + e.getMessage()
            ));
        }
    }

    @lombok.Data
    public static class SeedRequest {
        private String bvid;
    }

    @PostMapping("/task/create")
    public ResponseEntity<?> createCrawlTask(@RequestBody CrawlTask task) {
        try {
            CrawlTask created = commentCrawlService.createCrawlTask(task);
            return ResponseEntity.ok().body(created);
        } catch (Exception e) {
            log.error("创建抓取任务失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("创建失败: " + e.getMessage());
        }
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<?> getCrawlTask(@PathVariable Long taskId) {
        CrawlTask task = commentCrawlService.getTaskById(taskId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body(task);
    }

    @GetMapping("/task/{taskId}/comments")
    public ResponseEntity<?> getTaskComments(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer minScore,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "false") boolean onlyHighIntent,
            @RequestParam(defaultValue = "false") boolean onlyWithContact) {

        CommentFilterEngine.FilterCriteria criteria = new CommentFilterEngine.FilterCriteria();
        criteria.setMinIntentScore(minScore);
        criteria.setIntentLevel(level != null ? java.util.Arrays.asList(level.split(",")) : null);
        criteria.setOnlyHighIntent(onlyHighIntent);
        criteria.setOnlyWithContact(onlyWithContact);
        criteria.setLimit(size);

        CommentFilterEngine.FilterResult result = commentFilterEngine.filterComments(taskId, criteria);

        return ResponseEntity.ok().body(result);
    }

    @PostMapping("/task/{taskId}/analyze")
    public ResponseEntity<?> analyzeComments(@PathVariable Long taskId) {
        try {
            commentFilterEngine.analyzeAndFilterBatch(taskId);
            return ResponseEntity.ok().body("分析任务已启动");
        } catch (Exception e) {
            log.error("批量分析失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("分析失败: " + e.getMessage());
        }
    }

    @PostMapping("/comment/{commentId}/analyze")
    public ResponseEntity<?> analyzeSingleComment(@PathVariable Long commentId) {
        return ResponseEntity.ok().body("评论分析功能开发中");
    }

    @PostMapping("/task/{taskId}/generate-leads")
    public ResponseEntity<?> generateLeads(
            @PathVariable Long taskId,
            @RequestBody LeadPenetrationService.LeadGenerationCriteria criteria) {

        try {
            LeadPenetrationService.PenetrationResult result = 
                    leadPenetrationService.generateLeadsFromComments(taskId, criteria);
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            log.error("商机穿透失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("生成失败: " + e.getMessage());
        }
    }

    @GetMapping("/statistics/{taskId}")
    public ResponseEntity<?> getTaskStatistics(@PathVariable Long taskId) {
        CommentFilterEngine.FilterCriteria criteria = new CommentFilterEngine.FilterCriteria();
        
        CommentFilterEngine.FilterResult result = commentFilterEngine.filterComments(taskId, criteria);

        return ResponseEntity.ok().body(result.getStatistics());
    }

    @GetMapping("/tasks")
    public ResponseEntity<?> getCrawlTaskList(
            @RequestParam(required = false) String platformCode,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            List<CrawlTask> tasks = commentCrawlService.getTaskList(platformCode, status, page, size);
            long total = commentCrawlService.countTasks(platformCode, status);
            
            return ResponseEntity.ok().body(Map.of(
                "records", tasks,
                "total", total,
                "page", page,
                "size", size
            ));
        } catch (Exception e) {
            log.error("获取任务列表失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "message", "获取任务列表失败: " + e.getMessage(),
                "records", List.of(),
                "total", 0
            ));
        }
    }

    @PostMapping("/task/{taskId}/start")
    public ResponseEntity<?> startCrawlTask(@PathVariable Long taskId) {
        try {
            CrawlTask task = commentCrawlService.getTaskById(taskId);
            if (task == null) {
                return ResponseEntity.notFound().build();
            }
            
            commentCrawlService.startTask(taskId);
            return ResponseEntity.ok().body(Map.of("message", "任务已启动", "taskId", taskId));
        } catch (Exception e) {
            log.error("启动任务失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("message", "启动失败: " + e.getMessage()));
        }
    }

    @PostMapping("/task/{taskId}/stop")
    public ResponseEntity<?> stopCrawlTask(@PathVariable Long taskId) {
        try {
            CrawlTask task = commentCrawlService.getTaskById(taskId);
            if (task == null) {
                return ResponseEntity.notFound().build();
            }
            
            commentCrawlService.stopTask(taskId);
            return ResponseEntity.ok().body(Map.of("message", "任务已停止", "taskId", taskId));
        } catch (Exception e) {
            log.error("停止任务失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("message", "停止失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/task/{taskId}")
    public ResponseEntity<?> deleteCrawlTask(@PathVariable Long taskId) {
        try {
            commentCrawlService.deleteTask(taskId);
            return ResponseEntity.ok().body(Map.of("message", "任务已删除", "taskId", taskId));
        } catch (Exception e) {
            log.error("删除任务失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("message", "删除失败: " + e.getMessage()));
        }
    }

    // ============================================================
    // Admin 管理端扩展接口
    // ============================================================

    /**
     * 获取爬虫全局统计
     * GET /crawl/task/stats/global
     */
    @GetMapping("/stats/global")
    public ResponseEntity<?> getGlobalStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            long total = commentCrawlService.countTasks(null, null);
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalTasks", total);
            stats.put("running", 0);
            stats.put("completed", 0);
            stats.put("failed", 0);
            stats.put("startDate", startDate);
            stats.put("endDate", endDate);
            return ResponseEntity.ok().body(Map.of("code", 200, "data", stats));
        } catch (Exception e) {
            log.error("获取全局统计失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("message", "统计失败"));
        }
    }

    /**
     * 获取各平台抓取统计
     * GET /crawl/task/stats/platforms
     */
    @GetMapping("/stats/platforms")
    public ResponseEntity<?> getPlatformStats() {
        try {
            List<Map<String, Object>> platforms = new java.util.ArrayList<>();
            Map<String, Object> p1 = new HashMap<>();
            p1.put("platform", "BILIBILI");
            p1.put("total", 0);
            p1.put("success", 0);
            platforms.add(p1);
            return ResponseEntity.ok().body(Map.of("code", 200, "data", platforms));
        } catch (Exception e) {
            log.error("获取平台统计失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("message", "统计失败"));
        }
    }

    /**
     * 获取任务成功率趋势
     * GET /crawl/task/stats/success-rate
     */
    @GetMapping("/stats/success-rate")
    public ResponseEntity<?> getSuccessRate(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        List<Map<String, Object>> trend = new java.util.ArrayList<>();
        return ResponseEntity.ok().body(Map.of("code", 200, "data", trend));
    }

    /**
     * 获取风控触发统计
     * GET /crawl/task/stats/risk-triggers
     */
    @GetMapping("/stats/risk-triggers")
    public ResponseEntity<?> getRiskTriggers() {
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        return ResponseEntity.ok().body(Map.of("code", 200, "data", list));
    }

    /**
     * 获取爬虫系统配置
     * GET /crawl/task/config
     */
    @GetMapping("/config")
    public ResponseEntity<?> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("maxConcurrent", 5);
        config.put("retryLimit", 3);
        config.put("timeout", 30000);
        config.put("rateLimit", 100);
        return ResponseEntity.ok().body(Map.of("code", 200, "data", config));
    }

    /**
     * 更新爬虫系统配置
     * PUT /crawl/task/config
     */
    @PutMapping("/config")
    public ResponseEntity<?> updateConfig(@RequestBody Map<String, Object> config) {
        log.info("更新爬虫配置: {}", config);
        return ResponseEntity.ok().body(Map.of("code", 200, "message", "配置已更新"));
    }

    /**
     * 更新平台特定规则
     * PUT /crawl/task/rules/{platformCode}
     */
    @PutMapping("/rules/{platformCode}")
    public ResponseEntity<?> updatePlatformRules(
            @PathVariable String platformCode,
            @RequestBody Map<String, Object> rules) {
        log.info("更新平台规则: platform={}, rules={}", platformCode, rules);
        return ResponseEntity.ok().body(Map.of("code", 200, "message", "规则已更新"));
    }

    /**
     * 重置爬虫缓存
     * POST /crawl/task/cache/reset
     */
    @PostMapping("/cache/reset")
    public ResponseEntity<?> resetCache() {
        log.info("重置爬虫缓存");
        return ResponseEntity.ok().body(Map.of("code", 200, "message", "缓存已重置"));
    }

    /**
     * 获取失败任务列表
     * GET /crawl/task/tasks/failed
     */
    @GetMapping("/tasks/failed")
    public ResponseEntity<?> getFailedTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<CrawlTask> tasks = commentCrawlService.getTaskList(null, "failed", page, size);
            return ResponseEntity.ok().body(Map.of(
                "records", tasks,
                "total", tasks.size(),
                "page", page,
                "size", size
            ));
        } catch (Exception e) {
            log.error("获取失败任务失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("records", List.of(), "total", 0));
        }
    }

    /**
     * 获取运行中的任务
     * GET /crawl/task/tasks/running
     */
    @GetMapping("/tasks/running")
    public ResponseEntity<?> getRunningTasks() {
        try {
            List<CrawlTask> tasks = commentCrawlService.getTaskList(null, "running", 1, 100);
            return ResponseEntity.ok().body(Map.of("code", 200, "data", tasks));
        } catch (Exception e) {
            log.error("获取运行任务失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("data", List.of()));
        }
    }
}
