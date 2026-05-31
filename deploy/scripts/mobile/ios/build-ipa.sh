#!/bin/bash
# ============================================================
#  北极星AI - iOS IPA 自动化构建与签名导出
#  基于: Xcode + xcodebuild + Fastlane + App Store Connect API
#  整合: SwiftUI 项目结构 (iOS 15+)
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"
source "$SCRIPT_DIR/../lib/env.sh"

usage() {
    cat << EOF
北极星AI iOS IPA 构建签名脚本

用法: $(basename $0) [命令] [选项]

命令:
    build           构建Archive (默认)
    export          导出IPA
    release         完整发布流程 (构建+导出+上传)
    upload          上传到App Store Connect / TestFlight
    clean           清理构建产物
    list-devices    查看已注册设备

选项:
    -s, --scheme SCHEME     Scheme选择 (BeijiXingAI, 默认自动检测)
    -c, --config CONFIG     配置选择 (Debug|Release, 默认Release)
    -t, --team TEAM         开发者Team ID
    --output DIR            输出目录
    --testflight            发布到TestFlight (非App Store)
    --no-upload             跳过上传步骤

示例:
    $(basename $0) build                          # Archive构建
    $(basename $0) export                         # 导出IPA
    $(basename $0) release                        # 完整发布流程
    $(basename $0) release --testflight           # 发布到TestFlight
    $(basename $0) upload -s BeijiXingAI          # 直接上传已有Archive

EOF
}

COMMAND="build"
SCHEME=""
CONFIGURATION="Release"
TEAM_ID=""
OUTPUT_DIR=""
TESTFLIGHT=false
NO_UPLOAD=false

while [[ $# -gt 0 ]]; do
    case $1 in
        build|export|release|upload|clean|list-devices)
            COMMAND=$1; shift ;;
        -h|--help) usage; exit 0 ;;
        -s|--scheme) SCHEME=$2; shift 2 ;;
        -c|--config) CONFIGURATION=$2; shift 2 ;;
        -t|--team) TEAM_ID=$2; shift 2 ;;
        --output) OUTPUT_DIR=$2; shift 2 ;;
        --testflight) TESTFLIGHT=true; shift ;;
        --no-upload) NO_UPLOAD=true; shift ;;
        *) log_error "未知参数: $1"; usage; exit 1 ;;
    esac
done

IOS_DIR=$(get_ios_dir)

