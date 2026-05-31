package com.beijixing.risk.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.beijixing.risk.entity.RiskRule;
import com.beijixing.risk.repository.mapper.RiskRuleMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 风控规则 Repository
 *
 * @author 林超 (EMP-SEC-001)
 */
@Repository
public class RiskRuleRepository extends ServiceImpl<RiskRuleMapper, RiskRule> {

    /**
     * 获取所有启用的规则
     */
    public List<RiskRule> findAllActive() {
        return this.list(
            new LambdaQueryWrapper<RiskRule>()
                .eq(RiskRule::getStatus, 1)
                .and(w -> w
                    .le(RiskRule::getEffectiveTime, LocalDateTime.now())
                    .ge(RiskRule::getExpireTime, LocalDateTime.now())
                    .or()
                    .isNull(RiskRule::getEffectiveTime)
                    .isNull(RiskRule::getExpireTime)
                )
                .orderByAsc(RiskRule::getPriority)
        );
    }

    /**
     * 根据规则编码查询
     */
    public RiskRule findByCode(String ruleCode) {
        LambdaQueryWrapper<RiskRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskRule::getRuleCode, ruleCode);
        return this.getOne(wrapper);
    }

    /**
     * 根据规则类型查询
     */
    public List<RiskRule> findByType(String ruleType) {
        LambdaQueryWrapper<RiskRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskRule::getRuleType, ruleType)
               .eq(RiskRule::getStatus, 1)
               .orderByAsc(RiskRule::getPriority);
        return this.list(wrapper);
    }
}
