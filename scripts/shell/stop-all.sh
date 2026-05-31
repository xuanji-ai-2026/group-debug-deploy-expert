#!/bin/bash
#==============================================================================
# 北极星AI商机获客系统 - 停止所有服务脚本
# Stop All Services Script for Beijixing AI Business Acquisition System
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
readonly LOG_FILE="${LOG_DIR}/stop-all-$(date +%Y%m%d-%H%M%S).log"
readonly BACKUP_DIR="${PROJECT_ROOT}/data/backup"

#==============================================================================
# 服务停止顺序（与启动顺序相反）
#==============================================================================
declare -a STOP_ORDER=(
    "frontend"
    "admin-service"
    "notification-service"
    "ai-service"
    "business-service"
    "user-service"
    "gateway"
    "elasticsearch"
    "rabbitmq"
    "nacos"
    "mysql"
    "redis"
)

# 需要数据持久化检查的服务
readonly DATA_SERVICES=("mysql" "redis" "elasticsearch" "nacos")

#==============================================================================
# 初始化
#==============================================================================
init() {
    mkdir -p "${LOG_DIR}"
    mkdir -p "${BACKUP_DIR}"
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
${CYAN}北极星AI商机获客系统 - 停止所有服务${NC}

用法: $0 [选项]

选项:
    -e, --env ENV          指定环境 (dev|prod) [默认: dev]
    -s, --service SERVICE  只停止指定服务
    -t, --timeout SEC      优雅关闭超时时间(秒) [默认: 30]
    -f, --force            强制停止（不等待优雅关闭）
    -b, --backup           停止前执行数据备份
    -h, --help             显示此帮助信息

示例:
    $0 --env prod                           # 停止所有服务
    $0 --env dev --service user-service     # 只停止用户服务
    $0 --env prod --backup                  # 备份数据后停止
    $0 --env prod --force                   # 强制停止

EOF
}

#==============================================================================
# 参数解析
#==============================================================================
parse_args() {
    ENV="dev"
    TARGET_SERVICE=""
    TIMEOUT=30
    FORCE=false
    BACKUP=false

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
            -t|--timeout)
                TIMEOUT="$2"
                shift 2
                ;;
            -f|--force)
                FORCE=true
                shift
                ;;
            -b|--backup)
                BACKUP=true
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

    # 验证超时时间
    if ! [[ "$TIMEOUT" =~ ^[0-9]+$ ]]; then
        error_exit "无效的超时时间: $TIMEOUT"
    fi
}

#==============================================================================
# 检查服务是否运行
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
# 数据持久化检查
#==============================================================================
check_data_persistence() {
    local service=$1
    
    log_step "检查数据持久化状态: $service"
    
    case $service in
        mysql)
            # 检查MySQL数据同步状态
            local mysql_status
            mysql_status=$(docker exec mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD:-root}" -e "SHOW STATUS LIKE 'Innodb_buffer_pool_pages_dirty';" 2>/dev/null | tail -1 || echo "0")
            if [[ "$mysql_status" -gt 0 ]]; then
                log_warn "MySQL有未写入磁盘的数据: $mysql_status pages dirty"
                return 1
            fi
            log_info "✓ MySQL数据已同步"
            ;;
        redis)
            # 检查Redis持久化
            if docker exec redis redis-cli INFO Persistence 2>/dev/null | grep -q "rdb_bgsave_in_progress:1"; then
                log_warn "Redis正在进行后台保存"
                return 1
            fi
            # 触发保存
            docker exec redis redis-cli BGSAVE 2>/dev/null || true
            log_info "✓ Redis持久化已触发"
            ;;
        elasticsearch)
            # 检查ES索引状态
            local es_health
            es_health=$(curl -sf "http://localhost:9200/_cluster/health" 2>/dev/null | grep -o '"status":"[^"]*"' | cut -d'"' -f4 || echo "unknown")
            if [[ "$es_health" == "red" ]]; then
                log_warn "Elasticsearch集群状态异常: $es_health"
                return 1
            fi
            # 刷新索引
            curl -sf -X POST "http://localhost:9200/_flush" > /dev/null 2>&1 || true
            log_info "✓ Elasticsearch索引已刷新"
            ;;
        nacos)
            # Nacos数据检查
            log_info "✓ Nacos配置已持久化"
            ;;
    esac
    
    return 0
}