check_ios_prerequisites() {
    local missing=()
    
    if ! command -v xcodebuild &>/dev/null; then missing+=("xcodebuild"); fi
    if ! command -v xcrun &>/dev/null; then missing+=("xcrun"); fi
    
    if (( ${#missing[@]} > 0 )); then
        log_error "缺少iOS构建依赖 (需要在macOS上运行):"
        for m in "${missing[@]}"; do log_error "  - $m"; done
        return 1
    fi
    
    XCODE_VERSION=$(xcodebuild -version 2>/dev/null | head -1 || echo "unknown")
    log_info "Xcode: ${XCODE_VERSION}"
    
    if [[ -z "$SCHEME" ]]; then
        SCHEME=$(xcodebuild -project "$IOS_DIR/BeijiXingAI.xcodeproj" -list 2>/dev/null | \
            grep -A20 "Schemes:" | tail -n +2 | head -1 | xargs || echo "BeijiXingAI")
    fi
    
    if [[ -z "$TEAM_ID" ]]; then
        TEAM_ID="${IOS_TEAM_ID:-}"
        if [[ -z "$TEAM_ID" ]]; then
            log_warn "未指定Team ID，使用Xcode默认设置"
        fi
    fi
    
    return 0
}

create_export_options() {
    local export_plist="$IOS_DIR/ExportOptions.plist"
    
    cat > "$export_plist" << EOFPLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" \
  "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>method</key>
    <string>$(if $TESTFLIGHT; then echo "development"; else echo "app-store"; fi)</string>
    <key>teamID</key>
    <string>${TEAM_ID:-YOUR_TEAM_ID}</string>
    <key>uploadBitcode</key>
    <false/>
    <key>uploadSymbols</key>
    <true/>
    <key>compileBitcode</key>
    <false/>
    <key>destination</key>
    <string>upload</string>
    <key>signingStyle</key>
    <string>automatic</string>
</dict>
</plist>
EOFPLIST
    
    echo "$export_plist"
}

do_archive() {
    local archive_path=${OUTPUT_DIR:-"$IOS_DIR/build/archives"}
    mkdir -p "$archive_path"
    
    local archive_name="${SCHEME}_$(ts)"
    local archive_file="$archive_path/${archive_name}.xcarchive"
    
    log_info "=========================================="
    log_info "  Archive构建: $SCHEME ($CONFIGURATION)"
    log_info "=========================================="
    
    xcodebuild archive \
        -project "$IOS_DIR/BeijiXingAI.xcodeproj" \
        -scheme "$SCHEME" \
        -configuration "$CONFIGURATION" \
        -archivePath "$archive_file" \
        -allowProvisioningUpdates \
        CODE_SIGNING_ALLOWED=YES \
        2>&1 | tee /tmp/xcode_archive.log | tail -50
    
    if [[ -d "$archive_file" ]]; then
        ARCHIVE_SIZE=$(du -sh "$archive_file" | awk '{print $1}')
        log_success "Archive成功: $archive_file (${ARCHIVE_SIZE})"
        echo "$archive_file"
    else
        log_error "Archive失败! 查看: /tmp/xcode_archive.log"
        tail -30 /tmp/xcode_archive.log
        return 1
    fi
}

do_export() {
    local archive_file=${1:-}
    
    if [[ -z "$archive_file" || ! -d "$archive_file" ]]; then
        log_error "请指定有效的Archive路径"
        return 1
    fi
    
    local export_dir=${OUTPUT_DIR:-"$IOS_DIR/build/ipa"}
    mkdir -p "$export_dir"
    
    local export_options
    export_options=$(create_export_options)
    
    log_info "导出IPA..."
    
    xcodebuild -exportArchive \
        -archivePath "$archive_file" \
        -exportPath "$export_dir" \
        -exportOptionsPlist "$export_options" \
        -allowProvisioningUpdates \
        2>&1 | tee /tmp/xcode_export.log | tail -30
    
    local ipa_path
    ipa_path=$(find "$export_dir" -name "*.ipa" -type f 2>/dev/null | head -1)
    
    if [[ -n "$ipa_path" && -f "$ipa_path" ]]; then
        IPA_SIZE=$(ls -lh "$ipa_path" | awk '{print $5}')
        log_success "IPA导出成功: $ipa_path (${IPA_SIZE})"
        
        local dsym_path
        dsym_path=$(find "$export_dir" -name "*.dSYM.zip" -type f 2>/dev/null | head -1)
        [[ -n "$dsym_path" ]] && log_info "dSYM符号表: $dsym_path"
        
        echo "$ipa_path"
    else
        log_error "IPA导出失败!"
        return 1
    fi
}

do_upload() {
    local ipa_file=$1
    
    if [[ ! -f "$ipa_file" ]]; then
        log_error "IPA文件不存在: $ipa_file"
        return 1
    fi
    
    log_info "上传到${TESTFLIGHT:+TestFlight}${TESTFLIGHT:-App Store}..."
    
    if command -v xcrun altool &>/dev/null; then
        xcrun altool --upload-app \
            -f "$ipa_file" \
            -t ios \
            -u "${APPLE_ID:-}" \
            -p "${APPLE_PASSWORD:-@keychain:AC_PASSWORD}" \
            2>&1 | tee /tmp/xcode_upload.log
    elif command -v notarytool &>/dev/null; then
        xcrun notarytool submit "$ipa_file" \
            --apple-id "${APPLE_ID:-}" \
            --password "${APPLE_PASSWORD:-@keychain:AC_PASSWORD}" \
            --team-id "${TEAM_ID:-}" \
            --wait 2>&1 | tee /tmp/xcode_upload.log
    else
        log_warn "上传工具不可用，请手动上传:"
        log_info "  Transporter.app 或 Application Loader"
        return 1
    fi
    
    if grep -q "No errors uploading" /tmp/xcode_upload.log 2>/dev/null || \
       grep -q "accepted" /tmp/xcode_upload.log 2>/dev/null; then
        log_success "上传成功!"
    else
        log_warn "上传可能失败，请检查日志"
    fi
}

do_release() {
    local timestamp=$(ts)
    local archive_dir="$(get_archive_dir)/releases/ios/${timestamp}"
    mkdir -p "$archive_dir"
    
    log_info "=========================================="
    log_info "  iOS完整发布流程"
    log_info "  归档目录: $archive_dir"
    log_info "=========================================="
    
    local archive_file
    archive_file=$(do_archive)
    
    if [[ -n "$archive_file" ]]; then
        local ipa_file
        ipa_file=$(do_export "$archive_file")
        
        if [[ -n "$ipa_file" && "$NO_UPLOAD" != "true" ]]; then
            do_upload "$ipa_file"
        fi
        
        cp -a "$ipa_file" "$archive_dir/" 2>/dev/null || true
        
        cat > "$archive_dir/release_manifest.txt" << EOF
北极星AI iOS 发布清单
时间戳: $(date '+%Y-%m-%d %H:%M:%S')
Scheme: ${SCHEME}
配置: ${CONFIGURATION}
IPA文件: $(basename "$ipa_file")
大小: $(ls -lh "$ipa_file" | awk '{print $5}')
SHA256: $(sha256sum "$ipa_file" | cut -d' ' -f1)
TestFlight: ${TESTFLIGHT:-否}
EOF
        
        log_success "发布完成! 归档位置: $archive_dir"
    fi
}

case $COMMAND in
    build)
        check_ios_prerequisites || exit 1
        do_archive
        ;;
        
    export)
        check_ios_prerequisites || exit 1
        local last_archive
        last_archive=$(ls -dt "$IOS_DIR/build/archives/"*.xcarchive 2>/dev/null | head -1)
        if [[ -z "$last_archive" ]]; then
            log_error "未找到Archive，请先执行 build 命令"
            exit 1
        fi
        do_export "$last_archive"
        ;;
        
    release)
        check_ios_prerequisites || exit 1
        do_release
        ;;
        
    upload)
        if [[ -z "${1:-}" ]]; then
            log_error "请指定要上传的IPA文件"
            exit 1
        fi
        do_upload "$1"
        ;;
        
    clean)
        rm -rf "$IOS_DIR/build/" 2>/dev/null || true
        rm -rf ~/Library/Developer/Xcode/DerivedData/BeijiXingAI-* 2>/dev/null || true
        log_success "清理完成"
        ;;
        
    list-devices)
        if command -v xcdevice &>/dev/null; then
            xcdevice list 2>/dev/null || true
        else
            log_info "查看已注册设备: https://developer.apple.com/account/resources/devices/list"
        fi
        ;;
esac
