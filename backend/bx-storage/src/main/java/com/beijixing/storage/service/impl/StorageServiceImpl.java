package com.beijixing.storage.service.impl;

import com.beijixing.storage.config.CosConfig;
import com.beijixing.storage.cos.CosUploadCallback;
import com.qcloud.cos.model.COSObject;
import com.beijixing.storage.dto.UploadCallbackRequest;
import com.beijixing.storage.dto.UploadRequest;
import com.beijixing.storage.service.CosService;
import com.beijixing.storage.service.StorageService;
import com.beijixing.storage.util.FileUtil;
import com.beijixing.storage.vo.FileVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 存储服务实现类
 * 
 * <p>实现文件存储的核心业务逻辑，包括：</p>
 * <ul>
 *   <li>ST-001: 腾讯云COS集成 - 文件上传到COS</li>
 *   <li>ST-002: 分片上传 - 大文件分片断点续传</li>
 *   <li>ST-003: CDN加速 - CDN域名访问</li>
 *   <li>ST-004: 文件访问权限控制 - 签名URL访问</li>
 * </ul>
 *
 * @author 叶宇（EMP-OPS-001）
 * @version 1.0.0
 */
@Slf4j
@Service
public class StorageServiceImpl implements StorageService {

    private final CosService cosService;
    private final CosConfig cosConfig;
    private final CosUploadCallback uploadCallback;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 分片上传上下文缓存Key前缀
     */
    private static final String UPLOAD_CONTEXT_KEY = "storage:upload:context:";

    /**
     * 分片上传有效期（7天）
     */
    private static final long UPLOAD_EXPIRE_DAYS = 7;

    public StorageServiceImpl(CosService cosService, CosConfig cosConfig,
                              CosUploadCallback uploadCallback,
                              RedisTemplate<String, Object> redisTemplate) {
        this.cosService = cosService;
        this.cosConfig = cosConfig;
        this.uploadCallback = uploadCallback;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public FileVO uploadFile(UploadRequest request, InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("上传文件输入流不能为空");
        }
        if (request == null) {
            throw new IllegalArgumentException("上传请求参数不能为空");
        }

        // 生成COS存储路径
        String cosPath = cosService.buildCosPath(
                FileUtil.generateStoragePath(request.getCategory(), request.getFileName())
        );

        // 获取文件大小
        long fileSize = request.getFileSize() > 0 ? request.getFileSize() : 0;

        // 上传到COS
        cosService.upload(cosPath, inputStream, request.getFileName(), fileSize);

        // 构建文件信息
        return buildFileVO(cosPath, request.getFileName(), fileSize, request.getSignedUrl());
    }

    @Override
    public List<FileVO> uploadFiles(List<UploadRequest> requests) {
        List<FileVO> results = new ArrayList<>();
        for (UploadRequest request : requests) {
            try {
                // 这里简化处理，实际应该获取每个文件的输入流
                FileVO fileVO = FileVO.builder()
                        .fileName(request.getFileName())
                        .fileSize(request.getFileSize())
                        .fileType(FileUtil.getFileType(request.getFileName()))
                        .build();
                results.add(fileVO);
            } catch (Exception e) {
                log.error("批量上传失败: {}", request.getFileName(), e);
            }
        }
        return results;
    }

    @Override
    public InitMultipartUploadResult initiateMultipartUpload(UploadRequest request) {
        // 生成文件唯一标识
        String fileKey = request.getFileKey();
        if (fileKey == null || fileKey.isEmpty()) {
            fileKey = UUID.randomUUID().toString().replace("-", "");
        }

        // 计算总分片数
        int totalParts = calculateTotalParts(request.getFileSize());

        // 生成COS存储路径
        String cosPath = cosService.buildCosPath(
                FileUtil.generateStoragePath(request.getCategory(), request.getFileName())
        );

        // 初始化分片上传
        String uploadId = cosService.initiateMultipartUpload(cosPath).getUploadId();

        // 创建上传上下文
        uploadCallback.createContext(
                fileKey, cosPath, uploadId, totalParts,
                request.getFileName(), request.getFileSize(),
                request.getFileMd5(), request.getTenantId(), request.getUserId()
        );

        // 缓存到Redis（用于分布式场景）
        cacheUploadContext(fileKey, cosPath, uploadId, totalParts);

        log.info("分片上传初始化成功: fileKey={}, uploadId={}, totalParts={}", 
                fileKey, uploadId, totalParts);

        return new InitMultipartUploadResult(fileKey, uploadId, cosPath, totalParts);
    }

