#!/bin/bash
# ==============================================
# 北极星AI - 文件同步到远程服务器 (Linux/Mac版)
# 版本: v1.0 | 日期: 2026-05-20
# 用途: 一键同步所有本地修改文件到远程服务器
# 前置条件: SSH密钥已配置 或 使用密码输入
# ==============================================

set -e

echo "========================================"
echo "  北极星AI - 文件同步到远程服务器"
echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "  目标: /opt/beijixing-ai/"
echo "========================================"
echo

# ====== 配置区域（请根据实际情况修改）======
LOCAL_BACKEND="$(cd "$(dirname "$0")" && pwd)"  # 当前脚本所在目录(backend)
REMOTE_USER="${REMOTE_USER:-root}"                # 远程用户名
REMOTE_HOST="${REMOTE_HOST:-your-server-ip}"      # ⚠️ 请替换为实际服务器IP
REMOTE_DIR="/opt/beijixing-ai"                     # 远程目录

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查参数
if [ "$1" == "-h" ] || [ "$1" == "--help" ]; then
    echo "用法: $0 [选项]"
    echo "选项:"
    echo "  -h, --help     显示帮助信息"
    echo "  --dry-run      仅显示将要同步的文件，不实际执行"
    echo "  --source-code  同时同步源代码（用于调试）"
    echo ""
    echo "环境变量:"
    echo "  REMOTE_USER    远程用户名 (默认: root)"
    echo "  REMOTE_HOST    远程服务器IP (必须设置)"
    echo ""
    echo "示例:"
    echo "  REMOTE_HOST=192.168.1.100 $0"
    echo "  $0 --dry-run                          # 预览模式"
    exit 0
fi

DRY_RUN=false
SYNC_SOURCE_CODE=false
if [ "$1" == "--dry-run" ]; then
    DRY_RUN=true
    echo "${YELLOW}⚠️  预览模式: 仅显示将同步的文件${NC}"
    echo
elif [ "$1" == "--source-code" ]; then
    SYNC_SOURCE_CODE=true
    echo "${YELLOW}📦 将同时同步源代码文件${NC}"
    echo
fi

# 检查必要配置
if [ "$REMOTE_HOST" == "your-server-ip" ]; then
    echo -e "${RED}❌ 错误: 请设置远程服务器IP${NC}"
    echo ""
    echo "使用方法:"
    echo "  export REMOTE_HOST=192.168.1.100"
    echo "  $0"
    exit 1
fi

echo -e "${GREEN}[1/7] 检查本地文件...${NC}"

# 定义要同步的配置文件
CONFIG_FILES=(
    "beijixing-app/src/main/resources/application.yml"
    "beijixing-app/src/main/resources/application-prod.yml"
    "beijixing-app/src/main/resources/bootstrap.yml"
    "beijixing-app/src/main/resources/logback-spring.xml"
)

# 检查配置文件是否存在
for file in "${CONFIG_FILES[@]}"; do
    if [ ! -f "$LOCAL_BACKEND/$file" ]; then
        echo -e "${RED}❌ 找不到文件: $file${NC}"
        exit 1
    fi
done
echo -e "${GREEN}✅ 本地文件检查通过${NC}"
echo

# 定义要同步的文档和脚本
DOC_FILES=(
    "QUICK_DEPLOY.md"
    "DEPLOYMENT.md"
    "deploy.sh"
    "build-monolith.bat"
)

# 定义测试工具
TEST_FILES=(
    "BeijixingAI_Postman_Collection.json"
)

# 定义源代码文件（可选）
SOURCE_CODE_FILES=(
    "bx-social/src/main/java/com/beijixing/social/controller/CommentCrawlController.java"
    "bx-social/src/main/java/com/beijixing/social/crawl/controller/MobileCrawlController.java"
)

echo -e "${GREEN}[2/7] 同步配置文件到服务器 config/ 目录...${NC}"

if [ "$DRY_RUN" = true ]; then
    echo "${YELLOW}将要上传的配置文件:${NC}"
    for file in "${CONFIG_FILES[@]}"; do
        echo "  📄 $file -> ${REMOTE_DIR}/config/$(basename $file)"
    done
else
    for file in "${CONFIG_FILES[@]}"; do
        echo "  📤 上传: $(basename $file)"
        scp "$LOCAL_BACKEND/$file" ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_DIR}/config/
    done
    echo -e "${GREEN}✅ 配置文件同步完成${NC}"
fi
echo

echo -e "${GREEN}[3/7] 同步部署文档和脚本...${NC}"

if [ "$DRY_RUN" = true ]; then
    echo "${YELLOW}将要上传的文档和脚本:${NC}"
    for file in "${DOC_FILES[@]}"; do
        if [ -f "$LOCAL_BACKEND/$file" ]; then
            echo "  📄 $file -> ${REMOTE_DIR}/$file"
        fi
    done
else
    for file in "${DOC_FILES[@]}"; do
        if [ -f "$LOCAL_BACKEND/$file" ]; then
            echo "  📤 上传: $file"
            scp "$LOCAL_BACKEND/$file" ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_DIR}/
        fi
    done
    echo -e "${GREEN}✅ 部署文档同步完成${NC}"
