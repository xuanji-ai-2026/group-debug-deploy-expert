# ============================================
# BeiJiXing AI - Deep Link Test
# ============================================
# Full depth testing of all features

$ErrorActionPreference = "Continue"

# ============================================
# Configuration
# ============================================
$AdbPath = "C:\Users\HenryChow\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$PackageName = "com.beijixing.app.debug"
$TestPhone = "19537722739"
$TestPassword = "Admin@123"

# ============================================
# Timestamp and Directories
# ============================================
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$TestDir = "d:\BeijiXing-AI\mobile\android\test-reports\deep-test-$Timestamp"
New-Item -ItemType Directory -Path $TestDir -Force | Out-Null

Write-Host "============================================" -ForegroundColor Cyan
Write-Host " BeiJiXing AI - Deep Full Link Test" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Test Directory: $TestDir" -ForegroundColor Gray
Write-Host ""

# ============================================
# Helper Functions
# ============================================
function Write-Log {
    param([string]$Message, [string]$Level = "INFO")
    $Color = switch($Level) { "ERROR" { "Red" } "WARN" { "Yellow" } "PASS" { "Green" } default { "Gray" } }
    $Time = Get-Date -Format "HH:mm:ss"
    Write-Host "[$Time] [$Level] $Message" -ForegroundColor $Color
    "[$Time] [$Level] $Message" | Out-File -Append "$TestDir\test.log"
}

function Take-Screenshot {
    param([string]$Name)
    Start-Sleep -Milliseconds 800
    & $AdbPath shell screencap -p /sdcard/screen.png
    & $AdbPath pull /sdcard/screen.png "$TestDir\$Name.png" 2>$null
    & $AdbPath shell rm /sdcard/screen.png
}

function Tap {
    param([int]$X, [int]$Y)
    & $AdbPath shell input tap $X $Y
    Start-Sleep -Milliseconds 500
}

function Swipe {
    param([int]$X1, [int]$Y1, [int]$X2, [int]$Y2, [int]$Duration = 300)
    & $AdbPath shell input swipe $X1 $Y1 $X2 $Y2 $Duration
    Start-Sleep -Milliseconds 500
}

function Input-Text {
    param([string]$Text)
    & $AdbPath shell input text $Text
    Start-Sleep -Milliseconds 300
}

function Press-Back {
    & $AdbPath shell input keyevent KEYCODE_BACK
    Start-Sleep -Milliseconds 800
}

function Press-Home {
    & $AdbPath shell input keyevent KEYCODE_HOME
    Start-Sleep -Milliseconds 500
}

function Get-ScreenSize {
    $wmOutput = & $AdbPath shell wm size
    if ($wmOutput -match "(\d+)x(\d+)") {
        return [int]$Matches[1], [int]$Matches[2]
    }
    return 1080, 2340
}

function Clear-Logs {
    & $AdbPath logcat -c
}

function Save-Logs {
    param([string]$Name)
    & $AdbPath logcat -d > "$TestDir\$Name.log"
    & $AdbPath logcat -d *:E > "$TestDir\$Name-errors.log"
}

# ============================================
# Test: 1. Check Device
# ============================================
Write-Log "Checking device connection..."
$Devices = & $AdbPath devices
if (-not $Devices -match "device$") {
    Write-Log "No device connected!" "ERROR"
    exit 1
}
$Width, $Height = Get-ScreenSize
Write-Log "Device: $Width x $Height"
Take-Screenshot "01-start-device"

# ============================================
# Test: 2. App Launch and Login
# ============================================
Write-Log "=== Step 2: App Launch & Login ===" "INFO"

& $AdbPath shell am force-stop $PackageName
Start-Sleep -Seconds 2
& $AdbPath shell am start -n "$PackageName/com.beijixing.app.ui.splash.SplashActivity"
Start-Sleep -Seconds 4
Take-Screenshot "02-splash-screen"

# Wait for login screen
Start-Sleep -Seconds 3
Take-Screenshot "03-login-screen"

# Tap password tab (assume position)
$TabX = [int]($Width * 0.25)
$TabY = [int]($Height * 0.18)
Tap $TabX $TabY
Take-Screenshot "04-password-tab"

# Enter phone
$PhoneX = [int]($Width * 0.5)
$PhoneY = [int]($Height * 0.30)
Tap $PhoneX $PhoneY
Input-Text $TestPhone
Take-Screenshot "05-phone-entered"

# Enter password
$PwdX = [int]($Width * 0.5)
$PwdY = [int]($Height * 0.42)
Tap $PwdX $PwdY
Input-Text $TestPassword
Take-Screenshot "06-password-entered"

# Tap login button
$LoginX = [int]($Width * 0.5)
$LoginY = [int]($Height * 0.55)
Tap $LoginX $LoginY
Start-Sleep -Seconds 5
Take-Screenshot "07-after-login"
Save-Logs "07-login"

# ============================================
# Test: 3. Main Navigation (Bottom Tabs)
# ============================================
Write-Log "=== Step 3: Main Navigation ===" "INFO"
$Tab1X = [int]($Width * 0.1)
$Tab2X = [int]($Width * 0.3)
$Tab3X = [int]($Width * 0.5)
$Tab4X = [int]($Width * 0.7)
$Tab5X = [int]($Width * 0.9)
$TabY = [int]($Height * 0.96)

# Test Tab 1 (Tasks)
Tap $Tab1X $TabY
Start-Sleep -Seconds 2
Take-Screenshot "08-tab1-tasks"
Save-Logs "08-tab1"

# Test Tab 2 (Leads)
Tap $Tab2X $TabY
Start-Sleep -Seconds 2
Take-Screenshot "09-tab2-leads"
Save-Logs "09-tab2"

