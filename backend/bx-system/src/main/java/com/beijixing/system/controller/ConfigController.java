package com.beijixing.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beijixing.system.entity.SysConfig;
import com.beijixing.system.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统参数配置控制器
 *
 * 功能：SM-001 参数配置（系统参数、动态配置）
 *
 * @author bx-system
 */
@Slf4j
@RestController
@RequestMapping("/admin/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    /**
     * SM-001-01: 分页查询配置列表
     * GET /api/v1/admin/configs
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> pageConfigs(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) String groupCode,
            @RequestParam(required = false) String configKey,
            @RequestParam(required = false) String configName,
            @RequestParam(required = false) Integer status) {
        log.info("分页查询配置列表，page={}, size={}, groupCode={}", page, size, groupCode);
        Page<SysConfig> result = configService.pageConfigs(page, size, groupCode, configKey, configName, status);
        return successData(result);
    }

    /**
     * SM-001-02: 获取配置详情
     * GET /api/v1/admin/configs/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getConfig(@PathVariable Long id) {
        SysConfig config = configService.getById(id);
        if (config == null) {
            return fail("配置不存在", 40401);
        }
        return successData(config);
    }

    /**
     * SM-001-03: 根据Key获取配置值（公开接口）
     * GET /api/v1/configs/key/{configKey}
     */
    @GetMapping("/key/{configKey}")
    public ResponseEntity<Map<String, Object>> getConfigByKey(@PathVariable String configKey) {
        String value = configService.getValueByKey(configKey);
        Map<String, String> data = new HashMap<>();
        data.put("configKey", configKey);
        data.put("configValue", value);
        return successData(data);
    }

    /**
     * 根据Key更新配置值
     * PUT /api/v1/admin/configs/key
     */
    @PutMapping("/key")
    public ResponseEntity<Map<String, Object>> updateConfigByKey(@RequestBody Map<String, String> body) {
        String key = body.get("key");
        String value = body.get("value");
        if (key == null || key.isEmpty()) {
            return fail("配置key不能为空", 40001);
        }
        log.info("更新配置：key={}, value={}", key, value);
        configService.updateByKey(key, value);
        return success("配置更新成功");
    }

    /**
     * SM-001-04: 根据分组获取所有配置（公开接口）
     * GET /api/v1/configs/group/{groupCode}
     */
    @GetMapping("/group/{groupCode}")
    public ResponseEntity<Map<String, Object>> getConfigsByGroup(@PathVariable String groupCode) {
        Map<String, String> configMap = configService.getConfigMap(groupCode);
        return successData(configMap);
    }

    /**
     * SM-001-05: 创建配置
     * POST /api/v1/admin/configs
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createConfig(@RequestBody SysConfig config) {
        log.info("创建配置：{}", config.getConfigKey());
        Long id = configService.create(config);
        return success("配置创建成功", Map.of("id", id));
    }

    /**
     * SM-001-06: 更新配置
     * PUT /api/v1/admin/configs/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateConfig(@PathVariable Long id, @RequestBody SysConfig config) {
        log.info("更新配置：id={}", id);
        configService.update(id, config);
        return success("配置更新成功");
    }

    /**
     * SM-001-07: 删除配置
     * DELETE /api/v1/admin/configs/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteConfig(@PathVariable Long id) {
        log.info("删除配置：id={}", id);
        configService.delete(id);
        return success("配置删除成功");
    }

    /**
     * SM-001-08: 刷新配置缓存
     * POST /api/v1/admin/configs/refresh-cache
     */
    @PostMapping("/refresh-cache")
    public ResponseEntity<Map<String, Object>> refreshCache(@RequestParam(required = false) String groupCode) {
        log.info("刷新配置缓存，groupCode={}", groupCode);
        configService.refreshCache(groupCode);
        return success("缓存刷新成功");
    }

    // ==================== 统一响应封装 ====================

    private ResponseEntity<Map<String, Object>> success(String message, Object data) {
        return successData(message, data);
    }

    private ResponseEntity<Map<String, Object>> success(String message) {
        return successData(message, null);
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
