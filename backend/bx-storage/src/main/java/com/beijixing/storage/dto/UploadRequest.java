package com.beijixing.storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传请求DTO
 * 
 * <p>用于接收文件上传请求参数，支持普通上传和分片上传场景。</p>
 *
 * @author 叶宇（EMP-OPS-001）
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadRequest {

    /**
     * 文件唯一标识（分片上传时必填，用于关联分片）
     */
    private String fileKey;

    /**
     * 文件名（原始文件名）
     */
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /**
     * 文件大小（字节）
     */
    @NotNull(message = "文件大小不能为空")
    @Positive(message = "文件大小必须为正数")
    private Long fileSize;

    /**
     * 文件MD5值（可选，用于校验）
     */
    private String fileMd5;

    /**
     * 分片序号（分片上传时必填，从1开始）
     */
    private Integer partNumber;

    /**
     * 总分片数（分片上传时必填）
     */
    private Integer totalParts;

    /**
     * 上传ID（分片上传时必填，标识一次分片上传任务）
     */
    private String uploadId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 文件分类目录（可选，如：avatar/content/attachment等）
     */
    private String category;

    /**
     * 是否需要签名URL访问（默认true）
     */
    @Builder.Default
    private Boolean signedUrl = true;

    /**
     * 判断是否为分片上传请求
     */
    public boolean isChunkedUpload() {
        return partNumber != null && totalParts != null && uploadId != null;
    }

    /**
     * 判断是否为初始化分片上传
     */
    public boolean isInitiateUpload() {
        return fileKey != null && partNumber == null && totalParts == null;
    }
}
