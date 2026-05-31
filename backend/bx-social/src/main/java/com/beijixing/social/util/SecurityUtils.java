package com.beijixing.social.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 安全工具集
 * 
 * 提供以下安全功能:
 * 1. 敏感数据脱敏（手机号、邮箱、身份证等）
 * 2. AES-256-GCM加密解密
 * 3. XSS攻击防护
 * 4. SQL注入检测
 *
 * @author 北极星AI团队
 * @version 3.0 (2026-05-20)
 */
@Slf4j
@Component
public class SecurityUtils {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "<script[^>]*>.*?</script>|javascript:|on\\w+\\s*=",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Map<String, String> SENSITIVE_PATTERNS = new HashMap<>();
    
    static {
        SENSITIVE_PATTERNS.put("phone", "(1[3-9]\\d)\\d{4}(\\d{4})");
        SENSITIVE_PATTERNS.put("email", "(\\w?)(\\w+@)(\\w+\\.\\w+)");
        SENSITIVE_PATTERNS.put("idCard", "(\\d{6})\\d{8}(\\d{4})");
        SENSITIVE_PATTERNS.put("bankCard", "(\\d{4})\\d+(\\d{4})");
    }

    /**
     * 手机号脱敏: 138****1234
     */
    public static String desensitizePhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.replaceAll(SENSITIVE_PATTERNS.get("phone"), "$1****$2");
    }

    /**
     * 邮箱脱敏: a***@gmail.com
     */
    public static String desensitizeEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return email;
        }
        return email.replaceAll(SENSITIVE_PATTERNS.get("email"), "$1***$2$3");
    }

    /**
     * 身份证号脱敏: 110***********1234
     */
    public static String desensitizeIdCard(String idCard) {
        if (!StringUtils.hasText(idCard) || idCard.length() < 14) {
            return idCard;
        }
        return idCard.replaceAll(SENSITIVE_PATTERNS.get("idCard"), "$1********$2");
    }

    /**
     * 银行卡号脱敏: 6222 **** **** 1234
     */
    public static String desensitizeBankCard(String bankCard) {
        if (!StringUtils.hasText(bankCard) || bankCard.length() < 8) {
            return bankCard;
        }
        return bankCard.replaceAll(SENSITIVE_PATTERNS.get("bankCard"), "$1 **** **** $2");
    }

    /**
     * AES-256-GCM加密
     * 
     * @param plaintext 明文
     * @param key 32字节密钥
     * @return Base64编码的密文（含IV和Tag）
     */
    public static String encrypt(String plaintext, String key) throws Exception {
        if (!StringUtils.hasText(plaintext)) return plaintext;
        
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        
        byte[] iv = new byte[12]; // GCM推荐IV长度12字节
        new java.security.SecureRandom().nextBytes(iv);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        
        // IV + 密文 + Tag 拼接后Base64编码
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
        
        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * AES-256-GCM解密
     */
    public static String decrypt(String ciphertext, String key) throws Exception {
        if (!StringUtils.hasText(ciphertext)) return ciphertext;
        
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        
        byte[] decoded = Base64.getDecoder().decode(ciphertext);
        
        byte[] iv = new byte[12];
        System.arraycopy(decoded, 0, iv, 0, 12);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
        byte[] decrypted = cipher.doFinal(decoded, 12, decoded.length - 12);
        
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * XSS过滤 - 移除危险HTML标签和事件属性
     */
    public static String sanitizeXss(String input) {
        if (!StringUtils.hasText(input)) return input;
        
        String sanitized = XSS_PATTERN.matcher(input).replaceAll("");

        // HTML转义特殊字符
        sanitized = sanitized.replace("<", "&lt;")
                            .replace(">", "&gt;")
                            .replace("\"", "&quot;")
                            .replace("'", "&#x27;");
        
        return sanitized;
    }

    /**
     * SQL注入检测
     * 
     * @param value 待检测的值
     * @return true表示可能包含SQL注入
     */
    public static boolean containsSqlInjection(String value) {
        if (!StringUtils.hasText(value)) return false;
        
        String upperValue = value.toUpperCase();
        String[] dangerousPatterns = {
            "' OR ", "--", "; DROP ", "; DELETE ", 
            " UNION SELECT ", " EXEC ", " EXECUTE ",
            "1=1", "1 = 1", " OR TRUE", " OR 1=1"
        };
        
        for (String pattern : dangerousPatterns) {
            if (upperValue.contains(pattern)) {
                log.warn("检测到潜在SQL注入: {}", value.substring(0, Math.min(value.length(), 50)));
                return true;
            }
        }
        
        return false;
    }

    /**
     * 生成安全的CSRF Token
     */
    public static String generateCsrfToken() {
        byte[] bytes = new byte[16];
        new java.security.SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 验证CSRF Token时效性（1小时有效）
     */
    public static boolean validateCsrfToken(String token, long creationTime) {
        if (!StringUtils.hasText(token)) return false;
        
        long ageMillis = System.currentTimeMillis() - creationTime;
        return ageMillis > 0 && ageMillis <= 3600000; // 1小时内有效
    }
}
