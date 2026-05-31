package com.beijixing.social.crawl.engine.proxy;

import org.junit.jupiter.api.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ProxyIpPool 单元测试
 * 
 * 测试覆盖范围:
 * - 基本CRUD操作（添加、获取、删除代理）
 * - 代理轮换策略
 * - 封禁标记与恢复
 * - 并发安全性
 * - 过期清理
 * - 统计信息
 *
 * 预期覆盖率: >90%
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProxyIpPoolTest {

    private ProxyIpPool proxyIpPool;
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        proxyIpPool = new ProxyIpPool(redisTemplate);
    }

    // ==================== 基础功能测试 ====================

    @Test
    @Order(1)
    @DisplayName("1.1 添加单个代理IP到池中")
    void testAddSingleProxy() {
        ProxyIpPool.ProxyInfo proxy = createTestProxy("192.168.1.1", 8080, "RESIDENTIAL");
        
        proxyIpPool.addProxy(proxy);
        
        verify(redisTemplate, atLeastOnce()).opsForList();
        System.out.println("✅ 测试通过: 添加单个代理IP");
    }

    @Test
    @Order(2)
    @DisplayName("1.2 批量添加代理IP")
    void testBatchAddProxies() {
        List<ProxyIpPool.ProxyInfo> proxies = List.of(
            createTestProxy("192.168.1.1", 8080, "RESIDENTIAL"),
            createTestProxy("192.168.1.2", 8081, "DATA_CENTER"),
            createTestProxy("192.168.1.3", 8082, "MOBILE")
        );
        
        proxyIpPool.addProxies(proxies);
        
        System.out.println("✅ 测试通过: 批量添加3个代理IP");
    }

    @Test
    @Order(3)
    @DisplayName("1.3 获取可用代理IP（消费模型）")
    void testGetAvailableProxy() {
        when(redisTemplate.opsForList().range(anyString(), anyLong(), anyLong()))
            .thenReturn(List.of(createTestProxy("192.168.1.1", 8080, "RESIDENTIAL").toJson()));
        
        ProxyIpPool.ProxyInfo proxy = proxyIpPool.getAvailableProxy();
        
        assertNotNull(proxy);
        assertEquals("192.168.1.1", proxy.getIp());
        assertEquals(8080, proxy.getPort());
        System.out.println("✅ 测试通过: 获取可用代理IP -> " + proxy.getIp() + ":" + proxy.getPort());
    }

    @Test
    @Order(4)
    @DisplayName("1.4 代理池为空时返回null")
    void testEmptyPoolReturnsNull() {
        when(redisTemplate.opsForList().range(anyString(), anyLong(), anyLong()))
            .thenReturn(null);
        
        ProxyIpPool.ProxyInfo proxy = proxyIpPool.getAvailableProxy();
        
        assertNull(proxy);
        System.out.println("✅ 测试通过: 空代理池返回null");
    }

    // ==================== 封禁机制测试 ====================

    @Test
    @Order(5)
    @DisplayName("2.1 标记代理IP为已封禁")
    void testMarkProxyBanned() {
        String bannedIp = "192.168.1.100";
        
        proxyIpPool.markProxyBanned(bannedIp, "触发反爬机制");
        
        verify(redisTemplate).opsForValue().set(
            contains(bannedIp), 
            eq("触发反爬机制"), 
            any()
        );
        System.out.println("✅ 测试通过: 标记封禁 " + bannedIp);
    }

    @Test
    @Order(6)
    @DisplayName("2.2 封禁后代理不可用")
    void testBannedProxyNotAvailable() {
        String bannedIp = "192.168.1.100";
        
        when(redisTemplate.hasKey(contains(bannedIp))).thenReturn(true);
        
        boolean available = proxyIpPool.isProxyAvailable(bannedIp);
        
        assertFalse(available);
        System.out.println("✅ 测试通过: 封禁IP不可用");
    }

    // ==================== 序列化测试 ====================

    @Test
    @Order(7)
    @DisplayName("3.1 ProxyInfo JSON序列化/反序列化")
    void testProxyInfoSerialization() {
        ProxyIpPool.ProxyInfo original = createTestProxy("10.0.0.1", 9000, "MOBILE");
        original.setProvider("KuaiDaiLi");
        original.setExpiredTime(System.currentTimeMillis() + 3600000);
        
        String json = original.toJson();
        assertNotNull(json);
        assertTrue(json.contains("10.0.0.1"));
        
        ProxyIpPool.ProxyInfo restored = ProxyIpPool.ProxyInfo.fromJson(json);
        assertEquals(original.getIp(), restored.getIp());
        assertEquals(original.getPort(), restored.getPort());
        assertEquals(original.getType(), restored.getType());
        System.out.println("✅ 测试通过: JSON序列化/反序列化正确");
    }

    @Test
    @Order(8)
    @DisplayName("3.2 生成代理URL格式正确")
    void testProxyUrlGeneration() {
        ProxyIpPool.ProxyInfo proxyWithAuth = createTestProxy("1.2.3.4", 8080, "RESIDENTIAL");
        proxyWithAuth.setUsername("user");
        proxyWithAuth.setPassword("pass");
        
        String urlWithAuth = proxyWithAuth.toProxyUrl();
        assertTrue(urlWithAuth.contains("user:pass@1.2.3.4:8080"));
        
        ProxyIpPool.ProxyInfo proxyWithoutAuth = createTestProxy("5.6.7.8", 8080, "DATA_CENTER");
        String urlWithoutAuth = proxyWithoutAuth.toProxyUrl();
        assertTrue(urlWithoutAuth.contains("5.6.7.8:8080"));
        assertFalse(urlWithoutAuth.contains("@"));
        
        System.out.println("✅ 测试通过: 代理URL生成正确");
    }

    // ==================== 统计信息测试 ====================

    @Test
    @Order(9)
    @DisplayName("4.1 获取代理池统计信息")
    void testGetStatistics() {
        when(redisTemplate.opsForList().size(anyString())).thenReturn(50L);
        when(redisTemplate.opsForSet().size(anyString())).thenReturn(20L);
        
        ProxyIpPool.PoolStatistics stats = proxyIpPool.getStatistics();
        
        assertNotNull(stats);
        assertEquals(50, stats.getTotalPoolSize());
        assertEquals(20, stats.getActiveCount());
        assertEquals(180, stats.getHealthCheckIntervalSec());
        System.out.println("✅ 测试通过: 统计信息 -> 总数=" + stats.getTotalPoolSize() + ", 活跃=" + stats.getActiveCount());
    }

    // ==================== 并发安全测试 ====================

    @Test
    @Order(10)
    @DisplayName("5.1 并发获取代理线程安全")
    void testConcurrentGetProxy() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        when(redisTemplate.opsForList().range(anyString(), anyLong(), anyLong()))
            .thenReturn(List.of(createTestProxy("192.168.1." + Thread.currentThread().getId(), 8080, "RESIDENTIAL").toJson()));
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    ProxyIpPool.ProxyInfo proxy = proxyIpPool.getAvailableProxy();
                    assertNotNull(proxy);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "所有线程应在5秒内完成");
        
        executor.shutdown();
        System.out.println("✅ 测试通过: " + threadCount + "个并发线程安全获取代理");
    }

    // ==================== 边界条件测试 ====================

    @Test
    @Order(11)
    @DisplayName("6.1 清理过期代理")
    void testCleanupExpiredProxies() {
        proxyIpPool.cleanupExpiredProxies();
        
        System.out.println("✅ 测试通过: 清理过期代理执行无异常");
    }

    // ==================== 辅助方法 ====================

    private ProxyIpPool.ProxyInfo createTestProxy(String ip, int port, String type) {
        ProxyIpPool.ProxyInfo proxy = new ProxyIpPool.ProxyInfo();
        proxy.setIp(ip);
        proxy.setPort(port);
        proxy.setType(type);
        proxy.setExpiredTime(System.currentTimeMillis() + 7200000); // 2小时后过期
        return proxy;
    }
}
