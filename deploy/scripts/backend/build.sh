#!/bin/bash
# ============================================================
#  北极星AI - 后端构建脚本
#  基于 Maven 多模块构建最佳实践
#  整合: Spring Boot官方CI指南 + Jenkins Pipeline最佳实践
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"
source "$SCRIPT_DIR/../lib/env.sh"

usage() {
    cat << EOF
北极星AI后端构建脚本

用法: $(basename $0) [选项] [服务名]

选项:
    -h, --help          显示帮助信息
    -e, --env ENV       环境选择 (dev|prod, 默认: dev)
    -s, --service SVC   构建指定服务 (不指定则构建全部)
    --skip-tests        跳过单元测试
    --clean             执行clean清理
    --check-mariadb     验证MariaDB驱动包含
    --parallel          并行构建子模块
    -o, --output DIR    输出目录 (默认: target/release)

示例:
    $(basename $0)                          # 构建全部
    $(basename $0) -s bx-user               # 构建用户服务
    $(basename $0) -s bx-user --clean       # 清理后构建用户服务
    $(basename $0) --parallel --skip-tests  # 并行跳测构建

EOF
}

PARALLEL=false
SKIP_TESTS=false
DO_CLEAN=false
CHECK_MARIADB=false
SERVICE_NAME=""
OUTPUT_DIR=""
DEPLOY_ENV="dev"

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help) usage; exit 0 ;;
        -e|--env) DEPLOY_ENV=$2; shift 2 ;;
        -s|--service) SERVICE_NAME=$2; shift 2 ;;
        --skip-tests) SKIP_TESTS=true; shift ;;
        --clean) DO_CLEAN=true; shift ;;
        --check-mariadb) CHECK_MARIADB=true; shift ;;
        --parallel) PARALLEL=true; shift ;;
        -o|--output) OUTPUT_DIR=$2; shift 2 ;;
        *) log_error "未知参数: $1"; usage; exit 1 ;;
    esac
done

load_env "$DEPLOY_ENV" || exit 1

BACKEND_DIR=$(get_backend_dir)
[[ -z "$OUTPUT_DIR" ]] && OUTPUT_DIR="$BACKEND_DIR/target/release"

ALL_SERVICES=(
    bx-gateway
    bx-user
    bx-tenant
    bx-content
    bx-lead
    bx-risk
    bx-billing
    bx-ai
    bx-message
    bx-storage
    bx-web
    bx-system
    bx-data
    bx-social
    bx-schedule
    bx-monitor
)

if [[ -n "$SERVICE_NAME" ]]; then
    if [[ ! -d "$BACKEND_DIR/$SERVICE_NAME" ]]; then
        log_error "服务目录不存在: $BACKEND_DIR/$SERVICE_NAME"
        exit 1
    fi
    BUILD_SERVICES=("$SERVICE_NAME")
else
    BUILD_SERVICES=("${ALL_SERVICES[@]}")
fi

log_info "=========================================="
log_info "北极星AI后端构建"
log_info "环境: $DEPLOY_ENV | 服务数: ${#BUILD_SERVICES[@]}"
log_info "时间: $(now)"
log_info "=========================================="

check_prerequisites java mvn || {
    log_error "缺少必需工具，请先安装Java和Maven"
    exit 1
}

java -version 2>&1 | head -1
mvn -version 2>&1 | head -1

MAVEN_OPTS="${MAVEN_OPTS:--Xms512m -Xmx1024m}"
export MAVEN_OPTS

MVN_ARGS=""
[[ "$SKIP_TESTS" == "true" ]] && MVN_ARGS="$MVN_ARGS -DskipTests"
[[ "$DO_CLEAN" == "true" ]] && MVN_ARGS="$MVN_ARGS clean"
[[ "$PARALLEL" == "true" ]] && MVN_ARGS="$MVN_ARGS -T 1C"

START_TIME=$(date +%s)

