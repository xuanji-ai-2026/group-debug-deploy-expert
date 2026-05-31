package com.beijixing.social.controller;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MobileOAuthController 单元测试
 *
 * 测试覆盖范围:
 * 1. 生成授权URL（PKCE参数生成）
 * 2. 处理OAuth回调（State验证 + Code交换）
 * 3. Token刷新接口
 * 4. 账号绑定/解绑
 * 5. 已绑定账号列表查询
 * 6. 安全性测试（CSRF防护、参数校验）
 *
 * @author 北极星AI团队
 * @version 2.0 (2026-05-20)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MobileOAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final Long TEST_USER_ID = 10001L;

    // ============================================================
    // 1. 生成授权URL测试（PKCE流程）
    // ============================================================

    @Test
    @Order(1)
    @DisplayName("测试1.1: 生成抖音授权URL - 基本流程")
    void testGenerateAuthUrl_Douyin() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/mobile/oauth/authorize/DOUYIN")
                                .header("X-User-Id", TEST_USER_ID)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.platform").value("DOUYIN"))
                .andExpect(jsonPath("$.data.authUrl").exists())
                .andExpect(jsonPath("$.data.state").exists())
                .andExpect(jsonPath("$.data.codeVerifier").exists())
                .andExpect(jsonPath("$.data.expiresIn").value(600))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        System.out.println("\n===== 抖音授权URL生成响应 =====");
        System.out.println(responseContent);
        System.out.println("================================\n");

        assertTrue(responseContent.contains("open.douyin.com"),
                "授权URL应包含抖音开放平台域名");
    }

    @Test
    @Order(2)
    @DisplayName("测试1.2: 生成小红书授权URL")
    void testGenerateAuthUrl_Xiaohongshu() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/mobile/oauth/authorize/XIAOHONGSHU")
                                .header("X-User-Id", TEST_USER_ID)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.platform").value("XIAOHONGSHU"))
                .andExpect(jsonPath("$.data.authUrl").exists())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        System.out.println("\n===== 小红书授权URL生成响应 =====");
        System.out.println(responseContent);
        System.out.println("==================================\n");

        assertTrue(responseContent.contains("open.xiaohongshu.com") ||
                        responseContent.contains("xiaohongshu.com"),
                "授权URL应包含小红书开放平台域名");
    }

    @Test
    @Order(3)
    @DisplayName("测试1.3: 生成快手授权URL")
    void testGenerateAuthUrl_Kuaishou() throws Exception {
        mockMvc.perform(
                        post("/api/mobile/oauth/authorize/KUAISHOU")
                                .header("X-User-Id", TEST_USER_ID)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.platform").value("KUAISHOU"))
                .andExpect(jsonPath("$.data.codeVerifier").isNotEmpty());

        System.out.println("✅ 快手授权URL生成成功");
    }

    @Test
    @Order(4)
    @DisplayName("测试1.4: 指定自定义Scope列表")
    void testGenerateAuthUrl_WithCustomScopes() throws Exception {
        String requestBody = "{\"scopes\": [\"user.info.basic\", \"video.comment.read\"]}";

        mockMvc.perform(
                        post("/api/mobile/oauth/authorize/DOUYIN")
                                .header("X-User-Id", TEST_USER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authUrl").exists());

        System.out.println("✅ 自定义Scope列表请求成功");
    }

    // ============================================================
    // 2. 安全性测试
    // ============================================================

    @Test
    @Order(10)
    @DisplayName("测试2.1: 缺少X-User-Id头信息应返回错误")
    void testMissingUserIdHeader() throws Exception {
        mockMvc.perform(
                        post("/api/mobile/oauth/authorize/DOUYIN")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(11)
    @DisplayName("测试2.2: 无效的平台代码应返回错误")
    void testInvalidPlatformCode() throws Exception {
        mockMvc.perform(
                        post("/api/mobile/oauth/authorize/INVALID_PLATFORM")
                                .header("X-User-Id", TEST_USER_ID)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @Order(12)
    @DisplayName("测试2.3: PKCE参数安全性验证")
    void testPKCESecurityParameters() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/mobile/oauth/authorize/DOUYIN")
                                .header("X-User-Id", TEST_USER_ID)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        org.json.JSONObject json = new org.json.JSONObject(content);
        org.json.JSONObject data = json.getJSONObject("data");

        String state = data.getString("state");
        String codeVerifier = data.getString("codeVerifier");
        String authUrl = data.getString("authUrl");

        System.out.println("\n===== PKCE安全参数验证 =====");
        System.out.println("State长度: " + state.length() + " (期望32)");
        System.out.println("Code Verifier长度: " + codeVerifier.length() + " (期望43-128)");
        System.out.println("Authorization URL包含code_challenge: " + authUrl.contains("code_challenge"));
        System.out.println("==============================\n");

        assertEquals(32, state.length(), "State应为32位UUID格式");
        assertTrue(codeVerifier.length() >= 43 && codeVerifier.length() <= 128,
                "Code Verifier应在43-128字符之间");
        assertTrue(authUrl.contains("code_challenge"),
                "授权URL应包含code_challenge参数");
        assertTrue(authUrl.contains(state),
                "授权URL应包含state参数用于CSRF防护");
    }

    @Test
    @Order(13)
    @DisplayName("测试2.4: 多次调用生成不同的PKCE参数")
    void testUniquePKCEPerRequest() throws Exception {
        MvcResult result1 = mockMvc.perform(
                        post("/api/mobile/oauth/authorize/DOUYIN")
                                .header("X-User-Id", TEST_USER_ID)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult result2 = mockMvc.perform(
                        post("/api/mobile/oauth/authorize/DOUYIN")
                                .header("X-User-Id", TEST_USER_ID)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        org.json.JSONObject json1 = new org.json.JSONObject(result1.getResponse().getContentAsString());
        org.json.JSONObject json2 = new org.json.JSONObject(result2.getResponse().getContentAsString());

        String state1 = json1.getJSONObject("data").getString("state");
        String state2 = json2.getJSONObject("data").getString("state");
        String verifier1 = json1.getJSONObject("data").getString("codeVerifier");
        String verifier2 = json2.getJSONObject("data").getString("codeVerifier");

        assertNotEquals(state1, state2,
                "每次请求应生成不同的state参数");
        assertNotEquals(verifier1, verifier2,
                "每次请求应生成不同的code_verifier");

        System.out.println("✅ PKCE参数唯一性验证通过");
    }

    // ============================================================
    // 3. OAuth回调处理测试
    // ============================================================

    @Test
    @Order(20)
    @DisplayName("测试3.1: 抖音回调处理 - 成功场景")
    void testHandleCallback_DouyinSuccess() throws Exception {
        String testCode = "test_authorization_code_" + UUID.randomUUID().toString().substring(0, 8);
        String testState = "test_state_" + UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(
                        get("/api/mobile/oauth/callback/DOUYIN")
                                .param("code", testCode)
                                .param("state", testState))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        System.out.println("\n===== 抖音回调成功处理 =====");
        System.out.println("Code: " + testCode);
        System.out.println("State: " + testState);
        System.out.println("=============================\n");
    }

    @Test
    @Order(21)
    @DisplayName("测试3.2: 小红书回调处理")
    void testHandleCallback_XiaohongshuSuccess() throws Exception {
        String testCode = "xhs_code_" + System.currentTimeMillis();
        String testState = "xhs_state_" + System.currentTimeMillis();

        mockMvc.perform(
                        get("/api/mobile/oauth/callback/XIAOHONGSHU")
                                .param("code", testCode)
                                .param("state", testState))
                .andExpect(status().isOk());

        System.out.println("✅ 小红书回调处理成功");
    }

    @Test
    @Order(22)
    @DisplayName("测试3.3: 回调缺少必要参数")
    void testCallback_MissingParameters() throws Exception {
        mockMvc.perform(
                        get("/api/mobile/oauth/callback/DOUYIN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(23)
    @DisplayName("测试3.4: State验证失败（模拟CSRF攻击）")
    void testCallback_InvalidState() throws Exception {
        String invalidState = "tampered_state_by_attacker";

        mockMvc.perform(
                        get("/api/mobile/oauth/callback/DOUYIN")
                                .param("code", "some_code")
                                .param("state", invalidState))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").exists());

        System.out.println("✅ CSRF攻击检测成功（无效State被拒绝）");
    }

    // ============================================================
    // 4. Token刷新接口测试
    // ============================================================

    @Test
    @Order(30)
    @DisplayName("测试4.1: 刷新Token - 正常流程")
    void testRefreshToken_Success() throws Exception {
        Long accountId = 77777L;

        mockMvc.perform(
                        post("/api/mobile/oauth/refresh/" + accountId)
                                .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isOk());

        System.out.println("✅ Token刷新接口正常响应");
    }

    @Test
    @Order(31)
    @DisplayName("测试4.2: 刷新不存在的账号ID")
    void testRefreshToken_NonExistentAccount() throws Exception {
        Long nonExistentId = 999999999L;

        mockMvc.perform(
                        post("/api/mobile/oauth/refresh/" + nonExistentId)
                                .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isNotFound());
    }

    // ============================================================
    // 5. 账号绑定管理测试
    // ============================================================

    @Test
    @Order(40)
    @DisplayName("测试5.1: 解除账号绑定")
    void testUnbindAccount() throws Exception {
        Long accountId = 66666L;

        mockMvc.perform(
                        delete("/api/mobile/oauth/unbind/" + accountId)
                                .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isOk());

        System.out.println("✅ 账号解绑接口正常响应");
    }

    @Test
    @Order(41)
    @DisplayName("测试5.2: 查询已绑定的社交账号列表")
    void testGetBoundAccounts() throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/api/mobile/oauth/accounts")
                                .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        System.out.println("\n===== 已绑定账号列表 =====");
        System.out.println(content);
        System.out.println("==========================\n");
    }

    // ============================================================
    // 6. HTTP方法安全测试
    // ============================================================

    @Test
    @Order(50)
    @DisplayName("测试6.1: 使用GET方法访问POST接口应返回405")
    void testHttpMethodSecurity_PostEndpoint() throws Exception {
        mockMvc.perform(
                        get("/api/mobile/oauth/authorize/DOUYIN")
                                .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @Order(51)
    @DisplayName("测试6.2: 使用POST方法访问GET接口应返回405")
    void testHttpMethodSecurity_GetEndpoint() throws Exception {
        mockMvc.perform(
                        post("/api/mobile/oauth/accounts")
                                .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @Order(52)
    @DisplayName("测试6.3: Content-Type校验")
    void testContentTypeValidation() throws Exception {
        mockMvc.perform(
                        post("/api/mobile/oauth/authorize/DOUYIN")
                                .header("X-User-Id", TEST_USER_ID)
                                .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().isUnsupportedMediaType());
    }

    // ============================================================
    // 7. 边界条件测试
    // ============================================================

    @Test
    @Order(60)
    @DisplayName("测试7.1: 极端长度的用户ID")
    void testExtremeUserIdLength() throws Exception {
        String longUserId = "1".repeat(1000);

        mockMvc.perform(
                        post("/api/mobile/oauth/authorize/DOUYIN")
                                .header("X-User-Id", longUserId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(61)
    @DisplayName("测试7.2: 特殊字符在URL路径中")
    void testSpecialCharactersInPath() throws Exception {
        mockMvc.perform(
                        post("/api/mobile/oauth/authorize/<script>alert('xss')</script>")
                                .header("X-User-Id", TEST_USER_ID)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
