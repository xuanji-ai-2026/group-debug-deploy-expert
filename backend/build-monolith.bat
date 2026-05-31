@echo off
chcp 65001 >nul
echo ============================================
echo  北极星AI - 极简单体应用构建脚本
echo  整合16个微服务为单一JAR包
echo ============================================
echo.

set "PROJECT_DIR=%~dp0..\"
set "MODULE_DIR=%PROJECT_DIR%beijixing-app"
set "TARGET_DIR=%MODULE_DIR%\target"
set "JAR_NAME=beijixing-app-1.0.0-SNAPSHOT.jar"

echo [1/5] 检查Java环境...
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: 未找到Java，请安装JDK 17+
    pause
    exit /b 1
)
echo ✓ Java环境正常

echo.
echo [2/5] 检查Maven环境...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: 未找到Maven，请安装Maven 3.8+
    pause
    exit /b 1
)
echo ✓ Maven环境正常

echo.
echo [3/5] 清理旧的构建产物...
call mvn clean -f "%PROJECT_DIR%pom.xml" -q
if errorlevel 1 (
    echo ❌ 清理失败
    pause
    exit /b 1
)
echo ✓ 清理完成

echo.
echo [4/5] 构建极简单体应用 (跳过测试)...
echo 这可能需要3-10分钟，取决于网络速度...
echo.
call mvn package -f "%MODULE_DIR%\pom.xml" -DskipTests -q
if errorlevel 1 (
    echo.
    echo ❌ 构建失败！请检查上方错误信息
    echo.
    echo 常见问题:
    echo   1. 编译错误: 检查Java版本是否为17
    echo   2. 依赖下载失败: 检查网络连接或Maven镜像配置
    echo   3. 端口冲突: 确保8080端口未被占用
    pause
    exit /b 1
)
echo ✓ 构建成功！

echo.
echo [5/5] 验证JAR包...
if exist "%TARGET_DIR%%JAR_NAME%" (
    for %%A in ("%TARGET_DIR%%JAR_NAME}") do set SIZE=%%~zA
    echo ✓ JAR包已生成: %JAR_NAME%
    echo   大小: %SIZE% bytes
    echo   路径: %TARGET_DIR%%JAR_NAME%
) else (
    echo ⚠ JAR包未找到，尝试查找实际文件...
    dir /b "%TARGET_DIR%*.jar" 2>nul
)

echo.
echo ============================================
echo  ✅ 构建完成！
echo ============================================
echo.
echo 下一步操作:
echo.
echo 方式1 - 本地测试运行:
echo   cd /d "%MODULE_DIR%"
echo   java -jar target\%JAR_NAME%
echo.
echo 方式2 - 手动上传到服务器 (推荐):
echo   1. 打开文件管理器，导航到:
echo      %TARGET_DIR%
echo   2. 上传 %JAR_NAME% 到服务器: /opt/beijixing-ai/
echo   3. 参考 deploy-monolith-guide.md 完成部署
echo.
echo 方式3 - 使用SCP命令行上传:
echo   scp "%TARGET_DIR%%JAR_NAME%" root@43.160.237.122:/opt/beijixing-ai/
echo.
pause
