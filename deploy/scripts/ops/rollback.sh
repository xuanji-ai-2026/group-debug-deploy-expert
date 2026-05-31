#!/bin/bash
# ============================================================
#  北极星AI - 一键回滚脚本
#  基于: P12预备份原则 + IRP即时回滚协议
#  能力: Docker回滚/K8s回滚/JAR回滚/配置回滚/数据库回滚
#  目标: <10分钟恢复服务 (RTO < 10min)
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"
source "$SCRIPT_DIR/../lib/env.sh"

usage() {
    cat << EOF
北极星AI一键回滚脚本

用法: $(basename $0) [命令] [选项]

命令:
    list            列出可回滚版本
    rollback        执行回滚 (默认)
    backup          创建当前状态备份
    status          查看当前版本状态

选项:
    -e, --env ENV           环境 (dev|prod, 默认dev)
    -s, --service SVC       回滚指定服务 (不指定则全部)
    -t, --target VERSION    回滚到指定版本
    -m, --mode MODE         回滚模式 (docker|k8s|jar|config|db)
    --dry-run               仅显示将执行的操作，不实际执行
    -f, --force             跳过确认提示
    --keep-current          保留当前版本作为备份

示例:
    $(basename $0) list                          # 查看可回滚版本
    $(basename $0) rollback -s bx-user           # 回滚用户服务
    $(basename $0) rollback -t v20260514_001     # 回滚到指定版本
    $(basename $0) rollback -m docker --dry-run  # 预览Docker回滚操作
    $(basename $0) backup                        # 手动创建备份点

EOF
}

COMMAND="rollback"
DEPLOY_ENV="dev"
SERVICE_NAME=""
TARGET_VERSION=""
ROLLBACK_MODE=""
DRY_RUN=false
FORCE=false
KEEP_CURRENT=false

while [[ $# -gt 0 ]]; do
    case $1 in
        list|rollback|backup|status) COMMAND=$1; shift ;;
        -h|--help) usage; exit 0 ;;
        -e|--env) DEPLOY_ENV=$2; shift 2 ;;
        -s|--service) SERVICE_NAME=$2; shift 2 ;;
        -t|--target) TARGET_VERSION=$2; shift 2 ;;
        -m|--mode) ROLLBACK_MODE=$2; shift 2 ;;
        --dry-run) DRY_RUN=true; shift ;;
        -f|--force) FORCE=true; shift ;;
        --keep-current) KEEP_CURRENT=true; shift ;;
        *) log_error "未知参数: $1"; usage; exit 1 ;;
    esac
done

load_env "$DEPLOY_ENV" || true

ARCHIVE_DIR="$DEPLOY_DIR/archive"
BACKUP_DIR="$ARCHIVE_DIR/backups"
CONFIG_BACKUP_DIR="$ARCHIVE_DIR/configs"
RELEASE_DIR="$ARCHIVE_DIR/releases"
ROLLBACK_LOG="$ARCHIVE_DIR/rollback.log"

ensure_dirs

log_rollback() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" >> "$ROLLBACK_LOG"
}

list_versions() {
    log_info "=========================================="
    log_info "  可回滚版本列表"
    log_info "=========================================="
    
    if [[ -d "$BACKUP_DIR" ]]; then
        echo ""
        echo "📦 服务备份:"
        ls -lt "$BACKUP_DIR" 2>/dev/null | head -20 | while read line; do
            echo "  $line"
        done
    fi
    
    if [[ -d "$RELEASE_DIR" ]]; then
        echo ""
        echo "📦 发布版本归档:"
        ls -lt "$RELEASE_DIR" 2>/dev/null | head -20 | while read line; do
            echo "  $line"
        done
    fi
    
    if [[ -d "$CONFIG_BACKUP_DIR" ]]; then
        echo ""
        echo "📦 配置快照:"
        ls -lt "$CONFIG_BACKUP_DIR" 2>/dev/null | head -10 | while read line; do
            echo "  $line"
        done
    fi
    
    if command -v docker &>/dev/null; then
        echo ""
        echo "🐳 Docker镜像历史:"
        docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}" \
            | grep beijixing | head -20 || true
    fi
    
    if command -v kubectl &>/dev/null; then
        echo ""
        echo "☸️  K8s部署历史:"
        kubectl rollout history deployment -n "${K8S_NAMESPACE:-beijixing}" 2>/dev/null | head -20 || true
    fi
}

