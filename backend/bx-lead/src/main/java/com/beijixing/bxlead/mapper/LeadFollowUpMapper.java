package com.beijixing.bxlead.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.bxlead.entity.LeadFollowUp;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 跟进记录Mapper
 * @author 朱怡
 * @since 1.0.0
 */
public interface LeadFollowUpMapper extends BaseMapper<LeadFollowUp> {
    
    /**
     * 查询商机的跟进记录列表
     */
    @Select("SELECT * FROM bx_lead_follow_up WHERE lead_id = #{leadId} ORDER BY create_time DESC")
    List<LeadFollowUp> selectByLeadId(@Param("leadId") Long leadId);
}