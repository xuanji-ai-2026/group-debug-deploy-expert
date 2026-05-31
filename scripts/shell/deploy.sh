#!/bin/bash

# 北极星AI商机获客系统 - 部署脚本

set -e

PROJECT_DIR=$(dirname $(dirname $(dirname $(readlink -f $0))))
DEPLOY_DIR="$PROJECT_DIR/deploy/docker"
BACKUP_DIR="/tmp/beijixing-backup-$(date +%Y%m%d-%H%M%S)"

echo "=== 北极星AI商机获客系统 - 开始部署 ==="
echo "备份目录: $BACKUP_DIR"
echo ""

# 创建备份目录
mkdir -p "$BACKUP_DIR"

# 停止现有服务
echo "=== 停止现有服务 ==="
cd "$DEPLOY_DIR"
if [ -f "docker-compose.yml" ]; then
    docker-compose down
    echo "✓ 服务已停止"
else
    echo "✗ docker-compose.yml 不存在"
    exit 1
fi
echo ""

# 备份配置文件
echo "=== 备份配置文件 ==="
cp -r .env* "$BACKUP_DIR/" 2>/dev/null || true
echo "✓ 配置文件已备份"
echo ""

# 启动服务
echo "=== 启动服务 ==="
docker-compose up -d
echo "✓ 服务已启动"
echo ""

# 等待服务就绪
echo "=== 等待服务就绪 ==="
sleep 10
echo "✓ 服务已就绪"
echo ""

# 健康检查
echo "=== 健康检查 ==="
MAX_RETRIES=5
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -f http://localhost:8080/actuator/health >/dev/null 2>&1; then
        echo "✓ 服务健康检查通过"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo "等待服务启动... ($RETRY_COUNT/$MAX_RETRIES)"
    sleep 5
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo "✗ 服务启动超时"
    exit 1
fi
echo ""

echo "=== 部署完成 ==="
echo "访问地址: http://localhost:8080"
echo "备份目录: $BACKUP_DIR"
