package com.beijixing.schedule.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("schedule_job")
public class ScheduleJob implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 任务类型：content_publish-内容发布, lead_generate-商机生成, risk_check-风控检查,
     * ai_billing-AI计费, data_sync-数据同步, health_check-健康检查, backup-备份
     */
    private String jobType;

    /**
     * Cron表达式
     */
    private String cron;

    /**
     * 任务参数（JSON格式）
     */
    private String params;

    /**
     * 任务状态：0-停止, 1-运行, 2-暂停
     */
    private Integer status;

    /**
     * 是否分布式执行：0-单机, 1-分布式
     */
    private Integer distributed;

    /**
     * 失败重试次数
     */
    private Integer retryTimes;

    /**
     * 任务超时时间（秒）
     */
    private Integer timeoutSeconds;

    /**
     * 执行器AppName
     */
    private String executorHandler;

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
    private String createBy;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 是否删除：0-未删除, 1-已删除
     */
    @TableLogic
    private Integer deleted;
}
