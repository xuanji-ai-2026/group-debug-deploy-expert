#!/bin/bash
#==============================================================================
# 北极星AI商机获客系统 - 健康检查脚本
# Health Check Script for Beijixing AI Business Acquisition System
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
readonly MAGENTA='\033[0;35m'
readonly NC='\033[0m'

#==============================================================================
# 日志配置
#==============================================================================
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
readonly LOG_DIR="${PROJECT_ROOT}/logs"
readonly LOG_FILE="${LOG_DIR}/health-check-$(date +%Y%m%d-%H%M%S).log"
readonly REPORT_DIR="${PROJECT_ROOT}/reports"

#==============================================================================
# 服务配置
#==============================================================================
declare -A SERVICE_ENDPOINTS=(
    ["gateway"]="http://localhost:8080/actuator/health"
    ["user-service"]="http://localhost:8081/actuator/health"
    ["business-service"]="http://localhost:8082/actuator/health"
    ["ai-service"]="http://localhost:8083/actuator/health"
    ["notification-service"]="http://localhost:8084/actuator/health"
    ["admin-service"]="http://localhost:8085/actuator/health"
)

declare -A INFRA_ENDPOINTS=(
    ["nacos"]="http://localhost:8848/nacos"
    ["mysql"]="localhost:3306"
    ["redis"]="localhost:6379"
    ["rabbitmq"]="http://localhost:15672"
    ["elasticsearch"]="http://localhost:9200/_cluster/health"
)

#==============================================================================
# 检查结果统计
#==============================================================================
declare -A CHECK_RESULTS
declare -i TOTAL_CHECKS=0
declare -i PASSED_CHECKS=0
declare -i FAILED_CHECKS=0

#==============================================================================
# 初始化
#==============================================================================
init() {
    mkdir -p "${LOG_DIR}"
    mkdir -p "${REPORT_DIR}"
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

#==============================================================================
# 使用说明
#==============================================================================
usage() {
    cat << EOF
${CYAN}北极星AI商机获客系统 - 健康检查脚本${NC}

用法: $0 [选项]

选项:
    -e, --env ENV          指定环境 (dev|prod) [默认: dev]
    -s, --service SERVICE  只检查指定服务
    -t, --type TYPE        检查类型 (all|infra|app|custom) [默认: all]
    -o, --output FORMAT    输出格式 (text|json|html) [默认: text]
    -f, --file PATH        保存报告到文件
    -q, --quiet            静默模式，只输出结果
    -h, --help             显示此帮助信息

示例:
    $0                                      # 完整健康检查
    $0 --type infra                         # 只检查基础设施
    $0 --service gateway                    # 只检查网关服务
    $0 --output json --file report.json     # 输出JSON报告

EOF
}

#==============================================================================
# 参数解析
#==============================================================================
parse_args() {
    ENV="dev"
    TARGET_SERVICE=""
    CHECK_TYPE="all"
    OUTPUT_FORMAT="text"
    REPORT_FILE=""
    QUIET=false

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
            -t|--type)
                CHECK_TYPE="$2"
                shift 2
                ;;
            -o|--output)
                OUTPUT_FORMAT="$2"
                shift 2
                ;;
            -f|--file)
                REPORT_FILE="$2"
                shift 2
                ;;
            -q|--quiet)
                QUIET=true
                shift
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            *)
                echo "未知参数: $1" >&2
                usage
                exit 1
                ;;
        esac
    done

    # 验证参数
    if [[ ! "$CHECK_TYPE" =~ ^(all|infra|app|custom)$ ]]; then
        echo "无效的检查类型: $CHECK_TYPE" >&2
        exit 1
    fi

    if [[ ! "$OUTPUT_FORMAT" =~ ^(text|json|html)$ ]]; then
        echo "无效的输出格式: $OUTPUT_FORMAT" >&2
        exit 1
    fi
}

