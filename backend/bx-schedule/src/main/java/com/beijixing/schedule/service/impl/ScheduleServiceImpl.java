package com.beijixing.schedule.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxl.job.core.context.XxlJobHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beijixing.schedule.entity.ScheduleJob;
import com.beijixing.schedule.mapper.ScheduleJobMapper;
import com.beijixing.schedule.service.ScheduleService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings("nullness")
public class ScheduleServiceImpl extends ServiceImpl<ScheduleJobMapper, ScheduleJob> implements ScheduleService {

    private static final String LOCK_PREFIX = "bx:schedule:lock:";
    private static final long LOCK_EXPIRE_SECONDS = 60;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${schedule.enable-distributed-lock:true}")
    private boolean enableDistributedLock;

    @Override
    public List<com.beijixing.schedule.vo.JobVO> listJobs() {
        return baseMapper.selectJobList();
    }

    @Override
    public com.beijixing.schedule.vo.JobVO getJobById(Long id) {
        return baseMapper.selectJobById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createJob(ScheduleJob job) {
        job.setStatus(0);
        return this.save(job);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateJob(ScheduleJob job) {
        return this.updateById(job);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteJob(Long id) {
        return this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean startJob(Long id) {
        ScheduleJob job = this.getById(id);
        if (job == null) {
            throw new RuntimeException("任务不存在");
        }
        job.setStatus(1);
        return this.updateById(job);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean stopJob(Long id) {
        ScheduleJob job = this.getById(id);
        if (job == null) {
            throw new RuntimeException("任务不存在");
        }
        job.setStatus(0);
        return this.updateById(job);
    }

    @Override
    public void executeJob(Long id, String params) {
        XxlJobHelper.log("手动触发任务执行, jobId={}, params={}", id, params);
        log.info("手动触发任务执行, jobId={}, params={}", id, params);
    }

    /**
     * 尝试获取分布式锁
     */
    public boolean tryLock(String lockKey) {
        if (!enableDistributedLock) {
            return true;
        }
        String key = LOCK_PREFIX + lockKey;
        String value = UUID.randomUUID().toString();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, value, LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放分布式锁
     */
    public void releaseLock(String lockKey) {
        if (!enableDistributedLock) {
            return;
        }
        String key = LOCK_PREFIX + lockKey;
        redisTemplate.delete(key);
    }
}
