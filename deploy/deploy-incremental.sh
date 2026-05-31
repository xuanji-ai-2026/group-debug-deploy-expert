#!/bin/bash
# ============================================================
#  北极星AI - 增量部署脚本 v1.0
#  解决痛点: 改一行代码不需要全量重构+上传
#  
#  用法:
#    ./deploy-incremental.sh bx-user              # 单服务部署
#    ./deploy-incremental.sh bx-user bx-tenant     # 多服务部署
#    ./deploy-incremental.sh --all                 # 全部重新部署
#    ./deploy-incremental.sh --status              # 查看状态
#
#  特点:
#    ✅ 仅构建和上传变更的服务
#    ✅ 仅重启受影响的服务 (不影响其他服务)
#    ✅ 自动备份当前运行的JAR (支持回滚)
#    ✅ 部署时间从30-60分钟缩短到2-5分钟/服务
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
BACKEND_DIR="$PROJECT_ROOT/backend"
LOG_DIR="$PROJECT_ROOT/deploy/logs"
DEPLOY_LOG="$LOG_DIR/deploy-$(date +%Y%m%d-%H%M%S).log"

mkdir -p "$LOG_DIR"

SSH_HOST="root@43.160.237.122"
SSH_KEY="$HOME/.ssh/singapore.pem"  # 或指定: D:/BeijiXing-AI/singapore.pem
REMOTE_BASE="/opt/beijixing-ai"

# 服务端口映射
declare -A PORT_MAP=(
    [bx-gateway]=8080
    [bx-user]=8081
    [bx-tenant]=8082
    [bx-content]=8083
    [bx-lead]=8084
    [bx-ai]=8085
    [bx-risk]=8086
    [bx-message]=8089
    [bx-storage]=8090
    [bx-system]=8091
    [bx-data]=8092
    [bx-social]=8093
    [bx-schedule]=8094
    [bx-monitor]=8095
    [bx-billing]=8096
)

# 数据库映射
declare -A DB_MAP=(
    [bx-user]="bx_user"
    [bx-tenant]="bx_tenant"
    [bx-content]="bx_content"
    [bx-lead]="bx_lead"
    [bx-ai]="bx_ai"
    [bx-risk]="bx_risk"
    [bx-billing]="bx_billing"
    [bx-message]="bx_message"
    [bx-storage]="bx_storage"
    [bx-system]="bx_system"
    [bx-data]="bx_data"
    [bx-social]="bx_social"
    [bx-schedule]="bx_schedule"
    [bx-monitor]="bx_monitor"
)

log_info()  { echo "[INFO]  $(date '+%H:%M:%S') $*" | tee -a "$DEPLOY_LOG"; }
log_warn()  { echo "[WARN]  $(date '+%H:%M:%S') $*" | tee -a "$DEPLOY_LOG"; }
log_error() { echo "[ERROR] $(date '+%H:%M:%S') $*" | tee -a "$DEPLOY_LOG"; }
log_success() { echo "[OK]    $(date '+%H:%M:%S') $*" | tee -a "$DEPLOY_LOG"; }

usage() {
    cat << EOF
北极星AI - 增量部署工具

用法: $(basename $0) [选项] [服务列表]

选项:
    --all             部署所有服务
    --status          查看远程服务状态
    --rollback SVC    回滚指定服务到上一版本
    --list            列出所有可用服务
    -h, --help         显示帮助信息

示例:
    $(basename $0) bx-user                      # 部署单个服务
    $(basename $0) bx-user bx-content           # 部署多个服务
    $(basename $0) --all                        # 全量部署
    $(basename $0) --status                     # 查看状态
    $(basename $0) --rollback bx-user            # 回滚用户服务

EOF
}

check_prerequisites() {
    log_info "检查前置条件..."
    
    if ! command -v java &>/dev/null; then
        log_error "Java未安装"
        exit 1
    fi
    
    if ! command -v mvn &>/dev/null; then
        log_error "Maven未安装"
        exit 1
    fi
    
    if ! command -v scp &>/dev/null; then
        log_error "SCP未安装"
        exit 1
    fi
    
    if ! command -v ssh &>/dev/null; then
        log_error "SSH未安装"
        exit 1
    fi
    
    if [ ! -f "$SSH_KEY" ]; then
        # 尝试备用路径
        SSH_KEY="D:/BeijiXing-AI/singapore.pem"
        if [ ! -f "$SSH_KEY" ]; then
            log_error "SSH密钥文件不存在: $SSH_KEY"
            exit 1
        fi
    fi
    
    log_success "前置条件检查通过"
}

