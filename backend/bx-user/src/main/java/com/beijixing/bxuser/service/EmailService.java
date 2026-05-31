package com.beijixing.bxuser.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class EmailService {

    @Value("${spring.mail.username:}")
    private String mailUsername;

    private final JavaMailSender javaMailSender;
    private final StringRedisTemplate redisTemplate;

    public EmailService(JavaMailSender javaMailSender,
                        @org.springframework.beans.factory.annotation.Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.javaMailSender = javaMailSender;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        if (mailUsername != null && !mailUsername.isEmpty()) {
            log.info("✅ 邮件服务初始化成功 - 发件人: {}", mailUsername);
        } else {
            log.warn("⚠️ 邮件服务未配置 - 请在application.yml中配置spring.mail相关属性");
        }
    }

    /**
     * 发送邮箱验证码
     * @param email 收件人邮箱
     * @param type 验证码类型：LOGIN/REGISTER/RESET_PWD/BIND_EMAIL
     * @return 是否发送成功
     */
    public boolean sendVerificationCode(String email, String type) {
        try {
            log.info("📧 开始发送邮箱验证码: email={}, type={}", email, type);

            if (javaMailSender == null || mailUsername == null || mailUsername.isEmpty()) {
                log.error("❌ 邮件服务未初始化！请检查application.yml中的spring.mail配置");
                return false;
            }

            // 频率限制检查
            String key = "email:code:" + email + ":" + type;
            try {
                if (redisTemplate != null && redisTemplate.hasKey(key)) {
                    Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                    if (ttl != null && ttl > 240) {
                        log.warn("⚠️ 发送频率限制: email={}, 剩余{}秒", email, ttl);
                        return false;
                    }
                }
            } catch (Exception redisEx) {
                log.error("❌ Redis操作异常: {}", redisEx.getMessage());
            }

            // 生成6位验证码
            String code = generateCode(6);
            log.info("   生成验证码: ****{}, Redis key: {}", code.substring(0, 2), key);

            // Redis原子锁: setIfAbsent防止高并发下验证码被覆盖
            // 场景: 用户快速连续点击"发送验证码"，多个线程同时执行到此
            // 无原子锁时: 后执行的set会覆盖先执行的code，导致用户输入的code与存储的不一致
            Boolean locked = false;
            try {
                if (redisTemplate != null) {
                    locked = redisTemplate.opsForValue().setIfAbsent(key, code, 5, TimeUnit.MINUTES);
                    if (Boolean.FALSE.equals(locked)) {
                        log.warn("⚠️ 验证码已存在(原子锁拦截): email={}, type={}", email, type);
                        return false;
                    }
                    log.info("   ✓ 验证码已原子写入Redis(setIfAbsent)，有效期5分钟");
                }
            } catch (Exception redisEx) {
                log.error("❌ Redis原子写操作异常: {}", redisEx.getMessage());
            }

            // 构建邮件内容
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailUsername);
            message.setTo(email);
            message.setSubject("【北极星AI】验证码");

            String emailContent = buildEmailContent(code, type);
            message.setText(emailContent);

            // 发送邮件
            javaMailSender.send(message);
            log.info("✅ 邮件发送成功: email={}, code=****{}, type={}", email, code.substring(0, 2), type);

            log.info("✅ 邮件发送完成(验证码已在发送前原子写入Redis)");

            return true;

        } catch (Exception e) {
            log.error("❌ 发送邮箱验证码异常: email={}, errorType={}, message={}",
                     email, e.getClass().getSimpleName(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 验证邮箱验证码
     * @param email 邮箱
     * @param code 用户输入的验证码
     * @param type 验证码类型
     * @return 是否验证通过
     */
    public boolean verifyCode(String email, String code, String type) {
        try {
            if (redisTemplate == null) {
                log.warn("Redis未配置，跳过验证码校验");
                return true;
            }

            String key = "email:code:" + email + ":" + type;
            String storedCode = redisTemplate.opsForValue().get(key);

            if (storedCode == null) {
                log.warn("⚠️ 验证码不存在或已过期: email={}, type={}", email, type);
                return false;
            }

            if (!storedCode.equals(code)) {
                log.warn("⚠️ 验证码错误: email={}, input={}, stored={}", email, code, storedCode);
                return false;
            }

            // 验证成功后删除验证码
            redisTemplate.delete(key);
            log.info("✅ 邮箱验证码验证成功: email={}, type={}", email, type);
            return true;

        } catch (Exception e) {
            log.error("❌ 邮箱验证码验证异常: email={}", email, e);
            return false;
        }
    }

    /**
     * 构建邮件内容
     */
    private String buildEmailContent(String code, String type) {
        StringBuilder content = new StringBuilder();
        content.append("尊敬的用户：\n\n");
        content.append("您好！\n\n");
        content.append("您的验证码为：").append(code).append("\n\n");
        content.append("验证码有效期为5分钟，请尽快使用。\n\n");

        switch (type.toUpperCase()) {
            case "LOGIN":
                content.append("您正在进行登录操作，如非本人操作请忽略此邮件。\n");
                break;
            case "REGISTER":
                content.append("您正在进行注册操作，如非本人操作请忽略此邮件。\n");
                break;
            case "RESET_PWD":
                content.append("您正在进行密码重置操作，如非本人操作请立即修改密码。\n");
                break;
            case "BIND_EMAIL":
                content.append("您正在进行绑定邮箱操作，如非本人操作请忽略此邮件。\n");
                break;
            default:
                content.append("如非本人操作请忽略此邮件。\n");
        }

        content.append("\n此邮件由系统自动发送，请勿直接回复。\n");
        content.append("\n北极星AI团队\n");
        content.append("https://www.beijixing-ai.com");

        return content.toString();
    }

    /**
     * 生成随机验证码
     */
    private String generateCode(int length) {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    /**
     * 检查邮件服务是否可用
     */
    public boolean isEmailServiceAvailable() {
        return javaMailSender != null && mailUsername != null && !mailUsername.isEmpty();
    }
}
