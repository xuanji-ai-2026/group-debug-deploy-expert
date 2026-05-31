# 北极星AI - 极简单体应用部署指南

## 📋 概述

本文档指导如何将16个微服务整合为单一Spring Boot应用并完成部署。

**核心优势:**
- ✅ 单一JAR包，简化部署流程
- ✅ 统一端口8080，无需网关
- ✅ 移除Spring Cloud依赖，减少~50MB体积
- ✅ 统一配置文件，降低维护成本

---

## 🎯 部署架构图

```
┌─────────────────────────────────────────┐
│         服务器: 43.160.237.122            │
│                                         │
│  ┌───────────────────────────────────┐  │
│  │  beijixing-app.jar (单体应用)      │  │
│  │  端口: 8080 | Context-Path: /     │  │
│  └──────────────┬────────────────────┘  │
│                 │                        │
│  ┌──────────────▼────────────────────┐  │
│  │  MariaDB (统一数据库)              │  │
│  │  数据库: beijixing_ai             │  │
│  └───────────────────────────────────┘  │
│                                         │
│  ┌───────────────────────────────────┐  │
│  │  Redis + RabbitMQ (可选)          │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

---

## 📦 第一步：准备部署文件

### 需要上传的文件清单

从本地构建完成后，需要手动上传以下文件到服务器：

```
本地路径 (Windows)                          服务器路径 (Linux)
─────────────────────────────────────────────────────────────
backend\beijixing-app\target\               /opt/beijixing-ai/
  └─ beijixing-app-1.0.0-SNAPSHOT.jar       → beijixing-app.jar

backend\beijixing-app\src\main\resources\   /opt/beijixing-ai/config/
  └─ application.yml                         → application.yml

backend\beijixing-app\src\main\resources\   /opt/beijixing-ai/db/
  └─ db\migration\merge-databases.sql        → merge-databases.sql
```

### 上传方式（三选一）

#### 方式1：文件管理器（推荐新手）
1. 使用 WinSCP、FileZilla 或 Xftp 连接服务器
2. 导航到 `/opt/beijixing-ai/`
3. 拖拽上传上述3个文件

#### 方式2：命令行SCP
```bash
# Windows PowerShell
scp d:\BeijiXing-AI\backend\beijixing-app\target\beijixing-app-1.0.0-SNAPSHOT.jar root@43.160.237.122:/opt/beijixing-ai/beijixing-app.jar

scp d:\BeijiXing-AI\backend\beijixing-app\src\main\resources\application.yml root@43.160.237.122:/opt/beijixing-ai/config/

scp d:\BeijiXing-AI\backend\beijixing-app\src\main\resources\db\migration\merge-databases.sql root@43.160.237.122:/opt/beijixing-ai/db/
```

#### 方式3：压缩包上传（最快）
```bash
# 本地打包
cd d:\BeijiXing-AI\backend
tar -czvf monolith-deploy.tar.gz \
    beijixing-app/target/beijixing-app-1.0.0-SNAPSHOT.jar \
    beijixing-app/src/main/resources/application.yml \
    beijixing-app/src/main/resources/db/merge-databases.sql

# 上传压缩包 (~50MB，比单个JAR快)
scp monolith-deploy.tar.gz root@43.160.237.122:/opt/beijixing-ai/

# 服务器解压
ssh root@43.160.237.122
cd /opt/beijixing-ai
tar -xzvf monolith-deploy.tar.gz
mv beijixing-app-1.0.0-SNAPSHOT.jar beijixing-app.jar
mkdir -p config db
mv application.yml config/
mv merge-databases.sql db/
```

---

## 🗄️ 第二步：数据库迁移

### 方案A：合并为单一数据库（推荐）

将14个独立schema合并为`beijixing_ai`：

```bash
# 1. SSH登录服务器
ssh root@43.160.237.122

# 2. 登录MariaDB
mysql -u root -p

# 3. 执行数据库合并脚本
source /opt/beijixing-ai/db/merge-databases.sql;

# 4. 或者使用一键命令（如果所有旧库都存在）
-- 在MariaDB中执行:
USE beijixing_ai;

-- 批量迁移表（取消注释并执行）
/*
SET @databases = 'bx_user,bx_tenant,bx_lead,bx_content,bx_ai,bx_risk,bx_billing,bx_message,bx_storage,bx_system,bx_data,bx_social,bx_schedule,bx_monitor';

SET @sql = (
    SELECT GROUP_CONCAT(
        CONCAT('RENAME TABLE ', table_schema, '.', table_name, ' TO beijixing_ai.', table_name, ';')
        SEPARATOR '\n'
    )
    FROM information_schema.tables
    WHERE table_schema IN (@databases)
    AND table_type = 'BASE TABLE'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
*/

