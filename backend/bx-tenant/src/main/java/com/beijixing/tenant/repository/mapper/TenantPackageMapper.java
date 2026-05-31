package com.beijixing.tenant.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.tenant.entity.TenantPackage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 租户套餐 Mapper 接口
 *
 * @author bx-tenant
 */
@Mapper
public interface TenantPackageMapper extends BaseMapper<TenantPackage> {

    /**
     * 查询租户的所有套餐记录
     *
     * @param tenantId 租户ID
     * @return 套餐列表
     */
    List<TenantPackage> selectByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 查询租户当前生效的套餐
     *
     * @param tenantId 租户ID
     * @return 当前生效套餐
     */
    TenantPackage selectCurrentByTenantId(@Param("tenantId") Long tenantId);
}
