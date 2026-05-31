#!/bin/bash
# ============================================================
# 北极星AI - 应用启动脚本（生产优化版 v2.0）
# 版本: v2.0
# 日期: 2026-05-21
# JVM参数方案A: 适用于QPS < 100的场景
# 包含完整的环境变量配置
# ============================================================

set -e

# 配置变量
APP_NAME="beijixing-app"
JAR_FILE="/opt/beijixing-ai/backend/beijixing-app/beijixing-app-1.0.0-SNAPSHOT.jar"
LOG_DIR="/opt/beijixing-ai/logs/"
CONFIG_DIR="/opt/beijixing-ai/config"

# ====== 环境变量配置 ======
export DB_HOST=localhost
export DB_PORT=3306
export DB_USER=root
export DB_PASSWORD='Beijixing@2024!'
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD='Redis@2026Secure!'

# JVM参数（方案A: 生产推荐 - 基于服务器7.5GB内存/2核CPU优化）
JAVA_OPTS="-Xms512m -Xmx1024m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:G1HeapRegionSize=16m \
  -XX:G1ReservePercent=15 \
  -Xlog:gc*:${LOG_DIR}/gc.log:time,uptime,level,tags:filecount=5,filesize=20m \
  -Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9999 \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.ssl=false \
  -Djava.rmi.server.hostname=43.160.237.122 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=${LOG_DIR}/heapdump.hprof \
  -XX:+UseStringDeduplication \
  -XX:InitiatingHeapOccupancyPercent=45"

# 创建日志目录
mkdir -p ${LOG_DIR}

# 检查是否已有实例运行
if [ -f "${LOG_DIR}/app.pid" ]; then
    PID=$(cat ${LOG_DIR}/app.pid)
    if ps -p ${PID} > /dev/null 2>&1; then
        echo "⚠️  应用已在运行 (PID: ${PID})"
        echo "正在停止旧进程..."
        kill ${PID}
        sleep 5
        if ps -p ${PID} > /dev/null 2>&1; then
            echo "强制停止..."
            kill -9 ${PID}
            sleep 2
        fi
    fi
fi

echo "🚀 启动北极星AI应用..."
echo "📋 环境配置:"
echo "   数据库: ${DB_HOST}:${DB_PORT}"
echo "   Redis: ${REDIS_HOST}:${REDIS_PORT}"
echo ""
echo "📋 JVM参数:"
echo "   堆内存: 512MB - 1024MB"
echo "   GC策略: G1GC (MaxPause=200ms)"
echo "   JMX端口: 9999"
echo "   GC日志: ${LOG_DIR}/gc.log"
echo ""

# 启动应用（使用JVM参数强制覆盖数据源配置，确保使用MariaDB而非H2）
# 通过Spring Boot属性指定Mapper扫描
nohup java ${JAVA_OPTS} \
     -Dspring.datasource.driver-class-name=org.mariadb.jdbc.Driver \
     -Dspring.datasource.url="jdbc:mariadb://localhost:3306/beijixing_ai?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai" \
     -Dspring.datasource.username=root \
     -Dspring.datasource.password=Beijixing@2024! \
     -Dspring.data.redis.host=localhost \
     -Dspring.data.redis.port=6379 \
     -Dspring.data.redis.password=Redis@2026Secure! \
     -Dai.service.url=http://localhost:8080 \
     -Dai.service.api-key=bx-ai-local-dev-key \
     -Dmybatis-plus.mapper-locations=classpath*:/mapper/**/*.xml \
     -Dmybatis-plus.type-aliases-package=com.beijixing.entity \
     -jar ${JAR_FILE} \
     --spring.config.additional-location=${CONFIG_DIR}/application.yml \
     --spring.profiles.active=prod \
     --spring.main.allow-bean-definition-overriding=true \
     > ${LOG_DIR}/app.log 2>&1 &

echo $! > ${LOG_DIR}/app.pid
echo "✅ 应用启动中... PID: $(cat ${LOG_DIR}/app.pid)"
echo "📊 日志文件: ${LOG_DIR}/app.log"
echo "💡 查看日志: tail -f ${LOG_DIR}/app.log"
echo "💡 健康检查: curl http://localhost:8080/actuator/health"
