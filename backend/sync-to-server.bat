@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
echo ========================================
echo   北极星AI - 文件同步到远程服务器
echo   时间: %date% %time%
echo   目标: /opt/beijixing-ai/
echo ========================================
echo.

set "LOCAL_BACKEND=d:\BeijiXing-AI\backend"
set "REMOTE_USER=root"
set "REMOTE_HOST=your-server-ip"  REM ⚠️ 请替换为实际服务器IP
set "REMOTE_DIR=/opt/beijixing-ai"

echo [1/6] 检查本地文件...
if not exist "%LOCAL_BACKEND%\beijixing-app\src\main\resources\application.yml" (
    echo ❌ 错误: 找不到 application.yml
    pause
    exit /b 1
)
echo ✅ 本地文件检查通过
echo.

echo [2/6] 同步配置文件到服务器 config/ 目录...
scp "%LOCAL_BACKEND%\beijixing-app\src\main\resources\application.yml" %REMOTE_USER%@%REMOTE_HOST%:%REMOTE_DIR%/config/
scp "%LOCAL_BACKEND%\beijixing-app\src\main\resources\application-prod.yml" %REMOTE_USER%@%REMOTE_HOST%:%REMOTE_DIR%/config/
scp "%LOCAL_BACKEND%\beijixing-app\src\main\resources\bootstrap.yml" %REMOTE_USER%@%REMOTE_HOST%:%REMOTE_DIR%/config/
scp "%LOCAL_BACKEND%\beijixing-app\src\main\resources\logback-spring.xml" %REMOTE_USER%@%REMOTE_HOST%:%REMOTE_DIR%/config/
if %errorlevel% neq 0 (
    echo ❌ 配置文件同步失败
    pause
    exit /b 1
)
echo ✅ 配置文件同步完成
echo.

echo [3/6] 同步部署文档和脚本...
scp "%LOCAL_BACKEND%\QUICK_DEPLOY.md" %REMOTE_USER%@%REMOTE_HOST%:%REMOTE_DIR%/
scp "%LOCAL_BACKEND%\DEPLOYMENT.md" %REMOTE_USER%@%REMOTE_HOST%:%REMOTE_DIR%/
scp "%LOCAL_BACKEND%\deploy.sh" %REMOTE_USER%@%REMOTE_HOST%:%REMOTE_DIR%/
scp "%LOCAL_BACKEND%\build-monolith.bat" %REMOTE_USER%@%REMOTE_HOST%:%REMOTE_DIR%/
if %errorlevel% neq 0 (
    echo ❌ 文档同步失败
    pause
    exit /b 1
)
echo ✅ 部署文档同步完成
echo.

echo [4/6] 同步Postman测试集合...
scp "%LOCAL_BACKEND%\BeijixingAI_Postman_Collection.json" %REMOTE_USER%@%REMOTE_HOST%:%REMOTE_DIR%/
if %errorlevel% neq 0 (
    echo ⚠️ Postman集合同步失败（非致命错误）
) else (
    echo ✅ Postman测试集合同步完成
)
echo.

echo [5/6] 设置文件权限...
ssh %REMOTE_USER%@%REMOTE_HOST% "cd %REMOTE_DIR% && chmod +x deploy.sh && chmod -R 755 config/ && ls -lh"
if %errorlevel% neq 0 (
    echo ⚠️ 权限设置失败（请手动执行: chmod +x deploy.sh）
) else (
    echo ✅ 权限设置完成
)
echo.

echo [6/6] 验证同步结果...
echo.
echo ====== 同步完成的文件清单 ======
echo 📁 配置文件:
echo    ├── config/application.yml
echo    ├── config/application-prod.yml
echo    ├── config/bootstrap.yml
echo    └── config/logback-spring.xml
echo.
echo 📁 部署文档:
echo    ├── QUICK_DEPLOY.md (v2.0)
echo    ├── DEPLOYMENT.md
echo    ├── deploy.sh
echo    └── build-monolith.bat
echo.
echo 📁 测试工具:
echo    └── BeijixingAI_Postman_Collection.json
echo.
echo ✅ 所有文件同步完成！
echo.
echo ========================================
echo   下一步操作:
echo   1. SSH登录服务器: ssh %REMOTE_USER%@%REMOTE_HOST%
echo   2. 进入目录: cd %REMOTE_DIR%
echo   3. 查看部署指南: cat QUICK_DEPLOY.md
echo   4. 启动应用: ./start.sh 或 nohup ./start.sh &
echo ========================================
echo.
pause