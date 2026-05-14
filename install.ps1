# ═══════════════════════════════════════════════════════════════
#  Group Debug & Deploy Expert - Windows Installer v1.0.1
#  通用调试部署专家团队 - Windows安装程序
#
#  Features:
#  ✅ File integrity verification (SHA256)
#  ✅ Install success/failure detection with detailed reasons
#  ✅ Auto-open user manual after installation
#  ✅ Beginner-friendly step-by-step guidance with colors
#  ✅ Comprehensive error handling and logging
# ═══════════════════════════════════════════════════════════════

[CmdletBinding()]
param(
    [switch]$Force,
    [switch]$Verbose,
    [string]$InstallPath = ""
)

# ═══════════════════════════════════════════════════════════════
#  Global Configuration
# ═══════════════════════════════════════════════════════════════
$ErrorActionPreference = "Stop"
$VERSION = "1.0.1"
$PACKAGE_NAME = "group-debug-deploy-expert"
$EXPECTED_FILES = 11
$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$INSTALL_SUCCESS = $false
$ERROR_LOG = @()
$WARNINGS = 0
$COPIED_COUNT = 0

# Target directory
if ($InstallPath) {
    $TARGET_DIR = $InstallPath
} else {
    $TARGET_DIR = Join-Path $env:USERPROFILE ".trae\skills\$PACKAGE_NAME"
}

# ═══════════════════════════════════════════════════════════════
#  Color Helper Functions (for beginner-friendly output)
# ═══════════════════════════════════════════════════════════════
function Write-Header {
    Write-Host ""
    Write-Host "╔══════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
    Write-Host "║" -ForegroundColor Cyan -NoNewline
    Write-Host " 🛡️  Group Debug & Deploy Expert Installer v$VERSION       " -ForegroundColor White -NoNewline
    Write-Host "║" -ForegroundColor Cyan
    Write-Host "║" -ForegroundColor Cyan -NoNewline
    Write-Host "    通用调试部署专家团队 - Enterprise-grade AI Team         " -ForegroundColor Gray -NoNewline
    Write-Host "║" -ForegroundColor Cyan
    Write-Host "╚══════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
    Write-Host ""
}

function Write-Step([string]$Message) {
    Write-Host "▶ $Message" -ForegroundColor Blue
}

function Write-Success([string]$Message) {
    Write-Host "✅ $Message" -ForegroundColor Green
}

function Write-WarningMsg([string]$Message) {
    Write-Host "⚠️  $Message" -ForegroundColor Yellow
    $script:WARNINGS++
}

function Write-ErrorLog([string]$Message) {
    Write-Host "❌ $Message" -ForegroundColor Red
    $script:ERROR_LOG += $Message
}

function Write-Info([string]$Message) {
    Write-Host "   ℹ️  $Message" -ForegroundColor White
}

# ═══════════════════════════════════════════════════════════════
#  Step 1: Pre-Installation Checks
# ═══════════════════════════════════════════════════════════════
function Test-Prerequisites {
    Write-Step "Step 1/6: Checking system requirements..."
    
    # Check PowerShell version
    $PSVersion = $PSVersionTable.PSVersion.ToString()
    Write-Success "PowerShell: $PSVersion"
    
    # Check disk space (need at least 10MB)
    $drive = Split-Path $SCRIPT_DIR -Qualifier
    $driveInfo = Get-WmiObject Win32_LogicalDisk -Filter "DeviceID='$drive'" -ErrorAction SilentlyContinue
    if ($driveInfo) {
        $freeSpaceGB = [math]::Round($driveInfo.FreeSpace / 1GB, 2)
        if ($freeSpaceGB -lt 0.01) {  # 10MB
            Write-ErrorLog "Insufficient disk space on $drive. Need at least 10MB, available: ${freeSpaceGB}GB"
            return $false
        }
        Write-Success "Disk space: ${freeSpaceGB}GB free on $drive"
    }
    
    # Check write permissions
    $parentDir = Split-Path $TARGET_DIR -Parent
    if (-not (Test-Path $parentDir)) {
        try {
            New-Item -ItemType Directory -Path $parentDir -Force | Out-Null
            Write-Success "Created parent directory: $parentDir"
        } catch {
            Write-ErrorLog "Cannot create directory: $parentDir (permission denied)"
            return $false
        }
    }
    
    # Test write access
    $testFile = Join-Path $parentDir ".write_test_$(Get-Random)"
    try {
        [System.IO.File]::WriteAllText($testFile, "test") | Out-Null
        Remove-Item $testFile -Force -ErrorAction SilentlyContinue
        Write-Success "Write permissions: OK"
    } catch {
        Write-ErrorLog "No write permission to: $parentDir"
        return $false
    }
    
    Write-Host ""
    return $true
}

