#!/bin/bash
# 详细表结构导出脚本

DB_PASSWORD='Beijixing@2024!'
OUTPUT_FILE="/tmp/db-tables-detail.txt"

echo "==========================================" > "$OUTPUT_FILE"
echo "  北极星AI - 数据库详细表结构报告" >> "$OUTPUT_FILE"
echo "  生成时间: $(date '+%Y-%m-%d %H:%M:%S')" >> "$OUTPUT_FILE"
echo "==========================================" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

databases_with_tables=(
    beijixing_user
    bx_ai
    bx_lead
    bx_system
    bx_user
)

for db in "${databases_with_tables[@]}"; do
    echo "【数据库: $db】" >> "$OUTPUT_FILE"
    echo "----------------------------------------" >> "$OUTPUT_FILE"
    
    tables=$(mariadb -u root -p"$DB_PASSWORD" -e "USE $db; SHOW TABLES;" 2>/dev/null | grep -v "Tables_in")
    
    for table in $tables; do
        echo "  表名: $table" >> "$OUTPUT_FILE"
        columns=$(mariadb -u root -p"$DB_PASSWORD" -e "USE $db; DESCRIBE $table;" 2>/dev/null | awk '{print "    " $1 " (" $2 ")"}' | grep -v Field)
        echo "$columns" >> "$OUTPUT_FILE"
        row_count=$(mariadb -u root -p"$DB_PASSWORD" -e "USE $db; SELECT COUNT(*) as count FROM $table;" 2>/dev/null | grep -v count)
        echo "    行数: $row_count" >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
    done
    
    echo "" >> "$OUTPUT_FILE"
done

cat "$OUTPUT_FILE"