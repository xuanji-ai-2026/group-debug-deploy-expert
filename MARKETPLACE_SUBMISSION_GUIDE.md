# 🚀 ClawHub & SkillHub 一键发布指南
<!-- Auto-Publish Guide v2.0.0 - Updated for CLI-based publishing -->

> **Complete Automation Guide** - Publish your skills to ClawHub and SkillHub with CLI tools  
> **完整自动化指南** - 使用CLI工具一键发布技能到ClawHub和SkillHub

---

## 📦 Quick Start / 快速开始 (3分钟)

### Option 1: Automated Script (Recommended) <!-- 方式1: 自动化脚本(推荐) -->

```powershell
# Windows PowerShell
.\auto-publish.ps1 -Action publish
```

```bash
# Linux/macOS Bash
chmod +x auto-publish.sh && ./auto-publish.sh publish
```

### Option 2: Manual CLI Commands <!-- 方式2: 手动CLI命令 -->

```bash
# Step 1: Install CLI / 安装CLI工具
npm install -g clawhub

# Step 2: Login with GitHub OAuth / 使用GitHub OAuth登录
clawhub login

# Step 3: Publish to ClawHub / 发布到ClawHub
clawhub publish .trae/skills/group-debug-deploy-expert `
  --slug group-debug-deploy-expert `
  --name "Group Debug & Deploy Expert" `
  --version 1.0.2

# Step 4: Publish to SkillHub (if needed) / 发布到SkillHub(如需要)
CLAWHUB_REGISTRY=https://skillhub.iflytek.com clawhub publish .trae/skills/group-debug-deploy-expert `
  --slug @zfx1818/open-group `
  --name "Group Debug & Deploy Expert" `
  --version 1.0.2
```

---

## 🔧 Prerequisites / 前置条件

| Requirement | Version | Check Command |
|-------------|---------|---------------|
| **Node.js** | >= 16.0.0 | `node -v` |
| **npm** | >= 8.0.0 | `npm -v` |
| **ClawHub CLI** | Latest | `npm install -g clawhub` |
| **GitHub Account** | Any | For OAuth login |
| **Skill Folder** | With SKILL.md | Verify path exists |

---

## 📖 Detailed Steps / 详细步骤

### Step 1: Install ClawHub CLI <!-- 步骤1: 安装ClawHub CLI -->

#### Windows:
```powershell
npm install -g clawhub
clawhub --cli-version  # Should show v0.15.0+
```

#### macOS/Linux:
```bash
npm install -g clawhub
clawhub --cli-version
```

#### Verification:
```
✅ If you see version number → Installation successful / 安装成功
❌ If "command not found" → Check Node.js installation / 检查Node.js安装
```

---

### Step 2: Authentication / 身份验证 <!-- 步骤2: 登录认证 -->

#### Method A: GitHub OAuth (Recommended) <!-- 方法A: GitHub OAuth(推荐) -->

```bash
clawhub login
```

**What happens:**
1. Browser opens to `https://clawhub.ai/cli/auth`
2. Login with your GitHub account
3. Click "Authorize application"
4. Browser redirects back to CLI automatically
5. Token saved locally for future use

**发生过程:**
1. 浏览器打开 ClawHub 授权页面
2. 使用 GitHub 账号登录
3. 点击 "Authorize application"
4. 浏览器自动跳转回 CLI
5. Token 保存在本地供后续使用

#### Method B: API Token (Headless/CI) <!-- 方法B: API Token(无头/CI环境) -->

1. **Get Token from ClawHub Web UI:**
   - Go to https://clawhub.ai
   - Login → Settings → API Tokens
   - Create new token → Copy it

2. **Use Token:**
   ```bash
   clawhub login --token clh_xxxxxxxxxxxx
   ```

#### Verify Login:
```bash
clawhub whoami
# Output: { handle: "your-github-username", displayName: "Your Name" }
```

---

### Step 3: Prepare Skill Folder <!-- 步骤3: 准备技能文件夹 -->

**Required Structure:**
```
your-skill/
├── SKILL.md          # Required! Main skill definition
├── README.md         # Documentation
├── LICENSE           # License file
├── VERSION           # Version info
└── [other files...]  # Supporting files
```

**SKILL.md Frontmatter Example:**
```markdown
---
name: group-debug-deploy-expert
description: "Universal debug & deploy expert with 21 iron principles..."
version: 1.0.2
metadata:
  tags:
    - debug
    - deploy
    - ai-agent
    - devops
    - enterprise
---

# Your skill content here...
```

---

### Step 4: Publish to ClawHub <!-- 步骤4: 发布到ClawHub -->

#### Basic Publish:
```bash
clawhub publish ./path/to/skill \
  --slug group-debug-deploy-expert \
  --name "Group Debug & Deploy Expert" \
  --version 1.0.2
```

