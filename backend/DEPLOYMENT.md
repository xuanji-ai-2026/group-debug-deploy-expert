# ==============================================
# 北极星AI - 远程服务器部署指南
# 版本: v1.0 | 更新时间: 2026-05-20
# 架构: 16个微服务 → 单一可执行JAR
# ==============================================

## 📋 部署前置条件

### 1. 服务器环境要求
```bash
# 操作系统: CentOS 7+ / Ubuntu 18.04+ / Debian 10+
# CPU: 2核以上
# 内存: 4GB以上（推荐8GB）
# 磁盘: 50GB以上可用空间
```

### 2. 必需软件
```bash
# Java 17 (必须)
java -version  # 应显示: openjdk version "17.x.x"

# MariaDB 10.5+ 或 MySQL 8.0+
mysql --version

# Redis 6.2+
redis-server --version
```

### 3. 网络端口
| 端口 | 用途 | 说明 |
|------|------|------|
| 8080 | 应用主端口 | HTTP API |
| 6379 | Redis | 缓存/分布式锁 |
| 3306 | MariaDB | 数据库 |
| 9999 | XXL-Job (可选) | 任务调度 |

---

## 🚀 快速部署步骤

### 步骤1: 上传JAR文件到服务器
```bash
# 本地执行（Windows PowerShell）
scp D:\BeijiXing-AI\backend\beijixing-app\target\beijixing-app-1.0.0-SNAPSHOT.jar user@your-server:/opt/beijixing-ai/

# 或者使用其他工具:
# - WinSCP / FileZilla (图形界面)
# - rsync (命令行增量同步)
```

### 步骤2: SSH登录服务器并配置环境
```bash
ssh user@your-server

# 创建目录结构
sudo mkdir -p /opt/beijixing-ai/{logs,backup,config}

# 备份旧版本（如果有）
if [ -f "/opt/beijixing-ai/beijixing-app.jar" ]; then
    sudo cp /opt/beijixing-ai/beijixing-app.jar \
         /opt/beijixing-ai/backup/beijixing-app-$(date +%Y%m%d%H%M%S).jar
fi

# 部署新版本
sudo cp beijixing-app-1.0.0-SNAPSHOT.jar /opt/beijixing-ai/beijixing-app.jar
cd /opt/beijixing-ai
```

### 步骤3: 配置数据库连接
```bash
# 创建环境变量配置文件
sudo tee /opt/beijixing-ai/config/env.sh > /dev/null << 'EOF'
#!/bin/bash
export DB_HOST="your-db-host-or-ip"
export DB_PORT=3306
export DB_NAME="beijixing_ai"
export DB_USER="root"
export DB_PASSWORD="your-database-password"

export REDIS_HOST="your-redis-host-or-ip"
export REDIS_PORT=6379
export REDIS_PASSWORD="your-redis-password"

# 可选配置
export JWT_SECRET="your-jwt-secret-key-minimum-32-chars"
export SMTP_HOST="smtp.qq.com"
export SMTP_PORT=465
export SMTP_USERNAME="your-email@qq.com"
export SMTP_PASSWORD="your-smtp-password"
EOF

# 设置权限
sudo chmod +x /opt/beijixing-ai/config/env.sh
source /opt/beijixing-ai/config/env.sh
```

### 步骤4: 启动应用
```bash
cd /opt/beijixing-ai

# 方式A: 前台运行（调试用）
source config/env.sh && java -jar beijixing-app.jar --spring.profiles.active=prod

# 方式B: 后台运行（生产推荐）
nohup bash -c 'source /opt/beijixing-ai/config/env.sh && java -jar beijixing-app.jar --spring.profiles.active=prod' > logs/application.log 2>&1 &

# 查看日志
tail -f logs/application.log
```

### 步骤5: 验证部署成功
```bash
# 检查进程是否运行
ps aux | grep beijixing-app

# 检查健康状态（等待30秒后）
curl http://localhost:8080/actuator/health

# 预期输出:
# {"status":"UP","components":{...}}

# 测试API访问
curl http://localhost:8080/api/v1/system/config/public
```

---

## 📊 已整合的16个微服务模块

