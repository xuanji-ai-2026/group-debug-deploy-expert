#!/bin/bash
# ============================================================
#  北极星AI - ADB 真机调试工具集
#  基于: P1绝对真实原则 + Screen-First Protocol
#  能力: 真实设备交互、屏幕截图、UI分析、日志抓取、应用管理
#  防幻觉: 所有结论必须基于adb输出，禁止推测!
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"
source "$SCRIPT_DIR/../lib/env.sh"

usage() {
    cat << EOF
北极星AI ADB真机调试工具

用法: $(basename $0) [命令] [选项]

命令:
    devices          列出已连接设备 (默认)
    screen           截取当前屏幕 (P1屏幕优先协议)
    ui-dump          导出UI层级结构 (uiautomator)
    top-activity     查看当前Activity/Fragment栈
    logcat           抓取日志 (支持过滤)
    install          安装APK到设备
    uninstall        卸载应用
    start-app        启动应用
    force-stop       强制停止应用
    clear-data       清除应用数据
    info             查看设备详细信息
    battery          查看电池状态
    cpu              查看CPU使用情况
    memory           查看内存使用情况
    storage          查看存储空间
    network          查看网络状态
    wifi             WiFi配置
    file-push        推送文件到设备
    file-pull        从设备拉取文件
    shell            执行shell命令
    input            模拟输入事件 (点击/滑动/文本)
    screenshot-record  录屏 (需要Android 10+)
    performance      性能数据采集

选项:
    -s, --serial SER   指定设备序列号 (多设备时必需)
    -p, --package PKG  指定应用包名
    -t, --tag TAG      logcat过滤标签
    -o, --output DIR   输出目录
    --format FMT       截图格式 (png/jpg, 默认png)
    --duration SEC     录屏时长 (秒)
    --verbose          详细输出

示例:
    $(basename $0) devices                    # 查看已连接设备
    $(basename $0) screen                     # 截屏保存
    $(basename $0) ui-dump                    # 导出UI结构
    $(basename $0) logcat -t "MyApp"          # 过滤日志
    $(basename $0) install app-debug.apk      # 安装APK
    $(basename $0) start-app -p com.example.app  # 启动应用
    $(basename $0) input tap 500 1200         # 点击坐标
    $(basename $0) performance                # 性能监控

EOF
}

COMMAND="devices"
DEVICE_SERIAL=""
PACKAGE_NAME=""
LOG_TAG=""
OUTPUT_DIR="./adb_output"
SCREEN_FORMAT="png"
RECORD_DURATION=30
VERBOSE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        devices|screen|ui-dump|top-activity|logcat|install|uninstall|start-app|force-stop|clear-data|info|battery|cpu|memory|storage|network|wifi|file-push|file-pull|shell|input|screenshot-record|performance)
            COMMAND=$1; shift ;;
        -h|--help) usage; exit 0 ;;
        -s|--serial) DEVICE_SERIAL=$2; shift 2 ;;
        -p|--package) PACKAGE_NAME=$2; shift 2 ;;
        -t|--tag) LOG_TAG=$2; shift 2 ;;
        -o|--output) OUTPUT_DIR=$2; shift 2 ;;
        --format) SCREEN_FORMAT=$2; shift 2 ;;
        --duration) RECORD_DURATION=$2; shift 2 ;;
        --verbose) VERBOSE=true; shift ;;
        *) log_error "未知参数: $1"; usage; exit 1 ;;
    esac
done

init_android_sdk || {
    log_error "Android SDK未初始化，请检查安装"
    exit 1
}

ADB_CMD=$(get_adb) || {
    log_error "ADB不可用"
    exit 1
}

mkdir -p "$OUTPUT_DIR"

build_adb_args() {
    local args=()
    [[ -n "$DEVICE_SERIAL" ]] && args+=("-s" "$DEVICE_SERIAL")
    echo "${args[@]}"
}

