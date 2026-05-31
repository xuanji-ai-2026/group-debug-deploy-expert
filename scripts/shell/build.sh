#!/bin/bash

# 北极星AI商机获客系统 - 构建脚本

set -e

PROJECT_DIR=$(dirname $(dirname $(dirname $(readlink -f $0))))
BACKEND_DIR="$PROJECT_DIR/backend"
FRONTEND_DIR="$PROJECT_DIR/frontend"
VERSION=${VERSION:-$(date +%Y%m%d-%H%M%S)}

echo "=== 北极星AI商机获客系统 - 开始构建 ==="
echo "版本: $VERSION"
echo ""

# 后端构建
echo "=== 开始构建后端 ==="
cd "$BACKEND_DIR"
mvn clean package -DskipTests
echo "✓ 后端构建完成"
echo ""

# PC端构建
echo "=== 开始构建PC端 ==="
if [ -d "$FRONTEND_DIR/web-pc" ]; then
    cd "$FRONTEND_DIR/web-pc"
    npm install
    npm run build
    echo "✓ PC端构建完成"
else
    echo "✗ PC端目录不存在"
fi
echo ""

# 管理端构建
echo "=== 开始构建管理端 ==="
if [ -d "$FRONTEND_DIR/web-admin" ]; then
    cd "$FRONTEND_DIR/web-admin"
    npm install
    npm run build
    echo "✓ 管理端构建完成"
else
    echo "✗ 管理端目录不存在"
fi
echo ""

echo "=== 构建完成 ==="
echo "输出目录:"
echo "  - 后端: $BACKEND_DIR/*/target/*.jar"
echo "  - PC端: $FRONTEND_DIR/web-pc/dist"
echo "  - 管理端: $FRONTEND_DIR/web-admin/dist"
