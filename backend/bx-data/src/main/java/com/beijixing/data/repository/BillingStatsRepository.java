package com.beijixing.data.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.data.entity.BillingStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 消费统计 Repository
 * 
 * @author 陈工（EMP-DATA-001）
 */
@Mapper
public interface BillingStatsRepository extends BaseMapper<BillingStats> {

    /**
     * 查询指定时间范围的消费统计数据
     */
    @Select("SELECT * FROM bx_billing_stats WHERE tenant_id = #{tenantId} " +
            "AND stat_date >= #{startDate} AND stat_date <= #{endDate} ORDER BY stat_date")
    List<BillingStats> findByTenantIdAndDateRange(
            @Param("tenantId") Long tenantId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

    /**
     * 查询消费汇总
     */
    @Select("SELECT SUM(points_consumed) as pointsConsumed, SUM(amount_consumed) as amountConsumed " +
            "FROM bx_billing_stats WHERE tenant_id = #{tenantId} AND stat_date = #{date}")
    BillingStats findBillingSummary(@Param("tenantId") Long tenantId, @Param("date") String date);

    /**
     * 按类型查询消费汇总
     */
    @Select("SELECT billing_type, SUM(amount_consumed) as amountConsumed, SUM(quantity) as quantity " +
            "FROM bx_billing_stats WHERE tenant_id = #{tenantId} AND stat_date = #{date} " +
            "GROUP BY billing_type")
    List<BillingStats> findBillingByType(@Param("tenantId") Long tenantId, @Param("date") String date);
}
