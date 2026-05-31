# JVM性能调优与监控告警配置方案

## 📊 系统资源现状

| 资源 | 总量 | 已用 | 可用 | 使用率 |
|------|------|------|------|--------|
| 内存 | 7.5GB | 1.3GB | 6.2GB | 17% |
| CPU | 2核 | - | - | 低负载 |
| 磁盘 | 80GB | 26GB | 55GB | 32% |
| Java | OpenJDK 17.0.17 | - | - | ✅ |

**结论**: 系统资源充裕，可分配充足内存给JVM以提升性能。

---

## 🎯 JVM参数优化方案

### 方案A: 生产推荐配置（适用于QPS < 100）

```bash
java \
  # ===== 堆内存设置 =====
  -Xms512m \                    # 初始堆内存512MB
  -Xmx1024m \                   # 最大堆内存1024MB（系统内存的14%）
  
  # ===== GC策略（G1垃圾收集器）=====
  -XX:+UseG1GC \                # 使用G1GC（JDK 9+默认，适合低延迟）
  -XX:MaxGCPauseMillis=200 \    # 最大GC停顿时间200ms
  -XX:G1HeapRegionSize=16m \    # G1区域大小16MB
  -XX:G1ReservePercent=15 \     # 预留15%空间避免晋升失败
  
  # ===== GC日志（用于问题排查）=====
  -Xlog:gc*:file=/opt/beijixing-ai/logs/gc.log:time,uptime,level,tags:filecount=5,filesize=20m \
  
  # ===== JVM监控（JMX）=====
  -Dcom.sun.management.jmxremote \                    # 启用JMX
  -Dcom.sun.management.jmxremote.port=9999 \          # JMX端口
  -Dcom.sun.management.jmxremote.authenticate=false \ # 开发环境关闭认证
  -Dcom.sun.management.jmxremote.ssl=false \          # 关闭SSL
  -Djava.rmi.server.hostname=43.160.237.122 \         # JMX绑定地址
  
  # ===== 性能优化 =====
  -XX:+HeapDumpOnOutOfMemoryError \                    # OOM时自动转储
  -XX:HeapDumpPath=/opt/beijixing-ai/logs/heapdump.hprof \  # 转储文件路径
  -XX:+UseStringDeduplication \                        # 字符串去重（节省内存）
  -XX:InitiatingHeapOccupancyPercent=45 \              # 并发GC触发阈值
  
  # ===== 应用启动 =====
  -jar beijixing-app.jar \
  --spring.config.location=classpath:/application.yml,/opt/beijixing-ai/config/application.yml
```

### 方案B: 高性能配置（适用于QPS 100-500）

```bash
java \
  -Xms1024m \                  # 初始堆内存1GB
  -Xmx2048m \                  # 最大堆内存2GB（27%系统内存）
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \   # 更严格的GC停顿控制
  -XX:G1HeapRegionSize=16m \
  -XX:G1ReservePercent=20 \
  -XX:ConcGCThreads=2 \        # 并发GC线程数（=CPU核心数）
  -XX:ParallelGCThreads=2 \    # 并行GC线程数
  -Xlog:gc*:file=/opt/beijixing-ai/logs/gc.log:time,uptime,level,tags:filecount=5,filesize=20m \
  -Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9999 \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.ssl=false \
  -Djava.rmi.server.hostname=43.160.237.122 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/opt/beijixing-ai/logs/heapdump.hprof \
  -XX:+UseStringDeduplication \
  -XX:InitiatingHeapOccupancyPercent=40 \
  -jar beijixing-app.jar
```

### 参数说明

| 参数 | 推荐值 | 说明 |
|------|--------|------|
| `-Xms` | 512m-1024m | 初始堆大小（避免运行时扩容） |
| `-Xmx` | 1024m-2048m | 最大堆大小（≤系统内存的50%） |
| `MaxGCPauseMillis` | 100-200ms | GC最大停顿时间（影响响应时间） |
| `G1ReservePercent` | 15-20% | 预留空间防止To Space溢出 |
| `ConcGCThreads` | 1-2 | 并发GC线程（建议=CPU核数） |

---

## 📈 Prometheus + Grafana 监控配置

