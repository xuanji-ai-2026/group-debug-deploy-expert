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
 * 系统操作日志控制器（前端统一路径映射）
 *
 * 前端路径: /system/log/*
 * 实际代理到: /admin/logs/*
 *
 * @author bx-system
 */
@Slf4j
@RestController
@RequestMapping("/system/log")
@RequiredArgsConstructor
public class SystemLogController {

    private final OperLogService operLogService;

    /**
     * 获取操作日志列表
     * GET /system/log/list
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getLogList(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        List<SysOperLog> logs;
        if (startDate != null && endDate != null) {
            LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
            LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");
            logs = operLogService.getLogsByTimeRange(start, end);
        } else if (userId != null) {
            logs = operLogService.getLogsByUserId(userId, size);
        } else {
            logs = operLogService.getRecentLogs(size);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", Map.of("list", logs, "total", logs.size(), "page", page, "size", size));
        return ResponseEntity.ok(result);
    }

    /**
     * 获取操作日志详情
     * GET /system/log/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getLogDetail(@PathVariable Long id) {
        SysOperLog log = operLogService.getById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", log);
        return ResponseEntity.ok(result);
    }
}
