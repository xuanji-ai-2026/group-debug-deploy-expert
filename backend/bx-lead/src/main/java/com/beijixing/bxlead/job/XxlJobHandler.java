package com.beijixing.bxlead.job;

import com.beijixing.bxlead.entity.Lead;
import com.beijixing.bxlead.enums.LeadLevel;
import com.beijixing.bxlead.mapper.LeadMapper;
import com.beijixing.bxlead.service.AiIntentAnalysisService;
import com.beijixing.bxlead.service.LeadService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * XXL-JOB 分布式任务处理器
 * @author 朱怡
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class XxlJobHandler {
    
    private final LeadMapper leadMapper;
    private final LeadService leadService;
    private final AiIntentAnalysisService aiIntentAnalysisService;
    
    private static final int ARCHIVE_DAYS = 180;
    private static final int BATCH_SIZE = 100;
    
    /**
     * 商机数据清理任务
     */
    @XxlJob("leadDataCleanupJob")
    public void leadDataCleanupJob() {
        String param = XxlJobHelper.getJobParam();
        log.info("执行商机数据清理任务, 参数: {}", param);
        
        try {
            XxlJobHelper.log("开始清理过期数据...");
            
            LocalDateTime archiveThreshold = LocalDateTime.now().minusDays(ARCHIVE_DAYS);
            List<Lead> expiredLeads = leadMapper.selectExpiredLeadsForArchive(archiveThreshold);
            
            int archivedCount = 0;
            for (Lead lead : expiredLeads) {
                try {
                    lead.setDeleted(1);
                    leadMapper.updateById(lead);
                    archivedCount++;
                } catch (Exception e) {
                    XxlJobHelper.log("归档商机[{}]失败: {}", lead.getId(), e.getMessage());
                }
            }
            
            XxlJobHelper.log("数据清理完成, 共归档{}条过期商机", archivedCount);
            XxlJobHelper.handleSuccess();
        } catch (Exception e) {
            log.error("数据清理任务执行失败", e);
            XxlJobHelper.handleFail(e.getMessage());
        }
    }
    
    /**
     * 商机意向评分重新计算任务
     */
    @XxlJob("leadScoreRecalculateJob")
    public void leadScoreRecalculateJob() {
        log.info("执行商机意向评分重新计算任务");
        
        try {
            XxlJobHelper.log("开始重新计算意向评分...");
            
            List<Lead> leadsToRecalculate = leadMapper.selectLeadsForScoreRecalculation();
            
            int recalculatedCount = 0;
            for (Lead lead : leadsToRecalculate) {
                try {
                    if (StringUtils.hasText(lead.getRequirementDesc())) {
                        Integer newScore = aiIntentAnalysisService.analyzeIntentScore(lead.getRequirementDesc());
                        Lead update = new Lead();
                        update.setId(lead.getId());
                        update.setIntentScore(newScore);
                        update.setLevel(LeadLevel.getByScore(newScore).getCode());
                        leadMapper.updateById(update);
                        recalculatedCount++;
                    }
                } catch (Exception e) {
                    XxlJobHelper.log("重算商机[{}]评分失败: {}", lead.getId(), e.getMessage());
                }
            }
            
            XxlJobHelper.log("意向评分重新计算完成, 共处理{}条商机", recalculatedCount);
            XxlJobHelper.handleSuccess();
        } catch (Exception e) {
            log.error("意向评分重新计算任务执行失败", e);
            XxlJobHelper.handleFail(e.getMessage());
        }
    }
    
    /**
     * 商机自动分配任务
     */
    @XxlJob("leadAutoAssignJob")
    public void leadAutoAssignJob() {
        log.info("执行商机自动分配任务");
        
        try {
            XxlJobHelper.log("开始自动分配商机...");
            
            List<Lead> unassignedLeads = leadMapper.selectUnassignedLeads(BATCH_SIZE);
            
            int assignedCount = 0;
            for (Lead lead : unassignedLeads) {
                try {
                    leadService.autoAssignLead(lead.getId());
                    assignedCount++;
                } catch (Exception e) {
                    XxlJobHelper.log("自动分配商机[{}]失败: {}", lead.getId(), e.getMessage());
                }
            }
            
            XxlJobHelper.log("商机自动分配完成, 共分配{}条商机", assignedCount);
            XxlJobHelper.handleSuccess();
        } catch (Exception e) {
            log.error("商机自动分配任务执行失败", e);
            XxlJobHelper.handleFail(e.getMessage());
        }
    }
}