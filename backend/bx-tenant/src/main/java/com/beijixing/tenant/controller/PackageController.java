package com.beijixing.tenant.controller;

import com.beijixing.tenant.dto.PackageChangeRequest;
import com.beijixing.tenant.entity.TenantPackage;
import com.beijixing.tenant.service.PackageService;
import com.beijixing.tenant.vo.PackageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 套餐管理控制器
 * 提供套餐购买/变更/退订等功能接口
 *
 * @author bx-tenant
 */
@Slf4j
@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;

    // ==================== TM-005 套餐管理 ====================

    /**
     * 购买/变更套餐
     * POST /api/v1/packages/purchase
     *
     * @param request 套餐变更请求
     * @return 统一响应
     */
    @PostMapping("/packages/purchase")
    public ResponseEntity<Map<String, Object>> purchasePackage(@Valid @RequestBody PackageChangeRequest request) {
        log.info("收到套餐购买请求，租户ID：{}，套餐ID：{}", request.getTenantId(), request.getPackageId());
        TenantPackage pkg = packageService.purchasePackage(request);
        return success("套餐购买成功", pkg);
    }

    /**
     * 获取租户的套餐列表
     * GET /api/v1/tenants/{tenantId}/packages
     *
     * @param tenantId 租户ID
     * @return 套餐列表
     */
    @GetMapping("/tenants/{tenantId}/packages")
    public ResponseEntity<Map<String, Object>> getTenantPackages(@PathVariable Long tenantId) {
        List<PackageVO> packages = packageService.getTenantPackages(tenantId);
        return successData(packages);
    }

    /**
     * 获取租户当前生效的套餐
     * GET /api/v1/tenants/{tenantId}/packages/current
     *
     * @param tenantId 租户ID
     * @return 当前套餐
     */
    @GetMapping("/tenants/{tenantId}/packages/current")
    public ResponseEntity<Map<String, Object>> getCurrentPackage(@PathVariable Long tenantId) {
        PackageVO currentPackage = packageService.getCurrentPackage(tenantId);
        if (currentPackage == null) {
            return fail("当前没有生效的套餐", 20005);
        }
        return successData(currentPackage);
    }

    /**
     * 退订套餐
     * DELETE /api/v1/packages/{id}
     *
     * @param id 租户套餐记录ID
     * @param operatorId 操作人ID
     * @return 统一响应
     */
    @DeleteMapping("/packages/{id}")
    public ResponseEntity<Map<String, Object>> cancelPackage(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long operatorId) {
        log.info("收到套餐退订请求，套餐记录ID：{}", id);
        packageService.cancelPackage(id, operatorId);
        return success("套餐退订成功");
    }

    // ==================== 统一响应封装 ====================


    private ResponseEntity<Map<String, Object>> success(String message) {
        return success(message, null);
    }

    private ResponseEntity<Map<String, Object>> success(String message, Object data) {
        return successData(message, data);
    }

    private ResponseEntity<Map<String, Object>> successData(Object data) {
        return successData("success", data);
    }

    private ResponseEntity<Map<String, Object>> successData(String message, Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", message);
        result.put("data", data);
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<Map<String, Object>> fail(String message, Integer code) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code != null ? code : 20000);
        result.put("message", message);
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }
}
