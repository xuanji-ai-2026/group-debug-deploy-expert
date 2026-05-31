#!/bin/bash
# ============================================================
#  北极星AI - Android APK 自动化构建与签名
#  基于: Gradle + Android SDK + Fastlane 最佳实践
#  整合: 现有build.gradle.kts (API 26-34, Compose, Hilt)
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"
source "$SCRIPT_DIR/../lib/env.sh"

usage() {
    cat << EOF
北极星AI Android APK 构建签名脚本

用法: $(basename $0) [命令] [选项]

命令:
    build           构建APK (默认)
    sign            签名APK
    release         完整发布流程 (构建+签名+归档)
    clean           清理构建产物
    variants        查看可用构建变体

选项:
    --flavor FLAVOR     渠道选择 (dev|prod, 默认dev)
    --type TYPE         构建类型 (debug|release, 默认release)
    --channels CH       多渠道打包 (逗号分隔, 如 huawei,xiaomi,oppo)
    --output DIR        输出目录
    --no-sign           跳过签名步骤

示例:
    $(basename $0) build                          # Debug构建
    $(basename $0) build --flavor prod            # 生产环境Release构建
    $(basename $0) release                        # 完整发布流程
    $(basename $0) build --channels huawei,xiaomi # 多渠道打包
    $(basename $0) sign --output ./signed_apks    # 签名并输出到指定目录

EOF
}

COMMAND="build"
FLAVOR="dev"
BUILD_TYPE="release"
CHANNELS=""
OUTPUT_DIR=""
SKIP_SIGN=false

while [[ $# -gt 0 ]]; do
    case $1 in
        build|sign|release|clean|variants) COMMAND=$1; shift ;;
        -h|--help) usage; exit 0 ;;
        --flavor) FLAVOR=$2; shift 2 ;;
        --type) BUILD_TYPE=$2; shift 2 ;;
        --channels) CHANNELS=$2; shift 2 ;;
        --output) OUTPUT_DIR=$2; shift 2 ;;
        --no-sign) SKIP_SIGN=true; shift ;;
        *) log_error "未知参数: $1"; usage; exit 1 ;;
    esac
done

ANDROID_DIR=$(get_android_dir)

