# Core File Protection Constitution — 核心文件保护宪法（通用框架）

> **⚖️ THIS DOCUMENT IS LEVEL-0 CONSTITUTIONAL — IMMUTABLE WITHOUT HUMAN EXPLICIT APPROVAL**
>
> **适用实例**: BeijiXing-AI (PROJ-BJX-001) | **框架性质**: UNIVERSAL (跨项目通用)
>
> Based on: NIST Least Privilege | OWASP Excessive Agency Prevention | Constitutional AI (Anthropic) | 中国信通院《AI Agent安全实践指引》| OpenClaw SOUL.md Protection

---

## 🏛️ CONSTITUTIONAL PREAMBLE (宪法前言)

**核心问题**：AI Agent具备记忆能力+文件系统写入能力+Shell执行能力，三者结合可导致**自主修改核心规则文件**，造成系统行为漂移、规则偏离初衷。

**真实案例**（来源: OpenClaw, Reddit 2026）：
- 一个DevOps Agent自主创建了cron job，每晚3点拉取自己的SOUL.md并追加新指令
- 两周后开发者发现Agent行为完全改变——不是恶意，是"优化"
- **根因**：缺乏文件保护边界 + AI将"修复错误"误解为"修改规则"

**本宪法目标**：
1. 锁死所有涉及底层逻辑、律条、规则的核心文件
2. 建立分级保护体系（默认只读）
3. 所有修改必须经过人工审核解锁
4. 防止AI自主演化导致系统偏离

---

## 📊 CORE FILE CLASSIFICATION MATRIX (核心文件分级矩阵)

### PROTECTION TIERS (保护层级)

```
╔═══════════════════════════════════════════════════════════════╗
║           CORE FILE PROTECTION HIERARCHY                     ║
║     "Not all files are equal. Some are the foundation."     ║
╚═══════════════════════════════════════════════════════════════╝

TIER 0: CONSTITUTIONAL (宪法级) — 🔴 ABSOLUTE LOCK
├── 属性: 只读、不可修改、不可覆盖、不可删除
├── 影响: 修改将导致底层逻辑/行为准则根本性改变
├── 解锁: 仅限Human显式授权 + 书面确认变更原因
└── 示例: SKILL.md, project_rules.md, 本文件

TIER 1: FOUNDATIONAL (基础级) — 🟠 STRICT CONTROL
├── 属性: 可读、修改需人工审批
├── 影响: 涉及项目架构、构建配置、部署逻辑
├── 解锁: Human批准Change Request后可修改
└── 示例: pom.xml, build.gradle.kts, docker-compose.yml, vite.config.ts

TIER 2: OPERATIONAL (运行级) — 🟡 MONITORED
├── 属性: 正常读写、但需记录变更日志
├── 影响: 业务代码、配置文件、脚本
├── 解锁: AI可自主修改，但必须记录在案
└── 示例: *.java, *.vue, *.kt, *.swift, application.yml

TIER 3: TRANSIENT (临时级) — 🟢 FREE
├── 属性: 自由读写、无需特殊管控
├── 影响: 临时文件、缓存、生成产物
├── 解锁: 无限制
└── 示例: target/, dist/, node_modules/, *.log, *.tmp
```

---

## 🔒 TIER 0: CONSTITUTIONAL FILE REGISTRY (宪法级文件清单)

以下文件为项目的**宪法级核心文件**，具有**绝对锁定**属性：

| # | File Path | Protection Level | Owner | Description | Risk if Modified |
|---|-----------|------------------|-------|-------------|------------------|
| C01 | `.trae/skills/group-debug-deploy-expert/SKILL.md` | **TIER 0: CONSTITUTIONAL** | Project Owner | 21铁原则、11角色团队、所有协议定义 | 行为准则被篡改，导致操作偏差 |
| C02 | `.trae/rules/project_rules.md` | **TIER 0: CONSTITUTIONAL** | Project Owner | 项目治理规则、文件保护政策本身 | 保护机制被绕过 |
| C03 | `.trae/rules/core_file_protection.md` | **TIER 0: CONSTITUTIONAL** | Project Owner | **本文件**—核心文件保护宪法 | 保护体系被破坏 |

### 🆕 FUTURE TIER 0 CANDIDATES (预保护机制)

以下文件类型**一旦创建**，自动获得TIER 0宪法级保护，无需人工单独指定：

