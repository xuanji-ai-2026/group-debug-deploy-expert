package com.beijixing.social.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beijixing.common.core.Result;
import com.beijixing.social.entity.NurturingStrategy;
import com.beijixing.social.entity.SocialAccount;
import com.beijixing.social.service.AccountService;
import com.beijixing.social.service.NurturingExecutionEngine;
import com.beijixing.social.service.NurturingStrategyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/nurturing")
@RequiredArgsConstructor
public class NurturingStrategyController {

    private final NurturingStrategyService nurturingStrategyService;
    private final AccountService accountService;
    private final NurturingExecutionEngine nurturingExecutionEngine;

    @GetMapping("/strategies")
    public Result<List<NurturingStrategy>> listStrategies(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Integer enabled) {
        LambdaQueryWrapper<NurturingStrategy> wrapper = new LambdaQueryWrapper<>();
        if (accountId != null) {
            wrapper.eq(NurturingStrategy::getAccountId, accountId);
        }
        if (enabled != null) {
            wrapper.eq(NurturingStrategy::getEnabled, enabled);
        }
        wrapper.orderByDesc(NurturingStrategy::getCreateTime);
        List<NurturingStrategy> strategies = nurturingStrategyService.list(wrapper);
        return Result.success(strategies);
    }

    @GetMapping("/strategy/{id}")
    public Result<NurturingStrategy> getStrategy(@PathVariable Long id) {
        NurturingStrategy strategy = nurturingStrategyService.getById(id);
        if (strategy == null) {
            return Result.error("策略不存在");
        }
        return Result.success(strategy);
    }

    @PostMapping("/strategy")
    public Result<NurturingStrategy> createStrategy(@RequestBody NurturingStrategy strategy) {
        try {
            NurturingStrategy saved = nurturingStrategyService.saveStrategy(strategy);
            return Result.success(saved);
        } catch (Exception e) {
            log.error("创建养号策略失败: {}", e.getMessage(), e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    @PutMapping("/strategy/{id}")
    public Result<NurturingStrategy> updateStrategy(
            @PathVariable Long id,
            @RequestBody NurturingStrategy strategy) {
        strategy.setId(id);
        try {
            NurturingStrategy updated = nurturingStrategyService.saveStrategy(strategy);
            return Result.success(updated);
        } catch (Exception e) {
            log.error("更新养号策略失败: {}", e.getMessage(), e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    @PutMapping("/strategy/{id}/status")
    public Result<Boolean> toggleStatus(
            @PathVariable Long id,
            @RequestParam Integer enabled) {
        boolean success;
        if (enabled == 1) {
            success = nurturingStrategyService.enableStrategy(id);
        } else {
            success = nurturingStrategyService.disableStrategy(id);
            if (success) {
                nurturingExecutionEngine.stopExecution(id);
            }
        }
        return success ? Result.success(true) : Result.error("操作失败");
    }

    @DeleteMapping("/strategy/{id}")
    public Result<Boolean> deleteStrategy(@PathVariable Long id) {
        nurturingExecutionEngine.stopExecution(id);
        boolean success = nurturingStrategyService.removeById(id);
        return success ? Result.success(true) : Result.error("删除失败");
    }

    @GetMapping("/strategy/{id}/progress")
    public Result<Map<String, Object>> getProgress(@PathVariable Long id) {
        Map<String, Object> progress = nurturingExecutionEngine.getProgressAsMap(id);
        if (progress == null) {
            return Result.error("无进度信息");
        }
        return Result.success(progress);
    }

    @PostMapping("/strategy/{id}/start")
    public Result<Boolean> startStrategy(@PathVariable Long id) {
        boolean started = nurturingExecutionEngine.startExecution(id);
        return started ? Result.success(true) : Result.error("启动失败，请检查策略配置");
    }

    @PostMapping("/strategy/{id}/stop")
    public Result<Boolean> stopStrategy(@PathVariable Long id) {
        nurturingExecutionEngine.stopExecution(id);
        return Result.success(true);
    }

    @GetMapping("/account/{accountId}/status")
    public Result<Integer> getAccountNurturingStatus(@PathVariable Long accountId) {
        SocialAccount account = accountService.getById(accountId);
        if (account == null) {
            return Result.error("账号不存在");
        }
        return Result.success(account.getNurturingStatus());
    }

    @GetMapping("/templates")
    public Result<List<Map<String, Object>>> getTemplates() {
        List<Map<String, Object>> templates = List.of(
                Map.of(
                        "name", "新手养号",
                        "description", "适合新账号，温和提升活跃度",
                        "dailyTargets", Map.of(
                                "likeCount", 10,
                                "commentCount", 5,
                                "shareCount", 2,
                                "followCount", 3,
                                "browseDuration", 30
                        ),
                        "duration", 7,
                        "riskLevel", "LOW"
                ),
                Map.of(
                        "name", "快速养号",
                        "description", "适合急需提升权重的账号",
                        "dailyTargets", Map.of(
                                "likeCount", 30,
                                "commentCount", 15,
                                "shareCount", 8,
                                "followCount", 10,
                                "browseDuration", 60
                        ),
                        "duration", 14,
                        "riskLevel", "MEDIUM"
                ),
                Map.of(
                        "name", "稳定运营",
                        "description", "适合已养成的账号，保持活跃度",
                        "dailyTargets", Map.of(
                                "likeCount", 20,
                                "commentCount", 8,
                                "shareCount", 4,
                                "followCount", 5,
                                "browseDuration", 45
                        ),
                        "duration", 30,
                        "riskLevel", "LOW"
                )
        );
        return Result.success(templates);
    }
}
