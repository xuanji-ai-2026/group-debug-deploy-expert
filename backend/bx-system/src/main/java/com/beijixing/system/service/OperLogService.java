package com.beijixing.system.service;

import com.beijixing.system.entity.SysOperLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 操作日志服务接口
 *
 * 功能：SM-006 日志管理（操作日志、审计日志）
 *
 * @author bx-system
 */
public interface OperLogService {

    /**
     * 记录操作日志
     *
     * @param log 日志实体
     */
    void saveLog(SysOperLog log);

    /**
     * 异步记录操作日志（推荐使用）
     *
     * @param moduleName 模块名称
     * @param operType 操作类型
     * @param operDesc 操作描述
     * @param requestMethod 请求方法
     * @param requestUrl 请求URL
     * @param requestParams 请求参数（JSON字符串）
     * @param userId 用户ID
     * @param userName 用户名
     * @param tenantId 租户ID
     * @param operIp 操作IP
     * @param operLocation 操作地点
     * @param userAgent User-Agent
     * @param duration 执行时长（毫秒）
     * @param status 操作状态：0-失败，1-成功
     * @param errorMsg 错误消息
     */
    void saveLogAsync(String moduleName, String operType, String operDesc,
                      String requestMethod, String requestUrl, String requestParams,
                      Long userId, String userName, Long tenantId,
                      String operIp, String operLocation, String userAgent,
                      Long duration, Integer status, String errorMsg);

    /**
     * 查询最近的日志
     *
     * @param limit 返回数量
     * @return 日志列表
     */
    List<SysOperLog> getRecentLogs(Integer limit);

    /**
     * 根据用户查询日志
     *
     * @param userId 用户ID
     * @param limit 返回数量
     * @return 日志列表
     */
    List<SysOperLog> getLogsByUserId(Long userId, Integer limit);

    /**
     * 根据时间范围查询日志
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日志列表
     */
    List<SysOperLog> getLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取操作统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计数据
     */
    Map<String, Object> getStatistics(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 清理旧日志
     *
     * @param days 保留天数
     * @return 清理数量
     */
    int cleanOldLogs(Integer days);

    /**
     * 获取日志详情
     *
     * @param id 日志ID
     * @return 日志实体
     */
    SysOperLog getById(Long id);
}
