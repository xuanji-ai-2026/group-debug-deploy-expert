@echo off
chcp 65001 >nul
echo ============================================
echo   bx-user模块快速编译脚本（含腾讯云SMS）
echo ============================================
echo.

set MAVEN_HOME=D:\BeijiXing-AI\tools\apache-maven-3.9.6
set PROJECT_DIR=D:\BeijiXing-AI\backend

echo [1/4] 检查Maven环境...
if exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo     ✓ Maven已安装: %MAVEN_HOME%
) else (
    echo     ✗ Maven未找到！请先运行安装脚本
    pause
    exit /b 1
)

echo.
echo [2/4] 清理旧的编译产物...
if exist "%PROJECT_DIR%\bx-user\target" (
    rmdir /s /q "%PROJECT_DIR%\bx-user\target"
    echo     ✓ 已清理target目录
)

echo.
echo [3/4] 开始编译bx-user模块...
echo     这可能需要1-2分钟...
echo.
call "%MAVEN_HOME%\bin\mvn.cmd" clean package -pl bx-user -am -DskipTests -f "%PROJECT_DIR%\pom.xml"

echo.
echo [4/4] 检查编译结果...
if exist "%PROJECT_DIR%\bx-user\target\*.jar" (
    echo.
    echo ============================================
    echo   ✅ 编译成功！
    echo ============================================
    for %%i in ("%PROJECT_DIR%\bx-user\target\*.jar") do (
        echo     JAR文件: %%~nxi
    )
    echo.
    echo 下一步操作：
    echo   1. 将JAR文件上传到远程服务器
    echo   2. 重启bx-user服务：systemctl restart bx-user
    echo   3. 测试短信发送功能
) else (
    echo.
    echo ============================================
    echo   ❌ 编译失败！
    echo ============================================
    echo 请检查上面的错误信息
)

echo.
pause
