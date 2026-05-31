package com.beijixing.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beijixing.system.entity.SysFile;
import com.beijixing.system.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件管理控制器
 *
 * 功能：SM-004 文件管理（文件上传、预览、删除）
 *
 * @author bx-system
 */
@Slf4j
@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * SM-004-01: 上传文件
     * POST /api/v1/admin/files
     */
    @PostMapping("/admin/files")
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String tag,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        log.info("上传文件：originalName={}, size={}, tag={}", file.getOriginalFilename(), file.getSize(), tag);
        SysFile sysFile = fileService.upload(file, tag, userId, tenantId);
        return success("文件上传成功", sysFile);
    }

    /**
     * SM-004-02: 上传文件（带自定义文件名）
     * POST /api/v1/admin/files/custom-name
     */
    @PostMapping("/admin/files/custom-name")
    public ResponseEntity<Map<String, Object>> uploadWithCustomName(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String customName,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        log.info("上传文件（自定义名称）：{}", customName);
        SysFile sysFile = fileService.upload(file, tag, userId, tenantId, customName);
        return success("文件上传成功", sysFile);
    }

    /**
     * SM-004-03: 获取文件信息
     * GET /api/v1/admin/files/{id}
     */
    @GetMapping("/admin/files/{id}")
    public ResponseEntity<Map<String, Object>> getFile(@PathVariable Long id) {
        SysFile file = fileService.getById(id);
        if (file == null) {
            return fail("文件不存在", 40404);
        }
        return successData(file);
    }

    /**
     * SM-004-04: 分页查询文件列表
     * GET /api/v1/admin/files
     */
    @GetMapping("/admin/files")
    public ResponseEntity<Map<String, Object>> pageFiles(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) String tag,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        log.info("分页查询文件列表，page={}, tag={}", page, tag);
        Page<SysFile> result = fileService.pageFiles(page, size, tag, userId, tenantId);
        return successData(result);
    }

    /**
     * SM-004-05: 获取用户文件列表
     * GET /api/v1/files/me
     */
    @GetMapping("/files/me")
    public ResponseEntity<Map<String, Object>> listMyFiles(
            @RequestHeader(value = "X-User-Id") Long userId) {
        List<SysFile> files = fileService.listByUser(userId);
        return successData(files);
    }

    /**
     * SM-004-06: 获取用户指定标签文件
     * GET /api/v1/files/me/tag/{tag}
     */
    @GetMapping("/files/me/tag/{tag}")
    public ResponseEntity<Map<String, Object>> listMyFilesByTag(
            @PathVariable String tag,
            @RequestHeader(value = "X-User-Id") Long userId) {
        List<SysFile> files = fileService.listByTag(tag, userId);
        return successData(files);
    }

    /**
     * SM-004-07: 删除文件
     * DELETE /api/v1/admin/files/{id}
     */
    @DeleteMapping("/admin/files/{id}")
    public ResponseEntity<Map<String, Object>> deleteFile(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        log.info("删除文件：id={}", id);
        fileService.delete(id, userId);
        return success("文件删除成功");
    }

    /**
     * SM-004-08: 批量删除文件
     * DELETE /api/v1/admin/files/batch
     */
    @DeleteMapping("/admin/files/batch")
    public ResponseEntity<Map<String, Object>> batchDeleteFiles(
            @RequestBody List<Long> ids,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        log.info("批量删除文件，数量：{}", ids.size());
        fileService.batchDelete(ids, userId);
        return success("文件批量删除成功", Map.of("count", ids.size()));
    }

    /**
     * SM-004-09: 下载文件（内部接口）
     * GET /api/v1/files/download/{fileKey}
     */
    @GetMapping("/files/download/{fileKey}")
    public ResponseEntity<byte[]> download(@PathVariable String fileKey) {
        log.info("下载文件：fileKey={}", fileKey);
        byte[] data = fileService.download(fileKey);

        SysFile file = fileService.getByFileKey(fileKey);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        if (file != null && file.getOriginalName() != null) {
            headers.setContentDispositionFormData("attachment", file.getOriginalName());
        }

        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    /**
     * SM-004-10: 预览文件（获取访问URL）
     * GET /api/v1/files/preview/{fileKey}
     */
    @GetMapping("/files/preview/{fileKey}")
    public ResponseEntity<Map<String, Object>> getPreviewUrl(@PathVariable String fileKey) {
        String accessUrl = fileService.getAccessUrl(fileKey);
        Map<String, String> result = new HashMap<>();
        result.put("fileKey", fileKey);
        result.put("accessUrl", accessUrl);
        return successData(result);
    }

    /**
     * 前端统一文件上传入口
     * POST /system/file/upload
     */
    @PostMapping("/system/file/upload")
    public ResponseEntity<Map<String, Object>> uploadForFrontend(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String tag,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        log.info("前端文件上传：originalName={}, size={}", file.getOriginalFilename(), file.getSize());
        SysFile sysFile = fileService.upload(file, tag, userId, tenantId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", Map.of("url", sysFile.getAccessUrl(), "fileKey", sysFile.getFileKey()));
        return ResponseEntity.ok(result);
    }

    // ==================== 统一响应封装 ====================

    private ResponseEntity<Map<String, Object>> success(String message) {
        return successData(message, null);
    }

    private ResponseEntity<Map<String, Object>> success(Object message, Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", message);
        result.put("data", data);
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<Map<String, Object>> successData(String message, Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", message);
        result.put("data", data);
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<Map<String, Object>> successData(Object data) {
        return success("success", data);
    }

    private ResponseEntity<Map<String, Object>> fail(String message, Integer code) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code != null ? code : 50000);
        result.put("message", message);
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }
}
