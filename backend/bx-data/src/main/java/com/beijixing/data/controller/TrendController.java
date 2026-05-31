package com.beijixing.data.controller;

import com.beijixing.data.service.TrendService;
import com.beijixing.data.vo.TrendChartVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 趋势分析控制器
 * 
 * @author 陈工（EMP-DATA-001）
 */
@Slf4j
@RestController
@RequestMapping("/data/trend")
@RequiredArgsConstructor
@Tag(name = "趋势分析", description = "趋势分析接口")
public class TrendController {

    private final TrendService trendService;

    /**
     * DA-006: 趋势分析
     * 
     * GET /api/v1/data/trend/{type}
     */
    @GetMapping("/{type}")
    @Operation(summary = "趋势分析", description = "获取数据趋势分析（支持同比、环比）")
    public ResponseEntity<ApiResponse<List<TrendChartVO>>> getTrend(
            @Parameter(description = "租户ID") 
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Long tenantId,
            @Parameter(description = "趋势类型(OPERATION/LEAD/ACCOUNT/BILLING)") 
            @PathVariable String type,
            @Parameter(description = "开始日期") 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "对比类型(YOY同比/MOM环比)") 
            @RequestParam(required = false, defaultValue = "MOM") String compareType) {
        
        String start = startDate != null ? startDate.toString() : LocalDate.now().minusDays(30).toString();
        String end = endDate != null ? endDate.toString() : LocalDate.now().toString();
        
        List<TrendChartVO> data;
        if ("YOY".equalsIgnoreCase(compareType)) {
            data = trendService.getYearOverYearTrend(tenantId, type, start, end);
        } else {
            data = trendService.getMonthOverMonthTrend(tenantId, type, start, end);
        }
        
        return success(data);
    }

    private <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 统一响应封装
     */
    public static class ApiResponse<T> {
        private int code;
        private String message;
        private T data;
        private long timestamp;

        public static <T> ApiResponse<T> success(T data) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setCode(200);
            response.setMessage("success");
            response.setData(data);
            response.setTimestamp(System.currentTimeMillis());
            return response;
        }

        // getters and setters
        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}
