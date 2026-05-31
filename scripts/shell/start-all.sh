#!/bin/bash
#==============================================================================
# 北极星AI商机获客系统 - 启动所有服务脚本
# Start All Services Script for Beijixing AI Business Acquisition System
#
# 作者: 吴刚 (EMP-SCRIPT-001)
# 职责: 运维开发工程师
#==============================================================================

set -e

#==============================================================================
# 颜色定义
#==============================================================================
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly CYAN='\033[0;36m'
readonly NC='\033[0m'

#==============================================================================
# 日志配置
#==============================================================================
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
readonly LOG_DIR="${PROJECT_ROOT}/logs"
readonly LOG_FILE="${LOG_DIR}/start-all-$(date +%Y%m%d-%H%M%S).log"

#==============================================================================
# 服务依赖顺序定义
#==============================================================================
# 格式: 服务名:依赖服务列表(逗号分隔):启动超时(秒)
declare -a SERVICE_DEPENDENCIES=(
    "redis::10"
    "mysql::30"
    "nacos::60"
    "rabbitmq::30"
    "elasticsearch::60"
    "gateway:redis,mysql,nacos:30"
    "user-service:mysql,redis:30"
    "business-service:mysql,redis,nacos:30"
    "ai-service:nacos,rabbitmq:60"
    "notification-service:rabbitmq,redis:30"
    "admin-service:gateway,user-service:30"
    "frontend:gateway:30"
)

#==============================================================================
# 初始化
#==============================================================================
init() {
    mkdir -p "${LOG_DIR}"
    exec > >(tee -a "${LOG_FILE}")
    exec 2>&1
}

#==============================================================================
# 日志函数
#==============================================================================
log_info() {
    echo -e "${GREEN}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_step() {
    echo -e "${BLUE}[STEP]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

#==============================================================================
# 错误处理
#==============================================================================
error_exit() {
    log_error "$1"
    exit 1
}

trap 'error_exit "脚本执行失败，行号: $LINENO"' ERR

#==============================================================================
# 使用说明
#==============================================================================
usage() {
    cat << EOF
${CYAN}北极星AI商机获客系统 - 启动所有服务${NC}

用法: $0 [选项]

选项:
    -e, --env ENV          指定环境 (dev|prod) [默认: dev]
    -s, --service SERVICE  只启动指定服务
    -f, --foreground       前台运行（不后台化）
    -n, --no-health-check  跳过健康检查
    -h, --help             显示此帮助信息

示例:
    $0 --env prod                           # 生产环境启动所有服务
    $0 --env dev --service user-service     # 只启动用户服务
    $0 --env dev --foreground               # 前台运行所有服务

EOF
}

#==============================================================================
# 参数解析
#==============================================================================
parse_args() {
    ENV="dev"
    TARGET_SERVICE=""
    FOREGROUND=false
    NO_HEALTH_CHECK=false

    while [[ $# -gt 0 ]]; do
        case $1 in
            -e|--env)
                ENV="$2"
                shift 2
                ;;
            -s|--service)
                TARGET_SERVICE="$2"
                shift 2
                ;;
            -f|--foreground)
                FOREGROUND=true
                shift
                ;;
            -n|--no-health-check)
                NO_HEALTH_CHECK=true
                shift
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            *)
                error_exit "未知参数: $1"
                ;;
        esac
    done

    # 验证环境参数
    if [[ ! "$ENV" =~ ^(dev|prod)$ ]]; then
        error_exit "无效的环境参数: $ENV (必须是 dev 或 prod)"
    fi
}

#==============================================================================
# 检查服务是否已在运行
#==============================================================================
is_service_running() {
    local service=$1
    
    # 检查Docker容器
    if docker ps --format '{{.Names}}' | grep -q "^${service}$"; then
        return 0
    fi
    
    # 检查进程
    if pgrep -f "$service" > /dev/null 2>&1; then
        return 0
    fi
    
    return 1
}

