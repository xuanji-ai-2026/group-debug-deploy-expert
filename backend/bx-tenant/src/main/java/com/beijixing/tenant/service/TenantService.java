package com.beijixing.tenant.service;

import com.beijixing.tenant.dto.*;
import com.beijixing.tenant.entity.Tenant;
import com.beijixing.tenant.vo.TenantVO;

import java.util.List;

/**
 * 租户服务接口
 * 功能：TM-001 租户注册、TM-002 租户审核、TM-003 批量审核、TM-004 租户状态管理
 *
 * @author bx-tenant
 */
public interface TenantService {

    // ==================== TM-001 租户注册流程 ====================

    /**
     * 创建租户（租户注册）
     *
     * @param request 创建租户请求
     * @return 租户信息
     */
    Tenant createTenant(CreateTenantRequest request);

    /**
     * 生成租户编码
     *
     * @return 租户编码
     */
    String generateTenantCode();

    // ==================== TM-002 租户审核 ====================

    /**
     * 审核租户
     *
     * @param tenantId 租户ID
     * @param request 审核请求
     */
    void auditTenant(Long tenantId, TenantAuditRequest request);

    // ==================== TM-003 批量审核 ====================

    /**
     * 批量审核租户
     *
     * @param request 批量审核请求
     * @return 成功数量
     */
    int batchAuditTenants(BatchAuditRequest request);

    // ==================== TM-004 租户状态管理 ====================

    /**
     * 变更租户状态（启用/禁用/注销）
     *
     * @param tenantId 租户ID
     * @param request 状态变更请求
     */
    void changeTenantStatus(Long tenantId, TenantStatusChangeRequest request);

    // ==================== 基础查询 ====================

    /**
     * 获取租户详情
     *
     * @param tenantId 租户ID
     * @return 租户视图对象
     */
    TenantVO getTenantById(Long tenantId);

    /**
     * 获取当前登录用户的租户信息
     *
     * @param userId 用户ID
     * @return 租户视图对象
     */
    TenantVO getTenantByUserId(Long userId);

    /**
     * 获取租户列表（分页）
     *
     * @param page 页码
     * @param size 每页数量
     * @param keyword 搜索关键词
     * @param status 状态筛选
     * @return 租户列表
     */
    List<TenantVO> listTenants(Integer page, Integer size, String keyword, Integer status);

    /**
     * 获取待审核租户列表
     *
     * @return 待审核租户列表
     */
    List<TenantVO> listPendingTenants();

    /**
     * 更新租户信息
     *
     * @param tenantId 租户ID
     * @param request 更新请求
     */
    void updateTenant(Long tenantId, UpdateTenantRequest request);

    /**
     * 根据租户编码获取租户
     *
     * @param tenantCode 租户编码
     * @return 租户实体
     */
    Tenant getTenantByCode(String tenantCode);
}
