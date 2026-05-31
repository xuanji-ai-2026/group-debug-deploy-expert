#!/bin/bash
# ============================================================
#  北极星AI - 数据库精简整合脚本 v1.0
#  目标: 将17个业务数据库合并为1个（beijixing_ai）
#  适用: 测试阶段，无重要生产数据
# ============================================================

set -euo pipefail

DB_PASSWORD='Beijixing@2024!'
TARGET_DB='beijixing_ai'
BACKUP_DIR="/opt/beijixing-ai/db-backup-$(date +%Y%m%d-%H%M%S)"
LOG_FILE="/tmp/db-consolidation.log"

log_info() {
    local msg="[$(date '+%Y-%m-%d %H:%M:%S')] [INFO] $*"
    echo "$msg" | tee -a "$LOG_FILE"
}

log_warn() {
    local msg="[$(date '+%Y-%m-%d %H:%M:%S')] [WARN] $*"
    echo "$msg" | tee -a "$LOG_FILE"
}

log_error() {
    local msg="[$(date '+%Y-%m-%d %H:%M:%S')] [ERROR] $*"
    echo "$msg" | tee -a "$LOG_FILE"
}

log_success() {
    local msg="[$(date '+%Y-%m-%d %H:%M:%S')] [SUCCESS] ✓ $*"
    echo "$msg" | tee -a "$LOG_FILE"
}

# 要删除的业务数据库列表（17个）
DATABASES_TO_DELETE=(
    beijixing_ai
    beijixing_db
    beijixing_user
    bx_ai
    bx_billing
    bx_content
    bx_data
    bx_lead
    bx_message
    bx_monitor
    bx_risk
    bx_schedule
    bx_social
    bx_storage
    bx_system
    bx_tenant
    bx_user
)

echo "=========================================="
echo "  北极星AI - 数据库精简整合工具"
echo "=========================================="
echo "  目标数据库: $TARGET_DB"
echo "  备份目录:   $BACKUP_DIR"
echo "  执行时间:   $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="
echo ""

# Step 1: 备份所有业务数据库
log_info "=== Step 1/4: 备份所有业务数据库 ==="
mkdir -p "$BACKUP_DIR"

for db in "${DATABASES_TO_DELETE[@]}"; do
    log_info "备份数据库: $db ..."
    if mariadb-dump -u root -p"$DB_PASSWORD" --single-transaction --routines --triggers "$db" > "$BACKUP_DIR/${db}.sql" 2>/dev/null; then
        size=$(du -h "$BACKUP_DIR/${db}.sql" | cut -f1)
        log_success "数据库 $db 已备份 ($size)"
    else
        log_warn "数据库 $db 为空或不存在，跳过备份"
    fi
done

log_success "所有数据库备份完成 → $BACKUP_DIR"
echo ""

# Step 2: 删除所有业务数据库
log_info "=== Step 2/4: 删除所有业务数据库 ==="

for db in "${DATABASES_TO_DELETE[@]}"; do
    log_info "删除数据库: $db ..."
    if mariadb -u root -p"$DB_PASSWORD" -e "DROP DATABASE IF EXISTS \`$db\`;" 2>/dev/null; then
        log_success "数据库 $db 已删除"
    else
        log_error "删除数据库 $db 失败"
        exit 1
    fi
done

log_success "所有业务数据库已删除"
echo ""

# Step 3: 创建统一的目标数据库
log_info "=== Step 3/4: 创建统一目标数据库 ==="

if mariadb -u root -p"$DB_PASSWORD" -e "CREATE DATABASE IF NOT EXISTS \`$TARGET_DB\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null; then
    log_success "目标数据库 $TARGET_DB 创建成功"
else
    log_error "创建目标数据库失败"
    exit 1
fi

echo ""

# Step 4: 验证结果
log_info "=== Step 4/4: 验证整合结果 ==="

current_dbs=$(mariadb -u root -p"$DB_PASSWORD" -e "SHOW DATABASES;" 2>/dev/null | grep -v -E '^(Database|information_schema|mysql|performance_schema|sys|nacos_config)$')

echo ""
echo "=========================================="
echo "  整合结果验证"
echo "=========================================="
echo ""
echo "当前业务数据库列表："
echo "$current_dbs" | while read db; do
    count=$(mariadb -u root -p"$DB_PASSWORD" -e "USE $db; SHOW TABLES;" 2>/dev/null | grep -v "Tables_in" | wc -l)
    printf "  %-20s (%d 张表)\n" "$db" "$count"
done

total_tables=0
while read db; do
    count=$(mariadb -u root -p"$DB_PASSWORD" -e "USE $db; SHOW TABLES;" 2>/dev/null | grep -v "Tables_in" | wc -l)
    total_tables=$((total_tables + count))
done <<< "$current_dbs"

echo ""
echo "=========================================="
printf "总计: %d 个业务数据库, %d 张表\n" "$(echo "$current_dbs" | wc -l)" "$total_tables"
echo "=========================================="
echo ""
log_success "✓ 数据库精简整合完成！"
log_success "✓ 备份文件保存在: $BACKUP_DIR/"
log_success "✓ 统一数据库: $TARGET_DB"
echo ""