build_service() {
    local service=$1
    local service_dir="$BACKEND_DIR/$service"
    
    if [ ! -d "$service_dir" ]; then
        log_error "服务目录不存在: $service_dir"
        return 1
    fi
    
    log_info "构建服务: $service ..."
    
    cd "$service_dir"
    
    if mvn clean package -DskipTests -q; then
        local jar_file="$service_dir/target/$service-1.0.0-SNAPSHOT.jar"
        if [ -f "$jar_file" ]; then
            local size=$(ls -lh "$jar_file" | awk '{print $5}')
            log_success "构建成功: $service ($size)"
            return 0
        else
            log_error "构建完成但JAR文件不存在: $jar_file"
            return 1
        fi
    else
        log_error "构建失败: $service"
        return 1
    fi
}

upload_jar() {
    local service=$1
    local jar_file="$BACKEND_DIR/$service/target/$service-1.0.0-SNAPSHOT.jar"
    
    if [ ! -f "$jar_file" ]; then
        log_error "本地JAR文件不存在: $jar_file"
        return 1
    fi
    
    log_info "上传JAR: $service ..."
    
    ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$SSH_HOST" \
        "mkdir -p $REMOTE_BASE/backend/$service/target/backup"
    
    # 备份远程当前JAR
    ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$SSH_HOST" \
        "if [ -f $REMOTE_BASE/backend/$service/target/$service-1.0.0-SNAPSHOT.jar ]; then \
            cp $REMOTE_BASE/backend/$service/target/$service-1.0.0-SNAPSHOT.jar \
               $REMOTE_BASE/backend/$service/target/backup/\$(date +%Y%m%d-%H%M%S).jar; \
         fi"
    
    # 上传新JAR
    if scp -i "$SSH_KEY" -o StrictHostKeyChecking=no \
        "$jar_file" \
        "$SSH_HOST:$REMOTE_BASE/backend/$service/target/"; then
        log_success "上传成功: $service"
        return 0
    else
        log_error "上传失败: $service"
        return 1
    fi
}

restart_service() {
    local service=$1
    local port=${PORT_MAP[$service]}
    local db=${DB_MAP[$service]:-""}
    
    log_info "重启服务: $service (端口:$port) ..."
    
    ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$SSH_HOST" << EOF
echo "停止旧进程..."
pkill -f "$service.*$port" 2>/dev/null || true
sleep 2

echo "启动新进程..."
nohup java -Xms96m -Xmx128m -XX:+UseG1GC \
    -Dspring.cloud.nacos.discovery.server-addr=localhost:8848 \
    -Dspring.cloud.nacos.config.server-addr=localhost:8848 \
    -Dspring.cloud.compatibility-verifier.enabled=false \
    -Dserver.address=0.0.0.0 -Dserver.port=$port \
    -Dspring.datasource.driver-class-name=org.mariadb.jdbc.Driver \
    -Dspring.datasource.url="jdbc:mariadb://127.0.0.1:3306/${db}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai" \
    -Dspring.datasource.username=root -Dspring.datasource.password='Beijixing@2024!' \
    -Dspring.redis.host=localhost -Dspring.redis.port=6379 -Dspring.redis.password='Redis@2026Secure!' \
    -jar "$REMOTE_BASE/backend/$service/target/$service-1.0.0-SNAPSHOT.jar" \
    > "$REMOTE_BASE/logs/$service.log" 2>&1 &

echo "PID=\$!"
sleep 1
EOF
    
    sleep 3
    
    # 验证启动
    if ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$SSH_HOST" \
        "netstat -tlnp 2>/dev/null | grep -q ':$port '"; then
        log_success "服务启动成功: $service:$port"
        return 0
    else
        log_warn "服务可能还在启动中: $service (等待30秒后验证)"
        sleep 30
        if ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$SSH_HOST" \
            "netstat -tlnp 2>/dev/null | grep -q ':$port '"; then
            log_success "服务启动成功(延迟): $service:$port"
            return 0
        else
            log_error "服务启动失败: $service (查看日志: $REMOTE_BASE/logs/$service.log)"
            return 1
        fi
    fi
}

