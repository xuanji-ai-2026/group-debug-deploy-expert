# ==============================================
# 北极星AI - 社媒爬虫系统 最佳实践与反爬策略指南
# 版本: v1.0 | 更新时间: 2026-05-16
# 基于2025年全网最新技术调研整理
# ==============================================

## 一、架构设计原则（Architecture Principles）

### 1.1 分层解耦设计（Layered Decoupling）
```
┌─────────────────────────────────────────┐
│           业务调度层 (Scheduler)          │  ← 任务编排、优先级管理
├─────────────────────────────────────────┤
│         平台适配层 (Platform Layer)       │  ← 各平台独立实现
├─────────────────────────────────────────┤
│         风控引擎层 (Risk Control)         │  ← 规则评估、违规处理
├─────────────────────────────────────────┤
│         基础设施层 (Infrastructure)       │  ← 代理池、Redis、DB
└─────────────────────────────────────────┘
```

**核心优势**：
- 新增平台只需实现接口，不影响现有代码
- 风控规则可独立升级，无需修改爬虫逻辑
- 支持热插拔，运行时动态加载/卸载

### 1.2 工厂模式 + 注册表（Factory + Registry）
```java
// 推荐实现方式
@Component
public class CrawlerEngineRegistry {
    private final Map<String, PlatformCrawlerEngine> engines = new ConcurrentHashMap<>();
    
    public void registerEngine(PlatformCrawlerEngine engine) {
        engines.put(engine.getPlatformCode().toUpperCase(), engine);
    }
    
    public PlatformCrawlerEngine getEngine(String platformCode) {
        return engines.get(platformCode.toUpperCase());
    }
}
```

## 二、反爬对抗核心策略（Anti-Detection Strategies）

### 2.1 IP代理池管理（Proxy Pool Management）

#### 三级IP筛选机制：
| 等级 | 标准 | 使用场景 |
|------|------|----------|
| L1-高优 | 成功率>90%, 延迟<2s, 存活>24h | 核心任务 |
| L2-中优 | 成功率70-90%, 延迟<5s, 存活>12h | 常规任务 |
| L3-备用 | 成功率50-70%, 延迟<10s, 存活>4h | 测试/备份 |

#### IP轮换三大策略：

**① 时间触发型（Time-Based）**
```java
// 固定间隔 + 随机抖动
long baseInterval = 10 * 60 * 1000; // 10分钟
long jitter = ThreadLocalRandom.current().nextLong(-3 * 60 * 1000, 3 * 60 * 1000);
Thread.sleep(baseInterval + jitter);
```
- 适用场景：新闻聚合、价格监控
- 优点：简单可控，易于调试

**② 请求计数型（Count-Based）**
```java
int maxRequestsPerIp = 50;
if (requestCount >= maxRequestsPerIp) {
    switchToNewProxy();
}
```
- 适用场景：社交媒体、API采集
- 优点：按实际负载调整，避免浪费

**③ 智能决策型（Adaptive）**
```java
if (responseTime > 800 || failRate > 30%) {
    switchToNewProxy();
    downgradeProxyQuality(currentProxy);
}
```
- 适用场景：高价值目标、金融数据
- 优点：自适应环境变化，效率最高

### 2.2 设备指纹伪装（Fingerprint Disguise）

#### User-Agent轮换池：
```java
private static final String[] USER_AGENTS = {
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_0_0) AppleWebKit/537.36 Safari/17.0",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) Mobile/15E148",
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/120.0.0.0"
};

public String getRandomUserAgent() {
    return USER_AGENTS[ThreadLocalRandom.current().nextInt(USER_AGENTS.length)];
}
```

#### 关键伪装维度清单：
- [x] User-Agent（浏览器+版本+操作系统）
- [x] Accept-Language（语言偏好）
- [x] Referer（来源页面）
- [x] Sec-Fetch-*系列头（Chrome特有）
- [ ] Canvas指纹（需Playwright支持）
- [ ] WebGL渲染参数
- [ ] 时区与屏幕分辨率
- [ ] 字体列表（中文环境）

### 2.3 行为模拟（Behavior Simulation）

