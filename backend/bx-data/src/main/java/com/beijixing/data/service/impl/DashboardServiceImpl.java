package com.beijixing.data.service.impl;

import com.beijixing.data.entity.AccountStats;
import com.beijixing.data.entity.BillingStats;
import com.beijixing.data.entity.LeadStats;
import com.beijixing.data.entity.OperationStats;
import com.beijixing.data.repository.AccountStatsRepository;
import com.beijixing.data.repository.BillingStatsRepository;
import com.beijixing.data.repository.LeadStatsRepository;
import com.beijixing.data.repository.OperationStatsRepository;
import com.beijixing.data.service.DashboardService;
import com.beijixing.data.vo.DashboardVO;
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
 * 数据看板服务实现
 * 
 * @author 陈工（EMP-DATA-001）
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("nullness")
public class DashboardServiceImpl implements DashboardService {

    private final OperationStatsRepository operationStatsRepository;
    private final LeadStatsRepository leadStatsRepository;
    private final AccountStatsRepository accountStatsRepository;
    private final BillingStatsRepository billingStatsRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public DashboardVO getOperationDashboard(Long tenantId, String startDate, String endDate) {
        log.info("获取运营数据看板, tenantId={}, startDate={}, endDate={}", tenantId, startDate, endDate);
        
        DashboardVO vo = new DashboardVO();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        String todayStr = today.format(DATE_FORMATTER);
        String yesterdayStr = yesterday.format(DATE_FORMATTER);
        
        // 获取今日汇总
        OperationStats todayStats = getOperationStats(tenantId, todayStr);
        // 获取昨日汇总
        OperationStats yesterdayStats = getOperationStats(tenantId, yesterdayStr);
        
        // 计算总计和变化率
        int totalView = todayStats != null ? todayStats.getViewCount() : 0;
        int yesterdayView = yesterdayStats != null ? yesterdayStats.getViewCount() : 0;
        
        vo.setTotal(BigDecimal.valueOf(totalView));
        vo.setToday(BigDecimal.valueOf(totalView));
        vo.setYesterday(BigDecimal.valueOf(yesterdayView));
        vo.setYearOverYearRate(calculateChangeRate(totalView, yesterdayView));
        vo.setMonthOverMonthRate(calculateChangeRate(totalView, yesterdayView));
        
        // 获取趋势数据
        vo.setTrendData(getOperationTrend(tenantId, startDate, endDate));
        
        return vo;
    }

    @Override
    public DashboardVO getLeadDashboard(Long tenantId, String startDate, String endDate) {
        log.info("获取商机数据看板, tenantId={}, startDate={}, endDate={}", tenantId, startDate, endDate);
        
        DashboardVO vo = new DashboardVO();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        String todayStr = today.format(DATE_FORMATTER);
        String yesterdayStr = yesterday.format(DATE_FORMATTER);
        
        // 获取今日汇总
        LeadStats todayStats = getLeadStats(tenantId, todayStr);
        LeadStats yesterdayStats = getLeadStats(tenantId, yesterdayStr);
        
        // 计算总计
        int totalLeads = todayStats != null ? todayStats.getNewLeads() : 0;
        int yesterdayLeads = yesterdayStats != null ? yesterdayStats.getNewLeads() : 0;
        
        vo.setTotal(BigDecimal.valueOf(totalLeads));
        vo.setToday(BigDecimal.valueOf(totalLeads));
        vo.setYesterday(BigDecimal.valueOf(yesterdayLeads));
        vo.setYearOverYearRate(calculateChangeRate(totalLeads, yesterdayLeads));
        vo.setMonthOverMonthRate(calculateChangeRate(totalLeads, yesterdayLeads));
        
        // 获取趋势数据
        vo.setTrendData(getLeadTrend(tenantId, startDate, endDate));
        
        return vo;
    }

    @Override
    public DashboardVO getAccountDashboard(Long tenantId, String startDate, String endDate) {
        log.info("获取账号数据看板, tenantId={}, startDate={}, endDate={}", tenantId, startDate, endDate);
        
        DashboardVO vo = new DashboardVO();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        String todayStr = today.format(DATE_FORMATTER);
        String yesterdayStr = yesterday.format(DATE_FORMATTER);
        
        // 获取今日账号统计
        List<AccountStats> todayStatsList = getAccountStats(tenantId, todayStr);
        List<AccountStats> yesterdayStatsList = getAccountStats(tenantId, yesterdayStr);
        
        int activeAccounts = todayStatsList.stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .toList().size();
        int yesterdayActive = yesterdayStatsList.stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .toList().size();
        
        vo.setTotal(BigDecimal.valueOf(activeAccounts));
        vo.setToday(BigDecimal.valueOf(activeAccounts));
        vo.setYesterday(BigDecimal.valueOf(yesterdayActive));
        vo.setYearOverYearRate(calculateChangeRate(activeAccounts, yesterdayActive));
        vo.setMonthOverMonthRate(calculateChangeRate(activeAccounts, yesterdayActive));
        
        // 获取趋势数据
        vo.setTrendData(getAccountTrend(tenantId, startDate, endDate));
        
        return vo;
    }