# ═══════════════════════════════════════════════════════════════
#  Step 2: File Integrity Verification (SHA256 + Count)
# ═══════════════════════════════════════════════════════════════
function Test-PackageIntegrity {
    Write-Step "Step 2/6: Verifying package integrity..."
    
    # Check if checksums file exists
    $checksumsFile = Join-Path $SCRIPT_DIR "checksums.sha256"
    if (-not (Test-Path $checksumsFile)) {
        Write-WarningMsg "checksums.sha256 not found, skipping SHA256 verification"
    } else {
        Write-Info "Running SHA256 integrity check..."
        
        # Read and verify checksums
        $checksums = Get-Content $checksumsFile | Where-Object { $_ -match '^\S+\s+\*\?(\S+)' }
        $verifiedCount = 0
        $failedCount = 0
        
        foreach ($line in $checksums) {
            if ($line -match '^(\S+)\s+\*?(\S+)') {
                $expectedHash = $Matches[1]
                $filePath = $Matches[2]
                $fullPath = Join-Path $SCRIPT_DIR $filePath
                
                if (Test-Path $fullPath) {
                    $actualHash = (Get-FileHash $fullPath -Algorithm SHA256).Hash.ToLower()
                    if ($expectedHash -eq $actualHash) {
                        $verifiedCount++
                    } else {
                        Write-ErrorLog "SHA256 mismatch: $filePath"
                        Write-Info "  Expected: $expectedHash"
                        Write-Info "  Actual:   $actualHash"
                        $failedCount++
                    }
                }
            }
        }
        
        if ($failedCount -gt 0) {
            Write-ErrorLog "SHA256 verification FAILED for $failedCount file(s)"
            Write-ErrorLog "The package may be corrupted or tampered with"
            return $false
        } elseif ($verifiedCount -gt 0) {
            Write-Success "SHA256 verification passed ($verifiedCount files)"
        }
    }
    
    # Verify expected file count
    $actualFiles = Get-ChildItem -Path $SCRIPT_DIR -File -Recurse |
        Where-Object { $_.FullName -notmatch '\\publish\\|\\.git\\' } |
        Where-Object { $_.Extension -match '\.(md|json|yml)$' -or $_.Name -match '^(LICENSE|VERSION|checksums|\.gitignore)$' }
    
    $actualFileCount = $actualFiles.Count
    
    if ($actualFileCount -lt $EXPECTED_FILES) {
        Write-ErrorLog "Incomplete package: Expected $EXPECTED_FILES files, found $actualFileCount"
        Write-ErrorLog "Some essential files may be missing or corrupted"
        
        # List missing critical files
        $requiredFiles = @("README.md", "package.json", "skill.json", "LICENSE", "VERSION")
        foreach ($reqFile in $requiredFiles) {
            $reqPath = Join-Path $SCRIPT_DIR $reqFile
            if (-not (Test-Path $reqPath)) {
                Write-ErrorLog "  Missing: $reqFile"
            }
        }
        return $false
    }
    
    Write-Success "File count verified: $actualFileCount files present"
    Write-Host ""
    return $true
}

