package com.beijixing.schedule.controller;

import com.beijixing.schedule.entity.ScheduleJob;
import com.beijixing.schedule.service.ScheduleService;
import com.beijixing.schedule.vo.JobVO;
import com.beijixing.schedule.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 定时任务控制器
 *
 * 原名: JobController (已重命名避免与bx-system.JobController冲突)
 * 功能: 调度任务管理（CRUD、启停、手动触发）
 * 最后更新: 2026-05-20 (极简单体版)
 */
@Slf4j
@RestController
@RequestMapping("/job")
public class ScheduleJobController {

    @Autowired
    private ScheduleService scheduleService;

    @GetMapping("/list")
    public Result<List<JobVO>> listJobs() {
        try {
            List<JobVO> jobs = scheduleService.listJobs();
            return Result.success(jobs);
        } catch (Exception e) {
            log.error("查询任务列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<JobVO> getJob(@PathVariable Long id) {
        try {
            JobVO job = scheduleService.getJobById(id);
            if (job == null) {
                return Result.error("任务不存在");
            }
            return Result.success(job);
        } catch (Exception e) {
            log.error("查询任务详情失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @PostMapping
    public Result<Void> createJob(@RequestBody ScheduleJob job) {
        try {
            boolean success = scheduleService.createJob(job);
            return success ? Result.success("任务创建成功") : Result.error("任务创建失败");
        } catch (Exception e) {
            log.error("创建任务失败", e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Void> updateJob(@PathVariable Long id, @RequestBody ScheduleJob job) {
        try {
            job.setId(id);
            boolean success = scheduleService.updateJob(job);
            return success ? Result.success("任务更新成功") : Result.error("任务更新失败");
        } catch (Exception e) {
            log.error("更新任务失败", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteJob(@PathVariable Long id) {
        try {
            boolean success = scheduleService.deleteJob(id);
            return success ? Result.success("任务删除成功") : Result.error("任务删除失败");
        } catch (Exception e) {
            log.error("删除任务失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/start")
    public Result<Void> startJob(@PathVariable Long id) {
        try {
            boolean success = scheduleService.startJob(id);
            return success ? Result.success("任务启动成功") : Result.error("任务启动失败");
        } catch (Exception e) {
            log.error("启动任务失败", e);
            return Result.error("启动失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/stop")
    public Result<Void> stopJob(@PathVariable Long id) {
        try {
            boolean success = scheduleService.stopJob(id);
            return success ? Result.success("任务停止成功") : Result.error("任务停止失败");
        } catch (Exception e) {
            log.error("停止任务失败", e);
            return Result.error("停止失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/execute")
    public Result<Void> executeJob(@PathVariable Long id,
                                   @RequestParam(required = false) String params) {
        try {
            scheduleService.executeJob(id, params);
            return Result.success("任务触发成功");
        } catch (Exception e) {
            log.error("触发任务失败", e);
            return Result.error("触发失败: " + e.getMessage());
        }
    }
}