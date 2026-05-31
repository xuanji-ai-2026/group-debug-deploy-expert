package com.beijixing.storage.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 文件信息返回VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String fileName;
    private String fileSuffix;
    private String fileType;  // 文件类型
    private Long fileSize;     // 文件大小（字节）
    private String fileUrl;
    private String category;
    private Long uploaderId;
    private String uploaderName;
    private String uploadTime;
    private String cosPath;
    private String accessUrl;
    private String contentType;
    private Boolean previewable;
}