# ═══════════════════════════════════════════════════════════════
#  Step 3: Installation Process
# ═══════════════════════════════════════════════════════════════
function Invoke-Installation {
    Write-Step "Step 3/6: Installing $PACKAGE_NAME v$VERSION..."
    
    # Create target directory
    try {
        New-Item -ItemType Directory -Path $TARGET_DIR -Force | Out-Null
        Write-Success "Created target directory: $TARGET_DIR"
    } catch {
        Write-ErrorLog "Failed to create installation directory: $TARGET_DIR"
        return $false
    }
    
    # Copy core skill files
    $coreFiles = @(
        ".trae\skills\group-debug-deploy-expert\SKILL.md",
        ".trae\skills\group-debug-deploy-expert\LICENSE",
        ".trae\skills\group-debug-deploy-expert\VERSION",
        ".trae\rules\core_file_protection.md",
        ".trae\rules\project_rules.md",
        ".trae\rules\tier0_manifest.yml"
    )
    
    $failedCount = 0
    
    foreach ($file in $coreFiles) {
        $srcPath = Join-Path $SCRIPT_DIR $file
        $dstPath = Join-Path $TARGET_DIR $file
        
        if (Test-Path $srcPath) {
            $dstParent = Split-Path $dstPath -Parent
            if (-not (Test-Path $dstParent)) {
                New-Item -ItemType Directory -Path $dstParent -Force | Out-Null
            }
            
            try {
                Copy-Item $srcPath $dstPath -Force
                $script:COPIED_COUNT++
            } catch {
                Write-WarningMsg "Failed to copy: $file"
                $failedCount++
            }
        } else {
            Write-WarningMsg "Source file not found: $file"
            $failedCount++
        }
    }
    
    # Copy metadata files
    $metaFiles = @("README.md", "README-DEPLOY.md", "package.json", "skill.json", "LICENSE", "VERSION", "checksums.sha256")
    
    foreach ($file in $metaFiles) {
        $srcPath = Join-Path $SCRIPT_DIR $file
        $dstPath = Join-Path $TARGET_DIR $file
        
        if (Test-Path $srcPath) {
            try {
                Copy-Item $srcPath $dstPath -Force
                $script:COPIED_COUNT++
            } catch {
                $failedCount++
            }
        }
    }
    
    if ($failedCount -gt 0) {
        Write-ErrorLog "Installation partially failed: $failedCount file(s) could not be copied"
        Write-Info "Successfully copied: $COPIED_COUNT/$($COPIED_COUNT + $failedCount) files"
        return $false
    }
    
    Write-Success "Installed $COPIED_COUNT files to $TARGET_DIR"
    Write-Host ""
    return $true
}

# ═══════════════════════════════════════════════════════════════
#  Step 4: Post-Installation Verification
# ═══════════════════════════════════════════════════════════════
function Confirm-Installation {
    Write-Step "Step 4/6: Verifying installation..."
    
    $issues = 0
    
    # Check SKILL.md exists (critical file)
    $skillFile = Join-Path $TARGET_DIR ".trae\skills\group-debug-deploy-expert\SKILL.md"
    if (Test-Path $skillFile) {
        $skillSize = (Get-Item $skillFile).Length
        $skillSizeKB = [math]::Round($skillSize / 1KB, 1)
        
        if ($skillSize -gt 1000) {
            Write-Success "Core skill file: OK (${skillSizeKB}KB)"
        } else {
            Write-ErrorLog "Core skill file appears corrupted (size: ${skillSize}B)"
            $issues++
        }
    } else {
        Write-ErrorLog "Core skill file (SKILL.md) NOT FOUND"
        $issues++
    }
    
    # Check LICENSE
    if (Test-Path (Join-Path $TARGET_DIR "LICENSE")) {
        Write-Success "License file: OK"
    } else {
        Write-WarningMsg "License file missing (non-critical)"
    }
    
    # Check README
    if (Test-Path (Join-Path $TARGET_DIR "README.md")) {
        Write-Success "Documentation (README): OK"
    } else {
        Write-WarningMsg "README missing (documentation unavailable)"
    }
    
    # Check package.json
    if (Test-Path (Join-Path $TARGET_DIR "package.json")) {
        Write-Success "Package metadata: OK"
    } else {
        Write-WarningMsg "package.json missing (npm features disabled)"
    }
    
    if ($issues -eq 0) {
        $script:INSTALL_SUCCESS = $true
        Write-Success "Installation verification: PASSED ✓"
        Write-Host ""
        return $true
    } else {
        Write-ErrorLog "Installation verification: FAILED ($issues issue(s))"
        Write-Host ""
        return $false
    }
}