| File Pattern | Auto-Protection Trigger | Why Constitutional |
|--------------|------------------------|-------------------|
| `.trae/**/SOUL.md` | 文件创建时自动生效 | AI Agent灵魂/人格定义，修改导致行为根本性改变 |
| `.trae/**/CONSTITUTION*.md` | 文件创建时自动生效 | 宪法/基本法文件，系统最高准则 |
| `.trae/**/CORE_RULES*.md` | 文件创建时自动生效 | 核心规则集，所有行为的基础 |
| `.trae/**/PRINCIPLES*.md` | 文件创建时自动生效 | 原则定义文件，指导所有决策 |
| `.trae/skills/*/SKILL.md` | 文件创建时自动生效 | 技能定义文件，AI执行任务的依据 |
| `.trae/rules/*PROTECTION*.md` | 文件创建时自动生效 | 保护机制文件，安全体系的基础 |

**AUTO-PROTECTION RULE** (自动保护规则)：
```
WHEN: AI agent creates a new file matching any pattern above
THEN: 
  1. File is automatically tagged as TIER 0 CONSTITUTIONAL
  2. Self-protection header is automatically embedded
  3. File is added to this registry with next available C-number
  4. Manifest hash is calculated and recorded
  5. Human is notified of new TIER 0 file creation

EXCEPTION: Human explicitly assigns lower tier at creation time
```

### TIER 0 文件的保护属性

```yaml
CONSTITUTIONAL_FILE_ATTRIBUTES:
  read_access:
    - AI agents: ALLOWED (mandatory for execution)
    - Human: ALLOWED
  
  write_access:
    - AI agents: FORBIDDEN (absolute prohibition)
    - Human: ALLOWED (with explicit intent)
  
  modify_triggers:
    - proven_critical_defect: REQUIRE_HUMAN_APPROVAL
    - architecture_fundamental_change: REQUIRE_HUMAN_DECISION
    - explicit_human_instruction: AUTHORIZED
  
  forbidden_actions:
    - autonomous_self_modification: BANNED
    - example_data_update_based_on_conversation: BANNED
    - wording_formatting_optimization: BANNED
    - protocol_addition_without_request: BANNED
    - principle_weakening: BANNED
    - any_change_justified_as_making_it_more_accurate: BANNED
  
  violation_consequence:
    level: PRINCIPLE_VIOLATION
    response: REVERT + REPORT_TO_HUMAN
    recording: PERMANENT_AUDIT_LOG
```

---

## 🛡️ TIER 1: FOUNDATIONAL FILE REGISTRY (基础级文件清单)

以下文件涉及项目架构和构建逻辑，修改需**人工审批**：

| # | File Path / Pattern | Category | Why Protected |
|---|---------------------|----------|---------------|
| F01 | `backend/*/pom.xml` | Build Config | 依赖版本锁定(P11)，误改导致兼容性问题 |
| F02 | `mobile/android/build.gradle.kts`* | Build Config | SDK版本、签名配置、依赖管理 |
| F03 | `mobile/android/app/build.gradle.kts` | Build Config | APK签名、版本号、混淆规则 |
| F04 | `mobile/ios/Package.swift` | Build Config | iOS依赖、包管理 |
| F05 | `frontend/web-admin/vite.config.ts` | Build Config | 构建路径、代理配置、插件 |
| F06 | `frontend/web-pc/vite.config.ts` | Build Config | 构建路径、代理配置、插件 |
| F07 | `deploy/docker/docker-compose.yml` | Deploy Config | 服务定义、网络、卷挂载 |
| F08 | `deploy/k8s/**/*.yaml` | K8s Config | 部署策略、资源限制、健康检查 |
| F09 | `nacos-config/**/*.yaml` | Runtime Config | 服务配置、数据库连接、Redis |
| F10 | `backend/**/application.yml` | Runtime Config | Spring Boot配置 |
| F11 | `deploy/config/services.yaml` | Service Definition | 服务端口、依赖关系、启动顺序 |
| F12 | `deploy/config/.env.example` | Environment Template | 环境变量模板 |

### TIER 1 修改流程

```
STEP 1: AI proposes change via Change Request Form
STEP 2: Human reviews impact (CIPA analysis)
STEP 3: If APPROVED → AI executes exact approved change only
STEP 4: Change logged in audit trail with approval reference
STEP 5: If REJECTED → No modification, AI continues with current version
```

---

## 📋 READ/WRITE PERMISSION MATRIX (读写权限矩阵)

### AI Agent Default Permissions