if [[ "${#BUILD_SERVICES[@]}" -eq 1 && -n "$SERVICE_NAME" ]]; then
    log_info "------------------------------------------"
    log_info "构建单模块: $SERVICE_NAME"
    log_info "------------------------------------------"
    
    cd "$BACKEND_DIR/$SERVICE_NAME"
    
    local mvn_cmd="mvn package $MVN_ARGS"
    
    log_info "执行: $mvn_cmd"
    
    if eval "$mvn_cmd"; then
        log_success "[$SERVICE_NAME] 构建成功"
    else
        log_error "[$SERVICE_NAME] 构建失败!"
        exit 1
    fi
    
    JAR_FILE="$BACKEND_DIR/$SERVICE_NAME/target/$SERVICE_NAME-1.0.0-SNAPSHOT.jar"
    if [[ -f "$JAR_FILE" ]]; then
        JAR_SIZE=$(ls -lh "$JAR_FILE" | awk '{print $5}')
        log_success "JAR产物: $JAR_FILE ($JAR_SIZE)"
    else
        log_warn "未找到预期JAR文件，检查target目录..."
        ls -la "$BACKEND_DIR/$SERVICE_NAME/target/"*.jar 2>/dev/null || true
    fi
else
    log_info "------------------------------------------"
    log_info "构建全量模块 (${#BUILD_SERVICES[@]} 个)"
    log_info "------------------------------------------"
    
    cd "$BACKEND_DIR"
    
    local mvn_cmd="mvn package $MVN_ARGS -pl"
    local first=true
    for svc in "${BUILD_SERVICES[@]}"; do
        if [[ -d "$svc" ]]; then
            [[ "$first" == "true" ]] && first=false || mvn_cmd="$mvn_cmd,"
            mvn_cmd="$mvn_cmd$svc"
        fi
    done
    mvn_cmd="$mvn_cmd -am"
    
    log_info "执行: $mvn_cmd"
    
    if eval "$mvn_cmd"; then
        log_success "全量构建完成"
    else
        log_error "全量构建失败!"
        exit 1
    fi
fi

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

log_info "=========================================="
log_info "  构建结果统计"
log_info "=========================================="

OK_COUNT=0
FAIL_COUNT=0
TOTAL_SIZE=0

for svc in "${BUILD_SERVICES[@]}"; do
    JAR_FILE="$BACKEND_DIR/$svc/target/$svc-1.0.0-SNAPSHOT.jar"
    
    if [[ -f "$JAR_FILE" ]]; then
        SIZE=$(ls -lh "$JAR_FILE" | awk '{print $5}')
        log_success "[OK]   $svc ($SIZE)"
        OK_COUNT=$((OK_COUNT + 1))
    else
        log_error "[FAIL] $svc (无JAR文件)"
        FAIL_COUNT=$((FAIL_COUNT + 1))
    fi
done

if [[ "$CHECK_MARIADB" == "true" ]]; then
    log_info ""
    log_info "=========================================="
    log_info "  MariaDB驱动验证"
    log_info "=========================================="
    
    TEST_JAR="$BACKEND_DIR/bx-user/target/bx-user-1.0.0-SNAPSHOT.jar"
    if [[ -f "$TEST_JAR" ]]; then
        MARIADB_COUNT=$(jar tf "$TEST_JAR" 2>/dev/null | grep -i mariadb | wc -l)
        if [[ "$MARIADB_COUNT" -gt 0 ]]; then
            log_success "MariaDB驱动已包含 (${MARIADB_COUNT} 个文件)"
        else
            log_error "MariaDB驱动未找到! 请检查pom.xml依赖配置"
        fi
    else
        log_error "bx-user JAR不存在，无法验证MariaDB驱动"
    fi
fi

log_info ""
log_info "=========================================="
log_info "  结果: $OK_COUNT 成功, $FAIL_COUNT 失败"
log_info "  耗时: ${DURATION}s"
log_info "=========================================="

if [[ "$FAIL_COUNT" -gt 0 ]]; then
    exit 1
fi

exit 0
