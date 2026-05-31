package com.beijixing.social.controller;

import com.beijixing.social.entity.AccountLog;
import com.beijixing.social.service.AccountLogService;
import com.beijixing.social.vo.ApiResponse;
import com.beijixing.social.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

/**
 * 社交账号日志查询控制器
 *
 * 原名: LogController (已重命名避免与bx-system.LogController冲突)
 * 功能: 社交平台账号操作日志查询
 * 最后更新: 2026-05-20 (极简单体版)
 */
@RestController
@RequestMapping("/log")
@RequiredArgsConstructor
public class SocialAccountLogController {

    private final AccountLogService logService;

    /** 分页查询日志 */
    @GetMapping("/page")
    public ApiResponse<PageVO<AccountLog>> pageLogs(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "20") Long pageSize,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String platformCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ApiResponse.success(logService.pageLogs(pageNum, pageSize, accountId, actionType, platformCode, startTime, endTime));
    }

    /** 获取日志详情 */
    @GetMapping("/{logId}")
    public ApiResponse<AccountLog> getLogDetail(@PathVariable Long logId) {
        return ApiResponse.success(logService.getById(logId));
    }
}
