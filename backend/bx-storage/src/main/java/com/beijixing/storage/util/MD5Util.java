package com.beijixing.storage.util;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5工具类
 * 
 * <p>提供MD5哈希计算的常用方法，用于文件完整性校验。</p>
 *
 * @author 叶宇（EMP-OPS-001）
 * @version 1.0.0
 */
public class MD5Util {

    /**
     * 计算字符串的MD5值
     * 
     * @param input 输入字符串
     * @return MD5哈希值（32位十六进制小写）
     */
    public static String md5(String input) {
        if (input == null) {
            return null;
        }
        return DigestUtils.md5Hex(input);
    }

    /**
     * 计算字节数组的MD5值
     * 
     * @param bytes 输入字节数组
     * @return MD5哈希值（32位十六进制小写）
     */
    public static String md5(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return DigestUtils.md5Hex(bytes);
    }

    /**
     * 计算文件的MD5值
     * 
     * <p>使用缓冲流读取文件，避免大文件导致内存溢出。</p>
     * 
     * @param file 输入文件
     * @return MD5哈希值（32位十六进制小写）
     * @throws IOException 如果文件读取失败
     */
    public static String md5(File file) throws IOException {
        if (file == null || !file.exists()) {
            return null;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            return DigestUtils.md5Hex(fis);
        }
    }

    /**
     * 计算输入流的MD5值
     * 
     * <p>注意：调用方需要负责关闭输入流。</p>
     * 
     * @param inputStream 输入流
     * @return MD5哈希值（32位十六进制小写）
     * @throws IOException 如果读取失败
     */
    public static String md5(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return null;
        }
        return DigestUtils.md5Hex(inputStream);
    }

    /**
     * 计算部分文件的MD5值（用于分片校验）
     * 
     * @param file 输入文件
     * @param start 起始位置（字节）
     * @param length 要读取的长度（字节）
     * @return MD5哈希值（32位十六进制小写）
     * @throws IOException 如果文件读取失败
     */
    public static String md5Partial(File file, long start, long length) throws IOException {
        if (file == null || !file.exists()) {
            return null;
        }
        
        try (FileInputStream fis = new FileInputStream(file)) {
            // 跳过起始位置
            if (start > 0) {
                fis.skip(start);
            }
            
            byte[] buffer = new byte[8192];
            long remaining = length;
            MessageDigest md = MessageDigest.getInstance("MD5");
            
            while (remaining > 0) {
                int read = fis.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read == -1) {
                    break;
                }
                md.update(buffer, 0, read);
                remaining -= read;
            }
            
            return bytesToHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            // MD5算法总是存在
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    /**
     * 验证文件MD5
     * 
     * @param file 文件
     * @param expectedMd5 期望的MD5值
     * @return 是否匹配
     */
    public static boolean verify(File file, String expectedMd5) {
        try {
            String actualMd5 = md5(file);
            return actualMd5 != null && actualMd5.equalsIgnoreCase(expectedMd5);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xff);
            if (hex.length() == 1) {
                sb.append("0");
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * 判断MD5值是否有效（32位十六进制）
     */
    public static boolean isValidMd5(String md5) {
        if (md5 == null) {
            return false;
        }
        return md5.matches("^[a-fA-F0-9]{32}$");
    }
}
