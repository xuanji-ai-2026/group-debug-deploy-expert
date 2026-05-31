@echo off
chcp 65001 >nul
echo ============================================
echo  北极星AI - 单体应用启动脚本
echo  整合16个微服务为单一JAR包
echo ============================================
echo.

set "APP_DIR=%~dp0"
set "JAR_FILE=beijixing-app-1.0.0-SNAPSHOT.jar"
set "LOG_FILE=app.log"
set "PID_FILE=app.pid"

cd /d "%APP_DIR%"

echo [1/4] 检查JAR文件...
if not exist "%JAR_FILE%" (
    echo ❌ 错误: 未找到 %JAR_FILE%
    echo 请确保部署包已正确解压
    pause
    exit /b 1
)
echo ✓ JAR文件存在 (%JAR_FILE%)

echo.
echo [2/4] 检查Java环境...
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: 未找到Java，请安装JDK 17+
    pause
    exit /b 1
)
echo ✓ Java环境正常

echo.
echo [3/4] 检查端口占用...
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul 2>&1
if not errorlevel 1 (
    echo ⚠️  警告: 端口8080已被占用
    echo 正在尝试终止占用进程...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
        echo 终止进程 PID: %%a
        taskkill /F /PID %%a >nul 2>&1
    )
    timeout /t 2 >nul
) else (
    echo ✓ 端口8080可用
)

echo.
echo [4/4] 启动北极星AI单体应用...
echo.

start "BeijiXing-AI-Monolith" java -Xms256m -Xmx768m ^
    -jar "%JAR_FILE%" ^
    --spring.config.location=application.yml ^
    >> "%LOG_FILE%" 2>&1

echo ✓ 应用正在启动...
echo.
echo ============================================
echo  启动信息:
echo  日志文件: %APP_DIR%%LOG_FILE%
echo  访问地址: http://localhost:8080
echo.
echo 查看启动日志:
echo   tail -f %LOG_FILE% (Linux/Mac)
echo   type %LOG_FILE% (Windows)
echo.
echo 停止应用:
echo   查找进程: netstat -ano | findstr :8080
echo   终止进程: taskkill /F /PID [PID]
echo ============================================
echo.
echo 等待应用启动（约30-60秒）...

timeout /t 35 /nobreak >nul

echo.
echo 检查启动状态...
findstr "北极星AI 极简单体应用启动成功" "%LOG_FILE%" >nul 2>&1
if not errorlevel 1 (
    echo.
    echo ✅✅✅ 应用启动成功！✅✅✅
    echo.
    echo 请访问: http://localhost:8080
) else (
    echo.
    echo ⚠️  应用可能仍在启动中，请查看日志文件:
    echo    type %LOG_FILE%
)

echo.
pause
