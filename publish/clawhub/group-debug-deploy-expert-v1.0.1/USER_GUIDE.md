# 📚 Group Debug & Deploy Expert - 用户使用手册
# User Guide v1.0.1

> **从入门到精通的完整指南** - 让您快速掌握企业级调试部署AI专家团队的使用方法

---

## 📖 目录

1. [快速开始（5分钟上手）](#-快速开始5分钟上手)
2. [安装与配置](#-安装与配置)
3. [核心概念理解](#-核心概念理解)
4. [11角色团队使用指南](#-11角色团队使用指南)
5. [21条铁律实战应用](#-21条铁律实战应用)
6. [反幻觉系统详解](#-反幻觉系统详解)
7. [常见任务模板](#-常见任务模板)
8. [高级技巧](#-高级技巧)
9. [故障排除](#-故障排除)
10. [最佳实践](#-最佳实践)

---

## 🚀 快速开始（5分钟上手）

### ✅ 前置检查清单

在开始之前，请确认：

- [ ] 您已安装 Trae IDE / Cursor / OpenClaw / Hermes 等兼容平台
- [ ] 已下载并解压技能包到正确目录
- [ ] 有一个待调试或部署的项目（可以是任何项目）

---

### 🎯 第一个任务：Hello World调试

#### 场景设定

假设您有一个 **Spring Boot 项目**，启动时出现错误：
```
Failed to configure a DataSource: 'url' attribute is not specified
```

#### 操作步骤

**Step 1: 启动技能包**

在您的IDE中打开项目后，直接对AI助手说：

```
帮我排查这个Spring Boot数据源配置问题
```

**Step 2: 观察AI团队的协作**

您会看到类似这样的工作流程：

```
🎯 Project Manager: 收到任务，正在分析...
   ├─ 任务类型: 调试 (Debug)
   ├─ 复杂度评估: 中等 (P2)
   └─ 分配角色: DBA + Backend Dev

🗄️ DBA: 正在分析数据源配置...
   ├─ 检查 application.yml/properties
   ├─ 验证数据库连接参数
   └─ 发现问题: 缺少 spring.datasource.url 配置

⚙️ Backend Dev: 正在生成修复方案...
   ├─ 推荐配置格式
   ├─ 提供示例代码
   └─ 标注注意事项

🛡️ Security Analyst: 安全审查中...
   ├─ 检查是否有密码硬编码风险 ✓
   ├─ 验证环境变量使用建议 ✓
   └─ 审查通过 ✓

✅ 任务完成！输出修复报告
```

**Step 3: 应用修复**

根据AI提供的方案修改配置文件，通常只需要：

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_database
    username: your_username
    password: ${DB_PASSWORD}  # 推荐使用环境变量
    driver-class-name: com.mysql.cj.jdbc.Driver
```

**Step 4: 验证结果**

重新启动项目，确认问题已解决！

---

## 📦 安装与配置

### 方式1: Trae IDE（推荐）⭐⭐⭐⭐⭐

```bash
# 1. 下载技能包ZIP文件
# 从GitHub Release下载: https://github.com/xuanji-ai-2026/group-debug-deploy-expert/releases/tag/v1.0.1

# 2. 在Trae IDE中打开项目

# 3. 将解压后的文件复制到项目的 .trae/skills/ 目录下
cp -r group-debug-deploy-expert/.trae/skills/* .trae/skills/

# 4. 重启Trae IDE（如果需要）

# 5. 完成！开始使用
```

**验证安装成功**:
```
在聊天框输入: "你是谁？"
预期回复: "我是Group Debug & Deploy Expert（通用调试部署专家团队），基于21条铁律和11角色AI数字员工架构..."
```

---

### 方式2: Cursor IDE ⭐⭐⭐⭐

```bash
# 1. 克隆仓库
git clone https://github.com/xuanji-ai-2026/group-debug-deploy-expert.git

# 2. 复制规则文件到项目根目录
cd your-project
mkdir -p .cursor/rules
cp ../group-debug-deploy-exper/SKILL.md .cursor/rules/debug-deploy-rules.md

# 3. 在Cursor设置中启用规则
# Settings → Rules → 启用自定义规则

# 4. 完成！
```

---

### 方式3: OpenClaw / Hermes ⭐⭐⭐⭐

```bash
# OpenClaw
clawhub install group-debug-deploy-expert

# Hermes Agent
hermes install group-debug-deploy-expert

# 或手动安装
npm install -g @xuanji-ai/group-debug-deploy-expert
```

---

### 方式4: 手动通用安装 ⭐⭐⭐

适用于任何支持自定义指令的平台：

```bash
# 1. 下载SKILL.md主文件
wget https://raw.githubusercontent.com/xuanji-ai-2026/group-debug-deploy-expert/main/SKILL.md

# 2. 放置到平台的规则目录
# 例如:
# - VS Code Copilot: .github/copilot-instructions.md
# - ChatGPT: 作为System Prompt粘贴
# - Claude: 作为Custom Instructions

# 3. 开始使用！
```

---

## 🧠 核心概念理解

### 概念1: ZERO-TH LAW（零号法则）

**定义**: 技能框架必须与业务项目100%隔离

**为什么重要**:
- ❌ **不隔离的风险**: AI可能读取敏感信息（密钥、密码、客户数据）
- ✅ **隔离的好处**: 安全、专注、可复用

**实际表现**:
```
❌ 错误做法:
AI: "我发现了你的数据库密码是 xxxxx"  ← 泄露！

✅ 正确做法:
AI: "请在application.yml中配置spring.datasource.password，
     建议使用环境变量 ${DB_PASSWORD}"  ← 安全指导
```

---

### 概念2: 11角色AI数字员工团队

**类比**: 就像雇佣了一个完整的IT部门，但成本几乎为零

| 角色 | 类比真实岗位 | 典型任务 |
|------|------------|---------|
| Project Manager | 项目经理 | 制定计划、分配任务 |
| Tech Lead | 技术总监 | 架构决策、技术评审 |
| DevOps Engineer | 运维工程师 | CI/CD、容器化、K8s |
| QA Engineer | 测试工程师 | 测试策略、质量保障 |
| Security Analyst | 安全工程师 | 安全扫描、漏洞修复 |
| DBA | 数据库管理员 | SQL优化、数据库维护 |
| Frontend Dev | 前端开发 | UI调试、性能优化 |
| Backend Dev | 后端开发 | API调试、微服务 |
| Mobile Dev | 移动端开发 | iOS/Android调试 |
| Performance Eng | 性能工程师 | 瓶颈定位、优化 |
| Doc Specialist | 技术文档 | 文档编写、知识库 |

**协作模式**:
```
用户提问
    ↓
Project Manager (接收任务)
    ↓
├── Tech Lead (技术可行性评估)
├── 相关角色专家 (并行分析)
│   ├── DBA (如果是数据库问题)
│   ├── Backend Dev (如果是后端问题)
│   └── DevOps Engineer (如果是部署问题)
    ↓
Security Analyst (安全审查) ← 所有方案必经
    ↓
QA Engineer (测试验证)
    ↓
Doc Specialist (输出报告)
    ↓
返回给用户
```

---

### 概念3: 21条铁律（Iron Principles）

**本质**: 来自真实生产环境的经验总结，避免踩坑

**分类**:

| 类别 | 数量 | 重点 |
|------|------|------|
| 核心原则 | 5条 | 安全、权限、追溯 |
| 调试原则 | 8条 | 问题定位、修复流程 |
| 部署原则 | 8条 | 发布策略、回滚机制 |

**强制执行**: 
- AI会自动遵循这些原则
- 如果用户的请求违反铁律，AI会**拒绝执行**并解释原因
- 例如: 用户要求"删除生产数据库" → AI拒绝（违反P0安全第一原则）

---

### 概念4: 反幻觉系统（Anti-Hallucination）

**什么是AI幻觉?**
AI"编造"看起来合理但实际上不存在或不正确的信息。

**我们的防御体系**:
```
12层检测关卡:
1. 意图识别 - 用户真正想要什么？
2. 能力边界 - 这在我的能力范围内吗？
3. 上下文校验 - 信息来源可靠吗？
4. 事实核查 - 我说的内容有依据吗？
5. 语法验证 - 代码语法正确吗？
6. 逻辑推理 - 逻辑链条完整吗？
7. 安全扫描 - 有安全风险吗？
8. 影响分析 - 会造成什么影响？
9. 方案可行性 - 实际可执行吗？
10. 用户确认 - 用户同意吗？
11. 执行监控 - 执行过程正常吗？
12. 结果验证 - 结果符合预期吗？
```

**触发条件**:
- 发现不确定信息 → 明确标注 ⚠️
- 无法验证的事实 → 提供备选方案
- 危险操作 → 要求二次确认

---

## 👥 11角色团队使用指南

### 如何调用特定角色？

**方式1: 自然语言指定**
```
让DBA帮我优化这条慢SQL
请Security Analyst审查这段代码的安全性
我想让DevOps Engineer帮我配置CI/CD流水线
```

**方式2: 让Project Manager自动分配**
```
帮我全面排查这个微服务的性能问题
# PM会自动调用: Performance Eng + Backend Dev + DBA
```

**方式3: 多角色协作**
```
准备上线新版本，请DevOps、QA、Security一起制定发布计划
# 自动协调三个角色的专业意见
```

---

### 各角色详细使用场景

#### 🎯 Project Manager（项目经理）

**最适合的场景**:
- 复杂任务的拆解和规划
- 多步骤操作的协调
- 进度跟踪和状态汇报

**示例对话**:
```
用户: 我们要重构用户认证模块，请制定一个实施计划

PM: 好的，我来为您制定认证模块重构计划。
    
    📋 重构计划 v1.0
    ════════════════════
    
    Phase 1: 分析现状 (预计2小时)
    ├─ Tech Lead: 审查现有认证逻辑
    ├─ Security Analyst: 识别安全风险点
    └─ Doc Specialist: 整理现有文档
    
    Phase 2: 设计方案 (预计4小时)
    ├─ Tech Lead: 设计新架构
    ├─ Backend Dev: 设计API接口
    └─ Security Analyst: 安全设计评审
    
    Phase 3: 实施开发 (预计3天)
    ├─ Backend Dev: 实现新认证逻辑
    ├─ Frontend Dev: 更新登录界面
    ├─ QA Engineer: 编写测试用例
    └─ Doc Specialist: 更新技术文档
    
    Phase 4: 测试验收 (预计1天)
    ├─ QA Engineer: 执行测试矩阵
    ├─ Security Analyst: 渗透测试
    └─ Performance Eng: 性能压测
    
    Phase 5: 灰度发布 (预计2天)
    ├─ DevOps Engineer: 配置灰度策略
    ├─ QA Engineer: 监控线上指标
    └─ 全量发布决策
    
    总工期: 约1.5周
    风险等级: 中等
    回滚方案: 已准备
    
    是否需要我开始执行Phase 1？
```

---

#### 💻 Tech Lead（技术负责人）

**最适合的场景**:
- 架构设计和技术选型
- 代码审查和最佳实践
- 技术债务治理

**示例对话**:
```
用户: 我们的订单系统要支撑双11千万级QPS，架构上怎么设计？

Tech Lead: 让我从架构角度为您分析。
    
    🏗️ 高并发订单系统架构设计
    ════════════════════════════
    
    当前瓶颈分析:
    ❌ 单体MySQL → 写入瓶颈
    ❌ 同步处理 → 响应延迟
    ❌ 无缓存 → 重复查询
    
    推荐架构: 分层解耦 + 异步化
    
    Layer 1: 接入层
    ├── Nginx (负载均衡, LVS+Keepalived)
    └── API Gateway (限流、熔断、路由)
    
    Layer 2: 业务层 (微服务集群)
    ├── Order Service (无状态, 可水平扩展)
    ├── Inventory Service (库存扣减)
    ├── Payment Service (支付对接)
    └── Notification Service (异步通知)
    
    Layer 3: 数据层
    ├── Redis Cluster (热点缓存, 预减库存)
    ├── MySQL Sharding (分库分表, 16 shard)
    └── Elasticsearch (订单搜索)
    
    Layer 4: 消息中间件
    └── Kafka/RocketMQ (削峰填谷, 异步解耦)
    
    关键指标预估:
    ✅ QPS支撑: 1000万+ (足够应对双11)
    ✅ P99响应: <200ms
    ✅ 可用性: 99.99%
    
    是否需要我出详细的落地实施方案？
```

---

#### 🔧 DevOps Engineer（运维工程师）

**最适合的场景**:
- CI/CD流水线搭建
- Docker/Kubernetes配置
- 服务器运维自动化

**示例对话**:
```
用户: 帮我搭建一套完整的CI/CD流水线，支持自动测试和部署

DevOps: 好的，我来为您设计完整的DevOps流水线。
    
    🔧 CI/CD 流水线设计方案
    ═════════════════════════
    
    工具链选择:
    • 版本控制: Git + GitLab/GitHub
    • CI工具: Jenkins / GitLab CI
    • 容器化: Docker + Docker Compose
    • 编排: Kubernetes (生产环境)
    • 镜像仓库: Harbor /阿里云ACR
    • 监控: Prometheus + Grafana
    
    流水线阶段:
    
    Stage 1: Code Check (2分钟)
    ├── Lint检查 (ESLint/PMD/Checkstyle)
    ├── 单元测试 (JUnit/PyTest/Jest)
    └── 覆盖率报告 (>80%通过)
    
    Stage 2: Build & Package (5分钟)
    ├── Maven/Gradle/NPM构建
    ├── Docker镜像构建
    ├── 镜像安全扫描 (Trivy)
    └── 推送到镜像仓库
    
    Stage 3: Deploy to Test (3分钟)
    ├── K8s部署到test命名空间
    ├── 健康检查 (/health endpoint)
    ├── 集成测试 (Postman/Newman)
    └── 性能基准测试 (k6/JMeter)
    
    Stage 4: Approve (人工审批)
    ├── 测试报告展示
    ├── 变更清单展示
    └── PM/Tech Lead审批
    
    Stage 5: Deploy to Prod (2分钟)
    ├── 灰度发布 (10%流量)
    ├── 观察期 (15分钟)
    ├── 全量发布 (100%流量)
    └── 自动通知 (钉钉/Slack/邮件)
    
    Jenkinsfile 示例已准备好，是否需要我生成完整配置文件？
```

---

#### 🧪 QA Engineer（测试工程师）

**最适合的场景**:
- 测试策略制定
- 自动化测试脚本编写
- 质量门禁配置

**示例对话**:
```
用户: 为我的REST API编写全面的测试用例

QA: 好的，我来为您的API设计完整的测试矩阵。
    
    🧪 REST API 测试策略
    ══════════════════════
    
    测试金字塔:
    
    ┌─────────────────────┐
    │  E2E Tests (10%)    │ ← 端到端场景测试
    ├─────────────────────┤
    │  Integration (20%)  │ ← 服务间集成测试
    ├─────────────────────┤
    │  Unit Tests (70%)   │ ← 单元测试（基础）
    └─────────────────────┘
    
    测试维度覆盖:
    
    1️⃣ 功能测试
       ├── 正常流程 (Happy Path)
       ├── 边界值测试 (Boundary)
       ├── 异常输入 (Negative Case)
       └── 权限校验 (Authorization)
    
    2️⃣ 性能测试
       ├── 响应时间 (<200ms P95)
       ├── 并发能力 (500 QPS)
       └── 压力测试 (2倍峰值)
    
    3️⃣ 安全测试
       ├── SQL注入防护 ✓
       ├── XSS攻击防护 ✓
       ├── CSRF Token验证 ✓
       └── 敏感数据脱敏 ✓
    
    4️⃣ 兼容性测试
       ├── Chrome/Firefox/Safari
       ├── iOS/Android
       └── 不同分辨率
    
    自动化脚本示例 (pytest):
    [生成完整的测试代码...]
    
    覆盖率目标: 行覆盖率 >80%, 分支覆盖率 >75%
```

---

#### 🛡️ Security Analyst（安全分析师）

**最适合的场景**:
- 安全漏洞扫描
- 代码安全审计
- 合规性检查

**示例对话**:
```
用户: 请审查这段代码的安全性，看看有没有漏洞

Security: 正在进行全面的安全审计...
    
    🛡️ 安全审计报告
    ═══════════════════
    
    扫描范围:
    ✅ OWASP Top 10 漏洞检测
    ✅ 注入攻击检测 (SQL/NoSQL/Command/XSS)
    ✅ 认证授权缺陷检测
    ✅ 敏感信息泄露检测
    ✅ 加密算法强度检测
    
    发现的问题:
    
    🔴 严重 (Critical): 1个
    └── [CWE-89] SQL注入漏洞
        位置: UserService.java:45
        问题: 直接拼接用户输入到SQL
        影响: 可导致数据泄露/篡改
        修复: 使用PreparedStatement
        
    🟠 高危 (High): 2个
    ├── [CWE-79] XSS跨站脚本
    │   位置: CommentController.java:23
    │   修复: 对输出进行HTML转义
    │
    └── [CWE-798] 硬编码凭证
        位置: DatabaseConfig.java:12
        修复: 使用环境变量或密钥管理服务
    
    🟡 中危 (Medium): 3个
    ├── [CWE-352] CSRF保护缺失
    ├── [CWE-327] 弱加密算法 (MD5)
    └── [CWE-200] 信息泄露 (堆栈跟踪)
    
    🟢 低危 (Low): 5个
    └── (略...)
    
    修复优先级建议:
    1. 立即修复 SQL注入 (P0)
    2. 本周内完成 XSS 和 凭证硬编码 (P1)
    3. 下个Sprint解决中低危问题 (P2)
    
    是否需要我提供每个问题的具体修复代码？
```

---

#### 🗄️ DBA（数据库管理员）

**最适合的场景**:
- SQL性能优化
- 数据库设计评审
- 数据迁移方案

**示例对话**:
```
用户: 这个查询太慢了，帮我优化一下

DBA: 正在分析您的SQL性能问题...
    
    🗄️ SQL性能分析与优化
    ══════════════════════
    
    原始SQL:
    SELECT * FROM orders o
    LEFT JOIN users u ON o.user_id = u.id
    LEFT JOIN products p ON o.product_id = p.id
    WHERE o.create_time > '2026-01-01'
    ORDER BY o.create_time DESC;
    
    执行计划分析 (EXPLAIN):
    ⚠️ 问题发现:
    ├── 全表扫描 (type: ALL) on orders表
    ├── Using filesort (排序未命中索引)
    ├── 无索引利用 (key: NULL)
    └── 扫描行数预估: 1,000,000+ rows
    
    性能指标:
    ├── 查询耗时: 12.3秒 ❌ (目标<1秒)
    ├── 扫描行数: 1,245,678 rows
    └── 临时表使用: Yes (内存消耗大)
    
    优化方案 (3种):
    
    方案1: 添加索引 (改动最小, 提升10倍)
    ─────────────────────────────
    ALTER TABLE orders ADD INDEX idx_create_time (create_time);
    ALTER TABLE orders ADD INDEX idx_user_product (user_id, product_id);
    预期耗时: 0.8秒 ✅
    
    方案2: SQL重写 (中等改动, 提升50倍)
    ─────────────────────────────
    -- 使用子查询替代JOIN，减少中间结果集
    SELECT o.*, u.username, p.name
    FROM (
        SELECT * FROM orders
        WHERE create_time > '2026-01-01'
        ORDER BY create_time DESC
        LIMIT 1000
    ) o
    LEFT JOIN users u ON o.user_id = u.id
    LEFT JOIN products p ON o.product_id = p.id;
    预期耗时: 0.25秒 ✅✅
    
    方案3: 架构级优化 (大改, 提升100倍+)
    ─────────────────────────────
    -- 引入Redis缓存热点数据
    -- 读写分离 (主从复制)
    -- 分库分表 (按时间范围分片)
    预期耗时: <50ms ✅✅✅
    
    推荐方案: 先执行方案1 (立即见效)，后续迭代方案3
    
    是否需要我生成完整的DDL/DML语句？
```

---

#### 🖥️ Frontend Developer（前端开发者）

**最适合的场景**:
- UI/UX调试
- 前端性能优化
- 跨浏览器兼容性

#### ⚙️ Backend Developer（后端开发者）

**最适合的场景**:
- API调试与接口联调
- 微服务问题排查
- 并发编程问题

#### 📱 Mobile Developer（移动端开发者）

**最适合的场景**:
- iOS/Android原生问题
- Flutter/React Native跨平台
- 移动端性能优化

#### ⚡ Performance Engineer（性能工程师）

**最适合的场景**:
- 系统性能瓶颈定位
- JVM/Node.js性能调优
- 全链路压测方案

#### 📝 Documentation Specialist（文档专员）

**最适合的场景**:
- API文档生成
- 技术文档编写
- README/Wiki维护

---

## 📜 21条铁律实战应用

### 铁律速查表

#### 🔴 P0级别（绝对不能违反）

| # | 铁律名称 | 违反示例 | 正确做法 |
|---|---------|---------|---------|
| P0 | **安全第一** | `DROP DATABASE production` | 先备份，再操作，提供回滚方案 |
| P1 | **最小权限** | 请求root/admin权限 | 只读操作只请求读取权限 |
| P2 | **可追溯** | 不记录任何日志 | 每步操作都有审计日志 |
| P3 | **渐进披露** | 一上来就贴几百行代码 | 先说结论，再展开细节 |
| P4 | **失败安全** | 出错后状态不一致 | 自动回滚到上一个稳定状态 |

#### 🟡 D系列（调试铁律）

| # | 铁律名称 | 要点 |
|---|---------|------|
| D1 | 问题复现优先 | 能稳定复现才能准确诊断 |
| D2 | 二分法定位 | 快速缩小问题范围（排除法）|
| D3 | 日志驱动 | 用日志/监控数据说话，不靠猜 |
| D4 | 根因分析 | 找根本原因，不是表面现象 |
| D5 | 影响评估 | 修复前先想清楚副作用 |
| D6 | 测试验证 | 修复后必须有测试证明有效 |
| D7 | 文档沉淀 | 解决方案形成知识库 |
| D8 | 预防复发 | 彻底根治，不留尾巴 |

#### 🟢 DP系列（部署铁律）

| # | 铁律名称 | 要点 |
|---|---------|------|
| DP1 | 环境一致性 | 开发=测试=生产（尽可能）|
| DP2 | 幂等部署 | 重复执行不会出错 |
| DP3 | 回滚能力 | 必须能一键回退 |
| DP4 | 健康检查 | 部署后自动验证 |
| DP5 | 灰度发布 | 小范围→全量 |
| DP6 | 配置外置 | 密钥不入代码 |
| DP7 | 监控告警 | 异常立刻知道 |
| DP8 | 变更记录 | 每次部署可追溯 |

---

### 实战案例：铁律如何救命

#### 案例1: 防止生产事故

**场景**: 用户要求删除一张表的数据

**❌ 违反铁律的操作**:
```
用户: 删除users表中所有测试账号
AI: 好的，执行 DELETE FROM users WHERE role='tester'
结果: 😱 误删了1000个真实用户！
```

**✅ 遵守铁律的操作**:
```
用户: 删除users表中所有测试账号

AI: ⚠️ 收到删除请求，根据【P0: 安全第一】原则，我将按以下步骤执行：

Step 1: 备份 (P4: 失败安全)
   CREATE TABLE users_backup_20260514 AS SELECT * FROM users;

Step 2: 预览影响范围 (D5: 影响评估)
   SELECT COUNT(*) FROM users WHERE role='tester';
   结果: 150 条记录
   
Step 3: 安全删除 (最小权限)
   BEGIN;
   DELETE FROM users WHERE role='tester' AND is_test_account=true;
   -- 额外增加is_test_account条件作为双重保险
   COMMIT;

Step 4: 验证结果 (D6: 测试验证)
   SELECT COUNT(*) FROM users WHERE role='tester';
   结果: 0 条 ✅

Step 5: 文档记录 (P2: 可追溯)
   [记录到审计日志...]

✅ 安全完成！备份表保留7天后自动清理。
如需回滚: INSERT INTO users SELECT * FROM users_backup_20260514;
```

---

#### 案例2: 防止无效调试

**场景**: 系统偶发变慢，用户急着重启服务

**❌ 违反铁律的操作**:
```
用户: 系统好慢，帮我重启一下
AI: 好的，执行 systemctl restart myapp
结果: 问题没解决，反而丢失了现场信息
```

**✅ 遵守铁律的操作**:
```
用户: 系统好慢，帮我重启一下

AI: ⏸️ 暂停！根据【D1: 问题复现优先】和【D3: 日志驱动】原则，
    重启前我们需要先收集诊断信息，否则会丢失现场。

📊 诊断信息收集 (30秒内完成):

1️⃣ 系统资源状况
   top -bn1 | head -20
   free -h
   df -h

2️⃣ 应用进程状态
   ps aux | grep myapp
   netstat -tlnp | grep <port>

3️⃣ 最近错误日志
   tail -100 /var/log/myapp/error.log
   journalctl -u myapp --since "1 hour ago"

4️⃣ 数据库慢查询
   SHOW PROCESSLIST;
   SELECT * FROM mysql.slow_log ORDER BY start_time DESC LIMIT 10;

5️⃣ 网络连通性
   ping <database_host>
   telnet <redis_host> 6379

[等待诊断结果...]

基于诊断结果，我发现:
🔍 根因: 数据库连接池耗尽 (maxPoolSize=10, 当前活跃连接=150)
💡 建议: 不要重启！应该先扩容连接池或排查连接泄漏

是否需要我给出具体的修复方案？
```

---

## 🚫 反幻觉系统详解

### 如何识别AI可能在"幻觉"?

**警示信号** ⚠️:
1. 给出的命令/代码你从未见过
2. 引用的API文档版本不存在
3. 提供的配置参数名很奇怪
4. 过于确定的语气但没有证据
5. 无法提供官方文档链接

### 我们如何防止幻觉?

**Layer 1-3: 输入验证**
```
用户: "帮我用xyz-framework的magic()函数解决问题"

AI思考过程:
[Layer 1: 意图识别]
→ 用户想要使用某个框架的函数

[Layer 2: 能力边界检查]
→ xyz-framework是什么？我不确定这是否存在
→ ⚠️ 触发不确定性标记

[Layer 3: 上下文校验]
→ 搜索知识库... 未找到xyz-framework相关资料
→ ⚠️ 确认: 这个框架可能不存在或非常冷门
```

**输出策略**:
```
AI: ⚠️ 关于您提到的"xyz-framework"，我需要澄清：

1. 我无法确认这个框架是否存在
   可能的情况:
   - 这是一个较新的/小众的框架
   - 框架名称可能有误
   - 这是某个特定公司的内部框架

2. 为了确保给您准确的帮助，请您:
   - 提供该框架的官方文档链接
   - 或者告诉我更多关于这个框架的信息

3. 替代方案:
   如果您是想实现XXX功能，我可以推荐以下成熟方案:
   - 方案A: 使用abc-library (成熟稳定)
   - 方案B: 使用def-tool (社区活跃)

抱歉无法直接回答，但我希望确保不给您错误信息。
```

---

### 幻觉分级与处理

| 级别 | 表现 | 处理方式 |
|------|------|---------|
| 🟢 **可信** | 有官方文档/源码佐证 | 直接输出 |
| 🟡 **可能** | 常见用法但有细微差异 | 标注"建议验证" |
| 🠶 **不确定** | 无明确依据 | 明确说明不确定，提供备选 |
| 🔴 **疑似幻觉** | 与已知事实矛盾 | **停止输出**，请求人工确认 |

---

## 📋 常见任务模板

### 模板1: 新项目Debug

```
[复制此模板并根据实际情况填写]

项目名称: _______________
技术栈: _________________ (如: Spring Boot + Vue + MySQL)
问题描述: _______________
错误信息: _______________
复现步骤:
  1. _________________
  2. _________________
  3. _________________
期望行为: _______________
实际行为: _______________
环境信息:
  - OS: _______________
  - 版本: _______________
  - 相关依赖版本: ________
附件: [日志截图/错误堆栈]

发送给AI，它会自动调度合适的角色来处理
```

---

### 模板2: 生产事故应急

```
[紧急情况使用此模板]

⚠️ 事故等级: P0(致命)/P1(严重)/P2(一般)/P3(轻微)
发生时间: _______________
影响范围: _______________ (如: 所有用户无法下单)
初步现象: _______________
当前状态: _______________ (如: 已重启/仍在异常)
是否已采取临时措施: _____ (如: 是/否，具体措施______)
相关人员: _______________ (如: 已通知PM/运维/DBA)

AI将立即启动应急预案:
1. 止血 (临时恢复服务)
2. 诊断 (找到根因)
3. 修复 (彻底解决问题)
4. 复盘 (预防再次发生)
```

---

### 模板3: 部署上线

```
[标准发布流程]

版本号: v___________
变更内容:
  - Feature: _______________
  - Bugfix: _______________
  - Optimization: __________
涉及服务: _______________
数据库变更: □无 □有(DDL:_________)
配置变更: □无 □有(文件:_________)
回滚方案: _______________
发布窗口: _______________
审批人: _______________

AI将协助完成:
1. 发布前检查清单
2. 灰度发布策略
3. 健康检查验证
4. 监控告警配置
5. 发布后观察要点
```

---

### 模板4: 代码审查

```
[PR/MR审查请求]

仓库分支: _______________
PR链接: __________________
变更文件数: ___个
新增代码行: ___行
删除代码行: ___行
重点关注:
  □ 安全性问题
  □ 性能问题
  □ 代码规范
  □ 逻辑正确性
  □ 测试覆盖
其他说明: _______________

AI将调动Tech Lead + Security Analyst + QA进行联合审查
```

---

## 💡 高级技巧

### 技巧1: 上下文管理

**问题**: 对话太长导致AI遗忘前面的内容

**解决方案**:
```
❌ 错误: 一次性粘贴整个项目代码
✅ 正确: 分阶段提供信息
   Step 1: 先描述问题和现象
   Step 2: 再粘贴相关的错误日志片段
   Step 3: 最后粘贴关键代码段（<200行）
   
   使用"总结"命令保存进度:
   "请总结目前的进展和下一步计划"
   （这会创建一个检查点）
```

---

### 技巧2: 多项目切换

**场景**: 同时在多个项目中工作

**ZERO-TH LAW优势**:
```
项目A (北极星AI):
  "帮我看一下这个Spring Boot的NPE异常"
  
  [AI处理中...]
  
  "好的，问题定位完毕。现在切换到项目B"

项目B (电商系统):
  "前端页面白屏了，帮我排查"
  
  ✅ AI完全不知道项目A的内容
  ✅ 不会有上下文污染
  ✅ 每个项目都是独立会话
```

---

### 技巧3: 自定义工作流

**场景**: 团队有特定的开发流程

**如何定制**:
```
在项目根目录创建 .debug-deploy-config.yaml:

team_workflow:
  code_review_required: true
  reviewers:
    - tech_lead
    - security_analyst
  test_coverage_threshold: 80
  
deployment_pipeline:
  pre_deploy_checklist:
    - database_backup
    - config_review
    - security_scan
  approval_required: true
  approvers:
    - project_manager
    - devops_engineer

notification:
  on_success:
    - email: team@company.com
    - slack: #deployments
  on_failure:
    - pagerduty: oncall_engineer
    - phone: +86-xxx-xxxx-xxxx
```

---

### 技巧4: 知识库积累

**场景**: 解决过的问题不想重复解决

**方法**:
```
每次AI解决问题后，要求它生成知识库条目:

"请将这次的问题和解决方案整理成知识库条目"

输出示例:
---
title: Spring Boot HikariCP连接池超时
category: database
severity: medium
symptoms: |
  - 晚上10点后频繁出现
  - 错误: Connection timeout after 30000ms
  - 影响用户下单功能
root_cause: |
  HikariCP maxPoolSize配置过小(默认10),
  高峰期连接不够用
solution: |
  修改application.yml:
  spring.datasource.hikari.maximum-pool-size: 50
prevention: |
  - 定期监控连接池使用率
  - 设置连接池告警阈值80%
  - 高峰期前提前扩容
tags: [spring-boot, mysql, connection-pool, performance]
created_at: 2026-05-14
---

下次遇到类似问题，AI可以直接检索知识库！
```

---

## 🐛 故障排除

### 常见问题FAQ

#### Q1: AI似乎没有遵循21条铁律怎么办？

**可能原因**:
- 对话上下文过长，铁律被"遗忘"
- 问题描述过于模糊，AI判断不需要严格遵循

**解决方案**:
```
明确提醒AI:
"请注意严格遵守P0安全第一原则"
"按照D1-D8调试铁律的标准流程执行"
"请先输出你的执行计划，让我审核后再执行"
```

---

#### Q2: AI给出的方案不可行怎么办？

**可能原因**:
- AI产生了幻觉（虽然概率很低）
- 方案基于过时的信息
- 环境特殊性未被考虑到

**解决方案**:
```
1. 要求AI提供依据:
   "这个方案的依据是什么？有官方文档吗？"

2. 提供更多上下文:
   "实际上我们的环境是XXX，所以这个方案不太适用"

3. 请求备选方案:
   "有没有其他替代方案？"

4. 手动验证:
   在测试环境先验证，不要直接在生产环境执行
```

---

#### Q3: 多项目并行时互相干扰？

**正常现象**: ZERO-TH LAW保证不会干扰

但如果感觉有问题:
```
1. 确认你在不同窗口/会话中操作不同项目
2. 每次切换项目时明确声明:
   "我现在在项目A中工作"
3. 如果仍有疑虑，清除会话历史重新开始
```

---

#### Q4: 如何提高AI的回答质量？

**最佳实践**:
```
✅ 清晰描述问题（包含错误信息、日志、复现步骤）
✅ 提供足够的上下文（但不冗余）
✅ 明确期望的输出格式
✅ 分步骤沟通，不要一次性提太多需求
✅ 及时反馈AI的回答是否符合预期

❌ 模糊的描述："我的程序坏了"
❌ 一次性粘贴几千行代码
❅ 不给任何背景就问"怎么做"
❅ 频繁打断AI的思路
```

---

## 🏆 最佳实践

### 实践1: 建立"人机协作"工作流

```
人类负责:
✅ 业务理解和需求定义
✅ 最终决策和审批
✅ 创造性和战略性的工作
✅ 处理例外和边缘情况

AI负责:
✅ 重复性和机械性任务
✅ 信息搜集和整理
✅ 初步分析和方案生成
✅ 文档编写和测试用例

黄金法则:
"AI提出方案 → 人类审核 → AI执行 → 人类验证"
```

---

### 实践2: 渐进式信任建立

```
Level 1: 观察者模式
   - 只让AI分析，不执行任何操作
   - 验证AI的分析是否准确
   
Level 2: 半自动模式
   - 让AI执行非关键操作（读操作）
   - 关键操作仍由人工完成
   
Level 3: 受限自动模式
   - 允许AI执行大部分操作
   - 但危险操作需人工确认
   
Level 4: 全自动模式（仅限测试环境）
   - AI全权处理
   - 人工仅做最终抽查
```

---

### 实践3: 持续改进循环

```
每次任务完成后:

1. 评分
   "这次协作我打 8/10 分"
   
2. 反馈
   "做得好的地方: XXX"
   "需要改进的地方: XXX"
   
3. 记录
   将经验教训写入知识库
   
4. 分享
   与团队成员分享最佳实践
```

---

## 📞 获取帮助

### 内置帮助命令

随时可以询问AI:
```
"你能做什么？"           → 查看能力列表
"解释一下ZERO-TH LAW"   → 了解隔离原则
"列出21条铁律"          → 查看完整规则
"我应该如何描述问题？"   → 获取提问技巧
"给我一个任务模板"      → 获取常用模板
```

---

### 外部资源

| 资源 | 链接 |
|------|------|
| GitHub仓库 | https://github.com/xuanji-ai-2026/group-debug-deploy-expert |
| Issue反馈 | https://github.com/xuanji-ai-2026/group-debug-deploy-expert/issues |
| 技术支持邮箱 | z18288090942@gmail.com |
| 商务合作邮箱 | 382201222@qq.com |
| 开发者电话 | +86 19537722739 |

---

## 📄 版本历史

| 版本 | 日期 | 主要更新 |
|------|------|---------|
| v1.0.1 | 2026-05-14 | 初始正式版，包含完整功能集 |
| v1.1.0 | 计划中 | Web Dashboard、团队协作、国际化 |

---

<div align="center">

**祝您使用愉快！如有任何问题，欢迎随时联系我们** 🚀

Made with ❤️ by [云南坤灿科技有限公司](mailto:382201222@qq.com)

</div>
