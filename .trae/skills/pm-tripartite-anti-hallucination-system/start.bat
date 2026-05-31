@echo off
chcp 65001 >nul
echo ============================================
echo   AI 协作系统 - 快速启动脚本
echo   《项目经理驱动的三权分立防幻觉系统》
echo ============================================
echo.

REM 检查Python是否安装
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到Python，请先安装Python 3.8+
    pause
    exit /b 1
)

echo [步骤1/5] 检测Python版本...
python --version

echo.
echo [步骤2/5] 安装依赖包...
pip install -r requirements.txt -q
if %errorlevel% neq 0 (
    echo [警告] 部分依赖安装失败，系统可能无法完整运行
)

echo.
echo [步骤3/5] 初始化项目结构...
python -c "from dispatcher import DispatcherEngine; engine = DispatcherEngine('.'); engine.enforce_directory_structure(); print('✓ 项目结构初始化完成')"

echo.
echo [步骤4/5] 验证模块导入...
python -c "import models; import tools; import config; import dispatcher; print('✓ 所有模块导入成功')"

echo.
echo [步骤5/5] 启动调度师引擎...
echo.
echo ╔══════════════════════════════════════╗
echo ║     🚀 调度师引擎正在启动...        ║
echo ╚════════════════════════════════════╝
echo.

REM 启动主程序（使用当前目录作为项目目录）
python main.py .

pause