#==============================================================================
# 记录检查结果
#==============================================================================
record_result() {
    local service=$1
    local status=$2
    local message=$3
    
    TOTAL_CHECKS+=1
    if [[ "$status" == "PASS" ]]; then
        PASSED_CHECKS+=1
    else
        FAILED_CHECKS+=1
    fi
    
    CHECK_RESULTS["${service}"]="${status}|${message}|$(date '+%Y-%m-%d %H:%M:%S')"
}

#==============================================================================
# 检查HTTP端点
#==============================================================================
check_http_endpoint() {
    local name=$1
    local url=$2
    local timeout=${3:-10}
    
    if [[ "$QUIET" == false ]]; then
        echo -n "检查 $name ... "
    fi
    
    local response
    local http_code
    
    response=$(curl -sf --max-time "$timeout" "$url" 2>&1) && http_code=200 || http_code=0
    
    if [[ "$http_code" == "200" ]]; then
        if [[ "$QUIET" == false ]]; then
            echo -e "${GREEN}✓${NC}"
        fi
        record_result "$name" "PASS" "HTTP 200 OK"
        return 0
    else
        if [[ "$QUIET" == false ]]; then
            echo -e "${RED}✗${NC}"
        fi
        record_result "$name" "FAIL" "无法访问 (${http_code})"
        return 1
    fi
}

#==============================================================================
# 检查TCP端口
#==============================================================================
check_tcp_port() {
    local name=$1
    local host_port=$2
    local timeout=${3:-5}
    
    if [[ "$QUIET" == false ]]; then
        echo -n "检查 $name ($host_port) ... "
    fi
    
    local host="${host_port%%:*}"
    local port="${host_port##*:}"
    
    if nc -z -w "$timeout" "$host" "$port" 2>/dev/null; then
        if [[ "$QUIET" == false ]]; then
            echo -e "${GREEN}✓${NC}"
        fi
        record_result "$name" "PASS" "端口连通"
        return 0
    else
        if [[ "$QUIET" == false ]]; then
            echo -e "${RED}✗${NC}"
        fi
        record_result "$name" "FAIL" "端口不通"
        return 1
    fi
}

#==============================================================================
# 检查基础设施
#==============================================================================
check_infrastructure() {
    log_step "检查基础设施服务"
    
    echo -e "\n${CYAN}=== 基础设施检查 ===${NC}"
    
    for name in "${!INFRA_ENDPOINTS[@]}"; do
        local endpoint="${INFRA_ENDPOINTS[$name]}"
        
        # 如果指定了特定服务，跳过其他
        if [[ -n "$TARGET_SERVICE" ]] && [[ "$name" != "$TARGET_SERVICE" ]]; then
            continue
        fi
        
        if [[ "$endpoint" == http* ]]; then
            check_http_endpoint "$name" "$endpoint"
        else
            check_tcp_port "$name" "$endpoint"
        fi
    done
}

#==============================================================================
# 检查应用服务
#==============================================================================
check_applications() {
    log_step "检查应用服务"
    
    echo -e "\n${CYAN}=== 应用服务检查 ===${NC}"
    
    for name in "${!SERVICE_ENDPOINTS[@]}"; do
        local endpoint="${SERVICE_ENDPOINTS[$name]}"
        
        # 如果指定了特定服务，跳过其他
        if [[ -n "$TARGET_SERVICE" ]] && [[ "$name" != "$TARGET_SERVICE" ]]; then
            continue
        fi
        
        check_http_endpoint "$name" "$endpoint"
    done
}

