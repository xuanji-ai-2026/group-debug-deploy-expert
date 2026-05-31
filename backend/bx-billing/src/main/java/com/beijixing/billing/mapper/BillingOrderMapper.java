package com.beijixing.billing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.billing.entity.BillingOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface BillingOrderMapper extends BaseMapper<BillingOrder> {
    
    /**
     * 根据订单号查询
     */
    @Select("SELECT * FROM bx_billing_order WHERE order_no = #{orderNo} AND deleted = 0")
    BillingOrder selectByOrderNo(@Param("orderNo") String orderNo);
    
    /**
     * 查询用户的待支付订单
     */
    @Select("SELECT * FROM bx_billing_order WHERE user_id = #{userId} " +
            "AND status = 0 AND expire_time > NOW() AND deleted = 0")
    List<BillingOrder> selectPendingOrders(@Param("userId") Long userId);
}
