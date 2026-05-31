#!/bin/bash
# ============================================================
#  北极星AI - 前端构建部署脚本
#  基于: Vite官方构建最佳实践 + Nginx静态资源部署
#  整合: Vue3 + Element Plus + Pinia 项目结构
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"
source "$SCRIPT_DIR/../lib/env.sh"

usage() {
    cat << EOF
北极星AI前端构建部署脚本

用法: $(basename $0) [命令] [选项]

命令:
    build       构建前端项目 (默认)
    deploy      部署到Nginx/服务器
    dev         启动开发服务器
    clean       清理构建产物
    analyze     分析打包体积

选项:
    -p, --project PROJ   项目选择 (web-admin|web-pc, 默认全部)
    -e, --env ENV        环境 (dev|prod, 默认dev)
    --mode MODE          构建模式 (development|production)
    --analyze            启用bundle分析
    -o, --output DIR     输出目录

示例:
    $(basename $0) build                          # 构建全部前端项目
    $(basename $0) build -p web-admin             # 仅构建管理后台
    $(basename $0) build -p web-pc --analyze      # 构建用户端+体积分析
    $(basename $0) deploy -e prod                 # 部署到生产环境
    $(basename $0) dev -p web-admin               # 启动管理后台开发服务器

EOF
}

COMMAND="build"
PROJECT=""
DEPLOY_ENV="dev"
BUILD_MODE="production"
ENABLE_ANALYZE=false
OUTPUT_DIR=""

while [[ $# -gt 0 ]]; do
    case $1 in
        build|deploy|dev|clean|analyze) COMMAND=$1; shift ;;
        -h|--help) usage; exit 0 ;;
        -p|--project) PROJECT=$2; shift 2 ;;
        -e|--env) DEPLOY_ENV=$2; shift 2 ;;
        --mode) BUILD_MODE=$2; shift 2 ;;
        --analyze) ENABLE_ANALYZE=true; shift ;;
        -o|--output) OUTPUT_DIR=$2; shift 2 ;;
        *) log_error "未知参数: $1"; usage; exit 1 ;;
    esac
done

load_env "$DEPLOY_ENV" || true

FRONTEND_DIR=$(get_frontend_dir)

declare -A FRONTEND_PROJECTS=(
    [web-admin]="管理后台 | Element Plus + Vite 5.1 | 端口:5173"
    [web-pc]="用户门户 | Element Plus + Vite 5.0 | 端口:5174"
)

TARGET_PROJECTS=()
if [[ -n "$PROJECT" ]]; then
    if [[ ! -d "$FRONTEND_DIR/$PROJECT" ]]; then
        log_error "项目不存在: $FRONTEND_DIR/$PROJECT"
        exit 1
    fi
    TARGET_PROJECTS+=("$PROJECT")
else
    for proj in "${!FRONTEND_PROJECTS[@]}"; do
        [[ -d "$FRONTEND_DIR/$proj" ]] && TARGET_PROJECTS+=("$proj")
    done
fi

check_prerequisites node npm || {
    log_error "缺少Node.js/npm，请先安装"
    log_info "推荐版本: Node.js >= 18 LTS"
    exit 1
}

NODE_VERSION=$(node -v 2>/dev/null)
NPM_VERSION=$(npm -v 2>/dev/null)
log_info "Node.js: ${NODE_VERSION} | npm: ${NPM_VERSION}"

build_project() {
    local project=$1
    local project_dir="$FRONTEND_DIR/$project"
    
    cd "$project_dir"
    
    if [[ ! -f "package.json" ]]; then
        log_warn "[$project] package.json 不存在，跳过"
        return 1
    fi
    
    log_info "=========================================="
    log_info "  构建: ${FRONTEND_PROJECTS[$project]}"
    log_info "  模式: $BUILD_MODE"
    log_info "=========================================="
    
    if [[ ! -d "node_modules" ]]; then
        log_info "[$project] 安装依赖..."
        npm ci --prefer-offline 2>&1 | tail -10
    fi
    
    local vite_cmd="vite build"
    [[ "$BUILD_MODE" == "development" ]] && vite_cmd="vite build --mode development"
    
    if [[ "$ENABLE_ANALYZE" == "true" ]]; then
        if npm list rollup-plugin-visualizer &>/dev/null; then
            vite_cmd="$vite_cmd --mode analyze"
        else
            log_warn "[$project] 未安装rollup-plugin-visualizer，跳过分析"
        fi
    fi
    
    local start_time=$(date +%s)
    
    if eval "$vite_cmd"; then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        
        local dist_size
        dist_size=$(du -sh "$project_dir/dist" 2>/dev/null | awk '{print $1}')
        
        log_success "[$project] 构建成功 (${duration}s, 大小: ${dist_size})"
        
        if [[ -f "$project_dir/dist/index.html" ]]; then
            log_info "  产物位置: $project_dir/dist/"
            
            local js_count css_count
            js_count=$(find "$project_dir/dist/assets" -name "*.js" 2>/dev/null | wc -l)
            css_count=$(find "$project_dir/dist/assets" -name "*.css" 2>/dev/null | wc -l)
            log_info "  JS文件: ${js_count}个 | CSS文件: ${css_count}个"
        fi
        
        return 0
    else
        log_error "[$project] 构建失败!"
        return 1
    fi
}

