package com.beijixing.risk.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.beijixing.risk.entity.RiskRecord;
import com.beijixing.risk.repository.mapper.RiskMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 风控记录 Repository
 *
 * @author 林超 (EMP-SEC-001)
 */
@Repository
public class RiskRecordRepository extends ServiceImpl<RiskMapper, RiskRecord> {

    /**
     * 根据租户ID和时间范围查询风控记录
     */
    public List<RiskRecord> findByTenantAndTimeRange(Long tenantId, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<RiskRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskRecord::getTenantId, tenantId)
               .ge(RiskRecord::getCreateTime, startTime)
               .le(RiskRecord::getCreateTime, endTime)
               .orderByDesc(RiskRecord::getCreateTime);
        return this.list(wrapper);
    }

    /**
     * 根据账号ID查询风控记录
     */
    public List<RiskRecord> findByAccountId(Long accountId) {
        LambdaQueryWrapper<RiskRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskRecord::getAccountId, accountId)
               .orderByDesc(RiskRecord::getCreateTime)
               .last("LIMIT 100");
        return this.list(wrapper);
    }

    /**
     * 根据规则ID查询触发的记录
     */
    public List<RiskRecord> findByTriggeredRuleId(Long ruleId, LocalDateTime startTime) {
        LambdaQueryWrapper<RiskRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskRecord::getTriggeredRuleId, ruleId)
               .ge(RiskRecord::getCreateTime, startTime)
               .orderByDesc(RiskRecord::getCreateTime);
        return this.list(wrapper);
    }

    /**
     * 统计今日拦截次数
     */
    public Long countTodayBlocked(Long tenantId, Long accountId) {
        LambdaQueryWrapper<RiskRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskRecord::getTenantId, tenantId)
               .eq(accountId != null, RiskRecord::getAccountId, accountId)
               .eq(RiskRecord::getAction, "BLOCK")
               .ge(RiskRecord::getCreateTime, LocalDateTime.now().withHour(0).withMinute(0).withSecond(0));
        return this.count(wrapper);
    }

    /**
     * 查询未处理的预警
     */
    public List<RiskRecord> findUnhandledRecords() {
        LambdaQueryWrapper<RiskRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskRecord::getStatus, 0)
               .ne(RiskRecord::getAction, "PASS")
               .orderByDesc(RiskRecord::getCreateTime)
               .last("LIMIT 100");
        return this.list(wrapper);
    }
}
