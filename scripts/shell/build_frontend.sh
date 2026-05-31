#!/bin/bash

# 前端项目构建脚本

echo "=== 构建PC端Web ==="
cd "$PROJECT_PATH/frontend/web-pc"
npm install
npm run build

echo "=== 构建管理后台 ==="
cd "$PROJECT_PATH/frontend/web-admin"
npm install
npm run build

echo "=== 构建完成 ==="