### 1️⃣ 安装Prometheus

**创建配置文件**: `/opt/beijixing-ai/monitoring/prometheus.yml`

```yaml
global:
  scrape_interval: 15s       # 抓取间隔
  evaluation_interval: 15s   # 规则评估间隔

scrape_configs:
  # Spring Boot应用监控
  - job_name: 'beijixing-ai'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
        labels:
          instance: 'production-server'
          environment: 'production'
    
  # Prometheus自身监控
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

# 告警规则文件
rule_files:
  - '/opt/beijixing-ai/monitoring/alert_rules.yml'
```

**启动Prometheus (Docker方式)**:
```bash
docker run -d \
  --name prometheus \
  -p 9090:9090 \
  -v /opt/beijixing-ai/monitoring/prometheus.yml:/etc/prometheus/prometheus.yml \
  -v /opt/beijixing-ai/monitoring/alert_rules.yml:/etc/prometheus/alert_rules.yml \
  prom/prometheus:latest
```

### 2️⃣ 配置告警规则

**创建文件**: `/opt/beijixing-ai/monitoring/alert_rules.yml`

```yaml
groups:
  - name: beijixing-ai-alerts
    interval: 30s
    rules:
      # === 应用级别告警 ===
      
      - alert: ApplicationDown
        expr: up{job="beijixing-ai"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "北极星AI应用宕机"
          description: "实例 {{ $labels.instance }} 已停止响应超过1分钟"
      
      - alert: HighResponseTime
        expr: http_server_requests_seconds_max{job="beijixing-ai"} > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "API响应时间过长"
          description: "接口 {{ $labels.uri }} 平均响应时间超过2秒（当前: {{ $value }}s）"
      
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "错误率过高"
          description: "5xx错误率超过5%（当前: {{ $value | humanizePercentage }}）"

      # === JVM级别告警 ===
      
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "JVM堆内存使用率过高"
          description: "堆内存使用率达到 {{ $value | humanizePercentage }}，超过85%阈值"
      
      - alert: FrequentGC
        expr: rate(jvm_gc_pause_seconds_sum[10m]) > 0.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "GC过于频繁"
          description: "GC每秒停顿时间超过500ms，可能存在内存泄漏"
      
      - alert: FullGCWarning
        expr: increase(jvm_gc_collection_seconds_count{gc="G1 Old Generation"}[1h]) > 3
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "Full GC频繁发生"
          description: "1小时内发生 {{ $value }} 次Full GC，需检查是否存在大对象或内存泄漏"

      # === 系统级别告警 ===
      
      - alert: HighCPUUsage
        expr: 100 - (avg by(instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "CPU使用率过高"
          description: "CPU使用率达到 {{ $value }}%，持续超过5分钟"
      
      - alert: DiskSpaceWarning
        expr: (node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"}) * 100 < 20
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "磁盘空间不足"
          description: "根分区剩余空间不足20%（当前: {{ $value }}%）"
      
      - alert: DiskSpaceCritical
        expr: (node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"}) * 100 < 10
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "磁盘空间严重不足"
          description: "根分区剩余空间不足10%（当前: {{ $value }}%），请立即清理！"
```

### 3️⃣ 安装Grafana

**启动Grafana (Docker)**:
```bash
docker run -d \
  --name grafana \
  -p 3000:3000 \
  -v grafana-data:/var/lib/grafana \
  grafana/grafana:latest
```

**访问地址**: http://43.160.237.122:3000  
**默认账号**: admin / admin

### 4️⃣ 导入监控仪表盘

**推荐仪表盘ID (Grafana.com)**:

| 用途 | Dashboard ID | 说明 |
|------|-------------|------|
| JVM监控 | 4701 | JVM Detailed Memory/Pool/GC Metrics |
| Spring Boot | 12900 | Spring Boot Statistics |
| 系统监控 | 8919 | Node Exporter Full |

**导入步骤**:
1. 登录Grafana → Dashboards → Import
2. 输入Dashboard ID → Load
3. 选择Prometheus数据源 → Import

---

## 🔧 快速部署脚本

### 一键安装监控组件

