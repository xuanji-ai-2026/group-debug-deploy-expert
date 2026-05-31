package com.beijixing.bxlead.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 截客来源实体类 - 记录竞品关键词监控数据源
 * @author 朱怡
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("bx_intercept_source")
public class InterceptSource implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /** 来源名称 */
    @TableField("source_name")
    private String sourceName;
    
    /** 来源类型 */
    @TableField("source_type")
    private String sourceType;
    
    /** 监控关键词 */
    @TableField("monitor_keywords")
    private String monitorKeywords;
    
    /** 竞品名称 */
    @TableField("competitor_name")
    private String competitorName;
    
    /** 数据来源URL/API */
    @TableField("source_url")
    private String sourceUrl;
    
    /** 原始内容 */
    @TableField("raw_content")
    private String rawContent;
    
    /** 内容链接 */
    @TableField("content_url")
    private String contentUrl;
    
    /** 发布者 */
    @TableField("publisher")
    private String publisher;
    
    /** 发布时间 */
    @TableField("publish_time")
    private LocalDateTime publishTime;
    
    /** 是否已处理 */
    @TableField("is_processed")
    private Boolean isProcessed;
    
    /** 处理时间 */
    @TableField("process_time")
    private LocalDateTime processTime;
    
    /** 生成的商机ID */
    @TableField("generated_lead_id")
    private Long generatedLeadId;
    
    /** AI意向评分 */
    @TableField("ai_intent_score")
    private Integer aiIntentScore;
    
    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}