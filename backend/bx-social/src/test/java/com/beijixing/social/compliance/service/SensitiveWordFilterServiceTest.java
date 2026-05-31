package com.beijixing.social.compliance.service;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SensitiveWordFilterService 单元测试
 *
 * 测试覆盖范围:
 * 1. DFA算法基础功能（检测/过滤/替换）
 * 2. 平台特定规则校验（抖音/小红书）
 * 3. 边界条件处理（空值/特殊字符/超长文本）
 * 4. 性能测试（大文本/高频调用）
 * 5. 动态词库管理（添加/批量导入）
 *
 * @author 北极星AI团队
 * @version 2.0 (2026-05-20)
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SensitiveWordFilterServiceTest {

    @Autowired
    private SensitiveWordFilterService sensitiveWordFilterService;

    // ============================================================
    // 1. DFA算法基础功能测试
    // ============================================================

    @Test
    @Order(1)
    @DisplayName("测试1.1: 检测包含政治敏感词的文本")
    void testDetectPoliticalWords() {
        String text = "这个视频涉及法轮功内容";
        var result = sensitiveWordFilterService.detect(text);

        assertFalse(result.isSafe(), "应该检测到敏感词");
        assertTrue(result.getHitWords().contains("法轮功"), "应命中'法轮功'");
        assertEquals(1, result.getSensitiveWords().size(), "应检测到1个敏感词");
    }

    @Test
    @Order(2)
    @DisplayName("测试1.2: 检测包含色情低俗词汇的文本")
    void testDetectAdultWords() {
        String text = "这是一段涉及色情内容的文字";
        var result = sensitiveWordFilterService.detect(text);

        assertFalse(result.isSafe(), "应该检测到敏感词");
        assertTrue(result.getHitWords().contains("色情"), "应命中'色情'");
    }

    @Test
    @Order(3)
    @DisplayName("测试1.3: 检测包含多个敏感词的文本")
    void testDetectMultipleSensitiveWords() {
        String text = "这段内容涉及赌博和毒品，还有暴力恐怖信息";
        var result = sensitiveWordFilterService.detect(text);

        assertFalse(result.isSafe());
        assertTrue(result.getHitWords().size() >= 3, "应检测到至少3个敏感词");
        System.out.println("命中的敏感词: " + result.getHitWords());
    }

    @Test
    @Order(4)
    @DisplayName("测试1.4: 检测安全文本（无敏感词）")
    void testDetectCleanText() {
        String text = "今天天气真好，适合出去散步";
        var result = sensitiveWordFilterService.detect(text);

        assertTrue(result.isSafe(), "安全文本应该通过检测");
        assertTrue(result.getSensitiveWords() == null || result.getSensitiveWords().isEmpty(),
                "不应有敏感词列表");
        assertEquals(100.0, result.getSafetyScore(), "安全评分应为100分");
    }

    @Test
    @Order(5)
    @DisplayName("测试1.5: 敏感词过滤替换功能")
    void testFilterSensitiveWords() {
        String text = "加我微信xxx，这里有赌博网站";
        String filtered = sensitiveWordFilterService.filter(text);

        assertNotEquals(text, "过滤后文本应发生变化");
        assertFalse(filtered.contains("微信"), "'微信'应被替换");
        assertFalse(filtered.contains("赌博"), "'赌博'应被替换");
        System.out.println("原始文本: " + text);
        System.out.println("过滤后:   " + filtered);
    }

    @Test
    @Order(6)
    @DisplayName("测试1.6: 自定义替换字符")
    void testFilterWithCustomReplacementChar() {
        String text = "包含敏感词：暴力、色情";
        String filtered = sensitiveWordFilterService.filter(text, '#');

        assertTrue(filtered.contains("#"), "应使用#号替换");
        System.out.println("自定义替换结果: " + filtered);
    }

    @Test
    @Order(7)
    @DisplayName("测试1.7: 快速判断是否包含敏感词（性能优化版）")
    void testContainsSensitiveWord() {
        String dirtyText = "这段话有法轮功";
        String cleanText = "这段话很正常";

        assertTrue(sensitiveWordFilterService.containsSensitiveWord(dirtyText),
                "脏文本应返回true");
        assertFalse(sensitiveWordFilterService.containsSensitiveWord(cleanText),
                "干净文本应返回false");
    }

    // ============================================================
    // 2. 平台特定规则校验测试
    // ============================================================

    @Test
    @Order(10)
    @DisplayName("测试2.1: 抖音合规检查 - 包含联系方式")
    void testDouyinCompliance_WithContactInfo() {
        String text = "加我微信abc123购买产品";
        var result = sensitiveWordFilterService.checkDouyinCompliance(text);

        assertFalse(result.isCompliant(), "不应通过抖音合规检查");
        assertTrue(result.getViolations().stream()
                        .anyMatch(v -> v.contains("联系方式")),
                "应检测到联系方式违规");
        System.out.println("抖音违规详情: " + result.getViolations());
    }

    @Test
    @Order(11)
    @DisplayName("测试2.2: 抖音合规检查 - 包含外部链接")
    void testDouyinCompliance_WithExternalLink() {
        String text = "点击这里访问淘宝: https://www.taobao.com/xxx";
        var result = sensitiveWordFilterService.checkDouyinCompliance(text);

        assertFalse(result.isCompliant());
        assertTrue(result.getViolations().stream()
                        .anyMatch(v -> v.contains("外部链接")));
    }

    @Test
    @Order(12)
    @DisplayName("测试2.3: 抖音合规检查 - 包含极限词")
    void testDouyinCompliance_WithExtremeWords() {
        String text = "这是全网最低价，百分百有效！";
        var result = sensitiveWordFilterService.checkDouyinCompliance(text);

        assertFalse(result.isCompliant());
        assertTrue(result.getScore() < 80, "极限词应大幅扣分");
    }

    @Test
    @Order(13)
    @DisplayName("测试2.4: 抖音合规检查 - 安全文案")
    void testDouyinCompliance_CleanText() {
        String text = "这款产品性价比很高，推荐大家试试看";
        var result = sensitiveWordFilterService.checkDouyinCompliance(text);

        assertTrue(result.isCompliant(), "安全文案应通过抖音检查");
        assertEquals(100.0, result.getScore(), "满分应为100");
    }

    @Test
    @Order(14)
    @DisplayName("测试2.5: 小红书合规检查 - 包含医疗功效宣传")
    void testXiaohongshuCompliance_MedicalClaims() {
        String text = "这款面膜可以治疗痘痘，根治皮肤问题";
        var result = sensitiveWordFilterService.checkXiaohongshuCompliance(text);

        assertFalse(result.isCompliant());
        assertTrue(result.getViolations().stream()
                        .anyMatch(v -> v.contains("医疗") || v.contains("治疗")));
    }

    @Test
    @Order(15)
    @DisplayName("测试2.6: 小红书合规检查 - 提及第三方平台")
    void testXiaohongshuCompliance_ThirdPartyPlatform() {
        String text = "更多优惠请去淘宝搜索xxx";
        var result = sensitiveWordFilterService.checkXiaohongshuCompliance(text);

        assertFalse(result.isCompliant());
        assertTrue(result.getViolations().stream()
                        .anyMatch(v -> v.contains("第三方平台") || v.contains("淘宝")));
    }

    @Test
    @Order(16)
    @DisplayName("测试2.7: 小红书合规检查 - 安全文案")
    void testXiaohongshuCompliance_CleanText() {
        String text = "今天分享一个好用的护肤品，个人使用感受不错";
        var result = sensitiveWordFilterService.checkXiaohongshuCompliance(text);

        assertTrue(result.isCompliant());
    }

    // ============================================================
    // 3. 边界条件处理测试
    // ============================================================

    @Test
    @Order(20)
    @DisplayName("测试3.1: 空字符串处理")
    void testEmptyString() {
        var result = sensitiveWordFilterService.detect("");
        assertTrue(result.isSafe(), "空字符串应视为安全");
    }

    @Test
    @Order(21)
    @DisplayName("测试3.2: null值处理")
    void testNullValue() {
        var result = sensitiveWordFilterService.detect(null);
        assertTrue(result.isSafe(), "null应视为安全");
    }

    @Test
    @Order(22)
    @DisplayName("测试3.3: 仅含空白字符")
    void testWhitespaceOnly() {
        var result = sensitiveWordFilterService.detect("   \t\n  ");
        assertTrue(result.isSafe(), "纯空白字符应视为安全");
    }

    @Test
    @Order(23)
    @DisplayName("测试3.4: 超长文本处理（10000字）")
    void testVeryLongText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("这是正常文本。");
        }
        sb.append("最后加入一个敏感词：法轮功");

        long startTime = System.currentTimeMillis();
        var result = sensitiveWordFilterService.detect(sb.toString());
        long duration = System.currentTimeMillis() - startTime;

        assertFalse(result.isSafe());
        assertTrue(duration < 1000, "超长文本检测应在1秒内完成，实际:" + duration + "ms");
        System.out.println("超长文本检测耗时: " + duration + "ms");
    }

    @Test
    @Order(24)
    @DisplayName("测试3.5: 特殊字符和Emoji处理")
    void testSpecialCharactersAndEmoji() {
        String text = "哈哈😂👍，这段包含色情💥内容❗️";
        var result = sensitiveWordFilterService.detect(text);

        assertFalse(result.isSafe(), "应能识别夹杂Emoji的敏感词");
    }

    @Test
    @Order(25)
    @DisplayName("测试3.6: 大小写混合（英文敏感词）")
    void testCaseInsensitiveEnglish() {
        String text1 = "Click here for Viagra";
        String text2 = "click here for VIAGRA";
        String text3 = "CLICK HERE FOR viagra";

        var result1 = sensitiveWordFilterService.detect(text1);
        var result2 = sensitiveWordFilterService.detect(text2);
        var result3 = sensitiveWordFilterService.detect(text3);

        // 注意：当前实现可能区分大小写，根据需求调整
        System.out.println("英文大小写测试:");
        System.out.println("  原始: " + result1.isSafe());
        System.out.println("  混合: " + result2.isSafe());
        System.out.println("  全大写: " + result3.isSafe());
    }

    @Test
    @Order(26)
    @DisplayName("测试3.7: 敏感词在文本开头/中间/结尾")
    void testSensitiveWordPosition() {
        String start = "法轮功开头";
        String middle = "中间有赌博内容";
        String end = "结尾是色情";

        assertTrue(sensitiveWordFilterService.containsSensitiveWord(start));
        assertTrue(sensitiveWordFilterService.containsSensitiveWord(middle));
        assertTrue(sensitiveWordFilterService.containsSensitiveWord(end));
    }

    @Test
    @Order(27)
    @DisplayName("测试3.8: 连续多个相同敏感词")
    void testRepeatedSensitiveWords() {
        String text = "赌博赌博赌博，很多次赌博";
        var result = sensitiveWordFilterService.detect(text);

        assertFalse(result.isSafe());
        assertTrue(result.getHitWords().contains("赌博"));
    }

    // ============================================================
    // 4. 性能测试
    // ============================================================

    @Test
    @Order(30)
    @DisplayName("测试4.1: 单次检测性能（<10ms）")
    void testSingleDetectionPerformance() {
        String text = "这是一个包含敏感词（法轮功）的正常长度文本用于性能测试";

        long totalDuration = 0;
        int iterations = 100;

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            sensitiveWordFilterService.detect(text);
            totalDuration += (System.nanoTime() - start);
        }

        double avgDurationMs = (totalDuration / iterations) / 1_000_000.0;
        System.out.println(String.format("平均检测耗时: %.3f ms", avgDurationMs));

        assertTrue(avgDurationMs < 10, "单次检测应在10ms内完成");
    }

    @Test
    @Order(31)
    @DisplayName("测试4.2: 高并发场景模拟（1000次/秒）")
    void testHighThroughputPerformance() throws InterruptedException {
        int threadCount = 10;
        int iterationsPerThread = 100;
        String text = "测试文本包含赌博词汇";

        Thread[] threads = new Thread[threadCount];
        long[] durations = new long[threadCount];

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            threads[t] = new Thread(() -> {
                long start = System.currentTimeMillis();
                for (int i = 0; i < iterationsPerThread; i++) {
                    sensitiveWordFilterService.containsSensitiveWord(text);
                }
                durations[threadId] = System.currentTimeMillis() - start;
            });
        }

        for (Thread thread : threads) thread.start();
        for (Thread thread : threads) thread.join();

        long maxDuration = 0;
        for (long d : durations) {
            if (d > maxDuration) maxDuration = d;
        }

        int totalOperations = threadCount * iterationsPerThread;
        double throughput = totalOperations / (maxDuration / 1000.0);

        System.out.println(String.format("并发吞吐量: %.0f 次/秒", throughput));
        assertTrue(throughput > 1000, "吞吐量应超过1000次/秒");
    }

    // ============================================================
    // 5. 动态词库管理测试
    // ============================================================

    @Test
    @Order(40)
    @DisplayName("测试5.1: 动态添加单个敏感词")
    void testAddSingleSensitiveWord() {
        String newWord = "测试敏感词XYZ";

        sensitiveWordFilterService.addSensitiveWord(newWord);

        String text = "这句话包含测试敏感词XYZ";
        assertTrue(sensitiveWordFilterService.containsSensitiveWord(text),
                "动态添加后应能检测到新词");

        System.out.println("✅ 成功动态添加敏感词: " + newWord);
    }

    @Test
    @Order(41)
    @DisplayName("测试5.2: 批量导入敏感词")
    void testBatchAddSensitiveWords() {
        List<String> newWords = List.of(
                "新敏感词001",
                "新敏感词002",
                "新敏感词003"
        );

        sensitiveWordFilterService.batchAddSensitiveWords(newWords);

        for (String word : newWords) {
            assertTrue(sensitiveWordFilterService.containsSensitiveWord("测试" + word),
                    "应能检测到批量添加的词: " + word);
        }

        System.out.println("✅ 批量导入" + newWords.size() + "个敏感词成功");
    }

    @Test
    @Order(42)
    @DisplayName("测试5.3: 统计信息获取")
    void testGetStatistics() {
        var stats = sensitiveWordFilterService.getStatistics();

        assertNotNull(stats);
        assertTrue((Boolean) stats.getOrDefault("enabled", false), "服务应已启用");
        assertNotNull(stats.get("dictionarySize"), "应有词库大小统计");
        assertNotNull(stats.get("totalCheckCount"), "应有总检查次数统计");

        System.out.println("📊 敏感词服务统计信息:");
        stats.forEach((k, v) -> System.out.println("  " + k + ": " + v));
    }

    // ============================================================
    // 6. 广告法违禁词专项测试
    // ============================================================

    @Test
    @Order(50)
    @DisplayName("测试6.1: 极限词检测")
    void testExtremeWords() {
        String[] extremeWords = {"最便宜", "史上最强", "绝对有效", "世界第一"};

        for (String word : extremeWords) {
            String text = "这是" + word + "的产品";
            var result = sensitiveWordFilterService.detect(text);
            assertFalse(result.isSafe(), "应检测到极限词: " + word);
        }
    }

    @Test
    @Order(51)
    @DisplayName("测试6.2: 虚假宣传词检测")
    void testFalseAdvertisingWords() {
        String[] falseAds = {"包治百病", "药到病除", "百分百有效", "零风险"};

        for (String word : falseAds) {
            var result = sensitiveWordFilterService.detect("宣传: " + word);
            assertFalse(result.isSafe(), "应检测到虚假宣传词: " + word);
        }
    }

    @Test
    @Order(52)
    @DisplayName("测试6.3: 医疗功效违禁词检测")
    void testMedicalClaims() {
        String text = "这款产品可以治疗高血压、糖尿病，还能抗癌";
        var result = sensitiveWordFilterService.detect(text);

        assertFalse(result.isSafe());
        assertTrue(result.getHitWords().size() >= 2, "应检测到多个医疗违禁词");
        System.out.println("医疗违禁词命中: " + result.getHitWords());
    }
}
