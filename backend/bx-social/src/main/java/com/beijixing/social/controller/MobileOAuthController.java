package com.beijixing.social.controller;

import com.beijixing.ai.model.ApiResponse;
import com.beijixing.social.compliance.service.TokenSecurityService;
import com.beijixing.social.config.OAuthConfig;
import com.beijixing.social.entity.SocialAccount;
import com.beijixing.social.service.OAuthService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * 移动端OAuth授权控制器 v2.0 (2026合规版)
 *
 * 核心功能:
 * 1. **生成授权URL**: 支持PKCE安全扩展，防止授权码拦截攻击
 * 2. **处理Deep Link回调**: Android Intent Scheme + iOS Universal Links
 * 3. **Token交换**: 用authorization_code换取access_token + refresh_token
 * 4. **用户信息获取**: 获取社交平台用户昵称、头像等基本信息
 *
 * OAuth 2.0 + PKCE流程:
 * ```
 * ┌─────────┐      ①生成授权URL(PKCE)       ┌──────────────┐
 * │ Mobile  │ ─────────────────────────────→ │   北极星后端    │
 * │  App    │ ←───────────────────────────── │              │
 * └────┬────┘      ②返回authUrl+state        └──────────────┘
 *      │
 *      ▼ ③打开社交APP/浏览器
 * ┌──────────────┐
 * │ 抖音/小红书   │ ④用户扫码确认授权
 * │ 授权页面     │
 * └────┬─────────┘
 *      │
 *      ▼ ⑤Deep Link回调(携带code+state)
 * ┌─────────┐      ⑥发送code+state           ┌──────────────┐
 * │ Mobile  │ ─────────────────────────────→ │   北极星后端    │
 * │  App    │ ←───────────────────────────── │              │
 * └─────────┘      ⑦返回Token+用户信息         └──────────────┘
 * ```
 *
 * @author 北极星AI团队
 * @version 2.0 (2026-05-20)
 */
@RestController
@RequestMapping("/mobile/oauth")
@Slf4j
public class MobileOAuthController {

    @Autowired
    private OAuthService oauthService;

    @Autowired
    private OAuthConfig oauthConfig;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private TokenSecurityService tokenSecurityService;

    // ====== Redis Key前缀 ======
    private static final String PKCE_CACHE_PREFIX = "oauth:pkce:";
    private static final String STATE_CACHE_PREFIX = "oauth:state:";

    // ====== 回调地址配置 ======
    // 生产环境通过环境变量 OAUTH_CALLBACK_BASE_URL 注入
    // 默认使用北极星AI实际域名
    private static final String CALLBACK_BASE_URL = System.getenv().getOrDefault(
            "OAUTH_CALLBACK_BASE_URL",
            "https://api.beijixing-ai.com/oauth/callback"
    );

