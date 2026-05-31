package com.beijixing.schedule.controller;

import com.beijixing.schedule.service.JobExecutorService;
import com.beijixing.schedule.vo.JobLogVO;
import com.beijixing.schedule.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/job/log")
public class JobLogController {

    @Autowired
    private JobExecutorService jobExecutorService;

    /**
     * 查询执行日志列表
     */
    @GetMapping("/list")
    public Result<List<JobLogVO>> listLogs(
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        try {
            List<JobLogVO> logs = jobExecutorService.listLogs(jobId, status, startTime, endTime);
            return Result.success(logs);
        } catch (Exception e) {
            log.error("查询执行日志失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询日志详情
     */
    @GetMapping("/{id}")
    public Result<JobLogVO> getLog(@PathVariable Long id) {
        try {
            JobLogVO log = jobExecutorService.getLogById(id);
            if (log == null) {
                return Result.error("日志不存在");
            }
            return Result.success(log);
        } catch (Exception e) {
            log.error("查询日志详情失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询最近执行日志
     */
    @GetMapping("/recent")
    public Result<List<JobLogVO>> getRecentLogs(@RequestParam(defaultValue = "20") int limit) {
        try {
            List<JobLogVO> logs = jobExecutorService.listRecentLogs(limit);
            return Result.success(logs);
        } catch (Exception e) {
            log.error("查询最近日志失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
