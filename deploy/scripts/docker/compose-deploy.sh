#!/bin/bash
# ============================================================
#  北极星AI - Docker容器化部署脚本
#  基于: Docker Compose + BuildKit + 健康检查 + 滚动更新
#  整合: 现有docker-compose.yml (11服务+6基础设施)
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"
source "$SCRIPT_DIR/../lib/env.sh"

usage() {
    cat << EOF
北极星AI Docker部署脚本

用法: $(basename $0) [命令] [选项]

命令:
    up          启动服务 (默认)
    down        停止并移除容器
    restart     重启服务
    build       构建镜像
    logs        查看日志
    status      查看状态
    health      健康检查
    cleanup     清理未使用资源
    scale       扩缩容

选项:
    -e, --env ENV           环境 (dev|prod|base, 默认dev)
    -s, --service SVC       指定服务 (不指定则全部)
    -d, --detach            后台运行
    --no-deps               不启动依赖服务
    --force-recreate        强制重建容器
    --build                 启动前先构建
    --pull                  拉取最新基础镜像
    -f, --force             跳过确认提示

示例:
    $(basename $0) up -e dev                    # 启动开发环境
    $(basename $0) up -e prod -d                # 后台启动生产环境
    $(basename $0) up -s bx-user --build        # 构建并启动用户服务
    $(basename $0) restart -s mysql             # 重启MySQL
    $(basename $0) logs -s bx-gateway --tail 100 # 查看网关最近日志
    $(basename $0) cleanup                     # 清理无用资源

EOF
}

COMMAND="up"
DEPLOY_ENV="dev"
SERVICE_NAME=""
DETACH=false
NO_DEPS=false
FORCE_RECREATE=false
DO_BUILD=false
DO_PULL=false
FORCE=false
LOG_TAIL="100"

while [[ $# -gt 0 ]]; do
    case $1 in
        up|down|restart|build|logs|status|health|cleanup|scale)
            COMMAND=$1; shift ;;
        -h|--help) usage; exit 0 ;;
        -e|--env) DEPLOY_ENV=$2; shift 2 ;;
        -s|--service) SERVICE_NAME=$2; shift 2 ;;
        -d|--detach) DETACH=true; shift ;;
        --no-deps) NO_DEPS=true; shift ;;
        --force-recreate) FORCE_RECREATE=true; shift ;;
        --build) DO_BUILD=true; shift ;;
        --pull) DO_PULL=true; shift ;;
        -f|--force) FORCE=true; shift ;;
        --tail) LOG_TAIL=$2; shift 2 ;;
        *) log_error "未知参数: $1"; usage; exit 1 ;;
    esac
done

load_env "$DEPLOY_ENV" || true

DOCKER_DIR="$DEPLOY_DIR/docker"
COMPOSE_FILE=$(get_compose_file "$DEPLOY_ENV")

get_compose_file() {
    local env=$1
    case $env in
        dev)   echo "$DOCKER_DIR/docker-compose.dev.yml" ;;
        prod)  echo "$DOCKER_DIR/docker-compose.prod.yml" ;;
        base)  echo "$DOCKER_DIR/docker-compose.yml" ;;
        *)     echo "$DOCKER_DIR/docker-compose.yml" ;;
    esac
}

check_docker() {
    check_prerequisites docker docker-compose || {
        log_error "Docker环境未就绪"
        exit 1
    }
    
    if ! docker info &>/dev/null; then
        log_error "Docker守护进程未运行"
        exit 1
    fi
    
    log_info "Docker版本: $(docker --version)"
}

compose_cmd() {
    local cmd="$1"
    shift
    
    local args=(-f "$COMPOSE_FILE")
    
    [[ -n "$SERVICE_NAME" ]] && args+=("$SERVICE_NAME")
    [[ "$DETACH" == "true" ]] && args+=(-d)
    [[ "$NO_DEPS" == "true" ]] && args+=(--no-deps)
    [[ "$FORCE_RECREATE" == "true" ]] && args+=(--force-recreate)
    [[ "$DO_BUILD" == "true" ]] && args+=(--build)
    
    docker compose "${args[@]}" "$cmd" "$@"
}