do_devices() {
    log_info "=== ADB 设备列表 ==="
    
    local args
    args=$(build_adb_args)
    
    echo ""
    printf "%-20s %-15s %-10s %s\n" "序列号" "状态" "产品" "模型"
    echo "----------------------------------------------"
    
    while IFS= read -r line; do
        if [[ -z "$line" || "$line" == *"List of devices"* || "$line" == *"daemon"* ]]; then
            continue
        fi
        
        local serial state
        serial=$(echo "$line" | awk '{print $1}')
        state=$(echo "$line" | awk '{print $2}')
        
        local product model
        product=$($ADB_CMD ${args:+$args} -s "$serial" shell getprop ro.product.name 2>/dev/null || echo "?")
        model=$($ADB_CMD ${args:+$args} -s "$serial" shell getprop ro.product.model 2>/dev/null || echo "?")
        
        printf "%-20s %-15s %-10s %s\n" "$serial" "$state" "$product" "$model"
        
        if [[ "$state" == "device" ]]; then
            local android_ver sdk_ver
            android_ver=$($ADB_CMD ${args:+$args} -s "$serial" shell getprop ro.build.version.release 2>/dev/null || echo "?")
            sdk_ver=$($ADB_CMD ${args:+$args} -s "$serial" shell getprop ro.build.version.sdk 2>/dev/null || echo "?")
            
            echo "                      Android: ${android_ver} (SDK ${sdk_ver})"
        fi
    done < <($ADB_CMD ${args:+$args} devices -l 2>/dev/null)
    
    echo ""
}

do_screen() {
    log_info "=== 屏幕截图 (P1屏幕优先协议) ==="
    
    local args
    args=$(build_adb_args)
    
    local timestamp=$(ts)
    local screen_file="$OUTPUT_DIR/screen_${timestamp}.${SCREEN_FORMAT}"
    local device_path="/sdcard/screen_${timestamp}.png"
    
    log_info "截取屏幕..."
    
    $ADB_CMD ${args:+$args} shell screencap -p "$device_path" 2>/dev/null
    
    if $ADB_CMD ${args:+$args} pull "$device_path" "$screen_file" &>/dev/null; then
        $ADB_CMD ${args:+$args} shell rm "$device_path" 2>/dev/null || true
        
        local size
        size=$(ls -lh "$screen_file" | awk '{print $5}')
        log_success "截图保存: $screen_file (${size})"
        
        if command -v identify &>/dev/null && [[ "$VERBOSE" == "true" ]]; then
            identify "$screen_file" 2>/dev/null | head -3
        fi
    else
        log_error "截图失败!"
        return 1
    fi
}

do_ui_dump() {
    log_info "=== UI层级导出 (uiautomator dump) ==="
    
    local args
    args=$(build_adb_args)
    
    local timestamp=$(ts)
    local ui_file="$OUTPUT_DIR/ui_dump_${timestamp}.xml"
    local device_path="/sdcard/window_dump_${timestamp}.xml"
    
    log_info "导出UI结构..."
    
    local dump_result
    dump_result=$($ADB_CMD ${args:+$args} shell uiautomator dump "$device_path" 2>&1)
    
    if [[ "$dump_result" == *"/sdcard/"* ]]; then
        $ADB_CMD ${args:+$args} pull "$device_path" "$ui_file" &>/dev/null
        $ADB_CMD ${args:+$args} shell rm "$device_path" 2>/dev/null || true
        
        local size
        size=$(ls -lh "$ui_file" | awk '{print $5}')
        log_success "UI结构保存: $ui_file (${size})"
        
        if [[ "$VERBOSE" == "true" ]]; then
            log_info "UI节点统计:"
            grep -c 'node="' "$ui_file" 2>/dev/null || true
            echo "前20行预览:"
            head -20 "$ui_file"
        fi
    else
        log_error "UI导出失败: $dump_result"
        return 1
    fi
}

do_top_activity() {
    log_info "=== 当前Activity信息 ==="
    
    local args
    args=$(build_adb_args)
    
    $ADB_CMD ${args:+$args} shell dumpsys activity activities 2>/dev/null | \
        grep -E "mResumedActivity|mFocusedActivity|* TaskRecord" | head -10
}

do_logcat() {
    log_info "=== Logcat 日志 ==="
    
    local args
    args=$(build_adb_args)
    
    local log_args=(-d)
    [[ -n "$LOG_TAG" ]] && log_args+=(-s "$LOG_TAG")
    log_args+=(-v time)
    
    local log_file="$OUTPUT_DIR/logcat_$(ts).log"
    
    $ADB_CMD ${args:+$args} "${log_args[@]}" > "$log_file" 2>/dev/null
    
    local lines
    lines=$(wc -l < "$log_file" 2>/dev/null || echo 0)
    log_success "日志保存: $log_file (${lines} 行)"
    
    tail -50 "$log_file"
}