# 5. 验证迁移结果
SHOW DATABASES;
SHOW TABLES FROM beijixing_ai;

# 6. (可选) 确认无误后删除旧数据库
-- DROP DATABASE IF EXISTS bx_user;
-- DROP DATABASE IF EXISTS bx_tenant;
-- ... (其他13个数据库)
```

### 方案B：保持多数据库（兼容模式）

如果不想合并数据库，修改`application.yml`：

```yaml
# 方案B: 多数据源配置 (需要额外代码适配，不推荐)
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/bx_user  # 主数据源
    # 其他数据源需要配置多数据源...
```

> ⚠️ **警告**: 方案B需要修改Java代码以支持多数据源，违反"最小改动原则"，不推荐。

---

## ⚙️ 第三步：环境变量配置

创建环境变量文件 `/opt/beijixing-ai/.env`：

```bash
cat > /opt/beijixing-ai/.env << 'EOF'
# ==============================================
# 北极星AI - 极简单体应用环境变量
# 最后更新: 2026-05-20
# ==============================================

# ====== MariaDB数据库 ======
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=beijixing_ai          # 统一数据库名
export DB_USER=root
export DB_PASSWORD=your_password_here     # ⚠️ 修改为实际密码

# ====== Redis ======
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=                   # 无密码则留空

# ====== JWT密钥 ======
export JWT_SECRET=YourSuperSecretJWTKey2024ForTokenGeneration!  # ⚠️ 修改为强密钥

# ====== AI模型API (可选) ======
export AI_PROVIDER_TYPE=volcano           # volcano 或 deepseek
export VOLCENGINE_API_KEY=                # 火山引擎API Key
export DEEPSEEK_API_KEY=                  # DeepSeek API Key

# ====== 腾讯云短信 (bx-user模块) ======
export TENCENT_SECRET_ID=
export TENCENT_SECRET_KEY=

# ====== 邮件服务 (bx-user + bx-monitor) ======
export SMTP_HOST=smtp.qq.com
export SMTP_PORT=465
export SMTP_USERNAME=your@qq.com
export SMTP_PASSWORD=your_smtp_auth_code

# ====== RabbitMQ (可选) ======
export RABBITMQ_HOST=localhost
export RABBITMQ_PORT=5672
EOF

chmod 600 /opt/beijixing-ai/.env
```

加载环境变量：

```bash
source /opt/beijixing-ai/.env
echo "✓ 环境变量已加载"
```

---

## 🚀 第四步：启动应用

### 方式1：前台运行（测试用）

```bash
cd /opt/beijixing-ai

java -jar beijixing-app.jar \
    --spring.config.location=config/application.yml
```

### 方式2：后台运行（生产推荐）

```bash
cd /opt/beijixing-ai

nohup java -jar beijixing-app.jar \
    --spring.config.location=config/application.yml \
    > logs/app.log 2>&1 &

echo $! > app.pid
echo "✅ 应用已在后台启动，PID: $(cat app.pid)"
```

### 方式3：Systemd服务（最佳实践）

创建服务文件：

```bash
cat > /etc/systemd/system/beijixing-ai.service << 'EOF'
[Unit]
Description=北极星AI极简单体应用
After=network.target mariadb.service redis.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/beijixing-ai
ExecStart=/usr/bin/java -jar beijixing-app.jar --spring.config.location=config/application.yml
Restart=on-failure
RestartSec=10
EnvironmentFile=/opt/beijixing-ai/.env

[Install]
WantedBy=multi-user.target
EOF
```

启动服务：

```bash
systemctl daemon-reload
systemctl enable beijixing-ai
systemctl start beijixing-ai
systemctl status beijixing-ai
```

---

## ✅ 第五步：验证部署

### 1. 检查进程状态

```bash
ps aux | grep beijixing-app | grep -v grep
# 应该看到Java进程正在运行
```

### 2. 检查健康端点

```bash
curl -s http://localhost:8080/actuator/health | jq .
# 预期输出: {"status":"UP",...}
```

### 3. 测试API可用性

```bash
# 测试根路径
curl -s http://localhost:8080/ | head -20

# 测试用户认证API (如果有)
curl -s http://localhost:8080/api/auth/login -X POST \
    -H "Content-Type: application/json" \
    -d '{"phone":"13800138000","password":"test123"}'

# 测试商机管理API (如果有)
curl -s http://localhost:8080/api/leads | head -20
```

### 4. 检查日志

```bash
# 实时查看日志
tail -f /opt/beijixing-ai/logs/beijixing-ai.log

