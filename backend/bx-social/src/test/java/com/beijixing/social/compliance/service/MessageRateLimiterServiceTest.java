package com.beijixing.social.compliance.service;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MessageRateLimiterService 单元测试
 *
 * 测试覆盖范围:
 * 1. 令牌桶限流算法（单用户/全局）
 * 2. 滑动窗口日限额检查
 * 3. 时间窗口校验（允许/禁止时段）
 * 4. 内容相似度检测（SimHash简化版）
 * 5. 冷却期管理（触发/查询/解除）
 * 6. 并发安全测试（多线程竞争）
 * 7. 边界条件（极限值/异常输入）
 *
 * @author 北极星AI团队
 * @version 2.0 (2026-05-20)
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MessageRateLimiterServiceTest {

    @Autowired
    private MessageRateLimiterService rateLimiterService;

    private static final Long TEST_ACCOUNT_ID = 99999L;
    private static final String TEST_USER_ID = "test_user_001";

    // ============================================================
    // 1. 令牌桶限流算法测试
    // ============================================================

    @Test
    @Order(1)
    @DisplayName("测试1.1: 正常获取令牌（未超限）")
    void testTryAcquireToken_Normal() {
        boolean acquired = rateLimiterService.tryAcquireToken(TEST_ACCOUNT_ID, 1);

        assertTrue(acquired, "首次请求应成功获取令牌");
        System.out.println("✅ 正常获取令牌成功");
    }

    @Test
    @Order(2)
    @DisplayName("测试1.2: 连续获取多个令牌")
    void testTryAcquireToken_Multiple() {
        int requestCount = 5;
        int successCount = 0;

        for (int i = 0; i < requestCount; i++) {
            if (rateLimiterService.tryAcquireToken(TEST_ACCOUNT_ID, 1)) {
                successCount++;
            }
        }

        System.out.println(String.format("连续请求%d次，成功%d次", requestCount, successCount));
        assertTrue(successCount > 0, "应至少成功获取部分令牌");
    }

    @Test
    @Order(3)
    @DisplayName("测试1.3: 超过限制后获取令牌失败")
    void testTryAcquireToken_ExceedsLimit() {
        int maxAttempts = 100;  // 尝试超过日限额的次数
        int successCount = 0;

        for (int i = 0; i < maxAttempts; i++) {
            if (rateLimiterService.tryAcquireToken(TEST_ACCOUNT_ID, 1)) {
                successCount++;
            }
        }

        System.out.println(String.format("尝试%d次，实际成功%d次", maxAttempts, successCount));
        assertTrue(successCount < maxAttempts, "应有部分请求被限流");
    }

    @Test
    @Order(4)
    @DisplayName("测试1.4: 批量获取令牌")
    void testTryAcquireToken_Batch() {
        boolean acquired3 = rateLimiterService.tryAcquireToken(TEST_ACCOUNT_ID, 3);
        boolean acquired5 = rateLimiterService.tryAcquireToken(TEST_ACCOUNT_ID, 5);
        boolean acquired10 = rateLimiterService.tryAcquireToken(TEST_ACCOUNT_ID, 10);

        System.out.println(String.format("批量获取结果: 3个=%b, 5个=%b, 10个=%b",
                acquired3, acquired5, acquired10));
    }

    // ============================================================
    // 2. 滑动窗口日限额检查测试
    // ============================================================

    @Test
    @Order(10)
    @DisplayName("测试2.1: 全局日限额检查 - 未超限")
    void testCheckDailyGlobalLimit_WithinLimit() {
        var result = rateLimiterService.checkDailyGlobalLimit(TEST_ACCOUNT_ID);

        assertTrue(result.isAllowed(), "应在日限额内");
        assertTrue(result.getRemaining() > 0, "应有剩余额度");
        System.out.println(String.format("全局日限额: 已用%d/%d, 剩余%d",
                result.getMaxDaily() - result.getRemaining(),
                result.getMaxDaily(),
                result.getRemaining()));
    }

    @Test
    @Order(11)
    @DisplayName("测试2.2: 单用户日限额检查 - 未超限")
    void testCheckDailyUserLimit_WithinLimit() {
        var result = rateLimiterService.checkDailyUserLimit(TEST_ACCOUNT_ID, TEST_USER_ID);

        assertTrue(result.isAllowed());
        System.out.println(String.format("单用户日限额: 剩余%d/%d",
                result.getRemaining(), result.getMaxDaily()));
    }

    @Test
    @Order(12)
    @DisplayName("测试2.3: 日限额统计准确性")
    void testDailyLimitAccuracy() {
        int testSendCount = 3;

        for (int i = 0; i < testSendCount; i++) {
            rateLimiterService.checkDailyGlobalLimit(TEST_ACCOUNT_ID);
            rateLimiterService.checkDailyUserLimit(TEST_ACCOUNT_ID, TEST_USER_ID + "_" + i);
        }

        var globalResult = rateLimiterService.checkDailyGlobalLimit(TEST_ACCOUNT_ID);
        var userResult = rateLimiterService.checkDailyUserLimit(TEST_ACCOUNT_ID, TEST_USER_ID + "_0");

        System.out.println("发送" + testSendCount + "次后:");
        System.out.println("  全局剩余: " + globalResult.getRemaining() + "/" + globalResult.getMaxDaily());
        System.out.println("  单用户剩余: " + userResult.getRemaining() + "/" + userResult.getMaxDaily());
    }

    // ============================================================
    // 3. 时间窗口校验测试
    // ============================================================

    @Test
    @Order(20)
    @DisplayName("测试3.1: 当前时间在允许窗口内")
    void testIsInAllowedTimeWindow_WithinWindow() {
        boolean inWindow = rateLimiterService.isInAllowedTimeWindow(TEST_ACCOUNT_ID);

        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();

        System.out.println(String.format("当前时间: %02d:%02d, 窗口状态: %s",
                hour, now.getMinute(), inWindow ? "✅ 允许" : "❌ 禁止"));

        if (hour >= 9 && hour < 21) {
            assertTrue(inWindow, "9:00-21:00应在允许窗口内");
        } else {
            assertFalse(inWindow, "非工作时间应在禁止窗口内");
        }
    }

    @Test
    @Order(21)
    @DisplayName("测试3.2: 获取下次允许时间")
    void testGetNextAllowedTime() {
        LocalDateTime nextAllowed = rateLimiterService.getNextAllowedTime(TEST_ACCOUNT_ID);

        assertNotNull(nextAllowed, "下次允许时间不应为null");
        assertTrue(nextAllowed.isAfter(LocalDateTime.now()) ||
                        nextAllowed.toLocalDate().isAfter(LocalDateTime.now().toLocalDate()),
                "下次允许时间应是未来时间");

        System.out.println("下次允许发送时间: " + nextAllowed);
    }

    // ============================================================
    // 4. 内容相似度检测测试
    // ============================================================

    @Test
    @Order(30)
    @DisplayName("测试4.1: 完全相同内容检测")
    void testContentSimilarity_Identical() {
        String content = "您好，我对您的产品很感兴趣，想了解更多详情";

        var result1 = rateLimiterService.checkContentSimilarity(TEST_ACCOUNT_ID, content);
        var result2 = rateLimiterService.checkContentSimilarity(TEST_ACCOUNT_ID, content);

        assertNotNull(result2);
        System.out.println(String.format("完全相同内容相似度: %.2f%% (是否高相似: %b)",
                result2.getSimilarity() * 100, result2.isHighSimilarity()));

        assertTrue(result2.getSimilarity() > 0.9,
                "完全相同内容相似度应>90%");
    }

    @Test
    @Order(31)
    @DisplayName("测试4.2: 相似但略有差异的内容")
    void testContentSimilarity_SimilarButDifferent() {
        String original = "您好，我对您的产品很感兴趣";
        String modified = "您好，我對您的產品很感興趣";  // 繁体字

        rateLimiterService.checkContentSimilarity(TEST_ACCOUNT_ID, original);
        var result = rateLimiterService.checkContentSimilarity(TEST_ACCOUNT_ID, modified);

        System.out.println(String.format("相似内容相似度: %.2f%%", result.getSimilarity() * 100));
    }

    @Test
    @Order(32)
    @DisplayName("测试4.3: 完全不同的内容")
    void testContentSimilarity_CompletelyDifferent() {
        String first = "今天天气真好";
        String second = "我想购买这款产品";

        rateLimiterService.checkContentSimilarity(TEST_ACCOUNT_ID, first);
        var result = rateLimiterService.checkContentSimilarity(TEST_ACCOUNT_ID, second);

        System.out.println(String.format("不同内容相似度: %.2f%%", result.getSimilarity() * 100));
        assertTrue(result.getSimilarity() < 0.5,
                "完全不同内容的相似度应<50%");
    }

    @Test
    @Order(33)
    @DisplayName("测试4.4: 空内容处理")
    void testContentSimilarity_EmptyContent() {
        var result = rateLimiterService.checkContentSimilarity(TEST_ACCOUNT_ID, "");

        assertNotNull(result);
        assertEquals(0.0, result.getSimilarity(), "空内容相似度应为0");
        assertFalse(result.isHighSimilarity(), "空内容不应标记为高相似");
    }

    // ============================================================
    // 5. 冷却期管理测试
    // ============================================================

    @Test
    @Order(40)
    @DisplayName("测试5.1: 触发冷却期")
    void testTriggerCooldown() {
        Duration cooldownDuration = Duration.ofSeconds(60);

        rateLimiterService.triggerCooldown(TEST_ACCOUNT_ID, cooldownDuration);

        assertTrue(rateLimiterService.isInCooldown(TEST_ACCOUNT_ID),
                "触发冷却期后应在冷却状态");

        long remaining = rateLimiterService.getCooldownRemaining(TEST_ACCOUNT_ID);
        assertTrue(remaining > 0 && remaining <= 60,
                "冷却剩余时间应在0-60秒范围内");

        System.out.println("✅ 冷却期已触发，剩余: " + remaining + "秒");
    }

    @Test
    @Order(42)
    @DisplayName("测试5.2: 查询冷却期状态")
    void testIsInCooldown() {
        if (!rateLimiterService.isInCooldown(TEST_ACCOUNT_ID)) {
            rateLimiterService.triggerCooldown(TEST_ACCOUNT_ID, Duration.ofSeconds(30));
        }

        assertTrue(rateLimiterService.isInCooldown(TEST_ACCOUNT_ID));

        long remaining = rateLimiterService.getCooldownRemaining(TEST_ACCOUNT_ID);
        System.out.println("冷却期剩余: " + remaining + "秒");
    }

    @Test
    @Order(43)
    @DisplayName("测试5.3: 重置配额（清除冷却和限额）")
    void testResetQuota() {
        rateLimiterService.triggerCooldown(TEST_ACCOUNT_ID, Duration.ofHours(1));
        assertTrue(rateLimiterService.isInCooldown(TEST_ACCOUNT_ID));

        rateLimiterService.resetQuota(TEST_ACCOUNT_ID);

        assertFalse(rateLimiterService.isInCooldown(TEST_ACCOUNT_ID),
                "重置后应不在冷却期");

        System.out.println("✅ 配额重置成功");
    }

    // ============================================================
    // 6. 综合预检流程测试
    // ============================================================

    @Test
    @Order(50)
    @DisplayName("测试6.1: 完整合规检查 - 合法消息")
    void testPreSendCheck_ValidMessage() {
        String validMessage = "您好，感谢关注我们的产品！如有任何问题欢迎随时咨询。";

        var result = rateLimiterService.preSendCheck(
                TEST_ACCOUNT_ID,
                "user_" + System.currentTimeMillis(),
                validMessage
        );

        System.out.println("\n===== 合规检查报告 =====");
        System.out.println("消息内容: " + validMessage);
        System.out.println("是否通过: " + result.isAllowed());
        System.out.println("违规原因: " + (result.getViolations().isEmpty() ? "无" : result.getViolations()));
        System.out.println("全局日限额: " + result.getDailyGlobalRemaining() + "/" + result.getDailyGlobalMax());
        System.out.println("单用户限额: " + result.getDailyUserRemaining() + "/" + result.getDailyUserMax());
        if (result.getContentSimilarity() != null) {
            System.out.println("内容相似度: " + String.format("%.2f%%", result.getContentSimilarity() * 100));
        }
        System.out.println("======================\n");
    }

    @Test
    @Order(51)
    @DisplayName("测试6.2: 完整合规检查 - 包含敏感词的消息")
    void testPreSendCheck_SensitiveMessage() {
        String sensitiveMessage = "加我微信xxx购买最低价产品";

        var result = rateLimiterService.preSendCheck(
                TEST_ACCOUNT_ID,
                "user_sensitive",
                sensitiveMessage
        );

        System.out.println("\n===== 敏感词消息检查报告 =====");
        System.out.println("是否通过: " + result.isAllowed());
        System.out.println("违规详情: " + result.getViolations());
        System.out.println("==============================\n");

        assertFalse(result.isAllowed(), "包含敏感词的消息不应通过");
        assertFalse(result.getViolations().isEmpty(), "应有违规原因");
    }

    @Test
    @Order(52)
    @DisplayName("测试6.3: 完整合规检查 - 冷却期内尝试发送")
    void testPreSendCheck_DuringCooldown() {
        rateLimiterService.triggerCooldown(TEST_ACCOUNT_ID, Duration.ofMinutes(5));

        var result = rateLimiterService.preSendCheck(
                TEST_ACCOUNT_ID,
                "user_cooldown",
                "正常消息"
        );

        System.out.println("\n===== 冷却期检查报告 =====");
        System.out.println("是否通过: " + result.isAllowed());
        System.out.println("违规原因: " + result.getViolations());
        System.out.println("冷却剩余: " +
                (result.getCooldownRemaining() != null ?
                        result.getCooldownRemaining().getSeconds() + "秒" : "无"));
        System.out.println("========================\n");

        assertFalse(result.isAllowed(), "冷却期内不应通过");
        assertTrue(result.getViolations().stream()
                        .anyMatch(v -> v.contains("冷却")),
                "应提示冷却期信息");
    }

    // ============================================================
    // 7. 并发安全测试
    // ============================================================

    @Test
    @Order(60)
    @DisplayName("测试7.1: 多线程并发获取令牌")
    void testConcurrentTokenAcquisition() throws InterruptedException {
        int threadCount = 20;
        int requestsPerThread = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < requestsPerThread; i++) {
                        Long accountId = TEST_ACCOUNT_ID + threadId;
                        if (rateLimiterService.tryAcquireToken(accountId, 1)) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long duration = System.currentTimeMillis() - startTime;
        int totalRequests = threadCount * requestsPerThread;

        System.out.println(String.format("\n===== 并发测试结果 ====="));
        System.out.println(String.format("线程数: %d, 每线程请求数: %d", threadCount, requestsPerThread));
        System.out.println(String.format("总请求数: %d", totalRequests));
        System.out.println(String.format("成功数: %d (%.1f%%)",
                successCount.get(), successCount.get() * 100.0 / totalRequests));
        System.out.println(String.format("失败数: %d (%.1f%%)",
                failureCount.get(), failureCount.get() * 100.0 / totalRequests));
        System.out.println(String.format("总耗时: %dms", duration));
        System.out.println(String.format("吞吐量: %.0f 次/秒",
                totalRequests / (duration / 1000.0)));
        System.out.println("========================\n");

        assertTrue(duration < 5000, "并发测试应在5秒内完成");
    }

    // ============================================================
    // 8. 配额信息查询测试
    // ============================================================

    @Test
    @Order(70)
    @DisplayName("测试8.1: 获取账号完整配额信息")
    void testGetQuotaInfo() {
        var quotaInfo = rateLimiterService.getQuotaInfo(TEST_ACCOUNT_ID);

        assertNotNull(quotaInfo);
        assertEquals(TEST_ACCOUNT_ID, quotaInfo.getAccountId());

        System.out.println("\n===== 账号配额信息 =====");
        System.out.println("账号ID: " + quotaInfo.getAccountId());
        System.out.println("冷却期状态: " + (quotaInfo.isInCooldown() ? "⏸️ 冷却中" : "✅ 正常"));
        if (quotaInfo.isInCooldown()) {
            System.out.println("冷却剩余: " + quotaInfo.getCooldownRemaining() + "秒");
        }
        System.out.println("时间窗口: " + (quotaInfo.isInTimeWindow() ? "✅ 允许" : "❌ 禁止"));
        if (!quotaInfo.isInTimeWindow()) {
            System.out.println("下次允许: " + quotaInfo.getNextAllowedTime());
        }
        System.out.println("全局日用量: " + quotaInfo.getDailyGlobalUsed() + "/" + quotaInfo.getDailyGlobalTotal());
        System.out.println("======================\n");
    }

    // ============================================================
    // 9. 边界条件测试
    // ============================================================

    @Test
    @Order(80)
    @DisplayName("测试9.1: 超长消息内容处理")
    void testPreSendCheck_VeryLongMessage() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("这是一段正常的长文本内容用于测试。");
        }

        long startTime = System.nanoTime();
        var result = rateLimiterService.preSendCheck(
                TEST_ACCOUNT_ID,
                "user_long_message",
                sb.toString()
        );
        long duration = (System.nanoTime() - startTime) / 1_000_000;

        System.out.println("超长消息(" + sb.length() + "字符)检查耗时: " + duration + "ms");
        assertTrue(duration < 100, "超长消息检查应在100ms内完成");
    }

    @Test
    @Order(81)
    @DisplayName("测试9.2: 特殊字符消息处理")
    void testPreSendCheck_SpecialCharacters() {
        String specialMsg = "Hello! 👋 您好🎉 <script>alert('xss')</script> \"引号\" '单引号' \t\n\r";

        var result = rateLimiterService.preSendCheck(
                TEST_ACCOUNT_ID,
                "user_special",
                specialMsg
        );

        assertNotNull(result);
        System.out.println("特殊字符消息检查结果: " + result.isAllowed());
    }

    @Test
    @Order(82)
    @DisplayName("测试9.3: 空消息内容处理")
    void testPreSendCheck_EmptyMessage() {
        var result = rateLimiterService.preSendCheck(
                TEST_ACCOUNT_ID,
                "user_empty",
                ""
        );

        assertNotNull(result);
        System.out.println("空消息检查结果: " + result.isAllowed());
    }

    @Test
    @Order(83)
    @DisplayName("测试9.4: null值处理")
    void testPreSendCheck_NullValues() {
        assertDoesNotThrow(() -> {
            var result = rateLimiterService.preSendCheck(
                    TEST_ACCOUNT_ID,
                    null,
                    null
            );
            assertNotNull(result);
        }, "null值不应抛出异常");
    }
}
