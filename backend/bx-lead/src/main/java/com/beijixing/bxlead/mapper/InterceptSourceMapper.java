package com.beijixing.bxlead.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.bxlead.entity.InterceptSource;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 截客来源Mapper
 * @author 朱怡
 * @since 1.0.0
 */
public interface InterceptSourceMapper extends BaseMapper<InterceptSource> {
    
    /**
     * 查询未处理的截客来源
     */
    @Select("SELECT * FROM bx_intercept_source WHERE is_processed = 0 ORDER BY create_time ASC LIMIT #{limit}")
    List<InterceptSource> selectUnprocessed(@Param("limit") Integer limit);
    
    /**
     * 标记为已处理
     */
    @Update("UPDATE bx_intercept_source SET is_processed = 1, process_time = NOW(), generated_lead_id = #{leadId} WHERE id = #{id}")
    int markAsProcessed(@Param("id") Long id, @Param("leadId") Long leadId);
}