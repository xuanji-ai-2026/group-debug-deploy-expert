#!/bin/bash
# ============================================================
#  北极星AI - 代码同步脚本 (方案B: 彻底修复)
#  只同步必要的源码和配置，不同步target/编译产物
# ============================================================

LOCAL_DIR="D:/BeijiXing-AI"
REMOTE_USER="root"
REMOTE_HOST="43.160.237.122"
REMOTE_KEY="D:/Singapore.pem"
REMOTE_BASE="/opt/beijixing-ai"

echo "=========================================="
echo "  代码同步: 本地 → 远程服务器"
echo "  $(date)"
echo "=========================================="

echo ""
echo "[1] 同步根POM文件..."
scp -i "$REMOTE_KEY" -o StrictHostKeyChecking=no \
    "$LOCAL_DIR/backend/pom.xml" \
    "$REMOTE_USER@$REMOTE_HOST:$REMOTE_BASE/backend/pom.xml"

echo ""
echo "[2] 同步各微服务POM文件..."
for SVC in bx-gateway bx-user bx-tenant bx-content bx-lead bx-ai bx-risk \
           bx-message bx-storage bx-system bx-data bx-social bx-schedule bx-monitor bx-billing; do
    if [ -f "$LOCAL_DIR/backend/$SVC/pom.xml" ]; then
        scp -i "$REMOTE_KEY" -o StrictHostKeyChecking=no \
            "$LOCAL_DIR/backend/$SVC/pom.xml" \
            "$REMOTE_USER@$REMOTE_HOST:$REMOTE_BASE/backend/$SVC/pom.xml" && \
        echo "   [OK] $SVC/pom.xml"
    fi
done

echo ""
echo "[3] 同步配置模板..."
if [ -f "$LOCAL_DIR/deploy/application-mariadb-standard-template.yml" ]; then
    scp -i "$REMOTE_KEY" -o StrictHostKeyChecking=no \
        "$LOCAL_DIR/deploy/application-mariadb-standard-template.yml" \
        "$REMOTE_USER@$REMOTE_HOST:$REMOTE_BASE/"
fi

echo ""
echo "[4] 同步部署脚本..."
for SCRIPT in build-v6-final.sh deploy-all-v7.sh isolate-old-dirs.sh; do
    if [ -f "$LOCAL_DIR/deploy/$SCRIPT" ]; then
        scp -i "$REMOTE_KEY" -o StrictHostKeyChecking=no \
            "$LOCAL_DIR/deploy/$SCRIPT" \
            "$REMOTE_USER@$REMOTE_HOST:$REMOTE_BASE/deploy/" 2>/dev/null || \
        scp -i "$REMOTE_KEY" -o StrictHostKeyChecking=no \
            "$LOCAL_DIR/deploy/$SCRIPT" \
            "$REMOTE_USER@$REMOTE_HOST:$REMOTE_BASE/" && \
        echo "   [OK] $SCRIPT"
    fi
done

echo ""
echo "=========================================="
echo "  代码同步完成!"
echo "  下一步: 在远程服务器执行 Maven 打包"
echo "=========================================="