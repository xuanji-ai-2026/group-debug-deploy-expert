#!/bin/bash
# ============================================================
#  北极星AI - 环境管理库
#  用途: 统一管理 .env 文件加载、环境变量验证
#  安全原则: 敏感信息不硬编码，从 .env 文件读取
# ============================================================

[[ -n "$_ENV_LOADED" ]] && return 0
_ENV_LOADED=1

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DEPLOY_DIR="$PROJECT_ROOT/deploy"
CONFIG_DIR="$DEPLOY_DIR/config"
SCRIPTS_DIR="$DEPLOY_DIR/scripts"

source "$SCRIPTS_DIR/lib/common.sh"

# ============================================================
#  环境检测与加载
# ============================================================
detect_env() {
    if [[ -n "$DEPLOY_ENV" ]]; then
        echo "$DEPLOY_ENV"
        return
    fi
    
    if [[ -f "$CONFIG_DIR/.env.prod" ]] && grep -q "ENV=prod" "$CONFIG_DIR/.env.prod" 2>/dev/null; then
        echo "prod"
        return
    fi
    
    echo "dev"
}

load_env() {
    local env=${1:-$(detect_env)}
    local env_file="$CONFIG_DIR/.env.$env"
    
    if [[ ! -f "$env_file" ]]; then
        log_error "环境配置文件不存在: $env_file"
        log_error "请复制 .env.example 并配置: cp $CONFIG_DIR/.env.example $env_file"
        return 1
    fi
    
    log_info "加载环境配置: $env ($env_file)"
    
    set -a
    source "$env_file"
    set +a
    
    export DEPLOY_ENV=$env
    export DEPLOY_ENV_FILE=$env_file
    
    log_success "环境变量加载完成: DEPLOY_ENV=$env"
    return 0
}

validate_env() {
    local required_vars=(
        "DB_HOST"
        "DB_PORT"
        "DB_USER"
        "DB_PASS"
        "REDIS_HOST"
        "REDIS_PORT"
        "REDIS_PASS"
        "NACOS_ADDR"
    )
    
    local missing=()
    
    for var in "${required_vars[@]}"; do
        if [[ -z "${!var}" ]]; then
            missing+=("$var")
        fi
    done
    
    if (( ${#missing[@]} > 0 )); then
        log_error "缺少必需的环境变量:"
        for var in "${missing[@]}"; do
            log_error "  - $var 未设置"
        done
        return 1
    fi
    
    log_info "环境变量验证通过 (${#required_vars[@]} 个变量)"
    return 0
}

# ============================================================
#  服务配置 (从 services.yaml 或默认值)
# ============================================================
get_service_port() {
    local service_name=$1
    case $service_name in
        gateway)   echo "${GATEWAY_PORT:-8080}" ;;
        user)      echo "${USER_PORT:-8081}" ;;
        tenant)    echo "${TENANT_PORT:-8082}" ;;
        content)   echo "${CONTENT_PORT:-8083}" ;;
        lead)      echo "${LEAD_PORT:-8084}" ;;
        risk)      echo "${RISK_PORT:-8085}" ;;
        billing)   echo "${BILLING_PORT:-8086}" ;;
        ai)        echo "${AI_PORT:-8087}" ;;
        message)   echo "${MESSAGE_PORT:-8088}" ;;
        storage)   echo "${STORAGE_PORT:-8089}" ;;
        web)       echo "${WEB_PORT:-8090}" ;;
        *)         log_error "未知服务: $service_name"; return 1 ;;
    esac
}

get_jvm_opts() {
    local service_name=$1
    local memory_tier=${MEMORY_TIER:-standard}
    
    case $memory_tier in
        micro)
            echo "-Xms32m -Xmx64m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Xss256k"
            ;;
        small)
            echo "-Xms64m -Xmx128m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Xss256k"
            ;;
        standard)
            echo "-Xms128m -Xmx256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Xss512k"
            ;;
        large)
            echo "-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Xss512k"
            ;;
        *)
            echo "-Xms128m -Xmx256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Xss512k"
            ;;
    esac
}

get_all_services() {
    echo "gateway user tenant content lead risk billing ai message storage web"
}

# ============================================================
#  版本管理
# ============================================================
get_version() {
    local component=$1
    local versions_file="$CONFIG_DIR/versions.yaml"
    
    if [[ -f "$versions_file" ]]; then
        local version
        version=$(grep "^${component}:" "$versions_file" 2>/dev/null | awk -F': ' '{print $2}' | tr -d '"' | tr -d "'")
        if [[ -n "$version" ]]; then
            echo "$version"
            return
        fi
    fi
    
    echo "${DEFAULT_VERSION:-1.0.0-SNAPSHOT}"
}

