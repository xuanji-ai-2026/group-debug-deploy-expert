package com.beijixing.data.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.data.entity.AccountStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 账号统计 Repository
 * 
 * @author 陈工（EMP-DATA-001）
 */
@Mapper
public interface AccountStatsRepository extends BaseMapper<AccountStats> {

    /**
     * 查询指定时间范围的账号统计数据
     */
    @Select("SELECT * FROM bx_account_stats WHERE tenant_id = #{tenantId} " +
            "AND stat_date >= #{startDate} AND stat_date <= #{endDate} ORDER BY stat_date")
    List<AccountStats> findByTenantIdAndDateRange(
            @Param("tenantId") Long tenantId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

    /**
     * 查询账号状态分布
     */
    @Select("SELECT status, COUNT(*) as count FROM bx_account_stats " +
            "WHERE tenant_id = #{tenantId} AND stat_date = #{date} GROUP BY status")
    List<AccountStats> findStatusDistribution(@Param("tenantId") Long tenantId, @Param("date") String date);
}