do_install() {
    local apk_file=${1:-}
    
    if [[ -z "$apk_file" || ! -f "$apk_file" ]]; then
        log_error "请指定有效的APK文件"
        return 1
    fi
    
    local args
    args=$(build_adb_args)
    
    log_info "安装APK: $apk_file"
    
    $ADB_CMD ${args:+$args} install -r -d "$apk_file" 2>&1 | tee /tmp/adb_install.log
    
    if grep -q "Success" /tmp/adb_install.log 2>/dev/null; then
        log_success "安装成功"
    else
        log_error "安装失败!"
        return 1
    fi
}

do_start_app() {
    if [[ -z "$PACKAGE_NAME" ]]; then
        log_error "请指定包名 (-p PACKAGE)"
        return 1
    fi
    
    local args
    args=$(build_adb_args)
    
    log_info "启动应用: $PACKAGE_NAME"
    
    $ADB_CMD ${args:+$args} shell monkey -p "$PACKAGE_NAME" -c android.intent.category.LAUNCHER 1 2>/dev/null || \
    $ADB_CMD ${args:+$args} shell am start -n "$PACKAGE_NAME"/.MainActivity 2>/dev/null || \
    log_warn "无法直接启动，尝试启动器方式..."
}

do_force_stop() {
    if [[ -z "$PACKAGE_NAME" ]]; then
        log_error "请指定包名 (-p PACKAGE)"
        return 1
    fi
    
    local args
    args=$(build_adb_args)
    
    log_info "强制停止: $PACKAGE_NAME"
    $ADB_CMD ${args:+$args} shell am force-stop "$PACKAGE_NAME"
    log_success "已停止"
}

do_clear_data() {
    if [[ -z "$PACKAGE_NAME" ]]; then
        log_error "请指定包名 (-p PACKAGE)"
        return 1
    fi
    
    local args
    args=$(build_adb_args)
    
    confirm "确认清除 $PACKAGE_NAME 的所有数据?" || return 0
    
    $ADB_CMD ${args:+$args} shell pm clear "$PACKAGE_NAME"
    log_success "数据已清除"
}

do_input() {
    local action=${1:-}
    shift || true
    
    local args
    args=$(build_adb_args)
    
    case $action in
        tap)
            local x=${1:-0}
            local y=${2:-0}
            log_info "点击坐标: ($x, $y)"
            $ADB_CMD ${args:+$args} shell input tap "$x" "$y"
            ;;
        swipe)
            local x1=${1:-0} y1=${2:-0} x2=${3:-0} y2=${4:-0}
            local duration=${5:-300}
            log_info "滑动: ($x1,$y1) -> ($x2,$y2) (${duration}ms)"
            $ADB_CMD ${args:+$args} shell input swipe "$x1" "$y1" "$x2" "$y2" "$duration"
            ;;
        text)
            local text="$*"
            log_info "输入文本: $text"
            $ADB_CMD ${args:+$args} shell input text "$text"
            ;;
        keyevent)
            local keycode=${1:-KEYCODE_HOME}
            log_info "按键: $keycode"
            $ADB_CMD ${args:+$args} shell input keyevent "$keycode"
            ;;
        *)
            log_error "未知输入动作: $action"
            echo "用法: input [tap|swipe|text|keyevent] ..."
            return 1
            ;;
    esac
}

do_info() {
    local args
    args=$(build_adb_args)
    
    log_info "=== 设备详细信息 ==="
    
    echo ""
    echo "品牌: $($ADB_CMD ${args:+$args} shell getprop ro.brand 2>/dev/null)"
    echo "型号: $($ADB_CMD ${args:+$args} shell getprop ro.product.model 2>/dev/null)"
    echo "设备: $($ADB_CMD ${args:+$args} shell getprop ro.product.device 2>/dev/null)"
    echo "Android版本: $($ADB_CMD ${args:+$args} shell getprop ro.build.version.release 2>/dev/null)"
    echo "SDK版本: $($ADB_CMD ${args:+$args} shell getprop ro.build.version.sdk 2>/dev/null)"
    echo "安全补丁: $($ADB_CMD ${args:+$args} shell getprop ro.build.version.security_patch 2>/dev/null)"
    echo "序列号: $($ADB_CMD ${args:+$args} shell getprop ro.serialno 2>/dev/null)"
    echo "分辨率: $($ADB_CMD ${args:+$args} shell wm size 2>/dev/null)"
    echo "密度: $($ADB_CMD ${args:+$args} shell wm density 2>/dev/null)"
    echo "CPU: $($ADB_CMD ${args:+$args} shell getprop ro.soc.model 2>/dev/null || echo "未知")"
    echo "内存: $(echo 'scale=1;' $($ADB_CMD ${args:+$args} shell cat /proc/meminfo 2>/dev/null | grep MemTotal | awk '{print $2}') '/1024' | bc 2>/dev/null || echo "?") MB"
    echo "电池: $($ADB_CMD ${args:+$args} shell dumpsys battery 2>/dev/null | grep level | head -1)"
}

