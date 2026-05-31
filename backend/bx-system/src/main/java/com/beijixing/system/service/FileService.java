package com.beijixing.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beijixing.system.entity.SysFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件管理服务接口
 *
 * 功能：SM-004 文件管理（文件上传、预览、删除）
 *
 * @author bx-system
 */
public interface FileService {

    /**
     * 上传文件
     *
     * @param file 上传的文件
     * @param tag 用途标签
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @return 文件记录
     */
    SysFile upload(MultipartFile file, String tag, Long userId, Long tenantId);

    /**
     * 上传文件（带自定义文件名）
     *
     * @param file 上传的文件
     * @param tag 用途标签
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @param customName 自定义文件名
     * @return 文件记录
     */
    SysFile upload(MultipartFile file, String tag, Long userId, Long tenantId, String customName);

    /**
     * 根据文件Key获取文件信息
     *
     * @param fileKey 文件Key
     * @return 文件记录
     */
    SysFile getByFileKey(String fileKey);

    /**
     * 根据ID获取文件信息
     *
     * @param id 文件ID
     * @return 文件记录
     */
    SysFile getById(Long id);

    /**
     * 分页查询文件列表
     *
     * @param page 页码
     * @param size 每页数量
     * @param tag 用途标签
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @return 分页结果
     */
    Page<SysFile> pageFiles(Integer page, Integer size, String tag, Long userId, Long tenantId);

    /**
     * 获取用户文件列表
     *
     * @param userId 用户ID
     * @return 文件列表
     */
    List<SysFile> listByUser(Long userId);

    /**
     * 获取用户指定标签文件列表
     *
     * @param tag 标签
     * @param userId 用户ID
     * @return 文件列表
     */
    List<SysFile> listByTag(String tag, Long userId);

    /**
     * 删除文件
     *
     * @param id 文件ID
     * @param userId 用户ID（用于权限校验）
     */
    void delete(Long id, Long userId);

    /**
     * 批量删除文件
     *
     * @param ids 文件ID列表
     * @param userId 用户ID（用于权限校验）
     */
    void batchDelete(List<Long> ids, Long userId);

    /**
     * 下载文件（获取文件字节数组）
     *
     * @param fileKey 文件Key
     * @return 文件字节数组
     */
    byte[] download(String fileKey);

    /**
     * 获取文件的访问URL
     *
     * @param fileKey 文件Key
     * @return 访问URL
     */
    String getAccessUrl(String fileKey);
}
