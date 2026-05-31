package com.beijixing.billing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.billing.entity.BillingConfig;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface BillingConfigMapper extends BaseMapper<BillingConfig> {
    
    /**
     * 根据配置编码查询
     */
    @Select("SELECT * FROM bx_billing_config WHERE config_code = #{code} AND deleted = 0")
    BillingConfig selectByCode(@Param("code") String code);
    
    /**
     * 根据配置类型查询
     */
    @Select("SELECT * FROM bx_billing_config WHERE config_type = #{type} AND enabled = 1 AND deleted = 0")
    List<BillingConfig> selectByType(@Param("type") String type);
}