#==============================================================================
# 检查系统资源
#==============================================================================
check_system_resources() {
    log_step "检查系统资源"
    
    echo -e "\n${CYAN}=== 系统资源检查 ===${NC}"
    
    # CPU使用率
    local cpu_usage
    cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1 || echo "0")
    cpu_usage=${cpu_usage%.*}
    
    if [[ "$cpu_usage" -lt 80 ]]; then
        echo -e "CPU使用率: ${GREEN}${cpu_usage}%${NC}"
        record_result "CPU" "PASS" "${cpu_usage}%"
    else
        echo -e "CPU使用率: ${RED}${cpu_usage}%${NC} (过高)"
        record_result "CPU" "WARN" "${cpu_usage}%"
    fi
    
    # 内存使用率
    local mem_info
    mem_info=$(free | grep Mem)
    local mem_total=$(echo "$mem_info" | awk '{print $2}')
    local mem_used=$(echo "$mem_info" | awk '{print $3}')
    local mem_usage=$((mem_used * 100 / mem_total))
    
    if [[ "$mem_usage" -lt 80 ]]; then
        echo -e "内存使用率: ${GREEN}${mem_usage}%${NC}"
        record_result "Memory" "PASS" "${mem_usage}%"
    else
        echo -e "内存使用率: ${RED}${mem_usage}%${NC} (过高)"
        record_result "Memory" "WARN" "${mem_usage}%"
    fi
    
    # 磁盘使用率
    local disk_usage
    disk_usage=$(df -h / | tail -1 | awk '{print $5}' | tr -d '%')
    
    if [[ "$disk_usage" -lt 80 ]]; then
        echo -e "磁盘使用率: ${GREEN}${disk_usage}%${NC}"
        record_result "Disk" "PASS" "${disk_usage}%"
    else
        echo -e "磁盘使用率: ${RED}${disk_usage}%${NC} (过高)"
        record_result "Disk" "WARN" "${disk_usage}%"
    fi
}

#==============================================================================
# 检查Docker容器
#==============================================================================
check_docker_containers() {
    log_step "检查Docker容器"
    
    echo -e "\n${CYAN}=== Docker容器检查 ===${NC}"
    
    if ! docker info &> /dev/null; then
        echo -e "${RED}Docker未运行${NC}"
        record_result "Docker" "FAIL" "Docker守护进程未运行"
        return 1
    fi
    
    echo -e "${GREEN}Docker守护进程运行正常${NC}"
    record_result "Docker" "PASS" "运行正常"
    
    # 检查容器状态
    local containers
    containers=$(docker ps --format '{{.Names}}' 2>/dev/null | grep -E 'gateway|user|business|ai|notification|admin|mysql|redis|nacos|rabbitmq|elasticsearch' || true)
    
    if [[ -z "$containers" ]]; then
        echo -e "${YELLOW}未找到相关容器${NC}"
        return 0
    fi
    
    echo -e "\n容器状态:"
    printf "%-30s %-15s %-15s\n" "容器名" "状态" "运行时间"
    echo "----------------------------------------------------------"
    
    while IFS= read -r container; do
        local status
        status=$(docker inspect --format='{{.State.Status}}' "$container" 2>/dev/null)
        local health
        health=$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}N/A{{end}}' "$container" 2>/dev/null)
        local uptime
        uptime=$(docker inspect --format='{{.State.StartedAt}}' "$container" 2>/dev/null | xargs -I {} date -d {} +%H:%M:%S 2>/dev/null || echo "N/A")
        
        if [[ "$status" == "running" ]]; then
            printf "%-30s ${GREEN}%-15s${NC} %-15s\n" "$container" "$status" "$uptime"
            record_result "container:$container" "PASS" "运行中"
        else
            printf "%-30s ${RED}%-15s${NC} %-15s\n" "$container" "$status" "$uptime"
            record_result "container:$container" "FAIL" "状态异常: $status"
        fi
    done <<< "$containers"
}