do_health_check() {
    log_info "执行Docker健康检查..."
    
    local containers
    containers=$(docker compose -f "$COMPOSE_FILE" ps --format "{{.Name}}" 2>/dev/null | grep -v -E "(mysql|redis|nacos|mongo|rabbitmq|es)" || true)
    
    local healthy=0
    local total=0
    local unhealthy_list=()
    
    for container in $containers; do
        ((total++)) || true
        
        local health_status
        health_status=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null || echo "unknown")
        
        case $health_status in
            healthy)
                ((healthy++)) || true
                log_success "[OK]   $container: healthy"
                ;;
            starting)
                log_warn "[WAIT] $container: starting..."
                unhealthy_list+=("$container")
                ;;
            unhealthy)
                log_error "[FAIL] $container: unhealthy!"
                unhealthy_list+=("$container")
                ;;
            *)
                log_warn "[??]   $container: ${health_status}"
                ;;
        esac
    done
    
    echo ""
    log_info "健康检查结果: ${healthy}/${total} 容器正常"
    
    if (( ${#unhealthy_list[@]} > 0 )); then
        log_warn "不健康容器: ${unhealthy_list[*]}"
        return 1
    fi
    
    return 0
}

case $COMMAND in
    up)
        check_docker
        
        log_info "=========================================="
        log_info "  北极星AI - Docker部署 ($DEPLOY_ENV)"
        log_info "  Compose文件: $(basename $COMPOSE_FILE)"
        log_info "  时间: $(now)"
        log_info "=========================================="
        
        if [[ "$DO_PULL" == "true" ]]; then
            log_info "拉取最新基础镜像..."
            compose_cmd pull 2>&1 | tail -10
        fi
        
        compose_cmd up 2>&1 | tee /tmp/docker_up.log
        
        sleep 5
        
        do_health_check || true
        
        log_info ""
        log_info "=========================================="
        log_info "  部署完成"
        log_info "=========================================="
        log_info "查看状态: $(basename $0) status"
        log_info "查看日志: $(basename $0) logs -s <service>"
        log_info "停止服务: $(basename $0) down"
        ;;
        
    down)
        check_docker
        
        if [[ "$FORCE" != "true" ]]; then
            confirm "⚠️  确认停止并移除所有容器? 数据卷将保留!" || {
                log_info "操作已取消"
                exit 0
            }
        fi
        
        log_info "停止并移除容器..."
        compose_cmd down --remove-orphans -v 2>&1
        log_success "已停止所有容器"
        ;;
        
    restart)
        check_docker
        log_info "重启服务..."
        compose_cmd restart 2>&1
        sleep 5
        do_health_check || true
        ;;
        
    build)
        check_docker
        log_info "构建Docker镜像..."
        
        local build_args=(--build-arg JAVA_VERSION=17 --build-arg MAVEN_OPTS="-Xms512m -Xmx1024m")
        
        if [[ -n "$SERVICE_NAME" ]]; then
            docker compose -f "$COMPOSE_FILE" build "${build_args[@]}" "$SERVICE_NAME" 2>&1
        else
            docker compose -f "$COMPOSE_FILE" build "${build_args[@]}" 2>&1
        fi
        
        log_success "构建完成"
        docker images | grep beijixing || true
        ;;
        
    logs)
        check_docker
        
        if [[ -n "$SERVICE_NAME" ]]; then
            docker compose -f "$COMPOSE_FILE" logs --tail "$LOG_TAIL" -f "$SERVICE_NAME" 2>&1
        else
            docker compose -f "$COMPOSE_FILE" logs --tail "$LOG_TAIL" 2>&1
        fi
        ;;
        
    status)
        check_docker
        echo ""
        docker compose -f "$COMPOSE_FILE" ps 2>&1
        echo ""
        do_health_check || true
        ;;
        
    health)
        check_docker
        do_health_check
        ;;
        
    cleanup)
        check_docker
        
        log_info "清理Docker资源..."
        
        echo ""
        log_info "清理前磁盘使用:"
        df -h / | tail -1
        
        docker system prune -af --volumes 2>&1 | tail -10
        
        echo ""
        log_info "清理后磁盘使用:"
        df -h / | tail -1
        
        log_success "Docker清理完成"
        ;;
        
    scale)
        if [[ -z "$SERVICE_NAME" ]]; then
            log_error "扩缩容需要指定服务 (-s SERVICE)"
            exit 1
        fi
        
        local replicas=${REPLICAS:-2}
        log_info "扩缩容: $SERVICE_NAME -> ${replicas}个实例"
        docker compose -f "$COMPOSE_FILE" up -d --scale "$SERVICE_NAME=$replicas" --no-recreate 2>&1
        ;;
esac
