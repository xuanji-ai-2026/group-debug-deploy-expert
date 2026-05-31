@echo off
chcp 65001 >nul
REM =============================================
REM 北极星AI - Android开发环境一键安装器
REM 自动安装：JDK 17 + Gradle + 构建APK
REM =============================================

echo.
echo ╔════════════════════════════════════════════╗
echo ║   北极星AI - Android开发环境自动安装    ║
echo ║   安装内容: JDK 17 + Gradle Wrapper     ║
echo ╚════════════════════════════════════════════╝
echo.

REM 检查管理员权限
net session >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [错误] 需要管理员权限运行此脚本！
    echo 请右键点击此文件，选择"以管理员身份运行"
    pause
    exit /b 1
)

echo [步骤1/5] 检查Java环境...
java -version >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo [✓] Java已安装
    java -version
) else (
    echo [✗] 未检测到Java，开始自动安装JDK 17...
    
    REM 创建临时目录
    set TEMP_DIR=%TEMP%\jdk-installer
    if not exist "%TEMP_DIR%" mkdir "%TEMP_DIR%"
    
    echo.
    echo 正在下载 JDK 17 (Eclipse Temurin)...
    echo 下载源: Adoptium.net
    
    REM 使用PowerShell下载JDK 17 (Windows x64 msi)
    powershell -Command "& {
        $url = 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9%%2B9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.9_9.msi'
        $output = '%TEMP_DIR%\jdk17.msi'
        Write-Host '正在下载 JDK 17，请稍候...'
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -Uri $url -OutFile $output -UseBasicParsing
        Write-Host '[✓] 下载完成'
    }"
    
    if exist "%TEMP_DIR%\jdk17.msi" (
        echo.
        echo 正在安装 JDK 17 (静默安装)...
        msiexec /i "%TEMP_DIR%\jdk17.msi" /quiet /norestart ADDLOCAL=FeatureMain,FeatureEnvironment,FeatureJarFileRunWith,FeatureJavaHome
        
        REM 等待安装完成
        timeout /t 30 /nobreak >nul
        
        REM 设置JAVA_HOME环境变量（用户级别）
        for /f "tokens=*" %%i in ('reg query "HKLM\SOFTWARE\JavaSoft\Java Development Kit" /v CurrentVersion 2^>nul ^| findstr "CurrentVersion"') do set JAVA_VER=%%i
        for /f "tokens=3" %%a in ('reg query "HKLM\SOFTWARE\JavaSoft\Java Development Kit\%JAVA_VER%" /v JavaHome 2^>nul') do set JAVA_HOME=%%a
        
        if defined JAVA_HOME (
            echo [✓] JDK 17 安装成功
            echo     安装路径: %JAVA_HOME%
            
            REM 设置环境变量
            setx JAVA_HOME "%JAVA_HOME%" /M >nul 2>&1
            setx PATH "%PATH%;%JAVA_HOME%\bin" /M >nul 2>&1
            
            echo [✓] 环境变量已设置 (需要重启终端生效)
        ) else (
            echo [警告] 安装完成但无法自动获取路径
            echo     请手动设置 JAVA_HOME 环境变量
        )
        
        REM 清理临时文件
        rd /s /q "%TEMP_DIR%" 2>nul
    ) else (
        echo [错误] JDK下载失败！
        echo     请手动下载: https://adoptium.net/
        pause
        exit /b 1
    )
)

echo.
echo [步骤2/5] 验证Java版本...
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot
if exist "%JAVA_HOME%\bin\java.exe" (
    echo [✓] 找到JDK 17: %JAVA_HOME%
    "%JAVA_HOME%\bin\java.exe" -version
) else (
    echo [!] 尝试查找其他JDK位置...
    for /r "C:\Program Files" %%j in (*javac.exe*) do (
        echo     发现: %%~dpj
        set JAVA_HOME=%%~dpj..
    )
)

echo.
echo [步骤3/5] 检查Gradle Wrapper...
cd /d "%~dp0"
if exist "gradle\wrapper\gradle-wrapper.jar" (
    echo [✓] Gradle Wrapper已就绪
) else (
    echo [!] Gradle Wrapper缺失，正在生成...
    
    REM 下载Gradle Wrapper JAR
    if not exist "gradle\wrapper" mkdir "gradle\wrapper"
    
    powershell -Command "& {
        $url = 'https://raw.githubusercontent.com/gradle/gradle/v8.4.0/gradle/wrapper/gradle-wrapper.jar'
        $output = 'gradle\wrapper\gradle-wrapper.jar'
        Write-Host '正在下载 Gradle Wrapper...'
        Invoke-WebRequest -Uri $url -OutFile $output -UseBasicParsing
    }"
    
    if exist "gradle\wrapper\gradle-wrapper.jar" (
        echo [✓] Gradle Wrapper下载完成
    ) else (
        echo [错误] Gradle Wrapper下载失败
        pause
        exit /b 1
    )
)

echo.
echo [步骤4/5] 准备构建环境...

REM 设置本次会话的环境变量
set PATH=%JAVA_HOME%\bin;%PATH%
set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk

REM 检查Android SDK
if not exist "%ANDROID_HOME%" (
    echo [警告] 未检测到Android SDK
    echo     如果构建失败，请安装Android Studio:
    echo     https://developer.android.com/studio
) else (
    echo [✓] Android SDK已找到: %ANDROID_HOME%
)

echo.
echo [步骤5/5] 开始构建Debug APK...
echo.

REM 执行Gradle构建
call gradlew.bat assembleDebug --no-daemon --console=plain

if %ERRORLEVEL% equ 0 (
    echo.
    echo ╔════════════════════════════════════════════╗
    echo ║          🎉 APK构建成功！                ║
    echo ╠════════════════════════════════════════════╣
    echo ║  输出路径:                                ║
    echo ║  app\build\outputs\apk\debug\              ║
    echo ║  文件名: app-debug.apk                     ║
    echo ╚════════════════════════════════════════════╝
    echo.
    
    REM 打开输出文件夹
    if exist "app\build\outputs\apk\debug\app-debug.apk" (
        explorer "app\build\outputs\apk\debug\"
        echo 已打开APK所在文件夹
    )
) else (
    echo.
    echo [❌] 构建失败！请检查上方错误信息
    echo.
    echo 常见问题:
    echo   1. Java版本不正确 (需要JDK 17)
    echo   2. Android SDK未安装或路径错误
    echo   3. 网络问题导致依赖下载失败
    echo.
    echo 解决方案:
    echo   - 安装JDK 17: https://adoptium.net/
    echo   - 安装Android Studio: https://developer.android.com/studio
)

echo.
pause
