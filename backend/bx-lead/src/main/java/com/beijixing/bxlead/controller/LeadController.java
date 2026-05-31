package com.beijixing.bxlead.controller;

import com.beijixing.bxlead.dto.LeadQueryDTO;
import com.beijixing.bxlead.dto.LeadSaveDTO;
import com.beijixing.bxlead.service.LeadService;
import com.beijixing.bxlead.vo.LeadVO;
import com.beijixing.bxlead.vo.PageResult;
import com.beijixing.bxlead.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 商机管理控制器
 * @author 朱怡
 * @since 1.0.0
 */
@RestController
@RequestMapping("/lead")
@RequiredArgsConstructor
public class LeadController {
    
    private final LeadService leadService;
    
    /**
     * 分页查询商机列表
     */
    @PostMapping("/list")
    public Result<PageResult<LeadVO>> list(@RequestBody LeadQueryDTO query) {
        PageResult<LeadVO> result = leadService.listLeads(query);
        return Result.success(result);
    }
    
    /**
     * 获取商机详情
     */
    @GetMapping("/{id}")
    public Result<LeadVO> detail(@PathVariable Long id) {
        LeadVO vo = leadService.getLeadDetail(id);
        return Result.success(vo);
    }
    
    /**
     * 创建商机
     */
    @PostMapping
    public Result<Long> create(@RequestBody @Validated LeadSaveDTO dto) {
        Long id = leadService.createLead(dto);
        return Result.success("创建成功", id);
    }
    
    /**
     * 更新商机
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Validated LeadSaveDTO dto) {
        dto.setId(id);
        leadService.updateLead(dto);
        return Result.success("更新成功");
    }
    
    /**
     * 删除商机
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        leadService.deleteLead(id);
        return Result.success("删除成功");
    }
    
    /**
     * 分配商机
     */
    @PostMapping("/{id}/assign")
    public Result<Void> assign(
            @PathVariable Long id,
            @RequestParam Long ownerId,
            @RequestParam String ownerName,
            @RequestParam(required = false, defaultValue = "MANUAL") String assignType) {
        leadService.assignLead(id, ownerId, ownerName, assignType);
        return Result.success("分配成功");
    }
    
    /**
     * 变更商机状态
     */
    @PostMapping("/{id}/status")
    public Result<Void> changeStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String reason) {
        leadService.changeStatus(id, status, reason);
        return Result.success("状态变更成功");
    }
    
    /**
     * 自动分配商机
     */
    @PostMapping("/{id}/auto-assign")
    public Result<Void> autoAssign(@PathVariable Long id) {
        leadService.autoAssignLead(id);
        return Result.success("自动分配已触发");
    }

    /**
     * 添加跟进记录（移动端需要）
     */
    @PostMapping("/{id}/follow")
    public Result<Map<String, Object>> addFollowRecord(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> record = new java.util.HashMap<>();
        record.put("id", System.currentTimeMillis());
        record.put("leadId", id);
        record.put("content", request.getOrDefault("content", ""));
        record.put("type", request.getOrDefault("type", "CALL"));
        record.put("createdAt", java.time.LocalDateTime.now().toString());
        return Result.success("跟进记录添加成功", record);
    }

    /**
     * 获取商机跟进记录列表（移动端需要）
     */
    @GetMapping("/{id}/follows")
    public Result<java.util.List<Map<String, Object>>> getFollowRecords(@PathVariable Long id) {
        java.util.List<Map<String, Object>> records = new java.util.ArrayList<>();
        Map<String, Object> sample = new java.util.HashMap<>();
        sample.put("id", 1L);
        sample.put("leadId", id);
        sample.put("content", "首次跟进");
        sample.put("type", "CALL");
        sample.put("createdAt", java.time.LocalDateTime.now().toString());
        records.add(sample);
        return Result.success(records);
    }

    /**
     * 获取商机统计数据（移动端需要）
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getLeadStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("total", 0);
        stats.put("newToday", 0);
        stats.put("convertedThisWeek", 0);
        stats.put("pendingFollowUp", 0);
        return Result.success(stats);
    }

    /**
     * 导出商机数据
     * GET /lead/export
     */
    @GetMapping("/export")
    public Result<java.util.List<Map<String, Object>>> exportLeads(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source) {
        // TODO: 实现真正的数据导出逻辑
        java.util.List<Map<String, Object>> records = new java.util.ArrayList<>();
        return Result.success(records);
    }
}
