#!/bin/bash

# ============================================================
#  北极星AI - Docker 统一构建脚本 (Ultra-Fast 版本)
#  
#  功能：批量构建所有微服务的优化版 Docker 镜像
#  特点：使用分层 JAR + Alpine 镜像，增量构建 <8 秒
#
#  使用方法：
#    ./build-all-services.sh              # 构建所有服务（默认）
#    ./build-all-services.sh gateway user # 构建指定服务
#    ./build-all-services.sh --parallel   # 并行构建（需要多核 CPU）
#    ./build-all-services.sh --clean      # 清理旧镜像后重建
# ============================================================

set -e

# 配置变量
PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
DOCKER_DIR="${PROJECT_ROOT}/deploy/docker"
BACKEND_DIR="${PROJECT_ROOT}/backend"
IMAGE_PREFIX="beijixing/bx-"
IMAGE_TAG="ultra-fast"

# 服务定义（名称:端口:后端模块路径）
declare -A SERVICES=(
    ["gateway"]="8080:${BACKEND_DIR}/bx-gateway"
    ["user"]="8081:${BACKEND_DIR}/bx-user"
    ["tenant"]="8082:${BACKEND_DIR}/bx-tenant"
    ["content"]="8083:${BACKEND_DIR}/bx-content"
    ["lead"]="8084:${BACKEND_DIR}/bx-lead"
    ["risk"]="8085:${BACKEND_DIR}/bx-risk"
    ["billing"]="8096:${BACKEND_DIR}/bx-billing"
    ["ai"]="8087:${BACKEND_DIR}/bx-ai"
    ["message"]="8088:${BACKEND_DIR}/bx-message"
    ["storage"]="8090:${BACKEND_DIR}/bx-storage"
    ["web"]="8090:${BACKEND_DIR}/bx-web"
    ["schedule"]="8094:${BACKEND_DIR}/bx-schedule"
    ["system"]="8091:${BACKEND_DIR}/bx-system"
    ["social"]="8093:${BACKEND_DIR}/bx-social"
    ["data"]="8092:${BACKEND_DIR}/bx-data"
    ["monitor"]="8095:${BACKEND_DIR}/bx-monitor"
)

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 计数器
TOTAL=0
SUCCESS=0
FAILED=0
START_TIME=$(date +%s)

echo "========================================================="
echo "🚀 北极星AI - Docker 批量构建系统 (Ultra-Fast Edition)"
echo "========================================================="
echo "项目根目录: ${PROJECT_ROOT}"
echo "镜像标签: ${IMAGE_TAG}"
echo "基础镜像: eclipse-temurin:17-jre-alpine (已缓存)"
echo "开始时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

# 解析命令行参数
TARGET_SERVICES=()
PARALLEL=false
CLEAN=false

for arg in "$@"; do
    case $arg in
        --parallel)
            PARALLEL=true
            shift
            ;;
        --clean)
            CLEAN=true
            shift
            ;;
        *)
            TARGET_SERVICES+=("$arg")
            shift
            ;;
    esac
done