#### 鼠标轨迹模拟（贝塞尔曲线）：
```javascript
// Playwright示例 - 从点A到点B的平滑移动
async function humanMouseMove(page, fromX, fromY, toX, toY) {
    const steps = 20 + Math.floor(Math.random() * 10);
    for (let i = 0; i <= steps; i++) {
        const t = i / steps;
        const x = fromX + (toX - fromX) * t + (Math.random() - 0.5) * 2;
        const y = fromY + (toY - fromY) * t + (Math.random() - 0.5) * 2;
        await page.mouse.move(x, y);
        await page.waitForTimeout(10 + Math.random() * 20);
    }
}
```

#### 请求延迟策略：
| 场景 | 基础延迟 | 随机抖动 | 说明 |
|------|---------|---------|------|
| 列表页翻页 | 2000ms | ±500ms | 模拟阅读时间 |
| 详情页加载 | 3000ms | ±1000ms | 模拟内容浏览 |
| 评论滚动 | 1500ms | ±300ms | 模拟互动操作 |
| 高频采集 | 500ms | ±200ms | 仅用于低风险目标 |

### 2.4 Cookie与会话管理（Session Management）

#### Cookie生命周期策略：
```
获取Cookie → 预热期(5min) → 稳定期(30min) → 衰减期(10min) → 失效
     ↓            ↓              ↓             ↓
  低频测试      正常使用      降低频率      强制更换
```

#### 多账号轮换机制：
```java
public class AccountRotator {
    private Queue<SocialAccount> accountPool = new ConcurrentLinkedQueue<>();
    
    public SocialAccount getNextAvailableAccount(String platformCode) {
        SocialAccount account = accountPool.poll();
        if (account != null && isAccountHealthy(account)) {
            return account;
        }
        return refreshAccountPool(platformCode);
    }
    
    private boolean isAccountHealthy(SocialAccount account) {
        long lastUsed = Duration.between(account.getLastUsedTime(), Instant.now()).toMinutes();
        return lastUsed < 30 && account.getErrorCount() < 3;
    }
}
```

## 三、风控规则引擎设计（Risk Control Engine）

### 3.1 规则分类体系（Rule Taxonomy）

| 类别 | 触发条件 | 典型动作 | 优先级 |
|------|---------|---------|--------|
| RATE_LIMIT | 单IP请求数超阈值 | DELAY_AND_RETRY | P7-P10 |
| BAN_DETECTION | 响应码403/封禁关键词 | SWITCH_PROXY | P8-P10 |
| SIGNATURE_ERROR | 签名验证失败 | ROTATE_ACCOUNT | P9-P10 |
| COOKIE_EXPIRY | 会话过期/失效 | ROTATE_ACCOUNT | P8 |
| BEHAVIOR_ANOMALY | 请求频率异常 | REDUCE_RATE | P6-P8 |

### 3.2 规则评估流程（Evaluation Pipeline）
```
请求进入 → 规则排序(优先级) → 逐条评估 → 
  ↓                                    ↓
通过                              触发动作
  ↓                                    ↓
记录成功                          记录违规
  ↓                                    ↓
更新统计                        执行恢复策略
```

### 3.3 动态更新机制（Dynamic Update）

#### 监控周期配置：
```yaml
risk-control:
  monitoring:
    interval-ms: 300000  # 5分钟检查一次
    platforms:
      - DOUYIN
      - XIAOHONGSHU
      - KUAISHOU
      - WEIBO
      - BILIBILI
  
  update-triggers:
    signature-change: true
    cookie-policy-change: true
    success-rate-drop-threshold: 10%  # 成功率下降超过10%触发更新
    
  alert-channels:
    - webhook: "https://your-webhook-url"
    - email: "ops@beijixing.com"
```

## 四、用户自定义采集池（User Collection Pool）

### 4.1 数据模型设计
```
用户(User)
  └── 采集池(CollectionPool) [1:N]
       ├── 账号项(AccountItem)
       │    ├── accountId (平台唯一ID)
       │    ├── nickname
       │    ├── avatarUrl
       │    └── lastCrawlTime
       └── 链接项(LinkItem)
            ├── url
            ├── title
            └── source (MANUAL/AUTO/IMPORT)
```

