# 🌐 全平台Web端发布操作指南
# Web-Based Publishing Guide v1.0

> **5分钟完成4个平台发布** - 浏览器操作，无需命令行

---

## ✅ 已完成：GitHub (100%)

**仓库地址**: https://github.com/xuanji-ai-2026/group-debug-deploy-expert  
**状态**: ✅ 已上线，包含完整营销材料

---

## 📦 平台1: npm Registry (需要2FA修复)

### 问题说明
npm要求**2FA验证**或**Granular Access Token**才能发布包。

### 🔧 解决方案A：启用2FA（推荐，3分钟）

#### 步骤1：启用两步验证
```
📍 打开: https://www.npmjs.com/settings/zfx1818/two-factor-auth
```

1. 点击 **"Enable 2FA"**
2. 选择验证方式：
   - **Authenticator App** (推荐) - 使用Google Authenticator/Microsoft Authenticator
   - 或 **SMS短信验证**

#### 步骤2：重新生成Token
```
📍 打开: https://www.npmjs.com/settings/zfx1818/tokens
```

1. 点击 **"Generate New Token"**
2. 选择类型：**"Automation"** 
3. 设置：
   - Name: `publish-open-group-v2`
   - ✅ 勾选 **"Bypass 2FA for automation"** (关键！)
4. 点击生成，复制新Token

#### 步骤3：提供新Token给我
将新生成的Token发给我，格式：
```
npm_新的token字符串
```

我会立即完成发布！

---

### 🔧 解决方案B：Granular Token（无需2FA）

#### 步骤1：创建Granular Token
```
📍 打开: https://www.npmjs.com/settings/zfx1818/tokens
```

1. 点击 **"Generate New Token"**
2. 选择 **"Granular Access Token"** (不是Classic Token)
3. 配置：
   - Token name: `granular-publish`
   - Expiration: 选择较长周期（如90天或1年）
   - Packages and scopes: **Read and write**
   - ✅ **勾选 "Bypass 2FA for automation"**
4. 复制Token发给我

---

## 🔶 平台2: ClawHub (OpenClaw) - Web方式提交

### 方法：通过ClawHub网站直接提交

#### 步骤1：访问ClawHub
```
📍 打开: https://clawhub.ai
```

#### 步骤2：登录/注册
- 点击右上角 **"Login"** 或 **"Sign Up"**
- 使用 **GitHub账号登录** (xuanji-ai-2026)
- 授权ClawHub访问

#### 步骤3：Publish Skill
1. 登录后，点击 **"Publish"** 按钮（通常在顶部导航或用户菜单）
2. 或访问: `https://clawhub.ai/publish` / `https://clawhub.ai/submit`

#### 步骤4：填写Skill信息

| 字段 | 填写内容 |
|------|---------|
| **Name/Slug** | `group-debug-deploy-expert` |
| **Display Name** | `Group Debug & Deploy Expert (通用调试部署专家团队)` |
| **Version** | `1.0.1` |
| **Description** | `Enterprise-grade AI debug & deploy expert team with 11 roles, 21 iron principles, ZERO-TH LAW isolation, anti-hallucination system. Universal compatibility with Trae/OpenClaw/Hermes/Cursor.` |
| **Category** | `Development Tools` / `DevOps` |
| **Tags** | `debug`, `deploy`, `devops`, `enterprise`, `security`, `multi-agent`, `ai-assistant`, `testing`, `ci-cd`, `production-ready` |
| **Repository URL** | `https://github.com/xuanji-ai-2026/group-debug-deploy-expert` |
| **Homepage** | `https://github.com/xuanji-ai-2026/group-debug-deploy-expert#readme` |
| **License** | `Dual License (See LICENSE file)` |

#### 步骤5：上传SKILL.md文件
- ClawHub会自动从您的GitHub仓库读取 `SKILL.md` 文件
- 或者手动上传 `.trae/skills/group-debug-deploy-expert/SKILL.md`

#### 步骤6：发布
- 点击 **"Publish"** / **"Submit"**
- 等待审核（通常是自动审核，几分钟内上线）

