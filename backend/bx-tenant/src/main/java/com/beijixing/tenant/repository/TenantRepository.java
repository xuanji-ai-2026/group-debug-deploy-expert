package com.beijixing.tenant.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beijixing.tenant.entity.Tenant;
import com.beijixing.tenant.repository.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 租户数据访问仓库
 *
 * @author bx-tenant
 */
@Repository
@RequiredArgsConstructor
public class TenantRepository {

    private final TenantMapper tenantMapper;

    /**
     * 根据ID查询租户
     *
     * @param id 租户ID
     * @return 租户实体
     */
    public Tenant findById(Long id) {
        return tenantMapper.selectById(id);
    }

    /**
     * 根据租户编码查询
     *
     * @param tenantCode 租户编码
     * @return 租户实体
     */
    public Tenant findByCode(String tenantCode) {
        return tenantMapper.selectByTenantCode(tenantCode);
    }

    /**
     * 保存租户
     *
     * @param tenant 租户实体
     * @return 租户ID
     */
    public Long save(Tenant tenant) {
        tenantMapper.insert(tenant);
        return tenant.getId();
    }

    /**
     * 更新租户
     *
     * @param tenant 租户实体
     */
    public void update(Tenant tenant) {
        tenantMapper.updateById(tenant);
    }

    /**
     * 根据手机号查询租户
     *
     * @param phone 手机号
     * @return 租户实体
     */
    public Tenant findByPhone(String phone) {
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tenant::getContactPhone, phone)
               .eq(Tenant::getDeleted, 0);
        return tenantMapper.selectOne(wrapper);
    }

    /**
     * 查询待审核租户列表
     *
     * @return 待审核租户列表
     */
    public List<Tenant> findPendingTenants() {
        return tenantMapper.selectPendingTenants();
    }

    /**
     * 批量更新租户状态
     *
     * @param ids 租户ID列表
     * @param status 目标状态
     * @param updateBy 更新人
     * @return 影响行数
     */
    public int batchUpdateStatus(List<Long> ids, Integer status, Long updateBy) {
        return tenantMapper.batchUpdateStatus(ids, status, updateBy);
    }
}
