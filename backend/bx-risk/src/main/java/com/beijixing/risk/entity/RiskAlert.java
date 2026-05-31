package com.beijixing.risk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 风控预警实体 - 记录风控系统产生的预警信息
 *
 * @author 林超 (EMP-SEC-001)
 */
@Data
@TableName("risk_alert")
public class RiskAlert implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 预警ID
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
     * 预警类型
     */
    private String alertType;

    /**
     * 预警级别：1-提示，2-警告，3-严重
     */
    private Integer alertLevel;

    /**
     * 预警内容
     */
    private String alertContent;

    /**
     * 处理建议
     */
    private String suggestion;

    /**
     * 状态：0-未处理，1-已处理，2-已忽略
     */
    private Integer status;

    /**
     * 处理时间
     */
    private LocalDateTime handleTime;

    /**
     * 处理人
     */
    private Long handleBy;

    /**
     * 处理备注
     */
    private String handleRemark;

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
}
