package com.beijixing.bxlead.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beijixing.bxlead.dto.LeadQueryDTO;
import com.beijixing.bxlead.dto.LeadSaveDTO;
import com.beijixing.bxlead.entity.InterceptSource;
import com.beijixing.bxlead.entity.Lead;
import com.beijixing.bxlead.entity.LeadStatusHistory;
import com.beijixing.bxlead.enums.LeadLevel;
import com.beijixing.bxlead.enums.LeadSource;
import com.beijixing.bxlead.enums.LeadStatus;
import com.beijixing.bxlead.mapper.InterceptSourceMapper;
import com.beijixing.bxlead.mapper.LeadMapper;
import com.beijixing.bxlead.mapper.LeadStatusHistoryMapper;
import com.beijixing.bxlead.service.AiIntentAnalysisService;
import com.beijixing.bxlead.service.LeadService;
import com.beijixing.bxlead.vo.LeadVO;
import com.beijixing.bxlead.vo.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 商机服务实现类
 * @author 朱怡
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("nullness")
public class LeadServiceImpl implements LeadService {

    private static String currentUserId = "1";
    
    private final LeadMapper leadMapper;
    private final InterceptSourceMapper interceptSourceMapper;
    private final LeadStatusHistoryMapper statusHistoryMapper;
    private final AiIntentAnalysisService aiIntentAnalysisService;
    
    @Override
    public PageResult<LeadVO> listLeads(LeadQueryDTO query) {
        Page<Lead> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<Lead> leadPage = leadMapper.selectLeadPage(page, query);
        
        List<LeadVO> voList = leadPage.getRecords().stream()
                .map(this::convertToVO)
                .toList();
        
        return PageResult.of((int) leadPage.getCurrent(), (int) leadPage.getSize(), 
                leadPage.getTotal(), voList);
    }
    
