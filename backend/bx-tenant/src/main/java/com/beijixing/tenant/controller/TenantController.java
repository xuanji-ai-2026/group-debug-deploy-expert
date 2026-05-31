package com.beijixing.tenant.controller;

import com.beijixing.tenant.dto.*;
import com.beijixing.tenant.service.TenantService;
import com.beijixing.tenant.vo.TenantVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 租户管理控制器
 * 提供租户注册、审核、状态管理等功能接口
 *
 * @author bx-tenant
 */
@Slf4j
@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    // ==================== TM-001 租户注册流程 ====================

    /**
     * 注册租户
     * POST /api/v1/tenants
     *
     * @param request 创建租户请求
     * @return 统一响应
     */
    @PostMapping("/tenants")
    public ResponseEntity<Map<String, Object>> registerTenant(@Valid @RequestBody CreateTenantRequest request) {
        log.info("收到租户注册请求，租户名称：{}", request.getTenantName());
        tenantService.createTenant(request);
        return success("租户注册成功，等待审核");
    }

    // ==================== TM-002 租户审核 ====================

    /**
     * 审核租户（管理端）
     * POST /api/v1/admin/tenants/{id}/audit
     *
     * @param id 租户ID
     * @param request 审核请求
     * @return 统一响应
     */
    @PostMapping("/admin/tenants/{id}/audit")
    public ResponseEntity<Map<String, Object>> auditTenant(
            @PathVariable Long id,
            @Valid @RequestBody TenantAuditRequest request) {
        log.info("收到租户审核请求，租户ID：{}", id);
        tenantService.auditTenant(id, request);
        return success("租户审核完成");
    }

    // ==================== TM-003 批量审核 ====================

    /**
     * 批量审核租户（管理端）
     * POST /api/v1/admin/tenants/batch-audit
     *
     * @param request 批量审核请求
     * @return 统一响应
     */
    @PostMapping("/admin/tenants/batch-audit")
    public ResponseEntity<Map<String, Object>> batchAuditTenants(@Valid @RequestBody BatchAuditRequest request) {
        log.info("收到批量审核请求，租户数量：{}", request.getTenantIds().size());
        int successCount = tenantService.batchAuditTenants(request);
        Map<String, Object> data = new HashMap<>();
        data.put("successCount", successCount);
        data.put("totalCount", request.getTenantIds().size());
        return success("批量审核完成，成功 {} 条".formatted(successCount), data);
    }

    // ==================== TM-004 租户状态管理 ====================

    /**
     * 变更租户状态（启用/禁用/注销）
     * POST /api/v1/admin/tenants/{id}/status
     *
     * @param id 租户ID
     * @param request 状态变更请求
     * @return 统一响应
     */
    @PostMapping("/admin/tenants/{id}/status")
    public ResponseEntity<Map<String, Object>> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody TenantStatusChangeRequest request) {
        log.info("收到租户状态变更请求，租户ID：{}，目标状态：{}", id, request.getTargetStatus());
        tenantService.changeTenantStatus(id, request);
        return success("状态变更成功");
    }

    // ==================== 基础查询接口 ====================

    /**
     * 获取当前登录用户的租户信息
     * GET /api/v1/tenants/me
     *
     * @param userId 用户ID（从请求头或Token获取，此处简化用参数）
     * @return 租户信息
     */
    @GetMapping("/tenants/me")
    public ResponseEntity<Map<String, Object>> getCurrentTenant(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        TenantVO tenant = tenantService.getTenantByUserId(userId);
        if (tenant == null) {
            return fail("租户信息不存在", 20001);
        }
        return successData(tenant);
    }

    /**
     * 获取租户详情
     * GET /api/v1/tenants/{id}
     *
     * @param id 租户ID
     * @return 租户信息
     */
    @GetMapping("/tenants/{id}")
    public ResponseEntity<Map<String, Object>> getTenant(@PathVariable Long id) {
        TenantVO tenant = tenantService.getTenantById(id);
        if (tenant == null) {
            return fail("租户不存在", 20001);
        }
        return successData(tenant);
    }

    /**
     * 获取租户列表（分页）
     * GET /api/v1/admin/tenants
     *
     * @param page 页码
     * @param size 每页数量
     * @param keyword 搜索关键词
     * @param status 状态筛选
     * @return 租户列表
     */
    @GetMapping("/admin/tenants")
    public ResponseEntity<Map<String, Object>> listTenants(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        List<TenantVO> tenants = tenantService.listTenants(page, size, keyword, status);
        return successData(tenants);
    }

    /**
     * 获取待审核租户列表
     * GET /api/v1/admin/tenants/pending
     *
     * @return 待审核租户列表
     */
    @GetMapping("/admin/tenants/pending")
    public ResponseEntity<Map<String, Object>> listPendingTenants() {
        List<TenantVO> tenants = tenantService.listPendingTenants();
        return successData(tenants);
    }

    /**
     * 更新租户信息
     * PUT /api/v1/tenants/{id}
     *
     * @param id 租户ID
     * @param request 更新请求
     * @return 统一响应
     */
    @PutMapping("/tenants/{id}")
    public ResponseEntity<Map<String, Object>> updateTenant(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTenantRequest request) {
        log.info("收到更新租户请求，租户ID：{}", id);
        tenantService.updateTenant(id, request);
        return success("租户信息更新成功");
    }

    // ==================== 统一响应封装 ====================

    /**
     * 成功响应（无数据）
     */
    private ResponseEntity<Map<String, Object>> success(String message) {
        return successData(message, null);
    }

    /**
     * 成功响应（带消息）
     */
    private ResponseEntity<Map<String, Object>> success(String message, Object data) {
        return successData(message, data);
    }

    /**
     * 成功响应（带数据）
     */
    private ResponseEntity<Map<String, Object>> successData(Object data) {
        return successData("success", data);
    }

    /**
     * 成功响应（带消息和数据）
     */
    private ResponseEntity<Map<String, Object>> successData(String message, Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", message);
        result.put("data", data);
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }

    /**
     * 失败响应
     */
    private ResponseEntity<Map<String, Object>> fail(String message, Integer code) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code != null ? code : 20000);
        result.put("message", message);
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }
}