    @Override
    public PartUploadResult uploadPart(UploadRequest request, InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("分片上传输入流不能为空");
        }
        if (request == null) {
            throw new IllegalArgumentException("分片上传请求参数不能为空");
        }

        String fileKey = request.getFileKey();
        String uploadId = request.getUploadId();
        int partNumber = request.getPartNumber();

        // 获取上传上下文
        CosUploadCallback.UploadContext context = uploadCallback.getContext(fileKey);
        if (context == null) {
            context = getContextFromCache(fileKey);
        }

        String cosPath;
        if (context != null) {
            cosPath = context.getCosPath();
        } else {
            cosPath = request.getCategory() != null 
                    ? cosService.buildCosPath(request.getCategory())
                    : "";
        }

        // 计算分片大小
        long partSize = cosConfig.getPartSize();
        if (request.getFileSize() > 0 && request.getTotalParts() != null) {
            if (request.getPartNumber() == request.getTotalParts()) {
                partSize = request.getFileSize() - (long) (request.getTotalParts() - 1) * cosConfig.getPartSize();
            }
        }

        // 上传分片（使用try-with-resources确保InputStream正确关闭）
        String eTag;
        try (InputStream is = inputStream) {
            eTag = cosService.uploadPart(cosPath, uploadId, partNumber, is, partSize)
                    .getETag();
        } catch (Exception e) {
            log.error("分片上传失败: fileKey={}, partNumber={}", fileKey, partNumber, e);
            throw new RuntimeException("分片上传失败: " + e.getMessage(), e);
        }

        // 更新进度
        uploadCallback.updatePartProgress(fileKey, partNumber, eTag);

        CosUploadCallback.UploadProgress progress = uploadCallback.getProgress(fileKey);
        int uploadedParts = progress != null ? progress.getUploadedParts() : 1;
        int totalParts = progress != null ? progress.getTotalParts() : request.getTotalParts();

        log.debug("分片上传成功: fileKey={}, partNumber={}, eTag={}", fileKey, partNumber, eTag);

