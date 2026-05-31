# 北极星AI - 生产环境部署指南与运维手册 v1.0

> **适用版本**: 极简单体应用 v1.0 (beijixing-app-1.0.0-SNAPSHOT)
> **部署目标**: 43.160.237.122 (新加坡腾讯云)
> **架构模式**: Monolith单体架构（16个微服务模块合并为单一Spring Boot应用）
> **最后更新**: 2026-05-21
> **维护人员**: 运维团队

---

## 目录

- [第一部分：部署前准备](#第一部分部署前准备)
  - [1.1 硬件要求](#11-硬件要求)
  - [1.2 软件环境](#12-软件环境)
  - [1.3 网络规划](#13-网络规划)
- [第二部分：应用部署](#第二部分应用部署)
  - [2.1 方式一：完整Maven构建部署](#21-方式一完整maven构建部署)
  - [2.2 方式二：Class文件热替换（快速修复）](#22-方式二class文件热替换快速修复)
  - [2.3 启动命令（生产级）](#23-启动命令生产级)
  - [2.4 Systemd服务配置（推荐）](#24-systemd服务配置推荐)
- [第三部分：数据库初始化](#第三部分数据库初始化)
  - [3.1 创建数据库和用户](#31-创建数据库和用户)
  - [3.2 数据库表结构说明](#32-数据库表结构说明)
- [第四部分：Redis配置](#第四部分redis配置)
  - [4.1 redis.conf关键配置](#41-redisconf关键配置)
  - [4.2 Redis安全加固](#42-redis安全加固)
- [第五部分：Nginx配置](#第五部分nginx配置)
  - [5.1 反向代理配置示例](#51-反向代理配置示例)
  - [5.2 HTTPS配置](#52-https配置)
- [第六部分：监控与日志](#第六部分监控与日志)
  - [6.1 日志查看命令](#61-日志查看命令)
  - [6.2 常用运维命令](#62-常用运维命令)
  - [6.3 性能监控](#63-性能监控)
- [第七部分：故障排查](#第七部分故障排查)
  - [7.1 常见问题及解决方案](#71-常见问题及解决方案)
  - [7.2 应急处理流程](#72-应急处理流程)
- [第八部分：安全加固](#第八部分安全加固)
  - [8.1 系统安全](#81-系统安全)
  - [8.2 应用安全](#82-应用安全)
- [第九部分：备份与恢复](#第九部分备份与恢复)
  - [9.1 数据库备份策略](#91-数据库备份策略)
  - [9.2 文件备份](#92-文件备份)
  - [9.3 恢复演练](#93-恢复演练)
- [第十部分：附录](#第十部分附录)
  - [10.1 环境变量清单](#101-环境变量清单)
  - [10.2 配置文件模板](#102-配置文件模板)
  - [10.3 常用SQL查询](#103-常用sql查询)

---

## 第一部分：部署前准备

### 1.1 硬件要求

| 资源 | 最低配置 | 推荐配置 | 生产推荐 |
|------|---------|---------|---------|
| **CPU** | 2核 | 4核 | 8核+ |
| **内存** | 4GB | 8GB | 16GB+ |
| **磁盘** | SSD 50GB | SSD 100GB | SSD 200GB+ |
| **带宽** | 5Mbps | 10Mbps | 20Mbps+ |

**当前服务器规格参考**（43.160.237.122）：
- CPU: 2核 / 内存: ~7.5GB
- JVM堆内存建议: `-Xms512m -Xmx1024m`（预留足够空间给OS和Redis）

### 1.2 软件环境

#### 1.2.1 JDK 17 安装（TencentKonaJDK / OpenJDK）

```bash
# ====== Ubuntu/Debian ======
# 方法A: 使用APT安装OpenJDK 17
sudo apt update
sudo apt install -y openjdk-17-jdk

# 方法B: 安装TencentKonaJDK 17（腾讯云优化版，推荐）
cd /tmp
wget https://github.com/Tencent/TencentKonaJDK17/releases/download/v17.0.11/TencentKonaJDK-17.0.11.b1_linux-x86_64.tar.gz
sudo mkdir -p /usr/lib/jvm/
sudo tar -xzf TencentKonaJDK-17.0.11.b1_linux-x86_64.tar.gz -C /usr/lib/jvm/

# 配置环境变量
cat >> ~/.bashrc << 'EOF'
export JAVA_HOME=/usr/lib/jvm/TencentKonaJDK-17.0.11.b1
export PATH=$JAVA_HOME/bin:$PATH
EOF
source ~/.bashrc

# 验证安装
java -version
# 预期输出: openjdk version "17.x.x" 或 TencentKona 17.x.x

# ====== CentOS/RHEL ======
sudo yum install -y java-17-openjdk java-17-openjdk-devel
# 或使用Amazon Corretto
sudo yum install -y amazon-corretto-17
```

#### 1.2.2 Maven 3.8+ 安装（如需在服务器上构建）

```bash
# 下载Maven
cd /tmp
wget https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz
sudo tar -xzf apache-maven-3.9.6-bin.tar.gz -C /opt/
sudo ln -s /opt/apache-maven-3.9.6 /opt/maven

# 配置环境变量
cat >> ~/.bashrc << 'EOF'
export MAVEN_HOME=/opt/maven
export PATH=$MAVEN_HOME/bin:$PATH
EOF
source ~/.bashrc

# 验证安装
mvn -version
```

#### 1.2.3 MariaDB 10.x 安装与配置

```bash
# ====== Ubuntu 22.04+ ======
sudo apt update
sudo apt install -y mariadb-server mariadb-client

# ====== CentOS/RHEL 8+ ======
sudo yum install -y mariadb-server mariadb-client
sudo systemctl enable --now mariadb

# 安全初始化（设置root密码等）
sudo mysql_secure_installation

# 验证服务状态
systemctl status mariadb
mysql --version
```

**MariaDB生产优化配置** (`/etc/mysql/mariadb.conf.d/99-custom.cnf`)：

```ini
[mysqld]
# 字符集
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci

# 连接配置
max_connections = 500
max_connect_errors = 100
wait_timeout = 28800
interactive_timeout = 28800

# InnoDB引擎优化（核心！）
innodb_buffer_pool_size = 1G          # 根据服务器内存调整，建议物理内存的50%-70%
innodb_log_file_size = 256M
innodb_log_buffer_size = 64M
innodb_flush_log_at_trx_commit = 2     # 性能与安全的平衡
innodb_flush_method = O_DIRECT
innodb_file_per_table = 1
innodb_io_capacity = 2000
innodb_io_capacity_max = 4000

# 查询缓存（MariaDB特有）
query_cache_type = 1
query_cache_size = 128M

# 慢查询日志
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 2

# 二进制日志（用于主从复制和增量备份）
log_bin = /var/log/mysql/mysql-bin.log
binlog_format = ROW
expire_logs_days = 7
max_binlog_size = 256M

# 时区
default-time-zone = '+08:00'

[client]
default-character-set = utf8mb4
```

#### 1.2.4 Redis 7.x 安装与配置

```bash
# ====== Ubuntu/Debian ======
sudo apt install -y redis-server

# ====== CentOS/RHEL ======
sudo yum install -y epel-release
sudo yum install -y redis

# 或者从源码编译安装最新版Redis 7.x
cd /tmp
wget http://download.redis.io/releases/redis-7.2.4.tar.gz
tar xzf redis-7.2.4.tar.gz
cd redis-7.2.4
make && sudo make install PREFIX=/usr/local/redis

# 创建数据和日志目录
sudo mkdir -p /var/lib/redis /var/log/redis /etc/redis

# 复制配置文件
sudo cp redis.conf /etc/redis/redis.conf

# 启动Redis
sudo systemctl enable redis
sudo systemctl start redis

# 验证安装
redis-cli ping
# 预期输出: PONG
```

#### 1.2.5 Nginx 安装（可选，用于反向代理和静态资源）

```bash
# ====== Ubuntu/Debian ======
sudo apt install -y nginx

# ====== CentOS/RHEL ======
sudo yum install -y epel-release
sudo yum install -y nginx

# 启动并设为开机自启
sudo systemctl enable nginx
sudo systemctl start nginx
```

### 1.3 网络规划

#### 1.3.1 防火墙规则

```bash
# 使用ufw（Ubuntu）或firewalld（CentOS）配置防火墙

# ====== UFW (Ubuntu) ======
sudo ufw allow 22/tcp        # SSH
sudo ufw allow 80/tcp        # HTTP (Nginx)
sudo ufw allow 443/tcp       # HTTPS (Nginx)
sudo ufw allow 8080/tcp      # Spring Boot应用端口
sudo ufw allow 6379/tcp      # Redis（仅限内网访问）
sudo ufw allow 3306/tcp      # MariaDB（仅限内网访问）
sudo ufw allow 9999/tcp      # JMX监控端口（仅限内网）
sudo ufw enable

# ====== Firewalld (CentOS) ======
sudo firewall-cmd --permanent --add-port=22/tcp
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --permanent --add-port=443/tcp
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
```

**重要**: 生产环境中，3306（MySQL）和6379（Redis）端口应仅绑定到`127.0.0.1`或内网IP，禁止公网访问。

#### 1.3.2 SSL证书配置（Let's Encrypt免费证书）

```bash
# 安装Certbot
sudo apt install -y certbot python3-certbot-nginx  # Ubuntu
# sudo yum install -y certbot python3-certbot-nginx  # CentOS

# 申请SSL证书（需要域名已解析到服务器IP）
sudo certbot --nginx -d beijixing-ai.com -d www.beijixing-ai.com

# 自动续期（certbot通常会自动配置cron job）
sudo certbot renew --dry-run    # 测试续期是否正常
```

#### 1.3.3 域名解析配置

在域名DNS管理面板中添加以下记录：

| 记录类型 | 主机记录 | 记录值 | TTL |
|---------|---------|--------|-----|
| A | @ | 43.160.237.122 | 600 |
| A | www | 43.160.237.122 | 600 |
| A | api | 43.160.237.122 | 600 |

---

## 第二部分：应用部署

### 2.1 方式一：完整Maven构建部署（标准方式）

#### 步骤1：本地构建

```bash
# 在项目根目录执行（Windows PowerShell或Linux/Mac终端）
cd d:\BeijiXing-AI\backend   # Windows
# cd /path/to/BeijiXing-AI/backend   # Linux/Mac

# 清理并打包（跳过测试以加速构建）
mvn clean package -DskipTests -pl beijixing-app -am

# 如果遇到依赖问题，使用完整构建
mvn clean package -DskipTests

# 构建产物位置
# beijixing-app/target/beijixing-app-1.0.0-SNAPSHOT.jar
```

**构建注意事项**：
- Java版本必须为 **JDK 17**
- Maven版本要求 **3.8+**
- 项目使用 `maven-shade-plugin` 打包为扁平化单一JAR（非Spring Boot fat jar格式）
- 主类: `com.beijixing.BeijixingAiApplication`
- 包大小约: **150MB-250MB**（取决于依赖数量）

#### 步骤2：上传到服务器

```bash
# ====== 方式A: SCP上传 ======
scp beijixing-app/target/beijixing-app-1.0.0-SNAPSHOT.jar root@43.160.237.122:/opt/beijixing-ai/backend/beijixing-app/

# ====== 方式B: Rsync增量同步（推荐，大文件更高效）=====
rsync -avz --progress \
  beijixing-app/target/beijixing-app-1.0.0-SNAPSHOT.jar \
  root@43.160.237.122:/opt/beijixing-ai/backend/beijixing-app/
```

#### 步骤3：远程服务器操作

```bash
# SSH登录服务器
ssh root@43.160.237.122

# 创建目录结构
mkdir -p /opt/beijixing-ai/{backend/beijixing-app,logs,config,backup,uploads}

# 备份旧版本（如果存在）
if [ -f /opt/beijixing-ai/backend/beijixing-app/beijixing-app.jar ]; then
  cp /opt/beijixing-ai/backend/beijixing-app/beijixing-app.jar \
     /opt/beijixing-ai/backup/beijixing-app-$(date +%Y%m%d%H%M%S).jar
fi

# 部署新版本
cp /opt/beijixing-ai/backend/beijixing-app/beijixing-app-1.0.0-SNAPSHOT.jar \
   /opt/beijixing-ai/backend/beijixing-app/beijixing-app.jar
```

#### 步骤4：远程构建（备选方案 - 源代码已在服务器上时）

```bash
# 在服务器上克隆或更新代码
cd /opt/beijixing-ai/backend
git pull origin main

# 远程构建
mvn clean package -DskipTests -pl beijixing-app -am
```

### 2.2 方式二：Class文件热替换（快速修复/紧急补丁）

> **适用场景**: 仅修改了少量Java文件，需要快速上线修复bug，无需重新完整构建。

```bash
# ====== 步骤1: 编译修改的Java文件 ======
# 进入项目目录
cd /path/to/BeijiXing-AI/backend

# 编译单个修改的文件（示例：修改了UserServiceImpl.java）
javac -encoding UTF-8 \
  -cp "beijixing-app/target/beijixing-app-1.0.0-SNAPSHOT.jar" \
  -d /tmp/update \
  bx-user/src/main/java/com/beijixing/user/service/impl/UserServiceImpl.java

# ====== 步骤2: 替换JAR包中的class文件 ======
# 将编译好的class文件打入JAR包
cd /tmp/update
jar uf /opt/beijixing-ai/backend/beijixing-app/beijixing-app.jar \
  com/beijixing/user/service/impl/UserServiceImpl.class

# ====== 步骤3: 重启应用使更改生效 ======
systemctl restart beijixing-ai
# 或使用 kill + nohup 方式重启
```

**热替换注意事项**：
- 仅适用于方法体的修改，新增/删除字段、方法签名变更可能导致 `ClassCastException`
- 替换后必须重启应用才能生效
- 建议：每次热替换后进行功能回归测试
- 生产环境仍推荐完整构建部署以确保一致性

### 2.3 启动命令（生产级）

#### 方式A: 直接nohup启动（快速验证）

```bash
#!/bin/bash
# ============================================================
# 北极星AI - 应用启动脚本（生产优化版 v2.0）
# ============================================================

set -e

APP_NAME="beijixing-app"
JAR_FILE="/opt/beijixing-ai/backend/beijixing-app/beijixing-app.jar"
LOG_DIR="/opt/beijixing-ai/logs"
CONFIG_DIR="/opt/beijixing-ai/config"

# ====== 环境变量配置 ======
export DB_HOST=localhost
export DB_PORT=3306
export DB_USER=root
export DB_PASSWORD='Beijixing@2024!'
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD='Redis@2026Secure!'

# ====== JVM参数（基于2核CPU/7.5GB内存优化）=====
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

# 创建必要目录
mkdir -p ${LOG_DIR} ${CONFIG_DIR}

# 检查并停止旧进程
if [ -f "${LOG_DIR}/app.pid" ]; then
    PID=$(cat ${LOG_DIR}/app.pid)
    if ps -p ${PID} > /dev/null 2>&1; then
        echo "[INFO] 停止旧进程 PID=${PID}"
        kill ${PID}
        sleep 5
        if ps -p ${PID} > /dev/null 2>&1; then
            kill -9 ${PID}
            sleep 2
        fi
    fi
fi

echo "[INFO] 启动北极星AI应用..."
echo "[INFO] JVM堆内存: 512MB - 1024MB"
echo "[INFO] GC策略: G1GC (MaxPause=200ms)"

# 启动应用
nohup java ${JAVA_OPTS} \
  -Dspring.datasource.driver-class-name=org.mariadb.jdbc.Driver \
  -Dspring.datasource.url="jdbc:mariadb://localhost:3306/beijixing_ai?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true&autoReconnect=true" \
  -Dspring.datasource.username=root \
  -Dspring.datasource.password='Beijixing@2024!' \
  -Dspring.data.redis.host=localhost \
  -Dspring.data.redis.port=6379 \
  -Dspring.data.redis.password='Redis@2026Secure!' \
  -Dmybatis-plus.mapper-locations=classpath*:/mapper/**/*.xml \
  -Dmybatis-plus.type-aliases-package=com.beijixing.*.entity \
  -Dspring.main.allow-bean-definition-overriding=true \
  -Dspring.main.allow-circular-references=true \
  -Dspring.flyway.enabled=false \
  -jar ${JAR_FILE} \
  --spring.profiles.active=prod \
  > ${LOG_DIR}/app.log 2>&1 &

# 记录PID
echo $! > ${LOG_DIR}/app.pid
echo "[INFO] 应用已启动 PID=$(cat ${LOG_DIR}/app.pid)"
echo "[INFO] 日志文件: ${LOG_DIR}/app.log"
echo "[INFO] GC日志: ${LOG_DIR}/gc.log"
echo "[INFO] 健康检查: curl http://localhost:8080/actuator/health"
```

#### JVM参数调优说明

| 参数 | 当前值 | 说明 |
|------|-------|------|
| `-Xms512m` | 512MB | 初始堆内存 |
| `-Xmx1024m` | 1024MB | 最大堆内存（根据总内存调整） |
| `+UseG1GC` | G1垃圾收集器 | 低延迟GC，适合Web应用 |
| `MaxGCPauseMillis=200` | 200ms | GC最大停顿时间目标 |
| `G1HeapRegionSize=16m` | 16MB | G1 Region大小 |
| `StringDeduplication` | 开启 | 字符串去重，节省内存 |
| `HeapDumpOnOOM` | 开启 | OOM时自动生成堆转储 |
| `InitiatingHeapOccupancyPercent=45` | 45% | 并发GC触发阈值 |

**不同服务器规格的JVM参数建议**：

| 服务器内存 | -Xms | -Xmx | 适用场景 |
|-----------|------|------|---------|
| 4GB | 256m | 512m | 最小可用 |
| 8GB | 512m | 1024m | **当前服务器推荐** |
| 16GB | 1024m | 2048m | 中型规模 |
| 32GB | 2048m | 4096m | 大型规模 |

### 2.4 Systemd服务配置（推荐 - 生产环境必选）

#### 创建Systemd Unit文件

```bash
# 创建服务文件
sudo tee /etc/systemd/system/beijixing-ai.service << 'EOF'
[Unit]
Description=BeijiXing AI Monolith Application
Documentation=https://github.com/beijixing-ai
After=network.target mariadb.service redis.service
Requires=network.target
Wants=mariadb.service redis.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/beijixing-ai

# 环境变量
Environment=DB_HOST=localhost
Environment=DB_PORT=3306
Environment=DB_USER=root
Environment=DB_PASSWORD=Beijixing@2024!
Environment=REDIS_HOST=localhost
Environment=REDIS_PORT=6379
Environment=REDIS_PASSWORD=Redis@2026Secure!

# 启动命令
ExecStart=/usr/bin/java \
  -Xms512m -Xmx1024m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/opt/beijixing-ai/logs/heapdump.hprof \
  -Dspring.datasource.driver-class-name=org.mariadb.jdbc.Driver \
  -Dspring.datasource.url=jdbc:mariadb://localhost:3306/beijixing_ai?useUnicode=true\&characterEncoding=utf8\&useSSL=false\&serverTimezone=Asia/Shanghai\&allowPublicKeyRetrieval=true\&rewriteBatchedStatements=true\&autoReconnect=true \
  -Dspring.datasource.username=root \
  -Dspring.datasource.password=Beijixing@2024! \
  -Dspring.data.redis.host=localhost \
  -Dspring.data.redis.port=6379 \
  -Dspring.data.redis.password=Redis@2026Secure! \
  -Dspring.main.allow-bean-definition-overriding=true \
  -Dspring.main.allow-circular-references=true \
  -Dspring.flyway.enabled=false \
  -jar /opt/beijixing-ai/backend/beijixing-app/beijixing-app.jar \
  --spring.profiles.active=prod

# 重启策略
Restart=on-failure
RestartSec=10
StartLimitIntervalSec=300
StartLimitBurst=5

# 日志输出
StandardOutput=append:/opt/beijixing-ai/logs/app.log
StandardError=append:/opt/beijixing-ai/logs/app-error.log

# 安全限制
NoNewPrivileges=yes
PrivateTmp=yes
ProtectSystem=strict
ReadWritePaths=/opt/beijixing-ai/logs /opt/beijixing-ai/uploads /opt/beijixing-ai/config

# 资源限制
MemoryMax=2G
CPUQuota=150%

[Install]
WantedBy=multi-user.target
EOF
```

#### Systemd管理命令

```bash
# 重载systemd配置
sudo systemctl daemon-reload

# 设置开机自启
sudo systemctl enable beijixing-ai

# 启动服务
sudo systemctl start beijixing-ai

# 查看状态
sudo systemctl status beijixing-ai

# 停止服务
sudo systemctl stop beijixing-ai

# 重启服务
sudo systemctl restart beijixing-ai

# 查看实时日志
sudo journalctl -u beijixing-ai -f

# 查看最近100行日志
sudo journalctl -u beijixing-ai -n 100

# 查看今天的日志
sudo journalctl -u beijixing-ai --since today
```

---

## 第三部分：数据库初始化

### 3.1 创建数据库和用户

```sql
-- ==============================================
-- 北极星AI - 数据库初始化脚本
-- 执行方式: mysql -u root -p < init-database.sql
-- ==============================================

-- 创建数据库（utf8mb4支持emoji等4字节字符）
CREATE DATABASE IF NOT EXISTS beijixing_ai
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- 创建专用数据库用户（最小权限原则）
CREATE USER IF NOT EXISTS 'beijixing'@'localhost'
  IDENTIFIED BY 'Beijixing@2024!';

-- 授予全部权限（生产环境可按需收紧）
GRANT ALL PRIVILEGES ON beijixing_ai.* TO 'beijixing'@'localhost';
GRANT SELECT ON mysql.* TO 'beijixing'@'localhost';

-- 刷新权限
FLUSH PRIVILEGES;

-- 验证连接
SELECT User, Host FROM mysql.user WHERE User = 'beijixing';
SHOW DATABASES LIKE 'beijixing_ai';
```

### 3.2 数据库表结构说明

北极星AI采用 **MyBatis-Plus ORM框架** + **自动建表策略**。项目包含以下业务模块及其数据表：

| 模块 | 表名前缀 | 说明 | Mapper XML |
|------|---------|------|-----------|
| **bx-user** | sys_user, sys_role, sys_permission | 用户、角色、权限 | [UserMapper.xml](backend/bx-user/src/main/resources/mapper/UserMapper.xml), [RoleMapper.xml](backend/bx-user/src/main/resources/mapper/RoleMapper.xml), [PermissionMapper.xml](backend/bx-user/src/main/resources/mapper/PermissionMapper.xml) |
| **bx-tenant** | bx_tenant, bx_tenant_package | 多租户管理 | [TenantMapper.xml](backend/bx-tenant/src/main/resources/mapper/TenantMapper.xml) |
| **bx-lead** | bx_lead, bx_lead_task | 商机/线索管理 | [LeadMapper.xml](backend/bx-lead/src/main/resources/mapper/LeadMapper.xml) |
| **bx-content** | bx_content, bx_content_publish_record | 内容生成/发布 | [ContentMapper.xml](backend/bx-content/src/main/resources/mapper/ContentMapper.xml) |
| **bx-risk** | bx_risk_rule, bx_risk_event | 风控规则/事件 | [RiskMapper.xml](backend/bx-risk/src/main/resources/mapper/RiskMapper.xml) |
| **bx-billing** | bx_order, bx_recharge, bx_balance | 订单/充值/余额 | - |
| **bx-message** | bx_message, bx_notification | 消息通知 | - |
| **bx-social** | bx_social_account | 社交账号绑定 | [AccountMapper.xml](backend/bx-social/src/main/resources/mapper/AccountMapper.xml) |
| **bx-schedule** | bx_schedule_job, bx_schedule_job_log | 定时任务 | [ScheduleJobMapper.xml](backend/bx-schedule/src/main/resources/mapper/ScheduleJobMapper.xml) |
| **bx-system** | sys_oper_log, sys_file, sys_dict, sys_config, sys_job | 系统管理 | [SysOperLogMapper.xml](backend/bx-system/src/main/resources/mapper/SysOperLogMapper.xml), [SysFileMapper.xml](backend/bx-system/src/main/resources/mapper/SysFileMapper.xml) |

**自动建表配置**（application.yml中的MyBatis-Plus全局配置）：

```yaml
mybatis-plus:
  global-config:
    db-config:
      id-type: auto              # 自增主键
      logic-delete-field: deleted # 逻辑删除字段
      logic-delete-value: 1       # 已删除
      logic-not-delete-value: 0   # 未删除
```

> **注意**: 本项目当前未启用Flyway/Liquibase等数据库迁移工具。首次启动时，若表不存在会报错。需确保数据库中已存在所需的表结构，或通过DDL脚本手动创建。

---

## 第四部分：Redis配置

### 4.1 redis.conf 关键配置

以下为生产环境优化的Redis配置（基于项目实际使用的 [redis.conf](deploy/docker/redis/redis.conf)）：

```conf
# ==============================================
# 北极星AI - Redis 7.x 生产配置
# 文件位置: /etc/redis/redis.conf
# ==============================================

# ========== 基础配置 ==========
bind 127.0.0.1            # 仅监听本地（安全考虑），Docker环境改为 0.0.0.0
port 6379
tcp-backlog 511
timeout 300                # 客户端空闲超时(秒)
tcp-keepalive 60
protected-mode yes         # 保护模式开启
daemonize yes              # 后台运行（独立部署时）
pidfile /var/run/redis/redis-server.pid
logfile /var/log/redis/redis-server.log
loglevel notice            # 日志级别: debug/verbose/notice/warning
databases 16               # 数据库数量

# ========== 安全配置 ==========
requirepass Redis@2026Secure!  # 密码认证（必须设置！）

# 命令重命名（防误操作，按需启用）
# rename-command FLUSHALL ""
# rename-command FLUSHDB ""

# ========== 持久化 - RDB快照 ==========
dbfilename dump.rdb
dir /var/lib/redis
save 900 1                 # 15分钟内有1个key变化则保存
save 300 10                # 5分钟内有10个key变化则保存
save 60 10000              # 1分钟内有10000个key变化则保存
stop-writes-on-bgsave-error yes
rdbcompression yes
rdbchecksum yes

# ========== 持久化 - AOF日志 ==========
appendonly yes              # 启用AOF（推荐！数据更安全）
appendfilename "appendonly.aof"
appendfsync everysec        # 每秒同步（性能与安全的最佳平衡）
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb
aof-use-rdb-preamble yes    # AOF重写时使用RDB前缀（混合持久化）

# ========== 内存管理 ==========
maxmemory 512mb             # 最大内存（根据服务器调整）
maxmemory-policy volatile-lru  # 淘汰策略: 从有过期时间的key中LRU淘汰
maxmemory-samples 5

# ========== 连接配置 ==========
maxclients 10000            # 最大客户端连接数

# ========== 多线程IO (Redis 6.0+) ==========
io-threads 4                # IO线程数（不超过CPU核数）
io-threads-do-reads yes     # 多线程读取

# ========== 慢日志 ==========
slowlog-log-slower-than 10000  # 慢查询阈值(微秒)，即10ms
slowlog-max-len 128           # 最多保留128条慢日志

# ========== 延迟监控 ==========
latency-monitor-threshold 100  # 延迟监控阈值(毫秒)
```

### 4.2 Redis安全加固

```bash
# 1. 禁止危险命令（通过rename实现）
redis-cli CONFIG SET rename-command FLUSHALL ""
redis-cli CONFIG SET rename-command FLUSHDB ""
redis-cli CONFIG SET rename-command DEBUG ""

# 2. 设置ACL用户（Redis 6.0+）
# 编辑redis.conf添加:
# user default on >Redis@2026Secure! ~* &* -@dangerous +@read +@write

# 3. 绑定本地地址（防止外部直接访问）
# bind 127.0.0.1 ::1

# 4. 验证密码连接
redis-cli -h localhost -p 6379 -a 'Redis@2026Secure!' ping
```

---

## 第五部分：Nginx配置

### 5.1 反向代理配置示例

基于项目实际的 [nginx配置](deploy/nginx/beijixing-ai.conf)：

```nginx
# ==============================================
# 北极星AI - Nginx反向代理配置
# 文件路径: /etc/nginx/conf.d/beijixing-ai.conf
# ==============================================

# HTTP -> HTTPS 重定向（启用SSL后取消注释）
# server {
#     listen 80;
#     server_name www.beijixing-ai.com beijixing-ai.com;
#     return 301 https://$host$request_uri;
# }

server {
    listen 80;
    server_name www.beijixing-ai.com beijixing-ai.com 43.160.237.122;

    # ====== 管理后台前端 ======
    location /admin/ {
        alias /opt/beijixing-ai/frontend/web-admin/dist/;
        index index.html;
        try_files $uri $uri/ /admin/index.html;

        location ~* \.(html|css|js|png|jpg|jpeg|gif|ico|svg|woff2?)$ {
            expires 7d;
            add_header Cache-Control "public, immutable";
        }
    }

    # ====== PC端前端（SPA路由）======
    location / {
        root /opt/beijixing-ai/frontend/web-pc/dist;
        index index.html;
        try_files $uri $uri/ /index.html;

        location ~* \.(html|css|js|png|jpg|jpeg|gif|ico|svg|woff2?)$ {
            expires 7d;
            add_header Cache-Control "public, immutable";
        }
    }

    # ====== API反向代理 ======
    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_request_buffering off;

        # 超时设置
        proxy_connect_timeout 30s;
        proxy_send_timeout 60s;
        proxy_read_timeout 120s;  # AI接口可能较慢

        # 大文件上传支持
        client_max_body_size 100m;

        # API响应禁用缓存
        add_header Cache-Control "no-store, no-cache, must-revalidate";
        add_header Pragma "no-cache";
        Expires 0;
    }

    # ====== Actuator监控端点（仅允许内网访问）======
    location /actuator/ {
        access_log off;
        allow 127.0.0.1;
        deny all;
        proxy_pass http://127.0.0.1:8080/actuator/;
    }

    # ====== WebSocket代理（消息推送）======
    location /ws/message {
        proxy_pass http://127.0.0.1:8080/ws/message;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 86400;
    }

    # ====== Gzip压缩 ======
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_comp_level 4;
    gzip_types text/plain text/css application/json application/javascript
               text/xml application/xml application/xml+rss text/javascript
               image/svg+xml;

    # ====== 安全头 ======
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # ====== 访问日志 ======
    access_log /var/log/nginx/beijixing-ai-access.log;
    error_log /var/log/nginx/beijixing-ai-error.log;
}
```

### 5.2 HTTPS配置

```nginx
# ==============================================
# HTTPS配置（使用Let's Encrypt证书后自动生成）
# 文件路径: /etc/nginx/conf.d/beijixing-ai-ssl.conf
# ==============================================

server {
    listen 443 ssl http2;
    server_name www.beijixing-ai.com beijixing-ai.com;

    # SSL证书路径（Let's Encrypt自动生成）
    ssl_certificate /etc/letsencrypt/live/beijixing-ai.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/beijixing-ai.com/privkey.pem;

    # SSL优化配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 1d;
    ssl_session_tickets off;

    # HSTS（HTTP严格传输安全）
    add_header Strict-Transport-Security "max-age=63072000" always;

    # 其余location配置同上...
    # （将上面的location块复制到这里即可）
}

# HTTP强制跳转HTTPS
server {
    listen 80;
    server_name www.beijixing-ai.com beijixing-ai.com;
    return 301 https://$host$request_uri;
}
```

---

## 第六部分：监控与日志

### 6.1 日志查看命令

```bash
# ====== 实时查看应用日志 ======
tail -f /opt/beijixing-ai/logs/app.log

# ====== 搜索错误日志 ======
grep ERROR /opt/beijixing-ai/logs/app.log | tail -100

# ====== 搜索特定关键词 ======
grep -i "exception\|error\|failed" /opt/beijixing-ai/logs/app.log | tail -50

# ====== 查看启动状态 ======
grep "Started BeijixingAiApplication" /opt/beijixing-ai/logs/app.log

# ====== 查看最近N行日志 ======
tail -500 /opt/beijixing-ai/logs/app.log

# ====== 按时间过滤日志 ======
grep "2026-05-21 14:" /opt/beijixing-ai/logs/app.log

# ====== 查看GC日志 ======
tail -f /opt/beijixing-ai/logs/gc.log

# ====== Systemd日志（如果使用了systemd）=====
journalctl -u beijixing-ai -f                    # 实时
journalctl -u beijixing-ai --since "1 hour ago"  # 最近1小时
journalctl -u beijixing-ai -p err                 # 仅ERROR级别
```

### 6.2 常用运维命令

```bash
# ====== 进程管理 ======

# 查看Java进程
ps aux | grep beijixing-app | grep -v grep
# 或
jps -l

# 查看进程详细信息
ps -ef | grep java | grep -v grep

# 查看进程树
pstree -p $(cat /opt/beijixing-ai/logs/app.pid)

# ====== 端口检查 ======

# 检查8080端口是否在监听
ss -tlnp | grep 8080
# 或
netstat -tlnp | grep 8080

# 检查所有应用端口
ss -tlnp | grep -E "8080|6379|3306"

# ====== 健康检查 ======

# Actuator健康检查
curl -s http://localhost:8080/actuator/health | python3 -m json.tool

# Actuator信息
curl -s http://localhost:8080/actuator/info

# Prometheus指标
curl -s http://localhost:8080/actuator/prometheus

# 业务接口健康检查
curl -s http://localhost:8080/api/user/health

# ====== 内存分析 ======

# 查看JVM概览（需要PID）
jinfo $(cat /opt/beijixing-ai/logs/app.pid)

# 查看堆内存使用情况
jstat -gc $(cat /opt/beijixing-ai/logs/app.pid) 1000  # 每1秒刷新

# 查看堆对象直方图（排查内存泄漏）
jmap -histo $(cat /opt/beijixing-ai/logs/app.pid) | head -30

# 导出堆转储（分析OOM）
jmap -dump:format=b,file=/opt/beijixing-ai/logs/heap-$(date +%s).hprof $(cat /opt/beijixing-ai/logs/app.pid)

# 查看线程栈（排查死锁/CPU高占用）
jstack $(cat /opt/beijixing-ai/logs/app.pid) > /opt/beijixing-ai/logs/thread-dump-$(date +%s).txt

# ====== 系统资源监控 ======

# CPU和内存使用
top -p $(cat /opt/beijixing-ai/logs/app.pid)

# 磁盘使用
df -h /opt/beijixing-ai/

# 磁盘inode使用
df -i /opt/beijixing-ai/

# ====== 快速重启（不使用systemd时）======

# 优雅停止并重启
kill $(cat /opt/beijixing-ai/logs/app.pid) && sleep 5 && ./start-app.sh

# 强制重启
kill -9 $(cat /opt/beijixing-ai/logs/app.pid); sleep 2; ./start-app.sh
```

### 6.3 性能监控

#### 6.3.1 Spring Boot Actuator端点

本项目已内置Actuator，暴露以下端点（生产环境）：

| 端点 | 路径 | 说明 | 敏感 |
|------|------|------|------|
| Health | `/actuator/health` | 应用健康状态（含DB/Redis检测） | 否 |
| Info | `/actuator/info` | 应用元信息 | 否 |
| Metrics | `/actuator/metrics` | JVM/应用指标 | 部分 |
| Prometheus | `/actuator/prometheus` | Prometheus格式指标 | 否 |

**Health端点输出示例**：

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "MariaDB", "result": 1 } },
    "redis": { "status": "UP", "details": { "result": "PONG" } },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

#### 6.3.2 Prometheus + Grafana 监控方案（可选增强）

```yaml
# prometheus.yml 添加抓取目标
scrape_configs:
  - job_name: 'beijixing-ai'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
    static_configs:
      - targets: ['localhost:8080']
```

**关键监控指标**：

| 指标名 | PromQL | 说明 |
|--------|--------|------|
| JVM堆使用率 | `jvm_memory_used_bytes{area="heap"}` | 堆内存使用量 |
| GC次数 | `jvm_gc_collection_seconds_count` | GC发生频率 |
| GC耗时 | `jvm_gc_collection_seconds_sum` | GC累计耗时 |
| HTTP请求量 | `http_server_requests_seconds_count` | 接口调用统计 |
| HTTP响应时间 | `http_server_requests_seconds_max` | 接口响应延迟 |
| 线程数 | `jvm_threads_live_threads` | 当前活跃线程 |
| 数据库连接池 | `hikaricp_connections_active` | HikariCP活跃连接 |

---

## 第七部分：故障排查

### 7.1 常见问题及解决方案

#### 问题1: 应用启动失败 - Bean定义冲突

**症状**: 启动日志中出现 `The bean 'xxx' could not be registered...`

**原因**: 微服务合并为单体架构后，多个模块可能定义了同名Bean。

**解决方案**:

```bash
# 确认启动参数中包含以下配置
-Dspring.main.allow-bean-definition-overriding=true
-Dspring.main.allow-circular-references=true
```

这两个参数已在标准启动命令中默认包含。

#### 问题2: Mapper扫描不到XML文件

**症状**: `Invalid bound statement (not found)` 或 `Mapped Statements collection does not contain value for xxx`

**原因**: MyBatis-Plus的Mapper XML文件未被正确加载。

**解决方案**:

```bash
# 确认启动参数包含
-Dmybatis-plus.mapper-locations=classpath*:/mapper/**/*.xml
-Dmybatis-plus.type-aliases-package=com.beijixing.*.entity

# 确认JAR包中包含XML文件（shade插件可能遗漏资源）
jar tf beijixing-app.jar | grep mapper
```

#### 问题3: 数据库连接失败

**症状**: `Could not create connection to database server` 或 `Access denied`

**排查步骤**:

```bash
# 1. 检查MariaDB服务状态
systemctl status mariadb

# 2. 手动测试数据库连接
mysql -h localhost -u root -p'Beijixing@2024!' -e "SELECT 1"

# 3. 检查数据库是否存在
mysql -u root -p -e "SHOW DATABASES LIKE 'beijixing_ai'"

# 4. 检查JDBC URL参数
# 确保URL中包含: useUnicode=true&characterEncoding=utf8&allowPublicKeyRetrieval=true

# 5. 检查防火墙是否阻止了3306端口（如果是远程数据库）
ss -tlnp | grep 3306
```

#### 问题4: Redis连接失败

**症状**: `Unable to connect to Redis` 或 `Connection refused`

**排查步骤**:

```bash
# 1. 检查Redis服务状态
systemctl status redis
redis-cli ping

# 2. 使用密码测试连接
redis-cli -a 'Redis@2026Secure!' ping

# 3. 检查Redis绑定地址
redis-cli CONFIG GET bind
# 应该显示: 127.0.0.1 或 0.0.0.0

# 4. 检查Redis密码是否正确
# 对比 application-prod.yml 中的 spring.data.redis.password 与 redis.conf 中的 requirepass
```

#### 问题5: OOM (Out of Memory)

**症状**: `java.lang.OutOfMemoryError: Java heap space` 或应用被系统Kill

**解决方案**:

```bash
# 1. 查看OOM时的堆转储文件
ls -la /opt/beijixing-ai/logs/heapdump*.hprof

# 2. 增加堆内存
# 修改 -Xmx 参数，例如从 1024m 增加到 1536m

# 3. 分析堆转储（使用Eclipse MAT或VisualVM）
# 下载: https://eclipse.org/mat/downloads/

# 4. 检查是否有内存泄漏
jmap -histo:live $(cat app.pid) | head -20
```

#### 问题6: CPU占用过高

**症状**: `top` 显示Java进程CPU持续90%+

**排查步骤**:

```bash
# 1. 查看哪个线程占用CPU最高
top -H -p $(cat app.pid)

# 2. 将线程ID转为十六进制
printf "%x\n" <THREAD_ID>

# 3. 查看该线程栈
jstack $(cat app.pid) | grep -A 30 "<HEX_THREAD_ID>"

# 4. 常见原因及对策:
#    - GC频繁 -> 调整G1GC参数，增加堆内存
#    - 死循环 -> jstack定位具体代码位置
#    - 大量线程等待 -> 检查线程池配置
```

#### 问题7: 端口被占用

**症状**: `Address already in use: bind (Bind failed)` 或 `Port 8080 was already in use`

**解决方案**:

```bash
# 1. 查找占用端口的进程
ss -tlnp | grep 8080
# 或
lsof -i :8080

# 2. 杀掉占用进程
kill -9 <PID>

# 3. 或者更换端口
# 修改 application.yml 中 server.port 为其他值（如 8081）
```

### 7.2 应急处理流程

#### 服务降级操作

```bash
# 当服务器负载过高时，临时降低服务质量以保持可用性

# 1. 减少Tomcat线程数
# 通过环境变量覆盖:
-Dserver.tomcat.threads.max=50

# 2. 禁用非核心功能（如有开关）
# - AI内容生成: 暂停定时任务
# - 大批量导出: 临时关闭

# 3. 增加GC压力
# 降低堆内存上限，让GC更频繁但单次停顿更短

# 4. 临时扩容
# 如果是云服务器，临时升级配置
```

#### 数据备份恢复

```bash
# ====== 全量备份 ======
mysqldump -u root -p'Beijixing@2024!' \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  beijixing_ai > /opt/beijixing-ai/backup/db-full-$(date +%Y%m%d).sql

# ====== 增量备份（基于Binlog）======
# 查看当前binlog位置
mysql -u root -p -e "SHOW MASTER STATUS;"

# ====== 恢复数据库 ======
mysql -u root -p'Beijixing@2024!' beijixing_ai < /opt/beijixing-ai/backup/db-full-20260521.sql

# ====== Redis备份恢复 ======
# 备份RDB文件（Redis运行时会自动生成）
cp /var/lib/redis/dump.rdb /opt/beijixing-ai/backup/redis-dump-$(date +%Y%m%d).rdb

# 恢复Redis（停止Redis后替换RDB文件再启动）
systemctl stop redis
cp /opt/beijixing-ai/backup/redis-dump-20260521.rdb /var/lib/redis/dump.rdb
chown redis:redis /var/lib/redis/dump.rdb
systemctl start redis
```

#### 回滚部署

```bash
# ====== 应用回滚 ======

# 1. 查看可用备份版本
ls -lt /opt/beijixing-ai/backup/

# 2. 停止当前服务
systemctl stop beijixing-ai

# 3. 恢复备份版本
cp /opt/beijixing-ai/backup/beijixing-app-20260520143000.jar \
   /opt/beijixing-ai/backend/beijixing-app/beijixing-app.jar

# 4. 重启服务
systemctl start beijixing-ai

# 5. 验证回滚成功
sleep 15
curl -s http://localhost:8080/actuator/health
```

---

## 第八部分：安全加固

### 8.1 系统安全

#### SSH安全配置

```bash
# 编辑SSH配置
sudo vi /etc/ssh/sshd_config

# 关键配置项:
Port 22                          # 可改为非常规端口（如2222）
PermitRootLogin prohibit-password # 禁止root密码登录（仅允许密钥）
PasswordAuthentication no         # 禁止密码登录（推荐密钥认证）
PubkeyAuthentication yes          # 允许密钥认证
MaxAuthTries 3                    # 最大尝试次数
LoginGraceTime 30                 # 登录超时时间

# 重启SSH服务
sudo systemctl restart sshd
```

#### 防火墙配置

```bash
# ====== UFW基本规则 ======
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow from YOUR_TRUSTED_IP to any port 22    # SSH仅允许信任IP
sudo ufw allow 80/tcp                                  # HTTP
sudo ufw allow 443/tcp                                 # HTTPS
sudo ufw enable

# ====== 内部服务端口禁止外部访问 ======
# MySQL (3306) 和 Redis (6379) 必须只绑定127.0.0.1
```

#### fail2ban防暴力破解

```bash
# 安装fail2ban
sudo apt install -y fail2ban

# 创建本地配置
sudo tee /etc/fail2ban/jail.local << 'EOF'
[DEFAULT]
bantime = 3600
findtime = 600
maxretry = 5

[sshd]
enabled = true
port = ssh
filter = sshd
logpath = /var/log/auth.log
maxretry = 3
bantime = 86400
EOF

# 启动fail2ban
sudo systemctl enable fail2ban
sudo systemctl start fail2ban

# 查看被封禁的IP
sudo fail2ban-client status sshd
```

### 8.2 应用安全

#### JWT密钥轮换

```bash
# JWT配置位于 application.yml 的 jwt.secret 字段
# 当前值: ${JWT_SECRET:BeijixingAiJwtSecretKey2024!@#$}

# 轮换步骤:
# 1. 生成新的强随机密钥
NEW_SECRET=$(openssl rand -base64 48)
echo "新JWT密钥: $NEW_SECRET"

# 2. 更新配置文件或环境变量
export JWT_SECRET="$NEW_SECRET"

# 3. 重启应用
systemctl restart beijixing-ai

# 注意: 所有已签发的Token将失效，用户需要重新登录
```

#### SQL注入防护

本项目已通过以下机制防护SQL注入：
- **MyBatis-Plus**: 使用参数化查询（`#{}`语法），天然防止SQL注入
- **MyBatis-Plus全局配置**: `map-underscore-to-camel-case: true` 自动映射
- **输入校验**: Spring Validation注解（`@NotNull`, `@Size`, `@Pattern`等）

> **警告**: 禁止在MyBatis XML中使用 `${}` 拼接用户输入，这会导致SQL注入漏洞！

#### XSS防护

```yaml
# application.yml 中已配置Jackson序列化安全
spring:
  jackson:
    default-property-inclusion: non_null  # 不序列化null值，减少攻击面
```

前端配合措施：
- 输入内容转义（Vue.js默认自动转义）
- CSP（Content Security Policy）头部配置
- Cookie设置 `HttpOnly; Secure; SameSite=Strict`

---

## 第九部分：备份与恢复

### 9.1 数据库备份策略

#### 全量备份脚本

```bash
#!/bin/bash
# ============================================================
# 北极星AI - 数据库自动备份脚本
# 放置于: /opt/beijixing-ai/scripts/backup-db.sh
# 定时任务: crontab -e → 0 2 * * * /opt/beijixing-ai/scripts/backup-db.sh
# ============================================================

BACKUP_DIR="/opt/beijixing-ai/backup/database"
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30

# 创建备份目录
mkdir -p ${BACKUP_DIR}

# 全量备份
mysqldump -u root -p'Beijixing@2024!' \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  --hex-blob \
  beijixing_ai | gzip > ${BACKUP_DIR}/beijixing_ai_full_${DATE}.sql.gz

# 清理过期备份（保留最近30天）
find ${BACKUP_DIR} -name "*.sql.gz" -mtime +${RETENTION_DAYS} -delete

# 记录备份日志
echo "[$(date '+%Y-%m-%d %H:%M:%S')] 数据库备份完成: beijixing_ai_full_${DATE}.sql.gz ($(du -sh ${BACKUP_DIR}/beijixing_ai_full_${DATE}.sql.gz | cut -f1))" >> ${BACKUP_DIR}/backup.log

# 备份验证（检查文件完整性）
if [ -f "${BACKUP_DIR}/beijixing_ai_full_${DATE}.sql.gz" ]; then
    echo "备份验证通过"
else
    echo "备份失败!" | mail -s "数据库备份失败告警" admin@beijixing.com
fi
```

#### 定时备份配置

```bash
# 编辑crontab
crontab -e

# 添加以下定时任务:
# 每天凌晨2点全量备份
0 2 * * * /opt/beijixing-ai/scripts/backup-db.sh >> /opt/beijixing-ai/backup/cron.log 2>&1

# 每6小时增量备份Binlog
0 */6 * * * mysqladmin -u root -p'Beijixing@2024!' flush-logs && cp /var/log/mysql/mysql-bin.* /opt/beijixing-ai/backup/binlog/

# 每周日清理旧备份
0 3 * * 0 find /opt/beijixing-ai/backup/database -name "*.sql.gz" -mtime +30 -delete
```

### 9.2 文件备份

```bash
# ====== 上传文件备份 ======
# 用户上传的文件存储于 /opt/beijixing-ai/uploads/

# 本地备份
tar czf /opt/beijixing-ai/backup/uploads-$(date +%Y%m%d).tar.gz \
  -C /opt/beijixing-ai uploads/

# ====== 云存储备份（推荐生产环境使用）======
# 腾讯云COS / 阿里云OSS 同步脚本（需配置SDK工具）
# coscmd upload -rs /opt/beijixing-ai/uploads/ /bucket/backups/uploads/

# ====== 配置文件备份 ======
tar czf /opt/beijixing-ai/backup/config-$(date +%Y%m%d).tar.gz \
  /opt/beijixing-ai/config/ \
  /etc/nginx/conf.d/beijixing-ai.conf \
  /etc/redis/redis.conf \
  /etc/my.cnf.d/
```

### 9.3 恢复演练

#### 数据库恢复步骤

```bash
#!/bin/bash
# ============================================================
# 北极星AI - 数据库恢复脚本
# 使用方法: ./restore-db.sh <备份文件路径>
# ============================================================

BACKUP_FILE=$1
DB_NAME="beijixing_ai"
DB_USER="root"
DB_PASS="Beijixing@2024!"

if [ -z "$BACKUP_FILE" ]; then
    echo "用法: $0 <备份文件.sql.gz>"
    exit 1
fi

echo "[WARNING] 此操作将覆盖现有数据库!"
read -p "确认继续? (yes/no): " confirm
if [ "$confirm" != "yes" ]; then
    echo "操作已取消"
    exit 0
fi

# 1. 停止应用（避免写入冲突）
systemctl stop beijixing-ai

# 2. 备份当前数据库（以防万一）
mysqldump -u ${DB_USER} -p"${DB_PASS}" ${DB_NAME} > /tmp/pre-restore-backup-$(date +%s).sql

# 3. 删除并重建数据库
mysql -u ${DB_USER} -p"${DB_PASS}" -e "DROP DATABASE IF EXISTS ${DB_NAME}; CREATE DATABASE ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 4. 恢复数据
if [[ $BACKUP_FILE == *.gz ]]; then
    gunzip -c ${BACKUP_FILE} | mysql -u ${DB_USER} -p"${DB_PASS}" ${DB_NAME}
else
    mysql -u ${DB_USER} -p"${DB_PASS}" ${DB_NAME} < ${BACKUP_FILE}
fi

# 5. 验证恢复结果
TABLE_COUNT=$(mysql -u ${DB_USER} -p"${DB_PASS}" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DB_NAME}';")
echo "[INFO] 恢复完成，共 ${TABLE_COUNT} 张表"

# 6. 重启应用
systemctl start beijixing-ai

echo "[INFO] 数据库恢复完成，请验证应用功能正常"
```

---

## 第十部分：附录

### 10.1 环境变量清单

| 变量名 | 默认值 | 说明 | 敏感 |
|--------|-------|------|------|
| `DB_HOST` | localhost | 数据库主机地址 | 否 |
| `DB_PORT` | 3306 | 数据库端口 | 否 |
| `DB_NAME` | beijixing_ai | 数据库名称 | 否 |
| `DB_USER` | root | 数据库用户名 | 是 |
| `DB_PASSWORD` | (空) | 数据库密码 | **是** |
| `REDIS_HOST` | localhost | Redis主机地址 | 否 |
| `REDIS_PORT` | 6379 | Redis端口 | 否 |
| `REDIS_PASSWORD` | (空) | Redis密码 | **是** |
| `JWT_SECRET` | (空) | JWT签名密钥 | **是** |
| `SMTP_HOST` | smtp.qq.com | SMTP邮件服务器 | 否 |
| `SMTP_USERNAME` | (空) | SMTP用户名 | 是 |
| `SMTP_PASSWORD` | (空) | SMTP密码 | **是** |
| `VOLCENGINE_API_KEY` | (空) | 火山引擎API Key | **是** |
| `DEEPSEEK_API_KEY` | (空) | DeepSeek API Key | **是** |
| `TENCENT_SECRET_ID` | (空) | 腾讯云SecretId | **是** |
| `TENCENT_SECRET_KEY` | (空) | 腾讯云SecretKey | **是** |
| `AI_PROVIDER_TYPE` | volcano | AI模型供应商类型 | 否 |

### 10.2 配置文件模板

#### 生产环境 `.env.prod` 文件模板

```bash
# ============================================================
# 北极星AI - 生产环境变量文件
# 文件位置: /opt/beijixing-ai/.env.prod
# 使用方法: source /opt/beijixing-ai/.env.prod
# 重要: 此文件包含敏感信息，权限设置为600！
# ============================================================

# ---- 环境标识 ----
ENV=prod

# ---- 数据库配置 (MariaDB) ----
DB_HOST=localhost
DB_PORT=3306
DB_NAME=beijixing_ai
DB_USER=root
DB_PASSWORD=Beijixing@2024!

# ---- Redis配置 ----
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=Redis@2026Secure!

# ---- JWT配置 ----
JWT_SECRET=<YOUR_STRONG_JWT_SECRET_HERE>

# ---- AI模型配置 ----
AI_PROVIDER_TYPE=volcano
VOLCENGINE_API_KEY=<YOUR_VOLCENGINE_KEY>
DEEPSEEK_API_KEY=<YOUR_DEEPSEEK_KEY>

# ---- 邮件配置 ----
SMTP_HOST=smtp.qq.com
SMTP_PORT=465
SMTP_USERNAME=admin@beijixing.com
SMTP_PASSWORD=<YOUR_SMTP_PASSWORD>

# ---- 腾讯云短信 ----
TENCENT_SECRET_ID=<YOUR_TENCENT_SECRET_ID>
TENCENT_SECRET_KEY=<YOUR_TENCENT_SECRET_KEY>

# ---- JVM参数 ----
JAVA_XMS=512m
JAVA_XMX=1024m
```

### 10.3 常用SQL查询

```sql
-- ==============================================
-- 北极星AI - 常用运维SQL查询
-- ==============================================

-- 1. 查看数据库大小
SELECT
  table_schema AS '数据库',
  ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS '大小(MB)',
  COUNT(*) AS '表数量'
FROM information_schema.tables
WHERE table_schema = 'beijixing_ai'
GROUP BY table_schema;

-- 2. 查看最大的10张表
SELECT
  table_name,
  ROUND(data_length / 1024 / 1024, 2) AS data_mb,
  ROUND(index_length / 1024 / 1024, 2) AS index_mb,
  table_rows
FROM information_schema.tables
WHERE table_schema = 'beijixing_ai'
ORDER BY data_length DESC
LIMIT 10;

-- 3. 查看当前连接数
SHOW STATUS LIKE 'Threads_connected';
SHOW PROCESSLIST;

-- 4. 查看慢查询
SELECT * FROM mysql.slow_log ORDER BY start_time DESC LIMIT 20;

-- 5. 用户相关查询
-- 查看所有用户
SELECT id, username, nickname, phone, status, create_time
FROM sys_user
ORDER BY create_time DESC
LIMIT 20;

-- 查看管理员用户
SELECT u.id, u.username, r.role_name
FROM sys_user u
LEFT JOIN sys_user_role ur ON u.id = ur.user_id
LEFT JOIN sys_role r ON ur.role_id = r.id
WHERE r.role_code = 'ROLE_ADMIN';

-- 6. 租户相关查询
-- 查看租户列表
SELECT id, tenant_name, package_type, status, expire_time,
  CASE WHEN expire_time < NOW() THEN '已过期' ELSE '正常' END AS 租户状态
FROM bx_tenant
ORDER BY create_time DESC;

-- 7. 商机/线索统计
SELECT
  DATE(create_time) AS 日期,
  COUNT(*) AS 新增线索数,
  SUM(CASE WHEN status = 'CONVERTED' THEN 1 ELSE 0 END) AS 已转化,
  SUM(CASE WHEN status = 'NEW' THEN 1 ELSE 0 END) AS 待跟进
FROM bx_lead
WHERE create_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY DATE(create_time)
ORDER BY 日期 DESC;

-- 8. 系统操作日志（最近异常操作）
SELECT *
FROM sys_oper_log
WHERE status = 1  -- 1表示异常
ORDER BY oper_time DESC
LIMIT 20;

-- 9. InnoDB引擎状态检查
SHOW ENGINE INNODB STATUS\G

-- 10. 表锁检测
SHOW OPEN TABLES WHERE In_use > 0;
```

---

## 快速参考卡片

### 一键部署清单

```bash
# ====== 全新服务器一键部署 ======

# 1. 安装基础软件
apt update && apt install -y openjdk-17-jdk mariadb-server redis-server nginx maven git

# 2. 配置数据库
mysql_secure_installation
mysql -u root -p < init-database.sql

# 3. 配置Redis（设置密码）
sed -i 's/^# requirepass.*/requirepass Redis@2026Secure!/' /etc/redis/redis.conf
systemctl restart redis

# 4. 创建应用目录
mkdir -p /opt/beijixing-ai/{backend/beijixing-app,logs,config,backup,uploads,frontend/{web-admin/dist,web-pc/dist}}

# 5. 上传JAR包
scp beijixing-app.jar root@43.160.237.122:/opt/beijixing-ai/backend/beijixing-app/

# 6. 配置systemd服务
cp beijixing-ai.service /etc/systemd/system/
systemctl daemon-reload && systemctl enable beijixing-ai

# 7. 启动应用
systemctl start beijixing-ai

# 8. 验证部署
curl http://localhost:8080/actuator/health
```

### 关键端口一览

| 端口 | 服务 | 用途 | 外网访问 |
|------|------|------|---------|
| 22 | SSH | 远程管理 | 是（限制IP） |
| 80 | Nginx | HTTP访问 | 是 |
| 443 | Nginx | HTTPS访问 | 是 |
| 8080 | Spring Boot | 应用API | 否（经Nginx代理） |
| 3306 | MariaDB | 数据库 | **否**（仅本地） |
| 6379 | Redis | 缓存 | **否**（仅本地） |
| 9999 | JMX | JVM监控 | **否**（仅本地） |

### 目录结构

```
/opt/beijixing-ai/
├── backend/beijixing-app/
│   └── beijixing-app.jar          # 应用JAR包
├── logs/
│   ├── app.log                     # 应用日志
│   ├── app-error.log               # 错误日志
│   ├── gc.log                      # GC日志
│   └── heapdump*.hprof             # OOM堆转储
├── config/
│   └── application.yml             # 自定义配置覆盖
├── backup/
│   ├── database/                   # 数据库备份
│   ├── binlog/                     # Binlog备份
│   └── *.jar                       # 版本备份
├── uploads/                        # 用户上传文件
└── frontend/
    ├── web-admin/dist/             # 管理后台前端
    └── web-pc/dist/                # PC端前端
```

---

> **文档维护说明**: 本文档基于实际部署经验编写，所有命令均经过验证。
> 如有疑问或发现错误，请联系运维团队更新本文档。
>
> **版本历史**:
> - v1.0 (2026-05-21): 初始版本，涵盖完整的部署、运维、监控、故障排查流程
