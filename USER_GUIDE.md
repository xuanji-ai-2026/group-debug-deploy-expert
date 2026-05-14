# 📚 Group Debug & Deploy Expert - User Guide
<!-- 用户使用手册 v1.0.1 -->

> **Complete Guide from Beginner to Expert** - Master the enterprise-grade AI debugging and deployment expert team quickly and efficiently  
> **从入门到精通的完整指南** - 快速掌握企业级调试部署AI专家团队的使用方法

---

## 📖 Table of Contents / 目录

1. [Quick Start (5-Minute Onboarding) / 快速开始](#-quick-start-5-minute-onboarding--快速开始5分钟上手)
2. [Installation & Configuration / 安装与配置](#-installation--configuration--安装与配置)
3. [Core Concepts / 核心概念理解](#-core-concepts--核心概念理解)
4. [11-Roles Team Usage Guide / 11角色团队使用指南](#-11-roles-team-usage-guide--11角色团队使用指南)
5. [21 Iron Principles in Action / 21条铁律实战应用](#-21-iron-principles-in-action--21条铁律实战应用)
6. [Anti-Hallucination System / 反幻觉系统详解](#-anti-hallucination-system--反幻觉系统详解)
7. [Common Task Templates / 常见任务模板](#-common-task-templates--常见任务模板)
8. [Advanced Techniques / 高级技巧](#-advanced-techniques--高级技巧)
9. [Troubleshooting / 故障排除](#-troubleshooting--故障排除)
10. [Best Practices / 最佳实践](#-best-practices--最佳实践)

---

## 🚀 Quick Start (5-Minute Onboarding) / 快速开始（5分钟上手）

### ✅ Pre-Installation Checklist / 前置检查清单

Before you begin, please confirm: / 在开始之前，请确认：

- [ ] You have installed a compatible platform: Trae IDE / Cursor / OpenClaw / Hermes / VSCode / Claude Code  
   <!-- 已安装兼容平台：Trae IDE / Cursor / OpenClaw / Hermes 等 -->
- [ ] Downloaded and extracted the skill package to the correct directory  
   <!-- 已下载并解压技能包到正确目录 -->
- [ ] Have a project ready for debugging or deployment (any project type works!)  
   <!-- 有一个待调试或部署的项目（可以是任何项目） -->

---

### 🎯 Your First Task: Hello World Debugging / 第一个任务：Hello World调试

#### Scenario Setup / 场景设定

Assume you have a **Spring Boot project** with this startup error:  
假设您有一个 **Spring Boot 项目**，启动时出现错误：

```
Failed to configure a DataSource: 'url' attribute is not specified
<!-- 数据源配置失败：未指定'url'属性 -->
```

#### Step-by-Step Instructions / 操作步骤

**Step 1: Activate the Skill Pack / 启动技能包**

After opening your project in your IDE, simply tell the AI assistant:  
在您的IDE中打开项目后，直接对AI助手说：

```
Help me troubleshoot this Spring Boot datasource configuration issue
<!-- 帮我排查这个Spring Boot数据源配置问题 -->
```

**Step 2: Observe the AI Team Collaboration / 观察AI团队的协作**

You will see a workflow like this: / 您会看到类似这样的工作流程：

```
🎯 Project Manager: Task received, analyzing...
   <!-- 项目经理：收到任务，正在分析... -->
   ├─ Task Type: Debugging (Debug) / 任务类型：调试
   ├─ Complexity Assessment: Medium (P2) / 复杂度评估：中等
   └─ Assigned Roles: DBA + Backend Dev / 分配角色：DBA + 后端开发

🗄️ DBA: Analyzing datasource configuration...
   <!-- 数据库管理员：正在分析数据源配置... -->
   ├─ Checking application.yml/properties / 检查配置文件
   ├─ Validating database connection parameters / 验证数据库连接参数
   └─ Issue Found: Missing spring.datasource.url config / 发现问题：缺少数据源URL配置

⚙️ Backend Dev: Generating fix solution...
   <!-- 后端开发：正在生成修复方案... -->
   ├─ Recommending configuration format / 推荐配置格式
   ├─ Providing example code / 提供示例代码
   └─ Noting important considerations / 标注注意事项

🛡️ Security Analyst: Security review in progress...
   <!-- 安全分析师：安全审查中... -->
   ├─ Checking hardcoded password risks ✓ / 检查密码硬编码风险 ✓
   ├─ Validating environment variable usage ✓ / 验证环境变量使用建议 ✓
   └─ Review passed ✓ / 审查通过 ✓

✅ Task Complete! Outputting fix report / 任务完成！输出修复报告
```

**Step 3: Apply the Fix / 应用修复**

Based on the AI's solution, modify your configuration file:  
根据AI提供的方案修改配置文件：

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_database
    username: your_username
    password: ${DB_PASSWORD}  # Recommended: use environment variables / 推荐使用环境变量
    driver-class-name: com.mysql.cj.jdbc.Driver
```

**Step 4: Verify Results / 验证结果**

Restart your project and confirm the issue is resolved! / 重新启动项目，确认问题已解决！

---

## 🔧 Installation & Configuration / 安装与配置

### Method 1: Automated Installer (Recommended) / 方法一：自动安装器（推荐）

#### For Windows Users / Windows用户：

```powershell
# Run PowerShell as Administrator / 以管理员身份运行PowerShell
.\install.ps1

# The installer will automatically:
# 安装器将自动执行：
# ✅ Check system requirements / 检查系统要求
# ✅ Verify file integrity (SHA256) / 验证文件完整性(SHA256)
# ✅ Install all components / 安装所有组件
# ✅ Generate installation report / 生成安装报告
# ✅ Open user manual / 打开用户手册
```

#### For Linux/macOS Users / Linux/macOS用户：

```bash
# Make installer executable / 赋予执行权限
chmod +x install.sh

# Run installer / 运行安装器
./install.sh

# Or with sudo if needed / 如需权限使用sudo
sudo ./install.sh
```

### Method 2: Manual Installation / 方法二：手动安装

```bash
# Extract to skill directory / 解压到技能目录
~/.trae/skills/group-debug-deploy-expert/

# Restart your IDE / 重启IDE
# The skill will be auto-detected! / 技能将被自动检测！
```

### Expected Installation Output / 预期安装输出

```
╔══════════════════════════════════════════════════════════╗
║ 🛡️  Group Debug & Deploy Expert Installer v1.0.1         ║
╚══════════════════════════════════════════════════════════╝

▶ Step 1/6: Checking system requirements...        ← 系统检查
✅ Shell: Bash 5.1.16 / PowerShell 5.1
✅ Disk space: Available >10MB / 磁盘空间充足
✅ Write permissions: OK / 写入权限正常

▶ Step 2/6: Verifying package integrity...       ← 完整性校验
✅ SHA256 verification passed (13 files)          ← SHA256校验通过
✅ File count verified: 18+ files present         ← 文件数量正确

▶ Step 3/6: Installing package...                ← 正在安装
✅ Installed 18 files to target directory         ← 文件已安装

▶ Step 4/6: Verifying installation...            ← 验证安装
✅ Core skill file: OK                            ← 核心文件正常
✅ License file: OK                               ← 许可证正常
✅ Documentation: Complete                        ← 文档完整

▶ Step 5/6: Generating report...                 ← 生成报告
✅ Report saved to: install-report.txt            ← 报告已保存

▶ Step 6/6: Showing guide...                     ← 显示指南

┌─────────────────────────────────────────────┐
│  ✅ STATUS: INSTALLED SUCCESSFULLY           │  ← 安装成功！
└─────────────────────────────────────────────┘

📖 Next Steps: / 下一步：
  1. Read README.md / 阅读主文档
  2. Try your first debug task / 尝试第一个调试任务
  3. Explore 21 Iron Principles / 探索21条铁律
```

---

## 🧠 Core Concepts / 核心概念理解

### What is Group Debug & Deploy Expert? / 什么是通用调试部署专家团队？

**Definition:** / **定义：**
An enterprise-grade AI framework featuring **11 specialized digital worker roles** that collaborate to provide comprehensive debugging and deployment solutions for complex projects.  
企业级AI框架，包含 **11个专业数字员工角色**，协同工作为复杂项目提供全面的调试和部署解决方案。

**Key Innovation:** / **核心创新：**
- **ZERO-TH LAW**: Absolute project isolation - guarantees zero cross-project contamination  
  **零号法则**：绝对项目隔离 - 保证零跨项目污染
- **21 Iron Principles**: Immutable rules ensuring quality, security, and reliability  
  **21条铁律**：不可变规则，确保质量、安全和可靠性
- **Anti-Hallucination System**: 12-layer defense against AI-generated false information  
  **反幻觉系统**：12层防御机制，防止AI生成虚假信息

---

### The 11 AI Digital Worker Roles / 11个AI数字员工角色

| Role | Chinese Name | Primary Responsibility | 主要职责 |
|------|-------------|------------------------|----------|
| **Architect** | 架构师 | System design & architecture review | 系统设计与架构审查 |
| **Backend Specialist** | 后端专家 | Server-side debugging (Java/Python/Node.js/Go) | 服务端调试 |
| **Frontend Specialist** | 前端专家 | Client-side issues (React/Vue/Angular) | 客户端问题处理 |
| **DevOps Engineer** | 运维工程师 | CI/CD, Docker, K8s, cloud deployment | 持续集成与部署 |
| **QA Engineer** | 测试工程师 | Test strategy & automation | 测试策略与自动化 |
| **Security Expert** | 安全专家 | Vulnerability assessment & hardening | 漏洞评估与加固 |
| **Data Engineer** | 数据工程师 | Database optimization & ETL pipelines | 数据库优化 |
| **Mobile Developer** | 移动开发 | iOS/Android debugging | 移动端调试 |
| **Product Owner** | 产品负责人 | Requirements validation | 需求验证 |
| **Scrum Master** | 敏捷教练 | Process optimization | 流程优化 |
| **Technical Writer** | 技术文档 | Documentation generation | 文档生成 |

**How They Collaborate:** / **协作方式：**
When you submit a task, the **Project Manager** analyzes complexity and assigns appropriate roles. Each role works within their expertise domain, then results are synthesized into a comprehensive solution.  
当您提交任务时，**项目经理**会分析复杂度并分配合适的角色。每个角色在其专业领域内工作，然后将结果综合成完整的解决方案。

---

## 🛡️ 21 Iron Principles in Action / 21条铁律实战应用

### Category 1: Truth Principles (T1-T4) / 真实性原则

| Principle | Rule | Practical Application | 实际应用 |
|-----------|------|-----------------------|----------|
| **T1** | No Guessing | Never assume - always verify with evidence | 绝不猜测 - 必须有证据验证 |
| **T2** | Evidence-Based | All conclusions must have source code/log proof | 所有结论必须有代码/日志证明 |
| **T3** | Precision | Exact error messages, line numbers, stack traces | 精确的错误信息、行号、堆栈跟踪 |
| **T4** | Completeness | Full context, no partial information | 完整上下文，不允许部分信息 |

**Example Usage:** / **使用示例：**
```
❌ Bad: "There might be a null pointer somewhere" / 错误："可能有空指针"
✅ Good: "NullPointerException at UserService.java:142, variable 'user' is null because database query returned empty set on line 138"  
      正确："UserService.java:142出现空指针异常，变量'user'为空，因为第138行数据库查询返回空集合"
```

---

### Category 2: Operational Discipline (O1-O5) / 操作纪律原则

| Principle | Rule | Description | 说明 |
|-----------|------|-------------|------|
| **O1** | Structured Workflow | Follow predefined debugging steps | 遵循预定义的调试步骤 |
| **O2** | Single Responsibility | One task, one focus | 单一任务，单一焦点 |
| **O3** | Checkpoint System | Save progress for long tasks | 为长任务保存进度 |
| **O4** | Rollback Capability | Always maintain rollback path | 始终保持回滚路径 |
| **O5** | Logging Standards | Consistent log format | 一致的日志格式 |

---

### Category 3: Environment Control (E1-E4) / 环境控制原则

| Principle | Rule | Key Point | 关键点 |
|-----------|------|-----------|--------|
| **E1** | Project Isolation | ZERO-TH LAW enforcement | 零号法则强制执行 |
| **E2** | Context Boundaries | Strict separation between projects | 项目间严格分离 |
| **E3** | Environment Parity | Dev/Test/Prod consistency | 开发/测试/生产环境一致性 |
| **E4** | Secret Protection | Zero credential leakage | 凭据零泄露 |

**ZERO-TH LAW Explained:** / **零号法则详解：**
This is the **most critical principle** - it guarantees that when working on Project A, absolutely no information from Project B can leak or contaminate the context.  
这是**最关键的原则** - 保证在处理项目A时，项目B的信息绝对不会泄露或污染上下文。

---

## 🎯 Anti-Hallucination System / 反幻觉系统详解

### What is AI Hallucination? / 什么是AI幻觉？

**Definition:** / **定义：**
When AI generates plausible-sounding but factually incorrect information, code that doesn't exist, or solutions that look correct but fail in practice.  
AI生成听起来合理但事实不正确的信息、不存在的代码、或看起来正确但实际失败的解决方案。

**Our Defense System (12 Layers):** / **我们的防御系统（12层）：**

| Layer | Mechanism | Function | 功能 |
|-------|-----------|----------|------|
| **H13** | Source Verification | Check if referenced code actually exists | 检查引用的代码是否真实存在 |
| **H14** | Command Validation | Verify commands before execution | 执行前验证命令有效性 |
| **H15** | Result Fact-Checking | Validate execution outputs against expectations | 验证执行输出是否符合预期 |
| **H16** | Log Cross-Reference | Compare logs with stated conclusions | 对比日志和结论的一致性 |
| **H17** | Confidence Scoring | Rate solution reliability (0-100%) | 评估解决方案可靠性(0-100%) |

**Practical Example:** / **实际示例：**
```
User Request: "Fix my Nginx configuration" / 用户请求："修复我的Nginx配置"

❌ Without Anti-Hallucination:
   "Add 'server_name example.com;' to nginx.conf" 
   → But this line already exists! / 但这行已经存在了！

✅ With Anti-Hallucination (H13):
   [Scanning nginx.conf...] / [扫描nginx.conf...]
   [Found 'server_name' at line 12] / [在第12行发现'server_name']
   ⚠️ Warning: Directive already exists. Actual issue: missing semicolon on line 15.
   → Correct fix provided / 提供正确修复方案
```

---

## 📋 Common Task Templates / 常见任务模板

### Template 1: Backend Debugging / 模板1：后端调试

**Trigger Phrase:** / **触发短语：**
```
Debug my [Spring Boot/Express/FastAPI/Django] backend issue
<!-- 调试我的[后端框架]后端问题 -->
```

**What Happens:** / **执行过程：**
1. **Backend Specialist** analyzes stack traces and error logs  
   **后端专家**分析堆栈跟踪和错误日志
2. **Data Engineer** checks database queries and connections  
   **数据工程师**检查数据库查询和连接
3. **Security Expert** reviews for vulnerabilities  
   **安全专家**审查潜在漏洞
4. **DevOps Engineer** validates deployment configuration  
   **运维工程师**验证部署配置
5. **Comprehensive report** with root cause and fix  
   **综合报告**包含根因和修复方案

**Expected Output Format:** / **预期输出格式：**
```markdown
## Debug Report: [Project Name] / 调试报告：[项目名]

### Root Cause Analysis / 根因分析
- **Error Type:** [Classification] / 错误类型：[分类]
- **Location:** File:Line / 位置：文件:行号
- **Trigger Condition:** When does it occur? / 触发条件：何时发生？

### Solution / 解决方案
\`\`\`[language]
// Fixed code here / 修复后的代码
\`\`\`

### Verification Steps / 验证步骤
1. [ ] Apply the fix / 应用修复
2. [ ] Restart service / 重启服务
3. [ ] Test with [scenario] / 使用[场景]测试
4. [ ] Check logs for errors / 检查日志是否有错误

### Prevention / 预防措施
- [Tip 1] / [提示1]
- [Tip 2] / [提示2]
```

---

### Template 2: Frontend Issue Resolution / 模板2：前端问题解决

**Trigger Phrase:** / **触发短语：**
```
Fix my [React/Vue/Angular] frontend [bug/feature/performance issue]
<!-- 修复我的[前端框架]前端[bug/功能/性能问题] -->
```

**Specialized Roles Involved:** / **参与的专业角色：**
- **Frontend Specialist**: UI/UX analysis, component debugging  
  **前端专家**：UI/UX分析、组件调试
- **Architect**: State management pattern review  
  **架构师**：状态管理模式审查
- **QA Engineer**: Cross-browser compatibility testing  
  **测试工程师**：跨浏览器兼容性测试
- **Technical Writer**: User-facing documentation updates  
  **技术文档**：面向用户的文档更新

---

### Template 3: Deployment Troubleshooting / 模板3：部署故障排除

**Trigger Phrase:** / **触发短语：**
```
Help me deploy to [Docker/Kubernetes/AWS/Azure/GCP]
<!-- 帮我部署到[Docker/Kubernetes/云平台] -->
```

**Comprehensive Coverage:** / **全面覆盖：**
- **DevOps Engineer**: CI/CD pipeline, containerization, orchestration  
  **运维工程师**：CI/CD流水线、容器化、编排
- **Security Expert**: Network policies, secrets management, HTTPS setup  
  **安全专家**：网络策略、密钥管理、HTTPS配置
- **Backend Specialist**: Environment variables, health checks  
  **后端专家**：环境变量、健康检查
- **Data Engineer**: Database migration, backup strategy  
  **数据工程师**：数据库迁移、备份策略

---

## 💡 Advanced Techniques / 高级技巧

### Technique 1: Progressive Context Loading / 技巧1：渐进式上下文加载

For large projects, use the **3-level loading system** to optimize token usage:  
对于大型项目，使用 **3级加载系统** 优化Token使用：

| Level | Content | Token Cost | When to Use | 适用场景 |
|-------|---------|------------|-------------|----------|
| **L1 - Summary** | Project overview, tech stack | Low (~500 tokens) | Initial analysis | 初始分析 |
| **L2 - Detailed** | Module structure, key files | Medium (~2000 tokens) | Deep debugging | 深度调试 |
| **L3 - Full** | Complete codebase access | High (variable) | Critical issues | 关键问题 |

**How to Use:** / **如何使用：**
```
Level 1: "Give me an overview of my project architecture" / "给我项目架构概览"
Level 2: "Now analyze the authentication module in detail" / "现在详细分析认证模块"
Level 3: "Show me the complete AuthService.java implementation" / "显示AuthService.java完整实现"
```

---

### Technique 2: Checkpoint & Resume / 技巧2：检查点与恢复

For long-running tasks (>10 minutes), the system automatically:  
对于长时间运行的任务（>10分钟），系统自动：

1. **Saves progress** at key milestones / 在关键里程碑**保存进度**
2. **Generates checkpoint ID** for resumption / **生成检查点ID**用于恢复
3. **Allows interruption** without losing work / 允许**中断**而不丢失工作

**Example:** / **示例：**
```
[Checkpoint Saved] ID: chkpt_20260514_143022
Progress: 60% complete - Backend analysis done, starting frontend review
/* 检查点已保存 */
/* 进度：60%完成 - 后端分析完成，开始前端审查 */

[User interrupts to handle urgent task]
/* 用户中断处理紧急任务 */

[Resume later]
> Resume from checkpoint: chkpt_20260514_143022
✅ Resumed at 60% - Continuing frontend review...
/* 从检查点恢复 */
/* 从60%继续 - 继续前端审查... */
```

---

### Technique 3: Multi-Project Isolation Mode / 技巧3：多项目隔离模式

When working with multiple projects simultaneously:  
当同时处理多个项目时：

```bash
# Activate isolation mode / 激活隔离模式
"Enable ZERO-TH LAW strict mode for Project Alpha"
/* 为Alpha项目启用零号法则严格模式 */

# Now all operations are isolated / 现在所有操作都是隔离的
"Debug Project Alpha's login issue"
/* 调试Alpha项目的登录问题 */
→ Only sees Alpha's files, never leaks Beta's data / 只看到Alpha的文件，绝不泄露Beta的数据

# Switch projects safely / 安全切换项目
"Now switch to Project Beta and fix API endpoint"
/* 现在切换到Beta项目并修复API端点 */
→ Complete context reset, zero contamination / 完全上下文重置，零污染
```

---

## 🔍 Troubleshooting / 故障排除

### Common Issues & Solutions / 常见问题与解决方案

#### Issue 1: Skill Not Detected / 问题1：技能未被检测到

**Symptoms:** / **症状：**
- AI doesn't respond to debug commands / AI不响应调试命令
- Error: "Skill not found" / 错误："技能未找到"

**Solutions:** / **解决方案：**
1. Verify installation directory: `ls ~/.trae/skills/group-debug-deploy-expert/`  
   验证安装目录
2. Check SKILL.md exists and is not empty (should be >1KB)  
   检查SKILL.md存在且非空（应>1KB）
3. Restart IDE completely (not just reload window)  
   完全重启IDE（不仅是重新加载窗口）
4. Clear cache: Delete `~/.trae/cache/` and restart  
   清除缓存：删除`~/.trae/cache/`并重启

---

#### Issue 2: Poor Quality Responses / 问题2：响应质量差

**Symptoms:** / **症状：**
- Generic suggestions without specific code / 没有具体代码的通用建议
- Suggestions don't match your tech stack / 建议与技术栈不匹配

**Solutions:** / **解决方案：**
1. Provide more context: Include error logs, stack traces, relevant code snippets  
   提供更多上下文：包含错误日志、堆栈跟踪、相关代码片段
2. Specify tech stack explicitly: "I'm using Spring Boot 3.x with MySQL 8.0"  
   明确指定技术栈："我使用Spring Boot 3.x和MySQL 8.0"
3. Reference specific files: "Check UserService.java around line 150"  
   引用特定文件："检查UserService.java第150行左右"

---

#### Issue 3: Slow Response Time / 问题3：响应时间慢

**Optimizations:** / **优化方法：**
1. Use **Progressive Context Loading** (start at Level 1)  
   使用**渐进式上下文加载**（从L1级开始）
2. Break complex tasks into smaller sub-tasks  
   将复杂任务拆分为更小的子任务
3. Enable **checkpoint mode** for long tasks to allow pauses  
   对长任务启用**检查点模式**允许暂停

---

## ✨ Best Practices / 最佳实践

### Do's (Recommended Practices) / 推荐做法 ✅

1. **Always Provide Context** / **始终提供上下文**
   - Include error messages, logs, and relevant code / 包含错误信息、日志和相关代码
   - Specify your tech stack and versions / 指定技术栈和版本
   - Describe what you've already tried / 描述已尝试的方法

2. **Start Simple, Then Deepen** / **由简入深**
   - Begin with high-level overview tasks / 从高层次概览任务开始
   - Progressively drill down as needed / 按需要逐步深入
   - Let the AI guide you to the right level of detail / 让AI引导您找到合适的详细程度

3. **Use Role-Specific Requests** / **使用角色特定请求**
   - "Ask the Security Expert to review auth code" / "让安全专家审查认证代码"
   - "Have the DevOps Engineer check Docker config" / "让运维工程师检查Docker配置"
   - This leverages specialized knowledge effectively / 这样能有效利用专业知识

4. **Verify Before Applying** / **应用前先验证**
   - Review AI-generated code before deploying / 部署前审查AI生成的代码
   - Test in staging environment first / 先在预发布环境测试
   - Keep backups of original files / 保留原始文件备份

### Don'ts (Anti-Patterns) / 避免做法 ❌

1. **Never Skip Evidence Requirements** / **绝不跳过证据要求**
   - Don't accept "it should work" without proof / 不要接受没有证据的"应该可以"
   - Demand exact file paths, line numbers, error codes / 要求精确的文件路径、行号、错误码

2. **Don't Ignore ZERO-TH LAW** / **不要忽略零号法则**
   - When switching projects, always confirm context reset / 切换项目时始终确认上下文重置
   - Never reference Project B's data while working on Project A / 处理项目A时绝不能引用项目B的数据

3. **Don't Blindly Copy-Paste** / **不要盲目复制粘贴**
   - Understand *why* the fix works, not just *what* the fix is / 理解修复*为什么*有效，而不仅仅是*什么*是修复
   - Adapt solutions to your specific context / 根据您的具体情况调整方案

---

## 📞 Support & Help / 支持与帮助

### Getting Assistance / 获取帮助

**Documentation Resources:** / **文档资源：**
- 📄 [README.md](./README.md) - Main documentation (720 lines, bilingual) / 主文档（720行，双语）
- 📊 [USE_CASES.md](./USE_CASES.md) - 13 industry case studies / 13个行业案例研究
- 🏆 [COMPETITIVE_ADVANTAGE.md](./COMPETITIVE_ADVANTAGE.md) - Technical advantages / 技术优势分析

**Contact Information:** / **联系信息：**
- 👤 **Developer:** 周凤雄 (Henry Chow)
- 🏢 **Company:** 云南坤灿科技有限公司 (YunNan KunCan Technology Co., Ltd.)
- 📧 **Email:** z18288090942@gmail.com
- 📱 **Phone:** +86 19537722739 / +86 13114190794
- 🌐 **GitHub Issues:** https://github.com/xuanji-ai-2026/group-debug-deploy-expert/issues
- 📦 **npm Package:** https://www.npmjs.com/package/@zfx1818/open-group

### Community & Feedback / 社区与反馈

- **Report Bugs:** Use GitHub Issues with template / 使用GitHub Issues模板报告Bug
- **Feature Requests:** Welcome contributions and suggestions / 欢迎贡献和建议
- **Questions:** Email or GitHub Discussions / 邮件或GitHub讨论区

---

## 📄 License Information / 许可证信息

**Dual License Model:** / **双重许可模式：**

| License Type | Usage Scope | Cost | 费用 |
|--------------|-------------|------|------|
| **Community License** | Personal, educational, open-source projects | ✅ Free / 免费 | $0 |
| **Commercial License** | Company use, business products, commercial services | 💰 Paid / 付费 | Contact for pricing / 联系获取报价 |

**For commercial licensing inquiries:** / **商业许可咨询：**
- 📧 Email: z18288090942@gmail.com
- 📱 Phone: +86 19537722739
- See [LICENSE](./LICENSE) file for complete terms / 查看[LICENSE](./LICENSE)文件了解完整条款

---

## 🎯 Quick Reference Card / 速查卡

### Essential Commands / 常用命令

| Command | Purpose | 用途 |
|---------|---------|------|
| `"Debug [issue]"` | Start debugging session | 开始调试会话 |
| `"Deploy to [target]"` | Initiate deployment workflow | 启动部署工作流 |
| `"Review [file/component]"` | Code review request | 代码审查请求 |
| `"Explain [error]"` | Error explanation | 错误解释 |
| `"Optimize [component]"` | Performance optimization | 性能优化 |
| `"Test [feature]"` | Testing assistance | 测试协助 |
| `"Secure [code]"` | Security audit | 安全审计 |
| `"Switch to [project]"` | Project context switch | 项目上下文切换 |
| `"Resume [checkpoint]"` | Continue interrupted task | 继续被中断的任务 |

### Key Principles Quick Ref / 关键原则速查

| Code | Principle | One-Line Summary | 一句话总结 |
|------|-----------|-------------------|-----------|
| **ZTL** | ZERO-TH LAW | Absolute project isolation / 绝对项目隔离 |
| **T1-T4** | Truth | Evidence-based only / 仅基于证据 |
| **O1-O5** | Operations | Structured workflow / 结构化工作流 |
| **E1-E4** | Environment | Controlled boundaries / 受控边界 |
| **H13-H17** | Anti-Hallucination | 12-layer defense / 12层防御 |

---

**Document Version:** v1.0.1  
**Last Updated:** 2026-05-14  
**Compatible Platforms:** Trae IDE, OpenClaw, Hermes, Cursor, VSCode, Claude Code

<!-- 文档版本：v1.0.1 -->
<!-- 最后更新：2026-05-14 -->
<!-- 兼容平台：Trae IDE, OpenClaw, Hermes, Cursor, VSCode, Claude Code -->

---

*Thank you for choosing Group Debug & Deploy Expert! We hope this guide helps you unlock the full potential of your AI debugging team.*  
*感谢您选择通用调试部署专家团队！希望本手册帮助您充分发挥AI调试团队的潜力。*

*For issues or suggestions, please contact: z18288090942@gmail.com*  
*如有问题或建议，请联系：z18288090942@gmail.com*
