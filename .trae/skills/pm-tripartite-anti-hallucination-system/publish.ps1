<#
.SYNOPSIS
    One-Click Publisher for PM-Tripartite-Anti-Hallucination-System
    
.DESCRIPTION
    Automates the complete publishing workflow:
    1. GitHub Release creation (with binary attachments)
    2. PyPI package upload (with token authentication)
    3. Post-publish verification and summary
    
.PARAMETER SkipGitHub
    Skip GitHub Release creation (only publish to PyPI)
    
.PARAMETER SkipPyPI
    Skip PyPI upload (only create GitHub Release)
    
.PARAMETER DryRun
    Simulate the publishing process without actually uploading
    
.PARAMETER Token
    PyPI API token (if not set in environment or .env file)
    
.EXAMPLE
    .\publish.ps1
    Full publish to both GitHub and PyPI
    
.EXAMPLE
    .\publish.ps1 -DryRun
    Preview what would be published without actually doing it
    
.EXAMPLE
    .\publish.ps1 -SkipGitHub -Token "pypi-xxxxx"
    Only publish to PyPI with explicit token
    
.NOTES
    Author: PM System Team
    Version: 1.0.0
    Date: 2026-06-01
#>

[CmdletBinding()]
param(
    [switch]$SkipGitHub,
    [switch]$SkipPyPI,
    [switch]$DryRun,
    [string]$Token = $null
)

# ============================================================================
# CONFIGURATION
# ============================================================================

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$Script:ProjectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$Script:DistDir = Join-Path $Script:ProjectDir "dist"
$Script:ProjectName = "pm-tripartite-anti-hallucination-system"
$Script:Version = "1.0.0"
$Script:GitHubRepo = "xuanji-ai-2026/group-debug-deploy-expert"
$Script:ReleaseNotesFile = Join-Path $Script:ProjectDir "RELEASE_NOTES.md"

Write-Host @"
╔═══════════════════════════════════════════════════════════════╗
║                                                               ║
║   🚀 One-Click Publisher v1.0.0                              ║
║   Project: $Script:ProjectName                               ║
║   Version: $Script:Version                                    ║
║                                                               ║
╚═══════════════════════════════════════════════════════════════╝
"@ -ForegroundColor Cyan

# ============================================================================
# HELPER FUNCTIONS
# ============================================================================

function Write-Step {
    param([string]$Message)
    Write-Host "`n▶ $Message" -ForegroundColor Yellow
}

function Write-Success {
    param([string]$Message)
    Write-Host "  ✅ $Message" -ForegroundColor Green
}

function Write-ErrorCustom {
    param([string]$Message)
    Write-Host "  ❌ $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "  ℹ️  $Message" -ForegroundColor Gray
}

function Test-CommandExists {
    param([string]$Command)
    $exists = Get-Command -Name $Command -ErrorAction SilentlyContinue
    return ($null -ne $exists)
}

function Get-PyPIToken {
    # Priority: Parameter > Environment > .env file > Prompt
    if ($Token) { return $Token }
    
    $envToken = $env:TWINE_PASSWORD
    if ($envToken) { 
        Write-Info "Using TWINE_PASSWORD from environment"
        return $envToken 
    }
    
    $envFile = Join-Path $Script:ProjectDir ".env"
    if (Test-Path $envFile) {
        Get-Content $envFile | ForEach-Object {
            if ($_ -match "^TWINE_PASSWORD=(.*)$") {
                $token = $Matches[1].Trim('"').Trim("'")
                if ($token) {
                    Write-Info "Using token from .env file"
                    return $token
                }
            }
        }
    }
    
    return $null
}

# ============================================================================
# PRE-CHECKS
# ============================================================================

Write-Step "Running pre-flight checks..."

$checksPassed = $true

