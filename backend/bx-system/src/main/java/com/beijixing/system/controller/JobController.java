package com.beijixing.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beijixing.system.entity.SysJob;
import com.beijixing.system.entity.SysJobLog;
import com.beijixing.system.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时任务管理控制器
 *
 * 功能：SM-003 定时任务（任务配置、执行日志）
 *
 * @author bx-system
 */
@Slf4j
@RestController
@RequestMapping("/admin/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    /**
     * SM-003-01: 分页查询任务列表
     * GET /api/v1/admin/jobs
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> pageJobs(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) String jobName,
            @RequestParam(required = false) String jobGroup,
            @RequestParam(required = false) Integer status) {
        log.info("分页查询任务列表，page={}, size={}", page, size);
        Page<SysJob> result = jobService.pageJobs(page, size, jobName, jobGroup, status);
        return successData(result);
    }

    /**
     * SM-003-02: 获取任务详情
     * GET /api/v1/admin/jobs/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getJob(@PathVariable Long id) {
        SysJob job = jobService.getJobById(id);
        if (job == null) {
            return fail("任务不存在", 40403);
        }
        return successData(job);
    }

    /**
     * SM-003-03: 创建任务
     * POST /api/v1/admin/jobs
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createJob(@RequestBody SysJob job) {
        log.info("创建定时任务：{}", job.getJobName());
        Long id = jobService.createJob(job);
        return success("任务创建成功", Map.of("id", id));
    }

    /**
     * SM-003-04: 更新任务
     * PUT /api/v1/admin/jobs/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateJob(@PathVariable Long id, @RequestBody SysJob job) {
        log.info("更新定时任务：id={}", id);
        jobService.updateJob(id, job);
        return success("任务更新成功");
    }

    /**
     * SM-003-05: 删除任务
     * DELETE /api/v1/admin/jobs/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteJob(@PathVariable Long id) {
        log.info("删除定时任务：id={}", id);
        jobService.deleteJob(id);
        return success("任务删除成功");
    }

    /**
     * SM-003-06: 立即执行一次任务
     * POST /api/v1/admin/jobs/{id}/execute
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<Map<String, Object>> executeOnce(@PathVariable Long id) {
        log.info("手动执行任务：id={}", id);
        jobService.executeOnce(id);
        return success("任务已触发执行");
    }

    /**
     * SM-003-07: 启动任务
     * POST /api/v1/admin/jobs/{id}/start
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<Map<String, Object>> startJob(@PathVariable Long id) {
        log.info("启动任务：id={}", id);
        jobService.startJob(id);
        return success("任务已启动");
    }

    /**
     * SM-003-08: 暂停任务
     * POST /api/v1/admin/jobs/{id}/pause
     */
    @PostMapping("/{id}/pause")
    public ResponseEntity<Map<String, Object>> pauseJob(@PathVariable Long id) {
        log.info("暂停任务：id={}", id);
        jobService.pauseJob(id);
        return success("任务已暂停");
    }

    // ==================== 执行日志 ====================

    /**
     * SM-003-09: 分页查询执行日志
     * GET /api/v1/admin/jobs/{jobId}/logs
     */
    @GetMapping("/{jobId}/logs")
    public ResponseEntity<Map<String, Object>> pageJobLogs(
            @PathVariable Long jobId,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer execStatus) {
        log.info("分页查询任务执行日志，jobId={}, page={}", jobId, page);
        Page<SysJobLog> result = jobService.pageJobLogs(page, size, jobId, execStatus);
        return successData(result);
    }

    /**
     * SM-003-10: 获取最近执行日志
     * GET /api/v1/admin/jobs/{jobId}/logs/recent
     */
    @GetMapping("/{jobId}/logs/recent")
    public ResponseEntity<Map<String, Object>> getRecentLogs(
            @PathVariable Long jobId,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        List<SysJobLog> logs = jobService.getRecentLogs(jobId, limit);
        return successData(logs);
    }

    /**
     * SM-003-11: 获取失败日志
     * GET /api/v1/admin/jobs/logs/failed
     */
    @GetMapping("/logs/failed")
    public ResponseEntity<Map<String, Object>> getFailedLogs(
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        List<SysJobLog> logs = jobService.getFailedLogs(limit);
        return successData(logs);
    }

    /**
     * SM-003-12: 清理旧日志
     * DELETE /api/v1/admin/jobs/logs/clean
     */
    @DeleteMapping("/logs/clean")
    public ResponseEntity<Map<String, Object>> cleanOldLogs(
            @RequestParam(required = false, defaultValue = "30") Integer days) {
        log.info("清理 {} 天前的任务日志", days);
        int count = jobService.cleanOldLogs(days);
        return success("日志清理成功", Map.of("cleanedCount", count));
    }

    // ==================== 统一响应封装 ====================

    private ResponseEntity<Map<String, Object>> success(String message, Object data) {
        return successData(message, data);
    }

    private ResponseEntity<Map<String, Object>> success(String message) {
        return successData(message, null);
    }

    private ResponseEntity<Map<String, Object>> successData(Object data) {
        return successData("success", data);
    }

    private ResponseEntity<Map<String, Object>> successData(String message, Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", message);
        result.put("data", data);
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<Map<String, Object>> fail(String message, Integer code) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code != null ? code : 50000);
        result.put("message", message);
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }
}
