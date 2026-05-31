package com.beijixing.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beijixing.tenant.entity.TenantResourceQuota;
import com.beijixing.tenant.mapper.TenantResourceQuotaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 租户资源配额服务
 * 实现配额检查、扣减、重置功能
 * 
 * @author bx-tenant
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantResourceQuotaService {

    private final TenantResourceQuotaMapper quotaMapper;

    // ==================== 资源类型常量（与数据库init_data一致） ====================
    public static final String RESOURCE_SOCIAL_ACCOUNT = "social_account";
    public static final String RESOURCE_DAILY_INTERCEPT = "daily_intercept_task";
    public static final String RESOURCE_DAILY_ACQUIRE = "daily_acquire_task";
    public static final String RESOURCE_MONTHLY_SMS = "monthly_sms";
    public static final String RESOURCE_AI_GENERATE = "ai_generate_times";
    public static final String RESOURCE_STORAGE_MB = "storage_mb";

    /**
     * 查询租户所有配额
     */
    public List<TenantResourceQuota> listByTenant(Long tenantId) {
        LambdaQueryWrapper<TenantResourceQuota> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantResourceQuota::getTenantId, tenantId);
        return quotaMapper.selectList(wrapper);
    }

    /**
     * 查询指定资源配额
     */
    public TenantResourceQuota getByTenantAndType(Long tenantId, String resourceType) {
        return quotaMapper.findByTenantAndType(tenantId, resourceType);
    }

    /**
     * 检查配额是否允许操作（不消耗配额）
     * @return true = 允许, false = 配额不足
     */
    public boolean checkQuota(Long tenantId, String resourceType) {
        TenantResourceQuota quota = getByTenantAndType(tenantId, resourceType);
        if (quota == null) {
            log.warn("配额不存在: tenantId={}, resourceType={}", tenantId, resourceType);
            return true; // 不存在的配额默许
        }
        if (quota.isExceeded()) {
            log.warn("配额不足: tenantId={}, resourceType={}, limit={}, used={}", 
                tenantId, resourceType, quota.getQuotaLimit(), quota.getUsedAmount());
            return false;
        }
        return true;
    }

    /**
     * 检查并扣减配额（原子操作，配额不足时拒绝）
     * @return true = 扣减成功, false = 配额不足
     */
    @Transactional
    public boolean tryConsume(Long tenantId, String resourceType, long amount) {
        // 先检查
        TenantResourceQuota quota = getByTenantAndType(tenantId, resourceType);
        if (quota == null) return true; // 不存在的配额默许
        if (quota.isExceeded()) return false;

        // 原子扣减（条件本身包含配额检查，防止超用）
        int updated = quotaMapper.incrementUsage(tenantId, resourceType, amount);
        if (updated <= 0) {
            log.warn("配额扣减失败（可能被其他请求占走）: tenantId={}, resourceType={}", tenantId, resourceType);
            return false;
        }

        log.debug("配额扣减成功: tenantId={}, resourceType={}, amount={}", tenantId, resourceType, amount);
        
        // 检查是否触发告警
        TenantResourceQuota updatedQuota = getByTenantAndType(tenantId, resourceType);
        if (updatedQuota != null && updatedQuota.isAlert()) {
            log.warn("租户配额告警: tenantId={}, resourceType={}, 使用率={}%", 
                tenantId, resourceType, 
                (updatedQuota.getUsedAmount() * 100.0 / updatedQuota.getQuotaLimit()));
        }
        return true;
    }

    /**
     * 返还配额（如任务取消、失败退回等场景）
     */
    @Transactional
    public void refund(Long tenantId, String resourceType, long amount) {
        LambdaQueryWrapper<TenantResourceQuota> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantResourceQuota::getTenantId, tenantId)
               .eq(TenantResourceQuota::getResourceType, resourceType);
        TenantResourceQuota quota = quotaMapper.selectOne(wrapper);
        if (quota == null) return;

        long newAmount = Math.max(0, quota.getUsedAmount() - amount);
        quota.setUsedAmount(newAmount);
        quotaMapper.updateById(quota);
        log.info("配额返还: tenantId={}, resourceType={}, amount={}, newUsed={}", 
            tenantId, resourceType, amount, newAmount);
    }

    /**
     * 检查并重置配额（定时任务调用，按重置周期）
     * 返回所有需要重置的资源类型列表
     */
    @Transactional
    public int resetExpiredQuotas() {
        LocalDateTime now = LocalDateTime.now();
        List<TenantResourceQuota> allQuotas = quotaMapper.selectList(null);
        int count = 0;
        for (TenantResourceQuota quota : allQuotas) {
            if (shouldReset(quota, now)) {
                quotaMapper.resetUsage(quota.getTenantId(), quota.getResourceType());
                count++;
                log.info("配额已重置: tenantId={}, resourceType={}", quota.getTenantId(), quota.getResourceType());
            }
        }
        return count;
    }

    /**
     * 判断配额是否应该重置
     */
    private boolean shouldReset(TenantResourceQuota quota, LocalDateTime now) {
        if (quota.getLastResetTime() == null) return false;
        if ("daily".equalsIgnoreCase(quota.getResetCycle())) {
            return now.toLocalDate().isAfter(quota.getLastResetTime().toLocalDate());
        }
        if ("monthly".equalsIgnoreCase(quota.getResetCycle())) {
            return now.getMonthValue() != quota.getLastResetTime().getMonthValue();
        }
        if ("yearly".equalsIgnoreCase(quota.getResetCycle())) {
            return now.getYear() != quota.getLastResetTime().getYear();
        }
        return false;
    }
}
