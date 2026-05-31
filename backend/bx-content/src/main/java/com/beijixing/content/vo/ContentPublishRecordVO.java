package com.beijixing.content.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发布记录VO
 * @author 胡云 (EMP-CONTENT-001)
 */
@Data
@Schema(description = "发布记录")
public class ContentPublishRecordVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "内容ID")
    private Long contentId;

    @Schema(description = "发布平台: 1-微信公众号 2-微博 3-抖音 4-小红书 5-B站 6-官网")
    private Integer platform;

    @Schema(description = "平台名称")
    private String platformName;

    @Schema(description = "平台内容ID")
    private String platformContentId;

    @Schema(description = "平台URL")
    private String platformUrl;

    @Schema(description = "发布状态: 0-待发布 1-发布中 2-发布成功 3-发布失败")
    private Integer status;

    @Schema(description = "发布状态名称")
    private String statusName;

    @Schema(description = "重试次数")
    private Integer retryCount;

    @Schema(description = "最大重试次数")
    private Integer maxRetryCount;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "完成时间")
    private LocalDateTime completeTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
