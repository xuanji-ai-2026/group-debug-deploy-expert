#!/bin/bash

# 北极星AI商机获客系统 - 前端集成测试脚本
# 作者: EMP-FE-QA-001 前端测试工程师
# 日期: 2026-04-08

echo "=========================================="
echo "北极星AI商机获客系统 - 前端集成测试"
echo "=========================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试统计
TEST_TOTAL=0
TEST_PASS=0
TEST_FAIL=0

# 测试函数
test_api_pc() {
    local test_name=$1
    local api_url=$2
    local expected_method=$3

    TEST_TOTAL=$((TEST_TOTAL + 1))
    echo -n "测试 $TEST_TOTAL: $test_name ... "

    # 模拟API调用（检查API文件中是否定义了该方法）
    if grep -r "function ${expected_method}" /workspace/projects/beijixing-ai/frontend/web-pc/src/api/*.js 2>/dev/null > /dev/null; then
        echo -e "${GREEN}✓ 通过${NC}"
        TEST_PASS=$((TEST_PASS + 1))
    else
        echo -e "${RED}✗ 失败${NC}"
        echo -e "  ${YELLOW}未找到 API 方法: ${expected_method}${NC}"
        TEST_FAIL=$((TEST_FAIL + 1))
    fi
}

test_api_admin() {
    local test_name=$1
    local api_url=$2
    local expected_method=$3

    TEST_TOTAL=$((TEST_TOTAL + 1))
    echo -n "测试 $TEST_TOTAL: $test_name ... "

    # 模拟API调用（检查API文件中是否定义了该方法）
    if grep -r "function ${expected_method}" /workspace/projects/beijixing-ai/frontend/web-admin/src/api/*.js 2>/dev/null > /dev/null; then
        echo -e "${GREEN}✓ 通过${NC}"
        TEST_PASS=$((TEST_PASS + 1))
    else
        echo -e "${RED}✗ 失败${NC}"
        echo -e "  ${YELLOW}未找到 API 方法: ${expected_method}${NC}"
        TEST_FAIL=$((TEST_FAIL + 1))
    fi
}

test_file_exist() {
    local test_name=$1
    local file_path=$2

    TEST_TOTAL=$((TEST_TOTAL + 1))
    echo -n "测试 $TEST_TOTAL: $test_name ... "

    if [ -f "$file_path" ]; then
        echo -e "${GREEN}✓ 通过${NC}"
        TEST_PASS=$((TEST_PASS + 1))
    else
        echo -e "${RED}✗ 失败${NC}"
        echo -e "  ${YELLOW}文件不存在: $file_path${NC}"
        TEST_FAIL=$((TEST_FAIL + 1))
    fi
}

echo ""
echo "=========================================="
echo "PC端 API 配置测试"
echo "=========================================="

# PC端 API 测试
test_api_pc "用户登录接口" "login" "login"
test_api_pc "获取当前用户信息" "getCurrentUser" "getCurrentUser"
test_api_pc "获取账户余额" "getBalance" "getBalance"
test_api_pc "获取商机列表" "getLeadList" "getLeadList"
test_api_pc "获取内容列表" "getContentList" "getContentList"
test_api_pc "发送消息" "sendMessage" "sendMessage"
test_api_pc "获取数据看板概览" "getDashboardOverview" "getDashboardOverview"

echo ""
echo "=========================================="
echo "管理端 API 配置测试"
echo "=========================================="

# 管理端 API 测试
test_api_admin "获取租户列表" "getTenantList" "getTenantList"
test_api_admin "获取租户详情" "getTenantDetail" "getTenantDetail"
test_api_admin "审核租户" "approveTenant" "approveTenant"
test_api_admin "获取计费概览" "getBillingOverview" "getBillingOverview"
test_api_admin "获取系统设置" "getSystemSettings" "getSystemSettings"

echo ""
echo "=========================================="
echo "文件完整性测试"
echo "=========================================="

# 文件完整性测试
test_file_exist "PC端环境配置文件" "/workspace/projects/beijixing-ai/frontend/web-pc/.env"
test_file_exist "PC端请求封装文件" "/workspace/projects/beijixing-ai/frontend/web-pc/src/utils/request.js"
test_file_exist "PC端主应用入口" "/workspace/projects/beijixing-ai/frontend/web-pc/src/main.js"
test_file_exist "PC端路由配置" "/workspace/projects/beijixing-ai/frontend/web-pc/src/router/index.js"
test_file_exist "管理端环境配置文件" "/workspace/projects/beijixing-ai/frontend/web-admin/.env"
test_file_exist "管理端请求封装文件" "/workspace/projects/beijixing-ai/frontend/web-admin/src/utils/request.js"
test_file_exist "管理端主应用入口" "/workspace/projects/beijixing-ai/frontend/web-admin/src/main.js"

echo ""
echo "=========================================="
echo "Vue 页面文件统计"
echo "=========================================="

PC_VUE_COUNT=$(find /workspace/projects/beijixing-ai/frontend/web-pc/src/views -name "*.vue" 2>/dev/null | wc -l)
ADMIN_VUE_COUNT=$(find /workspace/projects/beijixing-ai/frontend/web-admin/src/views -name "*.vue" 2>/dev/null | wc -l)

echo -n "PC端 Vue 页面文件数量: "
if [ "$PC_VUE_COUNT" -gt 0 ]; then
    echo -e "${GREEN}$PC_VUE_COUNT 个${NC}"
    TEST_PASS=$((TEST_PASS + 1))
else
    echo -e "${RED}0 个${NC}"
    TEST_FAIL=$((TEST_FAIL + 1))
fi
TEST_TOTAL=$((TEST_TOTAL + 1))

echo -n "管理端 Vue 页面文件数量: "
if [ "$ADMIN_VUE_COUNT" -gt 0 ]; then
    echo -e "${GREEN}$ADMIN_VUE_COUNT 个${NC}"
    TEST_PASS=$((TEST_PASS + 1))
else
    echo -e "${RED}0 个${NC}"
    TEST_FAIL=$((TEST_FAIL + 1))
fi
TEST_TOTAL=$((TEST_TOTAL + 1))

echo ""
echo "=========================================="
echo "API 模块统计"
echo "=========================================="

PC_API_COUNT=$(ls /workspace/projects/beijixing-ai/frontend/web-pc/src/api/*.js 2>/dev/null | wc -l)
ADMIN_API_COUNT=$(ls /workspace/projects/beijixing-ai/frontend/web-admin/src/api/*.js 2>/dev/null | wc -l)

echo -n "PC端 API 模块文件数量: "
if [ "$PC_API_COUNT" -gt 0 ]; then
    echo -e "${GREEN}$PC_API_COUNT 个${NC}"
    TEST_PASS=$((TEST_PASS + 1))
else
    echo -e "${RED}0 个${NC}"
    TEST_FAIL=$((TEST_FAIL + 1))
fi
TEST_TOTAL=$((TEST_TOTAL + 1))

echo -n "管理端 API 模块文件数量: "
if [ "$ADMIN_API_COUNT" -gt 0 ]; then
    echo -e "${GREEN}$ADMIN_API_COUNT 个${NC}"
    TEST_PASS=$((TEST_PASS + 1))
else
    echo -e "${RED}0 个${NC}"
    TEST_FAIL=$((TEST_FAIL + 1))
fi
TEST_TOTAL=$((TEST_TOTAL + 1))

echo ""
echo "=========================================="
echo "测试结果汇总"
echo "=========================================="

echo "总测试数: $TEST_TOTAL"
echo -e "通过: ${GREEN}$TEST_PASS${NC}"
echo -e "失败: ${RED}$TEST_FAIL${NC}"

PASS_RATE=$((TEST_PASS * 100 / TEST_TOTAL))
echo -e "通过率: ${GREEN}$PASS_RATE%${NC}"

if [ $TEST_FAIL -eq 0 ]; then
    echo -e "\n${GREEN}✓ 所有测试通过！前端集成测试完成。${NC}"
    exit 0
else
    echo -e "\n${RED}✗ 存在 $TEST_FAIL 个失败的测试项，请检查并修复。${NC}"
    exit 1
fi
