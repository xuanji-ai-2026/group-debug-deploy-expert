package com.beijixing.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beijixing.system.entity.SysJob;
import com.beijixing.system.entity.SysJobLog;

import java.util.List;

/**
 * 定时任务服务接口
 *
 * 功能：SM-003 定时任务（任务配置、执行日志）
 *
 * @author bx-system
 */
public interface JobService {

    // ==================== 任务管理 ====================

    /**
     * 分页查询任务列表
     *
     * @param page 页码
     * @param size 每页数量
     * @param jobName 任务名称（模糊搜索）
     * @param jobGroup 任务分组
     * @param status 状态
     * @return 分页结果
     */
    Page<SysJob> pageJobs(Integer page, Integer size, String jobName, String jobGroup, Integer status);

    /**
     * 获取任务详情
     *
     * @param id 任务ID
     * @return 任务实体
     */
    SysJob getJobById(Long id);

    /**
     * 创建任务
     *
     * @param job 任务实体
     * @return 任务ID
     */
    Long createJob(SysJob job);

    /**
     * 更新任务
     *
     * @param id 任务ID
     * @param job 任务实体
     */
    void updateJob(Long id, SysJob job);

    /**
     * 删除任务
     *
     * @param id 任务ID
     */
    void deleteJob(Long id);

    /**
     * 立即执行一次任务
     *
     * @param id 任务ID
     */
    void executeOnce(Long id);

    /**
     * 启动任务（启用定时调度）
     *
     * @param id 任务ID
     */
    void startJob(Long id);

    /**
     * 暂停任务
     *
     * @param id 任务ID
     */
    void pauseJob(Long id);

    // ==================== 执行日志 ====================

    /**
     * 分页查询任务执行日志
     *
     * @param page 页码
     * @param size 每页数量
     * @param jobId 任务ID
     * @param execStatus 执行状态
     * @return 分页结果
     */
    Page<SysJobLog> pageJobLogs(Integer page, Integer size, Long jobId, Integer execStatus);

    /**
     * 获取任务最近执行日志
     *
     * @param jobId 任务ID
     * @param limit 返回数量
     * @return 日志列表
     */
    List<SysJobLog> getRecentLogs(Long jobId, Integer limit);

    /**
     * 获取任务执行失败日志
     *
     * @param limit 返回数量
     * @return 失败日志列表
     */
    List<SysJobLog> getFailedLogs(Integer limit);

    /**
     * 记录任务执行结果
     *
     * @param log 执行日志实体
     */
    void saveJobLog(SysJobLog log);

    /**
     * 清理旧日志
     *
     * @param days 保留天数
     * @return 清理数量
     */
    int cleanOldLogs(Integer days);
}
