package com.beijixing.risk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 风控评分实体 - 记录账号/设备/IP的综合评分
 *
 * @author 林超 (EMP-SEC-001)
 */
@Data
@TableName("risk_score")
public class RiskScore implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评分ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 评分对象类型：account/device/ip/user
     */
    private String objectType;

    /**
     * 评分对象ID（账号ID/设备ID/IP等）
     */
    private String objectId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 综合评分（0-100）
     */
    private Integer totalScore;

    /**
     * 操作频率评分
     */
    private Integer frequencyScore;

    /**
     * 内容合规评分
     */
    private Integer complianceScore;

    /**
     * 触达成功评分
     */
    private Integer touchScore;

    /**
     * 账号活跃评分
     */
    private Integer activityScore;

    /**
     * 设备指纹评分
     */
    private Integer deviceScore;

    /**
     * IP评分
     */
    private Integer ipScore;

    /**
     * 风险等级
     */
    private String riskLevel;

    /**
     * 今日操作次数
     */
    private Integer todayOperationCount;

    /**
     * 本周操作次数
     */
    private Integer weekOperationCount;

    /**
     * 最后操作时间
     */
    private LocalDateTime lastOperationTime;

    /**
     * 异常标记：0-正常，1-异常
     */
    private Integer abnormalFlag;

    /**
     * 异常原因
     */
    private String abnormalReason;

    /**
     * 备注
     */
    private String remark;

    /**
     * 评分时间
     */
    private LocalDateTime scoreTime;

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
