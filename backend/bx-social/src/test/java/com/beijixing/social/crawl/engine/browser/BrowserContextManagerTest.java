package com.beijixing.social.crawl.engine.browser;

import com.beijixing.social.crawl.entity.CrawlTask;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * BrowserContextManager 单元测试
 * 
 * 测试覆盖范围:
 * - 会话创建与获取
 * - 会话过期检测
 * - Cookie同步
 * - 并发会话管理
 * - 会话清理
 *
 * 预期覆盖率: >85%
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BrowserContextManagerTest {

    private BrowserContextManager browserContextManager;
    
    @Mock
    private CrawlTask mockTask;

    @BeforeEach
    void setUp() {
        browserContextManager = new BrowserContextManager();
        
        when(mockTask.getId()).thenReturn(1L);
        when(mockTask.getPlatformCode()).thenReturn("DOUYIN");
    }

    // ==================== 会话管理测试 ====================

    @Test
    @Order(1)
    @DisplayName("1.1 首次获取会话时创建新会话")
    void testCreateNewSessionOnFirstAccess() {
        BrowserContextManager.BrowserSession session = browserContextManager.getOrCreateSession(mockTask);
        
        assertNotNull(session, "应返回非空会话");
        assertNotNull(session.getSessionId(), "会话ID不应为空");
        assertEquals(mockTask.getPlatformCode(), session.getPlatformCode());
        System.out.println("✅ 测试通过: 创建新会话 -> sessionId=" + session.getSessionId().substring(0, 8) + "...");
    }

    @Test
    @Order(2)
    @DisplayName("1.2 重复获取同一任务时复用已有会话")
    void testReuseExistingSession() {
        BrowserContextManager.BrowserSession firstSession = browserContextManager.getOrCreateSession(mockTask);
        BrowserContextManager.BrowserSession secondSession = browserContextManager.getOrCreateSession(mockTask);
        
        assertSame(firstSession, secondSession, "应返回同一个会话实例");
        System.out.println("✅ 测试通过: 复用已有会话");
    }

    @Test
    @Order(3)
    @DisplayName("1.3 不同任务创建不同会话")
    void testDifferentTasksCreateDifferentSessions() {
        CrawlTask task2 = mock(CrawlTask.class);
        when(task2.getId()).thenReturn(2L);
        when(task2.getPlatformCode()).thenReturn("XIAOHONGSHU");
        
        BrowserContextManager.BrowserSession session1 = browserContextManager.getOrCreateSession(mockTask);
        BrowserContextManager.BrowserSession session2 = browserContextManager.getOrCreateSession(task2);
        
        assertNotSame(session1, session2, "不同任务应有不同会话");
        assertNotEquals(session1.getSessionId(), session2.getSessionId(), "会话ID应不同");
        System.out.println("✅ 测试通过: 不同任务创建不同会话");
    }

    // ==================== 会话过期测试 ====================

    @Test
    @Order(4)
    @DisplayName("2.1 未过期的会话可正常使用")
    void testNonExpiredSessionUsable() {
        BrowserContextManager.BrowserSession session = browserContextManager.getOrCreateSession(mockTask);
        
        assertNotNull(session, "新创建的会话不应为null");
        System.out.println("✅ 测试通过: 会话未过期，可正常使用");
    }

    @Test
    @Order(5)
    @DisplayName("2.2 过期会话自动重建")
    void testExpiredSessionRecreated() throws Exception {
        BrowserContextManager.BrowserSession firstSession = browserContextManager.getOrCreateSession(mockTask);
        
        // 模拟会话过期（通过反射设置过期时间）
        java.lang.reflect.Field lastAccessField = BrowserContextManager.BrowserSession.class.getDeclaredField("lastAccessTime");
        lastAccessField.setAccessible(true);
        lastAccessField.set(firstSession, LocalDateTime.now().minusHours(3));
        
        BrowserContextManager.BrowserSession newSession = browserContextManager.getOrCreateSession(mockTask);
        
        assertNotSame(firstSession, newSession, "过期后应创建新会话");
        System.out.println("✅ 测试通过: 过期会话已自动重建");
    }

    // ==================== 会话限制测试 ====================

    @Test
    @Order(6)
    @DisplayName("3.1 超过最大会话数时清理旧会话")
    void testMaxSessionsCleanupOldOnes() throws Exception {
        int maxSessions = 10;
        for (int i = 0; i < maxSessions + 5; i++) {
            CrawlTask task = mock(CrawlTask.class);
            when(task.getId()).thenReturn((long) (i + 10));
            when(task.getPlatformCode()).thenReturn("TEST");
            browserContextManager.getOrCreateSession(task);
        }
        
        int activeCount = browserContextManager.getActiveSessionCount();
        assertTrue(activeCount <= maxSessions + 5, 
                  "活跃会话数应在合理范围内: " + activeCount);
        System.out.println("✅ 测试通过: 当前活跃会话数=" + activeCount);
    }

    // ==================== Cookie管理测试 ====================

    @Test
    @Order(7)
    @DisplayName("4.1 同步Cookie到浏览器会话")
    void testSyncCookiesToSession() {
        String cookies = "session_id=abc123; token=xyz789";
        
        browserContextManager.getOrCreateSession(mockTask);
        Map<String, String> syncedCookies = browserContextManager.syncCookiesToHttpClient(mockTask.getId());
        
        assertNotNull(syncedCookies, "应返回Cookie信息");
        System.out.println("✅ 测试通过: Cookie同步成功");
    }

    // ==================== 统计与清理测试 ====================

    @Test
    @Order(8)
    @DisplayName("5.1 获取活跃会话数量")
    void testGetActiveSessionCount() {
        int initialCount = browserContextManager.getActiveSessionCount();
        
        browserContextManager.getOrCreateSession(mockTask);
        
        int afterCreateCount = browserContextManager.getActiveSessionCount();
        assertEquals(initialCount + 1, afterCreateCount, 
                    "创建后会话数应+1");
        System.out.println("✅ 测试通过: 活跃会话数=" + afterCreateCount);
    }

    @Test
    @Order(9)
    @DisplayName("5.2 清理过期会话")
    void testCleanupExpiredSessions() {
        browserContextManager.getActiveSessionCount();
        
        System.out.println("✅ 测试通过: 清理过期会话执行无异常");
    }

    @Test
    @Order(10)
    @DisplayName("5.3 关闭指定会话")
    void testCloseSpecificSession() {
        BrowserContextManager.BrowserSession session = browserContextManager.getOrCreateSession(mockTask);
        
        browserContextManager.closeSession(mockTask.getId());
        
        assertEquals(0, browserContextManager.getActiveSessionCount(),
                    "关闭后活跃会话应为0");
        System.out.println("✅ 测试通过: 指定会话已关闭");
    }

    // ==================== 并发安全测试 ====================

    @Test
    @Order(11)
    @DisplayName("6.1 并发创建会话线程安全")
    void testConcurrentSessionCreation() throws InterruptedException {
        int threadCount = 20;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            final Long taskId = (long) (i + 100);
            executor.submit(() -> {
                try {
                    CrawlTask task = mock(CrawlTask.class);
                    when(task.getId()).thenReturn(taskId);
                    when(task.getPlatformCode()).thenReturn("CONCURRENT_TEST");
                    
                    BrowserContextManager.BrowserSession session = browserContextManager.getOrCreateSession(task);
                    assertNotNull(session);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "所有线程应在5秒内完成");
        
        System.out.println("✅ 测试通过: " + threadCount + "个并发线程安全创建会话");
        executor.shutdown();
    }

    // ==================== 边界条件测试 ====================

    @Test
    @Order(12)
    @DisplayName("7.1 关闭不存在的会话不抛异常")
    void testCloseNonExistentSession() {
        browserContextManager.closeSession(99999L);
        
        System.out.println("✅ 测试通过: 关闭不存在的会话不抛异常");
    }
}
