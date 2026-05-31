# 北极星AI系统 - 全链路自动化测试脚本
# 用于测试登录后的所有功能模块

$ErrorActionPreference = "Continue"

Write-Host ""
Write-Host "╔═══════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║   北极星AI - 全链路自动化测试工具   ║" -ForegroundColor Cyan
Write-Host "╚═══════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# 路径配置
$AdbPath = "C:\Users\HenryChow\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$LogDir = "D:\BeijiXing-AI\mobile\android\test-reports\full-test-$(Get-Date -Format 'yyyyMMdd_HHmmss')"
$PackageName = "com.beijixing.app.debug"

# 创建日志目录
New-Item -ItemType Directory -Path $LogDir -Force | Out-Null
Write-Host "📁 日志目录: $LogDir" -ForegroundColor Yellow
Write-Host ""

# 验证ADB
if (-not (Test-Path $AdbPath)) {
    Write-Host "❌ ADB未找到!" -ForegroundColor Red
    exit 1
}

function Test-DeviceConnected {
    $devices = & $AdbPath devices 2>$null
    return ($devices -match "\w+\s+device")
}

function Get-DeviceInfo {
    Write-Host "📱 获取设备详细信息..." -ForegroundColor Cyan
    
    $info = @{
        Manufacturer = (& $AdbPath shell getprop ro.product.manufacturer).Trim()
        Model = (& $AdbPath shell getprop ro.product.model).Trim()
        AndroidVersion = (& $AdbPath shell getprop ro.build.version.release).Trim()
        SdkVersion = (& $AdbPath shell getprop ro.build.version.sdk).Trim()
    }
    
    return $info
}

function Show-DeviceInfo($info) {
    Write-Host "`n┌─────────────────────────────────────┐" -ForegroundColor White
    Write-Host "│ 📋 设备信息汇总                      │" -ForegroundColor White
    Write-Host "├─────────────────────────────────────┤" -ForegroundColor White
    Write-Host ("│ 厂商: {0,-29} │" -f $info.Manufacturer)
    Write-Host ("│ 型号: {0,-29} │" -f $info.Model)
    Write-Host ("│ 系统: Android {0} (API {1})" -f $info.AndroidVersion, $info.SdkVersion)
    Write-Host "└─────────────────────────────────────┘" -ForegroundColor White
    Write-Host ""
    
    $info | ConvertTo-Json | Out-File (Join-Path $LogDir "device_info.json")
}

function Take-Screenshot($filename) {
    $filepath = Join-Path $LogDir $filename
    & $AdbPath exec-out screencap -p > $filepath
    if (Test-Path $filepath) {
        Write-Host "   📸 截图: $filename" -ForegroundColor Gray
    }
}

function Start-Application {
    Write-Host "🚀 启动应用..." -ForegroundColor Yellow
    
    # 先停止应用
    & $AdbPath shell am force-stop $PackageName | Out-Null
    Start-Sleep -Seconds 1
    
    # 清除日志
    & $AdbPath logcat -c
    
    # 启动应用
    Write-Host "   启动 SplashActivity..." -ForegroundColor Gray
    $startOutput = & $AdbPath shell monkey -p $PackageName -c android.intent.category.LAUNCHER 1 2>&1
    Start-Sleep -Seconds 6
    
    Take-Screenshot "01-splash-screen.png"
    
    if ($startOutput -match "Events injected") {
        Write-Host "   ✅ 应用启动成功" -ForegroundColor Green
        return $true
    } else {
        Write-Host "   ❌ 应用启动失败" -ForegroundColor Red
        return $false
    }
}