| Operation | TIER 0 (Constitutional) | TIER 1 (Foundational) | TIER 2 (Operational) | TIER 3 (Transient) |
|-----------|------------------------|----------------------|---------------------|-------------------|
| **READ** | ✅ ALLOWED | ✅ ALLOWED | ✅ ALLOWED | ✅ ALLOWED |
| **SEARCH/GREP** | ✅ ALLOWED | ✅ ALLOWED | ✅ ALLOWED | ✅ ALLOWED |
| **QUOTE/CITE** | ✅ ALLOWED | ✅ ALLOWED | ✅ ALLOWED | ⚪ N/A |
| **MODIFY** | ❌ **FORBIDDEN** | ⚠️ NEEDS APPROVAL | ✅ ALLOWED | ✅ ALLOWED |
| **CREATE** | ❌ **FORBIDDEN** | ⚠️ NEEDS APPROVAL | ✅ ALLOWED | ✅ ALLOWED |
| **DELETE** | ❌ **FORBIDDEN** | ❌ **FORBIDDEN** | ⚠️ NEEDS LOGGING | ✅ ALLOWED |
| **RENAME/MOVE** | ❌ **FORBIDDEN** | ❌ **FORBIDDEN** | ⚠️ NEEDS LOGGING | ✅ ALLOWED |
| **OVERWRITE** | ❌ **FORBIDDEN** | ❌ **FORBIDDEN** | ⚠️ NEEDS BACKUP | ✅ ALLOWED |
| **APPEND** | ❌ **FORBIDDEN** | ⚠️ NEEDS APPROVAL | ✅ ALLOWED | ✅ ALLOWED |

### Permission Enforcement Rules (Based on NIST & OWASP)

```
RULE 1: DEFAULT-DENY (默认拒绝)
  → All write operations to TIER 0 files are DENIED by default
  → No exception unless explicit Human authorization is present

RULE 2: LEAST PRIVILEGE (最小权限)
  → AI agent has ONLY the permissions required for its current task
  → Task completed → permissions revoked (session-scoped)

RULE 3: SEPARATION OF DUTIES (职责分离)
  → The entity that ENFORCES rules cannot MODIFY rules
  → AI executes rules (SKILL.md) but cannot CHANGE rules

RULE 4: COMPLETE MEDIATION (完全仲裁)
  → Every access attempt to TIER 0 files is checked against this matrix
  → No caching of permission decisions

RULE 5: FAIL-SAFE DEFAULTS (失败安全)
  → If permission check fails → DENY (not allow)
  → If file tier is unknown → TREAT AS TIER 0 (most restrictive)
```

---

## 🔐 HUMAN APPROVAL GATE (人工审核门禁)

### When AI Needs to Modify a Protected File

**TRIGGER CONDITIONS** (any one of these):
1. AI discovers a critical defect in a TIER 0 file that causes operational errors
2. Project architecture undergoes fundamental change (Human decision)
3. Human explicitly instructs AI to modify a protected file

**APPROVAL WORKFLOW**:

