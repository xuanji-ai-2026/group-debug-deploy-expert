package com.beijixing.storage.cos;

import com.beijixing.storage.config.CosConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 腾讯云COS客户端封装类
 * 
 * <p>封装腾讯云COS SDK常用操作，提供简洁的上传、下载、删除等方法。</p>
 * <p>支持功能：</p>
 * <ul>
 *   <li>普通文件上传</li>
 *   <li>分片上传（支持断点续传）</li>
 *   <li>签名URL生成</li>
 *   <li>文件删除</li>
 *   <li>文件存在性检查</li>
 * </ul>
 *
 * @author 叶宇（EMP-OPS-001）
 * @version 1.0.0
 */
@Slf4j
@Component
public class TencentCosClient {

    private final CosConfig cosConfig;
    private COSClient cosClient;

    public TencentCosClient(CosConfig cosConfig) {
        this.cosConfig = cosConfig;
    }

    /**
     * 初始化COS客户端
     */
    @PostConstruct
    public void init() {
        String secretId = cosConfig.getSecretId();
        String secretKey = cosConfig.getSecretKey();
        
        if (secretId == null || secretId.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            log.warn("腾讯云COS未配置（secretId/secretKey为空），COS功能不可用");
            return;
        }

        COSCredentials credentials = new BasicCOSCredentials(secretId, secretKey);
        
        Region region = new Region(cosConfig.getRegion());
        
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setRegion(region);
        clientConfig.setHttpProtocol(HttpProtocol.https);
        clientConfig.setConnectionTimeout(60000);
        clientConfig.setSocketTimeout(60000);

        this.cosClient = new COSClient(credentials, clientConfig);
        
        log.info("腾讯云COS客户端初始化成功，Region: {}, Bucket: {}", 
                cosConfig.getRegion(), cosConfig.getBucketName());
    }

    /**
     * 销毁COS客户端
     */
    @PreDestroy
    public void destroy() {
        if (cosClient != null) {
            cosClient.shutdown();
            log.info("腾讯云COS客户端已关闭");
        }
    }

    /**
     * 上传文件到COS
     *
     * @param cosPath COS存储路径（如：images/2024/01/01/xxx.jpg）
     * @param file 要上传的文件
     * @return 上传结果，包含ETag等信息
     */
    public PutObjectResult uploadFile(String cosPath, File file) {
        if (cosClient == null) {
            throw new IllegalStateException("COS客户端未初始化");
        }
        PutObjectRequest putObjectRequest = new PutObjectRequest(
                cosConfig.getBucketName(),
                cosPath,
                file
        );
        PutObjectResult result = cosClient.putObject(putObjectRequest);
        log.debug("文件上传成功: {}, ETag: {}", cosPath, result.getETag());
        return result;
    }

    /**
     * 上传字节数组到COS
     *
     * @param cosPath COS存储路径
     * @param content 字节数组内容
     * @param contentType 内容类型（MIME类型）
     * @return 上传结果
     */
    public PutObjectResult uploadBytes(String cosPath, byte[] content, String contentType) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(content.length);
        PutObjectRequest putObjectRequest = new PutObjectRequest(
                cosConfig.getBucketName(),
                cosPath,
                new java.io.ByteArrayInputStream(content),
                metadata
        );
        
