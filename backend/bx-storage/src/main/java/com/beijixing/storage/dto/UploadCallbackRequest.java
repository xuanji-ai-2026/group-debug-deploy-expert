package com.beijixing.storage.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分片上传回调请求DTO
 * 
 * <p>用于接收客户端完成所有分片上传后的回调请求，
 * 服务端执行合并分片操作。</p>
 *
 * @author 叶宇（EMP-OPS-001）
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadCallbackRequest {

    /**
     * 文件唯一标识（必填）
     */
    @NotBlank(message = "文件标识不能为空")
    private String fileKey;

    /**
     * 上传ID（腾讯云COS返回）
     */
    @NotBlank(message = "上传ID不能为空")
    private String uploadId;

    /**
     * 文件名（原始文件名）
     */
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件MD5值
     */
    private String fileMd5;

    /**
     * COS存储路径
     */
    private String cosPath;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 文件分类目录
     */
    private String category;
}
