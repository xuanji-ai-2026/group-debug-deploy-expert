package com.beijixing.storage.service.impl;

import com.beijixing.storage.config.CosConfig;
import com.beijixing.storage.cos.TencentCosClient;
import com.beijixing.storage.service.CosService;
import com.qcloud.cos.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * COS操作服务实现类
 * 
 * <p>封装腾讯云COS的底层操作，提供更简洁的API。</p>
 *
 * @author 叶宇（EMP-OPS-001）
 * @version 1.0.0
 */
@Slf4j
@Service
public class CosServiceImpl implements CosService {

    private final TencentCosClient cosClient;
    private final CosConfig cosConfig;

    public CosServiceImpl(TencentCosClient cosClient, CosConfig cosConfig) {
        this.cosClient = cosClient;
        this.cosConfig = cosConfig;
    }

    @Override
    public PutObjectResult upload(String cosPath, File file) {
        return cosClient.uploadFile(cosPath, file);
    }

    @Override
    public PutObjectResult upload(String cosPath, byte[] content, String contentType) {
        return cosClient.uploadBytes(cosPath, content, contentType);
    }

    @Override
    public PutObjectResult upload(String cosPath, InputStream inputStream,
                                  String contentType, long contentLength) {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream不能为null");
        }
        return cosClient.uploadInputStream(cosPath, inputStream, contentType, contentLength);
    }

    @Override
    public COSObject download(String cosPath) {
        return cosClient.getCosClient().getObject(cosConfig.getBucketName(), cosPath);
    }

    @Override
    public COSObject getObject(String cosPath) {
        return cosClient.getCosClient().getObject(cosConfig.getBucketName(), cosPath);
    }

    @Override
    public void delete(String cosPath) {
        cosClient.deleteFile(cosPath);
    }

    @Override
    public boolean exists(String cosPath) {
        return cosClient.doesFileExist(cosPath);
    }

    @Override
    public InitiateMultipartUploadResult initiateMultipartUpload(String cosPath) {
        return cosClient.initiateMultipartUpload(cosPath);
    }

    @Override
    public UploadPartResult uploadPart(String cosPath, String uploadId, int partNumber,
                                        InputStream inputStream, long partSize) {
        return cosClient.uploadPart(cosPath, uploadId, partNumber, inputStream, partSize);
    }

    @Override
    public List<PartETag> listParts(String cosPath, String uploadId) {
        return cosClient.listParts(cosPath, uploadId);
    }

    @Override
    public CompleteMultipartUploadResult completeMultipartUpload(String cosPath, String uploadId,
                                                                 List<PartETag> parts) {
        return cosClient.completeMultipartUpload(cosPath, uploadId, parts);
    }

    @Override
    public void abortMultipartUpload(String cosPath, String uploadId) {
        cosClient.abortMultipartUpload(cosPath, uploadId);
    }

    @Override
    public String generatePresignedUrl(String cosPath, int expirationSeconds) {
        return cosClient.generatePresignedUrl(cosPath, expirationSeconds);
    }

    @Override
    public String generateSignedUrl(String cosPath) {
        return cosClient.generateSignedDownloadUrl(cosPath);
    }

    @Override
    public String getBucketName() {
        return cosConfig.getBucketName();
    }

    @Override
    public String getAccessDomain() {
        return cosConfig.getAccessDomain();
    }

    @Override
    public String buildCosPath(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return relativePath;
        }
        // 如果相对路径以/开头，去掉开头的/
        String path = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        // 如果配置了basePath，添加到路径前面
        if (cosConfig.getBasePath() != null && !cosConfig.getBasePath().isEmpty()) {
            String basePath = cosConfig.getBasePath().startsWith("/") 
                    ? cosConfig.getBasePath().substring(1) 
                    : cosConfig.getBasePath();
            return basePath + "/" + path;
        }
        return path;
    }
}