#==============================================================================
# 检查依赖服务是否就绪
#==============================================================================
check_dependencies() {
    local deps=$1
    
    if [[ -z "$deps" ]]; then
        return 0
    fi
    
    IFS=',' read -ra DEP_ARRAY <<< "$deps"
    for dep in "${DEP_ARRAY[@]}"; do
        log_info "检查依赖服务: $dep"
        if ! is_service_running "$dep"; then
            log_warn "依赖服务未运行: $dep"
            return 1
        fi
        log_info "✓ 依赖服务就绪: $dep"
    done
    
    return 0
}

#==============================================================================
# 等待服务健康
#==============================================================================
wait_for_service() {
    local service=$1
    local timeout=$2
    local health_url=""
    
    log_info "等待服务健康: $service (超时: ${timeout}秒)"
    
    # 根据服务类型确定健康检查URL
    case $service in
        gateway)
            health_url="http://localhost:8080/actuator/health"
            ;;
        user-service)
            health_url="http://localhost:8081/actuator/health"
            ;;
        business-service)
            health_url="http://localhost:8082/actuator/health"
            ;;
        ai-service)
            health_url="http://localhost:8083/actuator/health"
            ;;
        notification-service)
            health_url="http://localhost:8084/actuator/health"
            ;;
        admin-service)
            health_url="http://localhost:8085/actuator/health"
            ;;
        nacos)
            health_url="http://localhost:8848/nacos"
            ;;
        rabbitmq)
            health_url="http://localhost:15672"
            ;;
        elasticsearch)
            health_url="http://localhost:9200/_cluster/health"
            ;;
        mysql)
            # MySQL使用端口检查
            local elapsed=0
            while [[ $elapsed -lt $timeout ]]; do
                if nc -z localhost 3306 2>/dev/null; then
                    log_info "✓ $service 已就绪"
                    return 0
                fi
                sleep 1
                ((elapsed++))
            done
            return 1
            ;;
        redis)
            # Redis使用端口检查
            local elapsed=0
            while [[ $elapsed -lt $timeout ]]; do
                if nc -z localhost 6379 2>/dev/null; then
                    log_info "✓ $service 已就绪"
                    return 0
                fi
                sleep 1
                ((elapsed++))
            done
            return 1
            ;;
    esac
    
    # HTTP健康检查
    if [[ -n "$health_url" ]]; then
        local elapsed=0
        while [[ $elapsed -lt $timeout ]]; do
            if curl -sf "$health_url" > /dev/null 2>&1; then
                log_info "✓ $service 已就绪"
                return 0
            fi
            sleep 2
            ((elapsed+=2))
            echo -n "."
        done
        echo
        return 1
    fi
    
    # 默认等待
    sleep 5
    return 0
}

#==============================================================================
# 启动单个服务
#==============================================================================
start_service() {
    local service=$1
    local deps=$2
    local timeout=$3
    
    log_step "启动服务: $service"
    
    # 检查是否已在运行
    if is_service_running "$service"; then
        log_warn "服务已在运行: $service"
        return 0
    fi
    
    # 检查依赖
    if ! check_dependencies "$deps"; then
        error_exit "依赖服务未就绪，无法启动: $service"
    fi
    
    # 根据环境选择启动方式
    local compose_file="${PROJECT_ROOT}/docker/docker-compose-${ENV}.yml"
    [[ ! -f "$compose_file" ]] && compose_file="${PROJECT_ROOT}/docker/docker-compose.yml"
    
    if [[ -f "$compose_file" ]]; then
        # 使用Docker Compose启动
        if [[ "$FOREGROUND" == true ]]; then
            docker-compose -f "$compose_file" up "$service" &
        else
            docker-compose -f "$compose_file" up -d "$service"
        fi
    else
        # 使用启动脚本
        local start_script="${PROJECT_ROOT}/scripts/services/start-${service}.sh"
        if [[ -f "$start_script" ]]; then
            bash "$start_script" --env "$ENV" &
        else
            log_warn "未找到启动脚本: $start_script"
            return 1
        fi
    fi
    
    # 等待服务就绪
    if [[ "$NO_HEALTH_CHECK" == false ]]; then
        if ! wait_for_service "$service" "$timeout"; then
            error_exit "服务启动超时: $service"
        fi
    fi
    
    log_success "✓ 服务启动成功: $service"
}