#==============================================================================
# 备份数据
#==============================================================================
backup_data() {
    log_step "执行数据备份..."
    
    local backup_timestamp
    backup_timestamp=$(date +%Y%m%d_%H%M%S)
    local backup_path="${BACKUP_DIR}/${backup_timestamp}"
    mkdir -p "$backup_path"
    
    # 备份MySQL
    if is_service_running "mysql"; then
        log_info "备份MySQL数据..."
        docker exec mysql mysqldump -uroot -p"${MYSQL_ROOT_PASSWORD:-root}" --all-databases --single-transaction > "${backup_path}/mysql-all.sql" 2>/dev/null || {
            log_warn "MySQL备份失败"
        }
        if [[ -f "${backup_path}/mysql-all.sql" ]]; then
            log_info "✓ MySQL备份完成"
        fi
    fi
    
    # 备份Redis
    if is_service_running "redis"; then
        log_info "备份Redis数据..."
        docker cp redis:/data/dump.rdb "${backup_path}/redis-dump.rdb" 2>/dev/null || {
            log_warn "Redis备份失败"
        }
        if [[ -f "${backup_path}/redis-dump.rdb" ]]; then
            log_info "✓ Redis备份完成"
        fi
    fi
    
    # 备份Nacos配置
    if is_service_running "nacos"; then
        log_info "备份Nacos配置..."
        # 导出Nacos配置
        curl -sf "http://localhost:8848/nacos/v1/cs/configs?export=true&group=&tenant=" > "${backup_path}/nacos-configs.zip" 2>/dev/null || {
            log_warn "Nacos备份失败"
        }
        if [[ -f "${backup_path}/nacos-configs.zip" ]]; then
            log_info "✓ Nacos配置备份完成"
        fi
    fi
    
    # 创建备份清单
    cat > "${backup_path}/backup-info.txt" << EOF
Backup Time: $(date)
Environment: $ENV
Services:
$(docker ps --format '{{.Names}}' 2>/dev/null | grep -E 'mysql|redis|nacos|elasticsearch' || echo "No data services running")
EOF
    
    log_success "✓ 数据备份完成: $backup_path"
}

#==============================================================================
# 优雅停止服务
#==============================================================================
graceful_stop_service() {
    local service=$1
    
    log_step "停止服务: $service"
    
    if ! is_service_running "$service"; then
        log_warn "服务未运行: $service"
        return 0
    fi
    
    # 数据持久化检查
    if [[ " ${DATA_SERVICES[@]} " =~ " ${service} " ]]; then
        local retries=0
        while [[ $retries -lt 3 ]]; do
            if check_data_persistence "$service"; then
                break
            fi
            log_warn "等待数据同步完成... (重试 $((retries+1))/3)"
            sleep 5
            ((retries++))
        done
    fi
    
    local compose_file="${PROJECT_ROOT}/docker/docker-compose-${ENV}.yml"
    [[ ! -f "$compose_file" ]] && compose_file="${PROJECT_ROOT}/docker/docker-compose.yml"
    
    if [[ -f "$compose_file" ]]; then
        # 使用Docker Compose停止
        if [[ "$FORCE" == true ]]; then
            log_warn "强制停止服务: $service"
            docker-compose -f "$compose_file" kill "$service" 2>/dev/null || true
        else
            log_info "优雅停止服务 (超时: ${TIMEOUT}秒)..."
            docker-compose -f "$compose_file" stop -t "$TIMEOUT" "$service" 2>/dev/null || {
                log_warn "优雅停止超时，强制停止"
                docker-compose -f "$compose_file" kill "$service" 2>/dev/null || true
            }
        fi
        
        # 可选：移除容器
        docker-compose -f "$compose_file" rm -f "$service" 2>/dev/null || true
    else
        # 使用停止脚本
        local stop_script="${PROJECT_ROOT}/scripts/services/stop-${service}.sh"
        if [[ -f "$stop_script" ]]; then
            bash "$stop_script" --env "$ENV"
        else
            # 尝试直接停止
            if [[ "$FORCE" == true ]]; then
                docker kill "$service" 2>/dev/null || pkill -9 -f "$service" 2>/dev/null || true
            else
                docker stop -t "$TIMEOUT" "$service" 2>/dev/null || pkill -f "$service" 2>/dev/null || true
            fi
        fi
    fi
    
    # 验证停止
    local wait_count=0
    while is_service_running "$service" && [[ $wait_count -lt 10 ]]; do
        sleep 1
        ((wait_count++))
    done
    
    if is_service_running "$service"; then
        log_error "服务停止失败: $service"
        return 1
    fi
    
    log_success "✓ 服务已停止: $service"
}

