package com.beijixing.social.service;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.beijixing.social.config.OAuthConfig;
import com.beijixing.social.entity.SocialAccount;
import com.beijixing.social.entity.AccountLog;
import com.beijixing.social.mapper.AccountLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * OAuth授权服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final OAuthConfig oauthConfig;
    private final AccountService accountService;
    private final AccountLogMapper logMapper;
    private final StringRedisTemplate redisTemplate;

    private static final String STATE_CACHE_PREFIX = "social:state:";

    /** 生成授权URL */
    public String generateAuthUrl(String platformCode, String redirectUri, String state) {
        OAuthConfig.PlatformConfig config = oauthConfig.getPlatformConfig(platformCode);
        if (config == null) {
            throw new IllegalArgumentException("不支持的平台: " + platformCode);
        }

        // 生成state并缓存
        String uuid = state != null ? state : UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(STATE_CACHE_PREFIX + uuid, platformCode, Duration.ofMinutes(10));

        Map<String, Object> params = new HashMap<>();
        params.put("client_key", config.getAppKey() != null ? config.getAppKey() : config.getAppId());
        params.put("redirect_uri", redirectUri);
        params.put("response_type", "code");
        params.put("scope", "user_info,video_list");
        params.put("state", uuid);

        String authUrl = config.getAuthorizeUrl() + "?" + HttpUtil.toParams(params);
        log.info("生成授权URL: {}", authUrl);
        return authUrl;
    }

    /** 处理授权回调 */
    public SocialAccount handleCallback(String platformCode, String code, String state) {
        OAuthConfig.PlatformConfig config = oauthConfig.getPlatformConfig(platformCode);
        if (config == null) {
            throw new IllegalArgumentException("不支持的平台: " + platformCode);
        }

        // 验证state
        String cachedPlatform = redisTemplate.opsForValue().get(STATE_CACHE_PREFIX + state);
        if (cachedPlatform != null && !cachedPlatform.equals(platformCode)) {
            throw new IllegalArgumentException("state验证失败");
        }

        // 获取Access Token
        Map<String, Object> tokenParams = new HashMap<>();
        tokenParams.put("client_key", config.getAppKey() != null ? config.getAppKey() : config.getAppId());
        tokenParams.put("client_secret", config.getAppSecret());
        tokenParams.put("code", code);
        tokenParams.put("grant_type", "authorization_code");

        try {
            String tokenResponse = HttpUtil.post(config.getTokenUrl(), tokenParams);
            JSONObject tokenJson = JSON.parseObject(tokenResponse);

            if (tokenJson.containsKey("error")) {
                throw new RuntimeException("获取Token失败: " + tokenJson.getString("error_description"));
            }

            String accessToken = tokenJson.getString("access_token");
            String refreshToken = tokenJson.getString("refresh_token");
            Long expiresIn = tokenJson.getLong("expires_in");
            Long refreshExpiresIn = tokenJson.getLong("refresh_expires_in");

            // 获取用户信息
            Map<String, Object> userParams = new HashMap<>();
            userParams.put("access_token", accessToken);
            String userResponse = HttpUtil.get(config.getUserInfoUrl(), userParams);
            JSONObject userJson = JSON.parseObject(userResponse);

            String openId = userJson.getString("openid");
            if (openId == null) openId = userJson.getString("unionid");
            String nickname = userJson.getString("nickname");
            String avatar = userJson.getString("avatar");

            // 保存或更新账号
            SocialAccount account = accountService.getSocialAccountByAccountId(platformCode, openId);
            if (account == null) {
                account = new SocialAccount();
                account.setAccountId(openId);
                account.setPlatformCode(platformCode);
                account.setNickname(nickname);
                account.setAvatarUrl(avatar);
                account.setStatus(1);
            }
            account.setAccessToken(accessToken);
            account.setRefreshToken(refreshToken);
            account.setTokenExpireTime(LocalDateTime.now().plusSeconds(expiresIn != null ? expiresIn : 7200));
            if (refreshExpiresIn != null) {
                account.setRefreshExpireTime(LocalDateTime.now().plusSeconds(refreshExpiresIn));
            }
            accountService.saveOrUpdate(account);

            // 记录日志
            saveLog(account.getId(), platformCode, "AUTH", "OAuth授权成功", null, "SUCCESS", null);

            // 清理state
            redisTemplate.delete(STATE_CACHE_PREFIX + state);

            log.info("OAuth授权成功: platform={}, openId={}", platformCode, openId);
            return account;

        } catch (Exception e) {
            log.error("OAuth授权失败: {}", e.getMessage(), e);
            throw new RuntimeException("OAuth授权失败: " + e.getMessage());
        }
    }

    /** 刷新Token */
    public boolean refreshToken(SocialAccount account) {
        OAuthConfig.PlatformConfig config = oauthConfig.getPlatformConfig(account.getPlatformCode());
        if (config == null || config.getRefreshUrl() == null) {
            log.warn("平台不支持Token刷新: {}", account.getPlatformCode());
            return false;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("client_key", config.getAppKey() != null ? config.getAppKey() : config.getAppId());
        params.put("refresh_token", account.getRefreshToken());
        params.put("grant_type", "refresh_token");

        try {
            String response = HttpUtil.post(config.getRefreshUrl(), params);
            JSONObject json = JSON.parseObject(response);

            if (json.containsKey("error")) {
                log.error("刷新Token失败: {}", json.getString("error_description"));
                return false;
            }

            String newAccessToken = json.getString("access_token");
            String newRefreshToken = json.getString("refresh_token");
            Long expiresIn = json.getLong("expires_in");

            accountService.updateToken(
                account.getId(),
                newAccessToken,
                newRefreshToken,
                LocalDateTime.now().plusSeconds(expiresIn != null ? expiresIn : 7200),
                LocalDateTime.now().plusSeconds(30 * 24 * 3600) // 默认30天
            );

            saveLog(account.getId(), account.getPlatformCode(), "REFRESH_TOKEN", "Token刷新成功", null, "SUCCESS", null);
            log.info("Token刷新成功: accountId={}", account.getId());
            return true;

        } catch (Exception e) {
            log.error("刷新Token异常: {}", e.getMessage());
            saveLog(account.getId(), account.getPlatformCode(), "REFRESH_TOKEN", "Token刷新失败", null, "FAIL", e.getMessage());
            return false;
        }
    }

    /** 批量刷新即将过期的Token */
    public void batchRefreshTokens() {
        accountService.selectExpiringAccounts(7).forEach(account -> {
            try {
                refreshToken(account);
            } catch (Exception e) {
                log.error("批量刷新Token失败: accountId={}", account.getId(), e);
            }
        });
    }

    private void saveLog(Long accountId, String platformCode, String actionType, String actionDesc, 
                         String targetId, String result, String failReason) {
        AccountLog log = new AccountLog();
        log.setAccountId(accountId);
        log.setPlatformCode(platformCode);
        log.setActionType(actionType);
        log.setActionDesc(actionDesc);
        log.setTargetId(targetId);
        log.setResult(result);
        log.setFailReason(failReason);
        log.setCreateTime(LocalDateTime.now());
        logMapper.insert(log);
    }

    /**
     * 根据用户ID获取社交账号列表
     */
    public List<SocialAccount> getAccountsByUserId(Long userId) {
        return accountService.getAccountsByUserId(userId);
    }

    /**
     * 根据ID获取社交账号
     */
    public SocialAccount getAccountById(Long accountId) {
        return accountService.getById(accountId);
    }

    /**
     * 更新社交账号
     */
    public void updateAccount(SocialAccount account) {
        accountService.updateById(account);
    }
}
