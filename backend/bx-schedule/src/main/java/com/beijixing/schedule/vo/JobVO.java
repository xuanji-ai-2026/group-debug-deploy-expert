package com.beijixing.schedule.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class JobVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String jobName;
    private String description;
    private String jobType;
    private String cron;
    private String params;
    private Integer status;
    private Integer distributed;
    private Integer retryTimes;
    private Integer timeoutSeconds;
    private String executorHandler;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createBy;
    private String updateBy;
}
