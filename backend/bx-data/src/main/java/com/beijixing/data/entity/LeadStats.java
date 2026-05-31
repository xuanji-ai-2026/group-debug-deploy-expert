package com.beijixing.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商机统计实体
 * 
 * @author 陈工（EMP-DATA-001）
 */
@Data
@TableName("bx_lead_stats")
public class LeadStats implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 统计日期
     */
    private String statDate;

    /**
     * 新增商机数
     */
    private Integer newLeads;

    /**
     * 跟进中商机数
     */
    private Integer followingLeads;

    /**
     * 已转化商机数
     */
    private Integer convertedLeads;

    /**
     * 转化率
     */
    private Double conversionRate;

    /**
     * 有效线索数
     */
    private Integer validLeads;

    /**
     * 无效线索数
     */
    private Integer invalidLeads;

    /**
     * 商机来源（APP/MINI_APP/WECHAT/OTHER）
     */
    private String source;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
