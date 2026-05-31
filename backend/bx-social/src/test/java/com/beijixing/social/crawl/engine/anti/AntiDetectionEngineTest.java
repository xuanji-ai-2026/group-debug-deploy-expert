package com.beijixing.social.crawl.engine.anti;

import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AntiDetectionEngine 单元测试
 * 
 * 测试覆盖范围:
 * - 正态分布延迟生成（μ=2s, σ=0.5s）
 * - User-Agent生成与轮换
 * - 鼠标轨迹模拟（非线性、带抖动）
 * - Stealth脚本生成
 * - Canvas指纹噪声
 * - 自适应速率调整
 * - 滚动行为生成
 *
 * 预期覆盖率: >90%
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AntiDetectionEngineTest {

    private AntiDetectionEngine antiDetectionEngine;

    @BeforeEach
    void setUp() {
        antiDetectionEngine = new AntiDetectionEngine();
    }

    // ==================== 延迟生成测试 ====================

    @Test
    @Order(1)
    @DisplayName("1.1 生成的延迟在合理范围内(500ms-8000ms)")
    void testDelayWithinRange() {
        for (int i = 0; i < 100; i++) {
            long delay = antiDetectionEngine.generateRealisticDelay();
            
            assertTrue(delay >= 500, "延迟应≥500ms，实际: " + delay + "ms");
            assertTrue(delay <= 8000, "延迟应≤8000ms，实际: " + delay + "ms");
        }
        System.out.println("✅ 测试通过: 100次延迟全部在500-8000ms范围内");
    }

    @Test
    @Order(2)
    @DisplayName("1.2 延迟时间符合正态分布特征")
    void testDelayNormalDistribution() {
        int iterations = 1000;
        double sum = 0;
        double sumSquares = 0;
        
        for (int i = 0; i < iterations; i++) {
            long delay = antiDetectionEngine.generateRealisticDelay();
            sum += delay;
            sumSquares += delay * delay;
        }
        
        double mean = sum / iterations;
        double variance = (sumSquares / iterations) - (mean * mean);
        double stdDev = Math.sqrt(variance);
        
        System.out.printf("✅ 统计: 均值=%.0fms (目标2000ms), 标准差=%.0fms (目标500ms)%n", mean, stdDev);
        
        assertTrue(mean >= 1500 && mean <= 2500, "均值应在1500-2500ms之间，实际: " + mean + "ms");
        assertTrue(stdDev >= 300 && stdDev <= 700, "标准差应在300-700ms之间，实际: " + stdDev + "ms");
    }

    // ==================== User-Agent测试 ====================

    @Test
    @Order(3)
    @DisplayName("2.1 随机User-Agent不为空且包含浏览器标识")
    void testRandomUserAgent() {
        String ua = antiDetectionEngine.getRandomUserAgent();
        
        assertNotNull(ua);
        assertTrue(ua.length() > 50, "UA长度应>50字符");
        assertTrue(ua.contains("Mozilla") || ua.contains("AppleWebKit"), 
                  "UA应包含浏览器标识");
        System.out.println("✅ 测试通过: 随机UA -> " + ua.substring(0, 50) + "...");
    }

    @Test
    @Order(4)
    @DisplayName("2.2 平台特定User-Agent正确")
    void testPlatformSpecificUserAgent() {
        String douyinUa = antiDetectionEngine.getPlatformUserAgent("DOUYIN");
        assertTrue(douyinUa.contains("iPhone"), "抖音UA应为移动端");
        
        String xhsUa = antiDetectionEngine.getPlatformUserAgent("XIAOHONGSHU");
        assertTrue(xhsUa.contains("Chrome"), "小红书UA应为桌面端Chrome");
        
        String weiboUa = antiDetectionEngine.getPlatformUserAgent("WEIBO");
        assertTrue(weiboUa.contains("Android"), "微博UA应为Android端");
        
        System.out.println("✅ 测试通过: 平台特定UA生成正确");
    }

    // ==================== 鼠标轨迹测试 ====================

    @Test
    @Order(5)
    @DisplayName("3.1 鼠标轨迹点数量合理")
    void testMouseTrajectoryPointCount() {
        var trajectory = antiDetectionEngine.generateMouseTrajectory(0, 0, 500, 400);
        
        assertNotNull(trajectory);
        assertTrue(trajectory.size() >= 20, "轨迹点数应≥20，实际: " + trajectory.size());
        assertTrue(trajectory.size() <= 200, "轨迹点数应≤200，实际: " + trajectory.size());
        System.out.println("✅ 测试通过: 轨迹包含" + trajectory.size() + "个点");
    }

    @Test
    @Order(6)
    @DisplayName("3.2 鼠标轨迹从起点到终点")
    void testMouseTrajectoryStartEnd() {
        int startX = 100, startY = 100;
        int endX = 500, endY = 400;
        
        var trajectory = antiDetectionEngine.generateMouseTrajectory(startX, startY, endX, endY);
        
        var firstPoint = trajectory.get(0);
        var lastPoint = trajectory.get(trajectory.size() - 1);
        
        assertEquals(startX, firstPoint.get("x"));
        assertEquals(startY, firstPoint.get("y"));
        
        boolean nearEndX = Math.abs(lastPoint.get("x") - endX) < 10;
        boolean nearEndY = Math.abs(lastPoint.get("y") - endY) < 10;
        assertTrue(nearEndX && nearEndY, "终点应接近目标位置");
        
        System.out.println("✅ 测试通过: 轨迹从(" + startX + "," + startY + ")到(" + endX + "," + endY + ")");
    }

    @Test
    @Order(7)
    @DisplayName("3.3 轨迹包含抖动（非线性）")
    void testTrajectoryHasJitter() {
        var trajectory = antiDetectionEngine.generateMouseTrajectory(0, 0, 300, 300);
        
        boolean hasJitter = false;
        for (int i = 1; i < trajectory.size() - 1; i++) {
            var prev = trajectory.get(i - 1);
            var curr = trajectory.get(i);
            int dx = Math.abs(curr.get("x") - prev.get("x"));
            int dy = Math.abs(curr.get("y") - prev.get("y"));
            if (dx > 5 || dy > 5) { // 非线性移动
                hasJitter = true;
                break;
            }
        }
        
        assertTrue(hasJitter, "轨迹应有非线性抖动");
        System.out.println("✅ 测试通过: 轨迹包含随机抖动");
    }

    // ==================== Stealth脚本测试 ====================

    @Test
    @Order(8)
    @DisplayName("4.1 Stealth脚本包含关键反检测代码")
    void testStealthScriptContent() {
        String script = antiDetectionEngine.getStealthScript();
        
        assertNotNull(script);
        assertTrue(script.contains("webdriver"), "应隐藏webdriver标记");
        assertTrue(script.contains("plugins"), "应伪装plugins");
        assertTrue(script.contains("languages"), "应伪装languages");
        assertTrue(script.contains("chrome"), "应添加chrome属性");
        System.out.println("✅ 测试通过: Stealth脚本包含所有反检测要素");
    }

    @Test
    @Order(9)
    @DisplayName("4.2 Canvas噪声脚本包含toDataURL重写")
    void testCanvasNoiseScript() {
        String script = antiDetectionEngine.getCanvasNoiseScript();
        
        assertNotNull(script);
        assertTrue(script.contains("toDataURL"), "应重写toDataURL方法");
        assertTrue(script.contains("getImageData"), "应修改图像数据");
        System.out.println("✅ 测试通过: Canvas噪声脚本正确");
    }

    // ==================== 自适应速率调整测试 ====================

    @Test
    @Order(10)
    @DisplayName("5.1 高成功率时降低延迟")
    void testHighSuccessRateReducesDelay() {
        double factor = antiDetectionEngine.calculateAdaptiveDelayFactor(95, 100);
        
        assertTrue(factor < 1.0, "高成功率(95%)应加速，系数<1.0，实际: " + factor);
        System.out.println("✅ 测试通过: 高成功率 -> 系数=" + factor);
    }

    @Test
    @Order(11)
    @DisplayName("5.2 低成功率时增加延迟")
    void testLowSuccessRateIncreasesDelay() {
        double factor = antiDetectionEngine.calculateAdaptiveDelayFactor(40, 100);
        
        assertTrue(factor > 2.0, "低成功率(40%)应大幅减速，系数>2.0，实际: " + factor);
        System.out.println("✅ 测试通过: 低成功率 -> 系数=" + factor);
    }

    @Test
    @Order(12)
    @DisplayName("5.3 正常成功率时保持默认")
    void testNormalSuccessRateMaintainsDefault() {
        double factor = antiDetectionEngine.calculateAdaptiveDelayFactor(85, 100);
        
        assertTrue(factor >= 0.8 && factor <= 1.2, "正常成功率(85%)应保持默认，系数接近1.0，实际: " + factor);
        System.out.println("✅ 测试通过: 正常成功率 -> 系数=" + factor);
    }

    // ==================== 滚动行为测试 ====================

    @Test
    @Order(13)
    @DisplayName("6.1 滚动行为参数合理")
    void testScrollBehaviorParameters() {
        AntiDetectionEngine.ScrollBehavior behavior = antiDetectionEngine.generateScrollBehavior();
        
        assertNotNull(behavior);
        assertTrue(List.of("up", "down").contains(behavior.getScrollDirection()), 
                  "方向应为up或down");
        assertTrue(behavior.getScrollDistance() >= 100 && behavior.getScrollDistance() <= 500,
                  "距离应在100-500px之间");
        assertTrue(behavior.getScrollDuration() >= 500 && behavior.getScrollDuration() <= 8000,
                  "持续时间应在500-8000ms之间");
        System.out.println("✅ 测试通过: 滚动行为 -> 方向=" + behavior.getScrollDirection() + 
                          ", 距离=" + behavior.getScrollDistance() + "px");
    }

    // ==================== 边界条件测试 ====================

    @Test
    @Order(14)
    @DisplayName("7.1 零请求时的自适应系数")
    void testZeroRequestsAdaptiveFactor() {
        double factor = antiDetectionEngine.calculateAdaptiveDelayFactor(0, 0);
        
        assertEquals(1.0, factor, "零请求时应返回默认系数1.0");
        System.out.println("✅ 测试通过: 零请求 -> 系数=1.0");
    }
}