### 预期结果
✅ Skill地址: `https://clawhub.ai/skills/group-debug-deploy-expert`

---

## 🎯 平台3: SkillHub (skillhub.club) - 87K+ Skills

### 方法A：通过CLI（如果Node.js路径问题已解决）

```bash
# 安装CLI（全局）
npm install -g @skill-hub/cli

# 登录（会打开浏览器OAuth）
skillhub login

# 发布当前目录的skill
skillhub publish \
  --tags "debug,deploy,devops,enterprise,security,multi-agent" \
  --github-repo "https://github.com/xuanji-ai-2026/group-debug-deploy-expert"
```

### 方法B：通过网站提交（推荐）

#### 步骤1：访问SkillHub
```
📍 打开: https://www.skillhub.club
```

#### 步骤2：登录/注册
- 点击 **"Sign In"** 或 **"Get Started"**
- 使用 **GitHub OAuth登录** (推荐)
- 或使用Google/邮箱注册

#### 步骤3：进入Publisher Dashboard
- 登录后访问: `https://www.skillhub.club/dashboard` 或 `https://www.skillhub.club/publish`
- 点击 **"Publish New Skill"** / **"+ New Skill"**

#### 步骤4：填写信息

| 字段 | 内容 |
|------|------|
| **Skill Slug** | `group-debug-deploy-expert` |
| **Name** | `Group Debug & Deploy Expert` |
| **Chinese Name** | `通用调试部署专家团队` |
| **Description** | `Universal debug & deploy expert for multi-project instances. 11-role AI team collaboration, 21 iron principles enforcement, ZERO-TH LAW absolute isolation, 12-layer anti-hallucination defense.` |
| **Long Description** | （从README.md复制第一段） |
| **Category** | `Development` > `DevOps` 或 `Debugging` |
| **Tags** | `debug`, `deploy`, `devops`, `enterprise`, `ai-agent`, `multi-agent`, `security`, `testing`, `production-ready`, `21-principles` |
| **Version** | `1.0.1` |
| **License** | `Dual License` |
| **Repository** | `https://github.com/xuanji-ai-2026/group-debug-deploy-expert` |
| **Author** | `周凤雄 (Henry Chow)` |
| **Author Email** | `z18288090942@gmail.com` |

#### 步骤5：关联GitHub仓库
- 在 **"GitHub Repo"** 字段填写:
  ```
  https://github.com/xuanji-ai-2026/group-debug-deploy-expert
  ```
- SkillHub会自动索引您的README和SKILL.md

#### 步骤6：发布
- 点击 **"Publish"** / **"Submit Skill"**
- 等待AI评分（SkillHub会用AI评估Practicality、Clarity、Automation、Quality、Impact）
- 通常几分钟内上线

### 预期结果
✅ Skill地址: `https://www.skillhub.club/skills/<your-username>/group-debug-deploy-expert`

---

## 🟠 平台4: HermesHub (Hermes Agent) - 自动索引

### 好消息：可能无需手动操作！

Hermes Agent支持**自动索引GitHub仓库**。由于您的代码已经在GitHub：

#### 方式1：用户直接安装（已可用）
```bash
# Hermes用户可以直接安装您的skill
hermes skills install xuanji-ai-2026/group-debug-deploy-expert
```

#### 方式2：通过agentskills.io提交（可选增强曝光）

##### 步骤1：访问agentskills
```
📍 打开: https://agentskills.io/submit
```
或
```
📍 打开: https://skills-hub.ai (备用站点)
```

##### 步骤2：登录
- 使用 **GitHub OAuth登录**

##### 步骤3：Submit Skill
填写表单：

| 字段 | 内容 |
|------|------|
| **Slug** | `group-debug-deploy-expert` |
| **Name** | `Group Debug & Deploy Expert` |
| **Summary** | `Enterprise debugging & deployment framework with 11 AI roles and 21 iron principles` |
| **Description** | （粘贴README.md的详细描述） |
| **Tags** | 同上 |
| **GitHub URL** | `https://github.com/xuanji-ai-2026/group-debug-deploy-expert` |
| **License** | Dual License |

