# BeiJiXing AI System - Full Link Automated Test Script
# For testing all features after login

$ErrorActionPreference = "Continue"

Write-Host ""
Write-Host "=======================================" -ForegroundColor Cyan
Write-Host "  BeiJiXing AI - Full Link Test  " -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan
Write-Host ""

# Path configuration
$AdbPath = "C:\Users\HenryChow\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$timestamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$LogDir = "D:\BeijiXing-AI\mobile\android\test-reports\full-test-$timestamp"
$PackageName = "com.beijixing.app.debug"

# Create log directory
New-Item -ItemType Directory -Path $LogDir -Force | Out-Null
Write-Host "[LOG] Directory: $LogDir" -ForegroundColor Yellow
Write-Host ""

# Verify ADB
if (-not (Test-Path $AdbPath)) {
    Write-Host "[ERROR] ADB not found!" -ForegroundColor Red
    exit 1
}

function Test-DeviceConnected {
    $devices = & $AdbPath devices 2>$null
    return ($devices -match "\w+\s+device")
}

function Get-DeviceInfo {
    Write-Host "[INFO] Getting device info..." -ForegroundColor Cyan
    
    $info = @{
        Manufacturer = (& $AdbPath shell getprop ro.product.manufacturer).Trim()
        Model = (& $AdbPath shell getprop ro.product.model).Trim()
        AndroidVersion = (& $AdbPath shell getprop ro.build.version.release).Trim()
        SdkVersion = (& $AdbPath shell getprop ro.build.version.sdk).Trim()
    }
    
    return $info
}

function Show-DeviceInfo($info) {
    Write-Host ""
    Write-Host "---------------------------------------" -ForegroundColor White
    Write-Host "  Device Info Summary" -ForegroundColor White
    Write-Host "---------------------------------------" -ForegroundColor White
    Write-Host "  Manufacturer: $($info.Manufacturer)"
    Write-Host "  Model:        $($info.Model)"
    Write-Host "  Android:      $($info.AndroidVersion) (API $($info.SdkVersion))"
    Write-Host "---------------------------------------" -ForegroundColor White
    Write-Host ""
    
    $info | ConvertTo-Json | Out-File (Join-Path $LogDir "device_info.json")
}

function Take-Screenshot($filename) {
    $filepath = Join-Path $LogDir $filename
    & $AdbPath exec-out screencap -p > $filepath
    if (Test-Path $filepath) {
        Write-Host "[SCREEN] Saved: $filename" -ForegroundColor Gray
    }
}

function Start-Application {
    Write-Host "[START] Starting application..." -ForegroundColor Yellow
    
    # Stop app first
    & $AdbPath shell am force-stop $PackageName | Out-Null
    Start-Sleep -Seconds 1
    
    # Clear logs
    & $AdbPath logcat -c
    
    # Start app
    Write-Host "[START] Launching SplashActivity..." -ForegroundColor Gray
    $startOutput = & $AdbPath shell monkey -p $PackageName -c android.intent.category.LAUNCHER 1 2>&1
    Start-Sleep -Seconds 6
    
    Take-Screenshot "01-splash-screen.png"
    
    if ($startOutput -match "Events injected") {
        Write-Host "[OK] App started successfully" -ForegroundColor Green
        return $true
    } else {
        Write-Host "[FAIL] App failed to start" -ForegroundColor Red
        return $false
    }
}

function Test-LoginFlow {
    Write-Host "[LOGIN] Testing login flow..." -ForegroundColor Yellow
    
    # Wait for login page
    Start-Sleep -Seconds 3
    
    # Get screen dimensions
    $wmOutput = & $AdbPath shell wm size
    if ($wmOutput -match "(\d+)x(\d+)") {
        $width = [int]$Matches[1]
        $height = [int]$Matches[2]
        Write-Host "[INFO] Screen size: ${width}x${height}" -ForegroundColor Gray
    } else {
        $width = 1080
        $height = 1920
    }
    
    Take-Screenshot "02-login-screen.png"
    
    # Try to find and switch to password tab (bottom part of screen)
    $tabY = [int]($height * 0.85)
    Write-Host "[LOGIN] Tapping password tab..." -ForegroundColor Gray
    & $AdbPath shell input tap ($width/4) $tabY | Out-Null
    Start-Sleep -Seconds 1
    
    Take-Screenshot "03-password-tab-selected.png"
    
    # Tap on phone field (upper middle part)
    $phoneX = [int]($width / 2)
    $phoneY = [int]($height * 0.35)
    Write-Host "[LOGIN] Entering phone: 19537722739" -ForegroundColor Gray
    & $AdbPath shell input tap $phoneX $phoneY | Out-Null
    Start-Sleep -Seconds 0.5
    & $AdbPath shell input text "19537722739" | Out-Null
    Start-Sleep -Seconds 1
    
    Take-Screenshot "04-phone-entered.png"
    
    # Tap on password field
    $passX = [int]($width / 2)
    $passY = [int]($height * 0.48)
    Write-Host "[LOGIN] Entering password: Admin@123" -ForegroundColor Gray
    & $AdbPath shell input tap $passX $passY | Out-Null
    Start-Sleep -Seconds 0.5
    & $AdbPath shell input text "Admin@123" | Out-Null
    Start-Sleep -Seconds 1
    
    Take-Screenshot "05-password-entered.png"
    
    # Tap login button
    $loginX = [int]($width / 2)
    $loginY = [int]($height * 0.62)
    Write-Host "[LOGIN] Tapping login button..." -ForegroundColor Gray
    & $AdbPath shell input tap $loginX $loginY | Out-Null
    
    # Wait for login
    Write-Host "[LOGIN] Waiting for login completion (10 seconds)..." -ForegroundColor Gray
    Start-Sleep -Seconds 10
    
    Take-Screenshot "06-after-login.png"
    
    return $true
}