    @Override
    public LeadVO getLeadDetail(Long id) {
        Lead lead = leadMapper.selectById(id);
        if (lead == null) {
            throw new RuntimeException("商机不存在");
        }
        return convertToVO(lead);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createLead(LeadSaveDTO dto) {
        Lead lead = new Lead();
        BeanUtils.copyProperties(dto, lead);
        
        // 生成商机编号
        lead.setLeadNo(generateLeadNo());
        
        // 设置初始状态
        if (!StringUtils.hasText(lead.getStatus())) {
            lead.setStatus(LeadStatus.NEW.getCode());
        }
        
        // AI意向分析
        if (StringUtils.hasText(lead.getRequirementDesc())) {
            Integer intentScore = aiIntentAnalysisService.analyzeIntentScore(lead.getRequirementDesc());
            lead.setIntentScore(intentScore);
            lead.setLevel(LeadLevel.getByScore(intentScore).getCode());
            lead.setAiAnalysisResult(aiIntentAnalysisService.generateAnalysisResult(lead.getRequirementDesc()));
        }
        
        lead.setFollowCount(0);
        leadMapper.insert(lead);
        
        // 记录状态历史
        saveStatusHistory(lead.getId(), null, lead.getStatus(), "创建商机");
        
        return lead.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLead(LeadSaveDTO dto) {
        Lead existing = leadMapper.selectById(dto.getId());
        if (existing == null) {
            throw new RuntimeException("商机不存在");
        }
        
        Lead lead = new Lead();
        BeanUtils.copyProperties(dto, lead);
        lead.setId(dto.getId());
        
        // 重新进行AI分析（如果需求描述变更）
        if (StringUtils.hasText(dto.getRequirementDesc()) && 
                !dto.getRequirementDesc().equals(existing.getRequirementDesc())) {
            Integer intentScore = aiIntentAnalysisService.analyzeIntentScore(dto.getRequirementDesc());
            lead.setIntentScore(intentScore);
            lead.setLevel(LeadLevel.getByScore(intentScore).getCode());
        }
        
        leadMapper.updateById(lead);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLead(Long id) {
        leadMapper.deleteById(id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignLead(Long leadId, Long ownerId, String ownerName, String assignType) {
        Lead lead = new Lead();
        lead.setId(leadId);
        lead.setOwnerId(ownerId);
        lead.setOwnerName(ownerName);
        lead.setAssignType(assignType);
        lead.setAssignTime(LocalDateTime.now());
        
        leadMapper.updateById(lead);
        log.info("商机[{}]已分配给[{}], 分配方式: {}", leadId, ownerName, assignType);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long leadId, String newStatus, String reason) {
        Lead lead = leadMapper.selectById(leadId);
        if (lead == null) {
            throw new RuntimeException("商机不存在");
        }
        
        String oldStatus = lead.getStatus();
        
        Lead update = new Lead();
        update.setId(leadId);
        update.setStatus(newStatus);
        leadMapper.updateById(update);
        
        // 记录状态变更历史
        saveStatusHistory(leadId, oldStatus, newStatus, reason);
    }
    
    @Override
    public void autoAssignLead(Long leadId) {
        Lead lead = leadMapper.selectById(leadId);
        if (lead == null) {
            log.warn("自动分配失败: 商机[{}]不存在", leadId);
            return;
        }
        
        Long ownerId = selectOwnerByRules(lead);
        if (ownerId == null) {
            log.warn("自动分配失败: 未找到匹配的负责人, 商机[{}]", leadId);
            return;
        }
        
        String ownerName = queryOwnerName(ownerId);
        assignLead(leadId, ownerId, ownerName, "AUTO");
        log.info("商机[{}]已自动分配给[{}]({}), 匹配规则: 地区={}, 行业={}", 
                leadId, ownerName, ownerId, lead.getRegion(), lead.getIndustry());
    }
    
    private Long selectOwnerByRules(Lead lead) {
        List<Map<String, Object>> candidates = findAvailableOwners();
        if (candidates.isEmpty()) {
            return null;
        }
        
        List<Map<String, Object>> matched = candidates.stream()
                .filter(owner -> matchRegion(owner, lead.getRegion()))
                .filter(owner -> matchIndustry(owner, lead.getIndustry()))
                .toList();
        
        if (matched.isEmpty()) {
            matched = candidates;
        }
        
        return matched.stream()
                .min(Comparator.comparingInt(o -> ((Number) o.getOrDefault("leadCount", 0)).intValue()))
                .map(o -> ((Number) o.get("ownerId")).longValue())
                .orElse(null);
    }
    
    private boolean matchRegion(Map<String, Object> owner, String region) {
        if (!StringUtils.hasText(region)) return true;
        String regions = (String) owner.get("regions");
        return !StringUtils.hasText(regions) || regions.contains(region);
    }
    
    private boolean matchIndustry(Map<String, Object> owner, String industry) {
        if (!StringUtils.hasText(industry)) return true;
        String industries = (String) owner.get("industries");
        return !StringUtils.hasText(industries) || industries.contains(industry);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateLeadFromIntercept(Long interceptSourceId) {
        InterceptSource source = interceptSourceMapper.selectById(interceptSourceId);
        if (source == null) {
            throw new RuntimeException("截客来源不存在");
        }
        
        // 分析内容
        Integer intentScore = aiIntentAnalysisService.analyzeIntentScore(source.getRawContent());
        String aiResult = aiIntentAnalysisService.generateAnalysisResult(source.getRawContent());
        
        // 创建商机
        Lead lead = new Lead();
        lead.setLeadNo(generateLeadNo());
        lead.setTitle("【截客】" + source.getCompetitorName() + "意向客户");
        lead.setSource(LeadSource.INTERCEPT.getCode());
        lead.setSource(source.getSourceType());
        lead.setCustomerName(source.getPublisher());
        lead.setRequirementDesc(source.getRawContent());
        lead.setStatus(LeadStatus.NEW.getCode());
        lead.setIntentScore(intentScore);
        lead.setLevel(LeadLevel.getByScore(intentScore).getCode());
        lead.setCompetitorKeywords(source.getCompetitorName());
        lead.setInterceptSourceId(interceptSourceId);
        lead.setAiAnalysisResult(aiResult);
        lead.setFollowCount(0);
        
        leadMapper.insert(lead);
        
        // 标记来源为已处理
        interceptSourceMapper.markAsProcessed(interceptSourceId, lead.getId());
        
        // 记录状态历史
        saveStatusHistory(lead.getId(), null, LeadStatus.NEW.getCode(), "从截客来源自动生成");
        
        log.info("从截客来源[{}]生成商机[{}]", interceptSourceId, lead.getId());
        
        return lead.getId();
    }
    
    private String generateLeadNo() {
        String prefix = "L" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 这里简化处理，实际应该使用分布式ID或数据库序列
        return prefix + System.currentTimeMillis() % 1000000;
    }
    
    private void saveStatusHistory(Long leadId, String oldStatus, String newStatus, String reason) {
        LeadStatusHistory history = new LeadStatusHistory();
        history.setLeadId(leadId);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangeReason(reason);
        
        String userIdStr = currentUserId;
        if (userIdStr != null && !userIdStr.isEmpty() && !"anonymous".equals(userIdStr)) {
            try {
                history.setOperatorId(Long.parseLong(userIdStr));
                history.setOperatorName(userIdStr);
            } catch (NumberFormatException e) {
                history.setOperatorId(1L);
                history.setOperatorName("系统");
            }
        } else {
            history.setOperatorId(1L);
            history.setOperatorName("系统");
        }
        
        statusHistoryMapper.insert(history);
    }
    
    private List<Map<String, Object>> findAvailableOwners() {
        return leadMapper.selectAvailableOwners();
    }
    
    private String queryOwnerName(Long ownerId) {
        Map<String, Object> owner = leadMapper.selectOwnerById(ownerId);
        if (owner != null) {
            return (String) owner.getOrDefault("ownerName", "未知用户");
        }
        return "未知用户";
    }
    
    @SuppressWarnings("nullness")
    private LeadVO convertToVO(Lead lead) {
        LeadVO vo = new LeadVO();
        BeanUtils.copyProperties(lead, vo);
        
        // 设置枚举描述
        LeadStatus status = LeadStatus.getByCode(lead.getStatus());
        if (status != null) {
            vo.setStatusDesc(status.getDesc());
        }
        
        LeadLevel level = LeadLevel.getByCode(lead.getLevel());
        if (level != null) {
            vo.setLevelDesc(level.getDesc());
        }
        
        LeadSource source = null;
        for (LeadSource s : LeadSource.values()) {
            if (s.getCode().equals(lead.getSource())) {
                source = s;
                break;
            }
        }
        if (source != null) {
            vo.setSourceDesc(source.getDesc());
        }
        
        vo.setIsIntercept(LeadSource.INTERCEPT.getCode().equals(lead.getSource()));
        
        return vo;
    }
}