#### Advanced Options:
```bash
clawhub publish ./path/to/skill \
  --slug group-debug-deploy-expert \
  --name "Group Debug & Deploy Expert" \
  --version 1.0.2 \
  --yes                    # Skip confirmation prompts
  --registry https://clawhub.ai  # Custom registry (optional)
```

#### Expected Output:
```
✅ Uploading files... (13 files)
✅ Validating SKILL.md frontmatter...
✅ Processing metadata...
✅ Creating version 1.0.2...
✅ Published successfully!

🔗 View at: https://clawhub.ai/your-handle/group-debug-deploy-expert
```

---

### Step 5: Publish to SkillHub (Optional) <!-- 步骤5: 发布到SkillHub(可选) -->

SkillHub uses **ClawHub-compatible API**, so same CLI works:

```bash
# Set SkillHub registry
export CLAWHUB_REGISTRY="https://skill.xfyun.cn"  # 讯飞云 SkillHub 公开实例

# Login to SkillHub (if different account)
clawhub login --token YOUR_SKILLHUB_TOKEN

# Publish
clawhub publish ./path/to/skill \
  --slug @zfx1818/open-group \
  --name "Group Debug & Deploy Expert" \
  --version 1.0.2
```

**Note:** SkillHub slug format may vary by instance. Check your SkillHub admin docs.

---

## ✅ Post-Publish Verification / 发布后验证

### Check Your Skill:
```bash
# Inspect published skill
clawhub inspect group-debug-deploy-expert

# Search for it
clawhub search "debug deploy expert"

# View all versions
clawhub inspect group-debug-deploy-expert --versions
```

### Update Existing Skill:
```bash
# Change version in SKILL.md or use --version flag
clawhub publish ./path/to/skill \
  --slug group-debug-deploy-expert \
  --version 1.0.3  # Increment version!
```

---

## 🛠️ Troubleshooting / 故障排除

### Common Issues:

| Error | Cause | Solution |
|-------|-------|----------|
| `EPERM: operation not permitted` | Sandbox/file permission | Run in terminal, not IDE |
| `Unauthorized` | Invalid/expired token | Re-run `clawhub login` |
| `Version already exists` | Duplicate version | Increment version number |
| `SKILL.md validation failed` | Missing/invalid frontmatter | Check YAML syntax |
| `Rate limit exceeded` | Too many requests | Wait and retry |

### Permission Issues (Trae IDE Users):
If you see sandbox errors like:
```
Error: EPERM: operation not permitted, mkdir '...\AppData\Roaming\clawhub'
```

**Solution:** Open PowerShell/CMD terminal directly (not through IDE):
```powershell
cd D:\BeijiXing-AI
.\auto-publish.ps1 -Action publish
```

---

## 📊 Platform Comparison / 平台对比

| Feature | ClawHub | SkillHub |
|---------|---------|----------|
| **URL** | https://clawhub.ai | https://skill.xfyun.cn |
| **Auth** | GitHub OAuth | API Token / OAuth |
| **CLI** | `clawhub` | Same (compatible) |
| **API** | `/api/v1/*` | Compatible layer |
| **Visibility** | Public | Public/Private |
| **Slug Format** | `skill-name` | `@namespace/skill-name` |
| **Free Tier** | ✅ Yes | ✅ Yes |
| **Rate Limit** | 300 writes/min | Varies by instance |

*Adjust SkillHub URL based on your deployment

---

## 🔄 CI/CD Integration / 持续集成

### GitHub Actions Example:
```yaml
name: Publish to ClawHub

on:
  push:
    tags:
      - 'v*'

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
      
      - name: Install ClawHub CLI
        run: npm install -g clawhub
      
      - name: Publish to ClawHub
        env:
          CLAWHUB_TOKEN: ${{ secrets.CLAWHUB_TOKEN }}
        run: |
          clawhub login --token $CLAWHUB_TOKEN
          clawhub publish .trae/skills/group-debug-deploy-expert \
            --slug group-debug-deploy-expert \
            --name "Group Debug & Deploy Expert" \
            --version ${GITHUB_REF#refs/tags/v} \
            --yes
```

---

## 📞 Support & Help / 支持与帮助

### Official Resources:
- **ClawHub Docs**: https://github.com/openclaw/clawhub/blob/main/docs/http-api.md
- **ClawHub CLI**: https://github.com/openclaw/clawhub/blob/main/docs/cli.md
- **SkillHub Integration**: https://github.com/iflytek/skillhub/blob/main/docs/openclaw-integration.md

### Get Help:
```bash
# CLI help
clawhub --help
clawhub publish --help
clawhub login --help

# Community Discord (if available)
# Check ClawHub website for links
```

### Contact Us:
- **Email**: z18288090942@gmail.com
- **Phone**: +86 19537722739
- **GitHub Issues**: https://github.com/xuanji-ai-2026/group-debug-deploy-expert/issues

---

<div align="center">

**🎉 Happy Publishing! / 祝发布顺利！**

[Back to README](./README.md) | [User Guide](./USER_GUIDE.md) | [Use Cases](./USE_CASES.md)

</div>
