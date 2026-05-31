package com.beijixing.risk.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.risk.entity.RiskRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 风控记录Mapper
 *
 * @author 林超 (EMP-SEC-001)
 */
@Mapper
public interface RiskMapper extends BaseMapper<RiskRecord> {

    /**
     * 查询账号在指定时间内的风险记录
     */
    @Select("SELECT * FROM risk_record WHERE account_id = #{accountId} AND create_time >= #{startTime} ORDER BY create_time DESC")
    List<RiskRecord> findByAccountIdAndTime(@Param("accountId") Long accountId, @Param("startTime") LocalDateTime startTime);

    /**
     * 查询高风险记录
     */
    @Select("SELECT * FROM risk_record WHERE risk_score < 60 AND status = 0 ORDER BY create_time DESC LIMIT 100")
    List<RiskRecord> findHighRiskRecords();

    /**
     * 统计各风险等级的数量
     */
    @Select("SELECT risk_level, COUNT(*) as count FROM risk_record WHERE tenant_id = #{tenantId} AND create_time >= #{startTime} GROUP BY risk_level")
    List<Map<String, Object>> countByRiskLevel(@Param("tenantId") Long tenantId, @Param("startTime") LocalDateTime startTime);

    /**
     * 统计各操作类型的数量
     */
    @Select("SELECT operation_type, COUNT(*) as count FROM risk_record WHERE tenant_id = #{tenantId} AND create_time >= #{startTime} GROUP BY operation_type")
    List<Map<String, Object>> countByOperationType(@Param("tenantId") Long tenantId, @Param("startTime") LocalDateTime startTime);
}