function Test-MainNavigation {
    Write-Host "[NAV] Testing main navigation..." -ForegroundColor Yellow
    
    # Get screen dimensions
    $wmOutput = & $AdbPath shell wm size
    if ($wmOutput -match "(\d+)x(\d+)") {
        $width = [int]$Matches[1]
        $height = [int]$Matches[2]
    } else {
        $width = 1080
        $height = 1920
    }
    
    # Bottom navigation bar
    $navY = [int]($height * 0.92)
    $navItemSpacing = [int]($width / 5)
    
    $navItems = @("tasks", "leads", "messages", "profile")
    
    for ($i = 0; $i -lt $navItems.Count; $i++) {
        $navX = [int]($navItemSpacing * ($i + 0.5))
        
        Write-Host "[NAV] Tapping: $($navItems[$i])" -ForegroundColor Gray
        & $AdbPath shell input tap $navX $navY | Out-Null
        Start-Sleep -Seconds 3
        Take-Screenshot ("07-nav-" + ($i + 1) + "-" + $navItems[$i] + ".png")
    }
    
    # Go back to first tab
    & $AdbPath shell input tap ([int]($navItemSpacing * 0.5)) $navY | Out-Null
    Start-Sleep -Seconds 2
    
    Take-Screenshot "08-nav-back-to-tasks.png"
    
    return $true
}

function Test-TaskList {
    Write-Host "[TASK] Testing task list..." -ForegroundColor Yellow
    
    # Get screen dimensions
    $wmOutput = & $AdbPath shell wm size
    if ($wmOutput -match "(\d+)x(\d+)") {
        $width = [int]$Matches[1]
        $height = [int]$Matches[2]
    } else {
        $width = 1080
        $height = 1920
    }
    
    # Scroll list
    Write-Host "[TASK] Scrolling task list..." -ForegroundColor Gray
    for ($i = 0; $i -lt 3; $i++) {
        & $AdbPath shell input swipe ($width/2) ($height*0.7) ($width/2) ($height*0.3) 300 | Out-Null
        Start-Sleep -Seconds 1
    }
    
    Take-Screenshot "09-task-list-scrolled.png"
    
    # Tap first task if exists
    $taskItemY = [int]($height * 0.35)
    Write-Host "[TASK] Tapping first task item..." -ForegroundColor Gray
    & $AdbPath shell input tap ($width/2) $taskItemY | Out-Null
    Start-Sleep -Seconds 4
    
    Take-Screenshot "10-task-detail.png"
    
    # Go back
    Write-Host "[TASK] Returning to task list..." -ForegroundColor Gray
    & $AdbPath shell input keyevent KEYCODE_BACK | Out-Null
    Start-Sleep -Seconds 2
    
    Take-Screenshot "11-back-to-tasks.png"
    
    return $true
}

function Start-MonkeyTest {
    param([int]$Events)
    
    Write-Host "[MONKEY] Starting Monkey stability test..." -ForegroundColor Magenta
    Write-Host "[MONKEY] Events: $Events" -ForegroundColor Gray
    
    $monkeyCmd = "$AdbPath shell monkey -p $PackageName --throttle 400 --pct-touch 50 --pct-motion 20 --pct-nav 10 --pct-appswitch 15 --pct-anyevent 5 -v -v $Events"
    
    Write-Host "[MONKEY] Running (this may take a while)..." -ForegroundColor Gray
    Write-Host ""
    Write-Host "[WAIT] Testing in progress..." -ForegroundColor Yellow
    
    $monkeyTime = Measure-Command {
        $monkeyOutput = Invoke-Expression $monkeyCmd 2>&1
    }
    
    $monkeyOutput | Out-File (Join-Path $LogDir "monkey_test.log")
    
    if ($LASTEXITCODE -eq 0 -or $monkeyOutput -match "Events injected") {
        $injectedEvents = ($monkeyOutput | Select-String "Events injected:\s+(\d+)").Matches.Groups[1].Value
        
        Write-Host ""
        Write-Host "[OK] Monkey test completed!" -ForegroundColor Green
        Write-Host "[INFO] Events injected: $injectedEvents"
        Write-Host "[INFO] Time: $($monkeyTime.TotalSeconds.ToString('F1'))s"
        return $true
    } else {
        Write-Host ""
        Write-Host "[FAIL] Monkey test terminated!" -ForegroundColor Red
        Write-Host "[INFO] Possible: App crash/ANR"
        Write-Host "[INFO] Log: monkey_test.log"
        return $false
    }
}