#==============================================================================
# 生成文本报告
#==============================================================================
generate_text_report() {
    local report_file=$1
    
    {
        echo "=============================================="
        echo "    北极星AI商机获客系统 - 健康检查报告"
        echo "=============================================="
        echo "检查时间: $(date '+%Y-%m-%d %H:%M:%S')"
        echo "环境: $ENV"
        echo "=============================================="
        echo ""
        echo "检查结果汇总:"
        echo "  总检查项: $TOTAL_CHECKS"
        echo "  通过: $PASSED_CHECKS"
        echo "  失败: $FAILED_CHECKS"
        echo ""
        echo "详细结果:"
        echo "----------------------------------------------"
        printf "%-30s %-10s %s\n" "检查项" "状态" "消息"
        echo "----------------------------------------------"
        
        for key in "${!CHECK_RESULTS[@]}"; do
            local value="${CHECK_RESULTS[$key]}"
            local status="${value%%|*}"
            local message="${value#*|}"
            message="${message%%|*}"
            
            printf "%-30s %-10s %s\n" "$key" "$status" "$message"
        done
        
        echo "----------------------------------------------"
        echo ""
        
        if [[ $FAILED_CHECKS -eq 0 ]]; then
            echo "✓ 所有检查通过，系统健康"
        else
            echo "✗ 发现 $FAILED_CHECKS 个问题，请检查"
        fi
        
        echo "=============================================="
        echo "日志文件: $LOG_FILE"
        echo "=============================================="
    } | tee "$report_file"
}

#==============================================================================
# 生成JSON报告
#==============================================================================
generate_json_report() {
    local report_file=$1
    
    {
        echo "{"
        echo "  \"timestamp\": \"$(date -Iseconds)\","
        echo "  \"environment\": \"$ENV\","
        echo "  \"summary\": {"
        echo "    \"total\": $TOTAL_CHECKS,"
        echo "    \"passed\": $PASSED_CHECKS,"
        echo "    \"failed\": $FAILED_CHECKS"
        echo "  },"
        echo "  \"results\": ["
        
        local first=true
        for key in "${!CHECK_RESULTS[@]}"; do
            local value="${CHECK_RESULTS[$key]}"
            local status="${value%%|*}"
            local rest="${value#*|}"
            local message="${rest%%|*}"
            local time="${rest##*|}"
            
            if [[ "$first" == true ]]; then
                first=false
            else
                echo ","
            fi
            
            echo -n "    {"
            echo -n "\"name\": \"$key\", "
            echo -n "\"status\": \"$status\", "
            echo -n "\"message\": \"$message\", "
            echo -n "\"timestamp\": \"$time\""
            echo -n "}"
        done
        
        echo ""
        echo "  ]"
        echo "}"
    } > "$report_file"
    
    log_info "JSON报告已保存: $report_file"
}

