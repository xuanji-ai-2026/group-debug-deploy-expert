package com.beijixing.billing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.billing.entity.PackagePurchase;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PackagePurchaseMapper extends BaseMapper<PackagePurchase> {
    
    /**
     * 查询用户生效中的套餐
     */
    @Select("SELECT * FROM bx_package_purchase WHERE user_id = #{userId} " +
            "AND status = 1 AND deleted = 0 ORDER BY expire_date ASC")
    List<PackagePurchase> selectActivePackages(@Param("userId") Long userId);
    
    /**
     * 查询用户所有套餐
     */
    @Select("SELECT * FROM bx_package_purchase WHERE user_id = #{userId} AND deleted = 0 " +
            "ORDER BY create_time DESC")
    List<PackagePurchase> selectByUserId(@Param("userId") Long userId);
}
