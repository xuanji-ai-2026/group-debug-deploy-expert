package com.beijixing.storage.controller;

import com.beijixing.storage.dto.UploadCallbackRequest;
import com.beijixing.storage.dto.UploadRequest;
import com.beijixing.storage.service.StorageService;
import com.beijixing.storage.vo.ApiResponse;
import com.beijixing.storage.vo.FileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 存储服务控制器
 * 
 * <p>提供文件存储相关的RESTful API，包括：</p>
 * <ul>
 *   <li>文件上传（普通上传和分片上传）</li>
 *   <li>分片上传管理（初始化、进度查询、完成、取消）</li>
 *   <li>文件访问（获取URL、下载）</li>
 *   <li>文件管理（删除、查询）</li>
 * </ul>
 *
 * @author 叶宇（EMP-OPS-001）
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/storage")
@Tag(name = "存储服务", description = "文件存储相关接口")
public class StorageController {

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    // ==================== 文件上传接口 ====================

    /**
     * 普通文件上传
     * 
     * <p>用于小文件（小于5MB）的直接上传。</p>
     *
     * @param file 上传的文件
     * @param category 文件分类目录（可选）
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 文件信息，包含访问URL
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "普通文件上传接口，支持小文件（小于5MB）")
    public ResponseEntity<ApiResponse<FileVO>> uploadFile(
            @Parameter(description = "上传的文件", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "文件分类目录")
            @RequestParam(value = "category", required = false, defaultValue = "attachment") String category,
            @Parameter(description = "租户ID")
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @Parameter(description = "用户ID")
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        
        try {
            UploadRequest request = UploadRequest.builder()
                    .fileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .category(category)
                    .tenantId(tenantId)
                    .userId(userId)
                    .signedUrl(true)
                    .build();

            FileVO result = storageService.uploadFile(request, file.getInputStream());
            
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return ResponseEntity.status(500).body(ApiResponse.fail(500, "文件上传失败: " + e.getMessage()));
        }
    }

    /**
     * 批量上传文件
     *
     * @param files 上传的文件列表
     * @param category 文件分类目录
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 文件信息列表
     */
    @PostMapping("/upload/batch")
    @Operation(summary = "批量上传文件", description = "批量上传多个文件")
    public ResponseEntity<ApiResponse<List<FileVO>>> uploadFiles(
            @Parameter(description = "上传的文件列表")
            @RequestParam("files") MultipartFile[] files,
            @Parameter(description = "文件分类目录")
            @RequestParam(value = "category", required = false, defaultValue = "attachment") String category,
            @Parameter(description = "X-Tenant-Id", hidden = true) Long tenantId,
            @Parameter(description = "X-User-Id", hidden = true) Long userId) {
        
        try {
            List<UploadRequest> requests = new java.util.ArrayList<>();
            for (MultipartFile file : files) {
                UploadRequest request = UploadRequest.builder()
                        .fileName(file.getOriginalFilename())
                        .fileSize(file.getSize())
                        .category(category)
                        .tenantId(tenantId)
                        .userId(userId)
                        .build();
                requests.add(request);
            }
            
            List<FileVO> results = storageService.uploadFiles(requests);
            return ResponseEntity.ok(ApiResponse.success(results));
        } catch (Exception e) {
            log.error("批量上传失败", e);
            return ResponseEntity.status(500).body(ApiResponse.fail(500, "批量上传失败: " + e.getMessage()));
        }
    }

    // ==================== 分片上传接口 ====================

