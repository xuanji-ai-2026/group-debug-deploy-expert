#!/bin/bash
# ============================================================
#  北极星AI - 公共函数库
#  基于2025-2026业界最佳实践
#  用途: 所有脚本的基础依赖，提供日志/颜色/工具检查等通用能力
# ============================================================

# 防止重复加载
[[ -n "$_COMMON_LOADED" ]] && return 0
_COMMON_LOADED=1

# ============================================================
#  颜色定义
# ============================================================
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly CYAN='\033[0;36m'
readonly BOLD='\033[1m'
readonly NC='\033[0m'

# ============================================================
#  日志系统 (带时间戳+级别)
# ============================================================
_log() {
    local level=$1
    shift
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    case $level in
        INFO)     echo -e "${GREEN}[INFO]${NC}    ${timestamp} $*" ;;
        WARN)     echo -e "${YELLOW}[WARN]${NC}    ${timestamp} $*" >&2 ;;
        ERROR)    echo -e "${RED}[ERROR]${NC}   ${timestamp} $*" >&2 ;;
        SUCCESS)  echo -e "${GREEN}[SUCCESS]${NC} ${timestamp} $*" ;;
        CRITICAL) echo -e "${RED}[CRITICAL]${NC}${timestamp} $*" >&2 ;;
        *)        echo -e "${timestamp} $*" ;;
    esac
}
log_info()    { _log INFO "$@"; }
log_warn()    { _log WARN "$@"; }
log_error()   { _log ERROR "$@"; }
log_success() { _log SUCCESS "$@"; }
log_critical(){ _log CRITICAL "$@"; }

# ============================================================
#  工具检查 (P7 真实操作原则: 必须验证工具可用性)
# ============================================================
require_tool() {
    local tool=$1
    local install_hint=${2:-"请安装 $tool"}
    
    if ! command -v "$tool" &> /dev/null; then
        log_error "必需工具未找到: $tool"
        log_error "安装提示: $install_hint"
        return 1
    fi
    log_info "工具检查通过: $tool ($(command -v "$tool"))"
    return 0
}

check_prerequisites() {
    local failed=0
    for tool in "$@"; do
        require_tool "$tool" || ((failed++))
    done
    return $failed
}

# ============================================================
#  版本检查 (P11 版本锁定原则)
# ============================================================
check_version() {
    local tool=$1
    local min_version=$2
    
    if ! command -v "$tool" &> /dev/null; then
        return 1
    fi
    
    local current_version
    current_version=$("$tool" --version 2>&1 | head -1 | grep -oE '[0-9]+\.[0-9]+(\.[0-9]+)?' | head -1)
    
    if [[ -z "$current_version" ]]; then
        log_warn "无法获取 $tool 版本信息"
        return 0
    fi
    
    if ! version_gte "$current_version" "$min_version"; then
        log_error "$tool 版本过低: 需要 >= $min_version, 当前: $current_version"
        return 1
    fi
    
    log_info "$tool 版本检查通过: $current_version (需要 >= $min_version)"
    return 0
}

version_gte() {
    local v1=$1 v2=$2
    printf '%s\n%s\n' "$v2" "$v1" | sort -V | tail -1 | grep -q "^$v1$"
}

# ============================================================
#  端口检查 (P8 预行动风险验证)
# ============================================================
check_port() {
    local port=$1
    local service_name=${2:-"服务"}
    
    if netstat -tlnp 2>/dev/null | grep -q ":${port} "; then
        local pid=$(netstat -tlnp 2>/dev/null | grep ":${port} " | awk '{print $7}' | cut -d'/' -f1)
        log_warn "端口 ${port} 已被占用 (${service_name}, PID: ${pid:-unknown})"
        return 1
    fi
    log_info "端口 ${port} 可用"
    return 0
}

wait_for_port() {
    local port=$1
    local timeout=${2:-30}
    local service_name=${3:-"服务"}
    
    log_info "等待 ${service_name} 启动在端口 ${port} (超时: ${timeout}s)..."
    
    local elapsed=0
    while (( elapsed < timeout )); do
        if netstat -tlnp 2>/dev/null | grep -q ":${port} "; then
            log_success "${service_name} 已启动在端口 ${port}"
            return 0
        fi
        sleep 2
        ((elapsed+=2))
    done
    
    log_error "${service_name} 启动超时 (${timeout}s)"
    return 1
}

# ============================================================
#  进程管理 (P16 资源配额治理)
# ============================================================
find_process() {
    local pattern=$1
    pgrep -f "$pattern" 2>/dev/null
}

