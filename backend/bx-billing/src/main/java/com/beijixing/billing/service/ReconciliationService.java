package com.beijixing.billing.service;

import com.beijixing.billing.constants.BillingConstants;
import com.beijixing.billing.entity.BillingOrder;
import com.beijixing.billing.mapper.BillingOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 财务对账服务
 * BL-009: 财务对账API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationService {
    
    private final BillingOrderMapper orderMapper;
    
    /**
     * 获取对账数据
     */
    public Map<String, Object> getReconciliationData(LocalDate startDate, LocalDate endDate, Long tenantId) {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1).atStartOfDay();
        
        // 查询时间段内的订单
        List<BillingOrder> orders = orderMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BillingOrder>()
                .eq(tenantId != null, BillingOrder::getTenantId, tenantId)
                .ge(BillingOrder::getCreateTime, startTime)
                .lt(BillingOrder::getCreateTime, endTime)
                .orderByAsc(BillingOrder::getCreateTime)
        );
        
        // 按日期分组统计
        Map<LocalDate, DailyStatistics> dailyStats = new HashMap<>();
        
        long totalRecharge = 0;
        long totalConsumption = 0;
        long totalRefund = 0;
        
        for (BillingOrder order : orders) {
            LocalDate orderDate = order.getCreateTime().toLocalDate();
            DailyStatistics stats = dailyStats.computeIfAbsent(orderDate, k -> new DailyStatistics());
            
            if (order.getStatus() == BillingConstants.ORDER_STATUS_PAID) {
                switch (order.getOrderType()) {
                    case BillingConstants.ORDER_TYPE_RECHARGE:
                        stats.rechargeAmount += order.getActualAmount();
                        stats.rechargeCount++;
                        totalRecharge += order.getActualAmount();
                        break;
                    case BillingConstants.ORDER_TYPE_CONSUMPTION:
                        stats.consumptionAmount += order.getAmount();
                        stats.consumptionCount++;
                        totalConsumption += order.getAmount();
                        break;
                    case BillingConstants.ORDER_TYPE_PACKAGE:
                        stats.packageAmount += order.getActualAmount();
                        stats.packageCount++;
                        totalRecharge += order.getActualAmount();
                        break;
                    case BillingConstants.ORDER_TYPE_REFUND:
                        stats.refundAmount += order.getAmount();
                        stats.refundCount++;
                        totalRefund += order.getAmount();
                        break;
                }
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("tenantId", tenantId);
        result.put("totalRecharge", totalRecharge);
        result.put("totalConsumption", totalConsumption);
        result.put("totalRefund", totalRefund);
        result.put("netAmount", totalRecharge - totalRefund);
        result.put("dailyStatistics", dailyStats.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .collect(Collectors.toList()));
        
        return result;
    }
    
    /**
     * 获取订单明细对账
     */
    public List<BillingOrder> getOrderDetails(LocalDate startDate, LocalDate endDate, 
                                               Integer orderType, Long tenantId) {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1).atStartOfDay();
        
        return orderMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BillingOrder>()
                .eq(tenantId != null, BillingOrder::getTenantId, tenantId)
                .eq(orderType != null, BillingOrder::getOrderType, orderType)
                .ge(BillingOrder::getCreateTime, startTime)
                .lt(BillingOrder::getCreateTime, endTime)
                .orderByDesc(BillingOrder::getCreateTime)
        );
    }
    
    /**
     * 日统计数据内部类
     */
    @lombok.Data
    public static class DailyStatistics {
        public long rechargeAmount;
        public int rechargeCount;
        public long consumptionAmount;
        public int consumptionCount;
        public long packageAmount;
        public int packageCount;
        public long refundAmount;
        public int refundCount;
    }
}