# Check dist directory
if (-not (Test-Path $Script:DistDir)) {
    Write-ErrorCustom "Distribution directory not found: $Script:DistDir"
    Write-Info "Run 'python -m build' first to generate packages"
    $checksPassed = $false
} else {
    $packages = Get-ChildItem -Path $Script:DistDir -Filter "*$Script:Version*"
    if ($packages.Count -eq 0) {
        Write-ErrorCustom "No packages found in dist/ directory"
        $checksPassed = $false
    } else {
        Write-Success "Found $($packages.Count) package(s):"
        $packages | ForEach-Object { Write-Info "  - $($_.Name) ($([Math]::Round($_.Length/1KB, 1)) KB)" }
    }
}

# Check RELEASE_NOTES.md
if (-not (Test-Path $Script:ReleaseNotesFile)) {
    Write-Warning "RELEASE_NOTES.md not found, will use minimal notes"
} else {
    Write-Success "Release notes found: RELEASE_NOTES.md"
}

# Check git status
Push-Location $Script:ProjectDir
try {
    $status = git status --porcelain 2>$null
    if ($status) {
        Write-Warning "Working directory has uncommitted changes"
        Write-Info "Consider committing before publishing"
    } else {
        Write-Success "Git working directory clean"
    }
}
finally {
    Pop-Location
}

if (-not $checksPassed) {
    Write-ErrorCustom "Pre-flight checks failed. Please fix issues above."
    exit 1
}

if ($DryRun) {
    Write-Step "[DRY RUN] Would perform the following:"
    if (-not $SkipGitHub) { Write-Info "  1. Create GitHub Release v$Script:Version" }
    if (-not $SkipPyPI) { Write-Info "  2. Upload packages to PyPI" }
    Write-Success "Dry run completed. No changes made."
    exit 0
}

# ============================================================================
# GITHUB RELEASE
# ============================================================================

if (-not $SkipGitHub) {
    Write-Step "Creating GitHub Release v$Script:Version..."
    
    Push-Location $Script:ProjectDir
    try {
        if (Test-CommandExists -Command "gh") {
            # Check authentication
            $authStatus = gh auth status 2>&1
            if ($LASTEXITCODE -ne 0) {
                Write-ErrorCustom "GitHub CLI not authenticated"
                Write-Info "Run 'gh auth login' first"
            } else {
                # Create release with binaries
                $releaseArgs = @(
                    "release", "create",
                    "v$Script:Version",
                    "./dist/*",
                    "--title", "🚀 v$Script:Version - Initial Release",
                    "--notes-file", "RELEASE_NOTES.md",
                    "--verify-tags"
                )
                
                & gh @releaseArgs 2>&1
                
                if ($LASTEXITCODE -eq 0) {
                    Write-Success "GitHub Release created successfully!"
                    Write-Info "URL: https://github.com/$Script:GitHubRepo/releases/tag/v$Script:Version"
                } else {
                    Write-ErrorCustom "Failed to create GitHub Release"
                    Write-Info "You can manually create it at:"
                    Write-Info "https://github.com/$Script:GitHubRepo/releases/new"
                }
            }
        } else {
            Write-Warning "GitHub CLI (gh) not installed"
            Write-Info "Install with: winget install GitHub.cli"
            Write-Info "Or manually create release at:"
            Write-Info "https://github.com/$Script:GitHubRepo/releases/new"
            
            # Generate manual instructions
            $manualInstructions = @"

📋 MANUAL GITHUB RELEASE INSTRUCTIONS:

1. Open: https://github.com/$Script:GitHubRepo/releases/new
2. Tag version: v$Script:Version
3. Release title: 🚀 v$Script:Version - Initial Release
4. Description: Copy content from RELEASE_NOTES.md
5. Attach binaries from dist/ folder:
   - pm_tripartite_anti_hallucination_system-$Script:Version-py3-none-any.whl
   - pm_tripartite_anti_hallucination_system-$Script:Version.tar.gz
6. Click 'Publish release'
"@
            Write-Host $manualInstructions -ForegroundColor Cyan
        }
    }
    finally {
        Pop-Location
    }
} else {
    Write-Step "Skipping GitHub Release ( -SkipGitHub flag)"
}

