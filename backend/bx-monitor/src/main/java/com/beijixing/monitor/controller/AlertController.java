package com.beijixing.monitor.controller;

import com.beijixing.monitor.entity.AlertRecord;
import com.beijixing.monitor.entity.AlertRule;
import com.beijixing.monitor.service.AlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    /**
     * 获取告警列表
     */
    @GetMapping
    public Map<String, Object> getAlerts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<AlertRecord> alerts = alertService.getAlertList(status, page, size);
        return Map.of("code", 0, "data", alerts, "page", page, "size", size);
    }

    /**
     * 获取告警规则列表
     */
    @GetMapping("/rules")
    public Map<String, Object> getRules() {
        List<AlertRule> rules = alertService.getAllRules();
        return Map.of("code", 0, "data", rules);
    }

    /**
     * 获取单个告警规则
     */
    @GetMapping("/rules/{ruleId}")
    public Map<String, Object> getRule(@PathVariable String ruleId) {
        AlertRule rule = alertService.getRule(ruleId);
        if (rule == null) {
            return Map.of("code", 404, "message", "规则不存在");
        }
        return Map.of("code", 0, "data", rule);
    }

    /**
     * 创建告警规则
     */
    @PostMapping("/rules")
    public Map<String, Object> createRule(@RequestBody AlertRule rule) {
        AlertRule created = alertService.createRule(rule);
        return Map.of("code", 0, "data", created, "message", "规则创建成功");
    }

    /**
     * 更新告警规则
     */
    @PutMapping("/rules/{ruleId}")
    public Map<String, Object> updateRule(@PathVariable String ruleId, @RequestBody AlertRule rule) {
        AlertRule updated = alertService.updateRule(ruleId, rule);
        if (updated == null) {
            return Map.of("code", 404, "message", "规则不存在");
        }
        return Map.of("code", 0, "data", updated, "message", "规则更新成功");
    }

    /**
     * 删除告警规则
     */
    @DeleteMapping("/rules/{ruleId}")
    public Map<String, Object> deleteRule(@PathVariable String ruleId) {
        boolean deleted = alertService.deleteRule(ruleId);
        if (!deleted) {
            return Map.of("code", 404, "message", "规则不存在");
        }
        return Map.of("code", 0, "message", "规则删除成功");
    }

    /**
     * 启用/禁用告警规则
     */
    @PatchMapping("/rules/{ruleId}/toggle")
    public Map<String, Object> toggleRule(@PathVariable String ruleId, @RequestParam boolean enabled) {
        AlertRule rule = alertService.getRule(ruleId);
        if (rule == null) {
            return Map.of("code", 404, "message", "规则不存在");
        }
        rule.setEnabled(enabled);
        alertService.updateRule(ruleId, rule);
        return Map.of("code", 0, "message", enabled ? "规则已启用" : "规则已禁用");
    }
}
