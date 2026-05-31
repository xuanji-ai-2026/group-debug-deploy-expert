#!/bin/bash

# 北极星AI商机获客系统 - 一键启动脚本
# 注意：Gateway使用8070端口，避免与openclaw冲突

PROJECT_ROOT="/workspace/projects/workspace/projects/beijixing-ai"
BACKEND_DIR="$PROJECT_ROOT/backend"
LOG_DIR="/tmp/beijixing"

mkdir -p "$LOG_DIR"

# 停止所有服务
echo "停止现有服务..."
pkill -f "bx-.*\.jar" 2>/dev/null
sleep 3

echo "启动服务..."

# 1. 用户服务
echo "1. bx-user (8081)"
nohup java -jar "$BACKEND_DIR/bx-user/target/bx-user-1.0.0-SNAPSHOT.jar" > "$LOG_DIR/bx-user.log" 2>&1 &
sleep 12

# 2. 网关服务（安全端口8070）
echo "2. bx-gateway (8070)"
nohup java -jar "$BACKEND_DIR/bx-gateway/target/bx-gateway-1.0.0-SNAPSHOT.jar" > "$LOG_DIR/bx-gateway.log" 2>&1 &
sleep 15

# 3. 租户服务
echo "3. bx-tenant (8082)"
nohup java -jar "$BACKEND_DIR/bx-tenant/target/bx-tenant-1.0.0-SNAPSHOT.jar" > "$LOG_DIR/bx-tenant.log" 2>&1 &
sleep 10

# 4. 商机服务
echo "4. bx-lead (8084)"
nohup java -jar "$BACKEND_DIR/bx-lead/target/bx-lead-1.0.0-SNAPSHOT.jar" > "$LOG_DIR/bx-lead.log" 2>&1 &
sleep 10

# 5. 内容服务
echo "5. bx-content (8083)"
nohup java -jar "$BACKEND_DIR/bx-content/target/bx-content-1.0.0-SNAPSHOT.jar" > "$LOG_DIR/bx-content.log" 2>&1 &
sleep 10

# 6. AI服务
echo "6. bx-ai (8085)"
nohup java -jar "$BACKEND_DIR/bx-ai/target/bx-ai-1.0.0-SNAPSHOT.jar" > "$LOG_DIR/bx-ai.log" 2>&1 &
sleep 10

# 7. 消息服务
echo "7. bx-message (8086)"
nohup java -jar "$BACKEND_DIR/bx-message/target/bx-message-1.0.0-SNAPSHOT.jar" > "$LOG_DIR/bx-message.log" 2>&1 &
sleep 10

echo ""
echo "所有服务已启动！"
echo "网关地址: http://localhost:8070"
echo "用户服务: http://localhost:8081"
echo ""
echo "查看日志: ls -la $LOG_DIR/"