increment_version() {
    local component=$1
    local versions_file="$CONFIG_DIR/versions.yaml"
    local current_version
    current_version=$(get_version "$component")
    
    local major minor patch
    IFS='.' read -r major minor patch <<< "$current_version"
    
    if [[ "$patch" == *"SNAPSHOT" ]]; then
        patch="SNAPSHOT"
    else
        patch=$((patch + 1))
    fi
    
    local new_version="${major}.${minor}.${patch}"
    
    if [[ -f "$versions_file" ]]; then
        sed -i "s/^${component}:.*/${component}: ${new_version}/" "$versions_file"
    fi
    
    echo "$new_version"
}

# ============================================================
#  路径工具
# ============================================================
get_backend_dir() { echo "$PROJECT_ROOT/backend"; }
get_frontend_dir() { echo "$PROJECT_ROOT/frontend"; }
get_android_dir()  { echo "$PROJECT_ROOT/mobile/android"; }
get_ios_dir()      { echo "$PROJECT_ROOT/mobile/ios"; }
get_log_dir()      { echo "$DEPLOY_DIR/logs"; }
get_backup_dir()   { echo "$DEPLOY_DIR/archive/backups/$(ts)"; }
get_archive_dir()  { echo "$DEPLOY_DIR/archive"; }

# ============================================================
#  Android SDK 真实路径检测 (P7 真实操作原则)
# ============================================================
init_android_sdk() {
    if [[ -n "$ANDROID_SDK_ROOT" && -d "$ANDROID_SDK_ROOT" ]]; then
        export ANDROID_HOME="$ANDROID_SDK_ROOT"
        return 0
    fi

    local sdk_paths=(
        "$HOME/AppData/Local/Android/Sdk"
        "$HOME/Library/Android/sdk"
        "/usr/local/android-sdk"
        "/home/$USER/Android/Sdk"
        "C:/Users/$USER/AppData/Local/Android/Sdk"
    )

    for p in "${sdk_paths[@]}"; do
        if [[ -d "$p" && -f "$p/platform-tools/adb.exe" ]]; then
            export ANDROID_HOME="$p"
            export ANDROID_SDK_ROOT="$p"
            break
        fi
    done

    if [[ -z "$ANDROID_HOME" ]]; then
        log_warn "Android SDK 未检测到，请设置 ANDROID_HOME 或安装 Android Studio"
        return 1
    fi

    local adb="$ANDROID_HOME/platform-tools/adb.exe"
    if [[ ! -f "$adb" ]]; then
        adb="$ANDROID_HOME/platform-tools/adb"
    fi

    if [[ -f "$adb" ]]; then
        export ADB="$adb"
        log_info "Android SDK: $ANDROID_HOME"
        log_info "ADB: $adb ($($adb version 2>/dev/null | head -1))"
        return 0
    fi

    return 1
}

get_adb() {
    if [[ -n "${ADB:-}" && -x "$ADB" ]]; then
        echo "$ADB"
        return 0
    fi

    local candidates=(
        "${ANDROID_HOME:-}/platform-tools/adb.exe"
        "${ANDROID_HOME:-}/platform-tools/adb"
        "C:/Program Files (x86)/MiPhoneAssistant/adb.exe"
        "D:/leidian/LDPlayerVK/adb.exe"
        "$(which adb 2>/dev/null)"
    )

    for c in "${candidates[@]}"; do
        if [[ -n "$c" && -f "$c" ]]; then
            echo "$c"
            return 0
        fi
    done

    return 1
}

check_android_device() {
    local adb
    adb=$(get_adb) || { log_error "ADB不可用"; return 1; }

    local devices
    devices=$("$adb" devices 2>/dev/null | grep -E "device|unauthorized|recovery|bootloader" | grep -v "List of devices")

    if [[ -z "$devices" ]]; then
        log_warn "未检测到Android设备/模拟器"
        return 1
    fi

    log_info "已连接设备:"
    echo "$devices" | while read line; do
        log_info "  $line"
    done
    return 0
}

ensure_dirs() {
    mkdir -p "$(get_log_dir)"
    mkdir -p "$(get_backup_dir)"
    mkdir -p "$DEPLOY_DIR/archive/configs"
    mkdir -p "$DEPLOY_DIR/archive/releases"
}

log_info "环境管理库加载完成 (env.sh)"
