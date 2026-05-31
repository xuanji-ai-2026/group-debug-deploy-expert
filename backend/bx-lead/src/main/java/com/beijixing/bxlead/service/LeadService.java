package com.beijixing.bxlead.service;

import com.beijixing.bxlead.dto.LeadQueryDTO;
import com.beijixing.bxlead.dto.LeadSaveDTO;
import com.beijixing.bxlead.vo.LeadVO;
import com.beijixing.bxlead.vo.PageResult;

/**
 * 商机服务接口
 * @author 朱怡
 * @since 1.0.0
 */
public interface LeadService {
    
    /**
     * 分页查询商机列表
     */
    PageResult<LeadVO> listLeads(LeadQueryDTO query);
    
    /**
     * 获取商机详情
     */
    LeadVO getLeadDetail(Long id);
    
    /**
     * 创建商机
     */
    Long createLead(LeadSaveDTO dto);
    
    /**
     * 更新商机
     */
    void updateLead(LeadSaveDTO dto);
    
    /**
     * 删除商机
     */
    void deleteLead(Long id);
    
    /**
     * 分配商机
     */
    void assignLead(Long leadId, Long ownerId, String ownerName, String assignType);
    
    /**
     * 变更商机状态
     */
    void changeStatus(Long leadId, String newStatus, String reason);
    
    /**
     * 自动分配商机
     */
    void autoAssignLead(Long leadId);
    
    /**
     * 从截客来源生成商机
     */
    Long generateLeadFromIntercept(Long interceptSourceId);
}