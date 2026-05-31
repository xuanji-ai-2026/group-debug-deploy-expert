# 北极星AI - 系统架构设计文档 v1.0

> **项目代号**: BeijiXing-AI (PROJ-BJX-001)
> **版本**: 1.0.0-SNAPSHOT
> **架构模式**: 极简单体架构（Monolith）| 微服务预留
> **最后更新**: 2026-05-21
> **文档状态**: 正式版

---

## 目录

- [1. 项目概述](#1-项目概述)
- [2. 系统架构图](#2-系统架构图)
- [3. 技术选型与决策记录（ADR）](#3-技术选型与决策记录adr)
- [4. 核心模块说明](#4-核心模块说明)
- [5. 数据库设计](#5-数据库设计)
- [6. 缓存策略](#6-缓存策略)
- [7. 安全设计](#7-安全设计)
- [8. 可观测性设计](#8-可观测性设计)
- [9. 性能优化](#9-性能优化)
- [10. 部署架构](#10-部署架构)
- [11. 扩展性设计](#11-扩展性设计)
- [12. 附录](#12-附录)

---

## 1. 项目概述

### 1.1 项目背景与目标

**北极星AI商机获客系统** 是面向企业/商户的**多租户SaaS智能运营获客平台**，核心定位为"AI驱动的一站式内容营销+社交获客+商机管理工具"。

**核心业务目标：**
- AI智能内容生成与多平台一键分发（微信公众号/微博/抖音/小红书/B站）
- 社交平台私信自动化获客（截客引擎 + 意向分析）
- 商机全生命周期管理（线索→跟进→报价→成交）
- 多租户SaaS运营（套餐管理/资源配额/计费体系）
- 企业级风控合规（敏感词过滤/频率限制/平台规则引擎）

### 1.2 技术栈总览

| 层次 | 技术 | 版本 | 用途 |
|------|------|------|------|
| **运行时** | Java (JDK) | 17 | 应用运行环境 |
| **核心框架** | Spring Boot | 3.3.6 | 应用框架 |
| **微服务框架** | Spring Cloud | 2023.0.5 | 服务治理（已禁用，保留依赖） |
| **服务发现** | Alibaba Nacos | 2.2.3 | 配置中心/注册中心（微服务阶段启用） |
| **ORM框架** | MyBatis Plus | 3.5.5 | 数据持久层 |
| **数据库** | MariaDB | 10.11 | 主数据存储 |
| **缓存** | Redis (Lettuce) | 6.0-alpine | 缓存/会话/分布式锁 |
| **分布式锁** | Redisson | 3.24.3 | 分布式并发控制 |
| **消息队列** | RabbitMQ | 3.11 | 异步消息解耦 |
| **任务调度** | XXL-Job | 2.4.0 | 分布式定时任务 |
| **搜索引擎** | Elasticsearch | 8.11.0 | 全文检索/商机搜索 |
| **文档数据库** | MongoDB | 6.0 | 日志/内容存储 |
| **对象存储** | 腾讯云COS SDK | 5.6.89 | 文件存储 |
| **API网关** | Spring Cloud Gateway | - | 路由/认证/限流 |
| **API文档** | Knife4j (OpenAPI 3) | 4.4.0 | 接口文档 |
| **JSON处理** | Fastjson2 / Jackson | 2.0.43 | 序列化/反序列化 |
| **工具库** | Hutool | 5.8.23 | 通用工具集 |
| **Excel处理** | EasyExcel | 3.3.4 | 数据导入导出 |
| **熔断降级** | Resilience4j | 2.1.0 | 容错保护 |
| **前端(PC)** | Vue.js 3 + Vite | - | 用户端/管理后台 |
| **前端(移动端)** | Android(Kotlin) / iOS(SwiftUI) | - | 移动端App |
| **容器化** | Docker + Docker Compose | 3.8 | 容器编排 |
| **编排调度** | Kubernetes | >=1.20 | 生产级部署 |

### 1.3 业务范围全景

```
┌─────────────────────────────────────────────────────────────────────┐
│                      北极星AI 业务域                                 │
├──────────┬──────────┬──────────┬──────────┬──────────┬─────────────┤
│  AI内容   │ 社交获客  │ 商机管理  │ 计费支付  │ 风控合规  │  基础设施   │
│  营销     │  引擎    │  CRM    │  体系    │  安全    │           │
├──────────┼──────────┼──────────┼──────────┼──────────┼─────────────┤
│ • 文案生成 │ • 抖音私信 │ • 线索录入 │ • 套餐订阅 │ • 敏感词  │ • 多租户   │
│ • 多平台  │ • 小红书  │ • 意向评分 │ • 充值消费 │ • 频率限  │ • RBAC权限  │
│  一键发布 │ • 微信    │ • 跟进管理 │ • 发票管理 │ • IP黑名单│ • 消息通知  │
│ • AI改写  │ • 截客引擎 │ • 漏斗分析 │ • 对账结算 │ • 操作审计│ • 监控告警  │
│ • 内容合规 │ • OAuth  │ • AI分析  │ • 余额预警 │ • 数据脱敏│ • 定时任务  │
└──────────┴──────────┴──────────┴──────────┴──────────┴─────────────┘
```

---

## 2. 系统架构图

### 2.1 整体架构分层图

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           接入层 (Access Layer)                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │  Web-PC      │  │ Web-Admin    │  │ Mobile App   │  │ OpenAPI/第三方   │  │
│  │  (Vue3+Vite) │  │ (Vue3+Vite)  │  │ (Android/iOS)│  │                  │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘  │
│         │                 │                 │                    │             │
│  ┌──────┴─────────────────┴─────────────────┴────────────────────┴─────────┐  │
│  │                     Nginx 反向代理 (TLS终止/静态资源/负载均衡)            │  │
│  └────────────────────────────────────┬───────────────────────────────────┘  │
└───────────────────────────────────────┼──────────────────────────────────────┘
                                        │
┌───────────────────────────────────────┼──────────────────────────────────────┐
│                        网关层 (Gateway Layer)                                  │
│  ┌────────────────────────────────────┴──────────────────────────────────┐  │
│  │              bx-gateway (Spring Cloud Gateway)                         │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐  │  │
│  │  │AuthFilter│ │RateLimit │ │LogFilter │ │Route     │ │Fallback    │  │  │
│  │  │JWT认证   │ │令牌桶限流 │ │请求日志  │ │路由转发  │ │熔断降级    │  │  │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └────────────┘  │  │
│  └────────────────────────────────────┬──────────────────────────────────┘  │
└───────────────────────────────────────┼──────────────────────────────────────┘
                                        │
┌───────────────────────────────────────┼──────────────────────────────────────┐
│                       应用层 (Application Layer)                              │
│  ┌────────────────────────────────────┴──────────────────────────────────┐  │
│  │               beijixing-app (极简单体应用 - 单一JAR)                   │  │
│  │                                                                        │  │
│  │  ┌─────────────────────────────────────────────────────────────────┐  │  │
│  │  │                    统一配置中心 (application.yml)                │  │  │
│  │  │  Tomcat:8080 | HikariCP:50 | Lettuce:50 | MDC TraceId          │  │  │
│  │  └─────────────────────────────────────────────────────────────────┘  │  │
│  │                                                                        │  │
│  │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐  │  │
│  │  │bx-user │bx-tenant│bx-lead │bx-content│bx-risk │bx-bill │  │  │
│  │  │用户认证 │多租户  │商机CRM │内容营销 │风控安全 │计费支付 │  │  │
│  │  └────────┘ └────────┘ └────────┘ └────────┘ └────────┘ └────────┘  │  │
│  │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐  │  │
│  │  │bx-ai   │bx-msg   │bx-store │bx-sched │bx-monitor│bx-system│  │  │
│  │  │AI模型  │消息通知 │文件存储 │定时任务  │监控告警  │系统管理 │  │  │
│  │  └────────┘ └────────┘ └────────┘ └────────┘ └────────┘ └────────┘  │  │
│  │  ┌────────┐ ┌────────┐ ┌────────┐                                    │  │
│  │  │bx-social│bx-data  │bx-task  │                                    │  │
│  │  │社交爬虫 │数据分析 │任务管理  │                                    │  │
│  │  └────────┘ └────────┘ └────────┘                                    │  │
│  │                                                                        │  │
│  │  ┌─────────────────────────────────────────────────────────────────┐  │  │
│  │  │  bx-common (公共基础库)                                         │  │  │
│  │  │  Result<T> | TenantContextHolder | TenantContextFilter |         │  │  │
│  │  │  BxTenantLineHandler | CommonConstants | TenantAwareEntity       │  │  │
│  │  └─────────────────────────────────────────────────────────────────┘  │  │
│  └────────────────────────────────────┬──────────────────────────────────┘  │
└───────────────────────────────────────┼──────────────────────────────────────┘
                                        │
┌───────────────────────────────────────┼──────────────────────────────────────┐
│                       基础设施层 (Infrastructure Layer)                        │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │ MariaDB  │  Redis    │RabbitMQ  │ MongoDB   │Elasticsearch│          │
│  │ 10.11    │  6.0     │  3.11    │  6.0     │  8.11     │          │
│  │ 主数据库  │ 缓存/会话 │ 消息队列  │ 日志/文档 │ 全文检索   │          │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐                          │
│  │ Nacos    │ XXL-Job   │Prometheus │ Grafana   │ 腾讯云COS  │              │
│  │ 配置中心  │ 任务调度  │ 指标采集  │ 可视化   │ 对象存储   │              │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘           │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 模块依赖关系图

```
                        ┌─────────────────┐
                        │  beijixing-app   │
                        │  (启动入口/聚合)  │
                        └────────┬────────┘
                                 │ depends
            ┌────────────────────┼────────────────────┐
            │                    │                    │
     ┌──────┴──────┐     ┌──────┴──────┐     ┌───────┴───────┐
     │  bx-common  │◄────│  所有业务模块  │◄────│  第三方依赖    │
     │  (基础库)    │     │  (14个模块)   │     │  (Spring等)   │
     └─────────────┘     └──────────────┘     └───────────────┘

业务模块内部依赖关系:
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│  bx-user ──────┐                                                │
│  (认证授权)     │                                                │
│                ▼                                                │
│  bx-tenant ◄── bx-lead ──► bx-content ──► bx-social            │
│  (多租户)      (商机CRM)  (内容营销)    (社交爬虫)               │
│       │            │           │             │                  │
│       │            ▼           ▼             │                  │
│       │        bx-data     bx-ai            │                  │
│       │        (数据分析)  (AI模型)         │                  │
│       │            │           │             │                  │
│       └────► bx-risk ◄──────────────────────┘                  │
│            (风控合规)                                           │
│                │                                               │
│                ▼                                               │
│          bx-billing ◄── bx-storage                             │
│          (计费支付)    (对象存储)                               │
│                │                                               │
│          bx-message ◄── bx-schedule                            │
│          (消息通知)    (定时任务)                               │
│                │                                               │
│          bx-monitor ◄── bx-system                              │
│          (监控告警)    (系统管理)                               │
│                │                                               │
│            bx-task                                              │
│          (任务管理)                                             │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### 2.3 核心数据流图

```
用户登录流程:
┌──────┐   HTTP   ┌────────┐   JWT验证   ┌────────┐   查询DB   ┌───────┐
│Client│ ──────► │Nginx  │ ─────────► │Gateway │ ────────► │bx-user│
│(PC/Mobile)   │Proxy  │            │AuthFilter│          │AuthService│
└──────┘        └────────┘            └────────┘          └───┬───┘
                                                          │
                                     ┌────────────────────┤
                                     │                    │
                              ┌──────▼──────┐     ┌──────▼──────┐
                              │  MariaDB     │     │   Redis     │
                              │ user表+角色表 │     │ Token缓存   │
                              └─────────────┘     └─────────────┘

AI内容生成流程:
┌────────┐  创建任务  ┌─────────┐  入队   ┌─────────┐  消费   ┌───────┐
│Web-PC  │ ────────► │bx-content│ ──────► │RabbitMQ │ ──────► │bx-ai  │
│管理员  │           │Controller│         │Queue   │         │ModelSvc│
└────────┘           └─────────┘         └─────────┘         └───┬───┘
                                                              │
                                                    ┌─────────▼─────────┐
                                                    │ 火山引擎/DeepSeek   │
                                                    │ LLM API调用         │
                                                    └─────────┬─────────┘
                                                              │
                                                    ┌─────────▼─────────┐
                                                    │ 结果回写Redis+DB   │
                                                    │ WebSocket通知前端  │
                                                    └───────────────────┘

社交截客流程:
┌────────┐  触发截客  ┌─────────┐  XXL-Job  ┌──────────┐  OAuth  ┌──────────┐
│定时任务 │ ────────► │bx-schedule│ ────────► │bx-social │ ──────► │抖音/小红书│
│Cron    │           │Executor  │           │Intercept │        │开放平台API│
└────────┘           └─────────┘           └────┬─────┘        └────┬─────┘
                                                  │                    │
                                          ┌───────▼──────┐   ┌──────▼──────┐
                                          │ 合规检查引擎  │◄──│ 平台私信API  │
                                          │ 敏感词+频率   │   │ 返回潜在客户 │
                                          │ 时间窗口检查  │   └─────────────┘
                                          └───────┬──────┘
                                                  │
                                          ┌───────▼──────┐
                                          │ bx-lead 写入  │
                                          │ 新商机线索    │
                                          └──────────────┘
```

---

## 3. 技术选型与决策记录（ADR）

### ADR-001: 极简单体架构 vs 微服务架构

| 维度 | 极简单体架构（当前选择） | 微服务架构（未来路径） |
|------|------------------------|---------------------|
| **决策时间** | Phase 1 (2026-05) | Phase 2+ (业务增长后) |
| **核心理由** | 团队规模小(3-5人)、MVP快速交付、运维成本低 | 模块独立部署、技术异构、弹性伸缩 |
| **实现方式** | Maven Shade Plugin合并17个模块为单一JAR | 各模块独立Spring Boot应用+Nacos注册 |
| **优势** | 零网络开销、事务一致性简单、调试方便、部署单一 | 故障隔离、独立扩缩容、技术栈灵活 |
| **劣势** | 单点故障风险、代码耦合、全量部署 | 分布式事务复杂、运维成本高、调试困难 |
| **切换路径** | beijixing-app → 拆分为独立Spring Boot应用 → 引入Nacos/Gateway | — |

**决策结论**: 当前阶段采用**极简单体架构**，通过Maven模块化保持代码边界清晰。当单节点QPS > 1000 或团队 > 10人时，启动微服务拆分。

### ADR-002: MyBatis Plus vs JPA/Hibernate

**选择结果**: MyBatis Plus 3.5.5

| 评估维度 | MyBatis Plus | JPA/Hibernate |
|---------|-------------|---------------|
| SQL可控性 | ✅ 完全可控，支持复杂SQL | ❌ HQL/JPQL有局限，复杂查询需Native Query |
| 学习曲线 | ✅ 低，MyBatis生态成熟 | ⚠️ 中高，ORM概念较多 |
| 性能 | ✅ 轻量级，无代理开销 | ⚠️ 一级/二级缓存机制复杂 |
| 中文社区 | ✅ 极其活跃，文档完善 | ⚠️ 相对较少 |
| 与MariaDB兼容 | ✅ 原生支持 | ⚠️ 方言适配需额外配置 |
| 分页插件 | ✅ 内置PaginationInterceptor | ✅ Pageable支持良好 |
| 逻辑删除 | ✅ @TableLogic注解即用 | ✅ @Where注解 |
| 多租户行隔离 | ✅ TenantLineInnerInterceptor | ⚠️ 需自定义或用Hibernate Filter |
| 代码生成 | ✅ AutoGenerator强大 | ⚠️ 需第三方工具 |

**关键配置项** ([application.yml#L120-L132](backend/beijixing-app/src/main/resources/application.yml)):
```yaml
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.beijixing.*.entity
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

### ADR-003: Redis (Lettuce) vs Memcached

**选择结果**: Redis 6.0 + Lettuce客户端 + Redisson 3.24.3

| 能力 | Redis | Memcached |
|------|-------|-----------|
| 数据结构 | String/Hash/List/Set/ZSet/Stream | 仅Key-Value |
| 持久化 | RDB+AOF | 无 |
| 发布订阅 | ✅ Pub/Sub | ❌ |
| 分布式锁 | ✅ Redisson RedLock | ❌ 需外部协调 |
| 过期策略 | ✅ 精确TTL/LFU/LRU | ✅ LRU |
| 内存效率 | ⚠️ 有额外结构开销 | ✅ 更高效 |
| 连接池 | Lettucus (Netty异步) | Xmemcached |

**连接池配置** ([application.yml#L47-L60](backend/beijixing-app/src/main/resources/application.yml)):
```yaml
spring.data.redis:
  lettuce.pool:
    max-active: 50
    max-idle: 20
    min-idle: 10
    max-wait: 5000ms
```

### ADR-004: XXL-Job vs Quartz vs Spring Scheduled

**选择结果**: XXL-Job 2.4.0 (分布式任务调度)

| 特性 | XXL-Job | Quartz | Spring @Scheduled |
|------|---------|--------|-------------------|
| 分布式 | ✅ 原生支持 | ⚠️ 需自行实现 | ❌ 单机 |
| 可视化管理 | ✅ Admin Dashboard | ❌ 需自建 | ❌ 无 |
| 弹性扩容 | ✅ 路由策略(轮询/故障转移/分片) | ⚠️ 有限 | ❌ 不支持 |
| 失败重试 | ✅ 内置 | ⚠️ 需插件 | ❌ 无 |
| 日志追溯 | ✅ 执行日志持久化 | ⚠️ 需自建 | ❌ 无 |
| 运维友好度 | ★★★★★ | ★★☆☆☆ | ★☆☆☆☆ |
| 适用场景 | 大量定时任务/分片任务 | 传统Java EE | 简单周期任务 |

**当前集成方式**: 通过 `bx-schedule` 模块的 `@XxlJob` 注解接入，Admin端口 `8080/xxl-job-admin`。

### ADR-005: MariaDB vs MySQL vs PostgreSQL

**选择结果**: MariaDB 10.11 (MySQL完全兼容替代)

| 维度 | MariaDB 10.11 | MySQL 8.0 | PostgreSQL 16 |
|------|--------------|-----------|---------------|
| 开源协议 | GPL v2 (完全开源) | GPL v2 (Oracle商业双授权) | PostgreSQL License |
| MySQL兼容性 | ✅ 100%二进制兼容 | — | ❌ SQL方言差异大 |
| 性能 | ✅ InnoDB优化更好 | 标准 | ✅ 复杂查询更强 |
| 存储引擎 | InnoDB/XtraDB/ColumnStore | InnoDB | 多种 |
| JSON支持 | ✅ 完整 | ✅ 完整 | ✅ 更强(JSONB) |
| 中国社区 | ✅ 活跃 | ✅ 最活跃 | ⚠️ 较少 |
| 驱动版本 | mariadb-java-client 3.3.2 | mysql-connector-j | pgjdbc |

**连接池配置 (HikariCP)** ([application.yml#L37-L44](backend/beijixing-app/src/main/resources/application.yml)):
```yaml
spring.datasource.hikari:
  minimum-idle: 10
  maximum-pool-size: 50
  connection-timeout: 30000
  idle-timeout: 600000
  max-lifetime: 1800000
  pool-name: beijixing-hikari-pool
  leak-detection-threshold: 60000
```

### ADR-006: 其他关键技术决策汇总

| 决策项 | 选择 | 理由 |
|--------|------|------|
| API网关 | Spring Cloud Gateway | 响应式编程模型、Filter链可扩展 |
| JWT库 | JJWT (io.jsonwebtoken) | 轻量、Spring Boot 3兼容好 |
| API文档 | Knife4j (OpenAPI 3) | 国产增强Swagger、中文友好 |
| JSON库 | Fastjson2 + Jackson | Fastjson2高性能 + Jackson默认序列化 |
| Excel | EasyExcel | 阿里开源、流式读取避免OOM |
| 熔断降级 | Resilience4j | 替代Hystrix、Spring Boot 3原生集成 |
| 文件存储 | 腾讯云COS | 国内访问速度快、价格优惠 |
| 短信服务 | 腾讯云SMS | 国内覆盖广、SDK成熟 |
| AI模型 | 火山引擎(豆包)+DeepSeek | 双供应商策略、成本优化 |
| 前端构建 | Vite | 极速HMR、ESM原生支持 |

---

## 4. 核心模块说明

### 4.1 beijixing-app（主应用 - 极简单体聚合器）

**职责**: 将17个Maven子模块合并为单一可执行JAR，作为整个系统的唯一运行入口。

**启动类**: `com.beijixing.BeijixingAiApplication`
**打包方式**: Maven Shade Plugin (扁平化合并所有class文件)
**运行端口**: TCP 8080

**关键配置特性**:

| 配置项 | 值 | 说明 |
|--------|-----|------|
| `spring.main.allow-bean-definition-overriding` | true | 多模块Bean定义合并必需 |
| `spring.main.allow-circular-references` | true | 模块间循环引用容忍 |
| `server.tomcat.threads.max` | 200 | 最大工作线程数 |
| `server.tomcat.max-connections` | 1000 | 最大连接数 |
| `spring.cloud.nacos.*` | disabled | 禁用Spring Cloud/Nacos |
| `springdoc.swagger-ui.enabled` | false | 生产环境关闭API文档 |

**统一配置中心**: 所有模块共享 [application.yml](backend/beijixing-app/src/main/resources/application.yml)，包含：
- 数据源配置 (MariaDB)
- Redis配置 (Lettuce连接池)
- JWT认证参数
- AI模型配置 (火山引擎/DeepSeek)
- 支付渠道配置 (支付宝/微信)
- 合规规则配置 (各平台限额/敏感词)
- XXL-Job调度配置
- 风控规则配置
- 监控采集配置
- 文件上传配置
- 邮件/RabbitMQ/WebSocket配置

### 4.2 bx-common（公共基础库）

**位置**: [bx-common/](backend/bx-common/)
**包名**: `com.beijixing.common.core`

**核心组件**:

| 类名 | 职责 | 关键方法/字段 |
|------|------|-------------|
| [`Result<T>`](backend/bx-common/src/main/java/com/beijixing/common/core/Result.java) | 统一API响应封装 | `success()`, `error()`, code/message/data/timestamp/traceId |
| [`TenantContextHolder`](backend/bx-common/src/main/java/com/beijixing/common/core/TenantContextHolder.java) | 多租户上下文 (ThreadLocal) | `setTenantId()`, `getTenantId()`, `clear()`, `setIgnoreTenant()` |
| [`TenantContextFilter`](backend/bx-common/src/main/java/com/beijixing/common/core/TenantContextFilter.java) | 租户ID请求头解析Filter | 从 `X-Tenant-Id` Header提取并设置ThreadLocal |
| [`BxTenantLineHandler`](backend/bx-common/src/main/java/com/beijixing/common/core/BxTenantLineHandler.java) | MyBatis Plus租户行拦截器 | 自动在SQL追加 `tenant_id = ?` 条件 |
| [`TenantAwareEntity`](backend/bx-common/src/main/java/com/beijixing/common/core/TenantAwareEntity.java) | 租户感知实体基类 | 标记需要租户隔离的实体类 |
| [`CommonConstants`](backend/bx-common/src/main/java/com/beijixing/common/core/CommonConstants.java) | 系统常量定义 | `SYSTEM_NAME`, `TENANT_ID_HEADER`, `REQUEST_ID_HEADER` |

**依赖关系**: 被**所有**其他业务模块依赖，无反向依赖。

### 4.3 bx-user（用户认证授权）

**位置**: [bx-user/](backend/bx-user/)
**包名**: `com.beijixing.bxuser`
**核心能力**: 注册/登录/JWT签发/短信验证码/RBAC权限

**核心类**:

| 类名 | 类型 | 职责 |
|------|------|------|
| [`AuthController`](backend/bx-user/src/main/java/com/beijixing/bxuser/controller/AuthController.java) | Controller | 登录/注册/刷新Token/验证码接口 |
| [`UserController`](backend/bx-user/src/main/java/com/beijixing/bxuser/controller/UserController.java) | Controller | 用户信息CRUD/修改密码 |
| [`JwtUtils`](backend/bx-user/src/main/java/com/beijixing/bxuser/util/JwtUtils.java) | Util | Access Token + Refresh Token双Token机制 |
| [`TencentSmsService`](backend/bx-user/src/main/java/com/beijixing/bxuser/service/TencentSmsService.java) | Service | 腾讯云短信发送(验证码/通知) |
| [`EmailService`](backend/bx-user/src/main/java/com/beijixing/bxuser/service/EmailService.java) | Service | 邮件发送(QQ邮箱SMTP) |
| [`PasswordValidatorUtil`](backend/bx-user/src/main/java/com/beijixing/bxuser/util/PasswordValidatorUtil.java) | Util | 密码强度校验 |
| [`VerificationCodeUtil`](backend/bx-user/src/main/java/com/beijixing/bxuser/util/VerificationCodeUtil.java) | Util | 验证码生成/校验(Redis存储) |
| [`GeoLocationUtil`](backend/bx-user/src/main/java/com/beijixing/bxuser/util/GeoLocationUtil.java) | Util | IP地理位置解析 |
| [`DeviceFingerprintUtil`](backend/bx-user/src/main/java/com/beijixing/bxuser/util/DeviceFingerprintUtil.java) | Util | 设备指纹生成(防刷) |
| [`GenerateHash`](backend/bx-user/src/main/java/com/beijixing/bxuser/util/GenerateHash.java) | Util | 密码哈希(BCrypt) |
| [`TokenCleanupTask`](backend/bx-user/src/main/java/com/beijixing/bxuser/task/TokenCleanupTask.java) | Task | 过期Token清理定时任务 |

**实体模型**:

| 实体 | 表名 | 关键字段 |
|------|------|---------|
| [`User`](backend/bx-user/src/main/java/com/beijixing/bxuser/entity/User.java) | `user` | id, phone, password, nickname, roleType, status, tenantId |
| [`Role`](backend/bx-user/src/main/java/com/beijixing/bxuser/entity/Role.java) | `role` | id, roleName, roleKey, status |
| [`Permission`](backend/bx-user/src/main/java/com/beijixing/bxuser/entity/Permission.java) | `permission` | id, permName, permKey, menuType |
| [`UserRole`](backend/bx-user/src/main/java/com/beijixing/bxuser/entity/UserRole.java) | `user_role` | userId, roleId |
| [`RolePermission`](backend/bx-user/src/main/java/com/beijixing/bxuser/entity/RolePermission.java) | `role_permission` | roleId, permissionId |
| [`RefreshToken`](backend/bx-user/src/main/java/com/beijixing/bxuser/entity/RefreshToken.java) | `refresh_token` | userId, token, expiresAt |
| [`LoginLog`](backend/bx-user/src/main/java/com/beijixing/bxuser/entity/LoginLog.java) | `login_log` | userId, ip, userAgent, loginTime |
| [`DataPermission`](backend/bx-user/src/main/java/com/beijixing/bxuser/entity/DataPermission.java) | `data_permission` | 数据权限范围 |

**JWT双Token机制**:
```
登录成功 → 生成AccessToken(2h) + RefreshToken(7d)
         → AccessToken放入Response Header
         → RefreshToken存入DB(refresh_token表) + Redis缓存

API请求 → Header携带Bearer AccessToken
         → Gateway AuthFilter验证签名+过期时间
         → 有效→注入X-User-Id/X-Tenant-Id到下游

Token刷新 → 用RefreshToken换取新AccessToken
         → RefreshToken也接近过期→要求重新登录

安全措施 → RefreshToken单设备限制(每用户仅保留最新N条)
         → 异地登录检测(IP变化告警)
         → Token黑名单(Redis Set存储已注销Token)
```

### 4.4 bx-tenant（多租户管理）

**位置**: [bx-tenant/](backend/bx-tenant/)
**包名**: `com.beijixing.tenant`
**核心能力**: 租户CRUD/套餐管理/资源配额/邀请奖励

**核心类**:

| 类名 | 类型 | 职责 |
|------|------|------|
| [`TenantController`](backend/bx-tenant/src/main/java/com/beijixing/tenant/controller/TenantController.java) | Controller | 租户信息/审核/状态变更 |
| [`PackageController`](backend/bx-tenant/src/main/java/com/beijixing/tenant/controller/PackageController.java) | Controller | 套餐升级/变更/续费 |
| [`TenantServiceImpl`](backend/bx-tenant/src/main/java/com/beijixing/tenant/service/impl/TenantServiceImpl.java) | Service | 租户生命周期管理 |
| [`PackageServiceImpl`](backend/bx-tenant/src/main/java/com/beijixing/tenant/service/impl/PackageServiceImpl.java) | Service | 套餐计算/配额分配 |
| [`TenantResourceQuotaService`](backend/bx-tenant/src/main/java/com/beijixing/tenant/service/TenantResourceQuotaService.java) | Service | 资源配额检查/扣减 |
| [`QuotaResetJob`](backend/bx-tenant/src/main/java/com/beijixing/tenant/job/QuotaResetJob.java) | Job | 月度配额重置(XXL-Job) |

**实体模型**:

| 实体 | 表名 | 关键字段 |
|------|------|---------|
| [`Tenant`](backend/bx-tenant/src/main/java/com/beijixing/tenant/entity/Tenant.java) | `sys_tenant` | id(雪花), tenantCode, tenantName, status, riskLevel, packageType, pointBalance |
| [`TenantPackage`](backend/bx-tenant/src/main/java/com/beijixing/tenant/entity/TenantPackage.java) | `tenant_package` | packageType, price, durationDays, featureList |
| [`TenantConfig`](backend/bx-tenant/src/main/java/com/beijixing/tenant/entity/TenantConfig.java) | `tenant_config` | 自定义配置项(key-value) |
| [`TenantResourceQuota`](backend/bx-tenant/src/main/java/com/beijixing/tenant/entity/TenantResourceQuota.java) | `tenant_resource_quota` | resourceType, limit, used, resetDate |
| [`InviteReward`](backend/bx-tenant/src/main/java/com/beijixing/tenant/entity/InviteReward.java) | `invite_reward` | inviterId, inviteeId, rewardPoints, status |

**枚举类型**:
- [`TenantStatus`](backend/bx-tenant/src/main/java/com/beijixing/tenant/enums/TenantStatus.java): PENDING(待审核) → NORMAL(正常) → DISABLED(禁用) → CANCELED(注销)
- [`PackageType`](backend/bx-tenant/src/main/java/com/beijixing/tenant/enums/PackageType.java): BASIC / ADVANCED / ANNUAL / LIFETIME

**套餐体系**:

| 套餐 | 月费(元) | AI生成次数/月 | 社交账号数 | 商机容量 | 数据导出 |
|------|---------|-------------|----------|---------|---------|
| Basic | 299 | 500 | 3 | 1,000 | ❌ |
| Advanced | 999 | 2000 | 10 | 10,000 | ✅ |
| Annual | 7999 | Unlimited | 30 | 50,000 | ✅ |
| Lifetime | 29999 | Unlimited | Unlimited | Unlimited | ✅ |

### 4.5 bx-lead（商机管理 CRM）

**位置**: [bx-lead/](backend/bx-lead/)
**包名**: `com.beijixing.bxlead`
**核心能力**: 线索录入/意向评分/AI分析/跟进管理/漏斗统计

**核心类**:

| 类名 | 类型 | 职责 |
|------|------|------|
| [`LeadController`](backend/bx-lead/src/main/java/com/beijixing/bxlead/controller/LeadController.java) | Controller | 商机CRUD/列表查询/详情 |
| [`LeadFollowUpController`](backend/bx-lead/src/main/java/com/beijixing/bxlead/controller/LeadFollowUpController.java) | Controller | 跟进记录管理 |
| [`LeadTaskController`](backend/bx-lead/src/main/java/com/beijixing/bxlead/controller/LeadTaskController.java) | Controller | 截客/获客任务管理 |
| [`LeadAnalysisController`](backend/bx-lead/src/main/java/com/beijixing/bxlead/controller/LeadAnalysisController.java) | Controller | AI意向分析/漏斗统计 |
| [`InterceptController`](backend/bx-lead/src/main/java/com/beijixing/bxlead/controller/InterceptController.java) | Controller | 截客来源配置 |
| [`LeadServiceImpl`](backend/bx-lead/src/main/java/com/beijixing/bxlead/service/impl/LeadServiceImpl.java) | Service | 商机CRUD业务逻辑 |
| [`LeadFollowUpServiceImpl`](backend/bx-lead/src/main/java/com/beijixing/bxlead/service/impl/LeadFollowUpServiceImpl.java) | Service | 跟进记录业务逻辑 |
| [`LeadAnalysisServiceImpl`](backend/bx-lead/src/main/java/com/beijixing/bxlead/service/impl/LeadAnalysisServiceImpl.java) | Service | 商机统计分析 |
| [`AiIntentAnalysisServiceImpl`](backend/bx-lead/src/main/java/com/beijixing/bxlead/service/impl/AiIntentAnalysisServiceImpl.java) | Service | AI意图识别(调用bx-ai) |
| [`InterceptJob`](backend/bx-lead/src/main/java/com/beijixing/bxlead/job/InterceptJob.java) | Job | 定时截客任务(XXL-Job) |
| [`XxlJobHandler`](backend/bx-lead/src/main/java/com/beijixing/bxlead/job/XxlJobHandler.java) | Job | XXL-Job任务处理器 |

**实体模型**:

| 实体 | 表名 | 关键字段 |
|------|------|---------|
| [`Lead`](backend/bx-lead/src/main/java/com/beijixing/bxlead/entity/Lead.java) | `bx_lead` | leadNo, title, source, channel, customerName, budgetAmount, status, intentScore, level, ownerId, aiAnalysisResult |
| [`LeadFollowUp`](backend/bx-lead/src/main/java/com/beijixing/bxlead/entity/LeadFollowUp.java) | `bx_lead_follow_up` | leadId, followType, content, nextFollowTime |
| [`LeadStatusHistory`](backend/bx-lead/src/main/java/com/beijixing/bxlead/entity/LeadStatusHistory.java) | `bx_lead_status_history` | leadId, fromStatus, toStatus, operatorId |
| [`InterceptSource`](backend/bx-lead/src/main/java/com/beijixing/bxlead/entity/InterceptSource.java) | `bx_intercept_source` | platformType, accountId, keywords, enabled |

**商机状态机** (`[LeadStatus](backend/bx-lead/src/main/java/com/beijixing/bxlead/enums/LeadStatus.java)`):

```
NEW(新建) → FOLLOWING(跟进中) → QUOTED(已报价) → NEGOTIATION(谈判中) → WON(成交)
                                        ↕                         ↕
                                   LOST(失败)                LOST(失败)
```

**商机等级** (`[LeadLevel](backend/bx-lead/src/main/java/com/beijixing/bxlead/enums/LeadLevel.java)`):
- **A**: 高意向(80-100分) → 24h内必须跟进
- **B**: 中意向(60-79分) → 48h内跟进
- **C**: 低意向(40-59分) → 每周跟进一次
- **D**: 信息不全(<40分) → 待补充确认

### 4.6 bx-content（内容管理与AI创作）

**位置**: [bx-content/](backend/bx-content/)
**核心能力**: AI文案生成/多平台发布/内容模板/素材管理/发布排期

**主要功能**:
- AI驱动的营销文案生成（标题/正文/海报文案）
- 一键多平台分发（微信/微博/抖音/小红书/B站）
- 内容素材库管理（图片/视频/文档）
- 发布日历与排期管理
- 内容效果数据回流

### 4.7 bx-social（社交爬虫 + 合规引擎）

**位置**: [bx-social/](backend/bx-social/)
**核心能力**: 社交平台OAuth/私信自动化/合规检查/频率控制/截客引擎

**合规规则引擎** (Phase 1基线):

| 平台 | 日私信上限 | 单用户上限 | 最小间隔 | 时间窗口 | 账号类型要求 |
|------|----------|----------|---------|---------|------------|
| 抖音(DOUYIN) | 50 | 3 | 60s | 09:00-21:00 | enterprise/staff |
| 小红书(XIAOHONGSHU) | 20 | 5 | 120s | 09:00-21:00 | enterprise/professional |
| 快手(KUAISHOU) | 30 | 3 | 90s | 09:00-21:00 | enterprise |
| 微信(WECHAT) | 100 | 10 | 30s | 08:00-22:00 | service/subscription |

**合规检查链路**:
```
发送请求 → 敏感词检测(句易网API/本地词库)
         → 频率限制检查(Redis滑动窗口)
         → 时间窗口检查(非工作时间拦截)
         → 内容相似度检测(Simhash防重复)
         → 平台规则校验(OAuth Scope/账号类型)
         → 全部通过 → 发送 / 任一不通过 → 拦截并记录
```

### 4.8 bx-billing（计费支付）

**位置**: [bx-billing/]
**核心能力**: 套餐订购/充值消费/发票管理/对账结算/余额预警

**支持的支付渠道**:
- 支付宝 (当面付/手机网站支付)
- 微信支付 (JSAPI/H5支付)
- 企业对公转账(线下)

### 4.9 bx-ai（AI模型调度）

**位置**: [bx-ai/](backend/bx-ai/)
**核心能力**: LLM模型调度/Prompt模板管理/Token计量/模型路由

**AI供应商配置**:

| 供应商 | 模型 | Endpoint | 用途 | 成本参考 |
|--------|------|----------|------|---------|
| 火山引擎(豆包) | ep-20240115142400-lmzjx | ark.cn-beijing.volces.com | 文案生成/改写 | ~¥0.008/千tokens |
| DeepSeek | deepseek-chat | api.deepseek.com | 意图分析/摘要 | ~¥0.001/千tokens |

**模型路由策略**:
- 默认使用火山引擎（响应速度优先）
- 火山引擎不可用时自动Failover至DeepSeek
- 长文本分析场景优先DeepSeek（128K上下文）

### 4.10 其他模块速查

| 模块 | 包名 | 核心职责 | 关键依赖 |
|------|------|---------|---------|
| **bx-message** | com.beijixing.message | 站内信/WebSocket实时推送/邮件通知 | RabbitMQ, WebSocket |
| **bx-storage** | com.beijixing.storage | 文件上传下载/COS集成/缩略图生成 | 腾讯云COS SDK |
| **bx-schedule** | com.beijixing.schedule | XXL-Job Executor/任务注册/执行日志 | xxl-job-core |
| **bx-monitor** | com.beijixing.monitor | 系统指标采集/健康检查/告警通知 | Micrometer, Prometheus |
| **bx-system** | com.beijixing.system | 字典管理/文件管理/操作日志/定时任务管理 | MyBatis Plus |
| **bx-data** | com.beijixing.data | Dashboard数据/报表统计/趋势分析 | Redis聚合缓存 |
| **bx-task** | com.beijixing.bxtask | 任务状态管理/异步任务追踪 | — |
| **bx-gateway** | com.beijixing.gateway | API路由/JWT认证/限流/日志 | Spring Cloud Gateway |
| **bx-risk** | com.beijixing.risk | 风控规则引擎/IP黑名单/行为分析 | Redis, Rule Engine |

---

## 5. 数据库设计

### 5.1 ER关系概览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          beijixing_ai (统一数据库)                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────┐     ┌──────────┐     ┌──────────────────┐                    │
│  │ sys_tenant│────│  user    │────│ user_role         │                    │
│  │ (租户)    │1:N │ (用户)   │1:N │ (用户-角色关联)    │                    │
│  └──────────┘     └────┬─────┘     └────────┬─────────┘                    │
│                        │                    │                                │
│                 ┌──────┴──────┐     ┌───────┴────────┐                       │
│                 │ role        │────│role_permission  │                       │
│                 │ (角色)      │1:N │ (角色-权限)     │                       │
│                 └─────────────┘     └────────────────┘                       │
│                                                                             │
│  ┌──────────────────────────────────────────────────────────────────┐       │
│  │                        业务核心区域                               │       │
│  ├──────────┐  ┌──────────┐  ┌──────────┐  ┌────────────────────┐   │       │
│  │ bx_lead  │  │bx_lead_  │  │bx_inter- │  │bx_content          │   │       │
│  │ (商机)   │──│follow_up │  │cept_src  │  │ (内容)             │   │       │
│  └──────────┘  └──────────┘  └──────────┘  └────────────────────┘   │       │
│                                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────────────────┐   │       │
│  │tenant_   │  │tenant_   │  │billing_  │  │bx_social_account   │   │       │
│  │package   │  │resource_ │  │order     │  │ (社交账号)          │   │       │
│  │ (套餐)   │  │quota     │  │ (订单)   │  │                     │   │       │
│  └──────────┘  └──────────┘  └──────────┘  └────────────────────┘   │       │
│                                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────────────────┐   │       │
│  │sys_config│  │sys_dict  │  │sys_oper_ │  │sys_file            │   │       │
│  │ (系统配置)│  │ (字典)   │  │log       │  │ (文件)             │   │       │
│  └──────────┘  └──────────┘  └──────────┘  └────────────────────┘   │       │
│                                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                               │
│  │refresh_  │  │login_log │  │alert_    │                               │
│  │token     │  │ (登录日志)│  │record    │                               │
│  └──────────┘  └──────────┘  └──────────┘                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 核心表结构

#### 用户与权限表组

```sql
-- 用户主表
CREATE TABLE user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone       VARCHAR(20)  NOT NULL UNIQUE COMMENT '手机号(登录名)',
    password    VARCHAR(255) NOT NULL COMMENT 'BCrypt哈希',
    nickname    VARCHAR(50)  DEFAULT '' COMMENT '昵称',
    real_name   VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    avatar      VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    role_type   TINYINT      DEFAULT 0 COMMENT '角色类型: 0普通 1管理员 2超级管理员',
    status      TINYINT      DEFAULT 1 COMMENT '状态: 0禁用 1正常',
    tenant_id   BIGINT       DEFAULT NULL COMMENT '所属租户ID',
    last_login_time DATETIME  DEFAULT NULL COMMENT '最后登录时间',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_status (tenant_id, status),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 角色表
CREATE TABLE role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name   VARCHAR(50)  NOT NULL COMMENT '角色名称',
    role_key    VARCHAR(50)  NOT NULL UNIQUE COMMENT '角色标识',
    status      TINYINT      DEFAULT 1 COMMENT '状态',
    remark      VARCHAR(255) DEFAULT NULL,
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 权限表
CREATE TABLE permission (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    perm_name   VARCHAR(100) NOT NULL COMMENT '权限名称',
    perm_key    VARCHAR(100) NOT NULL COMMENT '权限标识(menu:*:*格式)',
    menu_type   TINYINT DEFAULT 0 COMMENT '菜单类型: 0目录 1菜单 2按钮',
    parent_id   BIGINT DEFAULT 0 COMMENT '父级ID',
    sort_order  INT DEFAULT 0 COMMENT '排序',
    status      TINYINT DEFAULT 1,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';
```

#### 商机核心表

```sql
-- 商机主表 (对应 Lead 实体)
CREATE TABLE bx_lead (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    lead_no             VARCHAR(32)  NOT NULL UNIQUE COMMENT '商机编号(BJX+时间戳+随机)',
    title               VARCHAR(200) NOT NULL COMMENT '商机标题',
    source              VARCHAR(30)  DEFAULT NULL COMMENT '来源: MANUAL/INTERCEPT/IMPORT/AI',
    channel             VARCHAR(50)  DEFAULT NULL COMMENT '来源渠道',
    customer_name       VARCHAR(100) DEFAULT NULL COMMENT '客户名称',
    customer_phone      VARCHAR(20)  DEFAULT NULL COMMENT '客户电话(加密存储)',
    customer_email      VARCHAR(100) DEFAULT NULL COMMENT '客户邮箱',
    customer_company    VARCHAR(200) DEFAULT NULL COMMENT '客户公司',
    industry            VARCHAR(50)  DEFAULT NULL COMMENT '所属行业',
    region              VARCHAR(50)  DEFAULT NULL COMMENT '所在地区',
    requirement_desc    TEXT         DEFAULT NULL COMMENT '需求描述',
    budget_amount       DECIMAL(12,2) DEFAULT NULL COMMENT '预算金额',
    expected_deal_time  DATETIME     DEFAULT NULL COMMENT '预计成交时间',
    status              VARCHAR(20)  DEFAULT 'NEW' COMMENT '商机状态',
    intent_score        INT          DEFAULT 0 COMMENT '意向评分(0-100)',
    level               VARCHAR(5)   DEFAULT 'D' COMMENT '等级:A/B/C/D',
    owner_id            BIGINT       DEFAULT NULL COMMENT '负责人ID',
    owner_name          VARCHAR(50)  DEFAULT NULL COMMENT '负责人名称',
    assign_type         VARCHAR(20)  DEFAULT NULL COMMENT '分配方式: MANUAL/RULE/AI',
    assign_time         DATETIME     DEFAULT NULL COMMENT '分配时间',
    competitor_keywords  VARCHAR(500) DEFAULT NULL COMMENT '竞品关键词',
    intercept_source_id  BIGINT       DEFAULT NULL COMMENT '截客来源ID',
    ai_analysis_result  TEXT         DEFAULT NULL COMMENT 'AI分析结果(JSON)',
    follow_count        INT          DEFAULT 0 COMMENT '跟进次数',
    last_follow_time    DATETIME     DEFAULT NULL COMMENT '最后跟进时间',
    remark              TEXT         DEFAULT NULL COMMENT '备注',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by           BIGINT       DEFAULT NULL,
    update_by           BIGINT       DEFAULT NULL,
    deleted             TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_owner_status (owner_id, status),
    INDEX idx_tenant_create (tenant_id, create_time),
    INDEX idx_level_intent (level, intent_score),
    INDEX idx_source_channel (source, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商机主表';

-- 商机跟进记录表
CREATE TABLE bx_lead_follow_up (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    lead_id         BIGINT       NOT NULL COMMENT '商机ID',
    follow_type     VARCHAR(20)  NOT NULL COMMENT '跟进类型: PHONE/WECHAT/MEET/EMAIL/OTHER',
    content         TEXT         NOT NULL COMMENT '跟进内容',
    next_follow_time DATETIME    DEFAULT NULL COMMENT '下次跟进时间',
    follow_result   VARCHAR(20)  DEFAULT NULL COMMENT '跟进结果',
    creator_id      BIGINT       NOT NULL COMMENT '跟进人ID',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_lead_time (lead_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商机跟进记录表';
```

#### 租户表

```sql
-- 租户主表 (雪花算法ID)
CREATE TABLE sys_tenant (
    id                  BIGINT NOT NULL PRIMARY KEY COMMENT '雪花ID',
    tenant_code         VARCHAR(32)  NOT NULL UNIQUE COMMENT '租户编码',
    tenant_name         VARCHAR(100) NOT NULL COMMENT '企业名称',
    industry            VARCHAR(50)  DEFAULT NULL COMMENT '行业',
    contact_name        VARCHAR(50)  DEFAULT NULL COMMENT '联系人',
    contact_phone       VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
    contact_email       VARCHAR(100) DEFAULT NULL COMMENT '联系邮箱',
    business_license    VARCHAR(50)  DEFAULT NULL COMMENT '营业执照号',
    license_image       VARCHAR(500) DEFAULT NULL COMMENT '执照图片URL',
    status              TINYINT      DEFAULT 0 COMMENT '0待审核 1正常 2禁用 3注销',
    risk_level          TINYINT      DEFAULT 1 COMMENT '风控等级: 1低 2中 3高',
    package_type        VARCHAR(20)  DEFAULT 'basic' COMMENT '套餐类型',
    package_expire_time DATETIME    DEFAULT NULL COMMENT '套餐到期时间',
    point_balance       DECIMAL(10,2) DEFAULT 0.00 COMMENT '积分余额',
    total_consumption   DECIMAL(12,2) DEFAULT 0.00 COMMENT '累计消费',
    inviter_id          BIGINT       DEFAULT NULL COMMENT '邀请人ID',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by           BIGINT       DEFAULT NULL,
    update_by           BIGINT       DEFAULT NULL,
    deleted             TINYINT      DEFAULT 0,
    INDEX idx_status_package (status, package_type),
    INDEX idx_contact_phone (contact_phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户表';
```

### 5.3 数据分区策略

**当前阶段**: 单库单表，暂不需要分区。以下为预留策略：

| 表 | 预估数据量 | 分区策略建议 | 分区键 | 触发条件 |
|----|----------|------------|--------|---------|
| `bx_lead` | 100万+/年 | RANGE按月分区 | `create_time` | 单表 > 500万行 |
| `login_log` | 1000万+/年 | RANGE按月分区 | `login_time` | 单表 > 2000万行 |
| `sys_oper_log` | 500万+/年 | RANGE按月分区 | `oper_time` | 单表 > 1000万行 |
| `bx_lead_follow_up` | 300万+/年 | RANGE按月分区 | `create_time` | 单表 > 500万行 |
| `billing_order` | 200万+/年 | HASH按租户分区 | `tenant_id` | 单表 > 300万行 |

### 5.4 索引优化建议

**已有索引** (基于实体类@TableName推断):
- `user`: idx_phone(UNIQUE), idx_tenant_status
- `bx_lead`: idx_owner_status, idx_tenant_create, idx_level_intent, idx_source_channel
- `sys_tenant`: idx_status_package, idx_contact_phone

**推荐新增索引**:

```sql
-- 商机表复合索引(常用查询: 按负责人+状态+时间范围筛选)
ALTER TABLE bx_lead ADD INDEX idx_owner_status_time (owner_id, status, create_time);

-- 商机表客户搜索索引
ALTER TABLE bx_lead ADD INDEX idx_customer_search (customer_name, customer_phone);

-- 登录日志IP+时间索引(用于风控分析)
ALTER TABLE login_log ADD INDEX idx_ip_time (ip, login_time);

-- 操作日志用户+时间索引
ALTER TABLE sys_oper_log ADD INDEX idx_user_time (oper_user_id, oper_time);

-- 刷新Token用户+过期时间索引(Token清理任务)
ALTER TABLE refresh_token ADD INDEX idx_user_expires (user_id, expires_at);
```

---

## 6. 缓存策略

### 6.1 Redis数据结构设计

```
Redis Database 0 (beijixing-ai):
│
├── 认证相关 (TTL: 动态)
│   ├── {BX}:auth:token:{userId}                    → Hash (AccessToken信息)
│   ├── {BX}:auth:refresh:{tokenId}                 → String (RefreshToken)
│   ├── {BX}:auth:blacklist:{tokenJti}               → String (Token黑名单, TTL=剩余有效期)
│   ├── {BX}:auth:verify-code:{phone}               → String (短信验证码, TTL=300s)
│   └── {BX}:auth:device-fingerprint:{deviceId}      → Hash (设备指纹)
│
├── 租户相关 (TTL: 3600s)
│   ├── {BX}:tenant:info:{tenantId}                  → Hash (租户基本信息)
│   ├── {BX}:tenant:quota:{tenantId}:{resourceType}  → String (资源配额剩余)
│   └── {BX}:tenant:config:{tenantId}                → Hash (租户自定义配置)
│
├── 业务数据 (TTL: 可变)
│   ├── {BX}:lead:intent-cache:{leadId}              → Hash (AI意图分析缓存, TTL=86400s)
│   ├── {BX}:content:generate:{taskId}               → String (AI生成任务结果)
│   ├── {BX}:social:rate-limit:{accountId}:{date}    → String (每日计数器)
│   ├── {BX}:social:msg-interval:{accountId}          → ZSet (消息间隔控制, score=timestamp)
│   └── {BX}:billing:balance:{tenantId}               → String (余额缓存, TTL=300s)
│
├── 系统配置 (TTL: 1800s)
│   ├── {BX}:sys:dict:{dictType}                      → Hash (字典数据)
│   ├── {BX}:sys:config:{configKey}                  → String (系统配置)
│   └── {BX}:compliance:sensitive-result:{textHash}   → String (敏感词检测结果, TTL=3600s)
│
├── 分布式锁
│   ├── {BX}:lock:lead-assign:{batchId}               → Redisson RLock
│   ├── {BX}:lock:billing-consume:{orderId}           → Redisson RLock
│   └── {BX}:lock:schedule-task:{jobId}               → Redisson RLock
│
└── 监控指标 (TTL: 300s)
    ├── {BX}:monitor:metrics:system:{timestamp}       → Hash (系统指标)
    ├── {BX}:monitor:metrics:app:{timestamp}          → Hash (应用指标)
    └── {BX}:monitor:metrics:business:{timestamp}     → Hash (业务指标)
```

**Key命名规范**: `{BX}:{模块}:{用途}:{标识}` → 例: `bx:auth:token:10086`

### 6.2 缓存穿透防护

| 场景 | 防护策略 | 实现 |
|------|---------|------|
| 查询不存在的数据 | 布隆过滤器 + 空值缓存 | Redis BitMap + 缓存空字符串(TTL=60s) |
| 恶意请求不存在的ID | 参数白名单校验 | Controller层 `@Validated` + 业务校验 |
| 热点Key重建 | 分布式式互斥锁 | Redisson `tryLock()` + 重建后双写 |

### 6.3 缓存雪崩防护

| 策略 | 实现方式 |
|------|---------|
| TTL随机偏移 | 基础TTL ± 随机偏移(0~基础TTL*10%) |
| 多级缓存 | L1本地Caffeine(热点) + L2 Redis(全量) |
| 永久热点Key | 字典/配置类数据设置较长TTL + 主动失效 |
| 熔断降级 | Resilience4j CircuitBreaker → 缓存不可用时直连DB |

### 6.4 缓存击穿防护

| 策略 | 实现方式 |
|------|---------|
| 热点Key互斥锁 | Redisson `getLock("cache:rebuild:" + key)` |
| 逻辑过期 | Value中嵌入逻辑过期时间, 异步线程重建 |
| 永不过期 + 异步更新 | 热点数据设TTL=-1, 定时任务异步刷新 |

### 6.5 缓存更新策略

```
┌─────────────────────────────────────────────────────┐
│                 缓存更新策略矩阵                      │
├──────────────┬──────────┬──────────┬────────────────┤
│ 数据类型     │ 更新模式  │ 一致性   │ 适用场景       │
├──────────────┼──────────┼──────────┼────────────────┤
│ 用户信息     │ Write-Through │ 强一致  │ 登录/修改资料  │
│ 租户配额     │ Cache-Aside (Write) │ 最终一致  │ 扣减配额      │
│ 字典/配置    │ Cache-Aside (Read) + 定时刷新 │ 最终一致  │ 系统配置      │
│ AI生成结果   │ Write-Behind (MQ异步) │ 最终一致  │ 耗时操作      │
│ 商机统计数据 │ 定时预计算写入 │ 弱一致  │ Dashboard    │
│ 敏感词检测结果│ Cache-Aside (Read) │ 最终一致  │ 合规检查      │
└──────────────┴──────────┴──────────┴────────────────┘
```

---

## 7. 安全设计

### 7.1 认证机制（JWT + Token刷新）

**完整认证流程**:

```
┌──────────┐                                ┌──────────┐
│  Client  │                                │  Server  │
└─────┬────┘                                └─────┬────┘
      │ ① POST /api/auth/login                  │
      │ {phone, password, captcha}              │
      │────────────────────────────────────────►│
      │                                         │
      │                          ② 校验密码(BCrypt)
      │                          ③ 生成Token对:
      │                            AccessToken (2h)
      │                            RefreshToken (7d)
      │                          ④ RefreshToken存DB+Redis
      │                                         │
      │ ⑤ {accessToken, refreshToken, expiresIn} │
      │◄────────────────────────────────────────│
      │                                         │
      │ ⑥ 后续请求 Header: Bearer {accessToken} │
      │────────────────────────────────────────►│
      │                                         │
      │                          ⑦ Gateway AuthFilter
      │                          验证签名+过期时间
      │                          提取userId/tenantId
      │                          注入下游Header
      │                                         │
      │ ⑧ Response (X-Trace-Id)                 │
      │◄────────────────────────────────────────│
      │                                         │
      │ ⑨ Token即将过期(≤10min)                 │
      │ POST /api/auth/refresh                  │
      │ {refreshToken}                          │
      │────────────────────────────────────────►│
      │                                         │
      │                          ⑩ 验证RefreshToken
      │                          签发新Token对
      │                          作废旧RefreshToken
      │                                         │
      │ ⑪ 新Token对                             │
      │◄────────────────────────────────────────│
```

**安全增强措施**:
- Token使用 **HMAC-SHA256** 签名 (JJWT库)
- RefreshToken **单设备限制** (每用户最多N条有效)
- **异地登录检测** (IP变化触发安全告警)
- **密码错误次数限制** (5次/15分钟 → 账户锁定)
- **验证码防暴力破解** (图形验证码 + 短信验证码双重验证)
- **设备指纹绑定** (异常设备需二次验证)

### 7.2 授权模型（RBAC）

```
┌────────────────────────────────────────────────────────┐
│                    RBAC权限模型                         │
│                                                        │
│  User ──N:1──► Role ──N:1──► Permission               │
│                                                        │
│  角色(Role):                                            │
│  ├── SUPER_ADMIN  (超级管理员 - 全部权限)               │
│  ├── TENANT_ADMIN  (租户管理员 - 租户内全部权限)         │
│  ├── OPERATOR      (运营人员 - 内容/商机/数据只读+部分写)│
│  ├── SALESMAN      (销售人员 - 商机CRUD+跟进)           │
│  └── MEMBER        (普通成员 - 只读个人数据)            │
│                                                        │
│  权限(Permission) 格式: {模块}:{资源}:{操作}            │
│  ├── system:user:list     (系统:用户:列表)              │
│  ├── system:user:create   (系统:用户:创建)              │
│  ├── lead:own:edit        (商机:我的:编辑)              │
│  ├── lead:all:view        (商机:全部:查看)              │
│  ├── content:publish      (内容:发布)                   │
│  └── billing:recharge     (计费:充值)                   │
│                                                        │
│  数据权限(Data Permission):                             │
│  ├── ALL_DATA       (全部数据)                          │
│  ├── DEPT_DATA      (本部门数据)                        │
│  ├── DEPT_AND_CHILD (本部门及下级)                      │
│  ├── SELF_ONLY      (仅本人数据)                        │
│  └── CUSTOMIZE      (自定义)                            │
└────────────────────────────────────────────────────────┘
```

### 7.3 敏感词过滤

**双层检测机制**:

| 层级 | 方式 | 覆盖范围 | 延迟 | 成本 |
|------|------|---------|------|------|
| L1 本地词库 | DFA算法(前缀树) | 政治/涉黄/暴恐/违禁品 | <1ms | 免费 |
| L2 云端API | 句易网API | 最新法规/平台特定规则 | 50-200ms | 按量付费 |

**当前状态**: L1已启用，L2待申请API Key后启用。
**替换策略**: 检测到的敏感词替换为 `*` (可配置)。
**缓存策略**: 同一文本检测结果缓存1小时 (基于文本SimHash去重)。

### 7.4 风控规则引擎

**内置规则集** (配置于 `risk.rules.*`):

| 规则 | 类型 | 参数 | 动作 |
|------|------|------|------|
| 登录频率限制 | 滑动窗口 | 5次/15分钟/IP | CAPTCHA → LOCK |
| IP黑名单 | 实时匹配 | 每5分钟同步 | REJECT |
| 敏感操作 | 行为检测 | 改密/改绑/提现 | 二次验证(MFA) |
| 设备异常 | 指纹分析 | 新设备/异地 | 短信验证 |
| 金额异常 | 阈值检测 | 单笔>5000/日累计>20000 | 人工审核 |
| 批量操作 | 频率检测 | >50次/分钟 | RATE_LIMIT |

**风控处置动作**:
- `PASS` - 正常放行
- `CAPTCHA` - 要求输入验证码
- `VERIFY` - 要求短信/邮箱二次验证
- `REJECT` - 直接拒绝请求
- `LOCK` - 锁定账户/IP
- `REVIEW` - 转人工审核

### 7.5 数据脱敏方案

| 字段类型 | 脱敏规则 | 示例 |
|---------|---------|------|
| 手机号 | 中间4位掩码 | 138****5678 |
| 身份证号 | 保留前3后4 | 110***********1234 |
| 银行卡号 | 保留后4位 | ************8888 |
| 邮箱 | @前部分掩码 | r***@example.com |
| 姓名 | 首字可见+掩码 | 张** |
| 地址 | 详细地址隐藏 | 北京市朝阳区**** |

**实现方式**: 基于 Jackson `JsonSerializer` 自定义注解 `@Desensitize(type = DesensitizeType.PHONE)`，在API响应序列化时自动脱敏。

---

## 8. 可观测性设计

### 8.1 日志规范

**日志格式** (Logback Pattern):

```
%{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} [%X{traceId}] - %msg%n
```

**MDC (Mapped Diagnostic Context) 上下文字段**:

| 字段 | 来源 | 用途 |
|------|------|------|
| traceId | Gateway LogFilter生成(UUID) | 全链路追踪ID |
| userId | JWT解析 | 操作人标识 |
| tenantId | JWT解析/Header | 租户隔离标识 |
| clientIp | Request获取 | 来源IP |
| requestUri | Request获取 | 请求路径 |
| method | Request获取 | HTTP Method |
| latency | Filter计时 | 请求耗时(ms) |

**日志级别规范**:

| 级别 | 使用场景 | 示例 |
|------|---------|------|
| ERROR | 系统故障/不可恢复错误 | DB连接失败/外部API超时 |
| WARN | 可恢复异常/需要关注 | Token即将过期/配额不足 |
| INFO | 关键业务操作 | 用户登录/订单创建/支付完成 |
| DEBUG | 开发调试信息 | SQL参数/第三方API请求/响应 |
| TRACE | 最细粒度跟踪 | 方法入参出参/循环迭代 |

**日志输出目标**:
- 开发环境: Console (stdout)
- 生产环境: File + Console (滚动文件, 单文件最大200MB, 保留30天)

### 8.2 监控指标（Prometheus + Micrometer）

**Actuator暴露端点**: `/actuator/prometheus`

**核心指标分类**:

```
┌─────────────────────────────────────────────────────────────┐
│                    监控指标体系                              │
├─────────────┬───────────────────────────────────────────────┤
│ JVM指标     │ jvm_memory_used_bytes{area="heap"}            │
│             │ jvm_gc_pause_seconds_count{gc="G1 Young Gen"} │
│             │ jvm_threads_live_threads                      │
│             │ jvm_buffer_pool_used_bytes{pool="direct"}     │
├─────────────┼───────────────────────────────────────────────┤
│ Tomcat指标  │ tomcat_threads_active{port="8080"}            │
│             │ tomcat_sessions_active_max                   │
│             │ tomcat_requests_seconds_sum{uri="/api/..."}   │
├─────────────┼───────────────────────────────────────────────┤
│ DB连接池    │ hikaricp_connections_active{pool="..."}       │
│             │ hikaricp_connections_pending                 │
│             │ hikaricp_connections_max                     │
├─────────────┼───────────────────────────────────────────────┤
│ Redis指标   │ redis_commands_total{command="get",...}      │
│             │ redis_connections_active                    │
│             │ redis_memory_used_bytes                      │
├─────────────┼───────────────────────────────────────────────┤
│ 业务指标(自定义)│ bx_leads_created_total{source="INTERCEPT"}│
│             │ bx_auth_login_total{result="success"}        │
│             │ bx_ai_model_calls_total{provider="volcengine"}│
│             │ bx_content_publish_total{platform="douyin"}  │
│             │ bx_billing_order_amount_sum{type="recharge"} │
└─────────────┴───────────────────────────────────────────────┘
```

**指标采集器** (bx-monitor模块):

| 采集器 | 采集频率 | 数据源 |
|--------|---------|--------|
| [`SystemMetricsCollector`](backend/bx-monitor/src/main/java/com/beijixing/monitor/collector/SystemMetricsCollector.java) | 30s | OS (CPU/内存/磁盘/负载) |
| [`AppMetricsCollector`](backend/bx-monitor/src/main/java/com/beijixing/monitor/collector/AppMetricsCollector.java) | 15s | JVM/Tomcat/线程 |
| [`DatabaseMetricsCollector`](backend/bx-monitor/src/main/java/com/beijixing/monitor/collector/DatabaseMetricsCollector.java) | 30s | HikariCP/慢SQL |
| [`CacheMetricsCollector`](backend/bx-monitor/src/main/java/com/beijixing/monitor/collector/CacheMetricsCollector.java) | 30s | Redis命中率/内存/连接数 |
| [`BusinessMetricsCollector`](backend/bx-monitor/src/main/java/com/beijixing/monitor/collector/BusinessMetricsCollector.java) | 60s | 业务计数器/仪表盘 |

### 8.3 链路追踪

**当前方案**: 基于MDC TraceId的轻量级追踪
**可选升级路径**: Spring Cloud Sleuth + Zipkin / Jaeger

```
请求链路示例:
traceId=a1b2c3d4e5f6
├── [0ms]  GET /api/auth/login (Gateway)
│   └── [+5ms]  POST /bx-user/login (AuthController)
│       └── [+10ms] SELECT * FROM user WHERE phone=? (MyBatis)
│       └── [+15ms] SET bx:auth:verify-code:138xxxx5678 (Redis)
│       └── [+20ms] SMS Send (TencentCloud API)
├── [+500ms] Response 200 OK
```

### 8.4 告警通知

**告警通道** (bx-monitor模块):

| 通道 | 实现类 | 触发条件 |
|------|--------|---------|
| 钉钉Webhook | [`WebhookAlarmSender`](backend/bx-monitor/src/main/java/com/beijixing/monitor/alarm/WebhookAlarmSender.java) | P0/P1级别告警 |
| 企业微信Webhook | (同类实现) | P1/P2级别告警 |
| 邮件 | [`EmailAlarmSender`](backend/bx-monitor/src/main/java/com/beijixing/monitor/alarm/EmailAlarmSender.java) | P2/P3级别/日报 |
| 短信 | [`SmsAlarmSender`](backend/bx-monitor/src/main/java/com/beijixing/monitor/alarm/SmsAlarmSender.java) | P0紧急告警 |

**告警规则** (K8s Prometheus配置):

| 规则名 | 条件 | 级别 | 冷却时间 |
|--------|------|------|---------|
| ServiceDown | 服务健康检查连续失败5次 | P0 | — |
| HighErrorRate | 错误率 > 5% (持续5min) | P1 | 10min |
| HighLatency | P95延迟 > 1s (持续5min) | P1 | 10min |
| PodCrashLooping | Pod频繁重启 (>3次/10min) | P0 | — |
| HighMemoryUsage | 内存使用率 > 85% | P2 | 30min |
| HighCPUUsage | CPU使用率 > 80% | P2 | 30min |
| DBConnectionPoolExhausted | 连接池活跃率 > 90% | P1 | 10min |
| RedisHighMemory | Redis内存使用 > 80% | P2 | 30min |

---

## 9. 性能优化

### 9.1 JVM调优参数

```bash
# 生产环境推荐JVM参数 (G1GC + Java 17)
java \
  -Xms2g -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:G1HeapRegionSize=16m \
  -XX:+UnlockExperimentalVMOptions \
  -XX:G1NewSizePercent=30 \
  -XX:G1MaxNewSizePercent=40 \
  -XX:InitiatingHeapOccupancyPercent=45 \
  -XX:+UseStringDeduplication \
  -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/opt/beijixing-ai/logs/heapdump.hprof \
  -XX:+ExitOnOutOfMemoryError \
  -Djava.security.egd=file:/dev/./urandom \
  -Dfile.encoding=UTF-8 \
  -Duser.timezone=Asia/Shanghai \
  -jar beijixing-app.jar
```

**参数解读**:

| 参数 | 值 | 说明 |
|------|-----|------|
| `-Xms/-Xmx` | 2g | 堆内存初始=最大(避免动态扩缩容抖动) |
| `MaxGCPauseMillis` | 200 | G1 GC目标停顿<200ms |
| `G1HeapRegionSize` | 16m | G1 Region大小(适合2-4GB堆) |
| `InitiatingHeapOccupancyPercent` | 45 | 并发标记启动阈值(提前触发Mixed GC) |
| `UseStringDeduplication` | 启用 | 字符串去重(减少30%左右String内存) |
| `HeapDumpOnOutOfMemoryError` | 启用 | OOM时自动Dump(便于排查) |

### 9.2 数据库连接池配置 (HikariCP)

```yaml
# 当前生产配置 (见 application.yml)
spring.datasource.hikari:
  minimum-idle: 10          # 最小空闲连接(预热)
  maximum-pool-size: 50      # 最大连接数(公式: CPU核心*2 + 磁盘数)
  connection-timeout: 30000  # 获取连接超时(30s)
  idle-timeout: 600000       # 空闲连接回收(10min)
  max-lifetime: 1800000      # 连接最大存活(30min, <DB wait_timeout)
  pool-name: beijixing-hikari-pool
  leak-detection-threshold: 60000  # 泄露检测(60s未归还则警告)
```

**调优建议**:
- CPU核心数 ≤ 8: `maximum-pool-size = core_count * 2 + disk_count`
- CPU核心数 > 8: `maximum-pool-size = core_count + core_count/2 + disk_count`
- 监控指标: `hikaricp_connections_pending` (等待连接的请求数应 ≈ 0)

### 9.3 Redis连接池配置 (Lettuce)

```yaml
spring.data.redis.lettuce.pool:
  max-active: 50       # 最大连接数
  max-idle: 20         # 最大空闲连接
  min-idle: 10         # 最小空闲连接(保活)
  max-wait: 5000ms      # 获取连接最大等待
shutdown-timeout: 200ms # 关闭超时
```

### 9.4 HTTP客户端优化

| 场景 | 客户端 | 配置要点 |
|------|--------|---------|
| 外部API调用(腾讯云/阿里云) | Apache HttpClient 4.5.14 | 连接池 MaxPerRoute=20, Total=100 |
| AI模型调用(HTTP/SSE) | RestTemplate/WebClient | 超时 connect=5s/read=30s, 重试3次 |
| 社交平台OAuth | OkHttp / RestTemplate | 连接复用, TLS 1.2+ |
| 内部模块间调用 | 直接方法调用(单体优势) | 无网络开销 |

### 9.5 异步处理策略

| 场景 | 实现方式 | 线程池 | 说明 |
|------|---------|--------|------|
| 短信发送 | `@Async` + ThreadPoolExecutor | sms-pool(5线程) | 解耦主流程 |
| 邮件发送 | `@Async` + ThreadPoolExecutor | mail-pool(3线程) | 解耦主流程 |
| AI内容生成 | RabbitMQ Producer → Consumer | async-executor(10线程) | 耗时操作异步化 |
| 商机统计 | `@Scheduled` + 固定延迟 | scheduling-pool(10线程) | 定时预计算 |
| 日志异步写入 | Logback AsyncAppender | 独立IO线程 | 不阻塞业务线程 |
| 文件上传处理 | `CompletableFuture` | upload-pool(5线程) | 大文件异步处理 |

---

## 10. 部署架构

### 10.1 生产环境拓扑

```
                          Internet
                             │
                    ┌────────▼────────┐
                    │   CDN/WAF       │  (DDoS防护/SSL卸载)
                    │  (阿里云/Cloudflare)│
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │   Nginx Cluster  │  (LB + Reverse Proxy)
                    │   2节点 Active    │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
       ┌──────▼──────┐ ┌────▼─────┐ ┌──────▼──────┐
       │  beijixing  │ │  Static  │ │  WebSocket  │
       │  -app ×2    │ │  Files   │ │  /ws/message│
       │  (Tomcat)   │ │  (OSS)   │ │             │
       └──────┬──────┘ └──────────┘ └──────────────┘
              │
    ┌─────────┼─────────┬─────────┬─────────┐
    │         │         │         │         │
┌───▼──┐ ┌───▼───┐ ┌───▼───┐ ┌──▼───┐ ┌──▼────┐
│MariaDB│ │ Redis │ │RabbitMQ│ │MongoDB│ │ ES    │
│Cluster│ │Cluster│ │Cluster│ │ RS Set│ │Cluster│
│(主从) │ │(哨兵) │ │(镜像) │ │       │ │(3节点)│
└───────┘ └───────┘ └───────┘ └──────┘ └───────┘
```

### 10.2 Docker容器化方案

**镜像构建**: 每个(Dockerfile位于 [deploy/docker/services/](deploy/docker/services/))

**Docker Compose 编排** ([docker-compose.yml](deploy/docker/docker-compose.yml)):

| 服务 | 镜像 | 端口 | 资源 | 依赖 |
|------|------|------|------|------|
| mariadb | mariadb:10.11 | 3306 | 1CPU/2Gi | — |
| redis | redis:6.0-alpine | 6379 | 0.5CPU/512Mi | — |
| nacos | nacos/nacos-server:v2.2.3 | 8848/9848 | 0.5CPU/1Gi | mariadb |
| mongodb | mongo:6.0 | 27017 | 0.5CPU/1Gi | — |
| rabbitmq | rabbitmq:3.11-management-alpine | 5672/15672 | 0.5CPU/512Mi | — |
| elasticsearch | elasticsearch:8.11.0 | 9200/9300 | 1CPU/2Gi | — |
| gateway | beijixing/gateway:latest | 8080 | 1CPU/2Gi | mariadb,redis,rabbitmq |
| user | beijixing/user-service:latest | 8081 | 1CPU/1Gi | mariadb,redis |
| ... (其余13个业务服务同理) | | | | |

**网络配置**:
- 网络名称: `beijixing-network` (bridge driver)
- 子网: `172.20.0.0/16`
- 网关: `172.20.0.1`

**持久化卷**:
- `mariadb_data` - 数据库数据
- `redis_data` - Redis持久化(AOF)
- `mongodb_data` - 文档数据
- `rabbitmq_data` - 队列消息
- `elasticsearch_data` - 索引数据
- `nacos_data` / `nacos_logs` - Nacos数据和日志
- `storage_data` - 上传文件
- `ai_models` - AI模型文件

### 10.3 K8s编排建议

**命名空间**: `beijixing`

**部署清单** (见 [deploy/k8s/](deploy/k8s/)):

| 服务 | Deployment | Service | Replicas | Resource | Probe |
|------|-----------|---------|----------|----------|-------|
| gateway | gateway-deployment | LoadBalancer(80/8080) | 3 | 2CPU/2Gi | /actuator/health |
| user | user-deployment | ClusterIP(8001) | 3 | 1CPU/1Gi | /actuator/health |
| tenant | tenant-deployment | ClusterIP(8002) | 2 | 1CPU/1Gi | /actuator/health |
| content | content-deployment | ClusterIP(8003) | 2 | 1.5CPU/2Gi | /actuator/health |
| lead | lead-deployment | ClusterIP(8004) | 3 | 1.5CPU/2Gi | /actuator/health |
| risk | risk-deployment | ClusterIP(8005) | 2 | 2CPU/2Gi | /actuator/health |
| billing | billing-deployment | ClusterIP(8006) | 2 | 1CPU/1Gi | /actuator/health |
| ai | ai-deployment | ClusterIP(8007) | 2 | 4CPU/8Gi(+GPU) | /actuator/health |
| message | message-deployment | ClusterIP(8008) | 2 | 1CPU/1Gi | /actuator/health |
| storage | storage-deployment | ClusterIP(8009) | 2 | 1CPU/1Gi | /actuator/health |
| web | web-deployment | LoadBalancer(80/443) | 3 | 0.5CPU/512Mi | /actuator/health |

**滚动更新策略**:
```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 25%
    maxUnavailable: 25%
```

**Ingress域名路由**:
| 域名 | 目标服务 | 用途 |
|------|---------|------|
| api.beijixing.ai | gateway | RESTful API |
| app.beijixing.ai | web (web-pc) | 用户端 |
| admin.beijixing.ai | web (web-admin) | 管理后台 |

### 10.4 CI/CD流水线

```
┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
│ Code Push│──►│ Build    │──►│ Unit Test│──►│ Package  │──►│ Deploy   │
│ Git Push │   │ Maven    │   │ JUnit 5 │   │ JAR/Docker│   │ Staging  │
└──────────┘   └──────────┘   └──────────┘   └──────────┘   └────┬─────┘
                                                                   │
                                                           ┌───────▼───────┐
                                                           │ E2E Test      │
                                                           │ (Postman/Newman)│
                                                           └───────┬───────┘
                                                                   │
                                                           ┌───────▼───────┐
                                                           │ Production    │
                                                           │ Blue-Green    │
                                                           └───────────────┘
```

**可用脚本**:
- [build-monolith.bat](backend/build-monolith.bat): 本地单体打包
- [deploy.sh](backend/deploy.sh): 远程服务器部署
- [deploy/docker/build-all-services.sh](deploy/docker/build-all-services.sh): Docker镜像批量构建
- [deploy/scripts/backend/deploy.sh](deploy/scripts/backend/deploy.sh): 后端标准部署脚本

---

## 11. 扩展性设计

### 11.1 水平扩展策略

**当前(单体阶段)**:
- 部署多个实例 → Nginx负载均衡 → Session外置(Redis)
- 无状态设计: 所有状态存储在Redis/DB
- 幂等接口设计: 所有写操作支持幂等

**扩展路径**:
```
Phase 1 (当前): 单体 × N实例 + Nginx LB
    ↓ QPS > 1000 或 单实例内存 > 4GB
Phase 2: 按领域拆分为独立Spring Boot应用
    ↓ 业务线独立
Phase 3: 引入Service Mesh (Istio)
```

### 11.2 数据库分库分表预留

**分片键设计原则**:
- 用户相关表: `tenant_id` (租户维度)
- 商机相关表: `owner_id` (销售人员维度) 或 `tenant_id` (租户维度)
- 日志相关表: `create_time` (时间维度)
- 订单相关表: `tenant_id` + `create_time` (复合维度)

**中间件选型预留**: ShardingSphere-JDBC (已引入依赖可能性评估中)

### 11.3 微服务拆分路径

**拆分优先级** (按耦合度和独立性排序):

| 顺序 | 拆分模块 | 理由 | 依赖方 |
|------|---------|------|--------|
| 1 | bx-ai | GPU资源独占、无状态、易独立 | bx-content, bx-lead |
| 2 | bx-file/storage | IO密集、可独立扩展 | 所有模块 |
| 3 | bx-message | 有状态(WebSocket)、消息驱动 | 所有模块 |
| 4 | bx-user | 认证核心、独立性强 | 所有模块 |
| 5 | bx-lead | 业务核心、数据量大 | bx-ai, bx-social |
| 6 | bx-content | AI密集、外部API多 | bx-ai, bx-storage |
| 7 | bx-social | 平台API耦合深 | bx-lead, bx-risk |

**拆分通信方式**: 
- 同步: OpenFeign (声明式HTTP客户端)
- 异步: RabbitMQ (事件驱动)
- 配置: Nacos Config (集中配置)

### 11.4 多租户隔离方案

**当前实现**: 共享数据库 + 共享Schema + 行级隔离 (TenantLineInnerInterceptor)

```
SELECT * FROM bx_lead WHERE tenant_id = 123 AND deleted = 0
-- ↑ 由BxTenantLineHandler自动追加
```

**隔离级别演进路径**:

| 级别 | 方案 | 成本 | 安全性 | 适用阶段 |
|------|------|------|--------|---------|
| L1 行级隔离 | `tenant_id` WHERE条件 | 最低 | 低 | 当前 ✓ |
| L2 Schema隔离 | 每租户独立Schema | 低 | 中 | 付费租户>100 |
| L3 DB隔离 | 每租户独立数据库 | 高 | 高 | 付费租户>1000 |
| L4 实例隔离 | 独立数据库实例 | 最高 | 最高 | 大客户定制 |

---

## 12. 附录

### 12.1 配置项清单

**环境变量** (见 [.env.example](deploy/docker/.env.example)):

| 变量名 | 必填 | 默认值 | 说明 |
|--------|------|--------|------|
| `DB_HOST` | 是 | localhost | MariaDB主机 |
| `DB_PORT` | 否 | 3306 | MariaDB端口 |
| `DB_NAME` | 是 | beijixing_ai | 数据库名 |
| `DB_USER` | 是 | root | 数据库用户 |
| `DB_PASSWORD` | 是 | — | 数据库密码 |
| `REDIS_HOST` | 是 | localhost | Redis主机 |
| `REDIS_PORT` | 否 | 6379 | Redis端口 |
| `REDIS_PASSWORD` | 是 | — | Redis密码 |
| `JWT_SECRET` | 是 | — | JWT签名密钥(≥32字符) |
| `VOLCENGINE_API_KEY` | 否 | — | 火山引擎API Key |
| `DEEPSEEK_API_KEY` | 否 | — | DeepSeek API Key |
| `TENCENT_SECRET_ID` | 否 | — | 腾讯云SecretId |
| `SMTP_HOST` | 否 | smtp.qq.com | SMTP服务器 |
| `SMTP_USERNAME` | 否 | — | SMTP用户名 |
| `SMTP_PASSWORD` | 否 | — | SMTP密码 |

### 12.2 端口分配表

| 端口 | 服务 | 协议 | 说明 |
|------|------|------|------|
| 80 | Nginx | HTTP | 前端静态资源/反向代理 |
| 443 | Nginx | HTTPS | TLS终止 |
| 8080 | beijixing-app / gateway | HTTP | 主应用/API网关 |
| 8081 | bx-user | HTTP | 用户服务(微服务模式) |
| 8082 | bx-tenant | HTTP | 租户服务 |
| 8083 | bx-content | HTTP | 内容服务 |
| 8084 | bx-lead | HTTP | 商机服务 |
| 8085 | bx-ai | HTTP | AI服务 |
| 8086 | bx-risk | HTTP | 风控服务 |
| 8089 | bx-message | HTTP | 消息服务 |
| 8090 | bx-storage | HTTP | 存储服务 |
| 8091 | bx-system | HTTP | 系统服务 |
| 8092 | bx-data | HTTP | 数据服务 |
| 8093 | bx-social | HTTP | 社交服务 |
| 8094 | bx-schedule | HTTP | 调度服务 |
| 8095 | bx-monitor | HTTP | 监控服务 |
| 8096 | bx-billing | HTTP | 计费服务 |
| 9999 | XXL-Job Executor | HTTP | 任务执行器 |
| 8848 | Nacos | HTTP | 配置中心Console |
| 9848 | Nacos | gRPC | Nacos服务间通信 |
| 3306 | MariaDB | TCP | 数据库 |
| 6379 | Redis | TCP | 缓存 |
| 5672 | RabbitMQ | TCP | 消息队列 |
| 15672 | RabbitMQ | HTTP | 管理界面 |
| 27017 | MongoDB | TCP | 文档数据库 |
| 9200 | Elasticsearch | HTTP | 搜索引擎 |
| 9300 | Elasticsearch | TCP | ES节点通信 |

### 12.3 第三方服务集成清单

| 服务 | 提供商 | 用途 | 计费模式 | 状态 |
|------|--------|------|---------|------|
| 短信验证码 | 腾讯云SMS | 登录验证/通知 | 按条计费 | ✅ 已集成 |
| 对象存储 | 腾讯云COS | 文件上传/头像 | 按量计费 | ✅ 已集成 |
| AI大模型 | 火山引擎(豆包) | 文案生成/改写 | 按Token计费 | ✅ 已集成 |
| AI大模型 | DeepSeek | 意图分析/长文本 | 按Token计费 | ✅ 已集成 |
| 敏感词检测 | 句易网 | 内容合规检查 | 按次计费 | 🔲 待接入 |
| 支付宝支付 | 支付宝 | 在线充值 | 手续费 | 🔲 待开通 |
| 微信支付 | 微信支付 | 在线充值 | 手续费 | 🔲 待开通 |
| 邮件服务 | QQ邮箱SMTP | 系统通知 | 免费 | ✅ 已集成 |
| DNS/CDN | Cloudflare/阿里云 | 加速/防护 | 按流量 | 🔲 待配置 |
| 域名证书 | Let's Encrypt | HTTPS | 免费 | 🔲 待配置 |

### 12.4 项目目录结构总览

```
BeijiXing-AI/
├── backend/                      # 后端代码 (Maven多模块)
│   ├── pom.xml                   # 父POM (依赖管理)
│   ├── beijixing-app/            # 极简单体启动模块
│   │   └── src/main/
│   │       ├── java/.../BeijixingAiApplication.java
│   │       └── resources/
│   │           ├── application.yml      # 统一配置
│   │           ├── application-dev.yml  # 开发环境
│   │           ├── application-prod.yml # 生产环境
│   │           └── bootstrap.yml        # 禁用Cloud Bootstrap
│   ├── bx-common/                 # 公共基础库
│   ├── bx-gateway/                # API网关(Spring Cloud Gateway)
│   ├── bx-user/                   # 用户认证授权
│   ├── bx-tenant/                 # 多租户管理
│   ├── bx-lead/                   # 商机CRM
│   ├── bx-content/                # 内容营销
│   ├── bx-risk/                   # 风控合规
│   ├── bx-billing/                # 计费支付
│   ├── bx-ai/                     # AI模型调度
│   ├── bx-message/                # 消息通知
│   ├── bx-storage/                # 对象存储
│   ├── bx-monitor/                # 监控告警
│   ├── bx-schedule/               # 定时任务
│   ├── bx-system/                 # 系统管理
│   ├── bx-social/                 # 社交爬虫+合规
│   ├── bx-data/                   # 数据分析
│   └── bx-task/                   # 任务管理
├── frontend/                     # 前端代码
│   ├── web-pc/                   # 用户端 (Vue3 + Vite)
│   ├── web-admin/                # 管理后台 (Vue3 + Vite)
│   └── web-pc-simple/            # 简化版用户端
├── mobile/                       # 移动端代码
│   ├── android/                  # Android App (Kotlin)
│   └── ios/                      # iOS App (SwiftUI)
├── deploy/                       # 部署配置
│   ├── docker/                   # Docker Compose编排
│   │   ├── docker-compose.yml    # 基础版
│   │   ├── docker-compose.prod.yml  # 生产版
│   │   └── services/*/Dockerfile    # 各服务镜像
│   ├── k8s/                      # Kubernetes编排
│   ├── nginx/                    # Nginx配置
│   ├── config/                   # 服务定义/环境变量模板
│   └── scripts/                  # 运维脚本
├── nacos-config/                 # Nacos共享配置
│   ├── common-datasource.yml     # 数据源配置
│   ├── common-redis.yml          # Redis配置
│   ├── common-jwt.yml            # JWT配置
│   ├── common-logging.yml        # 日志配置
│   └── ai-model-config.yml       # AI模型配置
├── docs/                         # 项目文档
├── scripts/                      # 工具脚本
└── tools/                        # 开发工具(Maven等)
```

---

> **文档维护说明**
>
> - 本文档基于项目实际代码结构编写，所有配置项、类名、端口号均来自真实代码
> - 当架构发生重大变更时，应及时同步更新本文档
> - 技术决策记录(ADR)应在每次技术选型评审后补充
>
> **版本历史**
>
> | 版本 | 日期 | 作者 | 变更说明 |
> |------|------|------|---------|
> | v1.0 | 2026-05-21 | AI Assistant | 初始版本，基于实际代码完整创建 |
