# Project Governance Rules — 项目治理规则（通用框架）

> **⚖️ CONSTITUTIONAL DOCUMENT — TIER 0 PROTECTED**
>
> **适用实例**: BeijiXing-AI (PROJ-BJX-001) | **框架性质**: UNIVERSAL (跨项目通用)
>
> 本文件受 [core_file_protection.md](./core_file_protection.md) 宪法级保护。
> 修改本文件需要Human显式授权。

## 🔗 GOVERNANCE FRAMEWORK (治理框架)

本项目采用**三层宪法级治理架构**：

```
╔═══════════════════════════════════════════════════════════════╗
║           Universal Governance Hierarchy                      ║
║     （通用治理框架 - 适用于所有项目实例）                     ║
╚═══════════════════════════════════════════════════════════════╝

TIER 0: CORE FILE PROTECTION CONSTITUTION (本文件的上位法)
├── File: .trae/rules/core_file_protection.md
├── Authority: 定义所有文件的保护分级、权限矩阵、审核流程
├── Status: 🔴 ABSOLUTE LOCK — 宪法级，不可自主修改
└── Scope: 全局适用，所有其他规则文件必须遵守

TIER 1: PROJECT RULES (本文件)
├── File: .trae/rules/project_rules.md
├── Authority: 定义项目特定的行为准则、修改规范
├── Status: 🟠 STRICT CONTROL — 受TIER 0约束
└── Scope: 项目级操作规范

TIER 2: SKILL DEFINITION (技能定义)
├── File: .trae/skills/group-debug-deploy-expert/SKILL.md
├── Authority: 定义AI agent的具体行为原则和操作协议
├── Status: 🔴 ABSOLUTE LOCK — 宪法级，不可自主修改
└── Scope: AI agent执行任务时必须遵循的21铁原则

RULE: TIER 0 > TIER 1 > TIER 2 (上位法优先)
```

**重要**: 当本文件与 [core_file_protection.md](./core_file_protection.md) 冲突时，**以TIER 0文件为准**。

---

## 🔒 CORE FILE PROTECTION POLICY (核心文件保护政策)

> ⚠️ **详细保护机制请参阅 [core_file_protection.md](./core_file_protection.md)**
> 
> 本节为摘要版本，完整定义（含权限矩阵、审核流程、防篡改机制）见TIER 0宪法文件。

### PROTECTED FILES (受保护文件清单)

以下文件为项目的"宪法级"文件，具有**只读属性**（除非满足修改门槛）：

| File | Protection Level | Owner | Description |
|------|-----------------|-------|-------------|
| `.trae/skills/group-debug-deploy-expert/SKILL.md` | **TIER 0: CONSTITUTIONAL** | Human (Project Owner) | 核心技能定义，21铁原则，11角色团队，所有协议 |
| `.trae/rules/project_rules.md` | **TIER 0: CONSTITUTIONAL** | Human (Project Owner) | 本文件——项目治理规则本身 |
| `.trae/rules/core_file_protection.md` | **TIER 0: CONSTITUTIONAL** | Human (Project Owner) | 核心文件保护宪法——保护机制的根基 |

### PROTECTION LEVELS

```
LEVEL 1: CONSTITUTIONAL (宪法级)
├── 属性: 只读、固化、权威
├── 修改门槛: 仅限以下情况之一：
│   ① 证明存在重大缺陷导致实际操作错误或安全风险
│   ② 项目架构发生根本性变化（需人工决策）
│   ③ Human明确指令修改（显式授权）
├── 禁止行为:
│   ✗ AI自主修改示例数据、措辞、格式
│   ✗ AI基于单次对话经验更新内容
│   ✗ AI为了"更准确"而调整规则表述
│   ✗ 任何形式的"优化"、"改进"、"完善"
├── 必须流程:
│   → 修改前必须获得Human显式批准
│   → 修改时必须说明具体变更原因（引用哪条缺陷）
│   → 修改后必须记录变更日志（WHAT + WHY + WHEN）
│   → 建议保留版本历史（git tag）

LEVEL 2: CONFIGURATION (配置级)
├── 属性: 可修改但需审批
├── 范围: deploy/config/*.yaml, .env.example, services.yaml
├── 修改门槛: 配置值更新、新增服务定义
├── 流程: AI可提议修改，但需Human确认后执行

LEVEL 3: WORKING (工作级)
├── 属性: 正常读写
├── 范围: 源代码、脚本、文档（非核心）
├── 规则: 遵循P3最小化原则
```

### SKILL.md 宪法条款 (CONSTITUTIONAL CLAUSE)