do_performance() {
    local args
    args=$(build_adb_args)
    
    log_info "=== 性能监控 (实时采样5次) ==="
    
    for i in $(seq 1 5); do
        clear
        echo "===== 采样 $i/5 ====="
        echo ""
        
        echo "--- CPU ---"
        $ADB_CMD ${args:+$args} shell top -n 1 -b 2>/dev/null | head -15
        
        echo ""
        echo "--- 内存 ---"
        $ADB_CMD ${args:+$args} shell dumpsys meminfo 2>/dev/null | head -20
        
        echo ""
        echo "--- GPU (如可用) ---"
        $ADB_CMD ${args:+$args} shell dumpsys gfxinfo 2>/dev/null | tail -15 || true
        
        sleep 2
    done
}

case $COMMAND in
    devices)         do_devices ;;
    screen)          do_screen ;;
    ui-dump)         do_ui_dump ;;
    top-activity)    do_top_activity ;;
    logcat)          do_logcat ;;
    install)         do_install "${1:-}" ;;
    uninstall)
        if [[ -z "$PACKAGE_NAME" ]]; then log_error "请指定包名 (-p)"; exit 1; fi
        ADB_ARGS=$(build_adb_args)
        $ADB_CMD ${ADB_ARGS:+$ADB_ARGS} uninstall "$PACKAGE_NAME"
        ;;
    start-app)       do_start_app ;;
    force-stop)      do_force_stop ;;
    clear-data)      do_clear_data ;;
    info)            do_info ;;
    battery)
        ADB_ARGS=$(build_adb_args)
        $ADB_CMD ${ADB_ARGS:+$ADB_ARGS} shell dumpsys battery
        ;;
    cpu)
        ADB_ARGS=$(build_adb_args)
        $ADB_CMD ${ADB_ARGS:+$ADB_ARGS} shell top -n 1 -b
        ;;
    memory)
        ADB_ARGS=$(build_adb_args)
        $ADB_CMD ${ADB_ARGS:+$ADB_ARGS} shell dumpsys meminfo
        ;;
    storage)
        ADB_ARGS=$(build_adb_args)
        $ADB_CMD ${ADB_ARGS:+$ADB_ARGS} shell df -h
        ;;
    network)
        ADB_ARGS=$(build_adb_args)
        $ADB_CMD ${ADB_ARGS:+$ADB_ARGS} shell dumpsys connectivity | grep -A5 "NetworkAgentInfo"
        ;;
    wifi)
        ADB_ARGS=$(build_adb_args)
        $ADB_CMD ${ADB_ARGS:+$ADB_ARGS} shell dumpsys wifi | grep -E "mNetworkInfo|Wi-Fi|SSID"
        ;;
    file-push)
        ADB_ARGS=$(build_adb_args)
        local src=${1:-} dst=${2:-/sdcard/Download/}
        $ADB_CMD ${ADB_ARGS:+$ADB_ARGS} push "$src" "$dst"
        ;;
    file-pull)
        ADB_ARGS=$(build_adb_args)
        local src=${1:-} dst=${2:-.}
        $ADB_CMD ${ADB_ARGS:+$ADB_ARGS} pull "$src" "$dst"
        ;;
    shell)
        ADB_ARGS=$(build_adb_args)
        $ADB_CMD ${ADB_ARGS:+$ADB_ARGS} shell "${@:-}"
        ;;
    input)           do_input "${@:-}" ;;
    screenshot-record)
        ADB_ARGS=$(build_adb_args)
        local record_file="$OUTPUT_DIR/screenrecord_$(ts).mp4"
        log_info "录屏 ${RECORD_DURATION}s -> $record_file"
        $ADB_CMD ${ADB_ARGS:+$ADB_ARGS} shell screenrecord --time-limit "$RECORD_DURATION" "/sdcard/recording.mp4" &
        RECORD_PID=$!
        trap "kill $RECORD_PID 2>/dev/null" EXIT
        wait $RECORD_PID
        $ADB_CMD ${ADB_ARGS:+$ADB_ARGS} pull "/sdcard/recording.mp4" "$record_file"
        log_success "录屏完成: $record_file"
        ;;
    performance)     do_performance ;;
esac