    /**
     * 初始化分片上传
     * 
     * <p>大文件上传前需要先调用此接口获取uploadId和fileKey。</p>
     *
     * @param fileName 文件名
     * @param fileSize 文件大小（字节）
     * @param fileMd5 文件MD5（可选）
     * @param totalParts 总分片数
     * @param category 文件分类目录
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 分片上传初始化信息
     */
    @PostMapping("/upload/init")
    @Operation(summary = "初始化分片上传", description = "大文件分片上传前必须先调用此接口")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initiateMultipartUpload(
            @Parameter(description = "文件名", required = true)
            @RequestParam("fileName") String fileName,
            @Parameter(description = "文件大小（字节）", required = true)
            @RequestParam("fileSize") Long fileSize,
            @Parameter(description = "文件MD5")
            @RequestParam(value = "fileMd5", required = false) String fileMd5,
            @Parameter(description = "总分片数", required = true)
            @RequestParam("totalParts") Integer totalParts,
            @Parameter(description = "文件分类目录")
            @RequestParam(value = "category", required = false, defaultValue = "attachment") String category,
            @Parameter(description = "X-Tenant-Id", hidden = true) Long tenantId,
            @Parameter(description = "X-User-Id", hidden = true) Long userId) {
        
        try {
            UploadRequest request = UploadRequest.builder()
                    .fileName(fileName)
                    .fileSize(fileSize)
                    .fileMd5(fileMd5)
                    .totalParts(totalParts)
                    .category(category)
                    .tenantId(tenantId)
                    .userId(userId)
                    .build();

            StorageService.InitMultipartUploadResult result = 
                    storageService.initiateMultipartUpload(request);

            Map<String, Object> response = new HashMap<>();
            response.put("fileKey", result.getFileKey());
            response.put("uploadId", result.getUploadId());
            response.put("cosPath", result.getCosPath());
            response.put("totalParts", result.getTotalParts());

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("初始化分片上传失败", e);
            return ResponseEntity.status(500).body(ApiResponse.fail(500, "初始化分片上传失败: " + e.getMessage()));
        }
    }

    /**
     * 上传分片
     *
     * @param fileKey 文件唯一标识
     * @param uploadId 上传ID
     * @param partNumber 分片序号（从1开始）
     * @param file 分片文件
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 分片上传结果
     */
    @PostMapping("/upload/part")
    @Operation(summary = "上传分片", description = "上传分片文件")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadPart(
            @Parameter(description = "文件唯一标识", required = true)
            @RequestParam("fileKey") String fileKey,
            @Parameter(description = "上传ID", required = true)
            @RequestParam("uploadId") String uploadId,
            @Parameter(description = "分片序号（从1开始）", required = true)
            @RequestParam("partNumber") Integer partNumber,
            @Parameter(description = "分片文件", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "X-Tenant-Id", hidden = true) Long tenantId,
            @Parameter(description = "X-User-Id", hidden = true) Long userId) {
        
        try {
            UploadRequest request = UploadRequest.builder()
                    .fileKey(fileKey)
                    .uploadId(uploadId)
                    .partNumber(partNumber)
                    .fileSize(file.getSize())
                    .tenantId(tenantId)
                    .userId(userId)
                    .build();

            StorageService.PartUploadResult result = 
                    storageService.uploadPart(request, file.getInputStream());

            Map<String, Object> response = new HashMap<>();
            response.put("partNumber", result.getPartNumber());
            response.put("eTag", result.geteTag());
            response.put("uploadedParts", result.getUploadedParts());
            response.put("totalParts", result.getTotalParts());

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("分片上传失败", e);
            return ResponseEntity.status(500).body(ApiResponse.fail(500, "分片上传失败: " + e.getMessage()));
        }
    }

    /**
     * 查询分片上传进度
     *
     * @param fileKey 文件唯一标识
     * @return 上传进度
     */
    @GetMapping("/upload/progress/{fileKey}")
    @Operation(summary = "查询上传进度", description = "查询分片上传进度")
    public ResponseEntity<ApiResponse<StorageService.UploadProgress>> queryUploadProgress(
            @Parameter(description = "文件唯一标识", required = true)
            @PathVariable String fileKey) {
        
        StorageService.UploadProgress progress = storageService.queryUploadProgress(fileKey);
        if (progress == null) {
            return ResponseEntity.status(404).body(ApiResponse.fail(404, "未找到上传记录"));
        }
        return ResponseEntity.ok(ApiResponse.success(progress));
    }

    /**
     * 完成分片上传
     *
     * @param request 回调请求
     * @return 文件信息
     */
    @PostMapping("/upload/complete")
    @Operation(summary = "完成分片上传", description = "所有分片上传完成后调用此接口")
    public ResponseEntity<ApiResponse<FileVO>> completeMultipartUpload(
            @RequestBody @Validated UploadCallbackRequest request) {
        
        try {
            FileVO result = storageService.completeMultipartUpload(request);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("完成分片上传失败", e);
            return ResponseEntity.status(500).body(ApiResponse.fail(500, "完成分片上传失败: " + e.getMessage()));
        }
    }