check_android_prerequisites() {
    local missing=()
    
    if ! command -v java &>/dev/null; then missing+=("java"); fi
    if [[ ! -f "$ANDROID_DIR/gradlew" ]]; then missing+=("gradlew"); fi
    
    ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
    export ANDROID_HOME
    
    if [[ ! -d "$ANDROID_HOME" ]]; then
        log_warn "ANDROID_HOME未设置，尝试自动检测..."
        local possible=(
            "$HOME/AppData/Local/Android/Sdk"
            "/usr/local/android-sdk"
            "/home/$USER/Android/Sdk"
        )
        for p in "${possible[@]}"; do
            if [[ -d "$p" ]]; then
                ANDROID_HOME="$p"
                break
            fi
        done
    fi
    
    if (( ${#missing[@]} > 0 )); then
        log_error "缺少Android构建依赖:"
        for m in "${missing[@]}"; do log_error "  - $m"; done
        return 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -1 | grep -oE '1\.[0-9]+\.[0-9]+' | head -1 || echo "unknown")
    GRADLE_VERSION=$("$ANDROID_DIR/gradlew" --version 2>/dev/null | grep "Gradle" | head -1 || echo "unknown")
    
    log_info "Java: ${JAVA_VERSION}"
    log_info "Gradle: ${GRADLE_VERSION}"
    log_info "Android SDK: ${ANDROID_HOME:-未知}"
    return 0
}

build_apk() {
    cd "$ANDROID_DIR"
    
    chmod +x ./gradlew 2>/dev/null || true
    
    local gradle_task="assemble${FLAVOR^}${BUILD_TYPE^}"
    
    if [[ -n "$CHANNELS" ]]; then
        IFS=',' read -ra channel_array <<< "$CHANNELS"
        
        for channel in "${channel_array[@]}"; do
            log_info "=========================================="
            log_info "  构建渠道: ${channel} (${BUILD_TYPE})"
            log_info "=========================================="
            
            ./gradlew "-Pchannel=${channel}" "$gradle_task" \
                --no-daemon --stacktrace 2>&1 | tail -30
            
            check_build_result "$channel"
        done
    else
        log_info "=========================================="
        log_info "  构建: ${FLAVOR}/${BUILD_TYPE}"
        log_info "=========================================="
        
        ./gradlew "$gradle_task" --no-daemon --stacktrace 2>&1 | tail -30
        
        check_build_result ""
    fi
}

check_build_result() {
    local channel_suffix=${1:+_$1}
    local apk_pattern="*-${FLAVOR}-${BUILD_TYPE}${channel_suffix}.apk"
    local apk_path=$(find "$ANDROID_DIR/app/build/outputs/apk/" -name "$apk_pattern" -type f 2>/dev/null | head -1)
    
    if [[ -n "$apk_path" && -f "$apk_path" ]]; then
        APK_SIZE=$(ls -lh "$apk_path" | awk '{print $5}')
        log_success "APK构建成功: $apk_path (${APK_SIZE})"
        echo "$apk_path"
    else
        log_error "APK构建失败! 查找模式: ${apk_pattern}"
        find "$ANDROID_DIR/app/build/outputs/apk/" -name "*.apk" -type f 2>/dev/null | head -10 || true
        return 1
    fi
}

sign_apk() {
    local input_apk=$1
    local output_dir=${OUTPUT_DIR:-$(pwd)/signed_apks}
    
    mkdir -p "$output_dir"
    
    local keystore_path="${ANDROID_KEYSTORE_PATH:-./keystore/beijixing-release.jks}"
    local keystore_pass="${ANDROID_KEYSTORE_PASSWORD:-}"
    local key_alias="${ANDROID_KEY_ALIAS:-beijixing}"
    local key_password="${ANDROID_KEY_PASSWORD:-}"
    
    if [[ ! -f "$keystore_path" ]]; then
        log_error "签名密钥库不存在: $keystore_path"
        log_info "请配置 .env 文件中的 ANDROID_KEYSTORE_* 变量"
        return 1
    fi
    
    if [[ -z "$keystore_pass" || -z "$key_password" ]]; then
        log_error "签名密码未配置!"
        log_info "请在 .env 文件中设置:"
        log_info "  ANDROID_KEYSTORE_PASSWORD=<密码>"
        log_info "  ANDROID_KEY_PASSWORD=<密码>"
        return 1
    fi
    
    local base_name=$(basename "$input_apk" .apk)
    local signed_apk="$output_dir/${base_name}_signed.apk"
    
    log_info "签名中... V1+V2双重签名"
    
    apksigner sign \
        --ks "$keystore_path" \
        --ks-pass "pass:${keystore_pass}" \
        --ks-key-alias "$key_alias" \
        --key-pass "pass:${key_password}" \
        --out "$signed_apk" \
        --v1-signing-enabled true \
        --v2-signing-enabled true \
        "$input_apk" 2>&1
    
    if apksigner verify -v "$signed_apk" 2>&1; then
        SIGNED_SIZE=$(ls -lh "$signed_apk" | awk '{print $5}')
        log_success "签名验证通过: $signed_apk (${SIGNED_SIZE})"
        echo "$signed_apk"
    else
        log_error "签名验证失败!"
        return 1
    fi
}

do_release() {
    local timestamp=$(ts)
    local archive_dir="$(get_archive_dir)/releases/android/${timestamp}"
    mkdir -p "$archive_dir"
    
    log_info "=========================================="
    log_info "  完整发布流程"
    log_info "  归档目录: $archive_dir"
    log_info "=========================================="
    
    build_apk
    
    if [[ "$SKIP_SIGN" != "true" ]]; then
        local built_apk
        built_apk=$(find "$ANDROID_DIR/app/build/outputs/apk/" -name "*-${FLAVOR}-${BUILD_TYPE}.apk" -type f 2>/dev/null | head -1)
        
        if [[ -n "$built_apk" ]]; then
            local signed_apk
            signed_apk=$(sign_apk "$built_apk")
            
            cp -a "$signed_apk" "$archive_dir/"
            cp -a "$built_apk" "$archive_dir/"
            
            cat > "$archive_dir/release_manifest.txt" << EOF
北极星AI Android 发布清单
时间戳: $(date '+%Y-%m-%d %H:%M:%S')
渠道: ${FLAVOR}
类型: ${BUILD_TYPE}
原始APK: $(basename "$built_apk")
签名APK: $(basename "$signed_apk")
大小: $(ls -lh "$signed_apk" | awk '{print $5}')
SHA256: $(sha256sum "$signed_apk" | cut -d' ' -f1)
EOF
            
            log_success "发布完成! 归档位置: $archive_dir"
        fi
    fi
}

case $COMMAND in
    build)
        check_android_prerequisites || exit 1
        build_apk
        ;;
        
    sign)
        if [[ -z "${1:-}" ]]; then
            log_error "请指定要签名的APK文件"
            exit 1
        fi
        sign_apk "$1"
        ;;
        
    release)
        check_android_prerequisites || exit 1
        do_release
        ;;
        
    clean)
        cd "$ANDROID_DIR"
        ./gradlew clean 2>&1
        rm -rf "$ANDROID_DIR/app/build/outputs/apk/" 2>/dev/null || true
        log_success "清理完成"
        ;;
        
    variants)
        cd "$ANDROID_DIR"
        ./gradlew tasks --all 2>/dev/null | grep -i assemble || \
            log_info "请先执行 ./gradlew tasks 查看所有任务"
        ;;
esac