    /**
     * MOBILE-OAUTH-001: 生成移动端授权URL（含PKCE参数）
     *
     * 适用场景:
     * - 用户点击"绑定抖音账号"按钮时调用
     * - 返回完整的授权URL，移动端直接打开
     *
     * PKCE (Proof Key for Code Exchange) 安全机制:
     * - 防止授权码拦截攻击（Authorization Code Interception）
     * - 抖音开放平台强制要求使用PKCE
     * - 小红书推荐使用但非强制
     *
     * @param platform 平台代码: DOUYIN/XIAOHONGSHU/KUAISHOU
     * @param userId 当前登录用户ID（从JWT Token解析）
     * @param scopes 申请的权限列表（可选，默认使用平台推荐scope）
     * @return 授权URL + state + codeVerifier（移动端需保存codeVerifier用于后续验证）
     */
    @PostMapping("/authorize/{platform}")
    public ApiResponse<AuthorizationUrlResponse> generateAuthUrl(
            @PathVariable String platform,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) List<String> scopes) {

        log.info("📱 生成移动端授权URL: platform={}, userId={}", platform, userId);

        try {
            // 1. 校验平台是否支持
            if (!isPlatformSupported(platform)) {
                return ApiResponse.error("不支持的平台: " + platform);
            }

            // 2. 生成PKCE code_verifier + code_challenge
            String codeVerifier = generateCodeVerifier(); // S256随机数(43-128字符)
            String codeChallenge = computeS256Challenge(codeVerifier);

            log.debug("PKCE参数已生成: verifier长度={}, challenge={}",
                    codeVerifier.length(), codeChallenge.substring(0, 20) + "...");

            // 3. 生成state参数（防CSRF攻击）
            String state = UUID.randomUUID().toString().replace("-", "");

            // 4. 缓存state和code_verifier（有效期10分钟）
            cachePkceParameters(state, platform, codeVerifier, userId);

            // 5. 构建平台特定授权URL
            String authUrl = buildPlatformAuthUrl(platform, codeChallenge, state, scopes);

            AuthorizationUrlResponse response = new AuthorizationUrlResponse();
            response.setPlatform(platform);
            response.setAuthUrl(authUrl);
            response.setState(state);
            response.setCodeVerifier(codeVerifier);  // ⚠️ 移动端必须安全保存此值
            response.setExpiresIn(600);             // 10分钟有效期

            log.info("✅ 授权URL生成成功: platform={}, state={}", platform, state);

            return ApiResponse.success(response);

        } catch (Exception e) {
            log.error("❌ 生成授权URL失败: platform={}, error={}", platform, e.getMessage(), e);
            return ApiResponse.error("生成授权链接失败: " + e.getMessage());
        }
    }

    /**
     * MOBILE-OAUTH-002: 处理Deep Link OAuth回调
     *
     * 触发场景:
     * - Android: Intent Scheme回调（snssdk1128://oauth?code=xxx&state=yyy）
     * - iOS: Universal Links回调（https://yourapp.com/oauth/callback?code=xxx&state=yyy）
     *
     * 安全检查:
     * 1. 验证state参数（防CSRF）
     * 2. 如果是PKCE模式，验证code_challenge
     * 3. 检查code是否已被使用（防重放攻击）
     *
     * @param platform 平台代码
     * @param code 授权码（一次性，有效期10分钟）
     * @param state 状态参数（与请求时一致）
     * @param codeVerifier PKCE验证码（可选，如果平台要求PKCE）
     * @return Token对 + 用户基本信息
     */
    @GetMapping("/callback/{platform}")
    public ApiResponse<MobileOAuthSuccessResponse> handleCallback(
            @PathVariable String platform,
            @RequestParam String code,
            @RequestParam String state,
            @RequestParam(required = false) String codeVerifier) {

        log.info("🔄 收到OAuth回调: platform={}, code={}, state={}",
                platform, code.substring(0, Math.min(10, code.length())) + "...", state);

        try {
            // 1. 验证state参数（防CSRF攻击）
            validateState(state, platform);

            // 2. 获取缓存的PKCE参数
            PkceCache pkceCache = getPkceCache(state);
            if (pkceCache == null) {
                throw new RuntimeException("state参数无效或已过期");
            }

            // 3. 如果是PKCE模式，验证code_verifier
            if (codeVerifier != null && pkceCache.getCodeVerifier() != null) {
                if (!verifyPkce(codeVerifier, pkceCache.getCodeVerifier())) {
                    throw new RuntimeException("PKCE验证失败：code_verifier不匹配");
                }
                log.info("✅ PKCE验证通过");
            }

            // 4. 用authorization_code换取Token对
            SocialAccount account = oauthService.handleCallback(platform, code, state);
            if (account == null) {
                throw new RuntimeException("Token交换失败：请重新授权");
            }

            // 5. 加密存储Token（AES-256）
            tokenSecurityService.saveEncryptedTokens(
                    account.getId(),
                    account.getAccessToken(),
                    account.getRefreshToken(),
                    account.getTokenExpireTime() != null ?
                            (int) java.time.Duration.between(
                                    java.time.LocalDateTime.now(),
                                    account.getTokenExpireTime()
                            ).getSeconds() : 7200
            );

            // 6. 构建移动端响应（精简版，避免敏感信息泄露）
            MobileOAuthSuccessResponse response = new MobileOAuthSuccessResponse();
            response.setAccountId(account.getId());
            response.setPlatform(platform);
            response.setNickname(account.getNickname());
            response.setAvatar(account.getAvatarUrl());
            response.setOpenId(account.getAccountId());
            response.setScopes(getAuthorizedScopes(platform));
            response.setBindTime(LocalDateTime.now());

            log.info("🎉 OAuth授权成功: accountId={}, platform={}, nickname={}",
                    account.getId(), platform, account.getNickname());

            return ApiResponse.success(response);

        } catch (Exception e) {
            log.error("❌ OAuth回调处理失败: platform={}, error={}", platform, e.getMessage(), e);
            return ApiResponse.error("授权失败: " + e.getMessage());
        }
    }

    /**
     * MOBILE-OAUTH-003: 刷新Token（移动端调用）
     *
     * 触发时机:
     * - 收到401 Unauthorized错误时
     * - Token即将过期前主动刷新
     *
     * @param accountId 社交账号ID
     * @return 新的Token对
     */
    @PostMapping("/refresh/{accountId}")
    public ApiResponse<RefreshTokenResponse> refreshToken(@PathVariable Long accountId) {
        log.info("🔄 移动端请求刷新Token: accountId={}", accountId);

        try {
            boolean success = oauthService.refreshToken(oauthService.getAccountById(accountId));

            if (!success) {
                return ApiResponse.error("Token刷新失败，可能需要重新授权");
            }

            RefreshTokenResponse response = new RefreshTokenResponse();
            response.setAccountId(accountId);
            response.setRefreshedAt(java.time.LocalDateTime.now());
            response.setMessage("Token刷新成功");

            return ApiResponse.success(response);

        } catch (Exception e) {
            log.error("❌ Token刷新失败: accountId={}, error={}", accountId, e.getMessage(), e);
            return ApiResponse.error("Token刷新失败: " + e.getMessage());
        }
    }

    /**
     * MOBILE-OAUTH-004: 解除绑定社交账号
     *
     * @param accountId 社交账号ID
     * @return 操作结果
     */
    @DeleteMapping("/unbind/{accountId}")
    public ApiResponse<Void> unbindAccount(@PathVariable Long accountId) {
        log.info("🗑️ 移动端请求解绑账号: accountId={}", accountId);

        try {
            // 1. 销毁本地存储的Token
            tokenSecurityService.revokeToken(accountId);

            // 2. 调用平台revoke接口（可选）
            // oauthService.revokeToken(accountId);

            // 3. 更新数据库状态
            SocialAccount account = oauthService.getAccountById(accountId);
            if (account != null) {
                account.setStatus(0);  // 标记为已解绑
                oauthService.updateAccount(account);
            }

            log.info("✅ 账号解绑成功: accountId={}", accountId);
            return ApiResponse.success(null, "解绑成功");

        } catch (Exception e) {
            log.error("❌ 账号解绑失败: accountId={}, error={}", accountId, e.getMessage(), e);
            return ApiResponse.error("解绑失败: " + e.getMessage());
        }
    }

    /**
     * MOBILE-OAUTH-005: 获取已绑定账号列表
     *
     * @param userId 当前用户ID
     * @return 该用户绑定的所有社交账号
     */
    @GetMapping("/accounts")
    public ApiResponse<java.util.List<SocialAccount>> getBoundAccounts(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("📋 获取已绑定账号列表: userId={}", userId);

        try {
            List<SocialAccount> accounts = oauthService.getAccountsByUserId(userId);
            return ApiResponse.success(accounts);
        } catch (Exception e) {
            log.error("❌ 获取账号列表失败: error={}", e.getMessage(), e);
            return ApiResponse.error("获取失败: " + e.getMessage());
        }
    }

    // ============================================================
    // 内部方法：PKCE实现
    // ============================================================

    /**
     * 生成PKCE code_verifier
     *
     * 规范要求:
     * - 长度: 43-128字符
     * - 字符集: [A-Z] / [a-z] / [0-9] / "-" / "." / "_" / "~"
     * - 生成方式: 密码学安全的随机数生成器（CSPRNG）
     *
     * 参考文档: RFC 7636 - Proof Key for Code Exchange by OAuth Public Clients
     */
    private String generateCodeVerifier() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[32];  // 256位随机数
        secureRandom.nextBytes(randomBytes);

        // Base64 URL编码（无填充），确保符合字符集要求
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * 计算S256 code_challenge
     *
     * 公式: code_challenge = BASE64URL(SHA256(code_verifier))
     *
     * @param codeVerifier 原始verifier
     * @return challenge字符串
     */
    private String computeS256Challenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }

    /**
     * 验证PKCE code_verifier
     */
    private boolean verifyPkce(String receivedVerifier, String storedVerifier) {
        if (receivedVerifier == null || storedVerifier == null) return false;

        // 时间恒定比较，防止时序攻击
        if (receivedVerifier.length() != storedVerifier.length()) return false;

        int result = 0;
        for (int i = 0; i < receivedVerifier.length(); i++) {
            result |= receivedVerifier.charAt(i) ^ storedVerifier.charAt(i);
        }
        return result == 0;
    }

    // ============================================================
    // 内部方法：缓存管理
    // ============================================================

    /**
     * 缓存PKCE参数到Redis
     */
    private void cachePkceParameters(String state, String platform,
                                     String codeVerifier, Long userId) {
        String key = STATE_CACHE_PREFIX + state;
        String value = platform + ":" + userId + ":" + codeVerifier;

        redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(10));

        log.debug("PKCE参数已缓存: key={}, platform={}", key, platform);
    }

    /**
     * 从Redis获取缓存的PKCE参数
     */
    private PkceCache getPkceCache(String state) {
        String key = STATE_CACHE_PREFIX + state;
        String cachedValue = redisTemplate.opsForValue().get(key);

        if (cachedValue == null) return null;

        String[] parts = cachedValue.split(":");
        if (parts.length >= 3) {
            PkceCache cache = new PkceCache();
            cache.setPlatform(parts[0]);
            cache.setUserId(Long.parseLong(parts[1]));
            cache.setCodeVerifier(parts[2]);
            return cache;
        }

        return null;
    }

    /**
     * 验证state参数（防CSRF攻击）
     */
    private void validateState(String state, String expectedPlatform) {
        PkceCache cache = getPkceCache(state);
        if (cache == null) {
            throw new RuntimeException("state参数无效或已过期（可能被篡改）");
        }

        if (!expectedPlatform.equals(cache.getPlatform())) {
            throw new RuntimeException("平台不匹配（可能遭受CSRF攻击）");
        }

        // 使用后立即删除state（一次性使用）
        redisTemplate.delete(STATE_CACHE_PREFIX + state);
    }

    // ============================================================
    // 内部方法：URL构建
    // ============================================================

    /**
     * 构建平台特定的授权URL
     */
    private String buildPlatformAuthUrl(String platform, String codeChallenge,
                                        String state, List<String> scopes) {
        OAuthConfig.PlatformConfig config = getPlatformConfig(platform);
        if (config == null) {
            throw new RuntimeException("未找到平台配置: " + platform);
        }

        StringBuilder url = new StringBuilder(config.getAuthorizeUrl());
        url.append("?client_id=").append(config.getAppKey());
        url.append("&response_type=code");
        url.append("&redirect_uri=").append(CALLBACK_BASE_URL + "/" + platform.toLowerCase());
        url.append("&state=").append(state);

        // PKCE参数
        url.append("&code_challenge=").append(codeChallenge);
        url.append("&code_challenge_method=S256");

        // Scope参数
        if (scopes != null && !scopes.isEmpty()) {
            url.append("&scope=").append(String.join(",", scopes));
        } else {
            // 使用平台推荐的默认scope
            url.append("&scope=").append(getDefaultScope(platform));
        }

        return url.toString();
    }

    /**
     * 获取平台配置
     */
    private OAuthConfig.PlatformConfig getPlatformConfig(String platform) {
        switch (platform.toUpperCase()) {
            case "DOUYIN":
                return oauthConfig.getDouyin();
            case "XIAOHONGSHU":
                return oauthConfig.getXiaohongshu();
            case "KUAISHOU":
                return oauthConfig.getKuaishou();
            default:
                return null;
        }
    }

    /**
     * 检查平台是否受支持
     */
    private boolean isPlatformSupported(String platform) {
        return getPlatformConfig(platform) != null;
    }

    /**
     * 获取平台推荐的默认权限范围
     */
    private String getDefaultScope(String platform) {
        switch (platform.toUpperCase()) {
            case "DOUYIN":
                return "user_info,item_review,im.chat.create";
            case "XIAOHONGSHU":
                return "basic_info,item_review,im.message.send";
            case "KUAISHOU":
                return "user_info,comment_read,im.message.send";
            default:
                return "user_info";
        }
    }

    /**
     * 获取已授权的scope列表（用于展示给用户）
     */
    private List<String> getAuthorizedScopes(String platform) {
        String scopeStr = getDefaultScope(platform);
        return java.util.Arrays.asList(scopeStr.split(","));
    }

    // ============================================================
    // 数据模型
    // ============================================================

    @Data
    public static class AuthorizationUrlResponse {
        private String platform;          // 平台代码
        private String authUrl;           // 完整的授权URL（移动端直接打开）
        private String state;             // 状态参数（回调时原样返回）
        private String codeVerifier;      // PKCE验证码（⚠️ 移动端必须安全保存）
        private int expiresIn;            // 授权URL有效期（秒）
    }

    @Data
    public static class MobileOAuthSuccessResponse {
        private Long accountId;           // 社交账号ID（北极星系统内）
        private String platform;          // 平台代码
        private String nickname;          // 社交平台用户昵称
        private String avatar;            // 头像URL
        private String openId;            // 社交平台OpenID
        private List<String> scopes;      // 已授权的权限列表
        private LocalDateTime bindTime;   // 绑定时间
    }

    @Data
    public static class RefreshTokenResponse {
        private Long accountId;
        private LocalDateTime refreshedAt;
        private String message;
    }

    @Data
    private static class PkceCache {
        private String platform;
        private Long userId;
        private String codeVerifier;
    }
}
