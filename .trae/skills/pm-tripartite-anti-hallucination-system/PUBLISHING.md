# 🚀 Publishing Guide - PM-Tripartite-Anti-Hallucination-System

## 📋 Quick Start (3 Methods)

### **Method 1: Interactive Full Publish (Recommended)**

```powershell
cd .trae\skills\pm-tripartite-anti-hallucination-system
.\publish.ps1
```

**Features:**
- ✅ Pre-flight checks (dist files, git status)
- ✅ GitHub Release creation (with binary attachments)
- ✅ PyPI upload (with token prompt)
- ✅ Post-publish summary with links
- ✅ Dry-run mode (`-DryRun`)

---

### **Method 2: Quick PyPI-Only Publish**

```powershell
# Option A: With token parameter
.\quick-publish.ps1 -Token "pypi-your-token"

# Option B: Using environment variable
$env:TWINE_PASSWORD = "pypi-your-token"
.\quick-publish.ps1

# Option C: Using .env file (see Configuration section)
.\quick-publish.ps1
```

---

### **Method 3: Manual Step-by-Step**

#### **Step 1: Create GitHub Release**
1. Open: https://github.com/xuanji-ai-2026/group-debug-deploy-expert/releases/new
2. Tag: `v1.0.0`
3. Title: `🚀 v1.0.0 - Initial Release`
4. Description: Copy content from `RELEASE_NOTES.md`
5. Attach binaries from `dist/`:
   - `pm_tripartite_anti_hallucination_system-1.0.0-py3-none-any.whl`
   - `pm_tripartite_anti_hallucination_system-1.0.0.tar.gz`
6. Click **"Publish release"**

#### **Step 2: Upload to PyPI**
```bash
# Set token
set TWINE_PASSWORD=pypi-your-token

# Upload
twine upload dist/*
```

---

## 🔧 Configuration Options

### **Option 1: Command-Line Parameters**

```powershell
# Full publish (GitHub + PyPI)
.\publish.ps1

# Skip GitHub, only PyPI
.\publish.ps1 -SkipGitHub

# Skip PyPI, only GitHub
.\publish.ps1 -SkipPyPI

# Dry run (preview only)
.\publish.ps1 -DryRun

# Explicit token
.\publish.ps1 -Token "pypi-xxxxx"
```

### **Option 2: Environment Variables**

```powershell
# PowerShell session
$env:TWINE_PASSWORD = "pypi-your-token"
.\publish.ps1

# System-wide (permanent)
[Environment]::SetEnvironmentVariable("TWINE_PASSWORD", "pypi-your-token", "User")
```

### **Option 3: .env File (Recommended for Development)**

1. Copy template:
```bash
copy .env.example .env
```

2. Edit `.env`:
```env
TWINE_PASSWORD=pypi-your-api-token-here
```

3. Run script (auto-detects `.env`):
```powershell
.\publish.ps1
```

⚠️ **Security Note**: Add `.env` to `.gitignore` (already done)!

---

## 🔑 Getting Your PyPI API Token

### **Step 1: Register/Login**
- Visit: https://pypi.org/account/register/
- Or login if you already have an account

