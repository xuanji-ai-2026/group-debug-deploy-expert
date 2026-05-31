package com.beijixing.bxlead.service.impl;

import com.beijixing.bxlead.enums.LeadStatus;
import com.beijixing.bxlead.mapper.LeadMapper;
import com.beijixing.bxlead.service.LeadAnalysisService;
import com.beijixing.bxlead.vo.LeadFunnelVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 商机分析服务实现类
 * @author 朱怡
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeadAnalysisServiceImpl implements LeadAnalysisService {
    
    private final LeadMapper leadMapper;
    
    @Override
    public List<LeadFunnelVO> getFunnelAnalysis() {
        List<Map<String, Object>> stats = leadMapper.selectFunnelStats();
        List<LeadFunnelVO> result = new ArrayList<>();
        
        // 定义阶段顺序
        List<String> stageOrder = Arrays.asList(
                LeadStatus.NEW.getCode(),
                LeadStatus.FOLLOWING.getCode(),
                LeadStatus.QUOTED.getCode(),
                LeadStatus.NEGOTIATION.getCode(),
                LeadStatus.WON.getCode(),
                LeadStatus.LOST.getCode()
        );
        
        Map<String, Map<String, Object>> statMap = new HashMap<>();
        for (Map<String, Object> stat : stats) {
            statMap.put((String) stat.get("status"), stat);
        }
        
        Integer previousCount = null;
        Integer newCount = null;
        
        for (String stageCode : stageOrder) {
            LeadFunnelVO vo = new LeadFunnelVO();
            vo.setStageCode(stageCode);
            
            LeadStatus status = LeadStatus.getByCode(stageCode);
            vo.setStageName(status != null ? status.getDesc() : stageCode);
            
            Map<String, Object> stat = statMap.get(stageCode);
            if (stat != null) {
                Long count = (Long) stat.get("count");
                vo.setLeadCount(count.intValue());
                
                if (newCount == null) {
                    newCount = count.intValue();
                }
                
                // 计算转化率
                if (previousCount != null && previousCount > 0) {
                    BigDecimal rate = new BigDecimal(count)
                            .multiply(new BigDecimal(100))
                            .divide(new BigDecimal(previousCount), 2, RoundingMode.HALF_UP);
                    vo.setConversionRate(rate);
                } else {
                    vo.setConversionRate(new BigDecimal(100));
                }
                
                // 计算总转化率
                if (newCount > 0) {
                    BigDecimal totalRate = new BigDecimal(count)
                            .multiply(new BigDecimal(100))
                            .divide(new BigDecimal(newCount), 2, RoundingMode.HALF_UP);
                    vo.setTotalConversionRate(totalRate);
                }
                
                previousCount = count.intValue();
            } else {
                vo.setLeadCount(0);
                vo.setConversionRate(BigDecimal.ZERO);
                vo.setTotalConversionRate(BigDecimal.ZERO);
            }
            
            result.add(vo);
        }
        
        return result;
    }
    
    @Override
    public Map<String, Integer> countByStatus() {
        List<Map<String, Object>> list = leadMapper.countByStatus();
        Map<String, Integer> result = new HashMap<>();
        for (Map<String, Object> map : list) {
            String status = (String) map.get("status");
            Long count = (Long) map.get("count");
            result.put(status, count.intValue());
        }
        return result;
    }
    
    @Override
    public Map<String, Integer> countByLevel() {
        List<Map<String, Object>> list = leadMapper.countByLevel();
        Map<String, Integer> result = new HashMap<>();
        for (Map<String, Object> map : list) {
            String level = (String) map.get("level");
            Long count = (Long) map.get("count");
            result.put(level, count.intValue());
        }
        return result;
    }
    
    @Override
    public Map<String, Integer> countBySource() {
        // 简化实现
        return new HashMap<>();
    }
    
    @Override
    public String exportLeads(List<Long> leadIds, String format) {
        String fileName = "leads_export_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        
        if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
            fileName += ".xlsx";
            // 实际应写入文件并返回URL
            // EasyExcel.write(fileName, LeadExportVO.class).sheet("商机数据").doWrite(leads);
        } else {
            fileName += ".csv";
            // CSV导出实现
        }
        
        return fileName;
    }
}