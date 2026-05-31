package com.beijixing.risk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 风控规则实体 - 定义各类风控规则配置
 *
 * @author 林超 (EMP-SEC-001)
 */
@Data
@TableName("risk_rule")
public class RiskRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 规则ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 平台ID（NULL表示通用规则）
     */
    private Long platformId;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则类型：publish/message/follow/comment/login/access
     */
    private String ruleType;

    /**
     * 规则分类：frequency/content/device/ip/account/behavior
     */
    private String ruleCategory;

    /**
     * 规则编码（唯一标识）
     */
    private String ruleCode;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 规则配置（JSON格式）
     * 包含条件表达式、阈值、权重等配置
     */
    private String ruleConfig;

    /**
     * 风险类型
     */
    private String riskType;

    /**
     * 风险等级：1-低，2-中，3-高
     */
    private Integer riskLevel;

    /**
     * 扣分值（当规则触发时）
     */
    private Integer deductScore;

    /**
     * 执行优先级（越小越先执行）
     */
    private Integer priority;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 是否内置规则（内置规则不可删除）
     */
    private Integer builtIn;

    /**
     * 生效时间
     */
    private LocalDateTime effectiveTime;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

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
     * 更新人
     */
    private Long updateBy;

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
