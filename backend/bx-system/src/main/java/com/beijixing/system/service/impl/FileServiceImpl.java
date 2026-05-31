package com.beijixing.system.service.impl;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beijixing.system.config.FileUploadProperties;
import com.beijixing.system.entity.SysFile;
import com.beijixing.system.mapper.SysFileMapper;
import com.beijixing.system.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 文件管理服务实现
 *
 * @author bx-system
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("nullness")
public class FileServiceImpl implements FileService {

    private final SysFileMapper fileMapper;
    private final FileUploadProperties uploadProperties;

    @Override
    public SysFile upload(MultipartFile file, String tag, Long userId, Long tenantId) {
        return upload(file, tag, userId, tenantId, null);
    }

    @Override
    public SysFile upload(MultipartFile file, String tag, Long userId, Long tenantId, String customName) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        // 校验扩展名
        String originalFilename = file.getOriginalFilename();
        String fileExt = FileUtil.extName(originalFilename);
        List<String> allowedExts = Arrays.asList(uploadProperties.getAllowedExtensions().split(","));
        if (!allowedExts.contains(fileExt.toLowerCase())) {
            throw new IllegalArgumentException("不支持的文件类型：" + fileExt);
        }

        // 校验文件大小
        if (file.getSize() > uploadProperties.getMaxSize()) {
            throw new IllegalArgumentException("文件大小超出限制，最大支持：" + (uploadProperties.getMaxSize() / 1024 / 1024) + "MB");
        }

        // 生成存储路径
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileKey = datePath + "/" + UUID.randomUUID().toString().replace("-", "") + "." + fileExt;

        // 确保目录存在
        Path uploadPath = Paths.get(uploadProperties.getPath(), datePath);
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("创建上传目录失败：" + uploadPath, e);
        }

        // 保存文件
        @SuppressWarnings("nullness")
        File destFile = uploadPath.resolve(fileKey.substring(fileKey.lastIndexOf("/") + 1)).toFile();
        try {
            file.transferTo(destFile);
        } catch (IOException e) {
            throw new RuntimeException("保存文件失败", e);
        }

        // 构建访问URL（本地存储模式）
        String accessUrl = "/api/v1/files/" + fileKey;

        // 写入数据库
        SysFile sysFile = new SysFile();
        sysFile.setFileKey(fileKey);
        sysFile.setOriginalName(customName != null ? customName : originalFilename);
        sysFile.setFileType(file.getContentType());
        sysFile.setFileExt(fileExt);
        sysFile.setFileSize(file.getSize());
        sysFile.setStorageType("local");
        sysFile.setAccessUrl(accessUrl);
        sysFile.setUploadUserId(userId);
        sysFile.setTenantId(tenantId);
        sysFile.setTag(tag != null ? tag : "default");
        sysFile.setStatus(1);

        fileMapper.insert(sysFile);
        log.info("文件上传成功：fileKey={}, originalName={}, size={}", fileKey, originalFilename, file.getSize());

        return sysFile;
    }

    @Override
    public SysFile getByFileKey(String fileKey) {
        return fileMapper.selectByFileKey(fileKey);
    }

    @Override
    public SysFile getById(Long id) {
        return fileMapper.selectById(id);
    }

    @Override
    public Page<SysFile> pageFiles(Integer page, Integer size, String tag, Long userId, Long tenantId) {
        Page<SysFile> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysFile> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(tag)) {
            wrapper.eq(SysFile::getTag, tag);
        }
        if (userId != null) {
            wrapper.eq(SysFile::getUploadUserId, userId);
        }
        if (tenantId != null) {
            wrapper.eq(SysFile::getTenantId, tenantId);
        }
        wrapper.eq(SysFile::getStatus, 1);
        wrapper.orderByDesc(SysFile::getCreateTime);

        return fileMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public List<SysFile> listByUser(Long userId) {
        return fileMapper.selectByUserId(userId);
    }

    @Override
    public List<SysFile> listByTag(String tag, Long userId) {
        return fileMapper.selectByTag(tag, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId) {
        SysFile file = fileMapper.selectById(id);
        if (file == null) {
            throw new IllegalArgumentException("文件不存在：" + id);
        }
        // 校验权限
        if (userId != null && !userId.equals(file.getUploadUserId())) {
            throw new IllegalArgumentException("无权限删除此文件");
        }
        // 物理删除文件
        Path physicalPath = Paths.get(uploadProperties.getPath(), file.getFileKey());
        try {
            Files.deleteIfExists(physicalPath);
        } catch (IOException e) {
            log.warn("物理删除文件失败：{}", physicalPath, e);
        }
        // 逻辑删除记录
        fileMapper.deleteById(id);
        log.info("删除文件：id={}, fileKey={}", id, file.getFileKey());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids, Long userId) {
        for (Long id : ids) {
            delete(id, userId);
        }
        log.info("批量删除文件，数量：{}", ids.size());
    }

    @Override
    public byte[] download(String fileKey) {
        SysFile file = fileMapper.selectByFileKey(fileKey);
        if (file == null) {
            throw new IllegalArgumentException("文件不存在：" + fileKey);
        }
        Path physicalPath = Paths.get(uploadProperties.getPath(), file.getFileKey());
        try {
            return Files.readAllBytes(physicalPath);
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败：" + fileKey, e);
        }
    }

    @Override
    public String getAccessUrl(String fileKey) {
        return "/api/v1/files/" + fileKey;
    }
}
