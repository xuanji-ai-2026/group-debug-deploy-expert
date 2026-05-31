#!/bin/bash
# 数据库表统计脚本

DB_PASSWORD='Beijixing@2024!'

echo "=========================================="
echo "  北极星AI - 数据库表结构调查报告"
echo "=========================================="
echo ""

databases=(
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

total_tables=0

for db in "${databases[@]}"; do
    count=$(mariadb -u root -p"$DB_PASSWORD" -e "USE $db; SHOW TABLES;" 2>/dev/null | grep -v "Tables_in" | wc -l)
    total_tables=$((total_tables + count))
    printf "%-20s %3d 张表\n" "$db" "$count"
done

echo ""
echo "=========================================="
printf "总计: %-17s %3d 张表\n" "" "$total_tables"
echo "=========================================="