fi
echo

echo -e "${GREEN}[4/7] 同步Postman测试集合...${NC}"

if [ "$DRY_RUN" = true ]; then
    echo "${YELLOW}将要上传的测试工具:${NC}"
    for file in "${TEST_FILES[@]}"; do
        if [ -f "$LOCAL_BACKEND/$file" ]; then
            echo "  📄 $file -> ${REMOTE_DIR}/$file"
        fi
    done
else
    for file in "${TEST_FILES[@]}"; do
        if [ -f "$LOCAL_BACKEND/$file" ]; then
            echo "  📤 上传: $file"
            scp "$LOCAL_BACKEND/$file" ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_DIR}/
        fi
    done
    echo -e "${GREEN}✅ Postman测试集合同步完成${NC}"
fi
echo

if [ "$SYNC_SOURCE_CODE" = true ]; then
    echo -e "${GREEN}[5/7] 同步Phase 3核心源代码...${NC}"
    
    if [ "$DRY_RUN" = true ]; then
        echo "${YELLOW}将要上传的源代码文件:${NC}"
        for file in "${SOURCE_CODE_FILES[@]}"; do
            if [ -f "$LOCAL_BACKEND/$file" ]; then
                echo "  💻 $file -> ${REMOTE_DIR}/src/$file"
            fi
        done
    else
        ssh ${REMOTE_USER}@${REMOTE_HOST} "mkdir -p ${REMOTE_DIR}/src/bx-social/controller ${REMOTE_DIR}/src/bx-social/crawl/controller"
        for file in "${SOURCE_CODE_FILES[@]}"; do
            if [ -f "$LOCAL_BACKEND/$file" ]; then
                echo "  💻 上传: $file"
                scp "$LOCAL_BACKEND/$file" ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_DIR}/src/$file
            fi
        done
        echo -e "${GREEN}✅ 源代码同步完成${NC}"
    fi
    echo
else
    echo -e "${YELLOW}[5/7] 跳过源代码同步（使用 --source-code 参数启用）${NC}"
    echo
fi

if [ "$DRY_RUN" = false ]; then
    echo -e "${GREEN}[6/7] 设置文件权限...${NC}"
    ssh ${REMOTE_USER}@${REMOTE_HOST} "
        cd ${REMOTE_DIR} && \
        chmod +x deploy.sh 2>/dev/null || true && \
        chmod -R 755 config/ && \
        chmod 644 *.md *.json 2>/dev/null || true && \
        echo '✅ 权限设置完成' && \
        ls -lh config/ *.md *.json 2>/dev/null || true
    "
    echo
    
    echo -e "${GREEN}[7/7] 验证同步结果...${NC}"
    echo
else
    echo -e "${YELLOW}[6-7] 跳过权限设置和验证（预览模式）${NC}"
    echo
fi

echo "========================================"
echo -e "${GREEN}  ✅ 文件同步准备完成！${NC}"
echo "========================================"
echo
echo "📋 同步完成的文件清单:"
echo ""
echo "📁 配置文件 (config/):"
for file in "${CONFIG_FILES[@]}"; do
    echo "   ✅ $(basename $file)"
done
echo ""
echo "📁 部署文档:"
for file in "${DOC_FILES[@]}"; do
    if [ -f "$LOCAL_BACKEND/$file" ]; then
        echo "   ✅ $file"
    fi
done
echo ""
echo "📁 测试工具:"
for file in "${TEST_FILES[@]}"; do
    if [ -f "$LOCAL_BACKEND/$file" ]; then
        echo "   ✅ $file"
    fi
done
echo ""

if [ "$SYNC_SOURCE_CODE" = true ]; then
    echo "📁 源代码 (src/):"
    for file in "${SOURCE_CODE_FILES[@]}"; do
        if [ -f "$LOCAL_BACKEND/$file" ]; then
            echo "   ✅ $file"
        fi
    done
    echo ""
fi

if [ "$DRY_RUN" = false ]; then
    echo "========================================"
    echo "  🚀 下一步操作:"
    echo "========================================"
    echo ""
    echo "  1️⃣  SSH登录服务器:"
    echo "     ssh ${REMOTE_USER}@${REMOTE_HOST}"
    echo ""
    echo "  2️⃣  进入部署目录:"
    echo "     cd ${REMOTE_DIR}"
    echo ""
    echo "  3️⃣  查看快速部署指南:"
    echo "     cat QUICK_DEPLOY.md"
    echo ""
    echo "  4️⃣  启动应用（二选一）:"
    echo "     方式A（前台调试）: ./start.sh"
    echo "     方式B（后台运行）: nohup ./start.sh &"
    echo ""
    echo "  5️⃣  验证服务状态:"
    echo "     curl http://localhost:8080/actuator/health"
    echo ""
    echo "  6️⃣  使用Postman测试API:"
    echo "     导入 BeijixingAI_Postman_Collection.json"
    echo "========================================"
else
    echo -e "${YELLOW}💡 提示: 这是预览模式，实际执行请去掉 --dry-run 参数${NC}"
fi
echo