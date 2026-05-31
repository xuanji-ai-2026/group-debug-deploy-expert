# ============================================================
#  check-server-consistency.ps1
#  北极星AI - 远程服务器一致性自动校验脚本
#  用途: 对比本地D:\BeijiXing-AI与远程服务器的关键文件
#  前提: 需要SSH客户端 (Windows 10+ 已内置)
# ============================================================

<#
.SYNOPSIS
    校验本地与远程服务器文件一致性

.DESCRIPTION
    对比北极星AI项目的本地代码与远程生产服务器，
    确保所有部署关键文件完全一致

.PARAMETER RemoteHost
    远程服务器IP (默认: 43.160.237.122)

.PARAMETER CheckType
    校验类型: Full (全量) | Quick (快速) | ConfigOnly (仅配置)

.EXAMPLE
    .\check-server-consistency.ps1 -CheckType Quick
    
.EXAMPLE
    .\check-server-consistency.ps1 -CheckType Full -Verbose
#>

[CmdletBinding()]
param(
    [string]$RemoteHost = "43.160.237.122",
    [ValidateSet("Full", "Quick", "ConfigOnly")]
    [string]$CheckType = "Quick"
)

# ===================== 配置区 =====================
$ErrorActionPreference = "Stop"
$LocalBase = "D:\BeijiXing-AI"
$RemoteUser = "root"
$RemoteKey = "$LocalBase\singapore.pem"
$RemoteBase = "/opt/beijixing-ai"
$LogFile = "$LocalBase\docs\consistency-check-$(Get-Date -Format 'yyyyMMdd-HHmmss').log"

# ===================== 初始化 =====================
function Write-Log {
    param([string]$Message, [string]$Level = "INFO")
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $logEntry = "[$timestamp] [$Level] $Message"
    Write-Host $logEntry -ForegroundColor $(switch($Level) {
        "ERROR" {"Red"}; "WARN" {"Yellow"}; "SUCCESS" {"Green"; default {"White"}}
    })
    Add-Content -Path $LogFile -Value $logEntry
}

function Test-SshConnection {
    Write-Log "测试SSH连接: $RemoteUser@$RemoteHost ..."
    try {
        $testResult = ssh -i $RemoteKey -o StrictHostKeyChecking=no -o ConnectTimeout=10 $RemoteUser@$RemoteHost "echo 'OK'" 2>&1
        if ($testResult -match "OK") {
            Write-Log "SSH连接成功!" "SUCCESS"
            return $true
        } else {
            Write-Log "SSH连接失败: $testResult" "ERROR"
            return $false
        }
    } catch {
        Write-Log "SSH连接异常: $_" "ERROR"
        return $false
    }
}

function Get-RemoteFileHash {
    param([string]$RemotePath)
    try {
        $hash = ssh -i $RemoteKey -o StrictHostKeyChecking=no $RemoteUser@$RemoteHost "sha256sum '$RemotePath' 2>/dev/null | cut -d' ' -f1"
        return $hash.Trim()
    } catch {
        return "ERROR"
    }
}

function Compare-FileConsistency {
    param(
        [string]$Category,
        [string]$LocalPath,
        [string]$RemotePath
    )
    
    if (!(Test-Path $LocalPath)) {
        Write-Log "[MISSING] 本地文件不存在: $LocalPath" "WARN"
        return @{Status="LOCAL_MISSING"; LocalHash="N/A"; RemoteHash="N/A"}
    }
    
    $localHash = (Get-FileHash $LocalPath -Algorithm SHA256).Hash
    $remoteHash = Get-RemoteFileHash -RemotePath $RemotePath
    
    if ($remoteHash -eq "ERROR" -or $remoteHash -eq "") {
        Write-Log "[REMOTE_MISSING] 远程文件不存在或无法访问: $RemotePath" "WARN"
        return @{Status="REMOTE_MISSING"; LocalHash=$localHash; RemoteHash="N/A"}
    }
    
    if ($localHash -eq $remoteHash) {
        Write-Log "[OK] $Category : 一致" "SUCCESS"
        return @{Status="MATCH"; LocalHash=$localHash; RemoteHash=$remoteHash}
    } else {
        Write-Log "[MISMATCH] $Category : 不一致!" "ERROR"
        return @{Status="MISMATCH"; LocalHash=$localHash; RemoteHash=$remoteHash}
    }
}

