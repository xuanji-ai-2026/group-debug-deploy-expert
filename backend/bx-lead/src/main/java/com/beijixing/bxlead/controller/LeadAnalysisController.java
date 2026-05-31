package com.beijixing.bxlead.controller;

import com.beijixing.bxlead.service.LeadAnalysisService;
import com.beijixing.bxlead.vo.LeadFunnelVO;
import com.beijixing.bxlead.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商机分析控制器
 */
@RestController
@RequestMapping("/lead/analysis")
@RequiredArgsConstructor
public class LeadAnalysisController {
    
    private final LeadAnalysisService leadAnalysisService;
    
    /**
     * 商机漏斗分析
     */
    @GetMapping("/funnel")
    public Result<List<LeadFunnelVO>> funnel() {
        return Result.success(leadAnalysisService.getFunnelAnalysis());
    }
    
    /**
     * 按状态统计
     */
    @GetMapping("/status")
    public Result<Map<String, Integer>> byStatus() {
        return Result.success(leadAnalysisService.countByStatus());
    }
    
    /**
     * 按等级统计
     */
    @GetMapping("/level")
    public Result<Map<String, Integer>> byLevel() {
        return Result.success(leadAnalysisService.countByLevel());
    }
    
    /**
     * 按来源统计
     */
    @GetMapping("/source")
    public Result<Map<String, Integer>> bySource() {
        return Result.success(leadAnalysisService.countBySource());
    }
    
    /**
     * 导出商机数据
     */
    @PostMapping("/export")
    public Result<String> export(@RequestParam List<Long> leadIds, @RequestParam(defaultValue = "xlsx") String format) {
        String fileName = leadAnalysisService.exportLeads(leadIds, format);
        Result<String> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(fileName);
        return result;
    }
}
