#!/bin/bash
# ============================================================
# 北极星AI - JVM优化与监控部署脚本
# 版本: v1.0
# 日期: 2026-05-20
# 用途: 一键配置JVM参数 + Prometheus + Grafana监控
# ============================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}  北极星AI - JVM优化与监控部署工具${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# 配置变量
APP_NAME="beijixing-app"
JAR_FILE="/opt/beijixing-ai/backend/${APP_NAME}.jar"
LOG_DIR="/opt/beijixing-ai/logs"
CONFIG_DIR="/opt/beijixing-ai/config"
MONITOR_DIR="/opt/beijixing-ai/monitoring"

# JVM参数（方案A: 生产推荐）
JAVA_OPTS="-Xms512m -Xmx1024m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:G1HeapRegionSize=16m \
  -XX:G1ReservePercent=15 \
  -Xlog:gc*:file=${LOG_DIR}/gc.log:time,uptime,level,tags:filecount=5,filesize=20m \
  -Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9999 \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.ssl=false \
  -Djava.rmi.server.hostname=43.160.237.122 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=${LOG_DIR}/heapdump.hprof \
  -XX:+UseStringDeduplication \
  -XX:InitiatingHeapOccupancyPercent=45"

# 函数定义
check_root() {
    if [ "$EUID" -ne 0 ]; then
        echo -e "${RED}❌ 请使用root用户执行此脚本${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ 权限检查通过${NC}"
}

check_docker() {
    if ! command -v docker &> /dev/null; then
        echo -e "${YELLOW}⚠️  Docker未安装，正在安装...${NC}"
        curl -fsSL https://get.docker.com | sh
        systemctl enable docker && systemctl start docker
    fi
    echo -e "${GREEN}✅ Docker已就绪${NC}"
}

create_directories() {
    echo -e "${BLUE}📁 创建必要目录...${NC}"
    mkdir -p ${LOG_DIR} ${CONFIG_DIR} ${MONITOR_DIR}
    echo -e "${GREEN}✅ 目录创建完成${NC}"
}

setup_jvm_config() {
    echo -e "${BLUE}⚙️  配置JVM参数...${NC}"
    
    # 创建启动脚本
    cat > /opt/beijixing-ai/start-app.sh << EOF
#!/bin/bash
export JAVA_OPTS="${JAVA_OPTS}"

nohup java \${JAVA_OPTS} \\
     -jar ${JAR_FILE} \\
     --spring.config.location=classpath:/application.yml,${CONFIG_DIR}/application.yml \\
     > ${LOG_DIR}/app.log 2>&1 &

echo \$! > ${LOG_DIR}/app.pid
echo "✅ 应用启动中... PID: \$(cat ${LOG_DIR}/app.pid)"
EOF
    
    chmod +x /opt/beijixing-ai/start-app.sh
    echo -e "${GREEN}✅ JVM配置完成${NC}"
    echo ""
    echo -e "${YELLOW}📋 JVM参数摘要:${NC}"
    echo "   堆内存: 512MB - 1024MB"
    echo "   GC策略: G1GC (MaxPause=200ms)"
    echo "   JMX端口: 9999"
    echo "   GC日志: ${LOG_DIR}/gc.log"
}

setup_prometheus() {
    echo -e "${BLUE}📊 配置Prometheus...${NC}"
    
    cat > ${MONITOR_DIR}/prometheus.yml << 'PROMEOF'
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'beijixing-ai'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']
        labels:
          instance: 'production-server'
          environment: 'production'

rule_files:
  - '/etc/prometheus/alert_rules.yml'
PROMEOF

    # 告警规则
    cat > ${MONITOR_DIR}/alert_rules.yml << 'ALERTEOF'
groups:
  - name: beijixing-ai-alerts
    interval: 30s
    rules:
      - alert: ApplicationDown
        expr: up{job="beijixing-ai"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "北极星AI应用宕机"
          description: "实例 {{ $labels.instance }} 已停止响应超过1分钟"
      
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "JVM堆内存使用率过高"
          description: "堆内存使用率达到 {{ $value | humanizePercentage }}"
      
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "API错误率过高"
          description: "5xx错误率超过5%"
      
      - alert: DiskSpaceWarning
        expr: (node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"}) * 100 < 20
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "磁盘空间不足"
          description: "根分区剩余空间不足20%"
ALERTEOF

    echo -e "${GREEN}✅ Prometheus配置完成${NC}"
}

deploy_monitoring() {
    echo -e "${BLUE}🚀 部署监控系统...${NC}"
    
    # 检查并移除旧容器
    docker stop prometheus grafana 2>/dev/null || true
    docker rm prometheus grafana 2>/dev/null || true
    
    # 启动Prometheus
    docker run -d \
      --name prometheus \
      -p 9090:9090 \
      --add-host=host.docker.internal:host-gateway \
      -v ${MONITOR_DIR}/prometheus.yml:/etc/prometheus/prometheus.yml \
      -v ${MONITOR_DIR}/alert_rules.yml:/etc/prometheus/alert_rules.yml \
      prom/prometheus:latest
    
    # 启动Grafana
    docker run -d \
      --name grafana \
      -p 3000:3000 \
      grafana/grafana:latest
    
    echo -e "${GREEN}✅ 监控系统部署完成${NC}"
}

verify_deployment() {
    echo -e "${BLUE}🔍 验证部署状态...${NC}"
    sleep 5
    
    # 检查容器状态
    if docker ps | grep -q prometheus; then
        echo -e "${GREEN}✅ Prometheus运行正常${NC}"
    else
        echo -e "${RED}❌ Prometheus启动失败${NC}"
    fi
    
    if docker ps | grep -q grafana; then
        echo -e "${GREEN}✅ Grafana运行正常${NC}"
    else
        echo -e "${RED}❌ Grafana启动失败${NC}"
    fi
    
    echo ""
    echo -e "${BLUE}============================================${NC}"
    echo -e "${GREEN}🎉 部署完成！访问地址:${NC}"
    echo -e "${BLUE}============================================${NC}"
    echo ""
    echo -e "📊 Prometheus:  ${YELLOW}http://43.160.237.122:9090${NC}"
    echo -e "📈 Grafana:     ${YELLOW}http://43.160.237.122:3000${NC}"
    echo -e "🔧 JMX监控:     ${YELLOW}localhost:9999${NC}"
    echo ""
    echo -e "${BLUE}Grafana默认账号: admin / admin${NC}"
    echo ""
    echo -e "${YELLOW}推荐导入的仪表盘:${NC}"
    echo "  • JVM监控: ID 4701"
    echo "  • Spring Boot: ID 12900"
    echo "  • 系统监控: ID 8919"
    echo ""
    echo -e "${BLUE}下一步操作:${NC}"
    echo "  1. 启动应用: /opt/beijixing-ai/start-app.sh"
    echo "  2. 导入Grafana仪表盘"
    echo "  3. 执行Postman测试收集基线数据"
    echo "  4. 观察24-48小时后微调参数"
}

# 主流程
main() {
    check_root
    check_docker
    create_directories
    setup_jvm_config
    setup_prometheus
    deploy_monitoring
    verify_deployment
}

# 执行主函数
main "$@"