# ===================== 主程序 =====================
Write-Log "========================================"
Write-Log "  北极星AI - 服务器一致性校验"
Write-Log "  模式: $CheckType"
Write-Log "  时间: $(Get-Date)"
Write-Log "========================================"

$results = @()
$totalChecked = 0
$matchCount = 0
$mismatchCount = 0

# 测试SSH连接
$sshOk = Test-SshConnection
if (!$sshOk) {
    Write-Log ""
    Write-Log "⚠️  无法连接远程服务器，将仅检查本地文件存在性" "WARN"
    Write-Log "请确保:" "WARN"
    Write-Log "  1. SSH客户端已安装 (Windows 10+ 已内置)" "WARN"
    Write-Log "  2. SSH密钥路径正确: $RemoteKey" "WARN"
    Write-Log "  3. 服务器可达且SSH服务运行" "WARN"
    Write-Log ""
}

# ===================== Tier 1: POM文件检查 =====================
if ($CheckType -in @("Full", "Quick")) {
    Write-Log "`n--- [Tier 1] 后端POM文件检查 ---"
    
    # 根POM
    $totalChecked++
    $result = Compare-FileConsistency -Category "根pom.xml" `
        -LocalPath "$LocalBase\backend\pom.xml" `
        -RemotePath "$RemoteBase/backend/pom.xml"
    $results += $result
    if ($result.Status -eq "MATCH") { $matchCount++ }
    elseif ($result.Status -eq "MISMATCH") { $mismatchCount++ }
    
    # 模块POM
    $modules = @("bx-gateway","bx-user","bx-tenant","bx-content","bx-lead","bx-ai","bx-risk",
                 "bx-message","bx-storage","bx-system","bx-data","bx-social","bx-schedule","bx-monitor","bx-billing")
    
    foreach ($module in $modules) {
        $totalChecked++
        $result = Compare-FileConsistency -Category "$module/pom.xml" `
            -LocalPath "$LocalBase\backend\$module\pom.xml" `
            -RemotePath "$RemoteBase/backend/$module/pom.xml"
        $results += $result
        if ($result.Status -eq "MATCH") { $matchCount++ }
        elseif ($result.Status -eq "MISMATCH") { $mismatchCount++ }
    }
}

# ===================== Tier 2: 配置文件检查 =====================
if ($CheckType -in @("Full", "Quick", "ConfigOnly")) {
    Write-Log "`n--- [Tier 2] 应用配置文件检查 ---"
    
    foreach ($module in $modules) {
        # application.yml
        $ymlPath = "$LocalBase\backend\$module\src\main\application.yml"
        if (Test-Path $ymlPath) {
            $totalChecked++
            $result = Compare-FileConsistency -Category "$module/application.yml" `
                -LocalPath $ymlPath `
                -RemotePath "$RemoteBase/backend/$module/src/main/application.yml"
            $results += $result
            if ($result.Status -eq "MATCH") { $matchCount++ }
            elseif ($result.Status -eq "MISMATCH") { $mismatchCount++ }
        }
        
        # bx-ai bootstrap.yml (特殊处理)
        if ($module -eq "bx-ai") {
            $bootPath = "$LocalBase\backend\$module\src\main\bootstrap.yml"
            if (Test-Path $bootPath) {
                $totalChecked++
                $result = Compare-FileConsistency -Category "$module/bootstrap.yml ⚠️" `
                    -LocalPath $bootPath `
                    -RemotePath "$RemoteBase/backend/$module/src/main/bootstrap.yml"
                $results += $result
                if ($result.Status -eq "MATCH") { $matchCount++ }
                elseif ($result.Status -eq "MISMATCH") { $mismatchCount++ }
            }
        }
    }
}

