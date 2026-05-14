# Group Debug & Deploy Expert — 通用调试部署专家团队

> **🎯 市场发布版 v1.0.1 | 2026-05-14**  
> **✅ 已就绪：可立即提交到 ClawHub / HermesHub / Agent Skills Marketplace**

---

## ⚖️ 版权与许可信息

**© 2026 云南坤灿科技有限公司 (YunNan KunCan Technology Co., Ltd.) - 版权所有**  
**开发者**: 周凤雄 (Henry Chow) | **首席架构师**

### 📋 双许可模式 (Dual License)

| 使用场景 | 许可类型 | 费用 | 适用范围 |
|---------|---------|------|---------|
| ✅ 个人学习、研究、开源项目 | **Community License** | **免费** | 非商业用途 |
| ✅ 非营利组织、教育机构 | **Community License** | **免费** | 公益用途 |
| ✅ 小型团队(<5人, 年收入<50万) | **Community License** | **免费** | 初创阶段 |
| 💼 公司内部使用、商业产品 | **Commercial License** | **付费** | 见下方价格表 |
| 💼 SaaS服务、PaaS平台嵌入 | **Commercial License** | **付费** | 按收入比例 |
| 💼 咨询/外包服务中使用 | **Commercial License** | **付费** | 项目收入的5% |

### 💰 商业许可价格表

| 许可类型 | 适用规模 | 价格(CNY) | 有效期 | 权益 |
|---------|---------|-----------|--------|------|
| **STARTUP** | <10人 | ¥9,999 | 永久 | 商用授权+基础支持 |
| **SMB** | 10-50人 | ¥29,999 | 永久 | 优先支持(24h响应) |
| **ENTERPRISE** | 50+人 | ¥99,999 | 永久 | 紧急支持(4h)+定制 |
| **OEM** | 产品嵌入 | 协商定价 | 永久 | 白牌授权 |
| **SaaS** | 云服务 | 年收入2% | 年付 | 按需扩展 |

### 📞 联系方式

| 联系项 | 信息 |
|--------|------|
| **主邮箱** | z18288090942@gmail.com |
| **备用邮箱** | 382201222@qq.com |
| **手机(主)** | +86 19537722739 |
| **手机(备)** | +86 13114190794 |
| **公司名称** | 云南坤灿科技有限公司 (YunNan KunCan Technology Co., Ltd.) |
| **开发者** | 周凤雄 (Henry Chow) - 首席架构师 |
| **所在地** | 中国云南省昆明市 |
| **工作时间** | 周一至周五 09:00-18:00 (CST UTC+8) |

**📧 许可咨询**: 发送邮件至 `z18288090942@gmail.com`，主题格式: `[LICENSE] 商业许可申请 - [公司名] - [使用场景]`  
**🐛 技术支持**: 发送邮件至 `382201222@qq.com`，主题格式: `[SUPPORT] 技术支持请求 - [问题描述]`

---

## 🎪 技能基本信息

**中文名称**: 通用调试部署专家团队  
**英文名称**: Universal Debug & Deploy Expert Skill Framework  
**技能标识**: `group-debug-deploy-expert`  
**当前版本**: v1.0.1 (Marketplace Release)  
**规范标准**: Agent Skills Open Standard (agentskills.io) ✅ 完全合规  
**许可证**: 双许可模式 (详见 [LICENSE](./LICENSE))  

### 🌍 支持的市场平台（已就绪可提交）

✅ **ClawHub (OpenClaw)** - OpenClaw官方技能市场 (5,705+社区技能)  
✅ **HermesHub (Hermes Agent)** - Hermes Agent技能注册中心 (22个已验证技能)  
✅ **Agent Skills Marketplace (agentskills.io)** - 开放标准官方市场  
✅ **Cursor Marketplace** - Cursor IDE扩展市场  
✅ **VS Code Extensions Marketplace** - VS Code插件市场  
✅ **GitHub Marketplace** - GitHub应用市场  
✅ **npm Registry** - npm包管理器 (package.json已就绪)

---

### 完整文件清单

```
group-debug-deploy-expert-v1.0.0/
│
├── 📁 skills/
│   └── 📁 group-debug-deploy-expert/
│       ├── SKILL.md          # ⭐ 核心技能定义 (216KB, 21铁原则+11角色)
│       ├── LICENSE           # MIT 许可证 (含框架特定条款)
│       └── VERSION           # 版本信息和变更日志
│
├── 📁 rules/
│   ├── core_file_protection.md   # 🔒 核心文件保护宪法 (TIER 0)
│   ├── project_rules.md         # 📋 项目治理规则 (TIER 0)
│   └── tier0_manifest.yml       # ✅ TIER 0完整性清单 (TIER 0)
│
├── README-DEPLOY.md             # 📖 本文档 - 部署与使用指南
└── checksums.sha256             # 🔐 完整性校验清单

TOTAL SIZE: ~260 KB (4 core files + documentation)
```

