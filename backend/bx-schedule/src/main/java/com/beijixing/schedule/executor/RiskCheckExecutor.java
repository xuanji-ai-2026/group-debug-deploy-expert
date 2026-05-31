package com.beijixing.schedule.executor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.beijixing.bxlead.entity.Lead;
import com.beijixing.bxlead.mapper.LeadMapper;
import com.beijixing.content.entity.Content;
import com.beijixing.content.mapper.ContentMapper;
import com.beijixing.message.dto.SendNotificationRequest;
import com.beijixing.message.enums.NotificationType;
import com.beijixing.message.service.NotificationService;
import com.beijixing.risk.dto.RiskCheckRequest;
import com.beijixing.risk.entity.RiskRecord;
import com.beijixing.risk.repository.RiskRecordRepository;
import com.beijixing.risk.service.RiskService;
import com.beijixing.risk.vo.RiskDecisionVO;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.context.XxlJobHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 风控检查任务执行器
 * 负责检查商机和内容的合规性，识别风险
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiskCheckExecutor extends BaseExecutor {

    @Autowired(required = false)
    private LeadMapper leadMapper;

    @Autowired(required = false)
    private ContentMapper contentMapper;

    @Autowired(required = false)
    private RiskService riskService;

    @Autowired(required = false)
    private RiskRecordRepository riskRecordRepository;

    @Autowired(required = false)
    private NotificationService notificationService;

    private static final String LEAD_STATUS_PENDING_REVIEW = "pending_review";
    private static final String LEAD_STATUS_PASSED = "passed";
    private static final String LEAD_STATUS_RISK = "risk";
    private static final int CONTENT_STATUS_REVIEWING = 1;
    private static final int CONTENT_STATUS_APPROVED = 2;
    private static final int CONTENT_STATUS_REJECTED = 5;

    @XxlJob("riskCheckJob")
    public void xxlExecute() {
        String params = XxlJobHelper.getJobParam();
        execute(params);
    }

    @Override
    protected String doExecute(String params) {
        log.info("开始执行风控检查任务");
        XxlJobHelper.log("开始执行风控检查任务");

        int leadChecked = 0;
        int contentChecked = 0;
        int riskFound = 0;

        // 1. 检查待审核商机
        List<Long> pendingLeads = queryPendingLeads();
        for (Long leadId : pendingLeads) {
            try {
                boolean hasRisk = checkLeadRisk(leadId);
                if (hasRisk) {
                    markLeadRisk(leadId);
                    riskFound++;
                }
                updateLeadCheckStatus(leadId, hasRisk ? "risk" : "pass");
                leadChecked++;
            } catch (Exception e) {
                log.error("商机 {} 风控检查异常", leadId, e);
            }
        }

        // 2. 检查待发布内容
        List<Long> pendingContents = queryPendingContents();
        for (Long contentId : pendingContents) {
            try {
                List<String> risks = checkContentRisk(contentId);
                if (!risks.isEmpty()) {
                    markContentRisk(contentId, risks);
                    riskFound++;
                }
                updateContentCheckStatus(contentId, risks.isEmpty() ? "pass" : "risk");
                contentChecked++;
            } catch (Exception e) {
                log.error("内容 {} 风控检查异常", contentId, e);
            }
        }

        // 3. 发送风险告警
        if (riskFound > 0) {
            sendRiskAlert(riskFound);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("leadChecked", leadChecked);
        result.put("contentChecked", contentChecked);
        result.put("riskFound", riskFound);

        return toJson(result);
    }

    @Override
    protected String getJobName() {
        return "风控检查任务";
    }

    @Override
    protected String getJobType() {
        return "risk_check";
    }

    @Override
    protected int getTimeoutSeconds() {
        return 180; // 风控任务超时3分钟
    }

    /**
     * 查询待审核商机
     * 条件: status=pending_review 且未删除
     */
    private List<Long> queryPendingLeads() {
        try {
            if (leadMapper == null) {
                log.warn("LeadMapper未注入，跳过待审核商机查询");
                return Collections.emptyList();
            }

            LambdaQueryWrapper<Lead> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Lead::getStatus, LEAD_STATUS_PENDING_REVIEW)
                   .eq(Lead::getDeleted, 0)
                   .orderByAsc(Lead::getCreateTime)
                   .last("LIMIT 100");
            List<Lead> leads = leadMapper.selectList(wrapper);
            List<Long> leadIds = leads.stream().map(Lead::getId).collect(Collectors.toList());
            log.info("查询到 {} 个待审核商机", leadIds.size());
            return leadIds;
        } catch (Exception e) {
            log.error("查询待审核商机失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 查询待审核内容
     * 条件: status=1(审核中) 且未删除
     */
    private List<Long> queryPendingContents() {
        try {
            if (contentMapper == null) {
                log.warn("ContentMapper未注入，跳过待审核内容查询");
                return Collections.emptyList();
            }

            LambdaQueryWrapper<Content> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Content::getStatus, CONTENT_STATUS_REVIEWING)
                   .eq(Content::getDeleted, 0)
                   .orderByAsc(Content::getCreateTime)
                   .last("LIMIT 100");
            List<Content> contents = contentMapper.selectList(wrapper);
            List<Long> contentIds = contents.stream().map(Content::getId).collect(Collectors.toList());
            log.info("查询到 {} 个待审核内容", contentIds.size());
            return contentIds;
        } catch (Exception e) {
            log.error("查询待审核内容失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 检查商机风险
     * 调用风控服务执行企业黑名单、个人黑名单、关联风险等检查
     *
     * @param leadId 商机ID
     * @return 是否存在风险
     */
    private boolean checkLeadRisk(Long leadId) {
        try {
            if (leadMapper == null || riskService == null) {
                log.warn("LeadMapper或RiskService未注入，跳过商机风控检查: leadId={}", leadId);
                return false;
            }

            Lead lead = leadMapper.selectById(leadId);
            if (lead == null) {
                log.warn("商机 {} 不存在，跳过风控检查", leadId);
                return false;
            }

            RiskCheckRequest request = new RiskCheckRequest();
            request.setOperationType("lead_review");
            request.setRiskType("LEAD");
            request.setBusinessId(String.valueOf(leadId));
            request.setTenantId(1L);

            Map<String, Object> params = new HashMap<>();
            params.put("customerName", lead.getCustomerName());
            params.put("customerPhone", lead.getCustomerPhone());
            params.put("customerCompany", lead.getCustomerCompany());
            params.put("customerEmail", lead.getCustomerEmail());
            params.put("industry", lead.getIndustry());
            params.put("region", lead.getRegion());
            params.put("budgetAmount", lead.getBudgetAmount());
            params.put("title", lead.getTitle());
            params.put("requirementDesc", lead.getRequirementDesc());
            request.setRequestParams(params);

            log.info("开始检查商机 {} 风险, 客户={}, 公司={}", leadId, lead.getCustomerName(), lead.getCustomerCompany());

            long startTime = System.currentTimeMillis();
            RiskDecisionVO decision = riskService.checkRisk(request);
            long costMs = System.currentTimeMillis() - startTime;

            saveRiskRecord(request, decision, costMs);

            boolean hasRisk = !Boolean.TRUE.equals(decision.getPassed());
            log.info("商机 {} 风控检查完成: passed={}, score={}, level={}, action={}, 耗时{}ms",
                    leadId, decision.getPassed(), decision.getRiskScore(),
                    decision.getRiskLevel(), decision.getAction(), costMs);

            return hasRisk;
        } catch (Exception e) {
            log.error("商机 {} 风控检查异常", leadId, e);
            return false;
        }
    }

    /**
     * 检查内容风险
     * 调用风控服务执行敏感词、政治敏感、商业违规等检查
     *
     * @param contentId 内容ID
     * @return 风险标签列表（空列表表示无风险）
     */
    private List<String> checkContentRisk(Long contentId) {
        try {
            if (contentMapper == null || riskService == null) {
                log.warn("ContentMapper或RiskService未注入，跳过内容风控检查: contentId={}", contentId);
                return Collections.emptyList();
            }

            Content content = contentMapper.selectById(contentId);
            if (content == null) {
                log.warn("内容 {} 不存在，跳过风控检查", contentId);
                return Collections.emptyList();
            }

            RiskCheckRequest request = new RiskCheckRequest();
            request.setOperationType("content_review");
            request.setRiskType("CONTENT");
            request.setBusinessId(String.valueOf(contentId));
            request.setTenantId(1L);
            request.setUserId(content.getAuthorId());
            request.setContent(content.getContent());

            Map<String, Object> params = new HashMap<>();
            params.put("title", content.getTitle());
            params.put("summary", content.getSummary());
            params.put("contentType", content.getContentType());
            params.put("tags", content.getTags());
            params.put("authorName", content.getAuthorName());
            request.setRequestParams(params);

            log.info("开始检查内容 {} 风险, 标题={}", contentId, content.getTitle());

            long startTime = System.currentTimeMillis();
            RiskDecisionVO decision = riskService.quickCheck(request);
            long costMs = System.currentTimeMillis() - startTime;

            saveRiskRecord(request, decision, costMs);

            List<String> riskTags = decision.getRiskTags() != null ? decision.getRiskTags() : Collections.emptyList();
            if (decision.getTriggeredRules() != null && !decision.getTriggeredRules().isEmpty()) {
                riskTags = decision.getTriggeredRules().stream()
                        .map(r -> r.getRuleName() + "(" + r.getTriggerReason() + ")")
                        .collect(Collectors.toList());
            }

            if (!Boolean.TRUE.equals(decision.getPassed()) && riskTags.isEmpty()) {
                riskTags = Arrays.asList("风控评分不通过(score=" + decision.getRiskScore() + ", level=" + decision.getRiskLevel() + ")");
            }

            log.info("内容 {} 风控检查完成: passed={}, score={}, level={}, risks={}, 耗时{}ms",
                    contentId, decision.getPassed(), decision.getRiskScore(),
                    decision.getRiskLevel(), riskTags, costMs);

            return riskTags;
        } catch (Exception e) {
            log.error("内容 {} 风控检查异常", contentId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 标记商机风险 - 更新商机状态为risk并在remark中记录风险信息
     */
    private void markLeadRisk(Long leadId) {
        try {
            if (leadMapper == null) {
                log.warn("LeadMapper未注入，跳过商机风险标记: leadId={}", leadId);
                return;
            }

            LambdaUpdateWrapper<Lead> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Lead::getId, leadId)
                   .set(Lead::getStatus, LEAD_STATUS_RISK)
                   .set(Lead::getRemark, "[风控标记] " + LocalDateTime.now() + " 经风控检查发现风险")
                   .set(Lead::getUpdateTime, LocalDateTime.now());
            int rows = leadMapper.update(null, wrapper);
            if (rows > 0) {
                log.info("商机 {} 已标记为风险状态", leadId);
            } else {
                log.warn("商机 {} 风险标记更新失败", leadId);
            }
        } catch (Exception e) {
            log.error("标记商机 {} 风险失败", leadId, e);
        }
    }

    /**
     * 标记内容风险 - 更新内容状态为rejected并记录风险信息
     */
    private void markContentRisk(Long contentId, List<String> risks) {
        try {
            if (contentMapper == null) {
                log.warn("ContentMapper未注入，跳过内容风险标记: contentId={}", contentId);
                return;
            }

            String riskInfo = String.join("; ", risks);
            LambdaUpdateWrapper<Content> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Content::getId, contentId)
                   .set(Content::getStatus, CONTENT_STATUS_REJECTED)
                   .set(Content::getSummary, "[风控拒绝] " + LocalDateTime.now() + " 风险项: " + riskInfo)
                   .set(Content::getUpdateTime, LocalDateTime.now());
            int rows = contentMapper.update(null, wrapper);
            if (rows > 0) {
                log.info("内容 {} 已标记为风险状态, 原因: {}", contentId, riskInfo);
            } else {
                log.warn("内容 {} 风险标记更新失败", contentId);
            }
        } catch (Exception e) {
            log.error("标记内容 {} 风险失败", contentId, e);
        }
    }

    /**
     * 更新商机审核状态
     *
     * @param leadId 商机ID
     * @param status 状态: pass-通过, risk-有风险
     */
    private void updateLeadCheckStatus(Long leadId, String status) {
        try {
            if (leadMapper == null) {
                log.warn("LeadMapper未注入，跳过商机审核状态更新: leadId={}, status={}", leadId, status);
                return;
            }

            String targetStatus = "pass".equals(status) ? LEAD_STATUS_PASSED : LEAD_STATUS_RISK;
            String remarkSuffix = "pass".equals(status) ? "风控审核通过" : "风控审核发现风险";
            LambdaUpdateWrapper<Lead> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Lead::getId, leadId)
                   .in(Lead::getStatus, LEAD_STATUS_PENDING_REVIEW, LEAD_STATUS_RISK)
                   .set(Lead::getStatus, targetStatus)
                   .set(Lead::getRemark, "[风控审核] " + LocalDateTime.now() + " " + remarkSuffix)
                   .set(Lead::getUpdateTime, LocalDateTime.now());
            int rows = leadMapper.update(null, wrapper);
            log.info("商机 {} 审核状态更新为 {} (影响行数={})", leadId, targetStatus, rows);
        } catch (Exception e) {
            log.error("更新商机 {} 审核状态失败", leadId, e);
        }
    }

    /**
     * 更新内容审核状态
     *
     * @param contentId 内容ID
     * @param status 状态: pass-通过, risk-有风险
     */
    private void updateContentCheckStatus(Long contentId, String status) {
        try {
            if (contentMapper == null) {
                log.warn("ContentMapper未注入，跳过内容审核状态更新: contentId={}, status={}", contentId, status);
                return;
            }

            int targetStatus = "pass".equals(status) ? CONTENT_STATUS_APPROVED : CONTENT_STATUS_REJECTED;
            String remarkSuffix = "pass".equals(status) ? "风控审核通过" : "风控审核发现风险";
            LambdaUpdateWrapper<Content> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Content::getId, contentId)
                   .in(Content::getStatus, CONTENT_STATUS_REVIEWING, CONTENT_STATUS_REJECTED)
                   .set(Content::getStatus, targetStatus)
                   .set(Content::getUpdateTime, LocalDateTime.now());
            int rows = contentMapper.update(null, wrapper);
            log.info("内容 {} 审核状态更新为 {} (影响行数={})", contentId, targetStatus, rows);
        } catch (Exception e) {
            log.error("更新内容 {} 审核状态失败", contentId, e);
        }
    }

    /**
     * 发送风险告警通知
     *
     * @param count 风险项数量
     */
    private void sendRiskAlert(int count) {
        try {
            if (notificationService == null) {
                log.warn("NotificationService未注入，跳过风险告警通知发送，仅记录日志: riskCount={}", count);
                log.warn("风控检查告警: 检测到 {} 个风险项，请及时处理", count);
                return;
            }

            SendNotificationRequest request = new SendNotificationRequest();
            request.setUserId(0L);
            request.setNotificationType(NotificationType.RISK_WARNING);
            request.setTitle("风控检查告警");
            request.setContent(String.format(
                    "风控定时任务检测到 %d 个风险项（%s），请及时处理。\n\n" +
                    "检测时间: %s\n" +
                    "处理建议: 请登录系统查看风控记录详情。",
                    count,
                    count > 10 ? "数量较多，建议重点关注" : "正常范围",
                    LocalDateTime.now().toString()
            ));

            notificationService.sendNotification(request);
            log.warn("已发送风险告警通知, 风险项数: {}", count);
        } catch (Exception e) {
            log.error("发送风险告警通知失败, 风险项数: {}", count, e);
        }
    }

    /**
     * 保存风控检查记录到数据库
     */
    private void saveRiskRecord(RiskCheckRequest request, RiskDecisionVO decision, long costMs) {
        try {
            if (riskRecordRepository == null) {
                log.debug("RiskRecordRepository未注入，跳过风控记录保存");
                return;
            }

            RiskRecord record = new RiskRecord();
            record.setTenantId(request.getTenantId());
            record.setAccountId(request.getAccountId());
            record.setUserId(request.getUserId());
            record.setOperationType(request.getOperationType());
            record.setRiskType(request.getRiskType());
            record.setRiskScore(decision.getRiskScore());
            record.setRiskLevel(decision.getRiskLevel());
            record.setAction(decision.getAction());
            record.setRequestParams(com.alibaba.fastjson2.JSON.toJSONString(request.getRequestParams()));
            record.setRiskDetails(com.alibaba.fastjson2.JSON.toJSONString(decision));
            record.setDecisionTimeMs(costMs);
            record.setIpAddress(request.getIpAddress());
            record.setDeviceFingerprint(request.getDeviceFingerprint());
            record.setStatus(Boolean.TRUE.equals(decision.getPassed()) ? 1 : 2);
            record.setRemark(decision.getSuggestion());
            riskRecordRepository.save(record);
            log.debug("风控记录已保存, recordId={}", record.getId());
        } catch (Exception e) {
            log.error("保存风控记录失败", e);
        }
    }

    private String toJson(Map<String, Object> data) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }
}
