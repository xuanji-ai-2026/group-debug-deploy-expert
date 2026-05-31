package com.beijixing.bxlead.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商机跟进记录实体类
 * @author 朱怡
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("bx_lead_follow_up")
public class LeadFollowUp implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /** 商机ID */
    @TableField("lead_id")
    private Long leadId;
    
    /** 跟进方式 */
    @TableField("follow_type")
    private String followType;
    
    /** 跟进内容 */
    @TableField("follow_content")
    private String followContent;
    
    /** 跟进结果 */
    @TableField("follow_result")
    private String followResult;
    
    /** 下次跟进时间 */
    @TableField("next_follow_time")
    private LocalDateTime nextFollowTime;
    
    /** 下次跟进提醒 */
    @TableField("next_follow_remind")
    private Boolean nextFollowRemind;
    
    /** 跟进人ID */
    @TableField("follower_id")
    private Long followerId;
    
    /** 跟进人名称 */
    @TableField("follower_name")
    private String followerName;
    
    /** 客户反馈 */
    @TableField("customer_feedback")
    private String customerFeedback;
    
    /** 附件 */
    @TableField("attachments")
    private String attachments;
    
    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /** 创建人 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;
}