package com.beijixing.bxlead.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 跟进记录VO
 * @author 朱怡
 * @since 1.0.0
 */
@Data
public class LeadFollowUpVO {
    
    private Long id;
    
    /** 商机ID */
    private Long leadId;
    
    /** 跟进方式 */
    private String followType;
    
    /** 跟进内容 */
    private String followContent;
    
    /** 跟进结果 */
    private String followResult;
    
    /** 下次跟进时间 */
    private LocalDateTime nextFollowTime;
    
    /** 下次跟进提醒 */
    private Boolean nextFollowRemind;
    
    /** 跟进人ID */
    private Long followerId;
    
    /** 跟进人名称 */
    private String followerName;
    
    /** 客户反馈 */
    private String customerFeedback;
    
    /** 附件 */
    private String attachments;
    
    /** 创建时间 */
    private LocalDateTime createTime;
}