#==============================================================================
# 获取依赖顺序
#==============================================================================
get_service_order() {
    local ordered=()
    local visited=()
    
    visit_service() {
        local svc=$1
        
        # 检查是否已访问
        if [[ " ${visited[@]} " =~ " ${svc} " ]]; then
            return
        fi
        
        visited+=("$svc")
        
        # 找到服务定义
        for svc_def in "${SERVICE_DEPENDENCIES[@]}"; do
            local svc_name="${svc_def%%:*}"
            local rest="${svc_def#*:}"
            local svc_deps="${rest%:*}"
            
            if [[ "$svc_name" == "$svc" ]]; then
                # 先访问依赖
                if [[ -n "$svc_deps" ]]; then
                    IFS=',' read -ra DEPS <<< "$svc_deps"
                    for dep in "${DEPS[@]}"; do
                        visit_service "$dep"
                    done
                fi
                break
            fi
        done
        
        ordered+=("$svc")
    }
    
    # 构建启动顺序
    for svc_def in "${SERVICE_DEPENDENCIES[@]}"; do
        local svc_name="${svc_def%%:*}"
        visit_service "$svc_name"
    done
    
    echo "${ordered[@]}"
}

#==============================================================================
# 聚合日志
#==============================================================================
aggregate_logs() {
    log_step "启动日志聚合..."
    
    local log_aggregator="${PROJECT_ROOT}/scripts/logs/aggregate.sh"
    if [[ -f "$log_aggregator" ]]; then
        bash "$log_aggregator" --env "$ENV" &
        log_info "✓ 日志聚合已启动"
    else
        log_warn "日志聚合脚本不存在"
    fi
}

#==============================================================================
# 显示服务状态
#==============================================================================
show_service_status() {
    log_step "服务状态汇总"
    
    echo -e "\n${CYAN}========================================${NC}"
    echo -e "${CYAN}         服务运行状态                  ${NC}"
    echo -e "${CYAN}========================================${NC}"
    
    local compose_file="${PROJECT_ROOT}/docker/docker-compose-${ENV}.yml"
    [[ ! -f "$compose_file" ]] && compose_file="${PROJECT_ROOT}/docker/docker-compose.yml"
    
    if [[ -f "$compose_file" ]]; then
        docker-compose -f "$compose_file" ps
    fi
    
    echo -e "${CYAN}========================================${NC}"
}

#==============================================================================
# 主函数
#==============================================================================
main() {
    init
    parse_args "$@"
    
    log_info "=========================================="
    log_info "北极星AI商机获客系统 - 启动服务"
    log_info "环境: $ENV"
    log_info "日志: $LOG_FILE"
    log_info "=========================================="
    
    # 如果只启动指定服务
    if [[ -n "$TARGET_SERVICE" ]]; then
        for svc_def in "${SERVICE_DEPENDENCIES[@]}"; do
            local svc_name="${svc_def%%:*}"
            local rest="${svc_def#*:}"
            local svc_deps="${rest%:*}"
            local svc_timeout="${rest##*:}"
            
            if [[ "$svc_name" == "$TARGET_SERVICE" ]]; then
                start_service "$svc_name" "$svc_deps" "$svc_timeout"
                show_service_status
                return 0
            fi
        done
        error_exit "未知服务: $TARGET_SERVICE"
    fi
    
    # 按依赖顺序启动所有服务
    log_step "计算服务启动顺序..."
    local service_order
    service_order=$(get_service_order)
    log_info "启动顺序: $service_order"
    
    for svc_def in "${SERVICE_DEPENDENCIES[@]}"; do
        local svc_name="${svc_def%%:*}"
        local rest="${svc_def#*:}"
        local svc_deps="${rest%:*}"
        local svc_timeout="${rest##*:}"
        
        start_service "$svc_name" "$svc_deps" "$svc_timeout"
    done
    
    # 启动日志聚合
    aggregate_logs
    
    # 显示状态
    show_service_status
    
    log_info "=========================================="
    log_success "所有服务启动完成！"
    log_info "启动日志: $LOG_FILE"
    log_info "=========================================="
}

main "$@"