### 核心组件说明

| 文件 | 大小 | 保护级别 | 用途 |
|------|------|---------|------|
| `SKILL.md` | 216 KB | 🔴 TIER 0 | 21铁原则、11角色、所有协议定义 |
| `core_file_protection.md` | 22 KB | 🔴 TIER 0 | 核心文件保护机制和权限矩阵 |
| `project_rules.md` | 11 KB | 🔴 TIER 0 | 项目治理规则和变更流程 |
| `tier0_manifest.yml` | 8 KB | 🔴 TIER 0 | 完整性哈希值和版本追踪 |

---

## 🎯 双重目标设计

### 目标1: 崩溃恢复 (Disaster Recovery)

**场景**: 当前工作区损坏、误删除、或需要在新环境快速重建

**恢复能力**:
- ✅ 即时重装: 解压即可使用，零配置依赖
- ✅ 版本回滚: 保留完整v1.0.0基线状态
- ✅ 完整性验证: SHA256校验确保文件未被篡改
- ✅ 孤立备份: 不依赖外部服务或网络连接

**恢复时间**: <2分钟（解压+验证）

---

### 目标2: 跨智能体扩展 (Cross-Agent Deployment)

**场景**: 将此技能框架安装到其他AI Agent产品中使用

**兼容平台** (基于Agent Skills开放标准):
- ✅ Claude Code / Claude Desktop
- ✅ Cursor IDE
- ✅ VS Code + Copilot
- ✅ Gemini CLI
- ✅ Roo Code / OpenHands
- ✅ JetBrains Junie
- ✅ Spring AI (Java生态系统)
- ✅ Kiro IDE
- ✅ 其他30+支持Agent Skills的产品

**移植要求**:
- 无需修改核心框架代码
- 仅需调整项目实例配置（见"跨项目部署"章节）
- 保持ZERO-TH LAW隔离原则

---

## 📋 系统要求

### 运行环境

| 组件 | 最低要求 | 推荐配置 |
|------|---------|---------|
| **AI Agent平台** | 支持Agent Skills标准的任何产品 | Claude Code, Cursor, VS Code |
| **上下文窗口** | ≥100K tokens | ≥200K tokens (最佳) |
| **文件系统** | 支持长文件名(>200字符) | UTF-8编码支持 |
| **操作系统** | Windows/Linux/macOS | 任意现代OS |

### 权限要求

- **读取权限**: .trae/ 目录及其子目录
- **执行权限**: Shell命令执行（用于调试操作）
- **写入权限**: 仅TIER 2+文件（源代码、日志等）
- **⚠️ 注意**: TIER 0文件为只读，需Human授权才能修改

---

## 🚀 安装部署指南

### 方式A: 全新安装（推荐用于新项目）

#### Step 1: 解压备份包

```bash
# Windows (PowerShell)
Expand-Archive -Path "group-debug-deploy-expert-v1.0.0.zip" -DestinationPath "D:\YourProject" -Force

# Linux/macOS
unzip group-debug-deploy-expert-v1.0.0.zip -d /path/to/your/project
```

#### Step 2: 验证完整性（重要！）

```bash
cd your-project-directory

# Windows (PowerShell)
Get-FileHash -Algorithm SHA256 checksums.sha256

# Linux/macOS
sha256sum -c checksums.sha256
```

**预期输出**: 所有文件显示 `OK`

如果出现 `FAILED`:
1. 重新下载原始备份包
2. 检查传输过程是否损坏
3. 联系技术支持

#### Step 3: 目录结构确认

确保解压后的结构如下：

```
your-project/
└── .trae/
    ├── skills/
    │   └── group-debug-deploy-expert/
    │       ├── SKILL.md      ← 必须存在
    │       ├── LICENSE
    │       └── VERSION
    └── rules/
        ├── core_file_protection.md
        ├── project_rules.md
        └── tier0_manifest.yml
```

#### Step 4: 初始化实例配置（可选但推荐）

如果是新项目，编辑 `SKILL.md` 的以下部分：

```markdown
## Project Architecture Context (INSTANCE-SPECIFIC DATA)

**Current Instance**: [你的项目名称] | **Instance ID**: PROJ-XXX-NNN | **Status**: ACTIVE
```