    /**
     * 取消分片上传
     *
     * @param fileKey 文件唯一标识
     * @return 操作结果
     */
    @DeleteMapping("/upload/{fileKey}")
    @Operation(summary = "取消分片上传", description = "取消正在进行的上传")
    public ResponseEntity<ApiResponse<Void>> abortMultipartUpload(
            @Parameter(description = "文件唯一标识", required = true)
            @PathVariable String fileKey) {
        
        storageService.abortMultipartUpload(fileKey);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ==================== 文件访问接口 ====================

    /**
     * 获取文件访问URL
     *
     * @param fileKeyOrPath 文件Key或COS路径
     * @param signed 是否签名（默认true）
     * @return 文件访问URL
     */
    @GetMapping("/url")
    @Operation(summary = "获取文件访问URL", description = "获取文件的访问链接")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAccessUrl(
            @Parameter(description = "文件Key或COS路径", required = true)
            @RequestParam("fileKey") String fileKeyOrPath,
            @Parameter(description = "是否生成签名URL")
            @RequestParam(value = "signed", defaultValue = "true") Boolean signed) {
        
        String url = storageService.getAccessUrl(fileKeyOrPath, signed);
        
        Map<String, String> response = new HashMap<>();
        response.put("url", url);
        response.put("type", signed ? "signed" : "public");
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取文件信息
     *
     * @param fileKeyOrPath 文件Key或COS路径
     * @return 文件信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取文件信息", description = "获取文件的基本信息")
    public ResponseEntity<ApiResponse<FileVO>> getFileInfo(
            @Parameter(description = "文件Key或COS路径", required = true)
            @RequestParam("fileKey") String fileKeyOrPath) {
        
        FileVO fileInfo = storageService.getFileInfo(fileKeyOrPath);
        return ResponseEntity.ok(ApiResponse.success(fileInfo));
    }

    /**
     * 下载文件
     *
     * @param fileKeyOrPath 文件Key或COS路径
     * @param localPath 本地保存路径
     * @return 下载结果
     */
    @GetMapping("/download")
    @Operation(summary = "下载文件", description = "下载文件到指定路径")
    public ResponseEntity<ApiResponse<Map<String, String>>> downloadFile(
            @Parameter(description = "文件Key或COS路径", required = true)
            @RequestParam("fileKey") String fileKeyOrPath,
            @Parameter(description = "本地保存路径", required = true)
            @RequestParam("localPath") String localPath) {
        
        String result = storageService.downloadFile(fileKeyOrPath, localPath);
        
        Map<String, String> response = new HashMap<>();
        response.put("localPath", result);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== 文件管理接口 ====================

    /**
     * 删除文件
     *
     * @param fileKeyOrPath 文件Key或COS路径
     * @return 操作结果
     */
    @DeleteMapping
    @Operation(summary = "删除文件", description = "删除指定的文件")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @Parameter(description = "文件Key或COS路径", required = true)
            @RequestParam("fileKey") String fileKeyOrPath) {
        
        storageService.deleteFile(fileKeyOrPath);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 批量删除文件
     *
     * @param fileKeys 文件Key或COS路径列表
     * @return 操作结果
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除文件", description = "批量删除多个文件")
    public ResponseEntity<ApiResponse<Void>> deleteFiles(
            @Parameter(description = "文件Key或COS路径列表", required = true)
            @RequestParam("fileKeys") List<String> fileKeys) {
        
        storageService.deleteFiles(fileKeys);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 检查文件是否存在
     *
     * @param fileKeyOrPath 文件Key或COS路径
     * @return 是否存在
     */
    @GetMapping("/exists")
    @Operation(summary = "检查文件是否存在", description = "检查指定文件是否存在")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> fileExists(
            @Parameter(description = "文件Key或COS路径", required = true)
            @RequestParam("fileKey") String fileKeyOrPath) {
        
        boolean exists = storageService.fileExists(fileKeyOrPath);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