| # | 模块名 | 功能 | 核心API |
|---|--------|------|---------|
| 1 | bx-common | 公共工具类 | - |
| 2 | bx-user | 用户认证/权限 | `/api/v1/auth/*` |
| 3 | bx-tenant | 多租户管理 | `/api/v1/tenant/*` |
| 4 | bx-lead | 线索管理 | `/api/v1/lead/*` |
| 5 | bx-ai | AI模型集成 | `/api/v1/ai/*` |
| 6 | bx-social | 社交平台对接 | `/api/v1/social/*` |
| 7 | bx-content | 内容管理 | `/api/v1/content/*` |
| 8 | bx-risk | 风控引擎 | `/api/v1/risk/*` |
| 9 | bx-billing | 计费系统 | `/api/v1/billing/*` |
| 10 | bx-message | 消息中心 | `/api/v1/message/*` |
| 11 | bx-storage | 文件存储 | `/api/v1/storage/*` |
| 12 | bx-monitor | 监控告警 | `/api/v1/monitor/*` |
| 13 | bx-schedule | 定时任务 | `/api/v1/schedule/*` |
| 14 | bx-system | 系统管理 | `/api/v1/system/*` |
| 15 | bx-gateway | API网关 | Gateway Filters |
| 16 | bx-task | 任务管理 | `/api/v1/task/*` |

---

## 🔧 常见问题排查

### Q1: 启动失败 - 数据库连接错误
```bash
# 错误信息: Failed to determine a suitable driver class
# 解决: 检查MariaDB JDBC驱动是否在JAR中
unzip -l beijixing-app.jar | grep mariadb

# 应看到: org/mariadb/jdbc/Driver.class
```

### Q2: 启动失败 - Redis连接超时
```bash
# 错误信息: Unable to connect to Redis server
# 解决:
# 1. 检查Redis是否启动
redis-cli ping  # 应返回 PONG

# 2. 检查防火墙是否开放6379端口
sudo firewall-cmd --list-ports

# 3. 检查Redis绑定地址（默认只监听127.0.0.1）
sudo vim /etc/redis.conf
# 修改 bind 127.0.0.1 -> bind 0.0.0.0
sudo systemctl restart redis
```

### Q3: Bean冲突警告
```bash
# 日志中可能出现以下WARN（正常现象）:
# WARN Skipping MapperFactoryBean with name 'xxxMapper' ... Bean already defined
# WARN mapper[xxx] is ignored, because it exists, maybe from xml file

# 这些是MyBatis多模块扫描的重复检测警告，不影响功能
# 如需消除，可调整mybatis-plus.mapper-locations配置
```

### Q4: 内存不足 OOM
```bash
# 调整JVM内存参数
java -Xms512m -Xmx2g -jar beijixing-app.jar ...

# 或创建启动脚本
cat > start.sh << 'EOF'
#!/bin/bash
JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC"
source /opt/beijixing-ai/config/env.sh
java $JAVA_OPTS -jar beijixing-app.jar --spring.profiles.active=prod
EOF
chmod +x start.sh
```

---

## 📈 生产环境优化建议

### 1. 使用 Systemd 管理服务
```bash
sudo tee /etc/systemd/system/beijixing-ai.service > /dev/null << 'EOF'
[Unit]
Description=BeijiXing AI Application
After=network.target mariadb.service redis.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/beijixing-ai
ExecStart=/usr/bin/java -Xms512m -Xmx2g -jar beijixing-app.jar --spring.profiles.active=prod
EnvironmentFile=/opt/beijixing-ai/config/env Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# 启动服务
sudo systemctl daemon-reload
sudo systemctl enable beijixing-ai
sudo systemctl start beijixing-ai
sudo systemctl status beijixing-ai
```

### 2. Nginx 反向代理配置
```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        
        # 文件上传大小限制
        client_max_body_size 100M;
        
        # WebSocket支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 300s;
        proxy_read_timeout 300s;
    }
}
```

### 3. 日志轮转配置
```bash
sudo tee /etc/logrotate.d/beijixing-ai > /dev/null << 'EOF'
/opt/beijixing-ai/logs/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    create 0644 root root
    postrotate
        systemctl reload beijixing-ai > /dev/null 2>&1 || true
    endscript
}
EOF
```

---

## 🎯 下一步行动项

### Phase 3 续 - 评论抓取API开发
当前已完成单体架构合并和基础验证，下一步：
1. ✅ 单体应用打包成功（245MB单一JAR）
2. ⏳ 远程服务器部署验证
3. ⏳ 实现抖音/小红书评论抓取API
4. ⏳ 编写核心类单元测试
5. ⏳ Postman功能测试验证

### 关键技术栈总结
- **框架**: Spring Boot 3.3.6 + MyBatis-Plus 3.5.5
- **数据库**: MariaDB (H2用于开发测试)
- **缓存**: Redis + Redisson (分布式锁)
- **消息队列**: RabbitMQ (可选)
- **任务调度**: XXL-Job (可选)
- **监控**: Actuator + Prometheus
- **文档**: Knife4j/Swagger (生产关闭)

---

## 📞 技术支持

如遇问题，请检查：
1. 日志文件: `/opt/beijixing-ai/logs/application.log`
2. 健康检查: `http://your-server:8080/actuator/health`
3. 系统资源: `top` / `free -h` / `df -h`

**部署完成标志**: 看到 `北极星AI 极简单体应用启动成功！` 日志输出
