#!/bin/bash
# ============================================================
#  北极星AI - 后端服务部署脚本
#  支持: 裸机JAR部署 + Docker容器化部署
#  整合: 现有v12/v16内存优化方案 + 健康检查 + 优雅停启
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"
source "$SCRIPT_DIR/../lib/env.sh"

usage() {
    cat << EOF
北极星AI后端服务部署脚本

用法: $(basename $0) [命令] [选项]

命令:
    deploy      部署服务 (默认)
    start       启动服务
    stop        停止服务
    restart     重启服务
    status      查看状态
    logs        查看日志
    health      健康检查
    scale       扩缩容 (仅Docker模式)

选项:
    -e, --env ENV           环境 (dev|prod, 默认dev)
    -s, --service SVC       指定服务 (不指定则全部)
    -m, --mode MODE         部署模式 (jar|docker, 默认jar)
    --memory TIER           内存等级 (micro|small|standard|large)
    --batch                 分批启动 (避免OOM)
    --no-health-check       跳过健康检查等待
    -f, --force             强制重启 (不确认)

示例:
    $(basename $0) -e dev                      # 开发环境全部启动
    $(basename $0) -e prod -s bx-user          # 生产环境部署用户服务
    $(basename $0) -m docker -s bx-gateway     # Docker模式部署网关
    $(basename $0) --batch --memory micro      # 分批低内存启动

EOF
}

COMMAND="deploy"
DEPLOY_ENV="dev"
SERVICE_NAME=""
DEPLOY_MODE="jar"
MEMORY_TIER="standard"
BATCH_MODE=false
SKIP_HEALTH=false
FORCE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        deploy|start|stop|restart|status|logs|health|scale)
            COMMAND=$1; shift ;;
        -h|--help) usage; exit 0 ;;
        -e|--env) DEPLOY_ENV=$2; shift 2 ;;
        -s|--service) SERVICE_NAME=$2; shift 2 ;;
        -m|--mode) DEPLOY_MODE=$2; shift 2 ;;
        --memory) MEMORY_TIER=$2; shift 2 ;;
        --batch) BATCH_MODE=true; shift ;;
        --no-health-check) SKIP_HEALTH=true; shift ;;
        -f|--force) FORCE=true; shift ;;
        *) log_error "未知参数: $1"; usage; exit 1 ;;
    esac
done

load_env "$DEPLOY_ENV" || exit 1
ensure_dirs

BACKEND_DIR=$(get_backend_dir)
LOG_DIR=$(get_log_dir)

TARGET_SERVICES=()
if [[ -n "$SERVICE_NAME" ]]; then
    TARGET_SERVICES+=("$SERVICE_NAME")
else
    for svc in $(get_all_services); do
        TARGET_SERVICES+=("bx-$svc")
    done
fi

start_service_jar() {
    local svc_name=$1
    local port=$(get_service_port "${svc_name#bx-}")
    local jvm_opts=$(get_jvm_opts "$svc_name" "$MEMORY_TIER")
    local jar_path="$BACKEND_DIR/$svc_name/target/$svc_name-1.0.0-SNAPSHOT.jar"
    local db_name="bx_${svc_name#bx-}"
    
    if [[ ! -f "$jar_path" ]]; then
        log_warn "[$svc_name] JAR文件不存在: $jar_path"
        return 1
    fi
    
    if netstat -tlnp 2>/dev/null | grep -q ":${port} "; then
        log_info "[$svc_name] 端口 ${port} 已占用，跳过启动"
        return 0
    fi
    
    log_info "[$svc_name] 启动中... (端口:${port}, 内存:${MEMORY_TIER})"
    
    nohup java \
        $jvm_opts \
        -Dserver.address=0.0.0.0 \
        -Dserver.port=$port \
        -Dspring.datasource.driver-class-name=${DB_DRIVER:-org.mariadb.jdbc.Driver} \
        -Dspring.datasource.url="${DB_URL//\{DB_NAME\}/$db_name}" \
        -Dspring.datasource.username=${DB_USER} \
        -Dspring.datasource.password="${DB_PASS}" \
        -Dspring.redis.host=${REDIS_HOST} \
        -Dspring.redis.port=${REDIS_PORT} \
        -Dspring.redis.password="${REDIS_PASS}" \
        -Dspring.cloud.nacos.discovery.server-addr=${NACOS_ADDR} \
        -jar "$jar_path" > "$LOG_DIR/$svc_name.log" 2>&1 &
    
    local pid=$!
    echo $pid > "$LOG_DIR/$svc_name.pid"
    log_info "[$svc_name] 已启动 (PID: ${pid})"
    
    if [[ "$SKIP_HEALTH" != "true" ]]; then
        sleep 5
        wait_for_healthy "http://127.0.0.1:${port}/actuator/health" 60 "$svc_name" || {
            log_error "[$svc_name] 健康检查失败，查看日志: $LOG_DIR/$svc_name.log"
            tail -30 "$LOG_DIR/$svc_name.log" 2>/dev/null || true
            return 1
        }
    fi
    
    return 0
}

