package com.beijixing.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beijixing.system.entity.SysJob;
import com.beijixing.system.entity.SysJobLog;
import com.beijixing.system.mapper.SysJobLogMapper;
import com.beijixing.system.mapper.SysJobMapper;
import com.beijixing.system.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务服务实现
 *
 * @author bx-system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final SysJobMapper jobMapper;
    private final SysJobLogMapper jobLogMapper;

    // ==================== 任务管理 ====================

    @Override
    public Page<SysJob> pageJobs(Integer page, Integer size, String jobName, String jobGroup, Integer status) {
        Page<SysJob> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysJob> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(jobName)) {
            wrapper.like(SysJob::getJobName, jobName);
        }
        if (StringUtils.hasText(jobGroup)) {
            wrapper.eq(SysJob::getJobGroup, jobGroup);
        }
        if (status != null) {
            wrapper.eq(SysJob::getStatus, status);
        }
        wrapper.orderByAsc(SysJob::getId);

        return jobMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public SysJob getJobById(Long id) {
        return jobMapper.selectById(id);
    }

    @Override
    public Long createJob(SysJob job) {
        // 设置默认值
        if (job.getStatus() == null) {
            job.setStatus(0); // 默认暂停
        }
        if (job.getConcurrent() == null) {
            job.setConcurrent(1); // 允许并发
        }
        if (job.getMisfirePolicy() == null) {
            job.setMisfirePolicy(1); // 立即触发
        }
        jobMapper.insert(job);
        log.info("创建定时任务：{} - {}", job.getJobName(), job.getCronExpression());
        return job.getId();
    }

    @Override
    public void updateJob(Long id, SysJob job) {
        SysJob existing = jobMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("任务不存在：" + id);
        }
        job.setId(id);
        jobMapper.updateById(job);
        log.info("更新定时任务：id={}", id);
    }

    @Override
    public void deleteJob(Long id) {
        jobMapper.deleteById(id);
        log.info("删除定时任务：id={}", id);
    }

    @Override
    public void executeOnce(Long id) {
        SysJob job = jobMapper.selectById(id);
        if (job == null) {
            throw new IllegalArgumentException("任务不存在：" + id);
        }
        log.info("手动执行定时任务：id={}, name={}", id, job.getJobName());
        // 实际执行逻辑通过异步任务调度
        // 此处仅记录日志，实际触发由调度器完成
    }

    @Override
    public void startJob(Long id) {
        SysJob job = jobMapper.selectById(id);
        if (job == null) {
            throw new IllegalArgumentException("任务不存在：" + id);
        }
        job.setStatus(1); // 运行中
        jobMapper.updateById(job);
        log.info("启动定时任务：id={}, name={}", id, job.getJobName());
    }

    @Override
    public void pauseJob(Long id) {
        SysJob job = jobMapper.selectById(id);
        if (job == null) {
            throw new IllegalArgumentException("任务不存在：" + id);
        }
        job.setStatus(0); // 暂停
        jobMapper.updateById(job);
        log.info("暂停定时任务：id={}, name={}", id, job.getJobName());
    }

    // ==================== 执行日志 ====================

    @Override
    public Page<SysJobLog> pageJobLogs(Integer page, Integer size, Long jobId, Integer execStatus) {
        Page<SysJobLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysJobLog> wrapper = new LambdaQueryWrapper<>();

        if (jobId != null) {
            wrapper.eq(SysJobLog::getJobId, jobId);
        }
        if (execStatus != null) {
            wrapper.eq(SysJobLog::getExecStatus, execStatus);
        }
        wrapper.orderByDesc(SysJobLog::getCreateTime);

        return jobLogMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public List<SysJobLog> getRecentLogs(Long jobId, Integer limit) {
        return jobLogMapper.selectRecentByJobId(jobId, limit);
    }

    @Override
    public List<SysJobLog> getFailedLogs(Integer limit) {
        return jobLogMapper.selectFailedLogs(limit);
    }

    @Override
    public void saveJobLog(SysJobLog log) {
        jobLogMapper.insert(log);
    }

    @Async
    @Override
    public int cleanOldLogs(Integer days) {
        LocalDateTime beforeTime = LocalDateTime.now().minusDays(days);
        LambdaQueryWrapper<SysJobLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.le(SysJobLog::getCreateTime, beforeTime);
        int count = jobLogMapper.delete(wrapper);
        log.info("清理 {} 天前的任务日志，删除 {} 条", days, count);
        return count;
    }
}
