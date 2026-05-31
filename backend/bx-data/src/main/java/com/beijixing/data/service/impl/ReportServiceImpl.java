package com.beijixing.data.service.impl;

import com.beijixing.data.entity.AccountStats;
import com.beijixing.data.entity.BillingStats;
import com.beijixing.data.entity.LeadStats;
import com.beijixing.data.entity.OperationStats;
import com.beijixing.data.repository.AccountStatsRepository;
import com.beijixing.data.repository.BillingStatsRepository;
import com.beijixing.data.repository.LeadStatsRepository;
import com.beijixing.data.repository.OperationStatsRepository;
import com.beijixing.data.service.ReportService;
import com.beijixing.data.vo.ReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 报表服务实现
 * 
 * @author 陈工（EMP-DATA-001）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final OperationStatsRepository operationStatsRepository;
    private final LeadStatsRepository leadStatsRepository;
    private final AccountStatsRepository accountStatsRepository;
    private final BillingStatsRepository billingStatsRepository;

    @Override
    public List<ReportVO> exportReport(Long tenantId, String reportType, String startDate, String endDate) {
        log.info("导出报表, tenantId={}, reportType={}, startDate={}, endDate={}", 
                tenantId, reportType, startDate, endDate);
        
        return switch (reportType.toUpperCase()) {
            case "OPERATION" -> buildOperationReport(tenantId, startDate, endDate);
            case "LEAD" -> buildLeadReport(tenantId, startDate, endDate);
            case "ACCOUNT" -> buildAccountReport(tenantId, startDate, endDate);
            case "BILLING" -> buildBillingReport(tenantId, startDate, endDate);
            default -> new ArrayList<>();
        };
    }

    @Override
    public String exportCsvReport(Long tenantId, String reportType, String startDate, String endDate) {
        List<ReportVO> reportList = exportReport(tenantId, reportType, startDate, endDate);
        
        StringBuilder csv = new StringBuilder();
        csv.append("日期,指标名称,指标值,指标类型\n");
        
        for (ReportVO report : reportList) {
            csv.append(report.getDate()).append(",")
               .append(report.getMetricName()).append(",")
               .append(report.getMetricValue()).append(",")
               .append(report.getMetricType()).append("\n");
        }
        
        return csv.toString();
    }

    /**
     * 生成运营报表
     */
    private List<ReportVO> buildOperationReport(Long tenantId, String startDate, String endDate) {
        List<OperationStats> statsList = operationStatsRepository
                .findByTenantIdAndDateRange(tenantId, startDate, endDate);
        
        List<ReportVO> reportList = new ArrayList<>();
        
        for (OperationStats stats : statsList) {
            addReportEntry(reportList, stats.getStatDate(), "发布数量", 
                    String.valueOf(stats.getPublishCount()), "INTEGER");
            addReportEntry(reportList, stats.getStatDate(), "浏览数量", 
                    String.valueOf(stats.getViewCount()), "INTEGER");
            addReportEntry(reportList, stats.getStatDate(), "点赞数量", 
                    String.valueOf(stats.getLikeCount()), "INTEGER");
            addReportEntry(reportList, stats.getStatDate(), "评论数量", 
                    String.valueOf(stats.getCommentCount()), "INTEGER");
            addReportEntry(reportList, stats.getStatDate(), "分享数量", 
                    String.valueOf(stats.getShareCount()), "INTEGER");
            addReportEntry(reportList, stats.getStatDate(), "收藏数量", 
                    String.valueOf(stats.getFavoriteCount()), "INTEGER");
            addReportEntry(reportList, stats.getStatDate(), "互动率", 
                    String.format("%.2f%%", stats.getInteractionRate()), "PERCENT");
        }
        
        return reportList;
    }

    /**
     * 生成商机报表
     */
    private List<ReportVO> buildLeadReport(Long tenantId, String startDate, String endDate) {
        List<LeadStats> statsList = leadStatsRepository
                .findByTenantIdAndDateRange(tenantId, startDate, endDate);
        
        List<ReportVO> reportList = new ArrayList<>();
        
        for (LeadStats stats : statsList) {
            addReportEntry(reportList, stats.getStatDate(), "新增商机", 
                    String.valueOf(stats.getNewLeads()), "INTEGER");
            addReportEntry(reportList, stats.getStatDate(), "跟进中商机", 
                    String.valueOf(stats.getFollowingLeads()), "INTEGER");
            addReportEntry(reportList, stats.getStatDate(), "已转化商机", 
                    String.valueOf(stats.getConvertedLeads()), "INTEGER");
            addReportEntry(reportList, stats.getStatDate(), "转化率", 
                    String.format("%.2f%%", stats.getConversionRate()), "PERCENT");
            addReportEntry(reportList, stats.getStatDate(), "有效线索", 
                    String.valueOf(stats.getValidLeads()), "INTEGER");
            addReportEntry(reportList, stats.getStatDate(), "无效线索", 
                    String.valueOf(stats.getInvalidLeads()), "INTEGER");
        }
        
        return reportList;
    }

    /**
     * 生成账号报表
     */
    private List<ReportVO> buildAccountReport(Long tenantId, String startDate, String endDate) {
        List<AccountStats> statsList = accountStatsRepository
                .findByTenantIdAndDateRange(tenantId, startDate, endDate);
        
        List<ReportVO> reportList = new ArrayList<>();
        
        for (AccountStats stats : statsList) {
            addReportEntry(reportList, stats.getStatDate(), 
                    stats.getAccountName() + "-状态", stats.getStatus(), "STRING");
            addReportEntry(reportList, stats.getStatDate(), 
                    stats.getAccountName() + "-评分", 
                    stats.getScore() != null ? stats.getScore().toString() : "0", "DECIMAL");
            addReportEntry(reportList, stats.getStatDate(), 
                    stats.getAccountName() + "-粉丝数", 
                    String.valueOf(stats.getFollowers()), "INTEGER");
            addReportEntry(reportList, stats.getStatDate(), 
                    stats.getAccountName() + "-获客数", 
                    String.valueOf(stats.getLeads()), "INTEGER");
        }
        
        return reportList;
    }

    /**
     * 生成消费报表
     */
    private List<ReportVO> buildBillingReport(Long tenantId, String startDate, String endDate) {
        List<BillingStats> statsList = billingStatsRepository
                .findByTenantIdAndDateRange(tenantId, startDate, endDate);
        
        List<ReportVO> reportList = new ArrayList<>();
        
        for (BillingStats stats : statsList) {
            addReportEntry(reportList, stats.getStatDate(), 
                    stats.getBillingType() + "-消耗积分", 
                    stats.getPointsConsumed() != null ? stats.getPointsConsumed().toString() : "0", "DECIMAL");
            addReportEntry(reportList, stats.getStatDate(), 
                    stats.getBillingType() + "-消耗金额", 
                    stats.getAmountConsumed() != null ? stats.getAmountConsumed().toString() : "0", "DECIMAL");
            addReportEntry(reportList, stats.getStatDate(), 
                    stats.getBillingType() + "-获客成本", 
                    stats.getCostPerLead() != null ? stats.getCostPerLead().toString() : "0", "DECIMAL");
            addReportEntry(reportList, stats.getStatDate(), 
                    stats.getBillingType() + "-消费数量", 
                    String.valueOf(stats.getQuantity()), "INTEGER");
        }
        
        return reportList;
    }

    private void addReportEntry(List<ReportVO> reportList, String date, String metricName, 
                                String metricValue, String metricType) {
        ReportVO vo = new ReportVO();
        vo.setDate(date);
        vo.setMetricName(metricName);
        vo.setMetricValue(metricValue);
        vo.setMetricType(metricType);
        try {
            if ("INTEGER".equals(metricType)) {
                vo.setNumericValue(Double.parseDouble(metricValue));
            } else if ("DECIMAL".equals(metricType)) {
                vo.setNumericValue(Double.parseDouble(metricValue));
            } else if ("PERCENT".equals(metricType)) {
                vo.setNumericValue(Double.parseDouble(metricValue.replace("%", "")));
            }
        } catch (NumberFormatException e) {
            vo.setNumericValue(0.0);
        }
        reportList.add(vo);
    }
}