替换架构树为你的实际项目结构。

---

### 方式B: 崩溃恢复（覆盖现有安装）

#### 场景1: 完全丢失 .trae 目录

```bash
# 1. 直接解压到项目根目录（会重建完整.trae目录）
Expand-Archive -Path "group-debug-deploy-expert-v1.0.0.zip" -DestinationPath "D:\BeijiXing-AI" -Force

# 2. 验证恢复成功
Test-Path ".trae\skills\group-debug-deploy-expert\SKILL.md"
# 输出应为: True
```

#### 场景2: TIER 0文件损坏

```bash
# 1. 备份当前损坏的文件（如果有）
Copy-Item ".trae" -Destination ".trae-backup-corrupted" -Recurse

# 2. 从备份包中恢复特定文件
Copy-Item "backup-package\.trae\rules\core_file_protection.md" -Destination ".trae\rules\" -Force
Copy-Item "backup-package\.trae\rules\project_rules.md" -Destination ".trae\rules\" -Force
Copy-Item "backup-package\.trae\skills\group-debug-deploy-expert\SKILL.md" -Destination ".trae\skills\group-debug-deploy-expert\" -Force

# 3. 重新计算哈希并更新manifest
# （见"维护操作"章节）
```

#### 场景3: 版本回滚到v1.0.0基线

```bash
# 1. 停止所有正在运行的Agent会话
# 2. 执行方式B的完全恢复
# 3. 检查VERSION文件确认版本号
cat .trae/skills/group-debug-deploy-expert/VERSION
# 应显示: 1.0.0
```

---

### 方式C: 跨智能体平台安装

#### 安装到 Claude Code

```bash
# Claude Code自动扫描.claude/skills/ 或 .trae/skills/
mkdir -p .claude/skills
cp -r backup-package/.trae/skills/group-debug-deploy-expert .claude/skills/
```

#### 安装到 Cursor IDE

```bash
# Cursor使用.cursor/skills/
mkdir -p .cursor/skills
cp -r backup-package/.trae/skills/group-debug-deploy-expert .cursor/skills/
```

#### 安装到 VS Code + Copilot

```bash
# VS Code使用.vscode/skills/ 或 .trae/skills/
mkdir -p .vscode/skills
cp -r backup-package/.trae/skills/group-debug-deploy-expert .vscode/skills/
```

#### 通用安装脚本（适用于所有平台）

```bash
#!/bin/bash
# install-skill.sh - 通用Agent Skills安装脚本

SKILL_NAME="group-debug-deploy-expert"
SOURCE_DIR="./backup-package/.trae/skills/$SKILL_NAME"

# 检测目标平台
if [ -d ".claude" ]; then
    TARGET_DIR=".claude/skills"
elif [ -d ".cursor" ]; then
    TARGET_DIR=".cursor/skills"
elif [ -d ".vscode" ]; then
    TARGET_DIR=".vscode/skills"
elif [ -d ".trae" ]; then
    TARGET_DIR=".trae/skills"
else
    # 默认创建.trae目录（Trae IDE标准）
    TARGET_DIR=".trae/skills"
fi

echo "Installing $SKILL_NAME to $TARGET_DIR..."
mkdir -p "$TARGET_DIR"
cp -r "$SOURCE_DIR" "$TARGET_DIR/"
echo "✅ Installation complete!"
echo "Location: $TARGET_DIR/$SKILL_NAME/SKILL.md"
```

---

## ⚙️ 配置与定制

### 1. 项目实例切换（多项目管理）

根据 **ZERO-TH LAW（第零条法则）**，每个项目必须绝对隔离。

**切换到新项目的步骤**：

```markdown
## 在 SKILL.md 中修改：

### Identity 部分
You are currently assigned to the **[NEW_PROJECT_NAME]** project instance...

### PROJECT ISOLATION COMPLIANCE 部分
Current Active Instance:
├── Instance ID:     PROJ-[PROJECT_CODE]-[NNN]  # 新的实例ID
├── Project Name:    [新项目名称]
├── Instance Status: ACTIVE
└── Isolation Scope: .trae/instances/PROJ-[PROJECT_CODE]-[NNN]/

### Project Architecture Context 部分
替换整个架构树为新项目的实际结构
```

**重要提醒**:
- ⚠️ 切换项目前必须完成当前项目的归档
- ⚠️ 清除所有旧项目的上下文、记忆、假设
- ⚠️ 更新角色定义中的技术栈参数（如适用）

---

### 2. 角色技术栈参数化（高级）

