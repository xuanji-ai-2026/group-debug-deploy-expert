#!/bin/bash
# 测试单个服务启动 - 验证参数格式

LOG_DIR="/opt/beijixing-ai/logs"
mkdir -p "$LOG_DIR"

echo "=== 测试启动 bx-content ==="

# 使用与老服务完全相同的参数格式（已验证可工作）
java -Xms64m -Xmx128m -XX:+UseG1GC \
    -Dspring.cloud.nacos.discovery.server-addr=localhost:8848 \
    -Dspring.cloud.nacos.config.server-addr=localhost:8848 \
    -Dspring.cloud.compatibility-verifier.enabled=false \
    -Dserver.address=0.0.0.0 \
    -Dserver.port=8083 \
    -Dspring.datasource.driver-class-name=org.mariadb.jdbc.Driver \
    -Dspring.datasource.username=root \
    -Dspring.datasource.password='Beijixing@2024!' \
    -Dspring.datasource.url=jdbc:mariadb://127.0.0.1:3306/bx_content?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai \
    -Dspring.redis.host=localhost \
    -Dspring.redis.port=6379 \
    -Dspring.redis.password='Redis@2026Secure!' \
    -jar /opt/beijixing-ai/backend/bx-content/target/bx-content-1.0.0-SNAPSHOT.jar \
    > "$LOG_DIR/bx-content-test.log" 2>&1 &

PID=$!
echo "启动PID: $PID"
sleep 5

if ps -p $PID > /dev/null; then
    echo "✅ 进程存活"
    echo "检查端口8083..."
    ss -tlnp | grep ':8083' && echo "✅ 端口监听正常" || echo "❌ 端口未监听"
    echo ""
    echo "最后20行日志:"
    tail -20 "$LOG_DIR/bx-content-test.log"
else
    echo "❌ 进程已退出"
    echo "错误日志:"
    tail -30 "$LOG_DIR/bx-content-test.log"
fi
