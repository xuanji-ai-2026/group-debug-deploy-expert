package com.beijixing.bxlead.vo.task;

import lombok.Data;

import java.util.List;

@Data
public class TaskVO {
    private Long taskId;
    private Long tenantId;
    private String name;
    private String type;
    private String status;
    private List<String> platforms;
    private List<String> keywords;
    private Integer totalCount;
    private Integer completedCount;
    private Integer successCount;
    private Integer failCount;
    private Integer progress;
    private String startTime;
    private String endTime;
    private String actualEndTime;
    private String errorMsg;
    private String createTime;
    private String updateTime;
}