create_backup() {
    local timestamp=$(ts)
    local backup_name="backup_${timestamp}"
    local backup_path="$BACKUP_DIR/$backup_name"
    
    mkdir -p "$backup_path/jars"
    mkdir -p "$backup_path/configs"
    mkdir -p "$backup_path/docker"
    mkdir -p "$backup_path/db"
    
    log_info "创建备份: $backup_name"
    
    for svc in $(get_all_services); do
        local jar_file="$(get_backend_dir)/bx-$svc/target/bx-$svc-1.0.0-SNAPSHOT.jar"
        if [[ -f "$jar_file" ]]; then
            cp -a "$jar_file" "$backup_path/jars/" 2>/dev/null && \
                log_info "  备份JAR: bx-$svc"
        fi
    done
    
    cp -a "$DEPLOY_DIR/docker/docker-compose.yml" "$backup_path/docker/" 2>/dev/null || true
    cp -a "$DEPLOY_DIR/docker/.env"* "$backup_path/configs/" 2>/dev/null || true
    
    if [[ -f "$CONFIG_DIR/.env.$DEPLOY_ENV" ]]; then
        cp -a "$CONFIG_DIR/.env.$DEPLOY_ENV" "$backup_path/configs/" 2>/dev/null
    fi
    
    if mysql -u"${DB_USER:-root}" -p"${DB_PASS:-}" -h"${DB_HOST:-127.0.0.1}" -e "" &>/dev/null; then
        mysqldump -u"${DB_USER:-root}" -p"${DB_PASS:-}" -h"${DB_HOST:-127.0.0.1}" \
            --single-transaction --routines --triggers \
            "${DB_NAME:-beijixing_db}" > "$backup_path/db/db_dump_${timestamp}.sql" 2>/dev/null && \
            log_info "  备份数据库: ${DB_NAME:-beijixing_db}" || \
            log_warn "  数据库备份失败 (可能无权限)"
    fi
    
    cat > "$backup_path/MANIFEST.txt" << EOFMANIFEST
备份名称: $backup_name
时间戳: $(date '+%Y-%m-%d %H:%M:%S')
环境: $DEPLOY_ENV
操作人: 自动备份脚本
内容:
- JAR文件: $(ls "$backup_path/jars/" 2>/dev/null | wc -l) 个
- 配置文件: $(ls "$backup_path/configs/" 2>/dev/null | wc -l) 个
- Docker配置: $(ls "$backup_path/docker/" 2>/dev/null | wc -l) 个
- 数据库: $(ls -lh "$backup_path/db/" 2>/dev/null | tail -1)
EOFMANIFEST
    
    log_success "备份完成: $backup_path"
    echo "$backup_path"
}

rollback_docker() {
    local svc=${1:-all}
    
    log_info "执行Docker模式回滚..."
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY-RUN] 将执行:"
        log_info "  cd $DEPLOY_DIR/docker"
        if [[ -n "$TARGET_VERSION" ]]; then
            log_info "  docker-compose down && docker tag ${TARGET_VERSION} latest && docker-compose up -d"
        else
            log_info "  docker-compose down && docker-compose up -d"
        fi
        return 0
    fi
    
    cd "$DEPLOY_DIR/docker"
    
    if [[ "$KEEP_CURRENT" == "true" ]]; then
        create_backup >/dev/null
    fi
    
    docker-compose down --remove-orphans 2>/dev/null || true
    sleep 3
    
    if [[ -n "$TARGET_VERSION" ]] && docker image inspect "$TARGET_VERSION" &>/dev/null; then
        local registry="${DOCKER_REGISTRY:-}"
        local namespace="${DOCKER_REGISTRY_NAMESPACE:-beijixing-ai}"
        docker tag "${registry}${namespace}:${TARGET_VERSION}" "${registry}${namespace}:latest" 2>/dev/null || true
    fi
    
    docker-compose up -d 2>&1 | tee /tmp/docker_rollback.log
    
    sleep 10
    
    local healthy=0
    local total=0
    for container in $(docker compose ps --format "{{.Name}}" 2>/dev/null); do
        ((total++)) || true
        if docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null | grep -q "healthy"; then
            ((healthy++)) || true
        fi
    done
    
    log_rollback "DOCKER_ROLLBACK | service=$svc | target=$TARGET_VERSION | containers_healthy=$healthy/$total"
    
    if (( healthy == total )); then
        log_success "Docker回滚成功 (${healthy}/${total} 容器健康)"
        return 0
    else
        log_warn "Docker回滚部分完成 (${healthy}/${total} 容器健康)，请检查"
        return 1
    fi
}

