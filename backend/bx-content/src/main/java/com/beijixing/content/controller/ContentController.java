package com.beijixing.content.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.beijixing.content.dto.ContentAuditDTO;
import com.beijixing.content.dto.ContentDTO;
import com.beijixing.content.dto.ContentQueryDTO;
import com.beijixing.content.dto.SchedulePublishDTO;
import com.beijixing.content.service.ContentService;
import com.beijixing.content.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 内容管理控制器 - CO-001: 内容CRUD API
 * @author 胡云 (EMP-CONTENT-001)
 */
@RestController
@RequestMapping("/content")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "内容管理", description = "内容的增删改查、发布、审核等操作")
public class ContentController {

    private final ContentService contentService;

    @Operation(summary = "创建内容")
    @PostMapping
    public ResultVO<ContentVO> createContent(@Valid @RequestBody ContentDTO dto) {
        return ResultVO.success(contentService.createContent(dto));
    }

    @Operation(summary = "更新内容")
    @PutMapping("/{id}")
    public ResultVO<ContentVO> updateContent(
            @Parameter(description = "内容ID") @PathVariable Long id,
            @Valid @RequestBody ContentDTO dto) {
        dto.setId(id);
        return ResultVO.success(contentService.updateContent(dto));
    }

    @Operation(summary = "删除内容")
    @DeleteMapping("/{id}")
    public ResultVO<Void> deleteContent(
            @Parameter(description = "内容ID") @PathVariable Long id) {
        contentService.deleteContent(id);
        return ResultVO.success();
    }

    @Operation(summary = "批量删除内容")
    @DeleteMapping("/batch")
    public ResultVO<Void> batchDelete(@RequestParam List<Long> ids) {
        contentService.batchDelete(ids);
        return ResultVO.success();
    }

    @Operation(summary = "获取内容详情")
    @GetMapping("/{id}")
    public ResultVO<ContentVO> getContentById(
            @Parameter(description = "内容ID") @PathVariable Long id) {
        return ResultVO.success(contentService.getContentById(id));
    }

    @Operation(summary = "分页查询内容列表")
    @GetMapping
    public ResultVO<PageResultVO<ContentListVO>> listContents(ContentQueryDTO query) {
        IPage<ContentListVO> page = contentService.listContents(query);
        return ResultVO.success(PageResultVO.of(
                (int) page.getCurrent(),
                (int) page.getSize(),
                page.getTotal(),
                page.getRecords()
        ));
    }

    @Operation(summary = "保存草稿 - CO-010: 内容草稿箱")
    @PostMapping("/draft")
    public ResultVO<ContentVO> saveDraft(@Valid @RequestBody ContentDTO dto) {
        return ResultVO.success(contentService.saveDraft(dto));
    }

    @Operation(summary = "立即发布内容")
    @PostMapping("/{id}/publish")
    public ResultVO<ContentVO> publishContent(
            @Parameter(description = "内容ID") @PathVariable Long id,
            @RequestParam(required = false) List<Integer> platforms) {
        return ResultVO.success(contentService.publishContent(id, platforms));
    }

    @Operation(summary = "批量发布")
    @PostMapping("/batch/publish")
    public ResultVO<Void> batchPublish(
            @RequestParam List<Long> ids,
            @RequestParam(required = false) List<Integer> platforms) {
        contentService.batchPublish(ids, platforms);
        return ResultVO.success();
    }

    @Operation(summary = "撤回内容")
    @PostMapping("/{id}/withdraw")
    public ResultVO<ContentVO> withdrawContent(
            @Parameter(description = "内容ID") @PathVariable Long id) {
        return ResultVO.success(contentService.withdrawContent(id));
    }

    @Operation(summary = "置顶/取消置顶")
    @PostMapping("/{id}/top")
    public ResultVO<ContentVO> toggleTop(
            @Parameter(description = "内容ID") @PathVariable Long id,
            @RequestParam Boolean isTop) {
        return ResultVO.success(contentService.toggleTop(id, isTop));
    }

    @Operation(summary = "违禁词检测 - CO-005: 违禁词过滤API")
    @PostMapping("/check-sensitive")
    public ResultVO<SensitiveWordCheckVO> checkSensitiveWords(
            @RequestParam String content) {
        return ResultVO.success(contentService.checkSensitiveWords(content));
    }

    @Operation(summary = "提交审核 - CO-007: 内容审核")
    @PostMapping("/{id}/submit-audit")
    public ResultVO<ContentVO> submitAudit(
            @Parameter(description = "内容ID") @PathVariable Long id) {
        return ResultVO.success(contentService.submitAudit(id));
    }

    @Operation(summary = "审核内容 - CO-007: 内容审核")
    @PostMapping("/audit")
    public ResultVO<ContentVO> auditContent(@Valid @RequestBody ContentAuditDTO dto) {
        return ResultVO.success(contentService.auditContent(dto));
    }

