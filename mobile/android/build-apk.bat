@echo off
REM =============================================
REM 北极星AI商机获客系统 - 一键构建APK脚本
REM 支持Windows环境
REM =============================================

echo.
echo ========================================
echo   北极星AI - Android APK 构建工具
echo ========================================
echo.

REM 检查Java环境
java -version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [错误] 未检测到Java环境，请安装JDK 17+
    echo 下载地址: https://adoptium.net/
    pause
    exit /b 1
)

echo [✓] Java环境正常

REM 检查Android SDK
if exist "%ANDROID_HOME%" (
    echo [✓] Android SDK已找到: %ANDROID_HOME%
) else (
    echo [警告] 未设置ANDROID_HOME环境变量
    echo 尝试使用本地SDK...
)

echo.
echo 请选择构建类型:
echo   1. Debug版本（开发测试用）
echo   2. Release版本（生产发布用）
echo   3. 清理并重新构建
echo   0. 退出
echo.

set /p choice=请输入选项 (0-3):

if "%choice%"=="1" goto build_debug
if "%choice%"=="2" goto build_release
if "%choice%"=="3" goto clean_build
if "%choice%"=="0" goto end

echo 无效选项
pause
goto end

:build_debug
echo.
echo [构建] 开始构建Debug APK...
call gradlew.bat assembleDebug --no-daemon
if %ERRORLEVEL% equ 0 (
    echo.
    echo [成功] Debug APK构建完成！
    echo 输出路径: app\build\outputs\apk\debug\app-debug.apk
    explorer app\build\outputs\apk\debug\
) else (
    echo [失败] 构建出错，请检查日志
)
goto end

:build_release
echo.
echo [构建] 开始构建Release APK...
call gradlew.bat assembleRelease --no-daemon
if %ERRORLEVEL% equ 0 (
    echo.
    echo [成功] Release APK构建完成！
    echo 输出路径: app\build\outputs\apk\release\app-release.apk
    explorer app\build\outputs\apk\release\
) else (
    echo [失败] 构建出错，请检查日志
)
goto end

:clean_build
echo.
echo [清理] 正在清理旧构建产物...
call gradlew.bat clean --no-daemon
echo.
echo [构建] 重新构建Debug APK...
call gradlew.bat assembleDebug --no-daemon
if %ERRORLEVEL% equ 0 (
    echo.
    echo [成功] 清理并重新构建完成！
    explorer app\build\outputs\apk\debug\
) else (
    echo [失败] 构建出错
)
goto end

:end
echo.
pause