# ═══════════════════════════════════════════════════════════════
#  Step 5: Generate Installation Report
# ═══════════════════════════════════════════════════════════════
function New-InstallationReport {
    Write-Step "Step 5/6: Generating installation report..."
    
    $reportFile = Join-Path $TARGET_DIR "install-report.txt"
    
    $statusText = if ($INSTALL_SUCCESS) { "✅ SUCCESS" } else { "❌ FAILED" }
    $errorText = if ($ERROR_LOG.Count -gt 0) { $ERROR_LOG -join "`n" } else { "None" }
    
    $reportContent = @"
╔══════════════════════════════════════════════════════════╗
║     Installation Report - Group Debug & Deploy Expert      ║
║                   Version $VERSION                           ║
╚══════════════════════════════════════════════════════════╝

Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
Platform: $(systeminfo | Select-String 'OS Name:' | ForEach-Object { $_.Line.Trim() })
Installer Version: 1.0.1

─── Status ───────────────────────────────────────────────
Result: $statusText
Warnings: $WARNINGS
Errors: $errorText

─── Installation Details ──────────────────────────────────
Target Directory: $TARGET_DIR
Files Installed: $COPIED_COUNT
Package Source: $SCRIPT_DIR

─── Support Information ───────────────────────────────────
Documentation: $TARGET_DIR\README.md
User Guide: $TARGET_DIR\USER_GUIDE.md (if available)
Issues: https://github.com/xuanji-ai-2026/group-debug-deploy-expert/issues
Email: z18288090942@gmail.com

───────────────────────────────────────────────────────────
"@
    
    Set-Content -Path $reportFile -Value $reportContent -Encoding UTF8
    Write-Success "Report saved to: $reportFile"
    Write-Host ""
}

