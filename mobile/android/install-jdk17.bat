@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo.
echo ╔══════════════════════════════════════╗
echo ║  北极星AI - JDK 17 快速安装工具    ║
echo ╚══════════════════════════════════════╝
echo.

REM 检查是否已安装Java
where java >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo [✓] 检测到已安装的Java:
    java -version
    echo.
    set /p CONTINUE="是否继续重新安装? (y/n): "
    if /i not "%CONTINUE%"=="y" goto :eof
)

echo [步骤1/4] 下载JDK 17 (Eclipse Temurin)...
echo.

REM 设置下载URL和路径
set JDK_URL=https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9%%2B9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.9_9.msi
set TEMP_PATH=%TEMP%\OpenJDK17U-jdk_x64_windows_hotspot_17.0.9_9.msi

REM 使用PowerShell下载（更可靠）
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "Write-Host '正在从 Adoptium 下载 JDK 17...'; ^
     Write-Host '文件大小: ~170 MB'; ^
     Write-Host ''; ^
     [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; ^
     try { ^
         Invoke-WebRequest -Uri '%JDK_URL%' -OutFile '%TEMP_PATH%' -UseBasicParsing; ^
         Write-Host '[✓] 下载完成'; ^
         exit 0; ^
     } catch { ^
         Write-Host '[✗] 下载失败:' $_.Exception.Message; ^
         exit 1; ^
     }"

if %ERRORLEVEL% neq 0 (
    echo.
    echo [错误] 自动下载失败！
    echo.
    echo 请手动下载:
    echo   URL: https://adoptium.net/
    echo   选择: Temurin 17 ^(LTS^) ^> Windows ^> x64 ^> .msi
    echo.
    start https://adoptium.net/temurin/releases/?version=17
    pause
    exit /b 1
)

if not exist "%TEMP_PATH%" (
    echo [错误] 下载的文件不存在！
    pause
    exit /b 1
)

echo.
echo [步骤2/4] 安装JDK 17...
echo 正在执行静默安装（约需2-3分钟）...

REM 静默安装MSI
msiexec /i "%TEMP_PATH%" /quiet /norestart ADDLOCAL=FeatureMain,FeatureEnvironment,FeatureJarFileRunWith,FeatureJavaHome

REM 等待安装完成
echo 等待安装完成...
timeout /t 45 /nobreak >nul

echo.
echo [步骤3/4] 配置环境变量...

REM 查找安装路径
set JAVA_HOME=
for /f "tokens=2*" %%a in ('reg query "HKLM\SOFTWARE\JavaSoft\Java Development Kit" /v CurrentVersion 2^>nul') do set JAVA_VER=%%b
if defined JAVA_VER (
    for /f "tokens=2*" %%a in ('reg query "HKLM\SOFTWARE\JavaSoft\Java Development Kit\%JAVA_VER%" /v JavaHome 2^>nul') do set JAVA_HOME=%%b
)

if not defined JAVA_HOME (
    REM 尝试常见路径
    if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot" (
        set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotstack"
    ) else if exist "C:\Program Files\Eclipse Adoptium\jdk-17*" (
        for /d %%i in ("C:\Program Files\Eclipse Adoptium\jdk-17*") do set "JAVA_HOME=%%i"
    )
)

if defined JAVA_HOME (
    echo [✓] 找到JDK安装路径: %JAVA_HOME%
    
    REM 设置系统环境变量（需要管理员权限）
    reg add "HKLM\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" /v JAVA_HOME /t REG_SZ /d "%JAVA_HOME%" /f >nul 2>&1
    
    REM 添加到PATH
    for /f "tokens=2*" %%a in ('reg query "HKLM\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" /v Path 2^>nul') do set CURRENT_PATH=%%b
    echo %CURRENT_PATH% | findstr /i "%JAVA_HOME%\bin" >nul
    if errorlevel 1 (
        reg add "HKLM\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" /v Path /t REG_EXPAND_SZ /d "%CURRENT_PATH%;%JAVA_HOME%\bin" /f >nul 2>&1
        echo [✓] 已添加到系统PATH
    ) else (
        echo [!] PATH中已包含Java路径
    )
    
    echo [✓] 环境变量配置完成
) else (
    echo [警告] 无法自动检测安装路径
    echo 请手动设置JAVA_HOME环境变量
)

echo.
echo [步骤4/4] 验证安装...

REM 刷新当前会话的环境变量
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "JAVA_HOME=%JAVA_HOME%"

if exist "%JAVA_HOME%\bin\java.exe" (
    echo.
    echo ╔════════════════════════════════════════════╗
    echo ║          ✅ 安装成功！                  ║
    echo ╠════════════════════════════════════════════╣
    echo ║                                          ║
    "%JAVA_HOME%\bin\java.exe" -version 2>&1 | findstr /i "version"
    echo ║                                          ║
    echo ║  JAVA_HOME=%JAVA_HOME%                   ║
    echo ║                                          ║
    echo ╚════════════════════════════════════════════╝
    
    REM 清理临时文件
    del /q "%TEMP_PATH%" 2>nul
    
    echo.
    echo 重要提示:
    echo   ★ 环境变量将在新打开的命令行窗口中生效
    echo   ★ 请关闭当前窗口，重新运行构建脚本
    echo.
    echo 下一步操作:
    echo   1. 关闭此窗口
    echo   2. 重新打开CMD或PowerShell
    echo   3. 运行: cd mobile\android
    echo   4. 运行: .\gradlew.bat assembleDebug
    echo.
) else (
    echo [❌] 验证失败！未找到java.exe
    echo     路径: %JAVA_HOME%\bin\java.exe
)

pause
endlocal
