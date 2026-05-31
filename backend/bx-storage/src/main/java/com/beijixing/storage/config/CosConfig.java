package com.beijixing.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 腾讯云COS配置类
 * 
 * <p>用于配置腾讯云对象存储(COS)的相关参数，包括认证信息和存储桶配置。</p>
 *
 * @author 叶宇（EMP-OPS-001）
 * @version 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "cos")
public class CosConfig {

    /**
     * 腾讯云SecretId
     */
    private String secretId;

    /**
     * 腾讯云SecretKey
     */
    private String secretKey;

    /**
     * COS地域
     * 例如：ap-guangzhou（广州）、ap-shanghai（上海）
     */
    private String region;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * CDN加速域名（可选）
     * 如果配置了CDN，将优先使用CDN域名访问文件
     */
    private String cdnDomain;

    /**
     * 文件访问基础路径
     */
    private String basePath = "/storage";

    /**
     * 签名URL过期时间（秒）
     * 默认3600秒（1小时）
     */
    private Integer signUrlExpireSeconds = 3600;

    /**
     * 分片上传时每个分片的大小（字节）
     * 默认5MB
     */
    private Long partSize = 5242880L;

    /**
     * 最大分片数
     * 腾讯云COS限制最大10000个分片
     */
    private Integer maxParts = 10000;

    /**
     * 获取完整的Bucket名称（带-appid后缀）
     * 腾讯云COS的Bucket名称需要包含appid
     */
    public String getFullBucketName() {
        return bucketName;
    }

    /**
     * 获取文件访问域名
     * 优先返回CDN域名，否则返回COS默认域名
     */
    public String getAccessDomain() {
        if (cdnDomain != null && !cdnDomain.isEmpty()) {
            return cdnDomain;
        }
        return bucketName + ".cos." + region + ".myqcloud.com";
    }
}
