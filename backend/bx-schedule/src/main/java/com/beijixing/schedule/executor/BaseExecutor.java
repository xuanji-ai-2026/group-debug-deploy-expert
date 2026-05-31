package com.beijixing.schedule.executor;

import com.beijixing.schedule.entity.ScheduleJobLog;
import com.beijixing.schedule.service.JobExecutorService;
import com.beijixing.schedule.service.impl.ScheduleServiceImpl;
import com.xxl.job.core.context.XxlJobHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
public abstract class BaseExecutor {

    @Autowired
    protected JobExecutorService jobExecutorService;

    @Autowired
    protected ScheduleServiceImpl scheduleService;

    @Value("${schedule.task-timeout:300}")
    protected int taskTimeoutSeconds;

    /**
     * 任务超时时间（秒），子类可覆盖
     */
    protected int getTimeoutSeconds() {
        return taskTimeoutSeconds;
    }

    /**
     * 执行任务主逻辑
     */
    public void execute(String params) {
        String jobName = getJobName();
        String jobType = getJobType();

        log.info("[{}] 任务开始执行, params={}", jobName, params);
        XxlJobHelper.log("[{}] 任务开始执行, params={}", jobName, params);

        // 尝试获取分布式锁
        String lockKey = jobType + "_" + getLockKey(params);
        boolean locked = scheduleService.tryLock(lockKey);
        if (!locked) {
            String msg = "[{}] 获取分布式锁失败，任务正在其他节点执行";
            log.warn(msg, jobName);
            XxlJobHelper.log(msg, jobName);
            throw new RuntimeException("获取分布式锁失败");
        }

        ScheduleJobLog jobLog = null;
        try {
            // 记录开始
            jobLog = jobExecutorService.recordStart(null, jobName, jobType, params);

            // 执行任务
            String result = doExecute(params);

            // 记录成功
            jobExecutorService.recordSuccess(jobLog.getId(), result);

            log.info("[{}] 任务执行成功, result={}", jobName, result);
            XxlJobHelper.log("[{}] 任务执行成功, result={}", jobName, result);

        } catch (Exception e) {
            int retryTimes = jobLog != null ? jobLog.getRetryTimes() : 0;
            String failReason = e.getMessage();
            if (jobLog != null) {
                jobExecutorService.recordFail(jobLog.getId(), failReason, retryTimes);
            }

            log.error("[{}] 任务执行失败, failReason={}", jobName, failReason, e);
            XxlJobHelper.log("[{}] 任务执行失败, failReason={}", jobName, failReason);

            throw e;
        } finally {
            // 释放锁
            scheduleService.releaseLock(lockKey);
        }
    }

    /**
     * 执行具体任务逻辑
     */
    protected abstract String doExecute(String params);

    /**
     * 获取任务名称
     */
    protected abstract String getJobName();

    /**
     * 获取任务类型
     */
    protected abstract String getJobType();

    /**
     * 获取锁的键
     */
    protected String getLockKey(String params) {
        return "default";
    }
}
