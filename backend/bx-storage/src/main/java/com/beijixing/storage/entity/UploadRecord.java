package com.beijixing.storage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分片上传记录实体类
 * 
 * <p>用于记录分片上传的进度信息，支持断点续传功能。</p>
 * <p>当大文件使用分片上传时，系统会记录每个分片的上传状态，
 * 上传中断后可从上次成功的分片位置继续上传。</p>
 *
 * @author 叶宇（EMP-OPS-001）
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("bx_upload_record")
public class UploadRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 上传ID（腾讯云COS的分片上传任务ID）
     */
    private String uploadId;

    /**
     * 文件唯一标识（客户端生成，用于关联分片）
     */
    private String fileKey;

    /**
     * 文件名（原始文件名）
     */
    private String fileName;

    /**
     * 文件总大小（字节）
     */
    private Long totalSize;

    /**
     * 文件MD5值
     */
    private String fileMd5;

    /**
     * 分片大小（字节）
     */
    private Long partSize;

    /**
     * 总分片数
     */
    private Integer totalParts;

    /**
     * 已上传分片数
     */
    private Integer uploadedParts;

    /**
     * 已上传的分片序号列表（JSON格式：[1,2,3...]）
     */
    private String uploadedPartNumbers;

    /**
     * COS存储路径
     */
    private String cosPath;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 上传用户ID
     */
    private Long userId;

    /**
     * 上传状态：0-进行中，1-已完成，2-已取消，3-已过期
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 过期时间（分片上传有效期7天）
     */
    private LocalDateTime expireTime;

    /**
     * 完成时间
     */
    private LocalDateTime completeTime;
}
