package com.beijixing.tenant.service;

import com.beijixing.tenant.dto.PackageChangeRequest;
import com.beijixing.tenant.entity.TenantPackage;
import com.beijixing.tenant.vo.PackageVO;

import java.util.List;

/**
 * 套餐服务接口
 * 功能：TM-005 套餐管理
 *
 * @author bx-tenant
 */
public interface PackageService {

    /**
     * 购买/变更套餐
     *
     * @param request 套餐变更请求
     * @return 租户套餐记录
     */
    TenantPackage purchasePackage(PackageChangeRequest request);

    /**
     * 获取租户的套餐列表
     *
     * @param tenantId 租户ID
     * @return 套餐列表
     */
    List<PackageVO> getTenantPackages(Long tenantId);

    /**
     * 获取租户当前生效的套餐
     *
     * @param tenantId 租户ID
     * @return 当前套餐视图对象
     */
    PackageVO getCurrentPackage(Long tenantId);

    /**
     * 退订套餐
     *
     * @param tenantPackageId 租户套餐记录ID
     * @param operatorId 操作人ID
     */
    void cancelPackage(Long tenantPackageId, Long operatorId);

    /**
     * 根据ID获取套餐记录
     *
     * @param id 套餐记录ID
     * @return 租户套餐实体
     */
    TenantPackage getPackageById(Long id);
}
