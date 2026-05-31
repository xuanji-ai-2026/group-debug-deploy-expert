package com.beijixing.storage.service;

import com.beijixing.storage.dto.UploadCallbackRequest;
import com.beijixing.storage.dto.UploadRequest;
import com.beijixing.storage.vo.FileVO;

import java.io.InputStream;
import java.util.List;

/**
 * 存储服务接口
 * 
 * <p>定义文件存储的核心业务方法，包括：</p>
 * <ul>
 *   <li>文件上传（普通上传和分片上传）</li>
 *   <li>文件访问（普通URL和签名URL）</li>
 *   <li>文件管理（查询、删除）</li>
 *   <li>分片上传管理（初始化、进度查询、完成）</li>
 * </ul>
 *
 * @author 叶宇（EMP-OPS-001）
 * @version 1.0.0
 */
public interface StorageService {

    // ==================== 文件上传 ====================

    /**
     * 普通文件上传
     *
     * @param request 上传请求
     * @param inputStream 文件输入流
     * @return 文件信息
     */
    FileVO uploadFile(UploadRequest request, InputStream inputStream);

    /**
     * 批量上传文件
     *
     * @param requests 上传请求列表
     * @return 文件信息列表
     */
    List<FileVO> uploadFiles(List<UploadRequest> requests);

    // ==================== 分片上传 ====================

    /**
     * 初始化分片上传
     *
     * @param request 上传请求
     * @return 分片上传信息，包含uploadId和fileKey
     */
    InitMultipartUploadResult initiateMultipartUpload(UploadRequest request);

    /**
     * 上传分片
     *
     * @param request 上传请求
     * @param inputStream 分片输入流
     * @return 分片上传结果
     */
    PartUploadResult uploadPart(UploadRequest request, InputStream inputStream);

    /**
     * 查询分片上传进度
     *
     * @param fileKey 文件唯一标识
     * @return 分片进度信息
     */
    UploadProgress queryUploadProgress(String fileKey);

    /**
     * 完成分片上传
     *
     * @param request 回调请求
     * @return 文件信息
     */
    FileVO completeMultipartUpload(UploadCallbackRequest request);

    /**
     * 取消分片上传
     *
     * @param fileKey 文件唯一标识
     */
    void abortMultipartUpload(String fileKey);

    // ==================== 文件访问 ====================

    /**
     * 获取文件访问URL
     *
     * @param fileKeyOrPath 文件Key或COS路径
     * @param signed 是否生成签名URL
     * @return 文件访问URL
     */
    String getAccessUrl(String fileKeyOrPath, boolean signed);

    /**
     * 获取文件信息
     *
     * @param fileKeyOrPath 文件Key或COS路径
     * @return 文件信息
     */
    FileVO getFileInfo(String fileKeyOrPath);

    /**
     * 下载文件到本地
     *
     * @param fileKeyOrPath 文件Key或COS路径
     * @param localPath 本地保存路径
     * @return 本地文件路径
     */
    String downloadFile(String fileKeyOrPath, String localPath);

    // ==================== 文件管理 ====================

    /**
     * 删除文件
     *
     * @param fileKeyOrPath 文件Key或COS路径
     */
    void deleteFile(String fileKeyOrPath);

    /**
     * 批量删除文件
     *
     * @param fileKeyOrPaths 文件Key或COS路径列表
     */
    void deleteFiles(List<String> fileKeyOrPaths);

    /**
     * 检查文件是否存在
     *
     * @param fileKeyOrPath 文件Key或COS路径
     * @return 是否存在
     */
    boolean fileExists(String fileKeyOrPath);

    // ==================== 内部类（返回结果） ====================

    /**
     * 初始化分片上传结果
     */
    class InitMultipartUploadResult {
        private String fileKey;
        private String uploadId;
        private String cosPath;
        private int totalParts;

        public InitMultipartUploadResult(String fileKey, String uploadId, String cosPath, int totalParts) {
            this.fileKey = fileKey;
            this.uploadId = uploadId;
            this.cosPath = cosPath;
            this.totalParts = totalParts;
        }

        public String getFileKey() { return fileKey; }
        public String getUploadId() { return uploadId; }
        public String getCosPath() { return cosPath; }
        public int getTotalParts() { return totalParts; }
    }

    /**
     * 分片上传结果
     */
    class PartUploadResult {
        private int partNumber;
        private String eTag;
        private int uploadedParts;
        private int totalParts;

        public PartUploadResult(int partNumber, String eTag, int uploadedParts, int totalParts) {
            this.partNumber = partNumber;
            this.eTag = eTag;
            this.uploadedParts = uploadedParts;
            this.totalParts = totalParts;
        }

        public int getPartNumber() { return partNumber; }
        public String geteTag() { return eTag; }
        public int getUploadedParts() { return uploadedParts; }
        public int getTotalParts() { return totalParts; }
    }

    /**
     * 上传进度信息
     */
    class UploadProgress {
        private String fileKey;
        private String uploadId;
        private int totalParts;
        private int uploadedParts;
        private int progress;
        private String status;

        public UploadProgress(String fileKey, String uploadId, int totalParts, 
                              int uploadedParts, int progress, String status) {
            this.fileKey = fileKey;
            this.uploadId = uploadId;
            this.totalParts = totalParts;
            this.uploadedParts = uploadedParts;
            this.progress = progress;
            this.status = status;
        }

        public String getFileKey() { return fileKey; }
        public String getUploadId() { return uploadId; }
        public int getTotalParts() { return totalParts; }
        public int getUploadedParts() { return uploadedParts; }
        public int getProgress() { return progress; }
        public String getStatus() { return status; }
    }
}
