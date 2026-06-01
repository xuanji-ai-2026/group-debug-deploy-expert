# Quick Publisher - Minimal Interaction Version
# Usage: .\quick-publish.ps1 -Token "pypi-xxxxx"

param(
    [Parameter(Mandatory=$false)]
    [string]$Token = ""
)

$ErrorActionPreference = "Stop"
$ProjectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectName = "pm-tripartite-anti-hallucination-system"
$Version = "1.0.0"

Write-Host "`n⚡ Quick Publish Mode" -ForegroundColor Cyan

# Check dist
if (-not (Test-Path "$ProjectDir\dist")) {
    Write-Host "❌ No dist/ directory. Run: python -m build" -ForegroundColor Red
    exit 1
}

# Get token
if (-not $Token) {
    $Token = $env:TWINE_PASSWORD
}
if (-not $Token) {
    if (Test-Path "$ProjectDir\.env") {
        Get-Content "$ProjectDir\.env" | ForEach-Object {
            if ($_ -match "^TWINE_PASSWORD=(.*)$") {
                $Token = $Matches[1].Trim('"').Trim("'")
            }
        }
    }
}

if (-not $Token) {
    Write-Host "`n🔑 Enter PyPI API token:" -ForegroundColor Yellow
    $Token = Read-Host "   Token"
}

if ([string]::IsNullOrWhiteSpace($Token)) {
    Write-Host "❌ No token provided. Exiting." -ForegroundColor Red
    exit 1
}

# Upload to PyPI
Write-Host "`n📤 Uploading to PyPI..." -ForegroundColor Yellow
Push-Location $ProjectDir
try {
    $env:TWINE_USERNAME = "__token__"
    $env:TWINE_PASSWORD = $Token
    
    twine upload dist/* 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "`n✅ Success! Package available at:" -ForegroundColor Green
        Write-Host "   https://pypi.org/project/$ProjectName/" -ForegroundColor Cyan
        Write-Host "`n📦 Install with:" -ForegroundColor Yellow
        Write-Host "   pip install $ProjectName" -ForegroundColor White
    } else {
        Write-Host "`n❌ Upload failed. Check token and permissions." -ForegroundColor Red
    }
} finally {
    Pop-Location
    $env:TWINE_PASSWORD = ""
}
