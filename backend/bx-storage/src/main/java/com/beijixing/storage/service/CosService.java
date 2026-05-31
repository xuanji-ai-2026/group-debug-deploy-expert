package com.beijixing.storage.service;

import com.qcloud.cos.model.*;

import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * COS操作服务接口
 * 
 * <p>封装腾讯云COS的底层操作，提供更简洁的API。</p>
 *
 * @author 叶宇（EMP-OPS-001）
 * @version 1.0.0
 */
public interface CosService {

    // ==================== 基础操作 ====================

    /**
     * 上传文件
     *
     * @param cosPath COS存储路径
     * @param file 本地文件
     * @return 上传结果
     */
    PutObjectResult upload(String cosPath, File file);

    /**
     * 上传字节数组
     *
     * @param cosPath COS存储路径
     * @param content 字节数组
     * @param contentType 内容类型
     * @return 上传结果
     */
    PutObjectResult upload(String cosPath, byte[] content, String contentType);

    /**
     * 上传输入流
     *
     * @param cosPath COS存储路径
     * @param inputStream 输入流
     * @param contentType 内容类型
     * @param contentLength 内容长度
     * @return 上传结果
     */
    PutObjectResult upload(String cosPath, InputStream inputStream, 
                          String contentType, long contentLength);

    /**
     * 下载文件到本地
     *
     * @param cosPath COS存储路径
     * @param localPath 本地保存路径
     * @return 下载的文件对象
     */
    COSObject download(String cosPath);

    /**
     * 获取文件输入流
     *
     * @param cosPath COS存储路径
     * @return COS对象
     */
    COSObject getObject(String cosPath);

    /**
     * 删除文件
     *
     * @param cosPath COS存储路径
     */
    void delete(String cosPath);

    /**
     * 检查文件是否存在
     *
     * @param cosPath COS存储路径
     * @return 是否存在
     */
    boolean exists(String cosPath);

    // ==================== 分片上传 ====================

    /**
     * 初始化分片上传
     *
     * @param cosPath COS存储路径
     * @return 初始化结果
     */
    InitiateMultipartUploadResult initiateMultipartUpload(String cosPath);

    /**
     * 上传分片
     *
     * @param cosPath COS存储路径
     * @param uploadId 上传ID
     * @param partNumber 分片序号（从1开始）
     * @param inputStream 分片内容
     * @param partSize 分片大小
     * @return 分片结果
     */
    UploadPartResult uploadPart(String cosPath, String uploadId, int partNumber,
                                InputStream inputStream, long partSize);

    /**
     * 列出已上传的分片
     *
     * @param cosPath COS存储路径
     * @param uploadId 上传ID
     * @return 分片列表
     */
    List<PartETag> listParts(String cosPath, String uploadId);

    /**
     * 完成分片上传
     *
     * @param cosPath COS存储路径
     * @param uploadId 上传ID
     * @param parts 分片列表
     * @return 完成结果
     */
    CompleteMultipartUploadResult completeMultipartUpload(String cosPath, String uploadId,
                                                           List<PartETag> parts);

    /**
     * 中止分片上传
     *
     * @param cosPath COS存储路径
     * @param uploadId 上传ID
     */
    void abortMultipartUpload(String cosPath, String uploadId);

    // ==================== 签名URL ====================

    /**
     * 生成签名URL
     *
     * @param cosPath COS存储路径
     * @param expirationSeconds 过期时间（秒）
     * @return 签名URL
     */
    String generatePresignedUrl(String cosPath, int expirationSeconds);

    /**
     * 生成默认过期时间的签名URL
     *
     * @param cosPath COS存储路径
     * @return 签名URL
     */
    String generateSignedUrl(String cosPath);

    // ==================== 工具方法 ====================

    /**
     * 获取存储桶名称
     */
    String getBucketName();

    /**
     * 获取访问域名
     */
    String getAccessDomain();

    /**
     * 构建完整的COS路径
     *
     * @param relativePath 相对路径
     * @return 完整COS路径
     */
    String buildCosPath(String relativePath);
}