    @Override
    public DashboardVO getBillingDashboard(Long tenantId, String startDate, String endDate) {
        log.info("获取消费数据看板, tenantId={}, startDate={}, endDate={}", tenantId, startDate, endDate);
        
        DashboardVO vo = new DashboardVO();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        String todayStr = today.format(DATE_FORMATTER);
        String yesterdayStr = yesterday.format(DATE_FORMATTER);
        
        // 获取今日消费统计
        BillingStats todayStats = getBillingStats(tenantId, todayStr);
        BillingStats yesterdayStats = getBillingStats(tenantId, yesterdayStr);
        
        BigDecimal todayAmount = todayStats != null && todayStats.getAmountConsumed() != null 
                ? todayStats.getAmountConsumed() : BigDecimal.ZERO;
        BigDecimal yesterdayAmount = yesterdayStats != null && yesterdayStats.getAmountConsumed() != null 
                ? yesterdayStats.getAmountConsumed() : BigDecimal.ZERO;
        
        vo.setTotal(todayAmount);
        vo.setToday(todayAmount);
        vo.setYesterday(yesterdayAmount);
        vo.setYearOverYearRate(calculateChangeRate(todayAmount, yesterdayAmount));
        vo.setMonthOverMonthRate(calculateChangeRate(todayAmount, yesterdayAmount));
        
        // 获取趋势数据
        vo.setTrendData(getBillingTrend(tenantId, startDate, endDate));
        
        return vo;
    }

    @Override
    public List<TrendChartVO> getOperationTrend(Long tenantId, String startDate, String endDate) {
        List<OperationStats> statsList = operationStatsRepository
                .findByTenantIdAndDateRange(tenantId, startDate, endDate);
        
        return statsList.stream().map(stats -> {
            TrendChartVO vo = new TrendChartVO();
            vo.setDate(stats.getStatDate());
            vo.setValue(BigDecimal.valueOf(stats.getViewCount() != null ? stats.getViewCount() : 0));
            vo.setCategory("OPERATION");
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<TrendChartVO> getLeadTrend(Long tenantId, String startDate, String endDate) {
        List<LeadStats> statsList = leadStatsRepository
                .findByTenantIdAndDateRange(tenantId, startDate, endDate);
        
        return statsList.stream().map(stats -> {
            TrendChartVO vo = new TrendChartVO();
            vo.setDate(stats.getStatDate());
            vo.setValue(BigDecimal.valueOf(stats.getNewLeads() != null ? stats.getNewLeads() : 0));
            vo.setCategory("LEAD");
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<TrendChartVO> getAccountTrend(Long tenantId, String startDate, String endDate) {
        List<AccountStats> statsList = accountStatsRepository
                .findByTenantIdAndDateRange(tenantId, startDate, endDate);
        
        // 按日期聚合
        Map<String, Integer> dateCountMap = new HashMap<>();
        for (AccountStats stats : statsList) {
            if ("ACTIVE".equals(stats.getStatus())) {
                dateCountMap.merge(stats.getStatDate(), 1, Integer::sum);
            }
        }
        
        return dateCountMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    TrendChartVO vo = new TrendChartVO();
                    vo.setDate(entry.getKey());
                    vo.setValue(BigDecimal.valueOf(entry.getValue()));
                    vo.setCategory("ACCOUNT");
                    return vo;
                }).collect(Collectors.toList());
    }

    @Override
    public List<TrendChartVO> getBillingTrend(Long tenantId, String startDate, String endDate) {
        List<BillingStats> statsList = billingStatsRepository
                .findByTenantIdAndDateRange(tenantId, startDate, endDate);
        
        // 按日期聚合
        Map<String, BigDecimal> dateAmountMap = new HashMap<>();
        for (BillingStats stats : statsList) {
            if (stats.getAmountConsumed() != null) {
                dateAmountMap.merge(stats.getStatDate(), stats.getAmountConsumed(), BigDecimal::add);
            }
        }
        
        return dateAmountMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    TrendChartVO vo = new TrendChartVO();
                    vo.setDate(entry.getKey());
                    vo.setValue(entry.getValue());
                    vo.setCategory("BILLING");
                    return vo;
                }).collect(Collectors.toList());
    }

    // ========== 私有方法 ==========

    private OperationStats getOperationStats(Long tenantId, String date) {
        try {
            return operationStatsRepository.findTodaySummary(tenantId, date);
        } catch (Exception e) {
            log.warn("查询运营统计数据失败: {}", e.getMessage());
            return null;
        }
    }

    private LeadStats getLeadStats(Long tenantId, String date) {
        try {
            return leadStatsRepository.findConversionSummary(tenantId, date);
        } catch (Exception e) {
            log.warn("查询商机统计数据失败: {}", e.getMessage());
            return null;
        }
    }

    private List<AccountStats> getAccountStats(Long tenantId, String date) {
        try {
            return accountStatsRepository.findByTenantIdAndDateRange(tenantId, date, date);
        } catch (Exception e) {
            log.warn("查询账号统计数据失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private BillingStats getBillingStats(Long tenantId, String date) {
        try {
            return billingStatsRepository.findBillingSummary(tenantId, date);
        } catch (Exception e) {
            log.warn("查询消费统计数据失败: {}", e.getMessage());
            return null;
        }
    }

    private BigDecimal calculateChangeRate(int current, int previous) {
        if (previous == 0) {
            return current > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(current - previous)
                .divide(BigDecimal.valueOf(previous), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateChangeRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0 
                    ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