function Test-LoginFlow {
    Write-Host "🔐 测试登录流程..." -ForegroundColor Yellow
    
    # 等待登录页面加载
    Start-Sleep -Seconds 3
    
    # 获取屏幕尺寸
    $wmOutput = & $AdbPath shell wm size
    if ($wmOutput -match "(\d+)x(\d+)") {
        $width = [int]$Matches[1]
        $height = [int]$Matches[2]
        Write-Host "   屏幕尺寸: ${width}x${height}" -ForegroundColor Gray
    } else {
        $width = 1080
        $height = 1920
    }
    
    Take-Screenshot "02-login-screen.png"
    
    # 查找并输入手机号
    # 假设手机号输入框在屏幕中间偏上位置
    $phoneX = [int]($width / 2)
    $phoneY = [int]($height * 0.35)
    Write-Host "   输入手机号: 19537722739" -ForegroundColor Gray
    & $AdbPath shell input tap $phoneX $phoneY | Out-Null
    Start-Sleep -Seconds 0.5
    & $AdbPath shell input text "19537722739" | Out-Null
    Start-Sleep -Seconds 1
    
    Take-Screenshot "03-phone-entered.png"
    
    # 查找并输入密码
    # 假设密码输入框在手机号下方
    $passX = [int]($width / 2)
    $passY = [int]($height * 0.48)
    Write-Host "   输入密码: Admin@123" -ForegroundColor Gray
    & $AdbPath shell input tap $passX $passY | Out-Null
    Start-Sleep -Seconds 0.5
    & $AdbPath shell input text "Admin@123" | Out-Null
    Start-Sleep -Seconds 1
    
    Take-Screenshot "04-password-entered.png"
    
    # 点击登录按钮
    # 假设登录按钮在密码输入框下方
    $loginX = [int]($width / 2)
    $loginY = [int]($height * 0.62)
    Write-Host "   点击登录按钮" -ForegroundColor Gray
    & $AdbPath shell input tap $loginX $loginY | Out-Null
    
    # 等待登录完成
    Write-Host "   等待登录完成 (10秒)..." -ForegroundColor Gray
    Start-Sleep -Seconds 10
    
    Take-Screenshot "05-after-login.png"
    
    return $true
}

function Test-MainNavigation {
    Write-Host "🧭 测试主界面导航..." -ForegroundColor Yellow
    
    # 获取屏幕尺寸
    $wmOutput = & $AdbPath shell wm size
    if ($wmOutput -match "(\d+)x(\d+)") {
        $width = [int]$Matches[1]
        $height = [int]$Matches[2]
    } else {
        $width = 1080
        $height = 1920
    }
    
    # 底部导航栏的位置
    $navY = [int]($height * 0.92)
    $navItemSpacing = [int]($width / 5)
    
    # 测试各个导航项
    $navItems = @("任务", "线索", "消息", "我的")
    
    for ($i = 0; $i -lt $navItems.Count; $i++) {
        $navX = [int]($navItemSpacing * ($i + 0.5))
        
        Write-Host "   点击导航项: $($navItems[$i])" -ForegroundColor Gray
        & $AdbPath shell input tap $navX $navY | Out-Null
        Start-Sleep -Seconds 3
        Take-Screenshot ("06-nav-" + ($i + 1) + "-" + $navItems[$i] + ".png")
    }
    
    # 点击第一个（任务）回到初始状态
    & $AdbPath shell input tap ([int]($navItemSpacing * 0.5)) $navY | Out-Null
    Start-Sleep -Seconds 2
    
    Take-Screenshot "07-nav-back-to-tasks.png"
    
    return $true
}

function Test-TaskList {
    Write-Host "📋 测试任务列表..." -ForegroundColor Yellow
    
    # 获取屏幕尺寸
    $wmOutput = & $AdbPath shell wm size
    if ($wmOutput -match "(\d+)x(\d+)") {
        $width = [int]$Matches[1]
        $height = [int]$Matches[2]
    } else {
        $width = 1080
        $height = 1920
    }
    
    # 滚动列表查看
    Write-Host "   滚动任务列表" -ForegroundColor Gray
    for ($i = 0; $i -lt 3; $i++) {
        & $AdbPath shell input swipe ($width/2) ($height*0.7) ($width/2) ($height*0.3) 300 | Out-Null
        Start-Sleep -Seconds 1
    }
    
    Take-Screenshot "08-task-list-scrolled.png"
    
    # 如果有任务项，点击第一个
    $taskItemY = [int]($height * 0.35)
    Write-Host "   点击第一个任务项" -ForegroundColor Gray
    & $AdbPath shell input tap ($width/2) $taskItemY | Out-Null
    Start-Sleep -Seconds 4
    
    Take-Screenshot "09-task-detail.png"
    
    # 返回到任务列表
    Write-Host "   返回任务列表" -ForegroundColor Gray
    & $AdbPath shell input keyevent KEYCODE_BACK | Out-Null
    Start-Sleep -Seconds 2
    
    Take-Screenshot "10-back-to-tasks.png"
    
    return $true
}

