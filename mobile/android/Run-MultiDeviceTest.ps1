# 北极星AI系统 - 多机型自动化测试脚本 (PowerShell版)
# 用于在多个Android虚拟设备上运行测试

param(
    [switch]$QuickTest,  # 快速模式：只测试主设备
    [string]$DeviceName = ""  # 指定特定设备
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "🧪 北极星AI系统 - 多机型自动化测试" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# 配置路径
$AndroidHome = "$env:LOCALAPPDATA\Android\Sdk"
$AdbPath = Join-Path $AndroidHome "platform-tools\adb.exe"
$EmulatorPath = Join-Path $AndroidHome "emulator\emulator.exe"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

# 验证工具存在
if (-not (Test-Path $AdbPath)) {
    Write-Host "❌ ADB未找到: $AdbPath" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $EmulatorPath)) {
    Write-Host "❌ Emulator未找到: $EmulatorPath" -ForegroundColor Red
    exit 1
}

# 定义测试设备矩阵
$Devices = @(
    @{ Name="Pixel_6"; Api=30; Resolution="1080x2400"; Density="420dpi"; Priority="High" },
    @{ Name="Pixel_7"; Api=33; Resolution="1080x2400"; Density="420dpi"; Priority="High" },
    @{ Name="Pixel_8"; Api=34; Resolution="1080x2400"; Density="420dpi"; Priority="High" },
    @{ Name="Xiaomi_12"; Api=33; Resolution="1440x3200"; Density="560dpi"; Priority="Medium" },
    @{ Name="Samsung_S24"; Api=34; Resolution="1440x3168"; Density="550dpi"; Priority="Medium" }
)

if ($QuickTest) {
    $Devices = @($Devices[0])
    Write-Host "⚡ 快速模式：只测试 $($Devices[0].Name)" -ForegroundColor Yellow
}

if ($DeviceName) {
    $Devices = $Devices | Where-Object { $_.Name -like "*$DeviceName*" }
    if ($Devices.Count -eq 0) {
        Write-Host "❌ 未找到匹配的设备: $DeviceName" -ForegroundColor Red
        exit 1
    }
}

# 创建报告目录
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$ReportDir = Join-Path $ProjectRoot "test-reports\multi-device-$Timestamp"
New-Item -ItemType Directory -Path $ReportDir -Force | Out-Null

Write-Host "📋 测试计划:" -ForegroundColor Green
Write-Host "   - 设备数量: $($Devices.Count)"
Write-Host "   - 报告目录: $ReportDir"
Write-Host ""

$PassCount = 0
$FailCount = 0
$TotalTests = 0

