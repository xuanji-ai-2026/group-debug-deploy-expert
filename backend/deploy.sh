#!/bin/bash
# ==============================================
# 北极星AI - 远程服务器部署脚本
# 用途: 一键部署到远程Linux服务器
# 使用方法: ./deploy.sh [环境] (dev|prod)
# ==============================================

set -e

APP_NAME="beijixing-app"
JAR_FILE="target/${APP_NAME}-1.0.0-SNAPSHOT.jar"
REMOTE_DIR="/opt/beijixing-ai"
LOG_DIR="/opt/beijixing-ai/logs"
BACKUP_DIR="/opt/beijixing-ai/backup"

echo "========================================"
echo "  北极星AI 远程部署脚本 v1.0"
echo "========================================"

# 检查环境参数
ENV=${1:-prod}
if [ "$ENV" != "dev" ] && [ "$ENV" != "prod" ]; then
    echo "错误: 环境参数必须是 dev 或 prod"
    exit 1
fi

echo "📦 部署环境: $ENV"

# 本地构建
echo ""
echo "🔨 步骤1/5: 本地Maven构建..."
mvn clean package -DskipTests -pl beijixing-app -am -q
if [ $? -ne 0 ]; then
    echo "❌ Maven构建失败!"
    exit 1
fi
echo "✅ Maven构建成功"

# 检查JAR文件
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ JAR文件不存在: $JAR_FILE"
    exit 1
fi

echo ""
echo "📊 JAR文件信息:"
ls -lh "$JAR_FILE"

# 远程部署（示例，需替换为实际的服务器地址）
echo ""
echo "🚀 步骤2/5: 准备远程部署..."
echo "⚠️  请手动执行以下步骤（或配置SSH密钥后自动化）:"
echo ""
cat <<EOF
# ====== 远程服务器操作指南 ======

# 1. 上传JAR文件到服务器
scp $JAR_FILE user@your-server:$REMOTE_DIR/

# 2. SSH登录服务器
ssh user@your-server

# 3. 创建目录结构
sudo mkdir -p $REMOTE_DIR/{logs,backup,config}

# 4. 备份旧版本（如果有）
if [ -f "$REMOTE_DIR/$APP_NAME.jar" ]; then
    sudo cp "$REMOTE_DIR/$APP_NAME.jar" "$BACKUP_DIR/$APP_NAME-$(date +%Y%m%d%H%M%S).jar"
fi

# 5. 部署新版本
sudo cp $REMOTE_DIR/${APP_NAME}-1.0.0-SNAPSHOT.jar $REMOTE_DIR/$APP_NAME.jar

# 6. 设置环境变量（根据实际情况修改）
export DB_HOST="your-db-host"
export DB_PORT=3306
export DB_NAME="beijixing_ai"
export DB_USER="root"
export DB_PASSWORD="your-password"

export REDIS_HOST="your-redis-host"
export REDIS_PORT=6379
export REDIS_PASSWORD="your-redis-password"

# 7. 启动应用（后台运行）
cd $REMOTE_DIR
nohup java -jar $APP_NAME.jar --spring.profiles.active=$ENV > $LOG_DIR/startup.log 2>&1 &

# 8. 检查启动状态
sleep 10
tail -f $LOG_DIR/startup.log | grep -E "(Started|ERROR|Failed)"

# 9. 验证服务是否启动成功
curl http://localhost:8080/actuator/health

EOF

echo ""
echo "✅ 部署准备完成！"
echo ""
echo "📝 后续步骤:"
echo "   1. 将JAR文件上传到远程服务器"
echo "   2. 配置数据库和Redis连接信息"
echo "   3. 执行启动命令"
echo ""
echo "🌐 应用访问地址: http://your-server:8080"
echo "📊 健康检查: http://your-server:8080/actuator/health"
