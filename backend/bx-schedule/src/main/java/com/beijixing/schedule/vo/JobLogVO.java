package com.beijixing.schedule.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class JobLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long jobId;
    private String jobName;
    private String jobType;
    private String executorAddress;
    private String executorHandler;
    private String params;
    private Integer status;
    private Integer retryTimes;
    private String failReason;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long costTime;
    private String result;
    private String hostAddress;
    private LocalDateTime createTime;
}
