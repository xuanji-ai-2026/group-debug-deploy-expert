package com.beijixing.billing.service;

import com.beijixing.billing.constants.BillingConstants;
import com.beijixing.billing.dto.*;
import com.beijixing.billing.entity.*;
import com.beijixing.billing.mapper.*;
import com.beijixing.billing.util.RedisDistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("nullness")
public class BillingOrderService {
    
    private final BillingOrderMapper orderMapper;
    private final CreditAccountMapper creditAccountMapper;
    private final CreditAccountService creditAccountService;
    private final StringRedisTemplate redisTemplate;
    
    private static final DateTimeFormatter ORDER_NO_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    @Transactional
    public BillingOrderDTO createRechargeOrder(Long tenantId, RechargeRequestDTO request) {
        Long bonusAmount = calculateBonus(request.getAmount());
        
        BillingOrder order = new BillingOrder();
        order.setOrderNo(generateOrderNo());
        order.setTenantId(tenantId);
        order.setUserId(request.getUserId());
        order.setOrderType(BillingConstants.ORDER_TYPE_RECHARGE);
        order.setStatus(BillingConstants.ORDER_STATUS_PENDING);
        order.setPayType(request.getPayType());
        order.setAmount(request.getAmount());
        order.setActualAmount(request.getAmount());
        order.setBonusAmount(bonusAmount);
        order.setDescription(request.getDescription());
        order.setExpireTime(LocalDateTime.now().plusMinutes(30));
        
        orderMapper.insert(order);
        return convertToDTO(order);
    }
    
    @Transactional
    public boolean handlePaymentCallback(String orderNo, String transactionId, Integer payType) {
        BillingOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null || order.getStatus() != BillingConstants.ORDER_STATUS_PENDING) {
            return false;
        }
        
        String lockKey = "order:" + order.getId();
        RedisDistributedLock lock = new RedisDistributedLock(redisTemplate, lockKey, 30);
        
        try {
            boolean acquired = lock.tryLock();
            if (!acquired) {
                return false;
            }
            
            order.setStatus(BillingConstants.ORDER_STATUS_PAID);
            order.setPayType(payType);
            order.setTransactionId(transactionId);
            order.setPayTime(LocalDateTime.now());
            orderMapper.updateById(order);
            
            Long totalAmount = order.getActualAmount() + order.getBonusAmount();
            CreditAccount account = creditAccountMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CreditAccount>()
                    .eq(CreditAccount::getTenantId, order.getTenantId())
                    .eq(CreditAccount::getUserId, order.getUserId())
            );
            
            if (account != null) {
                creditAccountService.addBalance(account.getId(), totalAmount);
            }
            
            return true;
            
        } finally {
            lock.unlock();
        }
    }
    
    @Transactional
    public boolean cancelOrder(String orderNo) {
        BillingOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null || order.getStatus() != BillingConstants.ORDER_STATUS_PENDING) {
            return false;
        }
        
        order.setStatus(BillingConstants.ORDER_STATUS_CANCELLED);
        orderMapper.updateById(order);
        return true;
    }
    
    public BillingOrderDTO getOrderByNo(String orderNo) {
        BillingOrder order = orderMapper.selectByOrderNo(orderNo);
        return order != null ? convertToDTO(order) : null;
    }
    
    public List<BillingOrderDTO> getUserOrders(Long userId, Integer orderType) {
        List<BillingOrder> orders = orderMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BillingOrder>()
                .eq(BillingOrder::getUserId, userId)
                .eq(orderType != null, BillingOrder::getOrderType, orderType)
                .orderByDesc(BillingOrder::getCreateTime)
        );
        return orders.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    public Long calculateBonus(Long amount) {
        Long bonus = 0L;
        for (int[] tier : BillingConstants.RECHARGE_BONUS_TIERS) {
            if (amount >= tier[0]) {
                bonus = (long) tier[1];
            }
        }
        return bonus;
    }
    
    private String generateOrderNo() {
        return "BX" + LocalDateTime.now().format(ORDER_NO_FORMATTER) + 
               UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
    
    @SuppressWarnings("nullness")
    private BillingOrderDTO convertToDTO(BillingOrder order) {
        BillingOrderDTO dto = new BillingOrderDTO();
        BeanUtils.copyProperties(order, dto);
        return dto;
    }
}