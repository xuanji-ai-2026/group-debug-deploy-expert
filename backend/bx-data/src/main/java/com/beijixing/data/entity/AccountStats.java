package com.beijixing.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账号统计实体
 * 
 * @author 陈工（EMP-DATA-001）
 */
@Data
@TableName("bx_account_stats")
public class AccountStats implements Serializable {

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
     * 账号ID
     */
    private Long accountId;

    /**
     * 账号名称
     */
    private String accountName;

    /**
     * 账号平台（WECHAT/DOUYIN/XIAOHONGSHU/KUAISHOU）
     */
    private String platform;

    /**
     * 账号状态（ACTIVE/INACTIVE/BANNED）
     */
    private String status;

    /**
     * 评分
     */
    private BigDecimal score;

    /**
     * 粉丝数
     */
    private Integer followers;

    /**
     * 获客数
     */
    private Integer leads;

    /**
     * 统计日期
     */
    private String statDate;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
