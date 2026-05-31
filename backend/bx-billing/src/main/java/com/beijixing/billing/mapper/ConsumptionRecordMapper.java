package com.beijixing.billing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.billing.entity.ConsumptionRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ConsumptionRecordMapper extends BaseMapper<ConsumptionRecord> {
    
    /**
     * 根据调用ID查询
     */
    @Select("SELECT * FROM bx_consumption_record WHERE call_id = #{callId} AND deleted = 0")
    ConsumptionRecord selectByCallId(@Param("callId") String callId);
    
    /**
     * 查询用户的消费统计
     */
    @Select("SELECT SUM(total_cost) FROM bx_consumption_record " +
            "WHERE user_id = #{userId} AND deleted = 0")
    Long getTotalConsumptionByUserId(@Param("userId") Long userId);
}
