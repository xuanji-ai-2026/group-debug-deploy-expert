package com.beijixing.risk.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.risk.entity.RiskAlert;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风控预警Mapper
 *
 * @author 林超 (EMP-SEC-001)
 */
@Mapper
public interface RiskAlertMapper extends BaseMapper<RiskAlert> {
}
