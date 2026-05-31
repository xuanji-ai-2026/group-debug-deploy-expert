package com.beijixing.bxlead.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.bxlead.entity.LeadStatusHistory;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商机状态历史Mapper
 * @author 朱怡
 * @since 1.0.0
 */
public interface LeadStatusHistoryMapper extends BaseMapper<LeadStatusHistory> {
    
    /**
     * 查询商机的状态变更历史
     */
    @Select("SELECT * FROM bx_lead_status_history WHERE lead_id = #{leadId} ORDER BY create_time DESC")
    List<LeadStatusHistory> selectByLeadId(@Param("leadId") Long leadId);
}