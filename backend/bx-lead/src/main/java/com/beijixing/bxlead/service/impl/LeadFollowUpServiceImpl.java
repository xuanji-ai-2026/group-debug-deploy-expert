package com.beijixing.bxlead.service.impl;

import com.beijixing.bxlead.dto.LeadFollowUpDTO;
import com.beijixing.bxlead.entity.LeadFollowUp;
import com.beijixing.bxlead.mapper.LeadFollowUpMapper;
import com.beijixing.bxlead.mapper.LeadMapper;
import com.beijixing.bxlead.service.LeadFollowUpService;
import com.beijixing.bxlead.vo.LeadFollowUpVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商机跟进服务实现类
 * @author 朱怡
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("nullness")
public class LeadFollowUpServiceImpl implements LeadFollowUpService {
    
    private final LeadFollowUpMapper followUpMapper;
    private final LeadMapper leadMapper;
    
    private static final ThreadLocal<String> CURRENT_USER_ID = new ThreadLocal<>();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFollowUp(LeadFollowUpDTO dto) {
        LeadFollowUp followUp = new LeadFollowUp();
        BeanUtils.copyProperties(dto, followUp);

        String userIdStr = CURRENT_USER_ID.get();
        if (userIdStr != null && !userIdStr.isEmpty() && !"anonymous".equals(userIdStr)) {
            try {
                followUp.setFollowerId(Long.parseLong(userIdStr));
                followUp.setFollowerName(userIdStr);
            } catch (NumberFormatException e) {
                followUp.setFollowerId(1L);
                followUp.setFollowerName("销售");
            }
        } else {
            followUp.setFollowerId(1L);
            followUp.setFollowerName("销售");
        }
        
        followUpMapper.insert(followUp);
        
        // 更新商机的跟进统计
        leadMapper.updateFollowInfo(dto.getLeadId());
        
        log.info("商机[{}]新增跟进记录", dto.getLeadId());
    }
    
    @Override
    public List<LeadFollowUpVO> getFollowUpList(Long leadId) {
        List<LeadFollowUp> list = followUpMapper.selectByLeadId(leadId);
        return list.stream().map(this::convertToVO).toList();
    }
    
    @Override
    public LeadFollowUpVO getDetail(Long id) {
        LeadFollowUp followUp = followUpMapper.selectById(id);
        return convertToVO(followUp);
    }
    
    @SuppressWarnings("nullness")
    private LeadFollowUpVO convertToVO(LeadFollowUp followUp) {
        LeadFollowUpVO vo = new LeadFollowUpVO();
        BeanUtils.copyProperties(followUp, vo);
        return vo;
    }
}