# 查看启动日志（检查错误）
grep -i "error\|exception" /opt/beijixing-ai/logs/beijixing-ai.log | head -50
```

### 5. 前端验证

打开浏览器访问：
- Web前端: `http://43.160.237.122:8070`
- API文档: `http://43.160.237.122:8080/actuator`
- 健康检查: `http://43.160.237.122:8080/actuator/health`

---

## 🔧 第六步：常见问题排查

### 问题1：端口冲突

**症状**: `Address already in use: bind`

**解决方案**:
```bash
# 查看占用8080端口的进程
lsof -i :8080
netstat -tlnp | grep 8080

# 杀掉占用进程
kill -9 <PID>

# 或者修改application.yml中的端口
server:
  port: 8081  # 改为其他端口
```

### 问题2：数据库连接失败

**症状**: `Could not create connection to database server`

**解决方案**:
```bash
# 1. 检查MariaDB是否运行
systemctl status mariadb

# 2. 手动测试连接
mysql -h localhost -u root -p -e "SELECT 1"

# 3. 检查数据库名是否正确
mysql -e "SHOW DATABASES;" | grep beijixing

# 4. 检查防火墙（如果是远程数据库）
firewall-cmd --list-ports
```

### 问题3：Redis连接失败

**症状**: `Unable to connect to Redis`

**解决方案**:
```bash
# 检查Redis是否运行
redis-cli ping
# 应返回 PONG

# 如果未运行
systemctl start redis
```

### 问题4：内存不足（OOM）

**症状**: 应用启动后崩溃，日志显示`OutOfMemoryError`

**解决方案**:
```bash
# 增加JVM堆内存
java -Xms512m -Xmx1024m -jar beijixing-app.jar ...

# 或者修改Systemd服务
# 在 [Service] 段添加:
Environment="JAVA_OPTS=-Xms512m -Xmx1024m"
ExecStart=/usr/bin/java $JAVA_OPTS -jar ...
```

### 问题5：类冲突或Bean重复定义

**症状**: `The bean 'xxx' could not be registered...`

**原因**: 多个模块有相同的启动类或配置类

**解决方案**:
- 检查`BeijixingAiApplication.java`的`@ComponentScan`排除列表
- 确保所有15个子模块的启动类已被排除
- 如果仍有冲突，添加具体的排除规则

### 问题6：MyBatis Mapper扫描失败

**症状**: `Invalid bound statement (not found)`

**解决方案**:
```yaml
# 确认application.yml中配置正确:
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml  # 注意通配符*
```

---

## 🔄 第七步：回滚方案

如果新版本出现问题，快速回滚到微服务架构：

```bash
# 1. 停止单体应用
systemctl stop beijixing-ai
# 或
kill $(cat /opt/beijixing-ai/app.pid)

# 2. 恢复原来的微服务启动脚本
cd /opt/beijixing-ai/services
./start-all-services.sh  # 假设你有这个脚本

# 3. 验证微服务运行正常
curl -s http://localhost:8080/actuator/health
```

---

## 📊 性能对比指标

| 指标 | 微服务架构 (16个) | 单体应用 |
|------|-------------------|----------|
| **启动时间** | 5-10分钟 (并行) | 30-60秒 |
| **内存占用** | ~8GB (16个JVM) | ~1GB (1个JVM) |
| **JAR包大小** | ~1.6GB (总计) | ~150MB |
| **端口数量** | 13个 | 1个 (8080) |
| **配置文件** | 48个 | 1个 |
| **部署复杂度** | 高 | 低 |
| **运维成本** | 高 | 低 |

---

## 📝 维护清单

### 日常运维

- [ ] 每日检查应用日志: `tail -100 /opt/beijixing-ai/logs/beijixing-ai.log`
- [ ] 监控系统资源: `top`, `free -h`, `df -h`
- [ ] 备份数据库: `mysqldump -u root -p beijixing_ai > backup_$(date +%Y%m%d).sql`
- [ ] 检查健康端点: `curl http://localhost:8080/actuator/health`

### 定期更新

- [ ] 更新JAR包: 停止服务 → 上传新JAR → 启动服务
- [ ] 更新配置: 修改`config/application.yml` → 重启服务
- [ ] 数据库迁移: 如需变更表结构，编写新的SQL脚本

---

## 🆘 技术支持

如遇问题，请按以下顺序排查：

1. **查看日志**: `/opt/beijixing-ai/logs/beijixing-ai.log`
2. **检查端口**: `netstat -tlnp | grep 8080`
3. **检查进程**: `ps aux | grep java`
4. **测试数据库**: `mysql -u root -p -e "SELECT 1"`
5. **测试Redis**: `redis-cli ping`

---

**最后更新**: 2026-05-20
**适用版本**: beijixing-ai v1.0.0 (Monolith Edition)