deploy_project() {
    local project=$1
    local project_dir="$FRONTEND_DIR/$project"
    local dist_dir="$project_dir/dist"
    
    if [[ ! -d "$dist_dir" ]]; then
        log_error "[$project] dist目录不存在，请先执行build"
        return 1
    fi
    
    log_info "[$project] 部署中..."
    
    local deploy_target="${DEPLOY_TARGET:-/usr/share/nginx/html}"
    
    if command -v docker &>/dev/null && docker ps | grep -q nginx; then
        local container_name
        container_name=$(docker ps --format '{{.Names}}' | grep -i nginx | head -1)
        
        if [[ -n "$container_name" ]]; then
            local target_subdir="$deploy_target/${project}"
            log_info "  复制到Docker容器: $container_name:$target_subdir"
            docker exec "$container_name" mkdir -p "$target_subdir" 2>/dev/null || true
            docker cp "$dist_dir/." "$container_name:$target_subdir/" 2>&1
            
            log_success "[$project] 已部署到容器: $container_name"
            return 0
        fi
    fi
    
    if ssh -o ConnectTimeout=5 "${DEPLOY_SERVER:-root@127.0.0.1}" echo ok &>/dev/null; then
        local remote_path="${REMOTE_NGINX_ROOT:-/usr/share/nginx/html}/${project}}"
        log_info "  同步到远程服务器: ${DEPLOY_SERVER}:${remote_path}"
        
        ssh "${DEPLOY_SERVER:-root@127.0.0.1}" "mkdir -p $remote_path" 2>/dev/null || true
        scp -r "$dist_dir/"* "${DEPLOY_SERVER:-root@127.0.0.1}:${remote_path}/" 2>&1
        
        log_success "[$project] 已部署到远程服务器"
        return 0
    fi
    
    local local_nginx="${LOCAL_NGINX_ROOT:-./nginx/html}/${project}}"
    mkdir -p "$local_nginx"
    cp -a "$dist_dir/." "$local_nginx/" 2>&1
    
    log_success "[$project] 已部署到本地: $local_nginx"
    return 0
}

start_dev() {
    local project=$1
    local project_dir="$FRONTEND_DIR/$project"
    
    cd "$project_dir"
    
    if [[ ! -d "node_modules" ]]; then
        log_info "[$project] 安装依赖..."
        npm ci --prefer-offline 2>&1 | tail -10
    fi
    
    local port=${FRONTEND_DEV_PORT:-5173}
    [[ "$project" == "web-pc" ]] && port=5174
    
    log_info "[$project] 启动开发服务器 (端口: ${port})..."
    npx vite --host 0.0.0.0 --port "$port"
}

case $COMMAND in
    build)
        log_info "=========================================="
        log_info "  北极星AI - 前端构建 ($DEPLOY_ENV)"
        log_info "  时间: $(now)"
        log_info "=========================================="
        
        for proj in "${TARGET_PROJECTS[@]}"; do
            build_project "$proj" || true
        done
        
        log_info ""
        log_info "=========================================="
        log_info "  构建完成"
        log_info "=========================================="
        ;;
        
    deploy)
        for proj in "${TARGET_PROJECTS[@]}"; do
            deploy_project "$proj"
        done
        ;;
        
    dev)
        if (( ${#TARGET_PROJECTS[@]} > 1 )); then
            log_error "开发模式仅支持单项目启动 (-p 指定项目)"
            exit 1
        fi
        start_dev "${TARGET_PROJECTS[0]}"
        ;;
        
    clean)
        for proj in "${TARGET_PROJECTS[@]}"; do
            local dist_dir="$FRONTEND_DIR/$proj/dist"
            if [[ -d "$dist_dir" ]]; then
                rm -rf "$dist_dir"
                log_info "[$proj] 已清理dist目录"
            fi
        done
        ;;
        
    analyze)
        ENABLE_ANALYZE=true
        for proj in "${TARGET_PROJECTS[@]}"; do
            build_project "$proj"
        done
        ;;
esac
