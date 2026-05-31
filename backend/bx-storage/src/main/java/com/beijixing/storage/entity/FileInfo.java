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
 * 文件信息实体类
 * 
 * <p>存储已上传文件的基本信息，包括文件名、大小、类型、存储路径等。</p>
 *
 * @author 叶宇（EMP-OPS-001）
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("bx_file_info")
public class FileInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文件ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 文件名（原始文件名）
     */
    private String fileName;

    /**
     * 文件存储名（COS中的实际文件名，含路径）
     */
    private String storageName;

    /**
     * 文件后缀（扩展名）
     */
    private String fileSuffix;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件MIME类型
     */
    private String contentType;

    /**
     * 文件MD5值（用于校验）
     */
    private String fileMd5;

    /**
     * 文件访问URL（优先CDN）
     */
    private String accessUrl;

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
     * 文件类型：image/video/audio/document/other
     */
    private String fileType;

    /**
     * 文件状态：0-正常，1-删除
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
     * 删除时间
     */
    private LocalDateTime deleteTime;
}
