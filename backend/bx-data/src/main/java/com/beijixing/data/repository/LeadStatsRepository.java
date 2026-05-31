package com.beijixing.data.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.data.entity.LeadStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商机统计 Repository
 * 
 * @author 陈工（EMP-DATA-001）
 */
@Mapper
public interface LeadStatsRepository extends BaseMapper<LeadStats> {

    /**
     * 查询指定时间范围的商机统计数据
     */
    @Select("SELECT * FROM bx_lead_stats WHERE tenant_id = #{tenantId} " +
            "AND stat_date >= #{startDate} AND stat_date <= #{endDate} ORDER BY stat_date")
    List<LeadStats> findByTenantIdAndDateRange(
            @Param("tenantId") Long tenantId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

    /**
     * 查询商机转化汇总
     */
    @Select("SELECT SUM(new_leads) as newLeads, SUM(following_leads) as followingLeads, " +
            "SUM(converted_leads) as convertedLeads, SUM(valid_leads) as validLeads, " +
            "SUM(invalid_leads) as invalidLeads " +
            "FROM bx_lead_stats WHERE tenant_id = #{tenantId} AND stat_date = #{date}")
    LeadStats findConversionSummary(@Param("tenantId") Long tenantId, @Param("date") String date);
}
