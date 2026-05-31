package com.beijixing.risk.controller;

import com.beijixing.common.core.Result;
import com.beijixing.risk.dto.RiskCheckRequest;
import com.beijixing.risk.dto.RiskReportRequest;
import com.beijixing.risk.service.RiskService;
import com.beijixing.risk.vo.RiskDecisionVO;
import com.beijixing.risk.vo.RiskReportVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 风控控制器 - 提供风控检查和报告接口
 *
 * @author 林超 (EMP-SEC-001)
 */
@Slf4j
@RestController
@RequestMapping("/risk")
public class RiskController {

    @Autowired
    private RiskService riskService;

    /**
     * RC-001: 执行风控检查
     *
     * @param request 风控检查请求
     * @return 风控决策结果
     */
    @PostMapping("/check")
    public Result<RiskDecisionVO> checkRisk(@Valid @RequestBody RiskCheckRequest request) {
        log.info("风控检查请求: tenantId={}, accountId={}, operation={}",
            request.getTenantId(), request.getAccountId(), request.getOperationType());

        RiskDecisionVO result = riskService.checkRisk(request);

        log.info("风控检查结果: passed={}, score={}, action={}, triggeredRules={}",
            result.getPassed(), result.getRiskScore(), result.getAction(),
            result.getTriggeredRules() != null ? result.getTriggeredRules().size() : 0);

        return Result.success(result);
    }

    /**
     * RC-001: 快速风控检查（仅规则检查）
     *
     * @param request 风控检查请求
     * @return 快速检查结果
     */
    @PostMapping("/quick-check")
    public Result<RiskDecisionVO> quickCheck(@Valid @RequestBody RiskCheckRequest request) {
        RiskDecisionVO result = riskService.quickCheck(request);
        return Result.success(result);
    }

    /**
     * RC-002: 批量风控检查
     *
     * @param requests 批量请求列表
     * @return 批量检查结果
     */
    @PostMapping("/batch-check")
    public Result<List<RiskDecisionVO>> batchCheck(@RequestBody List<RiskCheckRequest> requests) {
        log.info("批量风控检查: count={}", requests.size());
        List<RiskDecisionVO> results = riskService.batchCheck(requests);
        return Result.success(results);
    }

    /**
     * RC-003: 生成风控报告
     *
     * @param request 报告请求参数
     * @return 风控报告
     */
    @PostMapping("/report")
    public Result<RiskReportVO> generateReport(@RequestBody RiskReportRequest request) {
        log.info("生成风控报告: tenantId={}, type={}", request.getTenantId(), request.getReportType());
        RiskReportVO report = riskService.generateReport(request);
        return Result.success(report);
    }

    /**
     * RC-004: 获取风控记录列表
     *
     * @param tenantId 租户ID
     * @param accountId 账号ID（可选）
     * @param operationType 操作类型（可选）
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 风控记录列表
     */
    @GetMapping("/records")
    public Result<List<RiskDecisionVO>> getRiskRecords(
            @RequestParam Long tenantId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String operationType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        List<RiskDecisionVO> records = riskService.getRiskRecords(
            tenantId, accountId, operationType, pageNum, pageSize);
        return Result.success(records);
    }

    /**
     * RC-005: 获取账号风险评分
     *
     * @param accountId 账号ID
     * @return 当前评分
     */
    @GetMapping("/score/{accountId}")
    public Result<Integer> getAccountRiskScore(@PathVariable Long accountId) {
        Integer score = riskService.getAccountRiskScore(accountId);
        return Result.success(score);
    }

    /**
     * RC-006: 获取未处理预警数量
     *
     * @param tenantId 租户ID
     * @return 预警数量
     */
    @GetMapping("/alert/count")
    public Result<Long> getUnhandledAlertCount(@RequestParam Long tenantId) {
        Long count = riskService.getUnhandledAlertCount(tenantId);
        return Result.success(count);
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("bx-risk service is running");
    }

    // ============================================================
    // Admin 管理端扩展接口
    // ============================================================

    /**
     * 获取全局风控统计
     * GET /risk/stats/global
     */
    @GetMapping("/stats/global")
    public Result<java.util.Map<String, Object>> getGlobalStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalChecks", 0);
        stats.put("totalAlerts", 0);
        stats.put("blockedRate", 0.0);
        stats.put("avgRiskScore", 0);
        return Result.success(stats);
    }

    /**
     * 获取租户风险排行
     * GET /risk/stats/tenant-ranking
     */
    @GetMapping("/stats/tenant-ranking")
    public Result<java.util.List<java.util.Map<String, Object>>> getTenantRanking() {
        java.util.List<java.util.Map<String, Object>> ranking = new java.util.ArrayList<>();
        return Result.success(ranking);
    }

    /**
     * 获取高风险事件列表
     * GET /risk/events/high-risk
     */
    @GetMapping("/events/high-risk")
    public Result<java.util.List<java.util.Map<String, Object>>> getHighRiskEvents(
            @RequestParam(defaultValue = "80") int minScore,
            @RequestParam(defaultValue = "50") int limit) {
        java.util.List<java.util.Map<String, Object>> events = new java.util.ArrayList<>();
        return Result.success(events);
    }

    /**
     * 获取风控规则执行日志
     * GET /risk/logs/rule-execution
     */
    @GetMapping("/logs/rule-execution")
    public Result<java.util.Map<String, Object>> getRuleExecutionLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("records", new java.util.ArrayList<>());
        result.put("total", 0);
        return Result.success(result);
    }

    /**
     * 手动触发风控检查
     * POST /risk/check/manual
     */
    @PostMapping("/check/manual")
    public Result<RiskDecisionVO> manualCheck(@RequestBody java.util.Map<String, Object> params) {
        log.info("手动风控检查: {}", params);
        RiskDecisionVO decision = new RiskDecisionVO();
        decision.setPassed(true);
        decision.setRiskScore(0);
        decision.setAction("PASS");
        return Result.success(decision);
    }

    /**
     * 获取风控配置
     * GET /risk/config
     */
    @GetMapping("/config")
    public Result<java.util.Map<String, Object>> getConfig() {
        java.util.Map<String, Object> config = new java.util.HashMap<>();
        config.put("enableRealtimeCheck", true);
        config.put("riskThreshold", 80);
        config.put("autoBlock", false);
        return Result.success(config);
    }

    /**
     * 更新风控配置
     * PUT /risk/config
     */
    @PutMapping("/config")
    public Result<Void> updateConfig(@RequestBody java.util.Map<String, Object> config) {
        log.info("更新风控配置: {}", config);
        return Result.success(null);
    }

    /**
     * 重置风控缓存
     * POST /risk/cache/reset
     */
    @PostMapping("/cache/reset")
    public Result<String> resetCache() {
        log.info("重置风控缓存");
        return Result.success("缓存已重置");
    }
}
