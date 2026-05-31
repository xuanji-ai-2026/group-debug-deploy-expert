# ==============================================
# 北极星AI - 远程服务器快速部署指南 v2.0
# 版本: 2026-05-20 | 适用环境: Linux (CentOS 7+/Ubuntu 18.04+)
# 前置条件: JAR包已手动上传至 /opt/beijixing-ai/
# ==============================================

## 📋 前置检查清单

### ✅ 必须已完成的步骤
```bash
# 1. JAR包已上传（您已完成）
ls -lh /opt/beijixing-ai/beijixing-app.jar
# 应显示: -rwxr-xr-x 1 root root 245M ... beijixing-app.jar

# 2. Java 17 已安装
java -version
# 应显示: openjdk version "17.x.x"

# 3. MariaDB 已安装并运行
systemctl status mariadb
mysql -u root -p -e "SELECT VERSION();"

# 4. Redis 已安装并运行
redis-cli ping
# 应返回: PONG
```

---

## 🎯 **5分钟快速部署**

### 步骤1：创建目录结构
```bash
sudo mkdir -p /opt/beijixing-ai/{logs,backup,config}
cd /opt/beijixing-ai
```

### 步骤2：配置数据库连接
```bash
cat > config/env.sh << 'EOF'
#!/bin/bash
# ====== 数据库配置 ======
export DB_HOST="localhost"           # 或您的数据库IP
export DB_PORT=3306
export DB_NAME="beijixing_ai"
export DB_USER="root"
export DB_PASSWORD="your-password"    # ⚠️ 请修改为实际密码

# ====== Redis配置 ======
export REDIS_HOST="localhost"         # 或您的Redis IP
export REDIS_PORT=6379
export REDIS_PASSWORD=""              # 如果Redis无密码则留空

# ====== JWT密钥（必须32字符以上）=====
export JWT_SECRET="beijixing-ai-jwt-secret-key-2026-minimum-32-chars"

# ====== 可选：邮件服务配置 ======
export SMTP_HOST="smtp.qq.com"
export SMTP_PORT=465
export SMTP_USERNAME="your-email@qq.com"
export SMTP_PASSWORD="your-smtp-auth-code"
EOF

chmod +x config/env.sh
source config/env.sh
```

### 步骤3：创建启动脚本
```bash
cat > start.sh << 'EOF'
#!/bin/bash
APP_NAME="beijixing-app"
JAR_FILE="/opt/beijixing-ai/beijixing-app.jar"
LOG_FILE="/opt/beijixing-ai/logs/application.log"

echo "========================================"
echo "  北极星AI 启动中..."
echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================"

# 加载环境变量
source /opt/beijixing-ai/config/env.sh

# JVM参数优化
JAVA_OPTS="-Xms512m -Xmx2g"
JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC"
JAVA_OPTS="$JAVA_OPTS -XX:MaxGCPauseMillis=200"
JAVA_OPTS="$JAVA_OPTS -Dfile.encoding=UTF-8"
JAVA_OPTS="$JAVA_OPTS -Duser.timezone=Asia/Shanghai"

# 启动应用
exec java $JAVA_OPTS -jar $JAR_FILE \
    --spring.profiles.active=prod \
    >> $LOG_FILE 2>&1
EOF

chmod +x start.sh
```

### 步骤4：创建停止脚本
```bash
cat > stop.sh << 'EOF'
#!/bin/bash
APP_NAME="beijixing-app"

echo "正在停止 $APP_NAME ..."

# 查找进程ID
PID=$(ps aux | grep "$APP_NAME" | grep -v grep | awk '{print $2}')

if [ -z "$PID" ]; then
    echo "⚠️  应用未运行"
    exit 0
fi

# 优雅停机（等待10秒）
kill $PID
sleep 10

# 强制终止（如果还在运行）
if ps -p $PID > /dev/null; then
    echo "强制终止..."
    kill -9 $PID
fi

echo "✅ 应用已停止"
EOF

chmod +x stop.sh
```

### 步骤5：启动应用
```bash
# 方式A：前台运行（调试用，可看到实时日志）
./start.sh

# 方式B：后台运行（生产推荐）
nohup ./start.sh &

# 查看日志
tail -f logs/application.log
```

---

## ✅ **验证部署成功**

### 检查1：确认进程运行
```bash
ps aux | grep beijixing-app | grep -v grep
# 应显示Java进程信息
```

### 检查2：健康状态接口
```bash
# 等待30秒后执行
curl http://localhost:8080/actuator/health

# 预期输出:
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"},
    ...
  }
}
```

### 检查3：查看启动成功标志
```bash
grep "北极星AI 极简单体应用启动成功" logs/application.log
# 应显示该行日志
```

### 检查4：端口监听
```bash
netstat -tlnp | grep 8080
# 应显示: tcp 0 0 0.0.0.0:8080 LISTEN [java_pid]
```

---

