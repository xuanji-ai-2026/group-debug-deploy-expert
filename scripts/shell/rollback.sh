#!/bin/bash

# 北极星AI商机获客系统 - 回滚脚本

set -e

BACKUP_DIR=$1
PROJECT_DIR=$(dirname $(dirname $(dirname $(readlink -f $0))))
DEPLOY_DIR="$PROJECT_DIR/deploy/docker"

if [ -z "$BACKUP_DIR" ]; then
    echo "用法: $0 <备份目录>"
    echo "示例: $0 /tmp/beijixing-backup-20240101-100000"
    exit 1
fi

if [ ! -d "$BACKUP_DIR" ]; then
    echo "错误: 备份目录不存在: $BACKUP_DIR"
    exit 1
fi

echo "=== 北极星AI商机获客系统 - 开始回滚 ==="
echo "备份目录: $BACKUP_DIR"
echo ""

# 停止当前服务
echo "=== 停止当前服务 ==="
cd "$DEPLOY_DIR"
docker-compose down
echo "✓ 服务已停止"
echo ""

# 恢复配置文件
echo "=== 恢复配置文件 ==="
if [ -f "$BACKUP_DIR/.env" ]; then
    cp "$BACKUP_DIR/.env" "$DEPLOY_DIR/"
    echo "✓ .env 已恢复"
fi

if [ -f "$BACKUP_DIR/.env.prod" ]; then
    cp "$BACKUP_DIR/.env.prod" "$DEPLOY_DIR/"
    echo "✓ .env.prod 已恢复"
fi
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

echo "=== 回滚完成 ==="
echo "访问地址: http://localhost:8080"