### **Step 2: Generate Token**
1. Go to: https://pypi.org/account/settings/#api-tokens
2. Click **"Add API token"**
3. Fill in:
   - Token name: `GitHub Actions` (or any name)
   - Scope: **"Entire account"** (if you'll publish multiple projects)
     - OR **"Project"** (for this project only, more secure)
4. Click **"Generate token"**
5. **Copy the token immediately!** (Format: `pypi-xxxxx...`)

### **Step 3: Use Token**
```powershell
# Method A: Parameter
.\publish.ps1 -Token "pypi-xxxxx..."

# Method B: Environment
$env:TWINE_PASSWORD = "pypi-xxxxx..."
.\publish.ps1
```

---

## 🛠️ Troubleshooting

### **Issue: "No dist/ directory found"**
**Solution**: Build packages first:
```bash
python -m build
```

### **Issue: "403 Forbidden from PyPI"**
**Causes & Solutions**:
1. **Invalid Token**: Regenerate token from PyPI settings
2. **Wrong Username**: Must be `__token__` (with underscores), NOT your username
3. **Package Name Taken**: Check if name already exists on PyPI
4. **Account Not Verified**: Verify your email on PyPI

### **Issue: "gh not recognized"**
**Solution**: Install GitHub CLI:
```bash
winget install GitHub.cli
# Then authenticate:
gh auth login
```

### **Issue: "Permission denied" when pushing tag**
**Cause**: Git credential lock or authentication issue
**Solution**:
```bash
# Clear credential cache
git credential reject <<EOF
protocol=https
host=github.com
EOF

# Try pushing again
git push origin v1.0.0
```

### **Issue: "Package already exists"**
**Cause**: Version already published
**Solution**: Update version in `pyproject.toml`, then rebuild:
```bash
# Change version to 1.0.1 in pyproject.toml
python -m build
twine upload dist/*  # Will upload as new version
```

---

## ✅ Post-Publish Verification Checklist

After publishing, verify:

- [ ] **GitHub Release visible**: 
  https://github.com/xuanji-ai-2026/group-debug-deploy-expert/releases/tag/v1.0.0

- [ ] **PyPI package accessible**: 
  https://pypi.org/project/pm-tripartite-anti-hallucination-system/

- [ ] **Installation works**:
  ```bash
  pip install pm-tripartite-anti-hallucination-system
  python -c "import pm_tripartite_anti_hallucination_system; print(pm_tripartite_anti_hallucination_system.__version__)"
  # Expected output: 1.0.0
  ```

- [ ] **Demo scripts run**:
  ```bash
  cd path/to/installed/package
  python demo_complete_self_driving.py
  ```

---

## 📊 Advanced Usage

### **Publish to TestPyPI First (Recommended)**

Test your package before publishing to production:

```bash
# Upload to TestPyPI
twine upload --repository testpypi dist/*

# Test installation
pip install --index-url https://test.pypi.org/simple/ pm-tripartite-anti-hallucination-system

# Verify it works
python -c "from self_driving_engine import SelfDrivingEngine; print('OK')"

# If everything works, publish to production PyPI
twine upload dist/*
```

### **Automated Publishing via CI/CD**

Your `.github/workflows/ci-cd.yml` is already configured!

To enable automatic publishing on tag push:

1. Go to: https://github.com/xuanji-ai-2026/group-debug-deploy-expert/settings/secrets/actions
2. Click **"New repository secret"**
3. Name: `PYPI_API_TOKEN`
4. Value: Your PyPI API token
5. Push a new tag:
   ```bash
   git tag v1.0.1
   git push origin v1.0.1
   ```
6. GitHub Actions will automatically build → test → publish!

### **Batch Publishing Multiple Versions**

```powershell
# Publish versions 1.0.0, 1.0.1, 1.1.0
@("1.0.0", "1.0.1", "1.1.0") | ForEach-Object {
    $ver = $_
    Write-Host "`n📦 Publishing v$ver..." -ForegroundColor Yellow
    
    # Update pyproject.toml version here...
    
    # Build and upload
    python -m build
    twine upload dist/* 
}
```

---

## 🎯 Best Practices

### **Before Publishing**
- [x] Run tests: `pytest tests/ -v`
- [x] Update version in `pyproject.toml`
- [x] Update `CHANGELOG.md`
- [x] Update `RELEASE_NOTES.md`
- [x] Build locally: `python -m build`
- [x] Test install: `pip install . -e`
- [x] Commit all changes: `git add . && git commit -m "chore: release v1.0.0"`
- [x] Create tag: `git tag -a v1.0.0 -m "Release v1.0.0"`
- [x] Push: `git push origin main --tags`

### **Security**
- ❌ NEVER commit `.env` file with real tokens
- ❌ NEVER log or print tokens in console output
- ✅ Use environment variables or `.env` file (gitignored)
- ✅ Rotate tokens periodically
- ✅ Use scoped tokens (project-level instead of account-level)

### **Versioning**
Follow [Semantic Versioning](https://semver.org/):
- **MAJOR** (1.x.x): Breaking changes
- **MINOR** (x.1.x): New features (backward-compatible)
- **PATCH** (x.x.1): Bug fixes only

---

## 📞 Support

**Issues?** Open a ticket at:
https://github.com/xuanji-ai-2026/group-debug-deploy-expert/issues

**Questions?** Check:
- [README.md](./README.md) - Quick start guide
- [SKILL.md](./SKILL.md) - Complete technical documentation
- [CHANGELOG.md](./CHANGELOG.md) - Version history

---

*Happy Publishing! 🚀*
