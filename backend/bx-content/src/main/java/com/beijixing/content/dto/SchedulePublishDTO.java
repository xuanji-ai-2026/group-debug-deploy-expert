package com.beijixing.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时发布配置DTO
 * @author 胡云 (EMP-CONTENT-001)
 */
@Data
@Schema(description = "定时发布配置")
public class SchedulePublishDTO {

    @Schema(description = "内容ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> contentIds;

    @Schema(description = "计划发布时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime scheduledTime;

    @Schema(description = "发布平台列表: 1-微信公众号 2-微博 3-抖音 4-小红书 5-B站 6-官网")
    private List<Integer> platforms;
}
