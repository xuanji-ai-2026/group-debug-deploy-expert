package com.beijixing.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 运营统计实体
 * 
 * @author 陈工（EMP-DATA-001）
 */
@Data
@TableName("bx_operation_stats")
public class OperationStats implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 统计日期
     */
    private String statDate;

    /**
     * 发布数量
     */
    private Integer publishCount;

    /**
     * 浏览数量
     */
    private Integer viewCount;

    /**
     * 点赞数量
     */
    private Integer likeCount;

    /**
     * 评论数量
     */
    private Integer commentCount;

    /**
     * 分享数量
     */
    private Integer shareCount;

    /**
     * 收藏数量
     */
    private Integer favoriteCount;

    /**
     * 互动率
     */
    private Double interactionRate;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
