package com.beijixing.schedule.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.beijixing.schedule.entity.ScheduleJob;
import com.beijixing.schedule.vo.JobVO;

import java.util.List;

public interface ScheduleService extends IService<ScheduleJob> {

    /**
     * 查询任务列表
     */
    List<JobVO> listJobs();

    /**
     * 查询任务详情
     */
    JobVO getJobById(Long id);

    /**
     * 创建任务
     */
    boolean createJob(ScheduleJob job);

    /**
     * 更新任务
     */
    boolean updateJob(ScheduleJob job);

    /**
     * 删除任务
     */
    boolean deleteJob(Long id);

    /**
     * 启动任务
     */
    boolean startJob(Long id);

    /**
     * 停止任务
     */
    boolean stopJob(Long id);

    /**
     * 手动执行任务
     */
    void executeJob(Long id, String params);
}
