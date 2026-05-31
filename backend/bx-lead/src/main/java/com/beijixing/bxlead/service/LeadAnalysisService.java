package com.beijixing.bxlead.service;

import com.beijixing.bxlead.vo.LeadFunnelVO;

import java.util.List;
import java.util.Map;

/**
 * 商机分析服务接口
 * @author 朱怡
 * @since 1.0.0
 */
public interface LeadAnalysisService {
    
    /**
     * 获取商机漏斗分析
     */
    List<LeadFunnelVO> getFunnelAnalysis();
    
    /**
     * 按状态统计
     */
    Map<String, Integer> countByStatus();
    
    /**
     * 按等级统计
     */
    Map<String, Integer> countByLevel();
    
    /**
     * 按来源统计
     */
    Map<String, Integer> countBySource();
    
    /**
     * 导出商机数据
     */
    String exportLeads(List<Long> leadIds, String format);
}