foreach ($Device in $Devices) {
    $TotalTests++
    $DeviceName = $Device.Name
    
    Write-Host "------------------------------------------" -ForegroundColor Gray
    Write-Host "🔧 测试设备 #$TotalTests : $DeviceName" -ForegroundColor White
    Write-Host "   API Level: $($Device.Api)" 
    Write-Host "   分辨率: $($Device.Resolution)"
    Write-Host "   密度: $($Device.Density)"
    Write-Host "------------------------------------------" -ForegroundColor Gray
    
    # 启动模拟器
    Write-Host "🚀 启动模拟器..." -ForegroundColor Yellow
    Start-Process -FilePath $EmulatorPath -ArgumentList "-avd", $DeviceName, "-no-snapshot-load"
    
    # 等待设备启动
    Write-Host "⏳ 等待设备就绪..." -ForegroundColor Yellow
    $MaxWait = 120  # 最大等待2分钟
    $Waited = 0
    $BootComplete = "0"
    
    while ($Waited -lt $MaxWait) {
        Start-Sleep -Seconds 5
        $Waited += 5
        
        try {
            $BootComplete = & $AdbPath shell getprop sys.boot_completed 2>$null
            if ($BootComplete -eq "1") { break }
        } catch {}
        
        Write-Host "   ⏱️  已等待 ${Waited}s..."
    }
    
    if ($BootComplete -eq "1") {
        Write-Host "✅ 设备启动成功" -ForegroundColor Green
        
        # 安装APK
        $ApkPath = Join-Path $ProjectRoot "app\build\outputs\apk\release\app-release.apk"
        
        if (Test-Path $ApkPath) {
            Write-Host "📦 安装APK..." -ForegroundColor Yellow
            $InstallResult = & $AdbPath install -r $ApkPath 2>&1
            
            if ($LASTEXITCODE -eq 0) {
                Write-Host "✅ APK安装成功" -ForegroundColor Green
                
                # 运行Monkey稳定性测试
                Write-Host "🐒 运行Monkey稳定性测试 (500事件)..." -ForegroundColor Yellow
                $Package = "com.beijixing.app"
                & $AdbPath shell monkey -p $Package --throttle 300 --pct-touch 50 --pct-motion 20 --pct-nav 10 --pct-appswitch 15 --pct-anyevent 5 -v 500 2>&1 | Out-File (Join-Path $ReportDir "${DeviceName}_monkey.log")
                
                if ($LASTEXITCODE -eq 0) {
                    Write-Host "✅ Monkey测试完成" -ForegroundColor Green
                    $PassCount++
                    Set-Content -Path (Join-Path $ReportDir "${DeviceName}_result.txt") -Value "PASS"
                } else {
                    Write-Host "❌ Monkey测试失败" -ForegroundColor Red
                    $FailCount++
                    Set-Content -Path (Join-Path $ReportDir "${DeviceName}_result.txt") -Value "FAIL"
                }
                
                # 收集日志和截图
                Write-Host "📸 收集日志和截图..." -ForegroundColor Yellow
                & $AdbPath logcat -d > (Join-Path $ReportDir "${DeviceName}_logcat.log")
                & $AdbPath exec-out screencap -p > (Join-Path $ReportDir "${DeviceName}_screenshot.png")
                
            } else {
                Write-Host "❌ APK安装失败" -ForegroundColor Red
                $FailCount++
                Set-Content -Path (Join-Path $ReportDir "${DeviceName}_result.txt") -Value "INSTALL_FAIL"
            }
        } else {
            Write-Host "❌ APK文件不存在: $ApkPath" -ForegroundColor Red
            $FailCount++
            Set-Content -Path (Join-Path $ReportDir "${DeviceName}_result.txt") -Value "APK_NOT_FOUND"
        }
    } else {
        Write-Host "❌ 设备启动超时 (${MaxWait}s)" -ForegroundColor Red
        $FailCount++
        Set-Content -Path (Join-Path $ReportDir "${DeviceName}_result.txt") -Value "BOOT_TIMEOUT"
    }
    
    # 停止模拟器
    Write-Host "🛑 停止模拟器..." -ForegroundColor Gray
    & $AdbPath emu kill 2>$null
    Start-Sleep -Seconds 3
    
    Write-Host ""
}

# 生成汇总报告
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "📊 测试结果汇总" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "总测试设备数: $TotalTests" -ForegroundColor White
Write-Host "✅ 通过: $PassCount" -ForegroundColor Green
Write-Host "❌ 失败: $FailCount" -ForegroundColor Red
if ($TotalTests -gt 0) {
    $PassRate = [math]::Round(($PassCount / $TotalTests) * 100, 1)
    Write-Host "通过率: ${PassRate}%" -ForegroundColor $(if ($PassRate -ge 80) { "Green" } else { "Red" })
}
Write-Host ""
Write-Host "详细报告位置: $ReportDir" -ForegroundColor Yellow
Write-Host "==========================================" -ForegroundColor Cyan

# 输出最终结果
if ($FailCount -eq 0) {
    Write-Host "`n🎉 所有测试通过！可以发布。" -ForegroundColor Green
    exit 0
} else {
    Write-Host "`n⚠️  存在失败的测试，请检查报告后修复问题。" -ForegroundColor Yellow
    exit 1
}
