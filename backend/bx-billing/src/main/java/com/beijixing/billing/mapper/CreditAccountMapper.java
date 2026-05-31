package com.beijixing.billing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.billing.entity.CreditAccount;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface CreditAccountMapper extends BaseMapper<CreditAccount> {
    
    /**
     * 增加余额
     */
    @Update("UPDATE bx_credit_account SET balance = balance + #{amount}, " +
            "total_recharge = total_recharge + #{amount}, " +
            "last_transaction_time = NOW(), update_time = NOW() " +
            "WHERE id = #{accountId} AND deleted = 0")
    int addBalance(@Param("accountId") Long accountId, @Param("amount") Long amount);
    
    /**
     * 扣除余额（乐观锁）
     */
    @Update("UPDATE bx_credit_account SET balance = balance - #{amount}, " +
            "total_consumption = total_consumption + #{amount}, " +
            "last_transaction_time = NOW(), update_time = NOW() " +
            "WHERE id = #{accountId} AND balance >= #{amount} AND deleted = 0")
    int deductBalance(@Param("accountId") Long accountId, @Param("amount") Long amount);
    
    /**
     * 冻结金额
     */
    @Update("UPDATE bx_credit_account SET balance = balance - #{amount}, " +
            "frozen_amount = frozen_amount + #{amount}, " +
            "update_time = NOW() " +
            "WHERE id = #{accountId} AND balance >= #{amount} AND deleted = 0")
    int freezeAmount(@Param("accountId") Long accountId, @Param("amount") Long amount);
    
    /**
     * 解冻金额
     */
    @Update("UPDATE bx_credit_account SET balance = balance + #{amount}, " +
            "frozen_amount = frozen_amount - #{amount}, " +
            "update_time = NOW() " +
            "WHERE id = #{accountId} AND frozen_amount >= #{amount} AND deleted = 0")
    int unfreezeAmount(@Param("accountId") Long accountId, @Param("amount") Long amount);
}
