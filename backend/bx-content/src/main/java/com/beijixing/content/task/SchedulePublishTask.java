package com.beijixing.content.task;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时发布任务
 * @author 胡云 (EMP-CONTENT-001)
 */
@Data
public class SchedulePublishTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 内容ID
     */
    private Long contentId;

    /**
     * 发布平台列表
     */
    private List<Integer> platforms;

    /**
     * 计划发布时间
     */
    private LocalDateTime scheduledTime;
}