## 🔧 **常用运维命令**

### 日志管理
```bash
# 实时查看日志
tail -f /opt/beijixing-ai/logs/application.log

# 搜索错误日志
grep "ERROR" /opt/beijixing-ai/logs/application.log | tail -20

# 查看最近100行日志
tail -100 /opt/beijixing-ai/logs/application.log
```

### 服务管理
```bash
# 重启应用
./stop.sh && sleep 2 && ./start.sh

# 查看JVM内存使用
jstat -gc $(pgrep -f beijixing-app) 5s

# 查看线程数
jstack $(pgrep -f beijixing-app) | grep "java.lang.Thread.State" | wc -l
```

### 数据库操作
```bash
# 登录MariaDB
mysql -u root -p beijixing_ai

# 查看表结构
SHOW TABLES;

# 备份数据库
mysqldump -u root -p beijixing_ai > backup_$(date +%Y%m%d).sql
```

### Redis操作
```bash
# 连接Redis
redis-cli

# 查看键数量
DBSIZE

# 清理缓存（谨慎使用）
FLUSHDB
```

---

## 🚨 **常见问题排查**

### Q1: 启动失败 - 端口被占用
```bash
# 查看占用端口的进程
netstat -tlnp | grep 8080

# 杀掉占用进程
kill -9 <PID>

# 或者修改端口（在application-prod.yml中）
server:
  port: 8081
```

### Q2: 数据库连接失败
```bash
# 测试数据库连接
mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASSWORD -e "SELECT 1"

# 常见原因：
# - 密码错误 → 检查 env.sh 中的 DB_PASSWORD
# - 防火墙阻止 → sudo firewall-cmd --add-port=3306/tcp
# - MariaDB未启动 → systemctl start mariadb
```

### Q3: Redis连接失败
```bash
# 测试Redis连接
redis-cli -h $REDIS_HOST -p $REDIS_PORT ping

# 常见原因：
# - Redis未启动 → systemctl start redis
# - 绑定地址问题 → 检查 redis.conf 中的 bind 设置
# - 密码错误 → 检查 env.sh 中的 REDIS_PASSWORD
```

### Q4: 内存不足 OOM
```bash
# 调整JVM内存（修改 start.sh）
JAVA_OPTS="-Xms256m -Xmx1g"  # 减小内存

# 或增加服务器swap空间
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

### Q5: 权限问题
```bash
# 确保所有文件权限正确
chown -R root:root /opt/beijixing-ai
chmod +x /opt/beijixing-ai/*.sh
chmod 755 /opt/beijixing-ai/beijixing-app.jar
```

---

## 🌐 **Nginx反向代理配置（可选）**

如果需要域名访问或HTTPS，配置Nginx反向代理：

```nginx
server {
    listen 80;
    server_name your-domain.com;  # 替换为您的域名

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

保存到 `/etc/nginx/conf.d/beijixing-ai.conf`，然后：
```bash
sudo nginx -t          # 测试配置
sudo systemctl reload nginx
```

---

## 📊 **Systemd服务管理（推荐用于生产环境）**

创建系统服务，实现开机自启和自动重启：

```bash
sudo tee /etc/systemd/system/beijixing-ai.service > /dev/null << 'EOF'
[Unit]
Description=BeijiXing AI Application
After=network.target mariadb.service redis.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/beijixing-ai
ExecStart=/opt/beijixing-ai/start.sh
Restart=always
RestartSec=10
StandardOutput=append:/opt/beijixing-ai/logs/systemd.log
StandardError=append:/opt/beijixing-ai/logs/systemd-error.log

[Install]
WantedBy=multi-user.target
EOF

# 启用并启动服务
sudo systemctl daemon-reload
sudo systemctl enable beijixing-ai
sudo systemctl start beijixing-ai

# 查看服务状态
sudo systemctl status beijixing-ai

# 查看日志
sudo journalctl -u beijixing-ai -f
```

---

## 📝 **部署检查清单**

部署完成后，请逐项确认：

- [ ] Java进程正在运行（`ps aux | grep beijixing`）
- [ ] 健康检查返回UP状态（`curl localhost:8080/actuator/health`）
- [ ] 端口8080正常监听（`netstat -tlnp | grep 8080`）
- [ ] 日志无ERROR级别错误
- [ ] 数据库连接正常（health check的db组件为UP）
- [ ] Redis连接正常（health check的redis组件为UP）
- [ ] 可以通过浏览器/IP访问 `http://your-server-ip:8080`
- [ ] Postman测试核心API通过（见Postman测试集合）

---

## 🎯 **下一步行动**

部署验证成功后：
1. ✅ 使用Postman测试集合验证API功能
2. 📝 根据业务需求调整配置参数
3. 🔄 配置监控告警（可选）
4. 📊 定期备份数据库

---

**🎉 部署完成后，请访问 http://your-server-ip:8080/actuator/health 确认服务状态！**
