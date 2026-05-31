package com.beijixing.content.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时发布任务
 */
@Data
public class SchedulePublishTask implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long contentId;
    private List<Integer> platforms;  // 使用Integer类型
    private LocalDateTime scheduledTime;
}
