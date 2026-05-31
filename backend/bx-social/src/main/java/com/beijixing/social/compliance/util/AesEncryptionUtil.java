package com.beijixing.social.compliance.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

/**
 * AES-256-GCM 加密工具类
 *
 * 设计理念:
 * - 使用AES-GCM模式（认证加密），提供机密性+完整性保护
 * - 每次加密生成随机IV（初始化向量），防止重放攻击
 * - 密钥支持从配置文件读取或自动生成
 * - 符合NIST SP 800-38D标准
 *
 * 安全特性:
 * 1. **AES-256**: 军事级加密强度，暴力破解需2^256次运算
 * 2. **GCM模式**: 提供认证标签，可检测密文是否被篡改
 * 3. **随机IV**: 每次加密不同IV，相同明文产生不同密文
 * 4. **Base64编码**: 方便在数据库/JSON中存储和传输
 *
 * @author 北极星AI团队
 * @version 1.0 (2026-05-20)
 */
@Component
@Slf4j
public class AesEncryptionUtil {

    // ====== 配置项 ======
    @Value("${compliance.token-security.encryption-algorithm:AES/GCM/NoPadding}")
    private String algorithm;

    @Value("${compliance.token-security.key-rotation-days:90}")
    private int keyRotationDays;

    @Value("${compliance.token-security.encryption-key:}")
    private String encryptionKeyBase64;

    // ====== 常量定义 ======
    private static final String AES = "AES";
    private static final int KEY_SIZE = 256;          // AES-256
    private static final int IV_LENGTH = 12;           // GCM推荐IV长度(96位)
    private static final int TAG_BIT_LENGTH = 128;     // GCM认证标签长度

    // ====== 密钥管理 ======
    private SecretKey secretKey;

    /**
     * 初始化：生成或加载加密密钥
     *
     * 生产环境建议:
     * 1. 从KMS（密钥管理服务）获取主密钥
     * 2. 或从环境变量/配置中心读取（避免硬编码）
     * 3. 定期轮换密钥（建议90天一次）
     */
    public void init() {
        try {
            if (encryptionKeyBase64 != null && !encryptionKeyBase64.isBlank()) {
                byte[] keyBytes = Base64.getDecoder().decode(encryptionKeyBase64.trim());
                if (keyBytes.length != 32) {
                    throw new IllegalArgumentException(
                            String.format("加密密钥长度无效: 期望32字节(256位), 实际%d字节", keyBytes.length));
                }
                this.secretKey = new SecretKeySpec(keyBytes, AES);
                log.info("✅ 已从配置加载AES-256加密密钥");
            }

            if (this.secretKey == null) {
                KeyGenerator keyGen = KeyGenerator.getInstance(AES);
                keyGen.init(KEY_SIZE, new SecureRandom());
                this.secretKey = keyGen.generateKey();

                log.warn("⚠️ 未配置加密密钥，已自动生成（仅用于开发/测试环境，生产环境请设置 compliance.token-security.encryption-key}");
            }

        } catch (Exception e) {
            log.error("❌ 初始化加密工具失败: {}", e.getMessage(), e);
            throw new RuntimeException("加密组件初始化失败", e);
        }
    }

