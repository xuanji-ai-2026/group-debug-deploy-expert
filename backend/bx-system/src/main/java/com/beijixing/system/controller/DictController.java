package com.beijixing.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beijixing.system.entity.SysDict;
import com.beijixing.system.entity.SysDictItem;
import com.beijixing.system.service.DictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据字典管理控制器
 *
 * 功能：SM-002 字典管理（数据字典、字典项）
 *
 * @author bx-system
 */
@Slf4j
@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    // ==================== 字典管理 ====================

    /**
     * SM-002-01: 分页查询字典列表
     * GET /api/v1/admin/dicts
     */
    @GetMapping("/admin/dicts")
    public ResponseEntity<Map<String, Object>> pageDicts(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) String dictCode,
            @RequestParam(required = false) String dictName,
            @RequestParam(required = false) Integer status) {
        log.info("分页查询字典列表，page={}, size={}", page, size);
        Page<SysDict> result = dictService.pageDicts(page, size, dictCode, dictName, status);
        return successData(result);
    }

    /**
     * SM-002-02: 获取字典详情
     * GET /api/v1/admin/dicts/{id}
     */
    @GetMapping("/admin/dicts/{id}")
    public ResponseEntity<Map<String, Object>> getDict(@PathVariable Long id) {
        SysDict dict = dictService.getDictById(id);
        if (dict == null) {
            return fail("字典不存在", 40402);
        }
        return successData(dict);
    }

    /**
     * SM-002-03: 创建字典
     * POST /api/v1/admin/dicts
     */
    @PostMapping("/admin/dicts")
    public ResponseEntity<Map<String, Object>> createDict(@RequestBody SysDict dict) {
        log.info("创建字典：{}", dict.getDictCode());
        Long id = dictService.createDict(dict);
        return success("字典创建成功", Map.of("id", id));
    }

    /**
     * SM-002-04: 更新字典
     * PUT /api/v1/admin/dicts/{id}
     */
    @PutMapping("/admin/dicts/{id}")
    public ResponseEntity<Map<String, Object>> updateDict(@PathVariable Long id, @RequestBody SysDict dict) {
        log.info("更新字典：id={}", id);
        dictService.updateDict(id, dict);
        return success("字典更新成功");
    }

    /**
     * SM-002-05: 删除字典
     * DELETE /api/v1/admin/dicts/{id}
     */
    @DeleteMapping("/admin/dicts/{id}")
    public ResponseEntity<Map<String, Object>> deleteDict(@PathVariable Long id) {
        log.info("删除字典：id={}", id);
        dictService.deleteDict(id);
        return success("字典删除成功");
    }

    /**
     * 更新字典状态
     * PUT /api/v1/admin/dicts/{id}/status
     */
    @PutMapping("/admin/dicts/{id}/status")
    public ResponseEntity<Map<String, Object>> updateDictStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        log.info("更新字典状态：id={}, status={}", id, body.get("status"));
        Integer status = body.get("status") != null ? ((Number) body.get("status")).intValue() : null;
        dictService.updateDictStatus(id, status);
        return success("字典状态更新成功");
    }

    // ==================== 字典项管理 ====================

    /**
     * SM-002-06: 获取字典的所有字典项
     * GET /api/v1/admin/dicts/{dictId}/items
     */
    @GetMapping("/admin/dicts/{dictId}/items")
    public ResponseEntity<Map<String, Object>> listItems(@PathVariable Long dictId) {
        List<SysDictItem> items = dictService.listItemsByDictId(dictId);
        return successData(items);
    }

    /**
     * SM-002-07: 根据字典编码获取启用字典项（公开接口）
     * GET /api/v1/dicts/{dictCode}/items
     */
    @GetMapping("/dicts/{dictCode}/items")
    public ResponseEntity<Map<String, Object>> listItemsByCode(@PathVariable String dictCode) {
        List<SysDictItem> items = dictService.listItemsByDictCode(dictCode);
        return successData(items);
    }

    /**
     * SM-002-08: 创建字典项
     * POST /api/v1/admin/dict-items
     */
    @PostMapping("/admin/dict-items")
    public ResponseEntity<Map<String, Object>> createItem(@RequestBody SysDictItem item) {
        log.info("创建字典项：dictId={}, value={}", item.getDictId(), item.getItemValue());
        Long id = dictService.createItem(item);
        return success("字典项创建成功", Map.of("id", id));
    }

    /**
     * SM-002-09: 批量创建字典项
     * POST /api/v1/admin/dict-items/batch
     */
    @PostMapping("/admin/dict-items/batch")
    public ResponseEntity<Map<String, Object>> batchCreateItems(@RequestBody List<SysDictItem> items) {
        log.info("批量创建字典项，数量：{}", items.size());
        dictService.batchCreateItems(items);
        return success("字典项批量创建成功", Map.of("count", items.size()));
    }

    /**
     * SM-002-10: 更新字典项
     * PUT /api/v1/admin/dict-items/{id}
     */
    @PutMapping("/admin/dict-items/{id}")
    public ResponseEntity<Map<String, Object>> updateItem(@PathVariable Long id, @RequestBody SysDictItem item) {
        log.info("更新字典项：id={}", id);
        dictService.updateItem(id, item);
        return success("字典项更新成功");
    }

    /**
     * SM-002-11: 删除字典项
     * DELETE /api/v1/admin/dict-items/{id}
     */
    @DeleteMapping("/admin/dict-items/{id}")
    public ResponseEntity<Map<String, Object>> deleteItem(@PathVariable Long id) {
        log.info("删除字典项：id={}", id);
        dictService.deleteItem(id);
        return success("字典项删除成功");
    }

    /**
     * 更新字典项状态
     * PUT /api/v1/admin/dict-items/{id}/status
     */
    @PutMapping("/admin/dict-items/{id}/status")
    public ResponseEntity<Map<String, Object>> updateItemStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        log.info("更新字典项状态：id={}, status={}", id, body.get("status"));
        Integer status = body.get("status") != null ? ((Number) body.get("status")).intValue() : null;
        dictService.updateItemStatus(id, status);
        return success("字典项状态更新成功");
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