        PutObjectResult result = cosClient.putObject(putObjectRequest);
        log.debug("字节数组上传成功: {}, 长度: {}", cosPath, content.length);
        return result;
    }

    /**
     * 上传输入流到COS
     *
     * @param cosPath COS存储路径
     * @param inputStream 输入流
     * @param contentType 内容类型
     * @param contentLength 内容长度（字节）
     * @return 上传结果
     */
    public PutObjectResult uploadInputStream(String cosPath, InputStream inputStream,
                                              String contentType, long contentLength) {
        if (inputStream == null) {
            log.error("上传失败: inputStream为null, cosPath={}", cosPath);
            throw new IllegalArgumentException("InputStream不能为null");
        }
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(contentLength);
        PutObjectRequest putObjectRequest = new PutObjectRequest(
                cosConfig.getBucketName(),
                cosPath,
                inputStream,
                metadata
        );
        
        PutObjectResult result = cosClient.putObject(putObjectRequest);
        log.debug("输入流上传成功: {}, 长度: {}", cosPath, contentLength);
        return result;
    }

    /**
     * 初始化分片上传
     *
     * @param cosPath COS存储路径
     * @return 分片上传初始化结果，包含uploadId
     */
    public InitiateMultipartUploadResult initiateMultipartUpload(String cosPath) {
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(
                cosConfig.getBucketName(),
                cosPath
        );
        InitiateMultipartUploadResult result = cosClient.initiateMultipartUpload(request);
        log.info("分片上传初始化成功: {}, UploadId: {}", cosPath, result.getUploadId());
        return result;
    }

    /**
     * 上传分片
     *
     * @param cosPath COS存储路径
     * @param uploadId 分片上传ID
     * @param partNumber 分片序号（从1开始）
     * @param inputStream 分片内容流
     * @param partSize 分片大小
     * @return 分片上传结果
     */
    public UploadPartResult uploadPart(String cosPath, String uploadId, int partNumber,
                                        InputStream inputStream, long partSize) {
        UploadPartRequest uploadPartRequest = new UploadPartRequest();
        uploadPartRequest.setBucketName(cosConfig.getBucketName());
        uploadPartRequest.setKey(cosPath);
        uploadPartRequest.setUploadId(uploadId);
        uploadPartRequest.setInputStream(inputStream);
        uploadPartRequest.setPartSize(partSize);
        uploadPartRequest.setPartNumber(partNumber);
        
        UploadPartResult result = cosClient.uploadPart(uploadPartRequest);
        log.debug("分片上传成功: {}, PartNumber: {}, ETag: {}", 
                cosPath, partNumber, result.getETag());
        return result;
    }

    /**
     * 完成分片上传
     *
     * @param cosPath COS存储路径
     * @param uploadId 分片上传ID
     * @param parts 分片列表
     * @return 完成结果
     */
    public CompleteMultipartUploadResult completeMultipartUpload(String cosPath, String uploadId,
                                                                  List<PartETag> parts) {
        CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(
                cosConfig.getBucketName(),
                cosPath,
                uploadId,
                parts
        );
        CompleteMultipartUploadResult result = cosClient.completeMultipartUpload(request);
        log.info("分片上传完成: {}, UploadId: {}", cosPath, uploadId);
        return result;
    }

    /**
     * 列出已上传的分片
     *
     * @param cosPath COS存储路径
     * @param uploadId 分片上传ID
     * @return 分片列表
     */
    public List<PartETag> listParts(String cosPath, String uploadId) {
        ListPartsRequest listPartsRequest = new ListPartsRequest(
                cosConfig.getBucketName(),
                cosPath,
                uploadId
        );
        
        List<PartETag> allParts = new ArrayList<>();
        PartListing partListing;
        
        do {
            partListing = cosClient.listParts(listPartsRequest);
            for (PartSummary part : partListing.getParts()) {
                allParts.add(new PartETag(part.getPartNumber(), part.getETag()));
            }
            listPartsRequest.setPartNumberMarker(partListing.getNextPartNumberMarker());
        } while (partListing.isTruncated());
        
        log.debug("已上传分片数量: {}", allParts.size());
        return allParts;
    }

    /**
     * 中止分片上传
     *
     * @param cosPath COS存储路径
     * @param uploadId 分片上传ID
     */
    public void abortMultipartUpload(String cosPath, String uploadId) {
        AbortMultipartUploadRequest abortRequest = new AbortMultipartUploadRequest(
                cosConfig.getBucketName(),
                cosPath,
                uploadId
        );
        cosClient.abortMultipartUpload(abortRequest);
        log.info("分片上传已中止: {}, UploadId: {}", cosPath, uploadId);
    }

    /**
     * 删除文件
     *
     * @param cosPath COS存储路径
     */
    public void deleteFile(String cosPath) {
        DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(
                cosConfig.getBucketName(),
                cosPath
        );
        cosClient.deleteObject(deleteObjectRequest);
        log.info("文件已删除: {}", cosPath);
    }

    /**
     * 检查文件是否存在
     *
     * @param cosPath COS存储路径
     * @return 是否存在
     */
    public boolean doesFileExist(String cosPath) {
        return cosClient.doesObjectExist(cosConfig.getBucketName(), cosPath);
    }

    /**
     * 生成签名URL（用于私有读写）
     *
     * @param cosPath COS存储路径
     * @param expirationSeconds 过期时间（秒）
     * @return 签名URL
     */
    public String generatePresignedUrl(String cosPath, int expirationSeconds) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                cosConfig.getBucketName(),
                cosPath,
                HttpMethodName.GET
        );
        request.setExpiration(expiresInSeconds(expirationSeconds));
        
        java.net.URL url = cosClient.generatePresignedUrl(request);
        log.debug("签名URL生成成功: {}, 有效期: {}秒", cosPath, expirationSeconds);
        return url.toString();
    }

    /**
     * 生成带签名的下载URL（GET请求）
     *
     * @param cosPath COS存储路径
     * @return 签名URL
     */
    public String generateSignedDownloadUrl(String cosPath) {
        return generatePresignedUrl(cosPath, cosConfig.getSignUrlExpireSeconds());
    }

    /**
     * 获取COS客户端实例（用于高级操作）
     */
    public COSClient getCosClient() {
        return cosClient;
    }

    /**
     * 获取存储桶名称
     */
    public String getBucketName() {
        return cosConfig.getBucketName();
    }

    /**
     * 计算签名URL过期时间
     */
    private java.util.Date expiresInSeconds(int seconds) {
        return new java.util.Date(System.currentTimeMillis() + seconds * 1000L);
    }
}
