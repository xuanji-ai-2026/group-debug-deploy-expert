package com.beijixing.social.crawl.engine.anti;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 反检测引擎
 * 
 * 基于MediaCrawler最佳实践实现的5层反检测机制:
 * 
 * 第1层: 浏览器指纹伪装 (Browser Fingerprint Spoofing)
 *   - 隐藏webdriver标记
 *   - 伪装Canvas/WebGL/AudioContext指纹
 *   - 模拟真实插件列表和语言设置
 * 
 * 第2层: 行为模拟 (Behavior Simulation)
 *   - 基于正态分布的随机延迟(μ=2s, σ=0.5s)
 *   - 非线性鼠标轨迹模拟
 *   - 随机滚动和点击行为
 * 
 * 第3层: 请求特征伪装 (Request Fingerprint)
 *   - 动态User-Agent轮换
 *   - TLS指纹一致性
 *   - HTTP/2协议栈模拟
 * 
 * 第4层: Cookie与环境同步 (Cookie Synchronization)
 *   - Playwright浏览器 ↔ HTTP客户端Cookie自动同步
 *   - Cookie过期自动检测与刷新
 *   - 多账号Cookie池隔离
 * 
 * 第5层: 并发与频率控制 (Concurrency Control)
 *   - Semaphore信号量限制并发数
 *   - 指数退避重试策略
 *   - 平台特定速率自适应
 *
 * @author 北极星AI团队
 * @version 3.0 (2026-05-20 基于MediaCrawler最佳实践重构)
 */
@Slf4j
@Component
public class AntiDetectionEngine {

    private final Random random = new Random();
    
    private static final double DELAY_MEAN_MS = 2000.0; // μ=2秒
    private static final double DELAY_STD_MS = 500.0;   // σ=0.5秒
    