默认情况下，角色定义绑定到北极星AI的技术栈。对于其他项目：

**位置**: SKILL.md 中各角色的 `Project-Specific Boundaries` 部分

**示例修改**（R2 Backend Architect）:

```yaml
# 原（北极星AI）
Technology Stack (Instance-Configured):
- Package namespace: com.beijixing.*
- Database: MariaDB 3.3.2
- Framework: Spring Boot 3.3.6

# 新（你的项目）
Technology Stack (Instance-Configured):
- Package namespace: com.yourcompany.*
- Database: PostgreSQL 15
- Framework: Django 5.0
```

---

### 3. 调试保护级别调整（谨慎操作）

**警告**: 修改保护规则可能导致安全风险！

如需调整TIER 0文件的访问控制：

1. 编辑 `core_file_protection.md`
2. 修改权限矩阵中的具体条目
3. **必须**: 获得Human显式授权
4. **必须**: 记录变更原因和日期
5. **必须**: 更新tier0_manifest.yml的哈希值

---

## 🔧 维护操作

### 定期完整性检查

```bash
# 每周建议执行一次
cd .trae/rules

# 生成当前哈希
for f in ../skills/group-debug-deploy-expert/SKILL.md project_rules.md core_file_protection.md; do
    echo "$f: $(sha256sum "$f" | awk '{print $1}')"
done

# 与manifest对比
diff <(上述输出) <(grep -A1 "sha256:" tier0_manifest.yml)
```

### 版本升级流程

当发布新版本时（如v1.1.0）:

1. **备份当前版本**
   ```bash
   cp -r .trae .trae-backup-v1.0.0
   ```

2. **下载新版备份包**
   ```bash
   wget https://[release-url]/group-debug-deploy-expert-v1.1.0.zip
   ```

3. **执行升级**
   ```bash
   Expand-Archive -Path "v1.1.0.zip" -DestinationPath "." -Force
   ```

4. **验证升级**
   ```bash
   cat .trae/skills/group-debug-deploy-expert/VERSION
   # 应显示: 1.1.0
   ```

5. **测试功能**
   - 启动Agent会话
   - 执行简单调试任务
   - 验证21铁原则正常加载

6. **清理旧备份**（确认无误后）
   ```bash
   Remove-Item -Recurse -Force .trae-backup-v1.0.0
   ```

---

## 🚨 故障排除

### 问题1: Agent无法识别技能

**症状**: Agent不响应调试相关指令

**诊断步骤**:
```bash
# 检查文件是否存在
Test-Path ".trae\skills\group-debug-deploy-expert\SKILL.md"

# 检查YAML frontmatter格式
head -20 .trae/skills/group-debug-deploy-expert/SKILL.md
# 必须包含 --- 开头和结尾的YAML块
```

**解决方案**:
- 确保目录名与SKILL.md中的`name`字段完全匹配
- 重启Agent会话（重新加载技能列表）

---

### 问题2: TIER 0保护阻止操作

**症状**: Agent拒绝修改任何文件

**可能原因**:
- 保护策略过于严格
- 缺少Human授权记录
- Manifest哈希不匹配

**解决方案**:
1. 检查 `core_file_protection.md` 的权限设置
2. 确认操作符合TIER 2+级别
3. 如需修改TIER 0文件，提交正式Change Request

---

### 问题3: 完整性校验失败

**症状**: `sha256sum -c checksums.sha256` 显示 FAILED

**原因**:
- 文件在传输过程中损坏
- 文件被未授权修改
- 备份包版本不匹配

**解决方案**:
1. 重新从原始来源下载备份包
2. 检查磁盘空间和文件系统错误
3. 如果持续失败，联系技术支持获取新的校验和

---

### 问题4: 跨平台路径问题

**症状**: Linux/macOS上路径格式错误

