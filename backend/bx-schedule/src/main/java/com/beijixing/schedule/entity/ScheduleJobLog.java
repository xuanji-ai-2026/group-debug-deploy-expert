package com.beijixing.schedule.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("schedule_job_log")
public class ScheduleJobLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
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
     * 任务类型
     */
    private String jobType;

    /**
     * 执行器地址
     */
    private String executorAddress;

    /**
     * 执行Handler
     */
    private String executorHandler;

    /**
     * 任务参数
     */
    private String params;

    /**
     * 执行状态：0-失败, 1-成功, 2-进行中
     */
    private Integer status;

    /**
     * 重试次数
     */
    private Integer retryTimes;

    /**
     * 失败原因
     */
    private String failReason;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 执行耗时（毫秒）
     */
    private Long costTime;

    /**
     * 执行结果
     */
    private String result;

    /**
     * 机器地址
     */
    private String hostAddress;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
