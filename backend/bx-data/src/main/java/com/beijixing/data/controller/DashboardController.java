package com.beijixing.data.controller;

import com.beijixing.data.vo.DashboardVO;
import com.beijixing.data.service.DashboardService;
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
 * 数据看板控制器
 * 
 * @author 陈工（EMP-DATA-001）
 */
@Slf4j
@RestController
@RequestMapping("/data/dashboard")
@RequiredArgsConstructor
@SuppressWarnings({"nullness", "rawtypes"})
@Tag(name = "数据看板", description = "数据看板接口")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * DA-001: 运营数据看板
     * 
     * GET /api/v1/data/dashboard/operation
     */
    @GetMapping("/operation")
    @Operation(summary = "运营数据看板", description = "获取运营数据看板（发布统计、互动统计）")
    public ResponseEntity<ApiResponse<DashboardVO>> getOperationDashboard(
            @Parameter(description = "租户ID") 
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Long tenantId,
            @Parameter(description = "开始日期") 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        String start = startDate != null ? startDate.toString() : LocalDate.now().minusDays(30).toString();
        String end = endDate != null ? endDate.toString() : LocalDate.now().toString();
        
        DashboardVO data = dashboardService.getOperationDashboard(tenantId, start, end);
        return success(data);
    }

    /**
     * DA-002: 获客数据看板
     * 
     * GET /api/v1/data/dashboard/lead
     */
    @GetMapping("/lead")
    @Operation(summary = "获客数据看板", description = "获取商机数据看板（商机统计、转化分析）")
    public ResponseEntity<ApiResponse<DashboardVO>> getLeadDashboard(
            @Parameter(description = "租户ID") 
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Long tenantId,
            @Parameter(description = "开始日期") 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        String start = startDate != null ? startDate.toString() : LocalDate.now().minusDays(30).toString();
        String end = endDate != null ? endDate.toString() : LocalDate.now().toString();
        
        DashboardVO data = dashboardService.getLeadDashboard(tenantId, start, end);
        return success(data);
    }

    /**
     * DA-003: 账号数据看板
     * 
     * GET /api/v1/data/dashboard/account
     */
    @GetMapping("/account")
    @Operation(summary = "账号数据看板", description = "获取账号数据看板（账号状态、评分趋势）")
    public ResponseEntity<ApiResponse<DashboardVO>> getAccountDashboard(
            @Parameter(description = "租户ID") 
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Long tenantId,
            @Parameter(description = "开始日期") 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        String start = startDate != null ? startDate.toString() : LocalDate.now().minusDays(30).toString();
        String end = endDate != null ? endDate.toString() : LocalDate.now().toString();
        
        DashboardVO data = dashboardService.getAccountDashboard(tenantId, start, end);
        return success(data);
    }

    /**
     * DA-004: 消费数据看板
     * 
     * GET /api/v1/data/dashboard/billing
     */
    @GetMapping("/billing")
    @Operation(summary = "消费数据看板", description = "获取消费数据看板（积分消费、成本分析）")
    public ResponseEntity<ApiResponse<DashboardVO>> getBillingDashboard(
            @Parameter(description = "租户ID") 
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Long tenantId,
            @Parameter(description = "开始日期") 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        String start = startDate != null ? startDate.toString() : LocalDate.now().minusDays(30).toString();
        String end = endDate != null ? endDate.toString() : LocalDate.now().toString();
        
        DashboardVO data = dashboardService.getBillingDashboard(tenantId, start, end);
        return success(data);
    }

    /**
     * 获取运营趋势
     */
    @GetMapping("/operation/trend")
    @Operation(summary = "运营趋势", description = "获取运营数据趋势")
    public ResponseEntity<ApiResponse<List>> getOperationTrend(
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Long tenantId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        String start = startDate != null ? startDate.toString() : LocalDate.now().minusDays(30).toString();
        String end = endDate != null ? endDate.toString() : LocalDate.now().toString();
        
        return success(dashboardService.getOperationTrend(tenantId, start, end));
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