# ===================== Tier 3: Nacos配置检查 =====================
if ($CheckType -eq "Full") {
    Write-Log "`n--- [Tier 3] Nacos配置中心 ---"
    
    $nacosConfigs = @("ai-model-config.yml","common-datasource.yml","common-jwt.yml",
                      "common-logging.yml","common-redis.yml")
    
    foreach ($cfg in $nacosConfigs) {
        $totalChecked++
        $result = Compare-FileConsistency -Category "nacos/$cfg ⚠️" `
            -LocalPath "$LocalBase\nacos-config\$cfg" `
            -RemotePath "$RemoteBase/nacos-config/$cfg"
        $results += $result
        if ($result.Status -eq "MATCH") { $matchCount++ }
        elseif ($result.Status -eq "MISMATCH") { $mismatchCount++ }
    }
}

# ===================== Tier 4: 数据库脚本检查 =====================
if ($CheckType -eq "Full") {
    Write-Log "`n--- [Tier 4] 数据库脚本 ---"
    
    $sqlFiles = @("init_database.sql","init_all_database.sql","init_data.sql",
                  "bx-user.sql","bx-lead.sql","bx-billing.sql","bx-risk.sql",
                  "tenant_init.sql","lead_init.sql","alter_add_foreign_keys.sql","bx_risk_init.sql")
    
    foreach ($sql in $sqlFiles) {
        # 尝试多个可能的位置
        $possiblePaths = @(
            "$LocalBase\scripts\sql\$sql",
            "$LocalBase\deploy\docker\mysql\$sql",
            "$LocalBase\backend\bx-data\src\main\resources\db\$sql"
        )
        
        $localSql = $possiblePaths | Where-Object { Test-Path $_ } | Select-Object -First 1
        
        if ($localSql) {
            $totalChecked++
            $result = Compare-FileConsistency -Category "sql/$sql" `
                -LocalPath $localSql `
                -RemotePath "$RemoteBase/scripts/sql/$sql"
            $results += $result
            if ($result.Status -eq "MATCH") { $matchCount++ }
            elseif ($result.Status -eq "MISMATCH") { $mismatchCount++ }
        }
    }
}

# ===================== 结果汇总 =====================
Write-Log "`n========================================"
Write-Log "  校验结果汇总"
Write-Log "========================================"
Write-Log "总检查文件数: $totalChecked"
Write-Log "✅ 一致文件数: $matchCount" "SUCCESS"
if ($mismatchCount -gt 0) {
    Write-Log "❌ 不一致文件数: $mismatchCount" "ERROR"
} else {
    Write-Log "❌ 不一致文件数: 0" "SUCCESS"}
Write-Log "📊 一致率: $([math]::Round(($matchCount / $totalChecked) * 100, 2))%"

if ($mismatchCount -gt 0) {
    Write-Log "`n⚠️  发现不一致的文件:" "WARN"
    $results | Where-Object {$_.Status -eq "MISMATCH"} | ForEach-Object {
        Write-Log "  • $($_.LocalHash.Substring(0,8))... (本地)" "WARN"
    }
    Write-Log ""
    Write-Log "建议执行: .\sync-configs-from-local.bat" "WARN"
}

# 输出到CSV
$resultsCsv = "$LocalBase\docs\consistency-results-$(Get-Date -Format 'yyyyMMdd-HHmmss').csv"
$results | Export-Csv -Path $resultsCsv -NoTypeInformation -Encoding UTF8
Write-Log "`n详细结果已保存: $resultsCsv"
Write-Log "日志文件: $LogFile"

Write-Log "`n========================================"
if ($mismatchCount -eq 0) {
    Write-Log "  ✅ 校验通过! 所有文件保持一致" "SUCCESS"
} else {
    Write-Log "  ❌ 发现差异! 请执行同步操作" "ERROR"
}
Write-Log "========================================"

return @{
    TotalChecked = $totalChecked
    Matched = $matchCount
    Mismatched = $mismatchCount
    ConsistencyRate = [math]::Round(($matchCount / $totalChecked) * 100, 2)
    LogFile = $LogFile
    ResultsFile = $resultsCsv
}