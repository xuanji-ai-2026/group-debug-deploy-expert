package com.beijixing.bxlead.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商机主表实体类
 * @author 朱怡
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("bx_lead")
public class Lead implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /** 商机编号 */
    @TableField("lead_no")
    private String leadNo;
    
    /** 商机标题 */
    @TableField("title")
    private String title;
    
    /** 商机来源 */
    @TableField("source")
    private String source;
    
    /** 来源渠道 */
    @TableField("channel")
    private String channel;
    
    /** 客户名称 */
    @TableField("customer_name")
    private String customerName;
    
    /** 客户电话 */
    @TableField("customer_phone")
    private String customerPhone;
    
    /** 客户邮箱 */
    @TableField("customer_email")
    private String customerEmail;
    
    /** 客户公司 */
    @TableField("customer_company")
    private String customerCompany;
    
    /** 所属行业 */
    @TableField("industry")
    private String industry;
    
    /** 所在地区 */
    @TableField("region")
    private String region;
    
    /** 需求描述 */
    @TableField("requirement_desc")
    private String requirementDesc;
    
    /** 预算金额 */
    @TableField("budget_amount")
    private BigDecimal budgetAmount;
    
    /** 预计成交时间 */
    @TableField("expected_deal_time")
    private LocalDateTime expectedDealTime;
    
    /** 商机状态 */
    @TableField("status")
    private String status;
    
    /** 意向评分 (0-100) */
    @TableField("intent_score")
    private Integer intentScore;
    
    /** 商机等级 A/B/C/D */
    @TableField("level")
    private String level;
    
    /** 负责人ID */
    @TableField("owner_id")
    private Long ownerId;
    
    /** 负责人名称 */
    @TableField("owner_name")
    private String ownerName;
    
    /** 分配方式 */
    @TableField("assign_type")
    private String assignType;
    
    /** 分配时间 */
    @TableField("assign_time")
    private LocalDateTime assignTime;
    
    /** 竞品关键词标记 */
    @TableField("competitor_keywords")
    private String competitorKeywords;
    
    /** 截客来源ID */
    @TableField("intercept_source_id")
    private Long interceptSourceId;
    
    /** AI分析结果 */
    @TableField("ai_analysis_result")
    private String aiAnalysisResult;
    
    /** 跟进次数 */
    @TableField("follow_count")
    private Integer followCount;
    
    /** 最后跟进时间 */
    @TableField("last_follow_time")
    private LocalDateTime lastFollowTime;
    
    /** 备注 */
    @TableField("remark")
    private String remark;
    
    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /** 创建人 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;
    
    /** 更新人 */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    
    /** 逻辑删除 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}