# ═══════════════════════════════════════════════════════════════
#  Step 6: Post-Install Guidance (Beginner-Friendly)
# ═══════════════════════════════════════════════════════════════
function Show-PostInstallGuide {
    Write-Step "Step 6/6: Showing getting-started guide..."
    
    Write-Host ""
    Write-Host "╔══════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
    Write-Host "║" -ForegroundColor Cyan -NoNewline
    Write-Host " 🎉 Installation Complete! Next Steps:" -ForegroundColor White -NoNewline
    Write-Host "                      " -NoNewline
    Write-Host "║" -ForegroundColor Cyan
    Write-Host "╚══════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
    Write-Host ""
    
    if ($INSTALL_SUCCESS) {
        Write-Host "┌─────────────────────────────────────────────┐" -ForegroundColor Green
        Write-Host "│" -ForegroundColor Green -NoNewline
        Write-Host "  ✅ STATUS: INSTALLED SUCCESSFULLY           " -ForegroundColor White -NoNewline
        Write-Host "│" -ForegroundColor Green
        Write-Host "└─────────────────────────────────────────────┘" -ForegroundColor Green
        Write-Host ""
        
        Write-Host "📖 Quick Start Guide for Beginners:" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "  1. Read the User Manual:" -ForegroundColor White
        Write-Host "     📄 $TARGET_DIR\README.md" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "  2. First Time Usage Example:" -ForegroundColor White
        Write-Host '     Say: "Help me debug my Spring Boot project"' -ForegroundColor Gray
        Write-Host ""
        Write-Host "  3. Learn the 21 Iron Principles:" -ForegroundColor White
        Write-Host "     See Section 3 in README.md" -ForegroundColor Gray
        Write-Host ""
        Write-Host "  4. Need Help?" -ForegroundColor White
        Write-Host "     📧 Email: z18288090942@gmail.com" -ForegroundColor Cyan
        Write-Host "     📱 Phone: +86 19537722739" -ForegroundColor Cyan
        Write-Host "     🌐 Issues: https://github.com/xuanji-ai-2026/group-debug-deploy-expert/issues" -ForegroundColor Cyan
        Write-Host ""
        
        # Auto-open documentation
        $readmePath = Join-Path $TARGET_DIR "README.md"
        if (Test-Path $readmePath) {
            Write-Host "ℹ️  Opening user manual..." -ForegroundColor Blue
            Start-Process $readmePath
        }
        
    } else {
        Write-Host "┌─────────────────────────────────────────────┐" -ForegroundColor Red
        Write-Host "│" -ForegroundColor Red -NoNewline
        Write-Host "  ❌ STATUS: INSTALLATION FAILED               " -ForegroundColor White -NoNewline
        Write-Host "│" -ForegroundColor Red
        Write-Host "└─────────────────────────────────────────────┘" -ForegroundColor Red
        Write-Host ""
        
        Write-Host "🔍 Troubleshooting Steps:" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "  1. Review error messages above" -ForegroundColor White
        Write-Host "  2. Check permissions on target directory:" -ForegroundColor White
        Write-Host "     ls '$(Split-Path $TARGET_DIR -Parent)'" -ForegroundColor Gray
        Write-Host ""
        Write-Host "  3. Ensure disk space is sufficient" -ForegroundColor White
        Write-Host "  4. Re-download the package and try again" -ForegroundColor White
        Write-Host ""
        Write-Host "  5. Contact support with this info:" -ForegroundColor White
        Write-Host "     Platform: $(systeminfo | Select-String 'OS Name:' | ForEach-Object { $_.Line.Trim() })" -ForegroundColor Gray
        Write-Host "     Errors: $($ERROR_LOG -join ', ')" -ForegroundColor Red
        Write-Host "     📧 z18288090942@gmail.com" -ForegroundColor Cyan
        Write-Host ""
    }
    
    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host " Thank you for choosing Group Debug & Deploy Expert!" -ForegroundColor Cyan
    Write-Host " 感谢您选择通用调试部署专家团队！" -ForegroundColor Cyan
    Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host ""
}

# ═══════════════════════════════════════════════════════════════
#  Main Execution Flow
# ═══════════════════════════════════════════════════════════════
function Main {
    Write-Header
    
    # Execute installation steps
    if (-not (Test-Prerequisites)) {
        Show-PostInstallGuide
        exit 1
    }
    
    if (-not (Test-PackageIntegrity)) {
        Write-ErrorLog "Package integrity verification failed. Aborting installation."
        Show-PostInstallGuide
        exit 1
    }
    
    if (-not (Invoke-Installation)) {
        Write-ErrorLog "Installation process failed."
        Confirm-Installation | Out-Null
        New-InstallationReport | Out-Null
        Show-PostInstallGuide
        exit 1
    }
    
    if (-not (Confirm-Installation)) {
        New-InstallationReport | Out-Null
        Show-PostInstallGuide
        exit 1
    }
    
    New-InstallationReport
    Show-PostInstallGuide
    
    # Exit with appropriate code
    if ($INSTALL_SUCCESS) {
        exit 0
    } else {
        exit 1
    }
}

# Run main function
Main