        return new PartUploadResult(partNumber, eTag, uploadedParts, totalParts);
    }

    @Override
    public UploadProgress queryUploadProgress(String fileKey) {
        CosUploadCallback.UploadProgress callbackProgress = uploadCallback.getProgress(fileKey);
        
        if (callbackProgress == null) {
            // 尝试从Redis获取
            callbackProgress = getProgressFromCache(fileKey);
        }
        
        if (callbackProgress == null) {
            return null;
        }

        String statusStr;
        switch (callbackProgress.getStatus()) {
            case 0: statusStr = "进行中"; break;
            case 1: statusStr = "已完成"; break;
            case 2: statusStr = "已取消"; break;
            default: statusStr = "未知"; break;
        }

        return new UploadProgress(
                callbackProgress.getFileKey(),
                null, // uploadId从缓存获取
                callbackProgress.getTotalParts(),
                callbackProgress.getUploadedParts(),
                callbackProgress.getProgress(),
                statusStr
        );
    }

    @Override
    public FileVO completeMultipartUpload(UploadCallbackRequest request) {
        String fileKey = request.getFileKey();
        String uploadId = request.getUploadId();
        String cosPath = request.getCosPath();

        // 如果cosPath为空，从上下文获取
        if (cosPath == null || cosPath.isEmpty()) {
            CosUploadCallback.UploadContext context = uploadCallback.getContext(fileKey);
            if (context != null) {
                cosPath = context.getCosPath();
            } else {
                context = getContextFromCache(fileKey);
                if (context != null) {
                    cosPath = context.getCosPath();
                }
            }
        }

        if (cosPath == null) {
            throw new RuntimeException("未找到上传上下文: " + fileKey);
        }

        // 列出所有已上传的分片
        List<com.qcloud.cos.model.PartETag> parts = cosService.listParts(cosPath, uploadId);

        if (parts.isEmpty()) {
            throw new RuntimeException("未找到已上传的分片: " + fileKey);
        }

        // 完成分片上传
        cosService.completeMultipartUpload(cosPath, uploadId, parts);

        // 标记完成
        uploadCallback.markCompleted(fileKey);

        // 清理缓存
        removeContextFromCache(fileKey);

        log.info("分片上传完成: fileKey={}, cosPath={}, parts={}", fileKey, cosPath, parts.size());

        // 构建文件信息
        return buildFileVO(cosPath, request.getFileName(), request.getFileSize(), true);
    }

    @Override
    public void abortMultipartUpload(String fileKey) {
        CosUploadCallback.UploadContext context = uploadCallback.getContext(fileKey);
        
        if (context == null) {
            context = getContextFromCache(fileKey);
        }

        if (context != null) {
            cosService.abortMultipartUpload(context.getCosPath(), context.getUploadId());
            uploadCallback.markCancelled(fileKey);
            removeContextFromCache(fileKey);
            log.info("分片上传已取消: fileKey={}", fileKey);
        }
    }

    @Override
    public String getAccessUrl(String fileKeyOrPath, boolean signed) {
        String cosPath = resolveCosPath(fileKeyOrPath);
        
        if (signed) {
            return cosService.generateSignedUrl(cosPath);
        } else {
            return buildDirectUrl(cosPath);
        }
    }

    @Override
    public FileVO getFileInfo(String fileKeyOrPath) {
        String cosPath = resolveCosPath(fileKeyOrPath);
        return buildFileVO(cosPath, FileUtil.getNameWithoutExtension(cosPath), 0, true);
    }

    @Override
    public String downloadFile(String fileKeyOrPath, String localPath) {
        String cosPath = resolveCosPath(fileKeyOrPath);
        
        try {
            COSObject cosObject = cosService.getObject(cosPath);
            if (cosObject == null) {
                throw new RuntimeException("文件不存在: " + cosPath);
            }
            
            java.io.File localFile = new java.io.File(localPath);
            java.io.File parentDir = localFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            try (InputStream is = cosObject.getObjectContent()) {
                if (is == null) {
                    throw new RuntimeException("无法获取文件内容流: " + cosPath);
                }
                org.apache.commons.io.IOUtils.copy(is, new java.io.FileOutputStream(localFile));
            }
            
            return localPath;
        } catch (Exception e) {
            log.error("下载文件失败: cosPath={}, localPath={}", cosPath, localPath, e);
            throw new RuntimeException("下载文件失败", e);
        }
    }

    @Override
    public void deleteFile(String fileKeyOrPath) {
        String cosPath = resolveCosPath(fileKeyOrPath);
        cosService.delete(cosPath);
        log.info("文件已删除: {}", cosPath);
    }

    @Override
    public void deleteFiles(List<String> fileKeyOrPaths) {
        for (String path : fileKeyOrPaths) {
            deleteFile(path);
        }
    }

    @Override
    public boolean fileExists(String fileKeyOrPath) {
        String cosPath = resolveCosPath(fileKeyOrPath);
        return cosService.exists(cosPath);
    }

    // ==================== 私有方法 ====================

    /**
     * 构建文件VO
     */
    private FileVO buildFileVO(String cosPath, String fileName, long fileSize, boolean signed) {
        String accessUrl;
        if (signed) {
            accessUrl = cosService.generateSignedUrl(cosPath);
        } else {
            accessUrl = buildDirectUrl(cosPath);
        }

        String fileSuffix = FileUtil.getExtension(fileName);
        String contentType = getContentType(fileSuffix);
        String fileType = FileUtil.getFileType(contentType);

        return FileVO.builder()
                .fileName(fileName)
                .cosPath(cosPath)
                .accessUrl(accessUrl)
                .fileSize(fileSize)
                .fileSuffix(fileSuffix)
                .contentType(contentType)
                .fileType(fileType)
                .uploadTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .previewable(FileUtil.isPreviewable(fileSuffix))
                .build();
    }

    /**
     * 构建直链URL（不带签名）
     */
    private String buildDirectUrl(String cosPath) {
        String domain = cosConfig.getAccessDomain();
        return "https://" + domain + "/" + cosPath;
    }

    /**
     * 计算总分片数
     */
    private int calculateTotalParts(long fileSize) {
        if (fileSize <= 0) {
            return 1;
        }
        long partSize = cosConfig.getPartSize();
        int totalParts = (int) ((fileSize + partSize - 1) / partSize);
        // 限制最大分片数
        return Math.min(totalParts, cosConfig.getMaxParts());
    }

    /**
     * 解析COS路径
     */
    private String resolveCosPath(String fileKeyOrPath) {
        if (fileKeyOrPath == null || fileKeyOrPath.isEmpty()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        
        // 如果是完整的COS路径（包含/），直接返回
        if (fileKeyOrPath.contains("/")) {
            return fileKeyOrPath.startsWith("/") 
                    ? fileKeyOrPath.substring(1) 
                    : fileKeyOrPath;
        }
        
        // 否则作为fileKey，从缓存中获取cosPath
        CosUploadCallback.UploadContext context = uploadCallback.getContext(fileKeyOrPath);
        if (context != null) {
            return context.getCosPath();
        }
        
        context = getContextFromCache(fileKeyOrPath);
        if (context != null) {
            return context.getCosPath();
        }
        
        return fileKeyOrPath;
    }

    /**
     * 获取Content-Type
     */
    private String getContentType(String extension) {
        if (extension == null || extension.isEmpty()) {
            return "application/octet-stream";
        }
        
        extension = extension.toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            case "pdf":
                return "application/pdf";
            case "mp4":
                return "video/mp4";
            case "mp3":
                return "audio/mpeg";
            case "txt":
                return "text/plain";
            case "html":
                return "text/html";
            case "json":
                return "application/json";
            case "xml":
                return "application/xml";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * 缓存上传上下文到Redis
     */
    private void cacheUploadContext(String fileKey, String cosPath, String uploadId, int totalParts) {
        Map<String, Object> context = new HashMap<>();
        context.put("fileKey", fileKey);
        context.put("cosPath", cosPath);
        context.put("uploadId", uploadId);
        context.put("totalParts", totalParts);
        context.put("uploadedParts", 0);
        
        String key = UPLOAD_CONTEXT_KEY + fileKey;
        redisTemplate.opsForHash().putAll(key, context);
        redisTemplate.expire(key, UPLOAD_EXPIRE_DAYS, TimeUnit.DAYS);
    }

    /**
     * 从Redis获取上传上下文
     */
    private CosUploadCallback.UploadContext getContextFromCache(String fileKey) {
        String key = UPLOAD_CONTEXT_KEY + fileKey;
        Map<Object, Object> map = redisTemplate.opsForHash().entries(key);
        
        if (map.isEmpty()) {
            return null;
        }
        
        return CosUploadCallback.UploadContext.builder()
                .fileKey((String) map.get("fileKey"))
                .cosPath((String) map.get("cosPath"))
                .uploadId((String) map.get("uploadId"))
                .totalParts((Integer) map.get("totalParts"))
                .uploadedParts(new java.util.concurrent.atomic.AtomicInteger(
                        ((Number) map.getOrDefault("uploadedParts", 0)).intValue()))
                .build();
    }

    /**
     * 从Redis获取进度
     */
    private CosUploadCallback.UploadProgress getProgressFromCache(String fileKey) {
        String key = UPLOAD_CONTEXT_KEY + fileKey;
        Map<Object, Object> map = redisTemplate.opsForHash().entries(key);
        
        if (map.isEmpty()) {
            return null;
        }
        
        return CosUploadCallback.UploadProgress.builder()
                .fileKey(fileKey)
                .totalParts((Integer) map.get("totalParts"))
                .uploadedParts(((Number) map.getOrDefault("uploadedParts", 0)).intValue())
                .progress(calculateProgress(
                        ((Number) map.getOrDefault("uploadedParts", 0)).intValue(),
                        ((Number) map.getOrDefault("totalParts", 1)).intValue()))
                .status(0)
                .build();
    }

    /**
     * 清理Redis中的上下文
     */
    private void removeContextFromCache(String fileKey) {
        String key = UPLOAD_CONTEXT_KEY + fileKey;
        redisTemplate.delete(key);
    }

    /**
     * 计算进度百分比
     */
    private int calculateProgress(int uploaded, int total) {
        if (total == 0) return 0;
        return (int) (uploaded * 100.0 / total);
    }
}
