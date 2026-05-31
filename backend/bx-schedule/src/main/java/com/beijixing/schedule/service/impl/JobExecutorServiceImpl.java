package com.beijixing.schedule.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.beijixing.schedule.entity.ScheduleJobLog;
import com.beijixing.schedule.mapper.ScheduleJobLogMapper;
import com.beijixing.schedule.service.JobExecutorService;
import com.beijixing.schedule.vo.JobLogVO;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class JobExecutorServiceImpl extends ServiceImpl<ScheduleJobLogMapper, ScheduleJobLog>
        implements JobExecutorService {

    @Override
    public ScheduleJobLog recordStart(Long jobId, String jobName, String jobType, String params) {
        ScheduleJobLog log = new ScheduleJobLog();
        log.setJobId(jobId);
        log.setJobName(jobName);
        log.setJobType(jobType);
        log.setParams(params);
        log.setStatus(2); // 进行中
        log.setRetryTimes(0);
        log.setStartTime(LocalDateTime.now());
        log.setHostAddress(getHostAddress());
        this.save(log);
        return log;
    }

    @Override
    @Async("logExecutor")
    public void recordSuccess(Long logId, String result) {
        ScheduleJobLog logEntity = this.getById(logId);
        if (logEntity != null) {
            logEntity.setStatus(1); // 成功
            logEntity.setEndTime(LocalDateTime.now());
            logEntity.setResult(result);
            if (logEntity.getStartTime() != null) {
                logEntity.setCostTime(
                        java.time.Duration.between(logEntity.getStartTime(), logEntity.getEndTime()).toMillis()
                );
            }
            this.updateById(logEntity);
        }
    }

    @Override
    @Async("logExecutor")
    public void recordFail(Long logId, String failReason, int retryTimes) {
        ScheduleJobLog logEntity = this.getById(logId);
        if (logEntity != null) {
            logEntity.setStatus(0); // 失败
            logEntity.setEndTime(LocalDateTime.now());
            logEntity.setFailReason(failReason);
            logEntity.setRetryTimes(retryTimes);
            if (logEntity.getStartTime() != null) {
                logEntity.setCostTime(
                        java.time.Duration.between(logEntity.getStartTime(), logEntity.getEndTime()).toMillis()
                );
            }
            this.updateById(logEntity);
        }
    }

    @Override
    public List<JobLogVO> listLogs(Long jobId, Integer status, String startTime, String endTime) {
        return baseMapper.selectJobLogList(jobId, status, startTime, endTime);
    }

    @Override
    public JobLogVO getLogById(Long id) {
        return baseMapper.selectJobLogById(id);
    }

    @Override
    public List<JobLogVO> listRecentLogs(int limit) {
        return baseMapper.selectRecentLogs(limit);
    }

    private String getHostAddress() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