kill_process() {
    local pattern=$1
    local signal=${2:-TERM}
    local timeout=${3:-10}
    
    local pids
    pids=$(find_process "$pattern")
    
    if [[ -z "$pids" ]]; then
        log_info "没有找到匹配进程: $pattern"
        return 0
    fi
    
    log_info "发送 ${signal} 信号给进程 (PIDs: $pids)"
    kill -$signal $pids 2>/dev/null
    
    local elapsed=0
    while (( elapsed < timeout )) && find_process "$pattern" &>/dev/null; do
        sleep 1
        ((elapsed++))
    done
    
    if find_process "$pattern" &>/dev/null; then
        log_warn "进程未响应 ${signal} 信号，强制终止..."
        kill -9 $pids 2>/dev/null
        sleep 1
    fi
    
    log_success "进程已终止: $pattern"
    return 0
}

# ============================================================
#  HTTP健康检查 (P1 绝对真实原则)
# ============================================================
health_check() {
    local url=$1
    local expected_code=${2:-200}
    local timeout=${3:-10}
    local retries=${3:-3}
    local service_name=${4:-"服务"}
    
    for i in $(seq 1 $retries); do
        local response_code
        response_code=$(curl -s -o /dev/null -w "%{http_code}" \
            --connect-timeout "$timeout" --max-time "$timeout" \
            "$url" 2>/dev/null)
        
        if [[ "$response_code" == "$expected_code" ]]; then
            log_success "${service_name} 健康检查通过: ${url} (HTTP ${response_code})"
            return 0
        fi
        
        log_warn "${service_name} 健康检查失败 (尝试 $i/$retries): HTTP ${response_code}"
        sleep 2
    done
    
    log_error "${service_name} 健康检查最终失败: ${url} (期望: HTTP ${expected_code})"
    return 1
}

wait_for_healthy() {
    local url=$1
    local timeout=${2:-120}
    local service_name=${3:-"服务"}
    
    log_info "等待 ${service_name} 变为健康状态 (超时: ${timeout}s)..."
    
    local start_time=$(date +%s)
    while true; do
        local current_time=$(date +%s)
        local elapsed=$((current_time - start_time))
        
        if (( elapsed >= timeout )); then
            log_error "${service_name} 健康等待超时 (${timeout}s)"
            return 1
        fi
        
        local code
        code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 --max-time 5 "$url" 2>/dev/null || echo "000")
        
        if [[ "$code" == "200" ]]; then
            log_success "${service_name} 已就绪 (${elapsed}s)"
            return 0
        fi
        
        sleep 3
    done
}

# ============================================================
#  文件操作安全 (P12 预备份原则)
# ============================================================
backup_file() {
    local file_path=$1
    local backup_dir=${2:-"./backup/$(date +%Y%m%d_%H%M%S)"}
    
    if [[ ! -f "$file_path" ]]; then
        log_warn "文件不存在，跳过备份: $file_path"
        return 0
    fi
    
    mkdir -p "$backup_dir"
    local backup_path="${backup_dir}/$(basename "$file_path").bak.$(date +%s)"
    
    cp -a "$file_path" "$backup_path"
    log_info "文件已备份: $file_path -> $backup_path"
    echo "$backup_path"
}

safe_write() {
    local file_path=$1
    local content=$2
    local create_backup=${3:-true}
    
    if [[ "$create_backup" == "true" ]] && [[ -f "$file_path" ]]; then
        backup_file "$file_path"
    fi
    
    echo "$content" > "$file_path"
    log_info "文件已写入: $file_path"
}

# ============================================================
#  错误处理与重试 (3-Exchange Stop Rule)
# ============================================================
retry() {
    local max_attempts=$1
    shift
    local description=$1
    shift
    
    local attempt=1
    while (( attempt <= max_attempts )); do
        log_info "执行 [${attempt}/${max_attempts}]: $description"
        
        if "$@"; then
            log_success "执行成功: $description"
            return 0
        fi
        
        if (( attempt < max_attempts )); then
            log_warn "执行失败，等待后重试... ($((max_attempts - attempt)) 次剩余)"
            sleep $((attempt * 2))
        fi
        
        ((attempt++))
    done
    
    log_error "执行最终失败 (已重试 ${max_attempts} 次): $description"
    return 1
}

# ============================================================
#  确认提示 (高风险操作保护)
# ============================================================
confirm() {
    local message=${1:-"确认执行此操作?"}
    local auto_confirm=${AUTO_CONFIRM:-false}
    
    if [[ "$auto_confirm" == "true" ]]; then
        return 0
    fi
    
    echo -en "${YELLOW}${message} (y/N): ${NC}"
    read -r answer
    [[ "$answer" =~ ^[Yy]$ ]]
}

# ============================================================
#  时间格式化
# ============================================================
now() { date '+%Y-%m-%d %H:%M:%S'; }
ts()   { date '+%Y%m%d_%H%M%S'; }

# ============================================================
#  加载顺序声明
# ============================================================
log_info "公共函数库加载完成 (common.sh)"
