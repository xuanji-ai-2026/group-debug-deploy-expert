#!/bin/bash
# ============================================================
#  北极星AI - K8s编排部署脚本
#  基于: kubectl + RollingUpdate + Canary Deployment
#  整合: 现有k8s/目录 (11个deployment YAML)
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"
source "$SCRIPT_DIR/../lib/env.sh"

usage() {
    cat << EOF
北极星AI K8s部署脚本

用法: $(basename $0) [命令] [选项]

命令:
    deploy          全量部署 (默认)
    rolling-update  滚动更新
    canary          金丝雀发布
    restart         重启服务
    scale           扩缩容
    status          查看状态
    logs            查看日志
    rollback        回滚版本
    cleanup         清理资源

选项:
    -e, --env ENV           环境 (dev|prod, 默认dev)
    -s, --service SVC       指定服务 (不指定则全部)
    -n, --namespace NS      命名空间 (默认beijixing)
    --replicas N            副本数 (默认2)
    --image VERSION         指定镜像版本
    --canary-weight N       金丝雀流量权重 (0-100, 默认10)
    --dry-run               仅显示将执行的操作

示例:
    $(basename $0) deploy                          # 全量部署
    $(basename $0) rolling-update -s user-service  # 滚动更新用户服务
    $(basename $0) canary -s user-service --canary-weight 20  # 金丝雀发布20%流量
    $(basename $0) scale -s gateway-service --replicas 3   # 网关扩容到3副本
    $(basename $0) status                         # 查看集群状态
    $(basename $0) logs -s user-service           # 查看用户服务日志

EOF
}

COMMAND="deploy"
DEPLOY_ENV="dev"
SERVICE_NAME=""
NAMESPACE="${K8S_NAMESPACE:-beijixing}"
REPLICAS=2
IMAGE_VERSION=""
CANARY_WEIGHT=10
DRY_RUN=false

while [[ $# -gt 0 ]]; do
    case $1 in
        deploy|rolling-update|canary|restart|scale|status|logs|rollback|cleanup)
            COMMAND=$1; shift ;;
        -h|--help) usage; exit 0 ;;
        -e|--env) DEPLOY_ENV=$2; shift 2 ;;
        -s|--service) SERVICE_NAME=$2; shift 2 ;;
        -n|--namespace) NAMESPACE=$2; shift 2 ;;
        --replicas) REPLICAS=$2; shift 2 ;;
        --image) IMAGE_VERSION=$2; shift 2 ;;
        --canary-weight) CANARY_WEIGHT=$2; shift 2 ;;
        --dry-run) DRY_RUN=true; shift ;;
        *) log_error "未知参数: $1"; usage; exit 1 ;;
    esac
done

load_env "$DEPLOY_ENV" || true

K8S_DIR="$DEPLOY_DIR/k8s"

check_k8s() {
    check_prerequisites kubectl || {
        log_error "kubectl未安装"
        exit 1
    }
    
    if ! kubectl cluster-info &>/dev/null; then
        log_error "无法连接K8s集群"
        exit 1
    fi
    
    local ctx
    ctx=$(kubectl config current-context 2>/dev/null || echo "unknown")
    local cluster
    cluster=$(kubectl cluster-info 2>/dev/null | head -1 | grep -oE 'http://[^)]+' | head -1)
    
    log_info "K8s上下文: $ctx"
    log_info "集群地址: ${cluster:-未知}"
    
    kubectl version --client --short 2>/dev/null
}

get_deployments() {
    if [[ -n "$SERVICE_NAME" ]]; then
        echo "$SERVICE_NAME"
    else
        echo "gateway-service user-service tenant-service content-service \
             lead-service risk-service billing-service ai-service \
             message-service storage-service web-service"
    fi
}

do_deploy() {
    local deployments
    deployments=$(get_deployments)
    
    log_info "创建命名空间..."
    kubectl apply -f "$K8S_DIR/namespace.yaml" ${DRY_RUN:+--dry-run=client}
    
    log_info "创建ConfigMap和Secret..."
    kubectl apply -f "$K8S_DIR/configmap.yaml" ${DRY_RUN:+--dry-run=client}
    kubectl apply -f "$K8S_DIR/secret.yaml" ${DRY_RUN:+--dry-run=client}
    
    for deploy in $deployments; do
        local yaml_file="$K8S_DIR/services/${deploy%-service}-deployment.yaml"
        
        if [[ ! -f "$yaml_file" ]]; then
            log_warn "Deployment文件不存在: $yaml_file"
            continue
        fi
        
        log_info "部署: $deploy"
        
        local apply_args=(-f "$yaml_file")
        [[ "$DRY_RUN" == "true" ]] && apply_args+=(--dry-run=client)
        
        if [[ -n "$IMAGE_VERSION" ]]; then
            kubectl set image deployment/"$deploy" \
                "*=${DOCKER_REGISTRY_NAMESPACE:-beijixing-ai}/${deploy}:${IMAGE_VERSION}" \
                -n "$NAMESPACE" ${DRY_RUN:+--dry-run=client} 2>/dev/null || true
        else
            kubectl apply "${apply_args[@]}" -n "$NAMESPACE" 2>&1
        fi
    done
    
    sleep 5
    show_status
}

