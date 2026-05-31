package com.beijixing.risk.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.beijixing.risk.entity.RiskAlert;
import com.beijixing.risk.repository.mapper.RiskAlertMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 风控预警 Repository
 *
 * @author 林超 (EMP-SEC-001)
 */
@Repository
public class RiskAlertRepository extends ServiceImpl<RiskAlertMapper, RiskAlert> {

    /**
     * 查询租户未处理的预警
     */
    public List<RiskAlert> findUnhandledByTenant(Long tenantId) {
        LambdaQueryWrapper<RiskAlert> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskAlert::getTenantId, tenantId)
               .eq(RiskAlert::getStatus, 0)
               .orderByDesc(RiskAlert::getCreateTime);
        return this.list(wrapper);
    }

    /**
     * 统计各级别预警数量
     */
    public long countByLevel(Long tenantId, Integer alertLevel) {
        LambdaQueryWrapper<RiskAlert> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskAlert::getTenantId, tenantId)
               .eq(alertLevel != null, RiskAlert::getAlertLevel, alertLevel)
               .eq(RiskAlert::getStatus, 0);
        return this.count(wrapper);
    }

    /**
     * 查询严重级别预警
     */
    public List<RiskAlert> findCriticalAlerts(Long tenantId) {
        LambdaQueryWrapper<RiskAlert> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskAlert::getTenantId, tenantId)
               .eq(RiskAlert::getAlertLevel, 3)  // 严重级别
               .eq(RiskAlert::getStatus, 0)
               .ge(RiskAlert::getCreateTime, LocalDateTime.now().minusDays(1));
        return this.list(wrapper);
    }
}