```bash
#!/bin/bash
# 文件: /opt/beijixing-ai/monitoring/setup-monitoring.sh

set -e

echo "🚀 开始部署监控系统..."

# 创建目录
mkdir -p /opt/beijixing-ai/monitoring

# 创建Prometheus配置
cat > /opt/beijixing-ai/monitoring/prometheus.yml << 'EOF'
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

rule_files:
  - '/etc/prometheus/alert_rules.yml'
EOF

# 创建告警规则
cat > /opt/beijixing-ai/monitoring/alert_rules.yml << 'EOF'
groups:
  - name: beijixing-ai-alerts
    rules:
      - alert: ApplicationDown
        expr: up{job="beijixing-ai"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "应用宕机"
          description: "实例已停止响应"
EOF

# 启动Prometheus
echo "📊 启动Prometheus..."
docker run -d \
  --name prometheus \
  -p 9090:9090 \
  --add-host=host.docker.internal:host-gateway \
  -v /opt/beijixing-ai/monitoring/prometheus.yml:/etc/prometheus/prometheus.yml \
  -v /opt/beijixing-ai/monitoring/alert_rules.yml:/etc/prometheus/alert_rules.yml \
  prom/prometheus:latest

# 启动Grafana
echo "📈 启动Grafana..."
docker run -d \
  --name grafana \
  -p 3000:3000 \
  grafana/grafana:latest

echo "✅ 监控系统部署完成！"
echo ""
echo "访问地址:"
echo "  - Prometheus: http://43.160.237.122:9090"
echo "  - Grafana: http://43.160.237.122:3000 (admin/admin)"
```

---

## 🎯 QPS与JVM参数对应关系表

| QPS范围 | 推荐配置 | 堆内存 | GC策略 | 适用场景 |
|---------|----------|--------|--------|----------|
| 0-50 | 方案A | 512m-1024m | G1GC (pause=200ms) | 开发/测试环境 |
| 50-150 | 方案B | 1024m-2048m | G1GC (pause=100ms) | 小规模生产 |
| 150-500 | 定制 | 2048m-4096m | G1GC (调优) | 中等规模生产 |
| 500+ | 架构调整 | 水平扩展 | - | 大规模集群 |

**当前建议**: 根据服务器资源（7.5GB内存），初始采用**方案A**，根据实际QPS数据逐步调整。

---

## 📋 Spring Boot Actuator配置检查

确保application.yml中已启用：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics,env,beans
  endpoint:
    health:
      show-details: always
    prometheus:
      enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
    export:
      prometheus:
        enabled: true
```

---

## ⚡ 快速验证命令

### 检查JVM参数是否生效
```bash
# 获取PID
PID=$(ps aux | grep beijixing-app | grep -v grep | awk '{print $2}')

# 查看JVM参数
jinfo -flags $PID

# 查看堆内存使用
jstat -gc $PID 1s 5

# 实时监控（需要JMX开启）
jvisualvm  # GUI工具连接 localhost:9999
```

### 测试监控端点
```bash
curl -s http://localhost:8080/actuator/prometheus | grep jvm_
curl -s http://localhost:8080/actuator/health | jq .
```

---

## 📞 实施步骤清单

- [ ] **Step 1**: 备份当前启动脚本
- [ ] **Step 2**: 更新JVM参数（选择方案A或B）
- [ ] **Step 3**: 重启应用并验证参数生效
- [ ] **Step 4**: 安装Prometheus + Grafana
- [ ] **Step 5**: 导入监控仪表盘
- [ ] **Step 6**: 配置告警通知渠道（邮件/钉钉/企业微信）
- [ ] **Step 7**: 运行Postman测试收集基线数据
- [ ] **Step 8**: 监控观察24-48小时，根据数据微调

---

## 🎓 参考文档

- [G1GC官方文档](https://docs.oracle.com/en/java/javase-17/gctuning/garbage-first-garbage-collector-tuning.html)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Prometheus最佳实践](https://prometheus.io/docs/practices/)
- [Grafana Dashboard市场](https://grafana.com/grafana/dashboards)

---

**文档版本**: v1.0  
**最后更新**: 2026-05-20  
**适用环境**: 生产服务器 (43.160.237.122)
