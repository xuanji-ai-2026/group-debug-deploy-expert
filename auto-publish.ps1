#!/usr/bin/env pwsh
# =============================================================================
# ClawHub & SkillHub Auto-Publish Script v1.0.0
# 自动化发布到 ClawHub 和 SkillHub 平台
# Supports: GitHub OAuth login, API Token auth, auto-publish
# =============================================================================

param(
    [string]$Action = "publish",        # publish|login|status|all
    [string]$Token = "",                 # API Token (optional, for headless)
    [string]$SkillPath = ".trae/skills/group-debug-deploy-expert",
    [string]$Slug = "group-debug-deploy-expert",
    [string]$Name = "Group Debug & Deploy Expert",
    [string]$Version = "1.0.2",
    [switch]$SkipClawHub = $false,
    [switch]$SkipSkillHub = $false,
    [switch]$Yes = $false                # Skip confirmations
)

# =============================================================================
# Configuration / 配置
# =============================================================================
$ErrorActionPreference = "Stop"
$CLAWHUB_REGISTRY = "https://clawhub.ai"
$SKILLHUB_REGISTRY = "https://skill.xfyun.cn"  # 讯飞云 SkillHub 公开实例

Write-Host @"
╔══════════════════════════════════════════════════════════╗
║  🚀 ClawHub & SkillHub Auto-Publish Tool v1.0.0         ║
║  自动化发布工具 - 支持 GitHub OAuth + API Token          ║
╚══════════════════════════════════════════════════════════╝
"@ -ForegroundColor Cyan

# =============================================================================
# Function: Check Prerequisites / 检查前置条件
# =============================================================================
function Test-Prerequisites {
    Write-Host "`n▶ Step 1/5: Checking prerequisites..." -ForegroundColor Yellow
    
    # Check Node.js
    try {
        $nodeVersion = node -v
        Write-Host "  ✅ Node.js: $nodeVersion" -ForegroundColor Green
    } catch {
        Write-Host "  ❌ Node.js not found. Please install Node.js first." -ForegroundColor Red
        exit 1
    }
    
    # Check npm/npx
    try {
        $npmVersion = npm -v
        Write-Host "  ✅ npm: $npmVersion" -ForegroundColor Green
    } catch {
        Write-Host "  ❌ npm not found." -ForegroundColor Red
        exit 1
    }
    
    # Check/install clawhub CLI
    Write-Host "  ℹ️  Checking/Installing ClawHub CLI..." -ForegroundColor Gray
    npm install -g clawhub 2>$null | Out-Null
    $clawhubVersion = npx clawhub --cli-version 2>$null
    if ($clawhubVersion) {
        Write-Host "  ✅ ClawHub CLI: v$clawhubVersion" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  ClawHub CLI installation may have issues" -ForegroundColor Yellow
    }
    
    # Check skill path
    if (Test-Path $SkillPath) {
        $fileCount = (Get-ChildItem $SkillPath -File).Count
        Write-Host "  ✅ Skill folder: $SkillPath ($fileCount files)" -ForegroundColor Green
        
        # Verify SKILL.md exists
        if (Test-Path "$SkillPath\SKILL.md") {
            Write-Host "  ✅ SKILL.md found" -ForegroundColor Green
        } else {
            Write-Host "  ❌ SKILL.md not found in skill folder!" -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host "  ❌ Skill folder not found: $SkillPath" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "`n  ✅ All prerequisites met!" -ForegroundColor Green
}

# =============================================================================
# Function: Login to ClawHub / 登录 ClawHub
# =============================================================================
function Connect-ClawHub {
    param([string]$ApiToken)
    
    Write-Host "`n▶ Step 2/5: Authenticating with ClawHub..." -ForegroundColor Yellow
    
    if ($ApiToken) {
        Write-Host "  🔑 Using API Token authentication..." -ForegroundColor Gray
        npx clawhub login --token $ApiToken
    } else {
        Write-Host "  🌐 Opening browser for GitHub OAuth login..." -ForegroundColor Gray
        Write-Host "     (If browser doesn't open, copy the URL manually)" -ForegroundColor Gray
        npx clawhub login
    }
    
    # Verify login
    $whoami = npx clawhub whoami 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  ✅ Authentication successful!" -ForegroundColor Green
        Write-Host "     $whoami" -ForegroundColor White
        return $true
    } else {
        Write-Host "  ❌ Authentication failed!" -ForegroundColor Red
        return $false
    }
}

# =============================================================================
# Function: Publish to Platform / 发布到平台
# =============================================================================
function Publish-ToPlatform {
    param(
        [string]$Platform,
        [string]$RegistryUrl,
        [string]$SlugOverride = ""
    )
    
    $effectiveSlug = if ($SlugOverride) { $SlugOverride } else { $Slug }
    
    Write-Host "`n▶ Publishing to $Platform..." -ForegroundColor Yellow
    Write-Host "  📦 Slug: $effectiveSlug" -ForegroundColor Gray
    Write-Host "  📌 Name: $Name" -ForegroundColor Gray
    Write-Host "  🔢 Version: $Version" -ForegroundColor Gray
    
    $publishArgs = @(
        "publish",
        $SkillPath,
        "--slug", $effectiveSlug,
        "--name", $Name,
        "--version", $Version
    )
    
    if ($RegistryUrl -ne $CLAWHUB_REGISTRY) {
        $publishArgs += "--registry", $RegistryUrl
    }
    
    if ($Yes) {
        $publishArgs += "--yes"
    }
    
    & npx clawhub $publishArgs
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  ✅ Successfully published to $Platform!" -ForegroundColor Green
        Write-Host "  🔗 URL: https://$($Platform.ToLower().Replace('hub',''))/$effectiveSlug" -ForegroundColor Cyan
        return $true
    } else {
        Write-Host "  ❌ Failed to publish to $Platform!" -ForegroundColor Red
        return $false
    }
}

