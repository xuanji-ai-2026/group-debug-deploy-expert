package com.beijixing.risk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beijixing.risk.dto.RiskCheckRequest;
import com.beijixing.risk.dto.RiskReportRequest;
import com.beijixing.risk.engine.DecisionEngine;
import com.beijixing.risk.engine.ScoreEngine;
import com.beijixing.risk.entity.RiskRecord;
import com.beijixing.risk.entity.RiskScore;
import com.beijixing.risk.repository.RiskRecordRepository;
import com.beijixing.risk.service.RiskService;
import com.beijixing.risk.vo.RiskDecisionVO;
import com.beijixing.risk.vo.RiskReportVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson2.JSON;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 风控服务实现
 *
 * @author 林超 (EMP-SEC-001)
 */
@Slf4j
@Service
@SuppressWarnings("nullness")
public class RiskServiceImpl implements RiskService {

    @Autowired
    private DecisionEngine decisionEngine;

    @Autowired
    private ScoreEngine scoreEngine;

    @Autowired
    private RiskRecordRepository riskRecordRepository;

    @Override
    public RiskDecisionVO checkRisk(RiskCheckRequest request) {
        // 调用决策引擎
        RiskDecisionVO decision = decisionEngine.makeDecision(request);

        // 如果需要记录，则保存风控记录
        if (decisionEngine.needsLogging(request)) {
            try {
                saveRiskRecord(request, decision);
            } catch (Exception e) {
                log.error("保存风控记录失败", e);
            }
        }

        return decision;
    }

    @Override
    public RiskDecisionVO quickCheck(RiskCheckRequest request) {
        return decisionEngine.quickCheck(request);
    }

    @Override
    public RiskReportVO generateReport(RiskReportRequest request) {
        RiskReportVO report = new RiskReportVO();
        report.setReportType(request.getReportType());
        report.setGenerateTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        LocalDateTime startTime = request.getStartTime() != null
            ? request.getStartTime()
            : LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = request.getEndTime() != null
            ? request.getEndTime()
            : LocalDateTime.now();
        report.setTimeRange(startTime + " ~ " + endTime);

        // 统计概览
        RiskReportVO.SummaryStats summary = generateSummaryStats(request.getTenantId(), startTime, endTime);
        report.setSummary(summary);

        // 风险分布
        report.setRiskDistribution(generateRiskDistribution(request.getTenantId(), startTime, endTime));

        // 风险类型分布
        report.setRiskTypeDistribution(generateRiskTypeDistribution(request.getTenantId(), startTime, endTime));

        // 趋势数据
        report.setTrendData(generateTrendData(request.getTenantId(), startTime, endTime));

        // Top风险账号
        report.setTopRiskAccounts(generateTopRiskAccounts(request.getTenantId(), startTime, endTime));

        return report;
    }

