package com.beijixing.storage.cos;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分片上传回调处理类
 * 
 * <p>管理分片上传的回调逻辑和进度跟踪，支持断点续传。</p>
 * <p>主要功能：</p>
 * <ul>
 *   <li>记录每个分片的上传状态</li>
 *   <li>在所有分片上传完成后触发合并操作</li>
 *   <li>支持上传进度查询</li>
 * </ul>
 *
 * @author 叶宇（EMP-OPS-001）
 * @version 1.0.0
 */
@Slf4j
@Component
public class CosUploadCallback {

    /**
     * 分片上传上下文缓存
     * key: fileKey（文件唯一标识）
     */
    private final ConcurrentHashMap<String, UploadContext> contextCache = new ConcurrentHashMap<>();

    /**
     * 分片上传上下文
     */
    @Data
    @Builder
    public static class UploadContext {
        /**
         * 文件唯一标识
         */
        private String fileKey;
        
        /**
         * COS存储路径
         */
        private String cosPath;
        
        /**
         * 上传ID（腾讯云COS）
         */
        private String uploadId;
        
        /**
         * 总分片数
         */
        private int totalParts;
        
        /**
         * 已上传分片数
         */
        private AtomicInteger uploadedParts;
        
        /**
         * 上传完成的分片序号列表
         */
        private ConcurrentHashMap<Integer, String> partETags;
        
        /**
         * 文件名
         */
        private String fileName;
        
        /**
         * 文件大小
         */
        private long fileSize;
        
        /**
         * 文件MD5
         */
        private String fileMd5;
        
        /**
         * 租户ID
         */
        private Long tenantId;
        
        /**
         * 用户ID
         */
        private Long userId;
        
        /**
         * 创建时间（毫秒）
         */
        private long createTime;
        
        /**
         * 上传状态：0-进行中，1-完成，2-取消
         */
        private volatile int status;

        /**
         * 添加已完成的分片
         */
        public synchronized void addPart(int partNumber, String eTag) {
            if (partETags.putIfAbsent(partNumber, eTag) == null) {
                uploadedParts.incrementAndGet();
            }
        }

        /**
         * 检查是否所有分片都已上传完成
         */
        public synchronized boolean isAllPartsUploaded() {
            return uploadedParts.get() >= totalParts;
        }

        /**
         * 获取上传进度（百分比）
         */
        public int getProgress() {
            if (totalParts == 0) return 0;
            return (int) (uploadedParts.get() * 100 / totalParts);
        }
    }

    /**
     * 创建上传上下文
     *
     * @param fileKey 文件唯一标识
     * @param cosPath COS存储路径
     * @param uploadId 上传ID
     * @param totalParts 总分片数
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @param fileMd5 文件MD5
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 上传上下文
     */
    public UploadContext createContext(String fileKey, String cosPath, String uploadId,
                                        int totalParts, String fileName, long fileSize,
                                        String fileMd5, Long tenantId, Long userId) {
        UploadContext context = UploadContext.builder()
                .fileKey(fileKey)
                .cosPath(cosPath)
                .uploadId(uploadId)
                .totalParts(totalParts)
                .uploadedParts(new AtomicInteger(0))
                .partETags(new ConcurrentHashMap<>())
                .fileName(fileName)
                .fileSize(fileSize)
                .fileMd5(fileMd5)
                .tenantId(tenantId)
                .userId(userId)
                .createTime(System.currentTimeMillis())
                .status(0)
                .build();
        
        contextCache.put(fileKey, context);
        log.info("创建分片上传上下文: fileKey={}, uploadId={}, totalParts={}", 
                fileKey, uploadId, totalParts);
        
        return context;
    }

    /**
     * 获取上传上下文
     *
     * @param fileKey 文件唯一标识
     * @return 上传上下文，不存在返回null
     */
    public UploadContext getContext(String fileKey) {
        return contextCache.get(fileKey);
    }

    /**
     * 更新分片上传进度
     *
     * @param fileKey 文件唯一标识
     * @param partNumber 分片序号
     * @param eTag 分片ETag
     */
    public void updatePartProgress(String fileKey, int partNumber, String eTag) {
        UploadContext context = contextCache.get(fileKey);
        if (context != null) {
            context.addPart(partNumber, eTag);
            log.debug("更新分片进度: fileKey={}, partNumber={}, progress={}%", 
                    fileKey, partNumber, context.getProgress());
        }
    }

    /**
     * 检查是否可以触发完成回调
     *
     * @param fileKey 文件唯一标识
     * @return 是否可以完成
     */
    public boolean canComplete(String fileKey) {
        UploadContext context = contextCache.get(fileKey);
        return context != null && context.isAllPartsUploaded() && context.getStatus() == 0;
    }

    /**
     * 标记上传完成
     *
     * @param fileKey 文件唯一标识
     */
    public void markCompleted(String fileKey) {
        UploadContext context = contextCache.get(fileKey);
        if (context != null) {
            context.setStatus(1);
            log.info("分片上传完成: fileKey={}, totalParts={}", 
                    fileKey, context.getTotalParts());
        }
    }

    /**
     * 标记上传取消
     *
     * @param fileKey 文件唯一标识
     */
    public void markCancelled(String fileKey) {
        UploadContext context = contextCache.get(fileKey);
        if (context != null) {
            context.setStatus(2);
            log.info("分片上传已取消: fileKey={}", fileKey);
        }
    }

    /**
     * 移除上传上下文
     *
     * @param fileKey 文件唯一标识
     */
    public void removeContext(String fileKey) {
        contextCache.remove(fileKey);
        log.debug("移除上传上下文: fileKey={}", fileKey);
    }

    /**
     * 获取上传进度
     *
     * @param fileKey 文件唯一标识
     * @return 进度信息，不存在返回null
     */
    public UploadProgress getProgress(String fileKey) {
        UploadContext context = contextCache.get(fileKey);
        if (context == null) {
            return null;
        }
        
        return UploadProgress.builder()
                .fileKey(fileKey)
                .totalParts(context.getTotalParts())
                .uploadedParts(context.getUploadedParts().get())
                .progress(context.getProgress())
                .status(context.getStatus())
                .build();
    }

    /**
     * 清理过期上下文（超过24小时的）
     */
    public void cleanupExpiredContexts() {
        long expireTime = System.currentTimeMillis() - 24 * 60 * 60 * 1000;
        contextCache.entrySet().removeIf(entry -> entry.getValue().getCreateTime() < expireTime);
        log.debug("清理过期上传上下文完成");
    }

    /**
     * 上传进度信息
     */
    @Data
    @Builder
    public static class UploadProgress {
        /**
         * 文件唯一标识
         */
        private String fileKey;
        
        /**
         * 总分片数
         */
        private int totalParts;
        
        /**
         * 已上传分片数
         */
        private int uploadedParts;
        
        /**
         * 进度百分比
         */
        private int progress;
        
        /**
         * 上传状态：0-进行中，1-完成，2-取消
         */
        private int status;
    }
}
