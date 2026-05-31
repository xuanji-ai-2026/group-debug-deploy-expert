package com.beijixing.schedule.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.beijixing.schedule.entity.ScheduleJobLog;
import com.beijixing.schedule.vo.JobLogVO;

import java.util.List;

public interface JobExecutorService extends IService<ScheduleJobLog> {

    /**
     * 记录开始执行
     */
    ScheduleJobLog recordStart(Long jobId, String jobName, String jobType, String params);

    /**
     * 记录执行成功
     */
    void recordSuccess(Long logId, String result);

    /**
     * 记录执行失败
     */
    void recordFail(Long logId, String failReason, int retryTimes);

    /**
     * 查询执行日志列表
     */
    List<JobLogVO> listLogs(Long jobId, Integer status, String startTime, String endTime);

    /**
     * 查询日志详情
     */
    JobLogVO getLogById(Long id);

    /**
     * 查询最近执行日志
     */
    List<JobLogVO> listRecentLogs(int limit);
}
