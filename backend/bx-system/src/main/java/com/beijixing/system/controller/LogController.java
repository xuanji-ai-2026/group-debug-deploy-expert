package com.beijixing.system.controller;

import com.beijixing.system.entity.SysOperLog;
import com.beijixing.system.service.OperLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志控制器
 *
 * 功能：SM-006 日志管理（操作日志、审计日志）
 *
 * @author bx-system
 */
@Slf4j
@RestController
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class LogController {

    private final OperLogService operLogService;

    /**
     * SM-006-01: 查询最近的日志
     * GET /api/v1/admin/logs/recent
     */
    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> getRecentLogs(
            @RequestParam(required = false, defaultValue = "50") Integer limit) {
        List<SysOperLog> logs = operLogService.getRecentLogs(limit);
        return successData(logs);
    }

    /**
     * SM-006-02: 根据用户查询日志
     * GET /api/v1/admin/logs/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getLogsByUser(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "50") Integer limit) {
        List<SysOperLog> logs = operLogService.getLogsByUserId(userId, limit);
        return successData(logs);
    }

    /**
     * SM-006-03: 根据时间范围查询日志
     * GET /api/v1/admin/logs/time-range
     */
    @GetMapping("/time-range")
    public ResponseEntity<Map<String, Object>> getLogsByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        List<SysOperLog> logs = operLogService.getLogsByTimeRange(startTime, endTime);
        return successData(logs);
    }

    /**
     * SM-006-04: 获取日志详情
     * GET /api/v1/admin/logs/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getLog(@PathVariable Long id) {
        SysOperLog log = operLogService.getById(id);
        if (log == null) {
            return fail("日志不存在", 40406);
        }
        return successData(log);
    }

    /**
     * SM-006-05: 获取操作统计
     * GET /api/v1/admin/logs/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        // 默认查询最近7天
        if (startTime == null) {
            startTime = LocalDateTime.now().minusDays(7);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }
        Map<String, Object> stats = operLogService.getStatistics(startTime, endTime);
        return successData(stats);
    }

    /**
     * SM-006-06: 清理旧日志
     * DELETE /api/v1/admin/logs/clean
     */
    @DeleteMapping("/clean")
    public ResponseEntity<Map<String, Object>> cleanOldLogs(
            @RequestParam(required = false, defaultValue = "90") Integer days) {
        log.info("清理 {} 天前的操作日志", days);
        int count = operLogService.cleanOldLogs(days);
        return success("日志清理成功", Map.of("cleanedCount", count));
    }

    // ==================== 统一响应封装 ====================

    private ResponseEntity<Map<String, Object>> success(String message, Object data) {
        return successData(message, data);
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
