#!/bin/bash

# 北极星AI - 单体应用停止脚本 (Linux版)

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
PID_FILE="app.pid"
LOG_FILE="app.log"

cd "$APP_DIR"

echo -e "${YELLOW}停止北极星AI单体应用...${NC}"

if [ ! -f "$PID_FILE" ]; then
    echo -e "${YELLOW}未找到PID文件，尝试通过端口查找进程...${NC}"
    PID=$(netstat -tlnp 2>/dev/null | grep ":8080" | awk '{print $7}' | cut -d'/' -f1)
    
    if [ -z "$PID" ]; then
        echo -e "${GREEN}✓ 应用未在运行${NC}"
        exit 0
    fi
else
    PID=$(cat "$PID_FILE")
fi

if kill -0 $PID 2>/dev/null; then
    echo "找到进程 PID: $PID"
    echo "正在停止..."
    
    # 发送SIGTERM信号
    kill $PID 2>/dev/null || true
    
    # 等待5秒
    sleep 5
    
    # 检查是否已停止
    if kill -0 $PID 2>/dev/null; then
        echo "进程仍在运行，强制终止..."
        kill -9 $PID 2>/dev/null || true
        sleep 2
    fi
    
    if ! kill -0 $PID 2>/dev/null; then
        echo -e "${GREEN}✓ 应用已成功停止${NC}"
        rm -f "$PID_FILE"
    else
        echo -e "${RED}❌ 无法停止进程${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}进程不存在 (PID: $PID)${NC}"
    rm -f "$PID_FILE"
fi

echo ""
echo -e "日志文件保留在: ${APP_DIR}/${LOG_FILE}"