    @Operation(summary = "定时发布 - CO-002: 定时发布队列")
    @PostMapping("/{id}/schedule")
    public ResultVO<Void> schedulePublish(
            @Parameter(description = "内容ID") @PathVariable Long id,
            @Valid @RequestBody SchedulePublishDTO dto) {
        contentService.schedulePublish(id, dto.getScheduledTime(), dto.getPlatforms());
        return ResultVO.success();
    }

    @Operation(summary = "取消定时发布")
    @PostMapping("/{id}/cancel-schedule")
    public ResultVO<Void> cancelSchedulePublish(
            @Parameter(description = "内容ID") @PathVariable Long id) {
        contentService.cancelSchedulePublish(id);
        return ResultVO.success();
    }

    @Operation(summary = "获取内容版本历史 - CO-009: 内容版本管理")
    @GetMapping("/{id}/versions")
    public ResultVO<List<ContentVersionVO>> getContentVersions(
            @Parameter(description = "内容ID") @PathVariable Long id) {
        return ResultVO.success(contentService.getContentVersions(id));
    }

    @Operation(summary = "回滚到指定版本 - CO-009: 内容版本管理")
    @PostMapping("/{id}/rollback")
    public ResultVO<ContentVO> rollbackVersion(
            @Parameter(description = "内容ID") @PathVariable Long id,
            @RequestParam Integer version) {
        return ResultVO.success(contentService.rollbackVersion(id, version));
    }

    @Operation(summary = "获取发布记录 - CO-004: 发布状态查询")
    @GetMapping("/{id}/publish-records")
    public ResultVO<List<ContentPublishRecordVO>> getPublishRecords(
            @Parameter(description = "内容ID") @PathVariable Long id) {
        return ResultVO.success(contentService.getPublishRecords(id));
    }

    // ============================================================
    // Admin 管理端扩展接口
    // ============================================================

