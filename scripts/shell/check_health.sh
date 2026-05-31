#!/bin/bash

# 健康检查脚本

echo "=== 北极星AI服务健康检查 ==="
echo ""

echo "bx-user (8081):"
curl -s http://localhost:8081/bx-user/actuator/health 2>/dev/null || echo "未运行"

echo ""
echo "bx-gateway (8070):"
curl -s http://localhost:8070/actuator/health 2>/dev/null || echo "未运行"

echo ""
echo "bx-tenant (8082):"
curl -s http://localhost:8082/bx-tenant/actuator/health 2>/dev/null || echo "未运行"

echo ""
echo "bx-lead (8084):"
curl -s http://localhost:8084/bx-lead/actuator/health 2>/dev/null || echo "未运行"
