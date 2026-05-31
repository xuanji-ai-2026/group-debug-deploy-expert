package com.beijixing.bxuser.service;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TencentSmsService {

    @Value("${tencent.secretId}")
    private String secretId;

    @Value("${tencent.secretKey}")
    private String secretKey;

    @Value("${tencent.sms.sdkAppId}")
    private String sdkAppId;

    @Value("${tencent.sms.signName}")
    private String signName;

    @Value("${tencent.sms.templateId:2641916}")
    private String templateId;

    private SmsClient smsClient;

    private final StringRedisTemplate redisTemplate;

    public TencentSmsService(@org.springframework.beans.factory.annotation.Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            log.info("🔧 开始初始化腾讯云SMS客户端...");
            log.info("   配置检查 - secretId: {}..., secretKey: {}..., sdkAppId: {}, signName: {}, templateId: {}",
                     secretId != null ? secretId.substring(0, Math.min(secretId.length(), 8)) : "null",
                     secretKey != null ? "******" : "null",
                     sdkAppId, signName, templateId);

            if (secretId == null || secretId.trim().isEmpty()) {
                log.error("❌ secretId为空！请检查application.yml中的tencent.secretId配置");
                return;
            }

            if (secretKey == null || secretKey.trim().isEmpty()) {
                log.error("❌ secretKey为空！请检查application.yml中的tencent.secretKey配置");
                return;
            }

            Credential cred = new Credential(secretId, secretKey);
            log.info("   ✓ Credential创建成功");

            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("sms.tencentcloudapi.com");
            httpProfile.setReqMethod(HttpProfile.REQ_POST);
            log.info("   ✓ HttpProfile配置完成");

            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            log.info("   ✓ ClientProfile配置完成");

            smsClient = new SmsClient(cred, "ap-guangzhou", clientProfile);
            log.info("✅ 腾讯云SMS客户端初始化成功 - SDKAppId: {}", sdkAppId);

        } catch (Exception e) {
            log.error("❌ 腾讯云SMS客户端初始化失败: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            if (e.getMessage() != null && e.getMessage().contains("credential")) {
                log.error("   ⚠️ 可能原因：secretId或secretKey格式错误！");
                log.error("   secretId格式示例：AKIDxxxxxxxxxxxxxx（不是纯数字）");
                log.error("   请到腾讯云控制台 -> 访问管理 -> API密钥管理 获取正确的凭证");
            }
        }
    }

    /**
     * 发送短信验证码
     * @param phone 手机号
     * @param type 验证码类型：LOGIN/REGISTER/RESET_PWD
     * @return 是否发送成功
     */
    public boolean sendVerificationCode(String phone, String type) {
        try {
            log.info("📱 开始发送短信验证码: phone={}, type={}", phone, type);

            if (smsClient == null) {
                log.error("❌ SMS客户端未初始化！请检查腾讯云配置和日志中的初始化错误信息");
                log.error("   配置状态: secretId={}..., sdkAppId={}", 
                         secretId != null ? secretId.substring(0, Math.min(secretId.length(), 8)) : "null",
                         sdkAppId);
                return false;
            }

            String code = generateCode(6);
            String key = "sms:code:" + phone + ":" + type;
            log.info("   生成验证码: ****{}, Redis key: {}", code.substring(0, 2), key);

            try {
                if (redisTemplate != null && redisTemplate.hasKey(key)) {
                    Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                    if (ttl != null && ttl > 240) {
                        log.warn("⚠️ 发送频率限制: phone={}, 剩余{}秒", phone, ttl);
                        return false;
                    }
                }
                log.info("   ✓ Redis频率检查通过");
            } catch (Exception redisEx) {
                log.error("❌ Redis操作异常: {} - {}", redisEx.getClass().getSimpleName(), redisEx.getMessage());
            }

            SendSmsRequest req = new SendSmsRequest();
            req.setSmsSdkAppId(sdkAppId);
            req.setSignName(signName);
            req.setTemplateId(templateId);

            String[] phoneNumberSet = {"+86" + phone};
            req.setPhoneNumberSet(phoneNumberSet);

            ArrayList<String> templateParamSet = new ArrayList<>();
            templateParamSet.add(code);
            templateParamSet.add("5");
            req.setTemplateParamSet(templateParamSet.toArray(new String[0]));
            log.info("   ✓ 请求参数构建完成，准备调用腾讯云API...");

            SendSmsResponse resp = smsClient.SendSms(req);
            log.info("   ✓ 腾讯云API响应成功");

            if ("Ok".equals(resp.getSendStatusSet()[0].getCode())) {
                try {
                    if (redisTemplate != null) {
                        redisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);
                    }
                    log.info("✅ 短信发送成功: phone={}, code=****{}, type={}", phone, code.substring(0, 2), type);
                } catch (Exception redisEx) {
                    log.warn("⚠️ Redis存储验证码失败，但短信已发送: {}", redisEx.getMessage());
                }
                return true;
            } else {
                log.error("❌ 短信发送失败: phone={}, errorCode={}, errorMessage={}", 
                         phone, resp.getSendStatusSet()[0].getCode(), 
                         resp.getSendStatusSet()[0].getMessage());
                log.error("   常见错误码: FailedOperation=操作失败, LimitExceeded=频率限制, UnauthorizedOperation=未授权");
                return false;
            }

        } catch (TencentCloudSDKException e) {
            log.error("❌ 腾讯云API异常: phone={}, requestId={}, message={}", 
                     phone, e.getRequestId(), e.getMessage(), e);
            log.error("   可能原因: 1. 凭证错误(secretId/secretKey) 2. 签名未审核通过 3. 模板未审核通过 4. 账号余额不足");
            return false;
        } catch (Exception e) {
            log.error("❌ 发送短信异常: phone={}, errorType={}, message={}", 
                     phone, e.getClass().getSimpleName(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 验证短信验证码
     * @param phone 手机号
     * @param code 用户输入的验证码
     * @param type 验证码类型
     * @return 是否验证通过
     */
    public boolean verifyCode(String phone, String code, String type) {
        try {
            if (redisTemplate == null) {
                log.warn("Redis未配置，跳过验证码校验");
                return true;
            }
            String key = "sms:code:" + phone + ":" + type;
            String storedCode = redisTemplate.opsForValue().get(key);

            if (storedCode == null) {
                log.warn("⚠️ 验证码不存在或已过期: phone={}, type={}", phone, type);
                return false;
            }

            if (!storedCode.equals(code)) {
                log.warn("⚠️ 验证码错误: phone={}, input={}, stored={}", phone, code, storedCode);
                return false;
            }

            redisTemplate.delete(key);
            log.info("✅ 验证码验证成功: phone={}, type={}", phone, type);
            return true;

        } catch (Exception e) {
            log.error("❌ 验证码验证异常: phone={}", phone, e);
            return false;
        }
    }

    /**
     * 生成随机验证码
     * @param length 验证码长度
     * @return 验证码字符串
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
     * 检查SMS客户端是否初始化成功
     * @return 是否已初始化
     */
    public boolean isSmsClientInitialized() {
        return smsClient != null;
    }

    /**
     * 获取配置状态信息（用于诊断）
     * @return 配置信息Map
     */
    public Map<String, Object> getConfigStatus() {
        Map<String, Object> config = new HashMap<>();
        config.put("secretId", secretId != null ? secretId.substring(0, Math.min(secretId.length(), 8)) + "***" : "null");
        config.put("secretKey", secretKey != null ? "******" : "null");
        config.put("sdkAppId", sdkAppId);
        config.put("signName", signName);
        config.put("templateId", templateId);
        config.put("smsClientInitialized", smsClient != null);
        return config;
    }
}