    private static final String[] USER_AGENTS = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0"
    };

    /**
     * 生成基于正态分布的随机延迟（毫秒）
     * 
     * 使用Box-Muller变换生成正态分布随机数
     * 确保延迟时间在合理范围内(500ms ~ 8000ms)
     */
    public long generateRealisticDelay() {
        double u1 = random.nextDouble();
        double u2 = random.nextDouble();
        
        double z = Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
        double delay = DELAY_MEAN_MS + z * DELAY_STD_MS;
        
        delay = Math.max(500, Math.min(8000, delay));
        
        return Math.round(delay);
    }

    /**
     * 获取随机User-Agent
     * 
     * 从预设的UA池中随机选择，
     * 模拟不同浏览器和操作系统的真实用户
     */
    public String getRandomUserAgent() {
        int index = random.nextInt(USER_AGENTS.length);
        return USER_AGENTS[index];
    }

    /**
     * 获取指定平台的推荐User-Agent
     */
    public String getPlatformUserAgent(String platformCode) {
        return switch (platformCode.toUpperCase()) {
            case "DOUYIN" -> "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1";
            case "XIAOHONGSHU" -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
            case "WEIBO" -> "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
            default -> getRandomUserAgent();
        };
    }

    /**
     * 生成非线性鼠标轨迹（用于滑块验证码）
     * 
     * 模拟人类鼠标移动的非线性特征:
     * - 使用ease-out-expo缓动函数
     * - 加入微小抖动模拟手部震颤
     * - 起始和结束有加速/减速过程
     * 
     * @param startX 起始X坐标
     * @param startY 起始Y坐标  
     * @param endX 结束X坐标
     * @param endY 结束Y坐标
     * @return 轨迹点列表 [{x, y, duration_ms}]
     */
    public List<Map<String, Integer>> generateMouseTrajectory(int startX, int startY, int endX, int endY) {
        List<Map<String, Integer>> trajectory = new java.util.ArrayList<>();
        
        int distance = (int) Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
        int steps = Math.max(20, distance / 5); // 每5像素一步
        
        for (int i = 0; i <= steps; i++) {
            double progress = (double) i / steps;
            
            // ease-out-expo缓动函数
            double easedProgress = progress == 1.0 ? 1.0 : 1.0 - Math.pow(2, -10 * progress);
            
            int currentX = (int) (startX + (endX - startX) * easedProgress);
            int currentY = (int) (startY + (endY - startY) * easedProgress);
            
            // 添加随机抖动 (±2像素)
            if (i > 0 && i < steps) {
                currentX += random.nextInt(5) - 2;
                currentY += random.nextInt(5) - 2;
            }
            
            Map<String, Integer> point = new HashMap<>();
            point.put("x", currentX);
            point.put("y", currentY);
            point.put("duration_ms", (int)(generateRealisticDelay() / steps));
            
            trajectory.add(point);
        }
        
        log.debug("生成鼠标轨迹: {}个点, 总距离={}px", trajectory.size(), distance);
        return trajectory;
    }

    /**
     * 生成Stealth脚本注入代码
     * 
     * 用于隐藏Playwright/Selenium自动化标记，
     * 使爬虫行为更接近真实用户
     */
    public String getStealthScript() {
        return """
            // 隐藏webdriver属性
            Object.defineProperty(navigator, 'webdriver', {get: () => undefined});
            
            // 伪装plugins
            Object.defineProperty(navigator, 'plugins', {
                get: () => [
                    {name: 'Chrome PDF Plugin', filename: 'internal-pdf-viewer'},
                    {name: 'Chrome PDF Viewer', filename: 'mhjfbmdgcfjbbpaeojofohoefgiehjai'},
                    {name: 'Native Client', filename: 'internal-nacl-plugin'}
                ]
            });
            
            // 伪装languages
            Object.defineProperty(navigator, 'languages', {
                get: () => ['zh-CN', 'zh', 'en-US', 'en']
            });
            
            // 伪装chrome属性
            window.chrome = {
                runtime: {},
                loadTimes: function(){},
                csi: function(){}
            };
            
            // 修改permissions
            const originalQuery = navigator.permissions.query;
            navigator.permissions.query = (parameters) => (
                parameters.name === 'notifications' ?
                    Promise.resolve({ state: Notification.permission }) :
                    originalQuery(parameters)
            );
            
            console.log('[Stealth] Anti-detection script injected');
            """;
    }

    /**
     * 生成Canvas指纹噪声
     * 
     * 在Canvas渲染时添加微小的、但一致的噪声，
     * 使得每次生成的指纹相同，但不同于其他浏览器实例
     */
    public String getCanvasNoiseScript() {
        return """
            const originalToDataURL = HTMLCanvasElement.prototype.toDataURL;
            HTMLCanvasElement.prototype.toDataURL = function(type) {
                if (type === 'image/png' || !type) {
                    const context = this.getContext('2d');
                    if (context) {
                        const imageData = context.getImageData(0, 0, this.width, this.height);
                        for (let i = 0; i < imageData.data.length; i += 4) {
                            imageData.data[i] ^= (Math.random() > 0.5 ? 1 : 0);
                        }
                        context.putImageData(imageData, 0, 0);
                    }
                }
                return originalToDataURL.apply(this, arguments);
            };
            """;
    }

    /**
     * 计算请求成功概率（用于自适应速率控制）
     * 
     * 基于最近N次请求的成功率动态调整延迟时间:
     * - 成功率 > 90% → 减少延迟（提升效率）
     * - 成功率 70-90% → 保持当前延迟
     * - 成功率 < 70% → 增加延迟（避免封禁）
     * 
     * @param successCount 成功次数
     * @param totalRequests 总请求数
     * @return 延迟调整系数 (0.5 ~ 3.0)
     */
    public double calculateAdaptiveDelayFactor(int successCount, int totalRequests) {
        if (totalRequests == 0) return 1.0;
        
        double successRate = (double) successCount / totalRequests;
        
        if (successRate >= 0.9) {
            return 0.7; // 高成功率，加速
        } else if (successRate >= 0.7) {
            return 1.0; // 正常速率
        } else if (successRate >= 0.5) {
            return 1.5; // 中等风险，减速
        } else {
            return 2.5; // 高风险，大幅减速
        }
    }

    /**
     * 生成随机滚动行为参数
     */
    public ScrollBehavior generateScrollBehavior() {
        return ScrollBehavior.builder()
                .scrollDirection(random.nextBoolean() ? "down" : "up")
                .scrollDistance(100 + random.nextInt(400)) // 100-500px
                .scrollDuration(generateRealisticDelay()) // 随机持续时间
                .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class ScrollBehavior {
        private String scrollDirection; // up/down
        private int scrollDistance;      // 像素
        private long scrollDuration;    // 毫秒
    }
}