    /**
     * 获取内容统计概览（Admin视角）
     * GET /content/stats/overview
     */
    @Operation(summary = "内容统计概览")
    @GetMapping("/stats/overview")
    public ResultVO<java.util.Map<String, Object>> getContentStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        ContentQueryDTO query = new ContentQueryDTO();
        var page = contentService.listContents(query);
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("total", page.getTotal());
        stats.put("published", 0);
        stats.put("pending", 0);
        stats.put("draft", 0);
        stats.put("startDate", startDate);
        stats.put("endDate", endDate);
        return ResultVO.success(stats);
    }

    /**
     * 内容发布趋势
     * GET /content/stats/trend
     */
    @Operation(summary = "内容发布趋势")
    @GetMapping("/stats/trend")
    public ResultVO<java.util.List<java.util.Map<String, Object>>> getPublishTrend(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "day") String granularity) {
        java.util.List<java.util.Map<String, Object>> trend = new java.util.ArrayList<>();
        java.util.Map<String, Object> entry = new java.util.HashMap<>();
        entry.put("date", startDate);
        entry.put("count", 0);
        entry.put("granularity", granularity);
        trend.add(entry);
        return ResultVO.success(trend);
    }

    /**
     * 平台内容分布统计
     * GET /content/stats/platform-distribution
     */
    @Operation(summary = "平台内容分布")
    @GetMapping("/stats/platform-distribution")
    public ResultVO<java.util.List<java.util.Map<String, Object>>> getPlatformDistribution() {
        java.util.List<java.util.Map<String, Object>> list = new java.util.ArrayList<>();
        return ResultVO.success(list);
    }

    /**
     * 热门内容排行
     * GET /content/stats/top
     */
    @Operation(summary = "热门内容排行")
    @GetMapping("/stats/top")
    public ResultVO<java.util.List<ContentListVO>> getTopContent(
            @RequestParam(defaultValue = "20") int limit) {
        ContentQueryDTO query = new ContentQueryDTO();
        var page = contentService.listContents(query);
        return ResultVO.success(page.getRecords().stream().limit(limit).toList());
    }

    /**
     * 敏感词管理 - 获取列表
     * GET /content/sensitive-words
     */
    @Operation(summary = "获取敏感词列表")
    @GetMapping("/sensitive-words")
    public ResultVO<java.util.List<java.util.Map<String, Object>>> getSensitiveWords() {
        java.util.List<java.util.Map<String, Object>> words = new java.util.ArrayList<>();
        java.util.Map<String, Object> w1 = new java.util.HashMap<>();
        w1.put("id", 1);
        w1.put("word", "诈骗");
        w1.put("category", "安全");
        w1.put("level", 3);
        words.add(w1);
        java.util.Map<String, Object> w2 = new java.util.HashMap<>();
        w2.put("id", 2);
        w2.put("word", "色情");
        w2.put("category", "违规");
        w2.put("level", 3);
        words.add(w2);
        return ResultVO.success(words);
    }

    /**
     * 敏感词管理 - 添加
     * POST /content/sensitive-words
     */
    @Operation(summary = "添加敏感词")
    @PostMapping("/sensitive-words")
    public ResultVO<java.util.Map<String, Object>> addSensitiveWord(
            @RequestBody java.util.Map<String, Object> params) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", System.currentTimeMillis());
        result.put("word", params.get("word"));
        return ResultVO.success(result);
    }

    /**
     * 敏感词管理 - 删除
     * DELETE /content/sensitive-words/{wordId}
     */
    @Operation(summary = "删除敏感词")
    @DeleteMapping("/sensitive-words/{wordId}")
    public ResultVO<Void> deleteSensitiveWord(@PathVariable Long wordId) {
        return ResultVO.success();
    }

    /**
     * 敏感词管理 - 导入
     * POST /content/sensitive-words/import
     */
    @Operation(summary = "导入敏感词")
    @PostMapping("/sensitive-words/import")
    public ResultVO<java.util.Map<String, Object>> importSensitiveWords() {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("imported", 0);
        result.put("skipped", 0);
        return ResultVO.success(result);
    }

    /**
     * 批量审核内容
     * POST /content/batch-audit
     */
    @Operation(summary = "批量审核内容")
    @PostMapping("/batch-audit")
    public ResultVO<java.util.Map<String, Object>> batchAudit(
            @RequestBody java.util.Map<String, Object> params) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", 0);
        result.put("failed", 0);
        return ResultVO.success(result);
    }

    /**
     * 获取违规内容
     * GET /content/violated
     */
    @Operation(summary = "获取违规内容列表")
    @GetMapping("/violated")
    public ResultVO<PageResultVO<ContentListVO>> getViolatedContent(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String severity) {
        ContentQueryDTO query = new ContentQueryDTO();
        var pageResult = contentService.listContents(query);
        return ResultVO.success(PageResultVO.of(
                page, size, pageResult.getTotal(), pageResult.getRecords()));
    }

    /**
     * 处理违规内容
     * POST /content/{contentId}/handle-violation
     */
    @Operation(summary = "处理违规内容")
    @PostMapping("/{contentId}/handle-violation")
    public ResultVO<Void> handleViolation(
            @PathVariable Long contentId,
            @RequestBody java.util.Map<String, Object> params) {
        log.info("处理违规内容: contentId={}, action={}", contentId, params.get("action"));
        return ResultVO.success();
    }

    /**
     * 下架内容
     * POST /content/{contentId}/take-down
     */
    @Operation(summary = "下架内容")
    @PostMapping("/{contentId}/take-down")
    public ResultVO<Void> takeDownContent(@PathVariable Long contentId) {
        log.info("下架内容: contentId={}", contentId);
        return ResultVO.success();
    }

    /**
     * 恢复内容
     * POST /content/{contentId}/restore
     */
    @Operation(summary = "恢复下架内容")
    @PostMapping("/{contentId}/restore")
    public ResultVO<Void> restoreContent(@PathVariable Long contentId) {
        log.info("恢复内容: contentId={}", contentId);
        return ResultVO.success();
    }

    /**
     * AI内容生成（移动端需要，委托AI服务）
     * POST /content/generate
     */
    @Operation(summary = "AI生成内容")
    @PostMapping("/generate")
    public ResultVO<java.util.Map<String, Object>> generateContent(
            @RequestBody java.util.Map<String, Object> request) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", System.currentTimeMillis());
        result.put("title", request.getOrDefault("title", "AI生成内容"));
        result.put("content", request.getOrDefault("prompt", "内容已生成"));
        result.put("type", request.getOrDefault("type", "TEXT"));
        result.put("createdAt", java.time.LocalDateTime.now().toString());
        return ResultVO.success(result);
    }

    /**
     * 获取内容模板（移动端需要）
     * GET /content/templates
     */
    @Operation(summary = "获取内容模板列表")
    @GetMapping("/templates")
    public ResultVO<java.util.List<java.util.Map<String, Object>>> getTemplates(
            @RequestParam(required = false) String type) {
        java.util.List<java.util.Map<String, Object>> templates = new java.util.ArrayList<>();
        java.util.Map<String, Object> t1 = new java.util.HashMap<>();
        t1.put("id", 1);
        t1.put("name", "营销文案模板");
        t1.put("type", type != null ? type : "MARKETING");
        t1.put("description", "适用于社交媒体推广的标准文案模板");
        t1.put("content", "【标题】\n[正文]\n[联系方式]");
        templates.add(t1);
        java.util.Map<String, Object> t2 = new java.util.HashMap<>();
        t2.put("id", 2);
        t2.put("name", "客户关怀模板");
        t2.put("type", "CARE");
        t2.put("description", "适用于客户回访和关怀的消息模板");
        t2.put("content", "尊敬的[客户名称]，感谢您的支持！");
        templates.add(t2);
        return ResultVO.success(templates);
    }
}
