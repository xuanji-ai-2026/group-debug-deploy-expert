#!/bin/bash

# ===========================================
#   AI 协作系统 - 快速启动脚本（Linux/Mac）
#   《项目经理驱动的三权分立防幻觉系统》
# ===========================================

set -e

echo "============================================"
echo "  AI 协作系统 - 快速启动脚本"
echo "  《项目经理驱动的三权分立防幻觉系统》"
echo "============================================"
echo ""

# 检查Python是否安装
if ! command -v python3 &> /dev/null; then
    echo "[错误] 未检测到Python3，请先安装Python 3.8+"
    exit 1
fi

echo "[步骤1/5] 检测Python版本..."
python3 --version

echo ""
echo "[步骤2/5] 安装依赖包..."
pip3 install -r requirements.txt -q || echo "[警告] 部分依赖安装失败，系统可能无法完整运行"

echo ""
echo "[步骤3/5] 初始化项目结构..."
python3 -c "from dispatcher import DispatcherEngine; engine = DispatcherEngine('.'); engine.enforce_directory_structure(); print('✓ 项目结构初始化完成')"

echo ""
echo "[步骤4/5] 验证模块导入..."
python3 -c "import models; import tools; import config; import dispatcher; print('✓ 所有模块导入成功')"

echo ""
echo "[步骤5/5] 启动调度师引擎..."
echo ""
echo "╔══════════════════════════════════════╗"
echo "║     🚀 调度师引擎正在启动...        ║"
echo "╚════════════════════════════════════╝"
echo ""

# 启动主程序
python3 main.py .
