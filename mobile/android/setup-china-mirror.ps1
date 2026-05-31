# ========================================
#  北极星AI - 中国开发者一键配置脚本
#  自动配置阿里云镜像 + 国内加速
# ========================================

$ErrorActionPreference = "Stop"
Write-Host ""
Write-Host "╔════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║   🇨🇳 北极星AI - 中国开发者环境配置工具     ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

Write-Host "[1/5] 检查项目目录..." -ForegroundColor Yellow
if (-not (Test-Path "build.gradle.kts")) {
    Write-Host "[错误] 未找到build.gradle.kts，请确保在android根目录运行此脚本" -ForegroundColor Red
    exit 1
}
Write-Host "[✓] 项目路径: $projectRoot" -ForegroundColor Green

Write-Host ""
Write-Host "[2/5] 配置项目级 build.gradle.kts (阿里云Maven镜像)..." -ForegroundColor Yellow

$buildGradlePath = "build.gradle.kts"
if (Test-Path $buildGradlePath) {
    $content = Get-Content $buildGradlePath -Raw -Encoding UTF8
    
    # 检查是否已经配置过阿里云
    if ($content -match "aliyun.com") {
        Write-Host "[跳过] 已配置阿里云镜像" -ForegroundColor Yellow
    } else {
        # 在 allprojects { repositories { 后添加阿里云镜像
        $aliyunConfig = @"

        // ========== 阿里云镜像（中国开发者加速）==========
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        // ================================================
"@
        
        # 找到 repositories { 的位置并插入
        $content = $content -replace '(?m)(\s*repositories\s*\{)', "`$aliyunConfig`n`$1"
        
        Set-Content $buildGradlePath -Value $content -Encoding UTF8
        Write-Host "[✓] 已添加阿里云Maven镜像" -ForegroundColor Green
    }
} else {
    Write-Host "[警告] 未找到项目级build.gradle.kts" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "[3/5] 配置 settings.gradle.kts..." -ForegroundColor Yellow

$settingsPath = "settings.gradle.kts"
if (Test-Path $settingsPath) {
    $content = Get-Content $settingsPath -Raw -Encoding UTF8
    
    if ($content -match "aliyun.com") {
        Write-Host "[跳过] 已配置阿里云镜像" -ForegroundColor Yellow
    } else {
        $aliyunSettings = @'
pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
    }
}
'@
        
        Set-Content $settingsPath -Value $aliyunSettings -Encoding UTF8
        Write-Host "[✓] 已配置settings.gradle.kts" -ForegroundColor Green
    }
} else {
    Write-Host "[提示] 未找到settings.gradle.kts（可选文件）" -ForegroundColor Gray
}

Write-Host ""
Write-Host "[4/5] 配置 gradle.properties (性能优化+镜像)..." -ForegroundColor Yellow

$gradlePropsPath = "gradle.properties"
if (Test-Path $gradlePropsPath) {
    $content = Get-Content $gradlePropsPath -Raw -Encoding UTF8
    
    if ($content -match "org.gradle.parallel=true") {
        Write-Host "[跳过] 已优化过Gradle配置" -ForegroundColor Yellow
    } else {
        $optimizations = @"

# ============================================
# 中国开发者优化配置
# ============================================

# 并行构建（加快速度）
org.gradle.parallel=true
org.gradle.caching=true

# JVM内存配置（根据电脑调整）
org.gradle.jvmargs=-Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8

# Daemon超时时间（保持后台运行）
org.gradle.daemon.idletimeout=600000

# 控制台输出编码
org.gradle.console.encoding=UTF-8
"@
        
        Add-Content -Path $gradlePropsPath -Value $optimizations -Encoding UTF8
        Write-Host "[✓] 已添加性能优化配置" -ForegroundColor Green
    }
} else {
    Write-Host "[创建] 新建gradle.properties" -ForegroundColor Yellow
    
    $defaultProps = @'
# Project-wide Gradle settings.
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true

# 中国开发者优化
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon.idletimeout=600000
org.gradle.console.encoding=UTF-8
'@
    
    Set-Content -Path $gradlePropsPath -Value $defaultProps -Encoding UTF8
    Write-Host "[✓] 已创建gradle.properties" -ForegroundColor Green
}

Write-Host ""
Write-Host "[5/5] 验证配置..." -ForegroundColor Yellow

$filesToCheck = @(
    "build.gradle.kts",
    "settings.gradle.kts",
    "gradle.properties",
    "app/build.gradle.kts",
    "gradle/wrapper/gradle-wrapper.properties"
)

$allExist = $true
foreach ($file in $filesToCheck) {
    if (Test-Path $file) {
        $size = [math]::Round((Get-Item $file).Length / 1KB, 2)
        Write-Host "  [✓] $file ($size KB)" -ForegroundColor Green
    } elseif ($file -eq "settings.gradle.kts") {
        Write-Host "  [-] $file (可选)" -ForegroundColor Gray
    } else {
        Write-Host "  [!] $file (缺失)" -ForegroundColor Red
        $allExist = $false
    }
}

Write-Host ""
if ($allExist) {
    Write-Host "╔════════════════════════════════════════════════╗" -ForegroundColor Green
    Write-Host "║          ✅ 配置完成！可以开始构建了！         ║" -ForegroundColor Green
    Write-Host "╠════════════════════════════════════════════════╣" -ForegroundColor Green
    Write-Host "║                                          ║" -ForegroundColor White
    Write-Host "║  下一步操作:                              ║" -ForegroundColor White
    Write-Host "║  1. 打开 Android Studio                 ║" -ForegroundColor White
    Write-Host "║  2. File → Open → 选择当前目录          ║" -ForegroundColor White
    Write-Host "║  3. 点击 Sync Now (如果弹出提示)        ║" -ForegroundColor White
    Write-Host "║  4. Build → Build APK(s)               ║" -ForegroundColor White
    Write-Host "║                                          ║" -ForegroundColor White
    Write-Host "║  预计时间: 首次10-20分钟                ║" -ForegroundColor Cyan
    Write-Host "║           后续 1-3分钟                   ║" -ForegroundColor Cyan
    Write-Host "╚════════════════════════════════════════════════╝" -ForegroundColor Green
    Write-Host ""
    Write-Host "详细指南: CHINA_BUILD_GUIDE.md" -ForegroundColor Yellow
    exit 0
} else {
    Write-Host "[警告] 部分文件缺失，但核心配置已完成" -ForegroundColor Yellow
    Write-Host "建议查看 CHINA_BUILD_GUIDE.md 手动补充" -ForegroundColor Yellow
    exit 0
}
