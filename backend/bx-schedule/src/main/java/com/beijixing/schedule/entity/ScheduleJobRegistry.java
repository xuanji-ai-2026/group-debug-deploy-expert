package com.beijixing.schedule.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("schedule_job_registry")
public class ScheduleJobRegistry implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 注册组名称
     */
    private String groupName;

    /**
     * 执行器AppName
     */
    private String appName;

    /**
     * 执行器名称
     */
    private String executorName;

    /**
     * 执行器地址
     */
    private String address;

    /**
     * 注册时间
     */
    private LocalDateTime registryTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 是否在线：0-离线, 1-在线
     */
    private Integer online;
}
