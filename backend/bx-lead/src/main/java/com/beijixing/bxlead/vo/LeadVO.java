package com.beijixing.bxlead.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商机VO
 * @author 朱怡
 * @since 1.0.0
 */
@Data
public class LeadVO {
    
    private Long id;
    
    /** 商机编号 */
    private String leadNo;
    
    /** 商机标题 */
    private String title;
    
    /** 商机来源 */
    private String source;
    
    /** 来源描述 */
    private String sourceDesc;
    
    /** 渠道 */
    private String channel;
    
    /** 客户名称 */
    private String customerName;
    
    /** 客户电话 */
    private String customerPhone;
    
    /** 客户邮箱 */
    private String customerEmail;
    
    /** 客户公司 */
    private String customerCompany;
    
    /** 行业 */
    private String industry;
    
    /** 地区 */
    private String region;
    
    /** 需求描述 */
    private String requirementDesc;
    
    /** 预算金额 */
    private BigDecimal budgetAmount;
    
    /** 预计成交时间 */
    private LocalDateTime expectedDealTime;
    
    /** 商机状态 */
    private String status;
    
    /** 状态描述 */
    private String statusDesc;
    
    /** 意向评分 (0-100) */
    private Integer intentScore;
    
    /** 商机等级 */
    private String level;
    
    /** 等级描述 */
    private String levelDesc;
    
    /** 负责人ID */
    private Long ownerId;
    
    /** 负责人名称 */
    private String ownerName;
    
    /** 分配方式 */
    private String assignType;
    
    /** 分配时间 */
    private LocalDateTime assignTime;
    
    /** 竞品关键词 */
    private String competitorKeywords;
    
    /** 是否截客 */
    private Boolean isIntercept;
    
    /** AI分析结果 */
    private String aiAnalysisResult;
    
    /** 跟进次数 */
    private Integer followCount;
    
    /** 最后跟进时间 */
    private LocalDateTime lastFollowTime;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
}