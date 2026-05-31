package com.beijixing.risk.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.beijixing.risk.entity.RiskStrategy;
import com.beijixing.risk.repository.mapper.RiskStrategyMapper;
import org.springframework.stereotype.Repository;

/**
 * 风控策略 Repository
 *
 * @author 林超 (EMP-SEC-001)
 */
@Repository
public class RiskStrategyRepository extends ServiceImpl<RiskStrategyMapper, RiskStrategy> {
}