stop_service_jar() {
    local svc_name=$1
    local port=$(get_service_port "${svc_name#bx-}")
    local pid_file="$LOG_DIR/$svc_name.pid"
    
    if [[ -f "$pid_file" ]]; then
        local pid=$(cat "$pid_file")
        if kill -0 "$pid" 2>/dev/null; then
            log_info "[$svc_name] 停止中... (PID: ${pid})"
            kill -TERM "$pid" 2>/dev/null
            
            local elapsed=0
            while (( elapsed < 15 )) && kill -0 "$pid" 2>/dev/null; do
                sleep 1
                ((elapsed++))
            done
            
            if kill -0 "$pid" 2>/dev/null; then
                log_warn "[$svc_name] 强制终止..."
                kill -9 "$pid" 2>/dev/null
            fi
        fi
        rm -f "$pid_file"
    else
        pkill -f "$svc_name.*808" 2>/dev/null || true
    fi
    
    log_success "[$svc_name] 已停止"
}

check_status() {
    log_info "=========================================="
    log_info "  服务状态检查"
    log_info "=========================================="
    
    for svc in "${TARGET_SERVICES[@]}"; do
        local port=$(get_service_port "${svc#bx-}")
        local status="DOWN"
        
        if netstat -tlnp 2>/dev/null | grep -q ":${port} "; then
            local http_code
            http_code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 3 "http://127.0.0.1:${port}/actuator/health" 2>/dev/null || echo "000")
            
            if [[ "$http_code" == "200" ]]; then
                status="UP ✓"
            else
                status="UNHEALTHY ($http_code)"
            fi
        fi
        
        printf "  %-20s 端口:%-6s %s\n" "$svc" "$port" "$status"
    done
}

case $COMMAND in
    deploy|start)
        log_info "=========================================="
        log_info "  北极星AI - 后端服务部署 ($DEPLOY_ENV)"
        log_info "  模式: $DEPLOY_MODE | 内存: $MEMORY_TIER"
        log_info "  时间: $(now)"
        log_info "=========================================="
        
        if [[ "$DEPLOY_MODE" == "jar" ]]; then
            for svc in "${TARGET_SERVICES[@]}"; do
                if [[ "$BATCH_MODE" == "true" ]]; then
                    start_service_jar "$svc" || true
                    sleep 3
                else
                    start_service_jar "$svc"
                fi
            done
        else
            log_error "Docker模式请使用: scripts/docker/compose-deploy.sh"
            exit 1
        fi
        
        check_status
        ;;
        
    stop)
        log_info "停止服务..."
        for svc in "${TARGET_SERVICES[@]}"; do
            stop_service_jar "$svc"
        done
        ;;
        
    restart)
        for svc in "${TARGET_SERVICES[@]}"; do
            log_info "重启: $svc"
            stop_service_jar "$svc"
            sleep 2
            start_service_jar "$svc"
        done
        ;;
        
    status)
        check_status
        ;;
        
    logs)
        local target=${SERVICE_NAME:-"all"}
        if [[ "$target" == "all" ]]; then
            tail -f $LOG_DIR/*.log 2>/dev/null || log_error "无日志文件"
        else
            tail -100f "$LOG_DIR/${target}.log" 2>/dev/null || log_error "日志不存在: $LOG_DIR/${target}.log"
        fi
        ;;
        
    health)
        log_info "执行全量健康检查..."
        all_healthy=true
        for svc in "${TARGET_SERVICES[@]}"; do
            local port=$(get_service_port "${svc#bx-}")
            health_check "http://127.0.0.1:${port}/actuator/health" 200 10 3 "$svc" || all_healthy=false
        done
        $all_healthy && log_success "所有服务健康" || log_error "存在不健康服务"
        ;;
        
    scale)
        log_error "扩缩容功能仅在Docker/K8s模式下可用"
        exit 1
        ;;
esac
