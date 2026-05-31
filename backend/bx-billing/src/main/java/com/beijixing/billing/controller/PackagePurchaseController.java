package com.beijixing.billing.controller;

import com.beijixing.billing.dto.BillingOrderDTO;
import com.beijixing.billing.dto.PackagePurchaseDTO;
import com.beijixing.billing.service.PackagePurchaseService;
import com.beijixing.common.core.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 套餐购买控制器
 * BL-005: 套餐购买
 */
@RestController
@RequestMapping("/billing/package")
@RequiredArgsConstructor
public class PackagePurchaseController {
    
    private final PackagePurchaseService packageService;
    
    /**
     * 获取套餐配置列表
     */
    @GetMapping("/configs")
    public Result<Map<String, PackagePurchaseService.PackageConfig>> getPackageConfigs() {
        return Result.success(packageService.getPackageConfigs());
    }
    
    /**
     * 创建套餐购买订单
     */
    @PostMapping("/order")
    public Result<BillingOrderDTO> createPackageOrder(
            @RequestParam Long tenantId,
            @RequestParam Long userId,
            @RequestParam String packageType) {
        try {
            BillingOrderDTO order = packageService.createPackageOrder(tenantId, userId, packageType);
            return Result.success(order);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取用户套餐列表
     */
    @GetMapping("/user/{userId}")
    public Result<List<PackagePurchaseDTO>> getUserPackages(@PathVariable Long userId) {
        List<PackagePurchaseDTO> packages = packageService.getUserPackages(userId);
        return Result.success(packages);
    }
    
    /**
     * 获取用户生效中的套餐
     */
    @GetMapping("/user/{userId}/active")
    public Result<List<PackagePurchaseDTO>> getActivePackages(@PathVariable Long userId) {
        List<PackagePurchaseDTO> packages = packageService.getActivePackages(userId);
        return Result.success(packages);
    }
}
