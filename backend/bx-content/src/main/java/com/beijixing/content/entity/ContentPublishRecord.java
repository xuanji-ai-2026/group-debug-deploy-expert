package com.beijixing.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 内容发布记录表
 * @author 胡云 (EMP-CONTENT-001)
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("content_publish_record")
public class ContentPublishRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 记录ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 内容ID */
    private Long contentId;

    /** 发布平台: 1-微信公众号 2-微博 3-抖音 4-小红书 5-B站 6-官网 */
    private Integer platform;

    /** 平台内容ID */
    private String platformContentId;

    /** 平台URL */
    private String platformUrl;

    /** 发布状态: 0-待发布 1-发布中 2-发布成功 3-发布失败 */
    private Integer status;

    /** 重试次数 */
    private Integer retryCount;

    /** 最大重试次数 */
    private Integer maxRetryCount;

    /** 错误信息 */
    private String errorMsg;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 完成时间 */
    private LocalDateTime completeTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
