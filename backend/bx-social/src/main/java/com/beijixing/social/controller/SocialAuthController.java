package com.beijixing.social.controller;

import com.beijixing.social.entity.SocialAccount;
import com.beijixing.social.service.OAuthService;
import com.beijixing.social.service.AccountService;
import com.beijixing.social.vo.ApiResponse;
import com.beijixing.social.vo.OAuthRequestVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 社交平台OAuth授权控制器
 *
 * 原名: AuthController (已重命名避免与bx-user.AuthController冲突)
 * 功能: 抖音/小红书等平台的OAuth2.0授权流程
 * 最后更新: 2026-05-20 (极简单体版)
 */
@Slf4j
@RestController
@RequestMapping("/social/auth")
@RequiredArgsConstructor
public class SocialAuthController {

    private final OAuthService oauthService;
    private final AccountService accountService;

    /** 生成授权URL */
    @PostMapping("/authorize/url")
    public ApiResponse<String> generateAuthUrl(@RequestBody @Valid OAuthRequestVO request) {
        try {
            String authUrl = oauthService.generateAuthUrl(
                    request.getPlatformCode(),
                    request.getRedirectUri(),
                    request.getState()
            );
            return ApiResponse.success(authUrl);
        } catch (Exception e) {
            log.error("生成授权URL失败: {}", e.getMessage());
            return ApiResponse.fail("生成授权URL失败: " + e.getMessage());
        }
    }

    /** OAuth回调处理 */
    @GetMapping("/callback")
    public ApiResponse<SocialAccount> callback(
            @RequestParam String platformCode,
            @RequestParam String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description) {
        try {
            if (error != null) {
                return ApiResponse.fail("授权失败: " + error_description);
            }
            SocialAccount account = oauthService.handleCallback(platformCode, code, state);
            return ApiResponse.success("授权成功", account);
        } catch (Exception e) {
            log.error("OAuth回调处理失败: {}", e.getMessage());
            return ApiResponse.fail("授权处理失败: " + e.getMessage());
        }
    }

    /** 刷新Token */
    @PostMapping("/refresh/{accountId}")
    public ApiResponse<String> refreshToken(@PathVariable Long accountId) {
        SocialAccount account = accountService.getById(accountId);
        if (account == null) {
            return ApiResponse.fail("账号不存在");
        }
        boolean result = oauthService.refreshToken(account);
        return result ? ApiResponse.success("Token刷新成功") : ApiResponse.fail("Token刷新失败");
    }

    /** 批量刷新即将过期的Token */
    @PostMapping("/refresh/batch")
    public ApiResponse<String> batchRefreshTokens() {
        oauthService.batchRefreshTokens();
        return ApiResponse.success("批量刷新任务已启动");
    }

    /** 获取即将过期的账号 */
    @GetMapping("/expiring")
    public ApiResponse<List<SocialAccount>> getExpiringAccounts(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success(accountService.selectExpiringAccounts(days));
    }
}