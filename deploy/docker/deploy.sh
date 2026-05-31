#!/bin/bash
# 北极星AI商机获客系统 - 部署脚本
# 运维总监: 叶宇 (EMP-OPS-001)

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 打印信息
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 显示帮助
show_help() {
    cat << EOF
北极星AI商机获客系统部署脚本

用法: $0 [命令] [环境]

命令:
    start       启动服务
    stop        停止服务
    restart     重启服务
    status      查看状态
    logs        查看日志
    clean       清理数据 (慎用!)
    backup      备份数据
    help        显示帮助

环境:
    dev         开发环境 (默认)
    prod        生产环境
    base        基础环境

示例:
    $0 start dev      # 启动开发环境
    $0 stop prod      # 停止生产环境
    $0 logs user      # 查看用户服务日志
    $0 status         # 查看所有服务状态

EOF
}

# 获取环境配置
get_compose_file() {
    local env=$1
    case $env in
        dev)
            echo "docker-compose.dev.yml"
            ;;
        prod)
            echo "docker-compose.prod.yml"
            ;;
        base|default)
            echo "docker-compose.yml"
            ;;
        *)
            echo "docker-compose.yml"
            ;;
    esac
}

# 检查Docker
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker未安装，请先安装Docker"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose未安装，请先安装Docker Compose"
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        print_error "Docker服务未运行，请启动Docker服务"
        exit 1
    fi
    
    print_info "Docker环境检查通过"
}

# 检查生产环境配置
check_prod_config() {
    if [ ! -d "secrets" ]; then
        print_error "生产环境缺少secrets目录，请先创建并配置密钥文件"
        echo ""
        echo "创建步骤:"
        echo "  mkdir -p secrets"
        echo "  echo 'your_password' > secrets/mysql_root_password.txt"
        echo "  # ... 其他密钥文件"
        exit 1
    fi
    
    local required_secrets=(
        "mysql_root_password.txt"
        "mysql_password.txt"
        "redis_password.txt"
        "mongo_root_username.txt"
        "mongo_root_password.txt"
        "rabbitmq_user.txt"
        "rabbitmq_password.txt"
        "elasticsearch_password.txt"
        "jwt_secret.txt"
        "grafana_admin_password.txt"
    )
    
    for secret in "${required_secrets[@]}"; do
        if [ ! -f "secrets/$secret" ]; then
            print_warn "缺少密钥文件: secrets/$secret"
        fi
    done
}

# 启动服务
start_services() {
    local env=$1
    local compose_file=$(get_compose_file $env)
    
    print_info "使用配置文件: $compose_file"
    
    if [ "$env" == "prod" ]; then
        check_prod_config
    fi
    
    print_info "正在启动服务..."
    docker-compose -f "$compose_file" up -d
    
    print_info "等待服务健康检查..."
    sleep 10
    
    print_info "服务状态:"
    docker-compose -f "$compose_file" ps
    
    echo ""
    print_info "服务启动完成!"
    
    case $env in
        dev)
            echo ""
            echo "开发环境地址:"
            echo "  Gateway API:  http://localhost:8080"
            echo "  RabbitMQ管理: http://localhost:15672"
            echo "  Swagger UI:   http://localhost:8081"
            ;;
        prod)
            echo ""
            echo "生产环境地址:"
            echo "  Gateway API:  http://localhost:80"
            echo "  RabbitMQ管理: http://localhost:15672"
            echo "  Prometheus:   http://localhost:9090"
            echo "  Grafana:      http://localhost:3000"
            ;;
    esac
}

# 停止服务
stop_services() {
    local env=$1
    local compose_file=$(get_compose_file $env)
    
    print_info "正在停止服务 ($compose_file)..."
    docker-compose -f "$compose_file" down
    print_info "服务已停止"
}

# 查看状态
show_status() {
    local env=$1
    local compose_file=$(get_compose_file $env)
    
    echo "============================================"
    echo "服务状态 ($env)"
    echo "============================================"
    docker-compose -f "$compose_file" ps
    
    echo ""
    echo "============================================"
    echo "资源使用"
    echo "============================================"
    docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}\t{{.BlockIO}}"
}

# 查看日志
show_logs() {
    local env=$1
    local service=$2
    local compose_file=$(get_compose_file $env)
    
    if [ -z "$service" ]; then
        docker-compose -f "$compose_file" logs -f --tail=100
    else
        docker-compose -f "$compose_file" logs -f --tail=100 "$service"
    fi
}

# 清理数据
clean_data() {
    local env=$1
    local compose_file=$(get_compose_file $env)
    
    print_warn "警告: 此操作将删除所有数据卷!"
    read -p "确定要继续吗? (yes/no): " confirm
    
    if [ "$confirm" == "yes" ]; then
        print_info "正在停止服务并清理数据..."
        docker-compose -f "$compose_file" down -v
        print_info "数据已清理"
    else
        print_info "操作已取消"
    fi
}

# 备份数据
backup_data() {
    local env=$1
    local backup_dir="backups/$(date +%Y%m%d_%H%M%S)"
    
    print_info "创建备份目录: $backup_dir"
    mkdir -p "$backup_dir"
    
    # 备份MySQL
    print_info "备份MySQL数据..."
    docker exec beijixing-mysql-${env} mysqldump -u root -p${MYSQL_ROOT_PASSWORD} beijixing_db > "$backup_dir/mysql_backup.sql" 2>/dev/null || true
    
    # 备份卷数据
    print_info "备份Docker卷数据..."
    docker run --rm -v beijixing-ai_mysql_data_${env}:/data -v "$(pwd)/$backup_dir":/backup alpine tar czf /backup/mysql_volume.tar.gz -C /data . 2>/dev/null || true
    
    print_info "备份完成: $backup_dir"
}

# 主函数
main() {
    local command=$1
    local env=${2:-base}
    local service=$3
    
    # 检查Docker环境
    check_docker
    
    case $command in
        start)
            start_services $env
            ;;
        stop)
            stop_services $env
            ;;
        restart)
            stop_services $env
            sleep 2
            start_services $env
            ;;
        status)
            show_status $env
            ;;
        logs)
            show_logs $env $service
            ;;
        clean)
            clean_data $env
            ;;
        backup)
            backup_data $env
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            print_error "未知命令: $command"
            show_help
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"