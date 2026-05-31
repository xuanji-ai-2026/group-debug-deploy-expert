#!/bin/bash
# ============================================================
#  清除所有旧Profile配置文件 + 验证resources目录
#  只保留 application.yml 和 bootstrap.yml
# ============================================================

echo "=========================================="
echo "  清除旧配置文件 + 验证"
echo "  $(date)"
echo "=========================================="

echo ""
echo "[1] 删除所有 application-dev.yml:"
find /opt/beijixing-ai/backend -name 'application-dev.yml' -type f -delete 2>/dev/null
echo "  已删除"

echo ""
echo "[2] 删除所有 application-prod.yml:"
find /opt/beijixing-ai/backend -name 'application-prod.yml' -type f -delete 2>/dev/null
echo "  已删除"

echo ""
echo "[3] 验证: 剩余的application-*.yml:"
find /opt/beijixing-ai/backend -name 'application-*.yml' -type f 2>/dev/null
if [ $? -eq 0 ]; then
    echo "  (无)"
fi

echo ""
echo "[4] 每个服务的resources目录 (应该只有application.yml和bootstrap.yml):"
for SVC in bx-gateway bx-user bx-tenant bx-content bx-lead bx-ai bx-risk \
           bx-message bx-storage bx-system bx-data bx-social bx-schedule bx-monitor bx-billing; do
    echo "--- $SVC ---"
    ls /opt/beijixing-ai/backend/$SVC/src/main/resources/*.yml 2>/dev/null || echo "(空)"
done

echo ""
echo "=========================================="
echo "  清理完成!"
echo "=========================================="