package com.beijixing.storage.util;

import org.apache.commons.io.FilenameUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件工具类
 * 
 * <p>提供文件操作的常用工具方法，包括路径生成、类型判断等。</p>
 *
 * @author 叶宇（EMP-OPS-001）
 * @version 1.0.0
 */
public class FileUtil {

    /**
     * 日期格式化器（按日）
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * 根据文件MimeType判断文件类型
     */
    private static final String TYPE_IMAGE = "image";
    private static final String TYPE_VIDEO = "video";
    private static final String TYPE_AUDIO = "audio";
    private static final String TYPE_DOCUMENT = "document";

    /**
     * 生成基于日期的存储路径
     * 
     * <p>格式：{category}/{yyyy/MM/dd}/{uuid}.{ext}</p>
     * 
     * @param category 文件分类目录
     * @param originalFileName 原始文件名
     * @return 存储路径
     */
    public static String generateStoragePath(String category, String originalFileName) {
        String datePath = LocalDate.now().format(DATE_FORMATTER);
        String extension = getExtension(originalFileName);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        
        StringBuilder path = new StringBuilder();
        if (category != null && !category.isEmpty()) {
            path.append(category).append("/");
        }
        path.append(datePath).append("/").append(uuid);
        
        if (extension != null && !extension.isEmpty()) {
            path.append(".").append(extension);
        }
        
        return path.toString();
    }

    /**
     * 获取文件扩展名
     * 
     * @param fileName 文件名
     * @return 扩展名（不含点），如 "jpg", "png", "pdf"
     */
    public static String getExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        String extension = FilenameUtils.getExtension(fileName);
        return extension != null ? extension.toLowerCase() : "";
    }

    /**
     * 获取不带扩展名的文件名
     * 
     * @param fileName 文件名
     * @return 不带扩展名的文件名
     */
    public static String getNameWithoutExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        return FilenameUtils.getBaseName(fileName);
    }

    /**
     * 根据MIME类型判断文件类型
     * 
     * @param mimeType 文件MIME类型
     * @return 文件类型：image/video/audio/document/other
     */
    public static String getFileType(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return TYPE_DOCUMENT;
        }
        
        if (mimeType.startsWith("image/")) {
            return TYPE_IMAGE;
        } else if (mimeType.startsWith("video/")) {
            return TYPE_VIDEO;
        } else if (mimeType.startsWith("audio/")) {
            return TYPE_AUDIO;
        } else if (mimeType.startsWith("text/") 
                || mimeType.contains("document")
                || mimeType.contains("spreadsheet")
                || mimeType.contains("presentation")
                || mimeType.contains("pdf")
                || mimeType.contains("word")) {
            return TYPE_DOCUMENT;
        }
        
        return "other";
    }

    /**
     * 根据扩展名判断文件是否可预览
     * 
     * @param extension 文件扩展名
     * @return 是否可预览
     */
    public static boolean isPreviewable(String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        String ext = extension.toLowerCase();
        return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") 
                || ext.equals("gif") || ext.equals("bmp") || ext.equals("webp")
                || ext.equals("pdf") || ext.equals("txt") || ext.equals("md");
    }

    /**
     * 格式化文件大小为可读字符串
     * 
     * @param size 文件大小（字节）
     * @return 格式化后的大小字符串，如 "1.5MB"
     */
    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + "B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2fKB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2fMB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2fGB", size / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 判断是否为图片类型
     */
    public static boolean isImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    /**
     * 判断是否为视频类型
     */
    public static boolean isVideo(String mimeType) {
        return mimeType != null && mimeType.startsWith("video/");
    }

    /**
     * 判断是否为音频类型
     */
    public static boolean isAudio(String mimeType) {
        return mimeType != null && mimeType.startsWith("audio/");
    }
}