# ============================================================================
# PYPI PUBLISH
# ============================================================================

if (-not $SkipPyPI) {
    Write-Step "Publishing to PyPI..."
    
    # Get token
    $pypiToken = Get-PyPIToken
    
    if (-not $pypiToken) {
        Write-ErrorCustom "No PyPI API token found!"
        Write-Info "Please provide token via one of:"
        Write-Info "  1. -Token parameter: .\publish.ps1 -Token 'pypi-xxxxx'"
        Write-Info "  2. Environment variable: `$env:TWINE_PASSWORD='pypi-xxxxx'`"
        Write-Info "  3. .env file: Create .env with TWINE_PASSWORD=pypi-xxxxx"
        
        # Prompt for token
        Write-Host "`n🔑 Enter your PyPI API token (or press Enter to skip):" -ForegroundColor Yellow
        $pypiToken = Read-Host "  Token"
        
        if ([string]::IsNullOrWhiteSpace($pypiToken)) {
            Write-Warning "No token provided. Skipping PyPI upload."
        }
    }
    
    if ($pypiToken) {
        Push-Location $Script:ProjectDir
        try {
            # Set environment variables for twine
            $env:TWINE_USERNAME = "__token__"
            $env:TWINE_PASSWORD = $pypiToken
            
            # Run twine upload
            Write-Info "Uploading packages to PyPI..."
            twine upload dist/* 2>&1
            
            if ($LASTEXITCODE -eq 0) {
                Write-Success "PyPI upload successful!"
                Write-Info "Package URL: https://pypi.org/project/$Script:ProjectName/"
                Write-Info "Install command: pip install $Script:ProjectName"
            } else {
                Write-ErrorCustom "Failed to upload to PyPI"
                Write-Info "Check your token and permissions."
            }
        }
        finally {
            Pop-Location
            # Clean up sensitive data
            $env:TWINE_PASSWORD = ""
            $pypiToken = ""
        }
    }
} else {
    Write-Step "Skipping PyPI upload ( -SkipPyPI flag)"
}

# ============================================================================
# POST-PUBLISH SUMMARY
# ============================================================================

Write-Step "Generating post-publish summary..."

$summary = @"

╔═══════════════════════════════════════════════════════════════╗
║                                                               ║
║   🎉 PUBLISHING COMPLETE!                                   ║
║                                                               ║
╠═══════════════════════════════════════════════════════════════╣
║                                                               ║
║   📦 Package Information                                     ║
║   ─────────────────────────────────────────────────────────   ║
║   Name:    $Script:ProjectName
║   Version: $Script:Version
║   License: MIT                                               ║
║                                                               ║
║   🔗 Links                                                  ║
║   ─────────────────────────────────────────────────────────   ║
"@


if (-not $SkipGitHub) {
    $summary += @"║   GitHub:  https://github.com/$Script:GitHubRepo/releases/tag/v$Script:Version
"@
}

if (-not $SkipPyPI) {
    $summary += @"║   PyPI:    https://pypi.org/project/$Script:ProjectName/
║   Install: pip install $Script:ProjectName
"@
}

$summary += @"║                                                               ║
║   📊 What's Next?                                           ║
║   ─────────────────────────────────────────────────────────   ║
║   1. Test installation: pip install $Script:ProjectName       ║
║   2. Run demo scripts: python demo_complete_self_driving.py   ║
║   3. Share on social media!                                  ║
║   4. Monitor download statistics                            ║
║                                                               ║
║   🙏 Thank you for using PM Tripartite System!              ║
║                                                               ║
╚═══════════════════════════════════════════════════════════════╝
"@

Write-Host $summary -ForegroundColor Green

# Save summary to file
$summaryFile = Join-Path $Script:ProjectDir "PUBLISH_SUMMARY_$((Get-Date).ToString('yyyyMMdd_HHmmss')).txt"
$summary | Out-File -FilePath $summaryFile -Encoding UTF8
Write-Info "Summary saved to: $summaryFile"

Write-Success "All done! 🎊"