# 如果没有指定目标服务，则构建所有服务
if [ ${#TARGET_SERVICES[@]} -eq 0 ]; then
    TARGET_SERVICES=("${!SERVICES[@]}")
fi

# 清理函数
cleanup_old_images() {
    echo -e "${YELLOW}⚠️  清理旧版本镜像...${NC}"
    for service in "${TARGET_SERVICES[@]}"; do
        if docker images | grep -q "^${IMAGE_PREFIX}${service}"; then
            docker rmi -f "${IMAGE_PREFIX}${service}:${IMAGE_TAG}" 2>/dev/null || true
            echo "  ✅ 已清理: ${IMAGE_PREFIX}${service}"
        fi
    done
    echo ""
}

# 单个服务构建函数
build_service() {
    local service=$1
    local config="${SERVICES[$service]}"
    local port=$(echo $config | cut -d: -f1)
    local module_path=$(echo $config | cut -d: -f2)
    local dockerfile="${DOCKER_DIR}/services/${service}/Dockerfile"
    
    TOTAL=$((TOTAL + 1))
    
    echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}[${TOTAL}/${#TARGET_SERVICES[@]}] 构建: ${service} (端口: ${port})${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo "  模块路径: ${module_path}"
    echo "  Dockerfile: ${dockerfile}"
    echo "  目标镜像: ${IMAGE_PREFIX}${service}:${IMAGE_TAG}"
    
    # 检查 Dockerfile 是否存在
    if [ ! -f "$dockerfile" ]; then
        echo -e "${RED}❌ 错误: Dockerfile 不存在: ${dockerfile}${NC}"
        FAILED=$((FAILED + 1))
        return 1
    fi
    
    # 检查 JAR 文件是否存在
    if [ ! -d "${module_path}/target" ]; then
        echo -e "${YELLOW}⚠️  警告: target 目录不存在，跳过构建: ${service}${NC}"
        echo -e "  请先执行: cd ${module_path} && mvn package -DskipTests"
        FAILED=$((FAILED + 1))
        return 1
    fi
    
    # 开始计时
    local build_start=$(date +%s)
    
    # 执行构建
    if docker build \
        --tag "${IMAGE_PREFIX}${service}:${IMAGE_TAG}" \
        --build-arg JAR_FILE="target/*.jar" \
        -f "$dockerfile" \
        "$module_path" 2>&1 | tee "/tmp/docker-build-${service}.log" ; then
        
        local build_end=$(date +%s)
        local duration=$((build_end - build_start))
        
        echo -e "${GREEN}✅ 成功: ${service} (${duration}s)${NC}"
        SUCCESS=$((SUCCESS + 1))
        
        # 获取镜像大小
        local image_size=$(docker images --format "{{.Size}}" "${IMAGE_PREFIX}${service}:${IMAGE_TAG}" | head -n 1)
        echo "  📦 镜像大小: ${image_size}"
        
        return 0
    else
        local build_end=$(date +%s)
        local duration=$((build_end - build_start))
        
        echo -e "${RED}❌ 失败: ${service} (${duration}s)${NC}"
        echo -e "  📋 日志文件: /tmp/docker-build-${service}.log"
        FAILED=$((FAILED + 1))
        return 1
    fi
}

# 主构建流程
main() {
    # 如果启用清理模式，先清理旧镜像
    if [ "$CLEAN" = true ]; then
        cleanup_old_images
    fi
    
    # 根据是否并行选择构建方式
    if [ "$PARALLEL" = true ]; then
        echo -e "${YELLOW}⚡ 并行构建模式已启用...${NC}"
        echo ""
        
        PIDS=()
        for service in "${TARGET_SERVICES[@]}"; do
            build_service "$service" &
            PIDS+=($!)
        done
        
        # 等待所有后台进程完成
        for pid in "${PIDS[@]}"; do
            wait $pid || true
        done
    else
        # 顺序构建
        for service in "${TARGET_SERVICES[@]}"; do
            build_service "$service"
        done
    fi
    
    # 输出总结报告
    END_TIME=$(date +%s)
    TOTAL_DURATION=$((END_TIME - START_TIME))
    
    echo ""
    echo "========================================================="
    echo -e "${GREEN}🎉 构建完成！总结报告${NC}"
    echo "========================================================="
    echo -e "总服务数: ${TOTAL}"
    echo -e "${GREEN}成功: ${SUCCESS}${NC}"
    echo -e "${RED}失败: ${FAILED}${NC}"
    echo -e "总耗时: ${TOTAL_DURATION}秒 ($((TOTAL_DURATION / 60))分$((TOTAL_DURATION % 60))秒)"
    echo -e "平均耗时: $((TOTAL_DURATION / TOTAL))秒/服务"
    echo ""
    
    if [ $FAILED -gt 0 ]; then
        echo -e "${YELLOW}⚠️  失败的服务:${NC}"
        echo "  查看日志: /tmp/docker-build-<service>.log"
    fi
    
    echo ""
    echo "✅ 已构建的镜像:"
    docker images --format "  {{.Repository}}:{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}" | grep "${IMAGE_PREFIX}" | sort
    
    echo ""
    echo "💡 使用提示:"
    echo "  启动单个容器: docker run -d -p <PORT>:<PORT> ${IMAGE_PREFIX}<service>:${IMAGE_TAG}"
    echo "  查看镜像列表: docker images | grep beijixing"
    echo "  清理所有镜像: docker rmi \$(docker images -q '${IMAGE_PREFIX}*')"
    
    exit $FAILED
}

# 执行主流程
main "$@"