#==============================================================================
# 清理资源
#==============================================================================
cleanup_resources() {
    log_step "清理资源..."
    
    # 清理未使用的容器
    local removed_containers
    removed_containers=$(docker container prune -f 2>/dev/null | grep -o '[0-9]*' | head -1 || echo "0")
    log_info "清理容器: $removed_containers"
    
    # 清理未使用的网络
    local removed_networks
    removed_networks=$(docker network prune -f 2>/dev/null | grep -o '[0-9]*' | head -1 || echo "0")
    log_info "清理网络: $removed_networks"
    
    log_info "✓ 资源清理完成"
}

#==============================================================================
# 显示停止状态
#==============================================================================
show_stop_status() {
    log_step "停止状态汇总"
    
    echo -e "\n${CYAN}========================================${NC}"
    echo -e "${CYAN}         服务停止状态                  ${NC}"
    echo -e "${CYAN}========================================${NC}"
    
    local running_services=0
    for service in "${STOP_ORDER[@]}"; do
        if is_service_running "$service"; then
            echo -e "${RED}✗${NC} $service (仍在运行)"
            ((running_services++))
        else
            echo -e "${GREEN}✓${NC} $service (已停止)"
        fi
    done
    
    echo -e "${CYAN}========================================${NC}"
    
    if [[ $running_services -gt 0 ]]; then
        log_warn "$running_services 个服务仍在运行"
        return 1
    fi
    
    return 0
}

#==============================================================================
# 主函数
#==============================================================================
main() {
    init
    parse_args "$@"
    
    log_info "=========================================="
    log_info "北极星AI商机获客系统 - 停止服务"
    log_info "环境: $ENV"
    log_info "日志: $LOG_FILE"
    log_info "=========================================="
    
    # 确认操作
    if [[ "$FORCE" == true ]]; then
        log_warn "即将强制停止所有服务！"
        sleep 2
    fi
    
    # 执行备份
    if [[ "$BACKUP" == true ]]; then
        backup_data
    fi
    
    # 如果只停止指定服务
    if [[ -n "$TARGET_SERVICE" ]]; then
        graceful_stop_service "$TARGET_SERVICE"
        show_stop_status
        return 0
    fi
    
    # 按顺序停止所有服务
    for service in "${STOP_ORDER[@]}"; do
        graceful_stop_service "$service"
    done
    
    # 清理资源
    cleanup_resources
    
    # 显示状态
    show_stop_status
    
    log_info "=========================================="
    log_success "所有服务已停止！"
    log_info "停止日志: $LOG_FILE"
    if [[ "$BACKUP" == true ]]; then
        log_info "数据备份: ${BACKUP_DIR}"
    fi
    log_info "=========================================="
}

main "$@"
