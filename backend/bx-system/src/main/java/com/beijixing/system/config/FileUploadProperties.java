package com.beijixing.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件上传配置属性
 *
 * @author bx-system
 */
@Data
@Component
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadProperties {

    /**
     * 文件存储路径
     */
    private String path = "/data/uploads";

    /**
     * 允许的文件扩展名
     */
    private String allowedExtensions = "jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,zip,rar";

    /**
     * 最大文件大小（字节）
     */
    private Long maxSize = 104857600L;
}
