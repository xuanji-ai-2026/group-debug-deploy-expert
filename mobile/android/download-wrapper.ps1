$ErrorActionPreference = "Stop"

$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptPath

if (Test-Path "gradle\wrapper\gradle-wrapper.jar") {
    Remove-Item "gradle\wrapper\gradle-wrapper.jar" -Force
}

Write-Host "Downloading gradle-wrapper.jar..." -ForegroundColor Yellow

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$url = "https://github.com/gradle/gradle/raw/v8.4.0/gradle/wrapper/gradle-wrapper.jar"
$output = "gradle\wrapper\gradle-wrapper.jar"

try {
    Invoke-WebRequest -Uri $url -OutFile $output -UseBasicParsing
    
    if (Test-Path $output) {
        $size = (Get-Item $output).Length
        $sizeKB = [math]::Round($size / 1KB, 2)
        Write-Host "SUCCESS: $sizeKB KB" -ForegroundColor Green
        exit 0
    } else {
        throw "File not created"
    }
} catch {
    Write-Host "ERROR: $_" -ForegroundColor Red
    exit 1
}