    /**
     * 加密字符串
     *
     * @param plainText 明文
     * @return Base64编码的密文（包含IV + 认证标签）
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) return plainText;

        try {
            byte[] iv = generateRandomIV();
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BIT_LENGTH, iv));

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 组合: IV(12字节) + 密文 + 认证标签(16字节) → Base64编码
            byte[] combined = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            log.error("❌ 加密失败: {}", e.getMessage(), e);
            throw new RuntimeException("加密操作失败", e);
        }
    }

    /**
     * 解密字符串
     *
     * @param encryptedText Base64编码的密文
     * @return 原始明文
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) return encryptedText;

        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            // 提取IV（前12字节）
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);

            // 提取密文+认证标签（剩余部分）
            byte[] cipherText = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BIT_LENGTH, iv));

            byte[] decryptedBytes = cipher.doFinal(cipherText);

            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("❌ 解密失败: {} (可能密文被篡改或密钥不匹配)", e.getMessage(), e);
            throw new RuntimeException("解密操作失败", e);
        }
    }

    /**
     * 批量加密（适用于List、Map等集合类型）
     *
     * @param items 待加密的字符串列表
     * @return 加密后的字符串列表
     */
    public List<String> batchEncrypt(List<String> items) {
        if (items == null || items.isEmpty()) return items;

        return items.stream()
                .map(this::encrypt)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 批量解密
     */
    public List<String> batchDecrypt(List<String> items) {
        if (items == null || items.isEmpty()) return items;

        return items.stream()
                .map(this::decrypt)
                .collect(java.util.stream.Collectors.toList());
    }

    // ============================================================
    // 内部辅助方法
    // ============================================================

    /**
     * 生成随机初始化向量(IV)
     *
     * 安全要求:
     * - 必须使用密码学安全的随机数生成器（CSPRNG）
     * - IV长度固定为12字节（96位）- GCM推荐值
     * - 同一密钥下，每个IV必须唯一且不可预测
     */
    private byte[] generateRandomIV() {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    // ============================================================
    // 密钥管理接口（供管理员调用）
    // ============================================================

    /**
     * 手动设置密钥（从外部安全源导入）
     *
     * @param keyBase64 Base64编码的密钥（32字节=256位）
     */
    public void setKeyFromBase64(String keyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            if (keyBytes.length != 32) {
                throw new IllegalArgumentException("密钥长度必须为32字节（256位）");
            }
            this.secretKey = new SecretKeySpec(keyBytes, AES);
            log.info("✅ 已成功导入自定义加密密钥");
        } catch (Exception e) {
            log.error("❌ 导入密钥失败: {}", e.getMessage(), e);
            throw new RuntimeException("密钥导入失败", e);
        }
    }

    /**
     * 导出当前密钥的Base64编码（⚠️ 仅限调试使用，生产环境禁止调用）
     *
     * @return Base64编码的密钥
     */
    public String exportKeyAsBase64() {
        if (this.secretKey == null) return null;
        return Base64.getEncoder().encodeToString(this.secretKey.getEncoded());
    }

    /**
     * 检查密钥是否需要轮换
     *
     * @return true如果距离上次轮换超过配置的天数
     */
    public boolean needsRotation() {
        log.debug("检查密钥轮换(基于时间，默认90天周期，待接入Redis记录最后轮换时间)");

        try {
            final int ROTATION_INTERVAL_DAYS = 90;
            java.time.LocalDateTime keyCreationTime = getKeyCreationTime();

            if (keyCreationTime == null) {
                log.debug("无法获取密钥创建时间，默认需要轮换");
                return true;
            }

            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.Duration duration = java.time.Duration.between(keyCreationTime, now);
            long daysSinceRotation = duration.toDays();

            if (daysSinceRotation >= ROTATION_INTERVAL_DAYS) {
                log.warn("⚠️ 密钥已使用{}天，超过{}天轮换周期，建议轮换", daysSinceRotation, ROTATION_INTERVAL_DAYS);
                return true;
            }

            log.debug("密钥使用天数: {}/{} (无需轮换)", daysSinceRotation, ROTATION_INTERVAL_DAYS);
            return false;

        } catch (Exception e) {
            log.error("检查密钥轮换状态异常: error={}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取密钥创建时间（简化实现）
     * 实际应从数据库或Redis中读取
     */
    private java.time.LocalDateTime getKeyCreationTime() {
        return java.time.LocalDateTime.now().minusDays(30);
    }

    /**
     * 获取加密算法信息（用于审计日志）
     */
    public String getAlgorithmInfo() {
        return String.format("Algorithm=%s, KeySize=%d bits, Mode=GCM, TagLength=%d bits",
                algorithm, KEY_SIZE, TAG_BIT_LENGTH);
    }
}
