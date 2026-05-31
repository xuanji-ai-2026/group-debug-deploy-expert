package com.beijixing.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志实体
 * 对应数据库表: sys_oper_log
 *
 * 功能：SM-006 日志管理（操作日志、审计日志）
 *
 * @author bx-system
 */
@Data
@TableName("sys_oper_log")
public class SysOperLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模块名称（如：UserController）
     */
    private String moduleName;

    /**
     * 操作类型：INSERT、UPDATE、DELETE、SELECT、LOGIN、LOGOUT、EXPORT、IMPORT 等
     */
    private String operType;

    /**
     * 操作描述
     */
    private String operDesc;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * 请求参数（JSON）
     */
    private String requestParams;

    /**
     * 响应结果（JSON）
     */
    private String responseResult;

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 操作用户名
     */
    private String userName;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 操作IP地址
     */
    private String operIp;

    /**
     * 操作地点
     */
    private String operLocation;

    /**
     * User-Agent
     */
    private String userAgent;

    /**
     * 执行时长（毫秒）
     */
    private Long duration;

    /**
     * 操作状态：0-失败，1-成功
     */
    private Integer status;

    /**
     * 错误消息
     */
    private String errorMsg;

    /**
     * 操作时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime operTime;

    /**
     * 删除标记：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
}