# Test Tab 3 (Center)
Tap $Tab3X $TabY
Start-Sleep -Seconds 2
Take-Screenshot "10-tab3-center"
Save-Logs "10-tab3"

# Test Tab 4 (Messages)
Tap $Tab4X $TabY
Start-Sleep -Seconds 2
Take-Screenshot "11-tab4-messages"
Save-Logs "11-tab4"

# Test Tab 5 (Profile)
Tap $Tab5X $TabY
Start-Sleep -Seconds 2
Take-Screenshot "12-tab5-profile"
Save-Logs "12-tab5"

# ============================================
# Test: 4. Tasks Tab Deep Dive
# ============================================
Write-Log "=== Step 4: Tasks Deep Dive ===" "INFO"
Tap $Tab1X $TabY
Start-Sleep -Seconds 2
Take-Screenshot "13-tasks-main"

# Scroll list
Swipe ($Width/2) ($Height*0.7) ($Width/2) ($Height*0.3)
Take-Screenshot "14-tasks-scrolled"

# Try to click task item
$TaskItemX = [int]($Width * 0.5)
$TaskItemY = [int]($Height * 0.4)
Tap $TaskItemX $TaskItemY
Start-Sleep -Seconds 3
Take-Screenshot "15-task-detail"
Save-Logs "15-task-detail"

# Press back
Press-Back
Take-Screenshot "16-back-to-tasks"

# Try FAB/Add button (if exists)
$FabX = [int]($Width * 0.85)
$FabY = [int]($Height * 0.85)
Tap $FabX $FabY
Start-Sleep -Seconds 2
Take-Screenshot "17-add-task-screen"
Save-Logs "17-add-task"

# Press back
Press-Back
Take-Screenshot "18-back-from-add"

# ============================================
# Test: 5. Leads Tab Deep Dive
# ============================================
Write-Log "=== Step 5: Leads Deep Dive ===" "INFO"
Tap $Tab2X $TabY
Start-Sleep -Seconds 2
Take-Screenshot "19-leads-main"

# Scroll list
Swipe ($Width/2) ($Height*0.7) ($Width/2) ($Height*0.3)
Take-Screenshot "20-leads-scrolled"

# Try to click lead item
Tap $TaskItemX $TaskItemY
Start-Sleep -Seconds 3
Take-Screenshot "21-lead-detail"
Save-Logs "21-lead-detail"

# Press back
Press-Back
Take-Screenshot "22-back-to-leads"

# ============================================
# Test: 6. Profile Tab Deep Dive
# ============================================
Write-Log "=== Step 6: Profile Deep Dive ===" "INFO"
Tap $Tab5X $TabY
Start-Sleep -Seconds 2
Take-Screenshot "23-profile-main"

# Test menu items in profile (tap various positions)
$Menu1Y = [int]($Height * 0.35)
$Menu2Y = [int]($Height * 0.45)
$Menu3Y = [int]($Height * 0.55)
$Menu4Y = [int]($Height * 0.65)
$MenuX = [int]($Width * 0.5)

@($Menu1Y, $Menu2Y, $Menu3Y, $Menu4Y) | ForEach-Object -Begin { $i = 24 } -Process {
    Tap $MenuX $_
    Start-Sleep -Seconds 2
    Take-Screenshot "$($i)-profile-menu-$($_)"
    Save-Logs "$($i)-menu"
    Press-Back
    $i++
}

# ============================================
# Test: 7. Try Direct Activity Launches
# ============================================
Write-Log "=== Step 7: Direct Activity Testing ===" "INFO"

$Activities = @(
    @{Name="TaskList"; Activity="com.beijixing.app.ui.task.TaskActivity"},
    @{Name="LeadList"; Activity="com.beijixing.app.ui.lead.LeadListActivity"},
    @{Name="Account"; Activity="com.beijixing.app.ui.account.AccountActivity"},
    @{Name="Recharge"; Activity="com.beijixing.app.ui.recharge.RechargeActivity"},
    @{Name="CrawlManage"; Activity="com.beijixing.app.ui.crawl.CrawlManagementActivity"},
    @{Name="AcquireTask"; Activity="com.beijixing.app.ui.acquire.AcquireTaskActivity"},
    @{Name="InterceptTask"; Activity="com.beijixing.app.ui.intercept.InterceptTaskActivity"}
)

$idx = 40
foreach ($act in $Activities) {
    Write-Log "Testing: $($act.Name)"
    try {
        & $AdbPath shell am start -n "$PackageName/$($act.Activity)"
        Start-Sleep -Seconds 3
        Take-Screenshot "$($idx)-activity-$($act.Name)"
        Save-Logs "$($idx)-$($act.Name)"
        Press-Back
        Start-Sleep -Seconds 1
    } catch {
        Write-Log "Failed to launch $($act.Name)" "WARN"
    }
    $idx++
}

# ============================================
# Test: 8. Go back to main and Monkey Test
# ============================================
Write-Log "=== Step 8: Stability Test ===" "INFO"
& $AdbPath shell am start -n "$PackageName/com.beijixing.app.ui.main.MainActivity"
Start-Sleep -Seconds 3
Take-Screenshot "60-back-to-main"

Write-Log "Running Monkey test (500 events)..."
& $AdbPath shell monkey -p $PackageName -v 500 > "$TestDir\monkey.log" 2>&1
Start-Sleep -Seconds 10
Take-Screenshot "61-after-monkey"
Save-Logs "61-monkey"

# ============================================
# Final: Save everything
# ============================================
Write-Log "=== Final: Complete ===" "INFO"
Save-Logs "99-final"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host " TEST COMPLETED!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Report at: $TestDir" -ForegroundColor Gray
Write-Host ""
