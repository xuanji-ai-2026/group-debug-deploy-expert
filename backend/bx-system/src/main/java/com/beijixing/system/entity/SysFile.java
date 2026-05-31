package com.beijixing.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统文件实体
 * 对应数据库表: sys_file
 *
 * 功能：SM-004 文件管理（文件上传、预览、删除）
 *
 * @author bx-system
 */
@Data
@TableName("sys_file")
public class SysFile implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文件ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件存储Key（OSS key 或本地相对路径）
     */
    private String fileKey;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 文件类型（MIME type）
     */
    private String fileType;

    /**
     * 文件扩展名
     */
    private String fileExt;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 存储方式：local、oss、minio
     */
    private String storageType;

    /**
     * 访问URL（CDN或直接访问地址）
     */
    private String accessUrl;

    /**
     * 文件上传用户ID
     */
    private Long uploadUserId;

    /**
     * 文件上传租户ID
     */
    private Long tenantId;

    /**
     * 文件用途标签（如：avatar、attachment、export）
     */
    private String tag;

    /**
     * 状态：0-禁用，1-正常
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 删除标记：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
}
