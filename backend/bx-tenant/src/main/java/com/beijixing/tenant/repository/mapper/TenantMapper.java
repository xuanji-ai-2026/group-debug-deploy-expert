package com.beijixing.tenant.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.tenant.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 租户 Mapper 接口
 *
 * @author bx-tenant
 */
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {

    /**
     * 根据租户编码查询
     *
     * @param tenantCode 租户编码
     * @return Tenant
     */
    Tenant selectByTenantCode(@Param("tenantCode") String tenantCode);

    /**
     * 查询待审核租户列表
     *
     * @return 待审核租户列表
     */
    List<Tenant> selectPendingTenants();

    /**
     * 批量更新状态
     *
     * @param ids 租户ID列表
     * @param status 目标状态
     * @param updateBy 更新人
     * @return 影响行数
     */
    int batchUpdateStatus(@Param("ids") List<Long> ids,
                         @Param("status") Integer status,
                         @Param("updateBy") Long updateBy);
}