# =============================================================================
# Function: Show Status / 显示状态
# =============================================================================
function Show-PublishStatus {
    Write-Host "`n▶ Checking current publish status..." -ForegroundColor Yellow
    
    # Check ClawHub
    Write-Host "`n📊 ClawHub Status:" -ForegroundColor Cyan
    npx clawhub inspect $Slug 2>$null
    
    # Search for our skill
    Write-Host "`n🔍 Search Results:" -ForegroundColor Cyan
    npx clawhub search "debug deploy expert" --limit 5 2>$null
}

# =============================================================================
# Main Execution Logic / 主执行逻辑
# =============================================================================
switch ($Action.ToLower()) {
    "login" {
        Test-Prerequisites
        Connect-ClawHub -ApiToken $Token
    }
    
    "status" {
        Test-Prerequisites
        Show-PublishStatus
    }
    
    "publish" {
        Test-Prerequisites
        
        # Login
        $loggedIn = Connect-ClawHub -ApiToken $Token
        if (-not $loggedIn) {
            Write-Host "`n❌ Cannot proceed without authentication." -ForegroundColor Red
            exit 1
        }
        
        # Publish to ClawHub
        $clawhubSuccess = $true
        if (-not $SkipClawHub) {
            $clawhubSuccess = Publish-ToPlatform -Platform "ClawHub" -RegistryUrl $CLAWHUB_REGISTRY
        } else {
            Write-Host "`n⏭️  Skipping ClawHub (as requested)" -ForegroundColor Yellow
        }
        
        # Publish to SkillHub
        $skillhubSuccess = $true
        if (-not $SkipSkillHub) {
            $skillhubSuccess = Publish-ToPlatform -Platform "SkillHub" -RegistryUrl $SKILLHUB_REGISTRY -SlugOverride "@zfx1818/open-group"
        } else {
            Write-Host "`n⏭️  Skipping SkillHub (as requested)" -ForegroundColor Yellow
        }
        
        # Summary
        Write-Host @"
`n╔══════════════════════════════════════════════════════════╗
║  📋 Publish Summary / 发布摘要                              ║
╠══════════════════════════════════════════════════════════╣
║  Skill: $Name                                               ║
║  Version: $Version                                           ║
║  Slug: $Slug                                                 ║
║                                                            ║
║  ClawHub: $(if($clawhubSuccess){'✅ Success'}else{'❌ Failed'})                                              ║
║  SkillHub: $(if($skillhubSuccess){'✅ Success'}else{'❌ Failed'})                                             ║
╚══════════════════════════════════════════════════════════╝
"@ -ForegroundColor $(if($clawhubSuccess -and $skillhubSuccess){"Green"}else{"Yellow"})
    }
    
    "all" {
        Test-Prerequisites
        $loggedIn = Connect-ClawHub -ApiToken $Token
        
        if ($loggedIn) {
            Publish-ToPlatform -Platform "ClawHub" -RegistryUrl $CLAWHUB_REGISTRY
            Publish-ToPlatform -Platform "SkillHub" -RegistryUrl $SKILLHUB_REGISTRY -SlugOverride "@zfx1818/open-group"
            Show-PublishStatus
        }
    }
    
    default {
        Write-Host @"
Usage:
  .\auto-publish.ps1 -Action publish              # Publish to both platforms
  .\auto-publish.ps1 -Action login                # GitHub OAuth login only
  .\auto-publish.ps1 -Action status               # Check publish status
  .\auto-publish.ps1 -Action all                  # Full workflow
  
Options:
  -Token YOUR_TOKEN       Use API Token instead of browser login
  -SkillPath ./path       Custom skill folder path
  -Slug my-skill          Custom slug name
  -Name "Display Name"    Custom display name
  -Version 1.0.0          Version number
  -SkipClawHub            Skip ClawHub publishing
  -SkipSkillHub           Skip SkillHub publishing
  -Yes                    Skip confirmation prompts

Examples:
  .\auto-publish.ps1 -Action login                           # Login first
  .\auto-publish.ps1 -Action publish -Token clh_xxxxx        # Token-based publish
  .\auto-publish.ps1 -Action publish -SkipSkillHub           # Only ClawHub
"@ -ForegroundColor White
    }
}
