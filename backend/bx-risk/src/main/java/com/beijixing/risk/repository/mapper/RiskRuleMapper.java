package com.beijixing.risk.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.risk.entity.RiskRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风控规则Mapper
 *
 * @author 林超 (EMP-SEC-001)
 */
@Mapper
public interface RiskRuleMapper extends BaseMapper<RiskRule> {
}
