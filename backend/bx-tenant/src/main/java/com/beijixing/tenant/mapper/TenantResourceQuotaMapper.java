package com.beijixing.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.tenant.entity.TenantResourceQuota;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 租户资源配额Mapper
 * @author bx-tenant
 */
@Mapper
public interface TenantResourceQuotaMapper extends BaseMapper<TenantResourceQuota> {

    /**
     * 查询指定租户的指定资源配额
     */
    @Select("SELECT * FROM tenant_resource_quota WHERE tenant_id = #{tenantId} AND resource_type = #{resourceType} AND is_deleted = 0")
    TenantResourceQuota findByTenantAndType(@Param("tenantId") Long tenantId, @Param("resourceType") String resourceType);

    /**
     * 原子递增已使用量（并发安全）
     */
    @Update("UPDATE tenant_resource_quota SET used_amount = used_amount + #{increment}, updated_at = NOW() WHERE tenant_id = #{tenantId} AND resource_type = #{resourceType} AND is_deleted = 0 AND (quota_limit = 0 OR used_amount + #{increment} <= quota_limit)")
    int incrementUsage(@Param("tenantId") Long tenantId, @Param("resourceType") String resourceType, @Param("increment") long increment);

    /**
     * 重置已使用量为0
     */
    @Update("UPDATE tenant_resource_quota SET used_amount = 0, last_reset_time = NOW(), updated_at = NOW() WHERE tenant_id = #{tenantId} AND resource_type = #{resourceType} AND is_deleted = 0")
    int resetUsage(@Param("tenantId") Long tenantId, @Param("resourceType") String resourceType);
}
