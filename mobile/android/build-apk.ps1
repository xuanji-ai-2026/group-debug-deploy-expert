# APK Build Script - Fixed version (uses user directory)
$ErrorActionPreference = "Stop"

$downloadDir = "$env:USERPROFILE\jdk-temp"
$jdkDir = "$env:USERPROFILE\jdk-17"

Write-Host "=== Beijixing AI - Local APK Builder ==="
Write-Host ""

# Step 1: Check Java
Write-Host "[1/6] Checking Java..."
$javaCmd = Get-Command java -ErrorAction SilentlyContinue

if (-not $javaCmd) {
    Write-Host "    Java not found, downloading JDK 17..."
    
    if (-not (Test-Path $jdkDir)) {
        if (-not (Test-Path $downloadDir)) {
            New-Item -ItemType Directory -Path $downloadDir -Force | Out-Null
        }
        
        Write-Host "    Downloading JDK 17 (~150MB) to user profile..."
        Write-Host "    Target: $downloadDir"
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        
        try {
            $zipPath = "$downloadDir\jdk17.zip"
            Invoke-WebRequest -Uri "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk" -OutFile $zipPath -UseBasicParsing
            Write-Host "    Download complete"
            
            Write-Host "    Extracting..."
            Expand-Archive -Path $zipPath -DestinationPath $downloadDir -Force
            
            $jdkDirs = Get-ChildItem "$downloadDir\jdk-*" -Directory
            foreach ($dir in $jdkDirs) {
                Move-Item $dir.FullName $jdkDir -Force
            }
            
            Remove-Item $zipPath -Force -ErrorAction SilentlyContinue
            Remove-Item $downloadDir -Recurse -Force -ErrorAction SilentlyContinue
            Write-Host "    JDK installed to $jdkDir"
        } catch {
            Write-Host "    Download failed: $_"
            Write-Host ""
            Write-Host "    Alternative: Please install JDK 17 manually from:"
            Write-Host "    https://adoptium.net/"
            Read-Host "Press Enter to exit"
            exit 1
        }
    }
    
    $env:JAVA_HOME = $jdkDir
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
} else {
    Write-Host "    Java OK"
}

Write-Host ""
Write-Host "[2/6] Checking project directory..."
$projectDir = "d:\AI生成\AI编程\AI商机获客系统\beijixing-ai-full-complete\beijixing-ai\mobile\android"

if (-not (Test-Path "$projectDir\gradlew.bat")) {
    Write-Host "    gradlew.bat not found!"
    Read-Host "Press Enter to exit"
    exit 1
}
Write-Host "    Project OK"

Set-Location $projectDir

Write-Host ""
Write-Host "[3/6] Preparing Gradle Wrapper..."
if (-not (Test-Path "gradle\wrapper\gradle-wrapper.jar")) {
    & .\gradlew.bat wrapper --gradle-version 8.4
}

Write-Host ""
Write-Host "=== Starting APK Build ==="
Write-Host ""
Write-Host "[4/6] Running Gradle build (this takes 5-15 minutes)..."

& .\gradlew.bat clean assembleDebug 2>&1

Write-Host ""
if ($LASTEXITCODE -eq 0) {
    Write-Host "=== BUILD SUCCESSFUL ==="
    
    $apkPath = "$projectDir\app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apkPath) {
        $apkInfo = Get-Item $apkPath
        $sizeMB = [math]::Round($apkInfo.Length / 1MB, 2)
        Write-Host ""
        Write-Host "APK File: app-debug.apk"
        Write-Host "Size: $sizeMB MB"
        Write-Host "Location: $apkPath"
        Write-Host "Time: $($apkInfo.LastWriteTime)"
        Write-Host ""
        Write-Host "Opening folder..."
        Explorer "$projectDir\app\build\outputs\apk\debug\"
    } else {
        Write-Host "APK file not generated!"
    }
} else {
    Write-Host "=== BUILD FAILED ==="
    Write-Host "Please check errors above"
}

Write-Host ""
Read-Host "Press Enter to exit"