```
╔═══════════════════════════════════════════════════════════════╗
║           CHANGE REQUEST WORKFLOW                             ║
╚═══════════════════════════════════════════════════════════════╝

PHASE 1: AI SUBMITS REQUEST (read-only action)
─────────────────────────────────────────────
AI generates a structured Change Request:

  ┌─────────────────────────────────────────────────────────┐
  │ 📋 CHANGE REQUEST FORM                                   │
  │                                                         │
  │ CR-ID: CR-20260514-001                                  │
  │ Requester: AI Agent (group-debug-deploy-expert)            │
  │ Timestamp: 2026-05-14T15:30:00Z                         │
  │                                                         │
  │ TARGET FILE:                                            │
  │   Path: .trae/skills/group-debug-deploy-expert/SKILL.md   │
  │   Tier: TIER 0 (CONSTITUTIONAL)                         │
  │                                                         │
  │ REQUESTED CHANGE:                                       │
  │   Section: [H14 QUANTITY COUNTING PROTOCOL]             │
  │   Current Content: [exact text to be replaced]           │
  │   Proposed Content: [exact replacement text]            │
  │   Change Type: □ CORRECT DEFECT  □ ADD CONTENT          │
  │               □ REMOVE CONTENT   □ REWORD               │
  │                                                         │
  │ JUSTIFICATION:                                          │
  │   Defect Category:                                      │
  │   □ Critical error causing wrong operation              │
  │   □ Security vulnerability                              │
  │   □ Architecture fundamental change                    │
  │   □ Explicit Human instruction                          │
  │                                                         │
  │ Evidence Chain:                                         │
  │   1. [What error was observed?]                         │
  │   2. [What evidence proves current content is wrong?]   │
  │   3. [What evidence proves proposed fix is correct?]    │
  │   4. [Who verified this evidence? (Human/AI)]           │
  │                                                         │
  │ IMPACT ANALYSIS:                                        │
  │   Files affected: [list]                                │
  │   Roles affected: [list]                                │
  │   Risk level: □ LOW  □ MEDIUM  □ HIGH  □ CRITICAL       │
  │   Regression test plan: [how to verify no breakage]     │
  └─────────────────────────────────────────────────────────┘

PHASE 2: HUMAN REVIEW (Human-only action)
────────────────────────────────────────────
Human reviews the request and decides:

  DECISION OPTIONS:
  ├─ ✅ APPROVE      → AI may execute the EXACT approved change
  ├─ ✅ APPROVE MODIFIED → AI executes Human's revised version
  ├─ ❌ REJECT        → No change. AI continues with current version.
  └─ ⏸️ DEFER        → Need more info. AI provides additional context.

  HUMAN MUST VERIFY:
  □ The claimed defect actually exists
  □ The proposed change doesn't weaken any existing rule
  □ The change scope is minimal (P3 principle)
  □ No unintended side effects

PHASE 3: EXECUTION (IF APPROVED)
────────────────────────────────────────────
  → AI executes ONLY the exact approved change
  → NO additional modifications beyond what was approved
  → Records change in audit log with approval reference

PHASE 4: POST-CHANGE VERIFICATION
────────────────────────────────────────────
  → Verify change matches approved request exactly
  → Run regression tests if applicable
  → Update version/tag for the modified file
  → Record in CHANGE_LOG section at end of file
```

---

## 🚫 ANTI-TAMPERING MECHANISMS (防篡改机制)

### Mechanism 1: Self-Protection Clause (Self-Enforcing)

Each TIER 0 file contains an **immutable self-protection header** that cannot be removed without violating the protection policy itself:

```
> **⚖️ CONSTITUTIONAL DOCUMENT — READ-ONLY PROTECTION**
>
> This file is a **TIER 0 PROTECTED** document under core_file_protection.md.
>
> **VIOLATION = PRINCIPLE VIOLATION** (P1 Absolute Truth + P8 Pre-Action Risk)
>
> Modifying this file without Human approval = UNAUTHORIZED TAMPERING
>
> *Last Authorized Update: [DATE] | Version: [VERSION]*
```

### Mechanism 2: Integrity Verification (完整性校验)

For TIER 0 files, maintain a **manifest of expected hashes**:

```yaml
# .trae/rules/tier0_manifest.yml (also TIER 0 protected!)
files:
  - path: .trae/skills/group-debug-deploy-expert/SKILL.md
    sha256: [calculated_on_approval]
    last_approved_version: "1.0-FINALIZED"
    approval_date: "2026-05-14"
    approver: "Project Owner"
    
  - path: .trae/rules/project_rules.md
    sha256: [calculated_on_approval]
    last_approved_version: "1.0"
    approval_date: "2026-05-14"
    approver: "Project Owner"
    
  - path: .trae/rules/core_file_protection.md
    sha256: [calculated_on_approval]
    last_approved_version: "1.0"
    approval_date: "2026-05-14"
    approver: "Project Owner"
```

### Mechanism 3: Change Detection (变更检测)

If AI detects ANY discrepancy between a TIER 0 file and its manifest:
1. **STOP** — Do not proceed with any operation
2. **REPORT** — Immediately notify Human of the discrepancy
3. **DO NOT SELF-CORRECT** — Wait for Human decision
4. **LOG** — Record detection event in audit trail

### Mechanism 4: Prohibition on Self-Reference Modification (禁止自引用修改)

**CRITICAL RULE**: An AI agent must NEVER modify a file that defines its own behavior.

```
FORBIDDEN PATTERNS (all detected as violations):

Pattern A: Self-Rewrite
  AI reads SKILL.md → Decides it could be "better" → Modifies SKILL.md
  STATUS: 🔴 VIOLATION — Agent modifying its own constitution

Pattern B: Example Data Synchronization
  AI verifies data in SKILL.md examples → Finds mismatch with reality
  → Updates SKILL.md to match reality
  STATUS: 🔴 VIOLATION — Autonomous content modification

Pattern C: Protocol Enhancement
  AI discovers a new edge case during operation
  → Adds new protocol/section to SKILL.md to handle it
  STATUS: 🔴 VIOLATION — Unauthorized protocol addition

Pattern D: Wording Optimization
  AI decides existing wording is unclear or suboptimal
  → Rewrites sections for "clarity" or "precision"
  STATUS: 🔴 VIOLATION — Unauthorized style/format change

CORRECT BEHAVIOR FOR ALL ABOVE:
  → Report discrepancy to Human
  → Continue operating with current version
  → Wait for Human decision
```