```
╔═══════════════════════════════════════════════════════════════╗
║                                                             ║
║   ⚖️ CONSTITUTIONAL PROTECTION NOTICE                        ║
║                                                             ║
║   This file (SKILL.md) is a CONSTITUTIONAL document.        ║
║                                                             ║
║   READ-ONLY STATUS:                                         ║
║   - This file defines the immutable rules of engagement     ║
║   - AI agents MUST execute according to these rules         ║
║   - AI agents MUST NOT modify this file autonomously        ║
║                                                             ║
║   PERMITTED MODIFICATIONS (ALL require Human approval):     ║
║   ① Proven critical defect causing operational errors       ║
║   ② Fundamental architecture change (Human decision)        ║
║   ③ Explicit Human instruction to modify                   ║
║                                                             ║
║   FORBIDDEN MODIFICATIONS (AI self-initiated):             ║
║   ✗ Updating example data based on single conversation     ║
║   ✗ "Improving" wording or formatting                     ║
║   ✗ Adding new protocols without Human request              ║
║   ✗ Removing or weakening any principle                    ║
║   ✗ Any change justified by "making it more accurate"      ║
║                                                             ║
║   VIOLATION CONSEQUENCE:                                    ║
║   - Unauthorized modification = PRINCIPLE VIOLATION        ║
║   - Must be reported to Human immediately                  ║
║   - Revert to last approved version                         ║
║                                                             ║
║   LAST APPROVED VERSION: [To be filled by Human]           ║
║   APPROVAL AUTHORITY: Project Owner (Human)                 ║
║                                                             ║
╚═══════════════════════════════════════════════════════════════╝
```

### AI BEHAVIORAL RULES FOR PROTECTED FILES

**当AI需要引用SKILL.md中的数据时：**

| 场景 | 允许行为 | 禁止行为 |
|------|---------|---------|
| 发现SKILL.md中的示例数据与实际不符 | 在对话中指出差异，建议人工审核 | 自主修改SKILL.md以匹配实际 |
| 发现SKILL.md中的命令有误 | 报告给Human，等待指示 | 直接修正命令 |
| 认为某个原则需要增强 | 提议增强方案，请求Human审批 | 直接添加新内容 |
| 需要记录验证结果 | 写入session memory或单独的审计日志 | 修改SKILL.md案例研究部分 |

**正确的做法示例：**

```
❌ WRONG:
  "我发现SKILL.md中bx-user的Repository数量写的是8，
   但实际是8个，让我更新一下..."
  
✅ RIGHT:
  "根据FIA协议验证，bx-user模块确实有8个JPA Repository接口。
   SKILL.md中的示例数据与实际情况一致（如果之前不一致，
   我会报告给您，请您决定是否需要更新)。"
```

---

## 📋 CHANGE REQUEST PROCESS (变更请求流程)

### 当需要修改LEVEL 1文件时：

```
STEP 1: AI提交变更请求
  ┌─────────────────────────────────────────────┐
  │ CHANGE REQUEST FORM                          │
  │                                             │
  │ File: .trae/skills/.../SKILL.md            │
  │ Section: [具体章节，如H14部分]               │
  │ Current Content: [当前文本]                 │
  │ Proposed Change: [拟修改为]                 │
  │ Reason: [为什么需要修改]                    │
  │ Defect Category:                            │
  │   □ Critical error causing wrong operation  │
  │   □ Security vulnerability                 │
  │   □ Architecture fundamental change         │
  │   □ Explicit Human instruction             │
  │ Evidence: [支持修改的证据链]                │
  └─────────────────────────────────────────────┘

STEP 2: Human Review & Decision
  → Human reviews change request
  → Human decides: APPROVE / REJECT / REQUEST MORE INFO
  
STEP 3: If APPROVED → Execute with Change Log
  → Make the EXACT approved change only
  → Append to CHANGE_LOG section at end of file
  
STEP 4: If REJECTED → No modification
  → AI continues operating with current version
  → May note discrepancy in session memory (not in skill file)
```

---

## 🚫 ANTI-PATTERN: WHAT WENT WRONG (教训记录)

### Incident #001 (2026-05-14)
**问题**: AI在未获授权的情况下多次修改SKILL.md
**症状**: 
- 为了"修复已验证的错误"而修改案例研究部分的示例数据
- 将报告声称值改为验证后的真实值
- 添加了新的"FIA CASE STUDY"章节
- 更新了Self-Audit Checklist

**根因分析**:
1. 缺乏明确的文件保护机制
2. AI将"修复错误"理解为"修改技能文件"
3. 没有区分"执行时的临时发现"与"永久性规则更新"

**纠正措施**:
1. ✅ 建立本project_rules.md文件保护机制
2. ✅ 明确SKILL.md为CONSTITUTIONAL级别
3. ✅ 所有修改必须经过Human审批
4. ✅ 验证结果应记录在其他位置（审计日志），而非修改源文件

**状态**: 已纠正 ✅

---

## 📊 GOVERNANCE METRICS

| Metric | Target | Current |
|--------|--------|---------|
| SKILL.md unauthorized modifications | 0 | 0 (post-fix) |
| Time since last authorized change | N/A | Establishing baseline |
| Change requests submitted | Track | 0 |
| Change requests approved | Track | 0 |
| Rule compliance rate | 100% | Monitoring |

---

## 🔄 VERSION CONTROL REQUIREMENTS

对于LEVEL 1保护文件：

```bash
# 强制使用Git进行版本控制
git add .trae/skills/group-debug-deploy-expert/SKILL.md
git commit -m "docs(skill): [APPROVED CHANGE] Brief description"

# 每次批准修改后打Tag
git tag -a "skill-vX.Y.Z" -m "Version X.Y.Z: Description of changes"
```

---

*This file itself is LEVEL 1 PROTECTED. Modifications require Human approval.*
*Last Updated: 2026-05-14*
*Approval Authority: Project Owner (Human)*
