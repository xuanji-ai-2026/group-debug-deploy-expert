package com.beijixing.data.service.impl;

import com.beijixing.data.entity.AccountStats;
import com.beijixing.data.entity.BillingStats;
import com.beijixing.data.entity.LeadStats;
import com.beijixing.data.entity.OperationStats;
import com.beijixing.data.repository.AccountStatsRepository;
import com.beijixing.data.repository.BillingStatsRepository;
import com.beijixing.data.repository.LeadStatsRepository;
import com.beijixing.data.repository.OperationStatsRepository;
import com.beijixing.data.service.TrendService;
import com.beijixing.data.vo.TrendChartVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 趋势分析服务实现
 * 
 * @author 陈工（EMP-DATA-001）
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("nullness")
public class TrendServiceImpl implements TrendService {

    private final OperationStatsRepository operationStatsRepository;
    private final LeadStatsRepository leadStatsRepository;
    private final AccountStatsRepository accountStatsRepository;
    private final BillingStatsRepository billingStatsRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public List<TrendChartVO> getTrend(Long tenantId, String type, String startDate, String endDate) {
        log.info("获取趋势数据, tenantId={}, type={}, startDate={}, endDate={}", 
                tenantId, type, startDate, endDate);
        
        return switch (type.toUpperCase()) {
            case "OPERATION" -> calculateTrend(tenantId, startDate, endDate, "OPERATION");
            case "LEAD" -> calculateTrend(tenantId, startDate, endDate, "LEAD");
            case "ACCOUNT" -> calculateTrend(tenantId, startDate, endDate, "ACCOUNT");
            case "BILLING" -> calculateTrend(tenantId, startDate, endDate, "BILLING");
            default -> new ArrayList<>();
        };
    }

    @Override
    public List<TrendChartVO> getYearOverYearTrend(Long tenantId, String type, 
                                                    String startDate, String endDate) {
        // 计算去年同期
        LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
        LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
        LocalDate lastYearStart = start.minusYears(1);
        LocalDate lastYearEnd = end.minusYears(1);
        
        String lastYearStartStr = lastYearStart.format(DATE_FORMATTER);
        String lastYearEndStr = lastYearEnd.format(DATE_FORMATTER);
        
        List<TrendChartVO> currentTrend = getTrend(tenantId, type, startDate, endDate);
        List<TrendChartVO> lastYearTrend = getTrend(tenantId, type, lastYearStartStr, lastYearEndStr);
        
        // 合并计算同比
        Map<String, BigDecimal> lastYearMap = lastYearTrend.stream()
                .collect(Collectors.toMap(TrendChartVO::getDate, TrendChartVO::getValue));
        
        for (TrendChartVO vo : currentTrend) {
            String lastYearDate = LocalDate.parse(vo.getDate(), DATE_FORMATTER)
                    .minusYears(1).format(DATE_FORMATTER);
            BigDecimal lastYearValue = lastYearMap.get(lastYearDate);
            
            if (lastYearValue != null && lastYearValue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal change = vo.getValue().subtract(lastYearValue);
                vo.setYoyChange(change);
                vo.setYoyRate(change.divide(lastYearValue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP));
            }
        }
        
        return currentTrend;
    }

    @Override
    public List<TrendChartVO> getMonthOverMonthTrend(Long tenantId, String type, 
                                                      String startDate, String endDate) {
        List<TrendChartVO> trendList = getTrend(tenantId, type, startDate, endDate);
        
        // 计算环比变化
        for (int i = 1; i < trendList.size(); i++) {
            TrendChartVO current = trendList.get(i);
            TrendChartVO previous = trendList.get(i - 1);
            
            if (previous.getValue().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal change = current.getValue().subtract(previous.getValue());
                current.setMomChange(change);
                current.setMomRate(change.divide(previous.getValue(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP));
            }
        }
        
        return trendList;
    }

    /**
     * 计算趋势数据
     */
    private List<TrendChartVO> calculateTrend(Long tenantId, String startDate, 
                                               String endDate, String type) {
        return switch (type) {
            case "OPERATION" -> {
                List<OperationStats> statsList = operationStatsRepository
                        .findByTenantIdAndDateRange(tenantId, startDate, endDate);
                yield statsList.stream().map(stats -> {
                    TrendChartVO vo = new TrendChartVO();
                    vo.setDate(stats.getStatDate());
                    vo.setValue(BigDecimal.valueOf(
                            stats.getViewCount() != null ? stats.getViewCount() : 0));
                    vo.setCategory(type);
                    return vo;
                }).collect(Collectors.toList());
            }
            case "LEAD" -> {
                List<LeadStats> statsList = leadStatsRepository
                        .findByTenantIdAndDateRange(tenantId, startDate, endDate);
                yield statsList.stream().map(stats -> {
                    TrendChartVO vo = new TrendChartVO();
                    vo.setDate(stats.getStatDate());
                    vo.setValue(BigDecimal.valueOf(
                            stats.getNewLeads() != null ? stats.getNewLeads() : 0));
                    vo.setCategory(type);
                    return vo;
                }).collect(Collectors.toList());
            }
            case "ACCOUNT" -> {
                List<AccountStats> statsList = accountStatsRepository
                        .findByTenantIdAndDateRange(tenantId, startDate, endDate);
                Map<String, Integer> dateCountMap = new HashMap<>();
                for (AccountStats stats : statsList) {
                    if ("ACTIVE".equals(stats.getStatus())) {
                        dateCountMap.merge(stats.getStatDate(), 1, Integer::sum);
                    }
                }
                yield dateCountMap.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> {
                            TrendChartVO vo = new TrendChartVO();
                            vo.setDate(entry.getKey());
                            vo.setValue(BigDecimal.valueOf(entry.getValue()));
                            vo.setCategory(type);
                            return vo;
                        }).collect(Collectors.toList());
            }
            case "BILLING" -> {
                List<BillingStats> statsList = billingStatsRepository
                        .findByTenantIdAndDateRange(tenantId, startDate, endDate);
                Map<String, BigDecimal> dateAmountMap = new HashMap<>();
                for (BillingStats stats : statsList) {
                    if (stats.getAmountConsumed() != null) {
                        dateAmountMap.merge(stats.getStatDate(), 
                                stats.getAmountConsumed(), BigDecimal::add);
                    }
                }
                yield dateAmountMap.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> {
                            TrendChartVO vo = new TrendChartVO();
                            vo.setDate(entry.getKey());
                            vo.setValue(entry.getValue());
                            vo.setCategory(type);
                            return vo;
                        }).collect(Collectors.toList());
            }
            default -> new ArrayList<>();
        };
    }
}
