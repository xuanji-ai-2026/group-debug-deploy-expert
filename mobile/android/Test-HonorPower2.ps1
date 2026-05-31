# 荣耀POWER 2 自动化测试脚本
# 用于自动化安装、测试、收集日志

param(
    [switch]$InstallOnly,     # 只安装不测试
    [switch]$TestOnly,        # 只测试（假设已安装）
    [switch]$CollectLogs,     # 只收集日志
    [int]$MonkeyEvents = 500  # Monkey测试事件数
)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "╔══════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║   荣耀POWER 2 (MagicOS) 专项测试工具    ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# 路径配置
$AdbPath = "C:\Users\HenryChow\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$ApkPath = "D:\BeijiXing-AI\mobile\android\app\build\outputs\apk\release\app-release.apk"
$LogDir = "D:\BeijiXing-AI\mobile\android\test-reports\honor-power2-$(Get-Date -Format 'yyyyMMdd_HHmmss')"
$PackageName = "com.beijixing.app"

# 创建日志目录
New-Item -ItemType Directory -Path $LogDir -Force | Out-Null
Write-Host "📁 日志目录: $LogDir" -ForegroundColor Yellow
Write-Host ""

# 验证ADB
if (-not (Test-Path $AdbPath)) {
    Write-Host "❌ ADB未找到!" -ForegroundColor Red
    exit 1
}

# 验证APK
if (-not (Test-Path $ApkPath) -and -not $TestOnly -and -not $CollectLogs) {
    Write-Host "❌ APK未找到: $ApkPath" -ForegroundColor Red
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
        RomVersion = (& $AdbPath shell getprop ro.build.display.id).Trim()
        Resolution = (& $AdbPath shell wm size 2>$null) -replace "Physical size: ", ""
        Density = (& $AdbPath shell wm density 2>$null) -replace "Physical density: ", ""
        CPU = (& $AdbPath shell getprop ro.product.cpu.abi).Trim()
        Memory = [math]::Round((& $AdbPath shell cat /proc/meminfo 2>$null | Select-String "MemTotal") -replace "[^\d]", "" / 1024 / 1024, 0)
        Storage = [math]::Round((& $AdbPath shell df /data 2>$null | Select-Object -Last 1) -split "\s+")[3] / 1024 / 1024, 0
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
    Write-Host ("│ ROM版本: {0,-25} │" -f $info.RomVersion)
    Write-Host ("│ 分辨率: {0,-26} │" -f $info.Resolution)
    Write-Host ("│ 屏幕密度: {0,-24} │" -f $info.Density)
    Write-Host ("│ CPU架构: {0,-24} │" -f $info.CPU)
    Write-Host ("│ 内存: {0} GB{1,$(21-$info.Memory.ToString().Length)} │" -f $info.Memory)
    Write-Host ("│ 可用存储: {0} GB{0,$(19-$info.Storage.ToString().Length)} │" -f $info.Storage)
    Write-Host "└─────────────────────────────────────┘" -ForegroundColor White
    Write-Host ""
    
    # 保存到文件
    $info | ConvertTo-Json | Out-File (Join-Path $LogDir "device_info.json")
}

function Install-APK {
    Write-Host "📦 开始安装APK..." -ForegroundColor Yellow
    
    # 先卸载旧版本（如果存在）
    & $AdbPath shell pm list packages 2>$null | Select-String $PackageName
    if ($?) {
        Write-Host "🗑️  发现旧版本，正在卸载..." -ForegroundColor Gray
        & $AdbPath uninstall $PackageName 2>$null | Out-Null
        Start-Sleep -Seconds 2
    }
    
    # 安装新版本
    Write-Host "⏳ 正在安装... (这可能需要30秒-2分钟)" -ForegroundColor Yellow
    $installOutput = & $AdbPath install -r -d $ApkPath 2>&1
    $installTime = Measure-Command { $installOutput }
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ APK安装成功！" -ForegroundColor Green
        Write-Host "   ⏱️  耗时: $($installTime.TotalSeconds.ToString('F1'))秒"
        
        # 记录安装信息
        "安装时间: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" | Out-File (Join-Path $LogDir "install.log")
        $ApkSize = (Get-Item $ApkPath).Length / 1MB
        "APK大小: ([math]::Round($ApkSize, 2)) MB" | Out-File (Join-Path $LogDir "install.log") -Append
        
        return $true
    } else {
        Write-Host "❌ APK安装失败！" -ForegroundColor Red
        Write-Host "   错误信息: $installOutput"
        $installOutput | Out-File (Join-Path $LogDir "install_error.log")
        return $false
    }
}

