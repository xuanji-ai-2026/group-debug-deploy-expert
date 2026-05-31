package com.beijixing.risk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 风控策略实体 - 定义风控策略组合
 *
 * @author 林超 (EMP-SEC-001)
 */
@Data
@TableName("risk_strategy")
public class RiskStrategy implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 策略ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 策略名称
     */
    private String strategyName;

    /**
     * 策略编码（唯一标识）
     */
    private String strategyCode;

    /**
     * 策略类型：account/operation/content/crawler/captcha
     */
    private String strategyType;

    /**
     * 策略描述
     */
    private String description;

    /**
     * 关联规则ID列表（JSON数组）
     */
    private String ruleIds;

    /**
     * 策略配置（JSON格式）
     * 包含规则组合方式、阈值等配置
     */
    private String strategyConfig;

    /**
     * 触发条件（JSON格式）
     * 描述何时启用该策略
     */
    private String triggerCondition;

    /**
     * 默认执行动作
     */
    private String defaultAction;

    /**
     * 风险阈值（分数低于此值触发策略）
     */
    private Integer riskThreshold;

    /**
     * 租户ID（NULL表示全局策略）
     */
    private Long tenantId;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建人
     */
    private Long createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;
}