#==============================================================================
# 生成HTML报告
#==============================================================================
generate_html_report() {
    local report_file=$1
    
    local overall_status="PASS"
    if [[ $FAILED_CHECKS -gt 0 ]]; then
        overall_status="FAIL"
    fi
    
    cat > "$report_file" << EOF
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>北极星AI商机获客系统 - 健康检查报告</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; }
        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
        h1 { color: #333; border-bottom: 3px solid #4CAF50; padding-bottom: 10px; }
        .summary { display: flex; gap: 20px; margin: 20px 0; }
        .summary-box { flex: 1; padding: 20px; border-radius: 8px; text-align: center; }
        .summary-box.total { background: #e3f2fd; }
        .summary-box.pass { background: #e8f5e9; }
        .summary-box.fail { background: #ffebee; }
        .summary-box h3 { margin: 0; color: #666; font-size: 14px; }
        .summary-box .number { font-size: 36px; font-weight: bold; margin: 10px 0; }
        .summary-box.total .number { color: #2196F3; }
        .summary-box.pass .number { color: #4CAF50; }
        .summary-box.fail .number { color: #f44336; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th { background: #4CAF50; color: white; padding: 12px; text-align: left; }
        td { padding: 12px; border-bottom: 1px solid #ddd; }
        tr:hover { background: #f5f5f5; }
        .status-pass { color: #4CAF50; font-weight: bold; }
        .status-fail { color: #f44336; font-weight: bold; }
        .status-warn { color: #ff9800; font-weight: bold; }
        .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; color: #666; font-size: 12px; }
    </style>
</head>
<body>
    <div class="container">
        <h1>🏥 健康检查报告</h1>
        <p><strong>检查时间:</strong> $(date '+%Y-%m-%d %H:%M:%S')</p>
        <p><strong>环境:</strong> $ENV</p>
        <p><strong>总体状态:</strong> <span class="status-${overall_status,,}">$overall_status</span></p>
        
        <div class="summary">
            <div class="summary-box total">
                <h3>总检查项</h3>
                <div class="number">$TOTAL_CHECKS</div>
            </div>
            <div class="summary-box pass">
                <h3>通过</h3>
                <div class="number">$PASSED_CHECKS</div>
            </div>
            <div class="summary-box fail">
                <h3>失败</h3>
                <div class="number">$FAILED_CHECKS</div>
            </div>
        </div>
        
        <h2>详细检查结果</h2>
        <table>
            <thead>
                <tr>
                    <th>检查项</th>
                    <th>状态</th>
                    <th>消息</th>
                    <th>时间</th>
                </tr>
            </thead>
            <tbody>
EOF

    for key in "${!CHECK_RESULTS[@]}"; do
        local value="${CHECK_RESULTS[$key]}"
        local status="${value%%|*}"
        local rest="${value#*|}"
        local message="${rest%%|*}"
        local time="${rest##*|}"
        local status_class="status-${status,,}"
        
        cat >> "$report_file" << EOF
                <tr>
                    <td>$key</td>
                    <td class="$status_class">$status</td>
                    <td>$message</td>
                    <td>$time</td>
                </tr>
EOF
    done

    cat >> "$report_file" << EOF
            </tbody>
        </table>
        
        <div class="footer">
            <p>北极星AI商机获客系统 - 自动化运维</p>
            <p>日志文件: $LOG_FILE</p>
        </div>
    </div>
</body>
</html>
EOF

    log_info "HTML报告已保存: $report_file"
}

#==============================================================================
# 主函数
#==============================================================================
main() {
    init
    parse_args "$@"
    
    if [[ "$QUIET" == false ]]; then
        log_info "=========================================="
        log_info "北极星AI商机获客系统 - 健康检查"
        log_info "环境: $ENV"
        log_info "类型: $CHECK_TYPE"
        log_info "日志: $LOG_FILE"
        log_info "=========================================="
    fi
    
    # 执行检查
    case $CHECK_TYPE in
        all)
            check_system_resources
            check_docker_containers
            check_infrastructure
            check_applications
            ;;
        infra)
            check_infrastructure
            ;;
        app)
            check_applications
            ;;
        custom)
            if [[ -n "$TARGET_SERVICE" ]]; then
                if [[ -n "${INFRA_ENDPOINTS[$TARGET_SERVICE]}" ]]; then
                    local endpoint="${INFRA_ENDPOINTS[$TARGET_SERVICE]}"
                    if [[ "$endpoint" == http* ]]; then
                        check_http_endpoint "$TARGET_SERVICE" "$endpoint"
                    else
                        check_tcp_port "$TARGET_SERVICE" "$endpoint"
                    fi
                elif [[ -n "${SERVICE_ENDPOINTS[$TARGET_SERVICE]}" ]]; then
                    check_http_endpoint "$TARGET_SERVICE" "${SERVICE_ENDPOINTS[$TARGET_SERVICE]}"
                else
                    error_exit "未知服务: $TARGET_SERVICE"
                fi
            fi
            ;;
    esac
    
    # 生成报告
    local report_path
    if [[ -n "$REPORT_FILE" ]]; then
        report_path="$REPORT_FILE"
    else
        report_path="${REPORT_DIR}/health-check-$(date +%Y%m%d-%H%M%S).${OUTPUT_FORMAT}"
    fi
    
    case $OUTPUT_FORMAT in
        text)
            generate_text_report "$report_path"
            ;;
        json)
            generate_json_report "$report_path"
            ;;
        html)
            generate_html_report "$report_path"
            ;;
    esac
    
    # 返回状态码
    if [[ $FAILED_CHECKS -eq 0 ]]; then
        exit 0
    else
        exit 1
    fi
}

main "$@"