deploy_single_service() {
    local service=$1
    local start_time=$(date +%s)
    
    log_info "=========================================="
    log_info "开始部署: $service"
    log_info "=========================================="
    
    if build_service "$service" && \
       upload_jar "$service" && \
       restart_service "$service"; then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        log_success "=========================================="
        log_success "部署完成: $service (耗时: ${duration}秒)"
        log_success "=========================================="
        return 0
    else
        log_error "部署失败: $service"
        return 1
    fi
}

show_status() {
    log_info "查询远程服务状态..."
    
    ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$SSH_HOST" << 'EOF'
echo "=========================================="
echo "  北极星AI - 服务状态报告"
echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="

echo ""
echo "---------- 端口状态 ----------"
for port in 8080 8081 8082 8083 8084 8085 8086 8089 8090 8091 8092 8093 8094 8095 8096; do
    if netstat -tlnp 2>/dev/null | grep -q ":$port "; then
        pid=$(netstat -tlnp 2>/dev/null | grep ":$port " | awk '{print $7}' | cut -d'/' -f1)
        echo "[OK]   :$port (PID: $pid)"
    else
        echo "[--]   :$port"
    fi
done

echo ""
echo "---------- Nacos注册状态 ----------"
nacos_result=$(curl -s 'http://localhost:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=20' 2>/dev/null)
count=$(echo "$nacos_result" | grep -o '"count":[0-9]*' | grep -o '[0-9]*')
echo "已注册服务数: $count"
echo "$nacos_result" | python3 -m json.tool 2>/dev/null || echo "$nacos_result"

echo ""
echo "---------- 系统资源 ----------"
free -h | head -2
df -h / | tail -1
uptime
EOF
}

rollback_service() {
    local service=$1
    local backup_dir="$REMOTE_BASE/backend/$service/target/backup"
    
    log_info "回滚服务: $service ..."
    
    # 查找最新的备份
    latest_backup=$(ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$SSH_HOST" \
        "ls -t $backup_dir/*.jar 2>/dev/null | head -1")
    
    if [ -z "$latest_backup" ]; then
        log_error "没有找到可用的备份文件"
        return 1
    fi
    
    log_info "找到备份: $(basename $latest_backup)"
    
    # 恢复备份
    ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$SSH_HOST" \
        "cp '$latest_backup' $REMOTE_BASE/backend/$service/target/$service-1.0.0-SNAPSHOT.jar"
    
    # 重启服务
    restart_service "$service"
    
    log_success "回滚完成: $service"
}

list_services() {
    echo "可用服务列表:"
    echo ""
    for service in "${!PORT_MAP[@]}"; do
        printf "  %-15s 端口: %-5s 数据库: %s\n" "$service" "${PORT_MAP[$service]}" "${DB_MAP[$service]:-'无'}"
    done
}

main() {
    if [ $# -eq 0 ]; then
        usage
        exit 1
    fi
    
    case "${1:-}" in
        --all)
            check_prerequisites
            log_info "全量部署模式"
            failed=0
            for service in "${!PORT_MAP[@]}"; do
                if ! deploy_single_service "$service"; then
                    failed=$((failed + 1))
                fi
                sleep 2
            done
            
            log_info "=========================================="
            if [ $failed -eq 0 ]; then
                log_success "全部服务部署成功!"
            else
                log_warn "$failed 个服务部署失败"
            fi
            log_info "=========================================="
            ;;
            
        --status)
            show_status
            ;;
            
        --rollback)
            if [ -z "${2:-}" ]; then
                log_error "请指定要回滚的服务名"
                usage
                exit 1
            fi
            rollback_service "$2"
            ;;
            
        --list)
            list_services
            ;;
            
        -h|--help)
            usage
            ;;
            
        *)
            check_prerequisites
            log_info "增量部署模式"
            failed=0
            for service in "$@"; do
                if [[ -v PORT_MAP[$service] ]]; then
                    if ! deploy_single_service "$service"; then
                        failed=$((failed + 1))
                    fi
                else
                    log_warn "未知服务: $service (跳过)"
                fi
                sleep 1
            done
            
            log_info "=========================================="
            log_info "部署结果: $(( $# - failed ))/$# 成功"
            if [ $failed -gt 0 ]; then
                log_warn "以下服务失败 (请查看日志: $DEPLOY_LOG)"
            fi
            log_info "=========================================="
            ;;
    esac
}

main "$@"