### 4.2 采集池操作API
- `POST /api/collection/pool` - 创建采集池
- `GET /api/collection/pools` - 列出我的采集池
- `POST /api/collection/pool/{id}/account` - 添加账号
- `POST /api/collection/pool/{id}/link` - 添加链接
- `GET /api/collection/pool/{id}/items` - 获取项目列表
- `DELETE /api/collection/pool/{id}/item/{itemId}` - 移除项目
- `POST /api/collection/pool/{id}/crawl` - 对整个池执行爬取

## 五、性能优化建议（Performance Optimization）

### 5.1 连接复用（Connection Pooling）
```java
// RestTemplate配置
@Bean
public RestTemplate restTemplate() {
    HttpComponentsClientHttpRequestFactory factory = 
        new HttpComponentsClientHttpRequestFactory();
    factory.setConnectTimeout(5000);
    factory.setReadTimeout(10000);
    
    PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
    cm.setMaxTotal(100);           // 最大连接数
    cm.setDefaultMaxPerRoute(20);   // 每路由最大连接数
    
    CloseableHttpClient httpClient = HttpClientBuilder.create()
        .setConnectionManager(cm)
        .build();
        
    return new RestTemplate(factory);
}
```

### 5.2 异步并发控制（Async Concurrency）
```java
@Async("crawlTaskExecutor")
public CompletableFuture<CrawlResult> crawlAsync(CrawlTask task) {
    // 使用独立的线程池执行爬取任务
}

// 线程池配置
@Bean("crawlTaskExecutor")
public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(20);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("CrawlTask-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    return executor;
}
```

## 六、监控与告警（Monitoring & Alerting）

### 6.1 关键指标（KPIs）
| 指标名称 | 计算公式 | 告警阈值 |
|---------|---------|---------|
| 请求成功率 | 成功数/总请求数×100% | <80% |
| 平均响应时间 | 总耗时/请求数 | >3000ms |
| IP封禁率 | 封禁IP数/总IP数×100% | >20% |
| 任务完成率 | 完成任务数/总任务数×100% | <90% |
| 数据完整度 | 实际抓取数/预期抓取数×100% | <95% |

### 6.2 日志规范（Logging Standards）
```java
// 结构化日志格式
log.info("[{}] {} | taskId={} | platform={} | count={} | cost={}ms",
    "CRAWL_SUCCESS", "评论爬取完成", 
    task.getId(), task.getPlatformCode(), 
    comments.size(), durationMs);

// 错误日志必须包含上下文
log.error("[{}] {} | taskId={} | platform={} | error={}",
    "CRAWL_FAILED", "签名验证失败",
    task.getId(), task.getPlatformCode(),
    e.getMessage(), e);
```

## 七、合规性要求（Compliance Requirements）

### 7.1 法律边界
- ✅ 只抓取公开可见的数据
- ❌ 不绕过登录墙/付费墙
- ❌ 不抓取个人隐私信息（手机号、地址等需脱敏）
- ⚠️ 遵守robots.txt协议
- ⚠️ 尊重平台的Terms of Service

### 7.2 技术伦理
- 合理控制请求频率，避免对目标服务器造成压力
- 明确标识User-Agent（包含联系方式）
- 提供数据来源追溯能力
- 定期清理过期数据

## 八、故障排查手册（Troubleshooting Guide）

### 8.1 常见问题速查表
| 错误现象 | 可能原因 | 解决方案 |
|---------|---------|---------|
| 403 Forbidden | IP被封禁 | 切换代理IP |
| 签名错误 | 算法版本过旧 | 更新签名生成逻辑 |
| Cookie失效 | 会话过期 | 重新登录获取新Cookie |
| 请求超时 | 目标服务器负载高 | 增加重试间隔 |
| 数据为空 | 页面结构变化 | 更新解析逻辑 |

### 8.2 应急预案
1. **IP大规模封禁**：立即切换至住宅代理池，降低请求频率50%
2. **签名算法变更**：暂停该平台任务，启动紧急逆向分析
3. **账号批量封禁**：启用备用账号池，排查异常行为
4. **服务不可用**：切换至降级模式（仅采集缓存数据）

---

## 参考资源（References）
- MediaCrawler开源项目：https://github.com/NanmiCoder/MediaCrawler
- Playwright官方文档：https://playwright.dev/java/
- Spring Boot异步编程：https://docs.spring.io/spring-boot/docs/
- 反爬技术白皮书（2025版）：https://blog.csdn.net/weixin_41943766/article/details/155193517