##### 步骤4：Publish
- 点击 **"Publish Skill"**

### 额外建议：在Hermes Discord分享
```
📍 加入: https://discord.gg/NousResearch
```

在 `#skills-showcase` 频道发布：
```
🚀 New Skill: Group Debug & Deploy Expert (通用调试部署专家团队)

📦 11-role AI team for enterprise-grade debugging & deployment
🔒 ZERO-TH LAW absolute isolation
🛡️ 21 iron principles + 12-layer anti-hallucination system
🌐 Compatible: Trae, OpenClaw, Hermes, Cursor, VS Code

🔗 GitHub: https://github.com/xuanji-ai-2026/group-debug-deploy-expert
📧 Contact: z18288090942@gmail.com
```

---

## 📋 操作清单（按优先级排序）

### 🔥 今天立即完成（10分钟）

- [ ] **1. npm 2FA** → 启用2FA或获取Granular Token（见上方方案A/B）
- [ ] **2. ClawHub** → 访问 https://clawhub.ai 并Publish（3分钟）
- [ ] **3. SkillHub** → 访问 https://www.skillhub.club 并Publish（3分钟）

### ⏰ 本周内完成

- [ ] **4. agentskills.io** → 提交到跨平台市场（5分钟）
- [ ] **5. Hermes Discord** → 在 #skills-showcase 分享（2分钟）

---

## 💡 提高成功率的技巧

### 通用最佳实践

1. **截图准备**
   - 准备1-2张产品截图或架构图
   - 大部分平台支持上传图片

2. **描述优化**
   - 前50字要吸引眼球（显示在搜索结果中）
   - 包含关键词：debug, deploy, devops, AI, enterprise, multi-agent
   - 中英文双语描述（如果有选项）

3. **Tag策略**
   - 核心标签：`debug`, `deploy`, `devops`, `ai-agent`
   - 差异化标签：`11-roles`, `21-principles`, `zeroth-law`, `anti-hallucination`
   - 行业标签：`enterprise`, `production-ready`, `security`

4. **链接完整性**
   - GitHub仓库（必填）
   - README文档（必填）
   - License文件（强烈建议）
   - 联系方式（建议）

---

## 🆘 遇到问题？

### 各平台帮助链接

| 平台 | 帮助文档 | 支持渠道 |
|------|---------|---------|
| **npm** | https://docs.npmjs.com/publishing-a-package | https://github.com/npm/support |
| **ClawHub** | https://github.com/openclaw/clawhub#readme | Discord: OpenClaw社区 |
| **SkillHub** | https://www.skillhub.club/docs | https://github.com/skillhub-club/cli/issues |
| **Hermes** | https://github.com/NousResearch/hermes-agent/blob/main/CONTRIBUTING.md | Discord: Nous Research |

### 联系我获取帮助

如果在任何步骤遇到困难：
- **直接告诉我卡在哪一步**
- 我可以提供**详细的图文指导**
- 或者帮您**准备好的文本内容**，您只需复制粘贴

---

## 🎯 完成后的验证清单

发布完成后，请确认以下链接可访问：

- [ ] **GitHub**: https://github.com/xuanji-ai-2026/group-debug-deploy-expert ✅
- [ ] **npm**: https://www.npmjs.com/package/open-group
- [ ] **ClawHub**: https://clawhub.ai/skills/group-debug-deploy-expert
- [ ] **SkillHub**: https://www.skillhub.club/skills/xxx/group-debug-deploy-expert
- [ ] **agentskills**: https://agentskills.io/skills/group-debug-deploy-expert

全部完成后回复我 **"全部完成"**，我会：
1. ✅ 更新README中的所有平台徽章链接
2. ✅ 推送更新到GitHub
3. ✅ 生成最终的**全平台发布成功报告**
4. ✅ 提供**后续推广建议**

---

**祝发布顺利！有任何问题随时问我！** 🚀

*最后更新: 2026-05-14*