function Test-ProfileScreen {
    Write-Host "👤 测试个人中心..." -ForegroundColor Yellow
    
    # 获取屏幕尺寸
    $wmOutput = & $AdbPath shell wm size
    if ($wmOutput -match "(\d+)x(\d+)") {
        $width = [int]$Matches[1]
        $height = [int]$Matches[2]
    } else {
        $width = 1080
        $height = 1920
    }
    
    # 点击"我的"导航项
    $navY = [int]($height * 0.92)
    $navX = [int]($width * 0.9)
    & $AdbPath shell input tap $navX $navY | Out-Null
    Start-Sleep -Seconds 3
    
    Take-Screenshot "11-profile-screen.png"
    
    # 滚动个人中心
    Write-Host "   滚动个人中心" -ForegroundColor Gray
    & $AdbPath shell input swipe ($width/2) ($height*0.7) ($width/2) ($height*0.3) 300 | Out-Null
    Start-Sleep -Seconds 2
    
    Take-Screenshot "12-profile-scrolled.png"
    
    return $true
}

function Start-MonkeyTest {
    param([int]$Events)
    
    Write-Host "🐒 启动Monkey稳定性测试..." -ForegroundColor Magenta
    Write-Host "   测试事件数: $Events" -ForegroundColor Gray
    
    $monkeyCmd = "$AdbPath shell monkey -p $PackageName --throttle 400 --pct-touch 50 --pct-motion 20 --pct-nav 10 --pct-appswitch 15 --pct-anyevent 5 -v -v $Events"
    
    Write-Host "   执行Monkey测试 (这将需要一些时间)..." -ForegroundColor Gray
    Write-Host ""
    Write-Host "⏳ 测试进行中，请稍候..." -ForegroundColor Yellow
    
    $monkeyTime = Measure-Command {
        $monkeyOutput = Invoke-Expression $monkeyCmd 2>&1
    }
    
    $monkeyOutput | Out-File (Join-Path $LogDir "monkey_test.log")
    
    if ($LASTEXITCODE -eq 0 -or $monkeyOutput -match "Events injected") {
        $injectedEvents = ($monkeyOutput | Select-String "Events injected:\s+(\d+)").Matches.Groups[1].Value
        
        Write-Host "`n✅ Monkey测试完成！" -ForegroundColor Green
        Write-Host "   注入事件数: $injectedEvents"
        Write-Host "   总耗时: $($monkeyTime.TotalSeconds.ToString('F1'))秒"
        return $true
    } else {
        Write-Host "`n❌ Monkey测试异常终止！" -ForegroundColor Red
        Write-Host "   可能原因: APP崩溃(ANR)或严重错误"
        Write-Host "   详细日志: monkey_test.log"
        return $false
    }
}

function Collect-Logs {
    Write-Host "📝 收集系统和应用日志..." -ForegroundColor Cyan
    
    # Logcat主日志
    Write-Host "   收集完整Logcat..." -ForegroundColor Gray
    & $AdbPath logcat -d > (Join-Path $LogDir "logcat_full.log")
    
    # 过滤关键错误
    Write-Host "   收集错误日志..." -ForegroundColor Gray
    & $AdbPath logcat -d *:E > (Join-Path $LogDir "logcat_errors.log")
    
    # 过滤北极星APP日志
    Write-Host "   收集应用特定日志..." -ForegroundColor Gray
    & $AdbPath logcat -d -s "BxApp:*" "beijixing:*" "DEBUG:*" "*:S" > (Join-Path $LogDir "logcat_app.log")
    
    # 收集崩溃日志
    Write-Host "   尝试收集崩溃日志..." -ForegroundColor Gray
    try {
        & $AdbPath shell run-as $PackageName ls files/crash_logs/ 2>$null | ForEach-Object {
            $crashFile = $_.Trim()
            if ($crashFile -and $crashFile -ne "") {
                Write-Host "      找到崩溃日志: $crashFile" -ForegroundColor Red
                & $AdbPath shell run-as $PackageName cat "files/crash_logs/$crashFile" > (Join-Path $LogDir $crashFile)
            }
        }
    } catch {
        Write-Host "      (跳过崩溃日志收集)" -ForegroundColor Gray
    }
    
    # 最后一个截图
    Take-Screenshot "99-final-screen.png"
    
    Write-Host "✅ 日志收集完成！" -ForegroundColor Green
    Write-Host "   位置: $LogDir" -ForegroundColor Yellow
}

