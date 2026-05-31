package com.beijixing.risk.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.risk.entity.RiskScore;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风控评分Mapper
 *
 * @author 林超 (EMP-SEC-001)
 */
@Mapper
public interface RiskScoreMapper extends BaseMapper<RiskScore> {
}