function Start-MonkeyTest {
    param([int]$Events)
    
    Write-Host "🐒 启动Monkey稳定性测试..." -ForegroundColor Magenta
    Write-Host "   测试事件数: $Events" -ForegroundColor Gray
    
    # Monkey参数说明:
    # --throttle 300: 每个事件间隔300ms
    # --pct-touch 50: 触摸事件50%
    # --pct-motion 20: 手势事件20%
    # --pct-nav 10: 导航事件10%
    # --pct-appswitch 15: Activity切换15%
    # --pct-anyevent 5: 其他事件5%
    
    $monkeyCmd = "$AdbPath shell monkey -p $PackageName --throttle 300 --pct-touch 50 --pct-motion 20 --pct-nav 10 --pct-appswitch 15 --pct-anyevent 5 -v $Events"
    
    Write-Host "   执行命令: monkey -p $PackageName ... -v $Events" -ForegroundColor DarkGray
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
        Write-Host "   平均每事件: $(($monkeyTime.TotalSeconds / [int]$injectedEvents).ToString('F2'))秒"
        return $true
    } else {
        Write-Host "`n❌ Monkey测试异常终止！" -ForegroundColor Red
        Write-Host "   可能原因: APP崩溃(ANR)或严重错误"
        return $false
    }
}

function Collect-Logs {
    Write-Host "📝 收集系统和应用日志..." -ForegroundColor Cyan
    
    # Logcat主日志
    Write-Host "   收集Logcat..." -ForegroundColor Gray
    & $AdbPath logcat -d > (Join-Path $LogDir "logcat_full.log")
    
    # 过滤关键错误
    Write-Host "   过滤崩溃日志..." -ForegroundColor Gray
    & $AdbPath logcat -d *:E > (Join-Path $LogDir "logcat_errors.log")
    
    # 过滤北极星APP日志
    & $AdbPath logcat -d -s "beijixing:*" "*:S" > (Join-Path $LogDir "logcat_app.log")
    
    # CrashLogger记录的崩溃日志（如果有）
    Write-Host "   收集CrashLogger日志..." -ForegroundColor Gray
    & $AdbPath shell run-as $PackageName ls files/crash_logs/ 2>$null | ForEach-Object {
        $crashFile = $_.Trim()
        if ($crashFile) {
            & $AdbPath shell run-as $PackageName cat "files/crash_logs/$crashFile" > (Join-Path $LogDir $crashFile)
        }
    }
    
    # 截图
    Write-Host "   截取当前屏幕..." -ForegroundColor Gray
    & $AdbPath exec-out screencap -p > (Join-Path $LogDir "screenshot_current.png")
    
    # 系统信息
    Write-Host "   收集系统属性..." -ForegroundColor Gray
    & $AdbPath shell getprop > (Join-Path $LogDir "system_properties.txt")
    
    # 已安装包列表
    & $AdbPath shell pm list packages -3 > (Join-Path $LogDir "installed_packages.txt")
    
    Write-Host "✅ 日志收集完成！" -ForegroundColor Green
    Write-Host "   位置: $LogDir" -ForegroundColor Yellow
}

function Run-LaunchTest {
    Write-Host "🚀 测试APP启动性能..." -ForegroundColor Yellow
    
    # 强制停止APP
    & $AdbPath shell am force-stop $PackageName | Out-Null
    Start-Sleep -Seconds 1
    
    # 冷启动测试
    Write-Host "   执行冷启动测试..." -ForegroundColor Gray
    $coldStartOutput = & $AdbPath shell am start -W -n $PackageName/.ui.login.LoginActivity 2>&1
    $coldStartOutput | Out-File (Join-Path $LogDir "cold_start_test.log")
    
    if ($coldStartOutput -match "TotalTime:\s+(\d+)") {
        $coldStartTime = [int]$Matches[1]
        Write-Host "   ⏱️  冷启动时间: ${coldStartTime}ms" -ForegroundColor $(if ($coldStartTime -lt 2000) { "Green" } elseif ($coldStartTime -lt 4000) { "Yellow" } else { "Red" })
    }
    
    Start-Sleep -Seconds 3
    
    # 热启动测试
    Write-Host "   执行热启动测试..." -ForegroundColor Gray
    & $AdbPath shell input keyevent KEYCODE_HOME | Out-Null
    Start-Sleep -Seconds 1
    
    $hotStartOutput = & $AdbPath shell am start -W -n $PackageName/.ui.login.LoginActivity 2>&1
    $hotStartOutput | Out-File (Join-Path $LogDir "hot_start_test.log")
    
    if ($hotStartOutput -match "TotalTime:\s+(\d+)") {
        $hotStartTime = [int]$Matches[1]
        Write-Host "   ⏱️  热启动时间: ${hotStartTime}ms" -ForegroundColor $(if ($hotStartTime -lt 500) { "Green" } elseif ($hotStartTime -lt 1000) { "Yellow" } else { "Red" })
    }
}

# ==================== 主流程 ====================