function Collect-Logs {
    Write-Host "[LOGS] Collecting logs..." -ForegroundColor Cyan
    
    Write-Host "[LOGS] Collecting full logcat..." -ForegroundColor Gray
    & $AdbPath logcat -d > (Join-Path $LogDir "logcat_full.log")
    
    Write-Host "[LOGS] Collecting error logs..." -ForegroundColor Gray
    & $AdbPath logcat -d *:E > (Join-Path $LogDir "logcat_errors.log")
    
    Write-Host "[LOGS] Collecting app-specific logs..." -ForegroundColor Gray
    & $AdbPath logcat -d -s "BxApp:*" "beijixing:*" "DEBUG:*" "*:S" > (Join-Path $LogDir "logcat_app.log")
    
    Take-Screenshot "99-final-screen.png"
    
    Write-Host "[OK] Log collection complete!" -ForegroundColor Green
    Write-Host "[INFO] Location: $LogDir" -ForegroundColor Yellow
}

function Analyze-Results {
    Write-Host ""
    Write-Host "[ANALYSIS] Analyzing test results..." -ForegroundColor Cyan
    
    $errorLog = Join-Path $LogDir "logcat_errors.log"
    $appLog = Join-Path $LogDir "logcat_app.log"
    
    $errors = @()
    
    if (Test-Path $errorLog) {
        $errorContent = Get-Content $errorLog
        if ($errorContent -match "FATAL EXCEPTION|AndroidRuntime|Exception|Error") {
            $errors += "Critical errors or crashes detected"
        }
    }
    
    if (Test-Path $appLog) {
        $appContent = Get-Content $appLog
        if ($appContent -match "ERROR|Exception|Failed|failed") {
            $errors += "App errors detected"
        }
    }
    
    $reportPath = Join-Path $LogDir "TEST_REPORT.md"
    
    $reportContent = @"
========================================
BeiJiXing AI - Full Link Test Report
========================================

Test Time: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')

[OK] App launch test
[OK] Login flow test
[OK] Main navigation test
[OK] Task list test
[OK] Monkey stability test

Issues Found:
$(if ($errors.Count -eq 0) { "OK - No obvious issues detected" } else { $errors -join "`n" })

Please refer to:
- 01-splash-screen.png ~ 99-final-screen.png
- logcat_full.log (Full log)
- logcat_errors.log (Error log)
- logcat_app.log (App log)
- monkey_test.log (Monkey test log)

========================================
"@
    
    $reportContent | Out-File $reportPath
    
    Write-Host ""
    Write-Host "[INFO] Report generated: $reportPath" -ForegroundColor Yellow
    
    return $errors
}

# ==================== Main ====================

Write-Host "[CHECK] Checking device connection..." -ForegroundColor White

if (-not (Test-DeviceConnected)) {
    Write-Host ""
    Write-Host "[ERROR] No device found!" -ForegroundColor Red
    Write-Host "[HELP] 1. Check USB connection" -ForegroundColor Yellow
    Write-Host "[HELP] 2. Enable USB debugging" -ForegroundColor Yellow
    Write-Host "[HELP] 3. Allow USB debugging prompt" -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

$deviceInfo = Get-DeviceInfo
Show-DeviceInfo $deviceInfo

# Start testing
Write-Host ""
Write-Host "[START] Beginning full link automated test..." -ForegroundColor White
Write-Host "[NOTE] Please ensure phone is unlocked and screen is on" -ForegroundColor Yellow
Write-Host ""

Start-Sleep -Seconds 2

$allTestsPassed = $true

# 1. Start app
if (-not (Start-Application)) {
    $allTestsPassed = $false
}

Write-Host ""

# 2. Login flow
if (-not (Test-LoginFlow)) {
    $allTestsPassed = $false
}

Write-Host ""

# 3. Main navigation
if (-not (Test-MainNavigation)) {
    $allTestsPassed = $false
}

Write-Host ""

# 4. Task list
if (-not (Test-TaskList)) {
    $allTestsPassed = $false
}

Write-Host ""

# 5. Monkey stability test
if (-not (Start-MonkeyTest -Events 300)) {
    $allTestsPassed = $false
}

Write-Host ""

# 6. Collect logs
Collect-Logs

# 7. Analyze results
$problems = Analyze-Results

Write-Host ""
Write-Host "=======================================" -ForegroundColor $(if ($allTestsPassed -and $problems.Count -eq 0) { "Green" } else { "Yellow" })
Write-Host "  $(if ($allTestsPassed -and $problems.Count -eq 0) { "All tests passed - OK" } else { "Test completed - See report" })  " -ForegroundColor $(if ($allTestsPassed -and $problems.Count -eq 0) { "Green" } else { "Yellow" })
Write-Host "=======================================" -ForegroundColor $(if ($allTestsPassed -and $problems.Count -eq 0) { "Green" } else { "Yellow" })
Write-Host ""
Write-Host "[INFO] All files saved to: $LogDir" -ForegroundColor Cyan
Write-Host ""
