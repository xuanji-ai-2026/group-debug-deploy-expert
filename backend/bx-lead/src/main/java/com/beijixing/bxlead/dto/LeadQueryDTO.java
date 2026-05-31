package com.beijixing.bxlead.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商机查询DTO
 * @author 朱怡
 * @since 1.0.0
 */
@Data
public class LeadQueryDTO {
    
    /** 关键字搜索（标题/客户名/公司） */
    private String keyword;
    
    /** 商机状态列表 */
    private List<String> statusList;
    
    /** 商机等级列表 */
    private List<String> levelList;
    
    /** 商机来源列表 */
    private List<String> sourceList;
    
    /** 行业 */
    private String industry;
    
    /** 地区 */
    private String region;
    
    /** 负责人ID */
    private Long ownerId;
    
    /** 最小意向评分 */
    private Integer minIntentScore;
    
    /** 最大意向评分 */
    private Integer maxIntentScore;
    
    /** 创建开始时间 */
    private LocalDateTime createTimeStart;
    
    /** 创建结束时间 */
    private LocalDateTime createTimeEnd;
    
    /** 预计成交开始时间 */
    private LocalDateTime expectedDealTimeStart;
    
    /** 预计成交结束时间 */
    private LocalDateTime expectedDealTimeEnd;
    
    /** 是否同业截客 */
    private Boolean isIntercept;
    
    /** 排序字段 */
    private String sortField;
    
    /** 排序方式 asc/desc */
    private String sortOrder;
    
    /** 页码 */
    private Integer pageNum = 1;
    
    /** 每页大小 */
    private Integer pageSize = 20;
}