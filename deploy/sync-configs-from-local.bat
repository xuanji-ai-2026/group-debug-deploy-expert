#!/bin/bash
# ============================================================
#  从 D:\BeijiXing-AI 同步配置文件到远程服务器
#  只同步 resources/*.yml 配置文件（不同步target/编译产物）
# ============================================================

LOCAL_BASE="D:/BeijiXing-AI"
REMOTE_USER="root"
REMOTE_HOST="43.160.237.122"
REMOTE_KEY="D:/Singapore.pem"
REMOTE_BACKEND="/opt/beijixing-ai/backend"

echo "=========================================="
echo "  同步配置文件: 本地 → 远程服务器"
echo "  源路径: D:\\BeijiXing-AI (唯一正确路径)"
echo "  目标: $REMOTE_HOST:$REMOTE_BACKEND"
echo "  $(date)"
echo "=========================================="

SYNC_COUNT=0
FAIL_COUNT=0

# 同步每个服务的 application.yml
for SVC in bx-gateway bx-user bx-tenant bx-content bx-lead bx-ai bx-risk \
           bx-message bx-storage bx-system bx-data bx-social bx-schedule bx-monitor bx-billing; do
    
    LOCAL_YML="$LOCAL_BASE/backend/$SVC/src/main/resources/application.yml"
    
    if [ -f "$LOCAL_YML" ]; then
        scp -i "$REMOTE_KEY" -o StrictHostKeyChecking=no \
            "$LOCAL_YML" \
            "$REMOTE_USER@$REMOTE_HOST:$REMOTE_BACKEND/$SVC/src/main/resources/application.yml" 2>/dev/null
        
        if [ $? -eq 0 ]; then
            echo "[OK] $SVC/application.yml"
            SYNC_COUNT=$((SYNC_COUNT+1))
        else
            echo "[FAIL] $SVC/application.yml"
            FAIL_COUNT=$((FAIL_COUNT+1))
        fi
    fi
    
    # 特殊处理 bx-ai 的 bootstrap.yml
    if [ "$SVC" = "bx-ai" ]; then
        LOCAL_BOOTSTRAP="$LOCAL_BASE/backend/bx-ai/src/main/resources/bootstrap.yml"
        if [ -f "$LOCAL_BOOTSTRAP" ]; then
            scp -i "$REMOTE_KEY" -o StrictHostKeyChecking=no \
                "$LOCAL_BOOTSTRAP" \
                "$REMOTE_USER@$REMOTE_HOST:$REMOTE_BACKEND/bx-ai/src/main/resources/bootstrap.yml" 2>/dev/null
            
            if [ $? -eq 0 ]; then
                echo "[OK] $SVC/bootstrap.yml"
                SYNC_COUNT=$((SYNC_COUNT+1))
            fi
        fi
    fi
done

# 同步根POM文件
scp -i "$REMOTE_KEY" -o StrictHostKeyChecking=no \
    "$LOCAL_BASE/backend/pom.xml" \
    "$REMOTE_USER@$REMOTE_HOST:$REMOTE_BACKEND/pom.xml" 2>/dev/null && \
echo "[OK] 根pom.xml" || echo "[FAIL] 根pom.xml"

echo ""
echo "=========================================="
echo "  同步完成: $SYNC_COUNT 成功, $FAIL_COUNT 失败"
echo "=========================================="