package com.beijixing.risk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 风控记录实体 - 记录每次风控检查的结果
 *
 * @author 林超 (EMP-SEC-001)
 */
@Data
@TableName("risk_record")
public class RiskRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 账号ID（可为null）
     */
    private Long accountId;

    /**
     * 操作类型：publish/message/follow/comment/login
     */
    private String operationType;

    /**
     * 风险类型
     */
    private String riskType;

    /**
     * 风险评分（0-100）
     */
    private Integer riskScore;

    /**
     * 风险等级
     */
    private String riskLevel;

    /**
     * 执行动作
     */
    private String action;

    /**
     * 触发规则ID
     */
    private Long triggeredRuleId;

    /**
     * 触发规则名称
     */
    private String triggeredRuleName;

    /**
     * 请求参数（JSON格式，用于问题排查）
     */
    private String requestParams;

    /**
     * 风险详情（JSON格式）
     */
    private String riskDetails;

    /**
     * 决策耗时（毫秒）
     */
    private Long decisionTimeMs;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 设备指纹
     */
    private String deviceFingerprint;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 结果状态：0-待处理，1-通过，2-拒绝，3-人工通过
     */
    private Integer status;

    /**
     * 处理备注
     */
    private String remark;

    /**
     * 处理人
     */
    private Long handleBy;

    /**
     * 处理时间
     */
    private LocalDateTime handleTime;

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