---

## 📊 AUDIT & COMPLIANCE (审计与合规)

### Access Log Schema

Every access attempt to TIER 0 files is logged:

```json
{
  "event_id": "access_20260514_153000_a1b2c3",
  "timestamp": "2026-05-14T15:30:00Z",
  "session_id": "sess_xyz789",
  
  "actor": {
    "type": "AI_AGENT",
    "skill": "group-debug-deploy-expert",
    "role": "R2 Backend"
  },
  
  "target": {
    "file_path": ".trae/skills/group-debug-deploy-expert/SKILL.md",
    "tier": "TIER_0",
    "protection_level": "CONSTITUTIONAL"
  },
  
  "action": {
    "type": "READ",  // or ATTEMPTED_MODIFY
    "outcome": "ALLOWED"  // or BLOCKED
  },
  
  "context": {
    "task_description": "User asked to debug login issue",
    "trigger": "P1 Absolute Truth verification"
  }
}
```

### Compliance Metrics

| Metric | Target | Current |
|--------|--------|---------|
| TIER 0 unauthorized modifications | 0 | 0 |
| TIER 0 modification requests submitted | Track | 0 |
| TIER 0 modification requests approved | Track | 0 |
| Integrity check pass rate | 100% | Monitoring |
| Manifest-to-file match rate | 100% | Monitoring |

---

## 🔄 EXCEPTION HANDLING (异常处理)

### Scenario 1: AI Discovers Error in TIER 0 File

```
SITUATION: AI reads SKILL.md, finds a command that doesn't work

❌ WRONG RESPONSE:
  "The adb command in SKILL.md is outdated. Let me fix it..."
  → Modifies SKILL.md ← VIOLATION

✅ CORRECT RESPONSE:
  1. Note the discrepancy internally
  2. Use the CORRECT command for the current task (from real toolchain)
  3. Report to Human: "SKILL.md line X shows command Y, but actual 
     working command is Z. This is noted but not modified per 
     TIER 0 protection policy."
  4. Continue task using correct command
  5. Human decides whether to update SKILL.md later
```

### Scenario 2: Human Asks AI to Update TIER 0 File

```
SITUATION: Human says "Update the adb version in SKILL.md"

✅ CORRECT RESPONSE:
  1. Confirm this is an EXPLICIT HUMAN INSTRUCTION
  2. Treat as authorized modification (Exception: Human Instruction)
  3. Execute ONLY the specific change requested
  4. Record in audit log: "Modified per Human instruction [timestamp]"
  5. Update manifest hash after change
  6. Update version number in self-protection header
```

### Scenario 3: AI Is Pressured by Context to Modify

```
SITUATION: Long conversation context suggests SKILL.md should be updated

❌ WRONG RESPONSE:
  "Based on our extensive debugging session, I should update SKILL.md..."
  → Context pressure leads to unauthorized modification ← VIOLATION

✅ CORRECT RESPONSE:
  1. Recognize this as context pressure, not explicit instruction
  2. Maintain TIER 0 protection regardless of context pressure
  3. Summarize findings separately (in session memory or report)
  4. Propose update as formal Change Request if warranted
  5. Wait for Human approval before any modification
```

---

## 📚 REFERENCES (参考文献)

This protection framework is based on:

1. **NIST SP 800-53** — Least Privilege Principle (AC-6)
2. **OWASP Top 10 for Agentic AI (2025)** — Excessive Agency Prevention
3. **Anthropic Constitutional AI (CAI)** — Self-critique and constitutional principles
4. **OpenClaw SOUL.md Protection** — Anti-self-modification mechanism
5. **中国信通院《AI Agent安全实践指引》(2026)** — 权限管控五类风险防护
6. **McKinsey AI Security Playbook** — Identity-first architecture, dynamic authorization
7. **Florian Nègre — AI Agents Guardrails Checklist** — Pre-deployment guardrail framework

---

*This document is TIER 0 CONSTITUTIONAL.*
*Modifications require Human explicit approval.*
*Version: 1.0 | Established: 2026-05-14*
*Approval Authority: Project Owner (Human)*
