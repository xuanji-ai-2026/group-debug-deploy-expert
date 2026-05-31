package com.beijixing.bxlead.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商机状态变更历史
 * @author 朱怡
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("bx_lead_status_history")
public class LeadStatusHistory implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /** 商机ID */
    @TableField("lead_id")
    private Long leadId;
    
    /** 原状态 */
    @TableField("old_status")
    private String oldStatus;
    
    /** 新状态 */
    @TableField("new_status")
    private String newStatus;
    
    /** 变更原因 */
    @TableField("change_reason")
    private String changeReason;
    
    /** 操作人ID */
    @TableField("operator_id")
    private Long operatorId;
    
    /** 操作人名称 */
    @TableField("operator_name")
    private String operatorName;
    
    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}