package com.beijixing.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务执行日志实体
 * 对应数据库表: sys_job_log
 *
 * 功能：SM-003 定时任务（任务配置、执行日志）
 *
 * @author bx-system
 */
@Data
@TableName("sys_job_log")
public class SysJobLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID
     */
    private Long jobId;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * 任务分组
     */
    private String jobGroup;

    /**
     * 执行状态：0-失败，1-成功
     */
    private Integer execStatus;

    /**
     * 执行时间（毫秒）
     */
    private Long execDuration;

    /**
     * 执行结果（正常时的返回信息或异常信息）
     */
    private String execResult;

    /**
     * 异常堆栈
     */
    private String exceptionInfo;

    /**
     * 开始执行时间
     */
    private LocalDateTime startTime;

    /**
     * 结束执行时间
     */
    private LocalDateTime endTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 删除标记：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
}
