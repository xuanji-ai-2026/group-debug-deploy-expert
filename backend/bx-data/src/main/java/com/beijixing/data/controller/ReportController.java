package com.beijixing.data.controller;

import com.beijixing.data.service.ReportService;
import com.beijixing.data.vo.ReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * 报表控制器
 * 
 * @author 陈工（EMP-DATA-001）
 */
@Slf4j
@RestController
@RequestMapping("/data/report")
@RequiredArgsConstructor
@Tag(name = "报表管理", description = "报表导出接口")
public class ReportController {

    private final ReportService reportService;

    /**
     * DA-005: 报表导出
     * 
     * GET /api/v1/data/report/export
     */
    @GetMapping("/export")
    @Operation(summary = "导出报表", description = "导出Excel/CSV格式报表")
    public ResponseEntity<byte[]> exportReport(
            @Parameter(description = "租户ID") 
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Long tenantId,
            @Parameter(description = "报表类型(OPERATION/LEAD/ACCOUNT/BILLING)") 
            @RequestParam(defaultValue = "OPERATION") String reportType,
            @Parameter(description = "开始日期") 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "导出格式(EXCEL/CSV)") 
            @RequestParam(defaultValue = "EXCEL") String format) {
        
        String start = startDate != null ? startDate.toString() : LocalDate.now().minusDays(30).toString();
        String end = endDate != null ? endDate.toString() : LocalDate.now().toString();
        
        String filename = String.format("%s_%s_%s.%s", 
                reportType, start, end, 
                "CSV".equalsIgnoreCase(format) ? "csv" : "xlsx");
        
        if ("CSV".equalsIgnoreCase(format)) {
            return exportCsv(tenantId, reportType, start, end, filename);
        } else {
            return exportExcel(tenantId, reportType, start, end, filename);
        }
    }

    /**
     * 导出CSV格式
     */
    private ResponseEntity<byte[]> exportCsv(Long tenantId, String reportType, 
                                              String startDate, String endDate, String filename) {
        String csvContent = reportService.exportCsvReport(tenantId, reportType, startDate, endDate);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csvContent.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 导出Excel格式
     */
    private ResponseEntity<byte[]> exportExcel(Long tenantId, String reportType, 
                                                String startDate, String endDate, String filename) {
        List<ReportVO> reportList = reportService.exportReport(tenantId, reportType, startDate, endDate);
        
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            com.alibaba.excel.EasyExcel.write(out, ReportVO.class)
                    .sheet("报表数据")
                    .doWrite(reportList);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        } catch (Exception e) {
            log.error("导出Excel失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