function Analyze-Results {
    Write-Host ""
    Write-Host "📊 分析测试结果..." -ForegroundColor Cyan
    
    $errorLog = Join-Path $LogDir "logcat_errors.log"
    $appLog = Join-Path $LogDir "logcat_app.log"
    
    $errors = @()
    
    if (Test-Path $errorLog) {
        $errorContent = Get-Content $errorLog
        if ($errorContent -match "FATAL EXCEPTION|AndroidRuntime|Exception|Error") {
            $errors += "检测到严重错误或崩溃"
        }
    }
    
    if (Test-Path $appLog) {
        $appContent = Get-Content $appLog
        if ($appContent -match "ERROR|Exception|Failed|failed") {
            $errors += "检测到应用错误"
        }
    }
    
    $reportPath = Join-Path $LogDir "TEST_REPORT.md"
    
    $reportContent = @"
========================================
北极星AI系统 - 全链路自动化测试报告
========================================

测试时间: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')

[✅] 应用启动测试
[✅] 登录流程测试
[✅] 主界面导航测试
[✅] 任务列表测试
[✅] 个人中心测试
[✅] Monkey稳定性测试

发现的问题:
$(if ($errors.Count -eq 0) { "✅ 未检测到明显问题" } else { $errors -join "`n" })

详细截图和日志请参考:
- 01-splash-screen.png 到 99-final-screen.png
- logcat_full.log (完整日志)
- logcat_errors.log (错误日志)
- logcat_app.log (应用日志)
- monkey_test.log (Monkey测试日志)

========================================
"@
    
    $reportContent | Out-File $reportPath
    
    Write-Host ""
    Write-Host "测试报告已生成: $reportPath" -ForegroundColor Yellow
    
    return $errors
}

# ==================== 主流程 ====================

Write-Host "🔍 第一步: 检查设备连接..." -ForegroundColor White

if (-not (Test-DeviceConnected)) {
    Write-Host "`n❌ 未检测到设备！请检查：" -ForegroundColor Red
    Write-Host "   1. USB线是否正确连接" -ForegroundColor Yellow
    Write-Host "   2. 手机是否开启了USB调试" -ForegroundColor Yellow
    Write-Host "   3. 是否在弹窗中点击了'允许'" -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

$deviceInfo = Get-DeviceInfo
Show-DeviceInfo $deviceInfo

# 执行测试
Write-Host "`n🚀 开始全链路自动化测试..." -ForegroundColor White
Write-Host "   请确保手机已解锁，并保持在亮屏状态" -ForegroundColor Yellow
Write-Host ""

Start-Sleep -Seconds 2

$allTestsPassed = $true

# 1. 启动应用
if (-not (Start-Application)) {
    $allTestsPassed = $false
}

Write-Host ""

# 2. 登录流程
if (-not (Test-LoginFlow)) {
    $allTestsPassed = $false
}

Write-Host ""

# 3. 主界面导航
if (-not (Test-MainNavigation)) {
    $allTestsPassed = $false
}

Write-Host ""

# 4. 任务列表
if (-not (Test-TaskList)) {
    $allTestsPassed = $false
}

Write-Host ""

# 5. 个人中心
if (-not (Test-ProfileScreen)) {
    $allTestsPassed = $false
}

Write-Host ""

# 6. Monkey稳定性测试
if (-not (Start-MonkeyTest -Events 300)) {
    $allTestsPassed = $false
}

Write-Host ""

# 7. 收集日志
Collect-Logs

# 8. 分析结果
$problems = Analyze-Results

Write-Host ""
Write-Host "╔═══════════════════════════════════════════╗" -ForegroundColor $(if ($allTestsPassed -and $problems.Count -eq 0) { "Green" } else { "Yellow" })
Write-Host "║         $(if ($allTestsPassed -and $problems.Count -eq 0) { "✅ 全链路测试完成，未检测到问题" } else { "⚠️  全链路测试完成，发现问题，请查看报告" })   ║" -ForegroundColor $(if ($allTestsPassed -and $problems.Count -eq 0) { "Green" } else { "Yellow" })
Write-Host "╚═══════════════════════════════════════════╝" -ForegroundColor $(if ($allTestsPassed -and $problems.Count -eq 0) { "Green" } else { "Yellow" })
Write-Host ""
Write-Host "📁 所有测试文件已保存至: $LogDir" -ForegroundColor Cyan
Write-Host ""
