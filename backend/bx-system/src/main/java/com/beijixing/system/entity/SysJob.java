package com.beijixing.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 定时任务实体
 * 对应数据库表: sys_job
 *
 * 功能：SM-003 定时任务（任务配置、执行日志）
 *
 * @author bx-system
 */
@Data
@TableName("sys_job")
public class SysJob implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * 任务分组（如：system、business、ai）
     */
    private String jobGroup;

    /**
     * 任务类名（Spring Bean名称或完整类名）
     */
    private String beanName;

    /**
     * cron 表达式
     */
    private String cronExpression;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 执行参数
     */
    private String execParams;

    /**
     * 状态：0-暂停，1-运行中
     */
    private Integer status;

    /**
     * 是否并发执行：0-否，1-是
     */
    private Integer concurrent;

    /**
     * 任务状态：
     * 0-NORMAL（正常）
     * 1-PAUSED（暂停）
     * 2-COMPLETE（完成）
     * 3-ERROR（错误）
     * 4-BLOCKED（阻塞）
     */
    private Integer misfirePolicy;

    /**
     * 上次执行时间
     */
    private LocalDateTime lastExecTime;

    /**
     * 下次执行时间
     */
    private LocalDateTime nextExecTime;

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
     * 创建人
     */
    private Long createBy;

    /**
     * 更新人
     */
    private Long updateBy;

    /**
     * 删除标记：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
}
