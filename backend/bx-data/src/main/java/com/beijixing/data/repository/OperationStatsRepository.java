package com.beijixing.data.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.data.entity.OperationStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 运营统计 Repository
 * 
 * @author 陈工（EMP-DATA-001）
 */
@Mapper
public interface OperationStatsRepository extends BaseMapper<OperationStats> {

    /**
     * 查询指定时间范围的运营统计数据
     */
    @Select("SELECT * FROM bx_operation_stats WHERE tenant_id = #{tenantId} " +
            "AND stat_date >= #{startDate} AND stat_date <= #{endDate} ORDER BY stat_date")
    List<OperationStats> findByTenantIdAndDateRange(
            @Param("tenantId") Long tenantId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

    /**
     * 查询今日汇总数据
     */
    @Select("SELECT SUM(publish_count) as publishCount, SUM(view_count) as viewCount, " +
            "SUM(like_count) as likeCount, SUM(comment_count) as commentCount, " +
            "SUM(share_count) as shareCount, SUM(favorite_count) as favoriteCount " +
            "FROM bx_operation_stats WHERE tenant_id = #{tenantId} AND stat_date = #{date}")
    OperationStats findTodaySummary(@Param("tenantId") Long tenantId, @Param("date") String date);
}