Write-Host "🔍 第一步: 检查设备连接..." -ForegroundColor White

if (-not (Test-DeviceConnected)) {
    Write-Host "`n❌ 未检测到设备！请检查：" -ForegroundColor Red
    Write-Host "   1. USB线是否正确连接" -ForegroundColor Yellow
    Write-Host "   2. 手机是否开启了USB调试" -ForegroundColor Yellow
    Write-Host "   3. 是否在弹窗中点击了'允许'" -ForegroundColor Yellow
    Write-Host "   4. USB模式是否为'文件传输(MTP)'" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "💡 提示: 荣耀手机可能需要额外开启'仅充电模式下允许ADB调试'" -ForegroundColor Cyan
    exit 1
}

$deviceInfo = Get-DeviceInfo
Show-DeviceInfo $deviceInfo

# 特殊检查：荣耀/MagicOS兼容性
Write-Host "🔍 执行荣耀/MagicOS专项检查..." -ForegroundColor Magenta

$magicOSIssues = @()

# 检查存储权限
$storagePermission = & $AdbPath shell dumpsys package $PackageName 2>$null | Select-String "android.permission.WRITE_EXTERNAL_STORAGE: granted=(\w+)"
if ($storagePermission -and $storagePermission.Matches.Groups[1].Value -ne "true") {
    $magicOSIssues += "存储权限可能需要用户手动授权"
}

# 检查包查询权限（Android 11+）
if ([int]$deviceInfo.SdkVersion -ge 30) {
    $queryAllPackages = & $AdbPath shell dumpsys package $PackageName 2>$null | Select-String "android.permission.QUERY_ALL_PACKAGES: granted=(\w+)"
    if (-not $queryAllPackages -or $queryAllPackages.Matches.Groups[1].Value -ne "true") {
        $magicOSIssues += "Android 11+需要QUERY_ALL_PACKAGES权限以调用第三方APP"
    }
}

if ($magicOSIssues.Count -gt 0) {
    Write-Host "⚠️  发现潜在的兼容性问题:" -ForegroundColor Yellow
    $magicOSIssues | ForEach-Object { Write-Host "   • $_" -ForegroundColor Yellow }
    Write-Host ""
}

# 根据参数执行相应操作
if (-not $TestOnly -and -not $CollectLogs) {
    $installSuccess = Install-APK
    if (-not $installSuccess) {
        exit 1
    }
}

if (-not $CollectLogs) {
    Run-LaunchTest
    Write-Host ""
    
    if (-not $InstallOnly) {
        $monkeySuccess = Start-MonkeyTest -Events $MonkeyEvents
        Write-Host ""
    }
}

Collect-Logs

# 生成测试报告
Write-Host "`n📊 生成测试报告..." -ForegroundColor Cyan

$reportContent = @"
========================================
北极星AI系统 v1.0.6 - 荣耀POWER 2 测试报告
========================================

测试时间: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
测试设备: $($deviceInfo.Manufacturer) $($deviceInfo.Model)
系统版本: Android $($deviceInfo.AndroidVersion) (API $($deviceInfo.SdkVersion))
ROM版本: $($deviceInfo.RomVersion)
屏幕规格: $($deviceInfo.Resolution) @ $($deviceInfo.Density)

【安装结果】
$(if(-not $TestOnly -and -not $CollectLogs){if($installSuccess){'✅ 成功'}else{'❌ 失败'}}else{'⏭️ 跳过'})

【启动性能】
冷启动: $(if(Test-Path (Join-Path $LogDir 'cold_start_test.log')){(Get-Content (Join-Path $LogDir 'cold_start_test.log') | Select-String 'TotalTime').Line}else{'未测试'})
热启动: $(if(Test-Path (Join-Path $LogDir 'hot_start_test.log')){(Get-Content (Join-Path $LogDir 'hot_start_test.log') | Select-String 'TotalTime').Line}else{'未测试'})

【Monkey稳定性】
$(if(-not $InstallOnly){if($monkeySuccess){'✅ 通过'}else{'❌ 失败'}}else{'⏭️ 跳过'})

【兼容性问题】
$(if($magicOSIssues.Count -gt 0){$magicOSIssues -join "`n"}else{'✅ 无明显问题'})

【日志位置】
$LogDir

========================================
"@

$reportContent | Out-File (Join-Path $LogDir "TEST_REPORT.md")

Write-Host "`n╔══════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║          ✅ 测试流程全部完成！            ║" -ForegroundColor Green
Write-Host "╠══════════════════════════════════════════╣" -ForegroundColor Green
Write-Host "║  详细报告: TEST_REPORT.md                  ║" -ForegroundColor Green
Write-Host "║  日志目录: test-reports/honor-power2-*     ║" -ForegroundColor Green
Write-Host "╚══════════════════════════════════════════╝" -ForegroundColor Green
