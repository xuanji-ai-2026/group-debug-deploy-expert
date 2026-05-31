package com.beijixing.bxlead.service;

import com.beijixing.bxlead.dto.LeadFollowUpDTO;
import com.beijixing.bxlead.vo.LeadFollowUpVO;

import java.util.List;

/**
 * 商机跟进服务接口
 * @author 朱怡
 * @since 1.0.0
 */
public interface LeadFollowUpService {
    
    /**
     * 创建跟进记录
     */
    void createFollowUp(LeadFollowUpDTO dto);
    
    /**
     * 获取商机的跟进记录列表
     */
    List<LeadFollowUpVO> getFollowUpList(Long leadId);
    
    /**
     * 获取跟进记录详情
     */
    LeadFollowUpVO getDetail(Long id);
}