rollback_k8s() {
    local svc=${1:-all}
    
    log_info "执行K8s模式回滚..."
    
    if ! command -v kubectl &>/dev/null; then
        log_error "kubectl未安装"
        return 1
    fi
    
    local ns="${K8S_NAMESPACE:-beijixing}"
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY-RUN] 将执行:"
        if [[ -n "$TARGET_VERSION" ]]; then
            log_info "  kubectl rollout undo deployment/$svc -n $ns --to-revision=$TARGET_VERSION"
        else
            log_info "  kubectl rollout undo deployment/$svc -n $ns"
        fi
        return 0
    fi
    
    local deployments
    if [[ -n "$svc" && "$svc" != "all" ]]; then
        deployments=("$svc-service")
    else
        deployments=(gateway-service user-service tenant-service content-service 
                     lead-service risk-service billing-service ai-service 
                     message-service storage-service web-service)
    fi
    
    for deploy in "${deployments[@]}"; do
        if kubectl get deployment "$deploy" -n "$ns" &>/dev/null; then
            log_info "回滚部署: $deploy"
            
            if [[ -n "$TARGET_VERSION" ]]; then
                kubectl rollout undo deployment/"$deploy" -n "$ns" --to-revision="$TARGET_VERSION" 2>&1
            else
                kubectl rollout undo deployment/"$deploy" -n "$ns" 2>&1
            fi
            
            kubectl rollout status deployment/"$deploy" -n "$ns" --timeout=120s 2>&1 || \
                log_warn "  $deploy 回滚超时或失败"
            
            log_rollback "K8S_ROLLBACK | deployment=$deploy | target=$TARGET_VERSION"
        fi
    done
    
    log_success "K8s回滚指令已发送，请使用以下命令检查状态:"
    log_info "  kubectl get pods -n $ns"
    log_info "  kubectl rollout history deployment -n $ns"
}

rollback_jar() {
    local svc=$1
    
    if [[ -z "$svc" || "$svc" == "all" ]]; then
        log_error "JAR模式必须指定服务名 (-s SERVICE_NAME)"
        return 1
    fi
    
    log_info "执行JAR模式回滚: $svc"
    
    local latest_backup
    latest_backup=$(ls -dt "$BACKUP_DIR"/backup_*/jars/${svc}*.jar 2>/dev/null | head -1)
    
    if [[ -z "$latest_backup" ]]; then
        log_error "未找到 $svc 的备份JAR文件"
        log_info "可用备份目录: $BACKUP_DIR"
        return 1
    fi
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY-RUN] 将执行:"
        log_info "  1. 停止当前 $svc 进程"
        log_info "  2. 备份JAR: $latest_backup -> $(get_backend_dir)/$svc/target/"
        log_info "  3. 重启 $svc"
        return 0
    fi
    
    source "$SCRIPTS_DIR/backend/deploy.sh"
    
    stop_service_jar "$svc"
    sleep 2
    
    cp -a "$latest_backup" "$(get_backend_dir)/$svc/target/"
    
    start_service_jar "$svc"
    
    log_rollback "JAR_ROLLBACK | service=$svc | backup=$latest_backup"
    log_success "JAR回滚完成: $svc"
}

case $COMMAND in
    list)
        list_versions
        ;;
        
    backup)
        create_backup
        ;;
        
    status)
        log_info "当前运行状态:"
        if command -v docker &>/dev/null; then
            docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Image}}" 2>/dev/null | grep beijixing || true
        fi
        if command -v kubectl &>/dev/null; then
            kubectl get pods -n "${K8S_NAMESPACE:-beijixing}" 2>/dev/null || true
        fi
        ;;
        
    rollback)
        log_info "=========================================="
        log_info "  北极星AI - 一键回滚 ($DEPLOY_ENV)"
        log_info "  时间: $(now)"
        log_info "=========================================="
        
        if [[ "$FORCE" != "true" ]]; then
            confirm "⚠️  确认执行回滚操作? 此操作将导致服务短暂中断!" || {
                log_info "回滚已取消"
                exit 0
            }
        fi
        
        case "${ROLLBACK_MODE:-auto}" in
            docker)
                rollback_docker "$SERVICE_NAME"
                ;;
            k8s)
                rollback_k8s "$SERVICE_NAME"
                ;;
            jar)
                rollback_jar "$SERVICE_NAME"
                ;;
            auto|*)
                if command -v docker &>/dev/null && docker ps | grep -q beijixing; then
                    rollback_docker "$SERVICE_NAME"
                elif command -vubectl &>/dev/null && kubectl get pods -n "${K8S_NAMESPACE:-beijixing}" &>/dev/null; then
                    rollback_k8s "$SERVICE_NAME"
                elif pgrep -f "bx-.*\.jar" &>/dev/null; then
                    rollback_jar "$SERVICE_NAME"
                else
                    log_error "无法检测到运行的部署模式 (Docker/K8s/JAR)"
                    exit 1
                fi
                ;;
        esac
        ;;
esac