do_rolling_update() {
    if [[ -z "$SERVICE_NAME" ]]; then
        log_error "滚动更新需要指定服务 (-s SERVICE_NAME)"
        exit 1
    fi
    
    log_info "执行滚动更新: $SERVICE_NAME"
    
    local args=(deployment/"$SERVICE_NAME" -n "$NAMESPACE")
    [[ "$DRY_RUN" == "true" ]] && args+=(--dry-run=client)
    
    if [[ -n "$IMAGE_VERSION" ]]; then
        kubectl set image "${args[@]}" \
            "*=${DOCKER_REGISTRY_NAMESPACE:-beijixing-ai}/${SERVICE_NAME}:${IMAGE_VERSION}"
    else
        kubectl rollout restart "${args[@]}"
    fi
    
    log_info "等待滚动更新完成..."
    kubectl rollout status deployment/"$SERVICE_NAME" -n "$NAMESPACE" --timeout=180s \
        ${DRY_RUN:+--dry-run=client} 2>&1
}

do_canary() {
    if [[ -z "$SERVICE_NAME" ]]; then
        log_error "金丝雀发布需要指定服务 (-s SERVICE_NAME)"
        exit 1
    fi
    
    log_info "金丝雀发布: $SERVICE_NAME (流量权重: ${CANARY_WEIGHT}%)"
    
    local stable_deployment="$SERVICE_NAME"
    local canary_deployment="${SERVICE_NAME}-canary"
    
    log_info "1. 部署金丝雀版本..."
    
    local canary_yaml
    canary_yaml=$(kubectl get deployment "$stable_deployment" -n "$NAMESPACE" -o yaml 2>/dev/null)
    
    if [[ -z "$canary_yaml" ]]; then
        log_error "无法获取 $stable_deployment 配置"
        exit 1
    fi
    
    echo "$canary_yaml" \
        | sed "s/name: ${stable_deployment}/name: ${canary_deployment}/" \
        | sed "s/app.kubernetes.io.name: ${stable_deployment}/app.kubernetes.io.name: ${canary_deployment}/" \
        | kubectl apply -n "$NAMESPACE" ${DRY_RUN:+--dry-run=client} -f - 2>&1
    
    kubectl patch deployment "$canary_deployment" -n "$NAMESPACE" -p "{\"spec\":{\"replicas\":1}}" \
        ${DRY_RUN:+--dry-run=client} 2>/dev/null || true
    
    log_info "2. 金丝雀已部署 (1副本)"
    log_info "3. 请通过Ingress或Service Mesh调整流量比例"
    log_info "   金丝雀: ${CANARY_WEIGHT}% / 稳定版: $((100-CANARY_WEIGHT))%"
}

show_status() {
    echo ""
    log_info "=========================================="
    log_info "  K8s集群状态 ($NAMESPACE)"
    log_info "=========================================="
    
    echo ""
    echo "📦 Pods:"
    kubectl get pods -n "$NAMESPACE" -o wide 2>/dev/null || true
    
    echo ""
    echo "🔌 Services:"
    kubectl get svc -n "$NAMESPACE" 2>/dev/null || true
    
    echo ""
    echo "📊 资源使用:"
    kubectl top pods -n "$NAMESPACE" 2>/dev/null || echo "  (metrics-server未安装)"
    
    echo ""
    echo "🔄 部署历史:"
    for deploy in $(get_deployments); do
        local revision
        revision=$(kubectl rollout history deployment/"$deploy" -n "$NAMESPACE" 2>/dev/null | tail -1 | awk '{print $1}' || echo "?")
        printf "  %-25s 当前版本: %s\n" "$deploy" "$revision"
    done
}

case $COMMAND in
    deploy)
        check_k8s
        log_info "=========================================="
        log_info "  北极星AI - K8s部署 ($DEPLOY_ENV)"
        log_info "  命名空间: $NAMESPACE"
        log_info "  时间: $(now)"
        log_info "=========================================="
        do_deploy
        ;;
        
    rolling-update)
        check_k8s
        do_rolling_update
        show_status
        ;;
        
    canary)
        check_k8s
        do_canary
        ;;
        
    restart)
        check_k8s
        for deploy in $(get_deployments); do
            log_info "重启: $deploy"
            kubectl rollout restart deployment/"$deploy" -n "$NAMESPACE" ${DRY_RUN:+--dry-run=client}
        done
        sleep 10
        show_status
        ;;
        
    scale)
        check_k8s
        if [[ -z "$SERVICE_NAME" ]]; then
            log_error "扩缩容需要指定服务 (-s SERVICE_NAME)"
            exit 1
        fi
        log_info "扩缩容: $SERVICE_NAME -> ${REPLICAS}副本"
        kubectl scale deployment/"$SERVICE_NAME" -n "$NAMESPACE" --replicas="$REPLICAS" \
            ${DRY_RUN:+--dry-run=client}
        show_status
        ;;
        
    status)
        check_k8s
        show_status
        ;;
        
    logs)
        check_k8s
        if [[ -n "$SERVICE_NAME" ]]; then
            kubectl logs -f -l "app.kubernetes.io/name=${SERVICE_NAME%-service}" -n "$NAMESPACE" --tail=100 2>/dev/null || \
                kubectl logs -f deployment/"$SERVICE_NAME" -n "$NAMESPACE" --tail=100 2>/dev/null || \
                log_error "无法获取 $SERVICE_NAME 日志"
        else
            kubectl logs -l "app.kubernetes.io/part-of=beijixing-ai" -n "$NAMESPACE" --tail=50 2>/dev/null || true
        fi
        ;;
        
    rollback)
        source "$SCRIPTS_DIR/../ops/rollback.sh"
        rollback_k8s "$SERVICE_NAME"
        ;;
        
    cleanup)
        check_k8s
        
        if confirm "⚠️  确认清理命名空间 $NAMESPACE 的所有资源?"; then
            kubectl delete namespace "$NAMESPACE" ${DRY_RUN:+--dry-run=client} 2>&1
            log_success "已清理命名空间: $NAMESPACE"
        fi
        ;;
esac