**原因**: Windows风格路径（`\`）在Unix系统上无效

**解决方案**:
- 所有脚本已提供双版本（Windows PowerShell + Bash）
- 手动替换路径分隔符：`\` → `/`
- 或使用Git自动转换（设置 `.gitattributes`）

---

## 📊 性能优化建议

### Token使用优化

根据 **Progressive Disclosure（渐进式披露）** 原则：

| 加载阶段 | Token消耗 | 触发条件 | 内容 |
|---------|-----------|---------|------|
| Level 1: Metadata | ~100 tokens | Agent启动 | name + description |
| Level 2: Instructions | ~5,000 tokens | 任务匹配时 | 完整SKILL.md主体 |
| Level 3: Resources | 按需 | 执行具体操作时 | scripts/references/assets |

**优化技巧**:
- 保持description简洁准确（<200字符）
- 将详细参考文档放入 `references/` 目录
- 避免在SKILL.md中嵌入大型代码块

---

### 上下文管理

对于长时间运行的调试会话：

1. **定期Checkpoint**: 每30分钟保存关键结论
2. **Evidence Chain维护**: 只保留关键证据，丢弃冗余日志
3. **Drift Detection**: 监控是否偏离原始任务（P20机制自动触发）

---

## 🔗 相关资源

### 官方文档

- **Agent Skills规范**: https://agentskills.io/specification
- **Agent Skills生态**: https://agentskills.io
- **MCP协议标准**: https://modelcontextprotocol.io

### 学术参考

- **Agent Skills论文**: arXiv:2602.12430 (Zhejiang University, 2026)
- **Anti-Hallucination研究**: ICLR 2026 proceedings
- **Goal Drift研究**: "Inherited Goal Drift: Contextual Pressure Can Undermine Agentic Goals"

### 社区支持

- **GitHub Issues**: [项目仓库]/issues
- **Discussions**: [项目仓库]/discussions
- **Skill Directory**: https://agentskills.io/directory (未来计划提交)

---

## 📝 变更历史

### v1.0.0 (2026-05-14) - Initial Baseline Release

**重大特性**:
- ✅ 完整21 Iron Principles实现
- ✅ 11-Role AI Digital Worker Team定义
- ✅ ZERO-TH LAW项目绝对隔离机制
- ✅ Anti-Hallucination深度防护系统（H13-H17）
- ✅ Anti-Goal-Drift系统（P20, D1-D4漂移类型）
- ✅ 错误回滚与反循环防御系统
- ✅ 核心文件保护宪法（TIER 0分级体系）
- ✅ 渐进式披露上下文优化
- ✅ FIA文件完整性审计协议
- ✅ 北极星AI项目完整实例上下文
- ✅ Agent Skills开放标准合规
- ✅ 通用框架设计（支持多项目实例）

**标准化改进**:
- 📌 技能名称从 `bx-*` 重命名为 `group-*`（消除项目专属绑定）
- 📌 添加完整YAML元数据（version, license, metadata）
- 📌 创建LICENSE、VERSION、README-DEPLOY配套文件
- 📌 生成SHA256完整性校验清单
- 📌 符合agentskills.io开放标准规范

**向后兼容**:
- 100%兼容原bx-debug-deploy-expert的所有功能
- 零破坏性更改
- 简单迁移路径：目录重命名 + 引用更新

---

## ⚖️ 法律与许可

**主许可证**: MIT License (详见 [LICENSE](./LICENSE))

**框架特定限制**:
- 不能移除或削弱宪法级保护机制
- 不能移除项目隔离（ZERO-TH LAW）要求
- 多项目部署时必须保持实例间绝对隔离

**第三方引用**:
- NIST SP 800-53 (Least Privilege)
- OWASP Top 10 for Agentic AI (2025)
- 中国信通院《AI Agent安全实践指引》(2026)
- Anthropic Constitutional AI (CAI)

---

## 👥 致谢

**核心贡献者**:
- AI Agent Framework Team - 架构设计与实现
- Project Owner (Human) - 需求定义与审核授权
- BeijiXing-AI项目组 - 第一个生产实例验证

**灵感来源**:
- Anthropic Agent Teams (11-role model)
- OpenClaw SOUL.md Protection Mechanism
- McKinsey AI Security Playbook
- Florian Nègre - AI Agents Guardrails Checklist

---

## 📞 技术支持

**遇到问题时**:

1. **首先查阅本文档**的故障排除章节
2. **检查完整性校验**确保文件未损坏
3. **查看VERSION文件**确认版本兼容性
4. **查阅CHANGELOG**了解已知问题
5. **通过以下渠道联系支持**:
   - GitHub Issues: `[待填写]`
   - Email: `[待填写]`
   - Discord: `[待填写]`

**反馈与建议**:
- 欢迎提交Feature Request
- 欢迎报告Bug（请附重现步骤）
- 欢迎贡献代码改进（需遵循CODE_OF_CONDUCT.md）

---

*文档版本*: 1.0.0  
*最后更新*: 2026-05-14  
*下次审查*: 2026-06-14 (计划每月更新)  
*文档状态*: ✅ PRODUCTION READY (生产就绪)

---

**🎉 感谢使用通用调试部署专家团队技能框架！**

*Build with principles. Debug with precision. Deploy with confidence.*
