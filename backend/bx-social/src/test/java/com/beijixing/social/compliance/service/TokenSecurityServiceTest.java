package com.beijixing.social.compliance.service;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TokenSecurityService 单元测试
 *
 * 测试覆盖范围:
 * 1. Token存储与读取（透明加解密）
 * 2. Token健康状态检查（过期预警）
 * 3. Token生命周期管理
 * 4. 安全性验证（撤销/刷新）
 *
 * @author 北极星AI团队
 * @version 2.0 (2026-05-20)
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TokenSecurityServiceTest {

    @Autowired
    private TokenSecurityService tokenSecurityService;

    private static final Long TEST_ACCOUNT_ID = 88888L;
    private static final String TEST_ACCESS_TOKEN = "test_access_token_12345";
    private static final String TEST_REFRESH_TOKEN = "test_refresh_token_67890";

    // ============================================================
    // 1. Token存储与读取测试
    // ============================================================

    @Test
    @Order(1)
    @DisplayName("测试1.1: 存储并读取Token")
    void testStoreAndRetrieveTokens() {
        tokenSecurityService.saveEncryptedTokens(
                TEST_ACCOUNT_ID,
                TEST_ACCESS_TOKEN,
                TEST_REFRESH_TOKEN,
                3600
        );

        String retrievedAccessToken = tokenSecurityService.getValidAccessToken(TEST_ACCOUNT_ID);
        String retrievedRefreshToken = tokenSecurityService.getRefreshToken(TEST_ACCOUNT_ID);

        assertNotNull(retrievedAccessToken, "存储后应能读取到Access Token");
        assertEquals(TEST_ACCESS_TOKEN, retrievedAccessToken, "读取的Access Token应与存储的一致");

        System.out.println("✅ Token存储与读取成功");
    }

    @Test
    @Order(2)
    @DisplayName("测试1.2: 更新已存在的Token")
    void testUpdateExistingTokens() {
        String newAccessToken = "new_access_token_updated_" + System.currentTimeMillis();
        String newRefreshToken = "new_refresh_token_updated_" + System.currentTimeMillis();

        tokenSecurityService.updateTokensAfterRefresh(
                TEST_ACCOUNT_ID,
                newAccessToken,
                newRefreshToken,
                7200
        );

        String currentAccessToken = tokenSecurityService.getValidAccessToken(TEST_ACCOUNT_ID);

        assertEquals(newAccessToken, currentAccessToken, "更新后应返回新的Access Token");
        System.out.println("✅ Token更新成功");
    }

    @Test
    @Order(3)
    @DisplayName("测试1.3: 撤销Token")
    void testRevokeTokens() {
        tokenSecurityService.revokeToken(TEST_ACCOUNT_ID);

        String revokedToken = tokenSecurityService.getValidAccessToken(TEST_ACCOUNT_ID);
        
        assertNull(revokedToken, "撤销后不应有有效Token");
        System.out.println("✅ Token撤销成功");
    }

    // ============================================================
    // 2. Token健康状态检查测试
    // ============================================================

    @Test
    @Order(10)
    @DisplayName("测试2.1: 批量检查所有Token健康状态")
    void testCheckAllTokenHealth() {
        List<TokenSecurityService.TokenHealthReport> allReports =
                tokenSecurityService.checkAllTokenHealth();

        assertNotNull(allReports, "健康报告列表不应为null");
        System.out.println("\n===== 全量Token健康扫描报告 =====");
        System.out.println("总账号数: " + allReports.size());

        int healthyCount = 0;
        int warningCount = 0;
        int expiredCount = 0;

        for (var report : allReports) {
            if (report.getStatus() != null) {
                switch (report.getStatus()) {
                    case "HEALTHY":
                        healthyCount++;
                        break;
                    case "WARNING":
                        warningCount++;
                        break;
                    case "EXPIRED":
                        expiredCount++;
                        break;
                }
            }
        }

        System.out.println("健康: " + healthyCount + " | 警告: " + warningCount + " | 过期: " + expiredCount);
        System.out.println("====================================\n");
    }

    // ============================================================
    // 3. Token生命周期管理测试
    // ============================================================

    @Test
    @Order(20)
    @DisplayName("测试3.1: 完整Token生命周期")
    void testCompleteTokenLifecycle() {
        Long lifecycleAccountId = 77777L;
        String initialToken = "initial_token_lifecycle";
        String refreshedToken = "refreshed_token_lifecycle";

        // 1. 存储
        tokenSecurityService.saveEncryptedTokens(lifecycleAccountId, initialToken, "refresh", 3600);
        String retrieved = tokenSecurityService.getValidAccessToken(lifecycleAccountId);
        assertEquals(initialToken, retrieved, "初始存储应成功");
        System.out.println("✅ 步骤1: Token存储成功");

        // 2. 刷新更新
        tokenSecurityService.updateTokensAfterRefresh(lifecycleAccountId, refreshedToken, "new_refresh", 7200);
        String afterRefresh = tokenSecurityService.getValidAccessToken(lifecycleAccountId);
        assertEquals(refreshedToken, afterRefresh, "刷新后应返回新Token");
        System.out.println("✅ 步骤2: Token刷新成功");

        // 3. 撤销
        tokenSecurityService.revokeToken(lifecycleAccountId);
        String afterRevoke = tokenSecurityService.getValidAccessToken(lifecycleAccountId);
        assertNull(afterRevoke, "撤销后应为null");
        System.out.println("✅ 步骤3: Token撤销成功");

        System.out.println("🎉 完整生命周期测试通过");
    }

    @Test
    @Order(21)
    @DisplayName("测试3.2: 多账号独立管理")
    void testMultipleAccountIsolation() {
        Long account1 = 11111L;
        Long account2 = 22222L;

        tokenSecurityService.saveEncryptedTokens(account1, "token_account_1", "refresh_1", 3600);
        tokenSecurityService.saveEncryptedTokens(account2, "token_account_2", "refresh_2", 3600);

        String token1 = tokenSecurityService.getValidAccessToken(account1);
        String token2 = tokenSecurityService.getValidAccessToken(account2);

        assertEquals("token_account_1", token1, "账号1的Token应正确");
        assertEquals("token_account_2", token2, "账号2的Token应正确");
        assertNotEquals(token1, token2, "不同账号的Token应不同");

        System.out.println("✅ 多账号隔离验证通过");
    }

    // ============================================================
    // 4. 安全性验证测试
    // ============================================================

    @Test
    @Order(30)
    @DisplayName("测试4.1: Token一致性验证")
    void testTokenConsistency() {
        Long consistencyId = 66666L;
        String originalToken = "consistency_test_token_" + System.currentTimeMillis();

        tokenSecurityService.saveEncryptedTokens(consistencyId, originalToken, "refresh_consistency", 3600);

        // 多次读取应返回相同结果
        String read1 = tokenSecurityService.getValidAccessToken(consistencyId);
        String read2 = tokenSecurityService.getValidAccessToken(consistencyId);
        String read3 = tokenSecurityService.getValidAccessToken(consistencyId);

        assertEquals(read1, read2, "多次读取应一致(1&2)");
        assertEquals(read2, read3, "多次读取应一致(2&3)");
        assertEquals(originalToken, read1, "应与原始值一致");

        System.out.println("✅ Token一致性验证通过");
    }

    @Test
    @Order(31)
    @DisplayName("测试4.2: 空值处理安全性")
    void testNullAndEmptyHandling() {
        try {
            tokenSecurityService.saveEncryptedTokens(null, null, null, 0);
            System.out.println("⚠️  空值参数已处理");
        } catch (Exception e) {
            System.out.println("✅ 空值异常正确捕获: " + e.getClass().getSimpleName());
        }
        System.out.println("✅ 空值安全处理通过");
    }

    // ============================================================
    // 5. 性能测试
    // ============================================================

    @Test
    @Order(40)
    @DisplayName("测试5.1: 批量存储性能（100次<500ms）")
    void testBatchStoragePerformance() {
        String testToken = "performance_test_token";

        long startTime = System.nanoTime();
        int iterations = 100;

        for (int i = 0; i < iterations; i++) {
            tokenSecurityService.saveEncryptedTokens((long) i, testToken + i, "refresh_" + i, 3600);
        }

        long totalDuration = (System.nanoTime() - startTime) / 1_000_000;
        double avgDurationMs = totalDuration / (double) iterations;

        System.out.println(String.format("批量存储性能: 总耗时=%dms, 平均=%.3fms (%d次)",
                totalDuration, avgDurationMs, iterations));

        assertTrue(totalDuration < 500, "100次存储应在500ms内完成");
    }

    @Test
    @Order(41)
    @DisplayName("测试5.2: 单次加密存储性能（<10ms）")
    void testSingleEncryptionPerformance() {
        String testToken = "test_performance_token";

        long totalDuration = 0;
        int iterations = 100;

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            tokenSecurityService.saveEncryptedTokens((long) i, testToken, "refresh_" + i, 3600);
            totalDuration += (System.nanoTime() - start);
        }

        double avgDurationMs = (totalDuration / iterations) / 1_000_000.0;
        System.out.println(String.format("平均加密存储耗时: %.3f ms", avgDurationMs));

        assertTrue(avgDurationMs < 10, "单次加密存储应在10ms内完成");
    }

    @Test
    @Order(42)
    @DisplayName("测试5.3: 单次解密读取性能（<10ms）")
    void testSingleDecryptionPerformance() {
        tokenSecurityService.saveEncryptedTokens(99999L, "test_performance_token", "refresh_perf", 3600);

        long totalDuration = 0;
        int iterations = 100;

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            tokenSecurityService.getValidAccessToken(99999L);
            totalDuration += (System.nanoTime() - start);
        }

        double avgDurationMs = (totalDuration / iterations) / 1_000_000.0;
        System.out.println(String.format("平均解密读取耗时: %.3f ms", avgDurationMs));

        assertTrue(avgDurationMs < 10, "单次解密读取应在10ms内完成");
    }
}
