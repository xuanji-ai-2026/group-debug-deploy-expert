package com.beijixing.billing.controller;

import com.beijixing.billing.entity.BillingConfig;
import com.beijixing.billing.service.BillingConfigService;
import com.beijixing.billing.service.ReconciliationService;
import com.beijixing.common.core.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 财务对账与配置控制器
 * BL-009: 财务对账API
 * BL-010: 扣点标准配置API
 */
@RestController
@RequestMapping("/billing/finance")
@RequiredArgsConstructor
public class FinanceController {
    
    private final ReconciliationService reconciliationService;
    private final BillingConfigService configService;
    
    /**
     * 获取对账数据
     * BL-009: 财务对账API
     */
    @GetMapping("/reconciliation")
    public Result<Map<String, Object>> getReconciliationData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long tenantId) {
        Map<String, Object> data = reconciliationService.getReconciliationData(startDate, endDate, tenantId);
        return Result.success(data);
    }
    
    /**
     * 获取所有计费配置
     * BL-010: 扣点标准配置API
     */
    @GetMapping("/configs")
    public Result<List<BillingConfig>> getAllConfigs() {
        List<BillingConfig> configs = configService.getAllConfigs();
        return Result.success(configs);
    }
    
    /**
     * 根据类型获取配置
     */
    @GetMapping("/configs/type/{configType}")
    public Result<List<BillingConfig>> getConfigsByType(@PathVariable String configType) {
        List<BillingConfig> configs = configService.getConfigsByType(configType);
        return Result.success(configs);
    }
    
    /**
     * 更新配置
     */
    @PutMapping("/configs/{configId}")
    public Result<Void> updateConfig(
            @PathVariable Long configId,
            @RequestParam String configValue) {
        boolean success = configService.updateConfig(configId, configValue);
        return success ? Result.success() : Result.error("更新失败");
    }
    
    /**
     * 新增配置
     */
    @PostMapping("/configs")
    public Result<Void> addConfig(@RequestBody BillingConfig config) {
        boolean success = configService.addConfig(config);
        return success ? Result.success() : Result.error("添加失败，配置编码可能已存在");
    }
    
    /**
     * 删除配置
     */
    @DeleteMapping("/configs/{configId}")
    public Result<Void> deleteConfig(@PathVariable Long configId) {
        boolean success = configService.deleteConfig(configId);
        return success ? Result.success() : Result.error("删除失败");
    }
}