    @Override
    public List<RiskDecisionVO> getRiskRecords(Long tenantId, Long accountId, String operationType,
                                               Integer pageNum, Integer pageSize) {
        Page<RiskRecord> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20);
        LambdaQueryWrapper<RiskRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskRecord::getTenantId, tenantId)
               .eq(accountId != null, RiskRecord::getAccountId, accountId)
               .eq(StringUtils.hasText(operationType), RiskRecord::getOperationType, operationType)
               .orderByDesc(RiskRecord::getCreateTime);

        Page<RiskRecord> result = riskRecordRepository.getBaseMapper().selectPage(page, wrapper);

        return result.getRecords().stream()
            .map(this::convertToDecisionVO)
            .collect(Collectors.toList());
    }

    @Override
    public Integer getAccountRiskScore(Long accountId) {
        // 先从数据库获取最新评分记录
        RiskScore score = scoreEngine.getAccountScore(accountId);
        if (score != null && score.getTotalScore() != null) {
            return score.getTotalScore();
        }
        return 100;  // 默认满分
    }

    @Override
    @Transactional
    public List<RiskDecisionVO> batchCheck(List<RiskCheckRequest> requests) {
        List<RiskDecisionVO> results = new ArrayList<>();
        for (RiskCheckRequest request : requests) {
            try {
                results.add(checkRisk(request));
            } catch (Exception e) {
                log.error("批量检查异常: accountId={}", request.getAccountId(), e);
                RiskDecisionVO errorVO = new RiskDecisionVO();
                errorVO.setPassed(true);  // 异常时默认放行
                errorVO.setErrorMessage("检查异常: " + e.getMessage());
                results.add(errorVO);
            }
        }
        return results;
    }

    @Override
    public Long getUnhandledAlertCount(Long tenantId) {
        LambdaQueryWrapper<RiskRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskRecord::getTenantId, tenantId)
               .eq(RiskRecord::getStatus, 0)
               .ne(RiskRecord::getAction, "PASS")
               .ge(RiskRecord::getCreateTime, LocalDateTime.now().minusDays(7));
        return riskRecordRepository.count(wrapper);
    }

    /**
     * 保存风控记录
     */
    private void saveRiskRecord(RiskCheckRequest request, RiskDecisionVO decision) {
        RiskRecord record = new RiskRecord();
        record.setTenantId(request.getTenantId());
        record.setAccountId(request.getAccountId());
        record.setOperationType(request.getOperationType());
        record.setRiskType(decision.getRiskType());
        record.setRiskScore(decision.getRiskScore());
        record.setRiskLevel(decision.getRiskLevel());
        record.setAction(decision.getAction());
        record.setIpAddress(request.getIpAddress());
        record.setDeviceFingerprint(request.getDeviceFingerprint());
        record.setUserId(request.getUserId());
        record.setRequestParams(JSON.toJSONString(request.getRequestParams()));
        record.setDecisionTimeMs(decision.getDecisionTimeMs());
        record.setStatus(decision.getPassed() ? 1 : 2);

        // 设置触发规则
        if (decision.getTriggeredRules() != null && !decision.getTriggeredRules().isEmpty()) {
            RiskDecisionVO.TriggeredRuleVO firstTrigger = decision.getTriggeredRules().get(0);
            record.setTriggeredRuleId(firstTrigger.getRuleId());
            record.setTriggeredRuleName(firstTrigger.getRuleName());
            record.setRiskDetails(JSON.toJSONString(decision.getTriggeredRules()));
        }

        riskRecordRepository.save(record);
        decision.setRecordId(record.getId());
    }

    /**
     * 将RiskRecord转换为RiskDecisionVO
     */
    private RiskDecisionVO convertToDecisionVO(RiskRecord record) {
        RiskDecisionVO vo = new RiskDecisionVO();
        vo.setRecordId(record.getId());
        vo.setRiskScore(record.getRiskScore());
        vo.setRiskLevel(record.getRiskLevel());
        vo.setRiskType(record.getRiskType());
        vo.setAction(record.getAction());
        vo.setActionDesc(record.getAction());
        vo.setPassed("PASS".equals(record.getAction()) || "WARN".equals(record.getAction()));
        vo.setDecisionTimeMs(record.getDecisionTimeMs());
        vo.setDecisionTime(record.getCreateTime() != null
            ? record.getCreateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        return vo;
    }

    /**
     * 生成概览统计
     */
    private RiskReportVO.SummaryStats generateSummaryStats(Long tenantId, LocalDateTime start, LocalDateTime end) {
        RiskReportVO.SummaryStats stats = new RiskReportVO.SummaryStats();
        List<RiskRecord> records = riskRecordRepository.findByTenantAndTimeRange(tenantId, start, end);

        stats.setTotalChecks(records.size());

        long passed = records.stream().filter(r -> "PASS".equals(r.getAction())).count();
        long blocked = records.stream().filter(r -> "BLOCK".equals(r.getAction()) || "BAN".equals(r.getAction())).count();
        long warned = records.stream().filter(r -> "WARN".equals(r.getAction())).count();

        stats.setTotalPassed((int) passed);
        stats.setTotalBlocked((int) blocked);
        stats.setTotalWarned((int) warned);

        stats.setPassRate(records.isEmpty() ? 100 : (int) (passed * 100 / records.size()));
        stats.setBlockRate(records.isEmpty() ? 0 : (int) (blocked * 100 / records.size()));

        OptionalDouble avgScore = records.stream()
            .mapToInt(r -> r.getRiskScore() != null ? r.getRiskScore() : 100)
            .average();
        stats.setAvgRiskScore(avgScore.isPresent() ? (int) avgScore.getAsDouble() : 100);

        return stats;
    }

    /**
     * 生成风险分布
     */
    private Map<String, Integer> generateRiskDistribution(Long tenantId, LocalDateTime start, LocalDateTime end) {
        Map<String, Integer> distribution = new HashMap<>();
        List<RiskRecord> records = riskRecordRepository.findByTenantAndTimeRange(tenantId, start, end);

        distribution.put("低风险", (int) records.stream().filter(r -> "LOW".equals(r.getRiskLevel())).count());
        distribution.put("中风险", (int) records.stream().filter(r -> "MEDIUM".equals(r.getRiskLevel())).count());
        distribution.put("高风险", (int) records.stream().filter(r -> "HIGH".equals(r.getRiskLevel())).count());
        distribution.put("严重风险", (int) records.stream().filter(r -> "CRITICAL".equals(r.getRiskLevel())).count());

        return distribution;
    }

    /**
     * 生成风险类型分布
     */
    private Map<String, Integer> generateRiskTypeDistribution(Long tenantId, LocalDateTime start, LocalDateTime end) {
        Map<String, Integer> distribution = new HashMap<>();
        List<RiskRecord> records = riskRecordRepository.findByTenantAndTimeRange(tenantId, start, end);

        records.stream()
            .filter(r -> r.getRiskType() != null)
            .forEach(r -> distribution.merge(r.getRiskType(), 1, Integer::sum));

        return distribution;
    }

    /**
     * 生成趋势数据
     */
    private List<RiskReportVO.TrendData> generateTrendData(Long tenantId, LocalDateTime start, LocalDateTime end) {
        List<RiskReportVO.TrendData> trendList = new ArrayList<>();
        // 简化实现：按天聚合
        List<RiskRecord> records = riskRecordRepository.findByTenantAndTimeRange(tenantId, start, end);

        Map<String, List<RiskRecord>> byDay = records.stream()
            .collect(Collectors.groupingBy(r ->
                r.getCreateTime().toLocalDate().toString()));

        byDay.forEach((day, dayRecords) -> {
            RiskReportVO.TrendData data = new RiskReportVO.TrendData();
            data.setDate(day);
            data.setCheckCount(dayRecords.size());
            data.setBlockCount((int) dayRecords.stream().filter(r -> "BLOCK".equals(r.getAction())).count());
            data.setPassCount((int) dayRecords.stream().filter(r -> "PASS".equals(r.getAction())).count());
            OptionalDouble avg = dayRecords.stream()
                .mapToInt(r -> r.getRiskScore() != null ? r.getRiskScore() : 100)
                .average();
            data.setAvgScore(avg.isPresent() ? (int) avg.getAsDouble() : 100);
            trendList.add(data);
        });

        return trendList;
    }

    /**
     * 生成Top风险账号
     */
    private List<RiskReportVO.AccountRiskVO> generateTopRiskAccounts(Long tenantId, LocalDateTime start, LocalDateTime end) {
        List<RiskReportVO.AccountRiskVO> topAccounts = new ArrayList<>();
        List<RiskRecord> records = riskRecordRepository.findByTenantAndTimeRange(tenantId, start, end);

        // 按账号分组，取评分最低的
        Map<Long, List<RiskRecord>> byAccount = records.stream()
            .filter(r -> r.getAccountId() != null)
            .collect(Collectors.groupingBy(RiskRecord::getAccountId));

        byAccount.forEach((accountId, accountRecords) -> {
            int avgScore = (int) accountRecords.stream()
                .mapToInt(r -> r.getRiskScore() != null ? r.getRiskScore() : 100)
                .average()
                .orElse(100);

            int blockCount = (int) accountRecords.stream()
                .filter(r -> "BLOCK".equals(r.getAction()) || "BAN".equals(r.getAction()))
                .count();

            if (blockCount > 0 || avgScore < 80) {
                RiskReportVO.AccountRiskVO vo = new RiskReportVO.AccountRiskVO();
                vo.setAccountId(accountId);
                vo.setRiskScore(avgScore);
                vo.setRiskLevel(com.beijixing.risk.enums.RiskLevel.fromScore(avgScore).name());
                vo.setBlockCount(blockCount);
                vo.setWarnCount((int) accountRecords.stream().filter(r -> "WARN".equals(r.getAction())).count());
                topAccounts.add(vo);
            }
        });

        // 按风险评分排序，取Top10
        return topAccounts.stream()
            .sorted(Comparator.comparingInt(RiskReportVO.AccountRiskVO::getRiskScore))
            .limit(10)
            .collect(Collectors.toList());
    }
}
