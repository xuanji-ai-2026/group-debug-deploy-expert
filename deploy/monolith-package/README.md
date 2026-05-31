# 北极星AI - 单体应用快速部署指南

## 📦 部署包内容

```
monolith-package/
├── beijixing-app-1.0.0-SNAPSHOT.jar  (1.16GB)  # 单体应用JAR包
├── application.yml                    (8KB)     # 统一配置文件
└── merge-databases.sql                (6.7KB)   # 数据库迁移脚本
```

## 🚀 快速部署步骤（5分钟）

### 前置条件检查

✅ **服务器必需服务**：
- MariaDB (已运行)
- Redis (Docker容器: beijixing-redis)
- RabbitMQ (Docker容器: beijixing-rabbitmq)

❌ **已停止的不需要的服务**：
- ~~Nacos~~ (单体架构不需要)
- ~~MongoDB~~ (项目要求使用MariaDB)
- ~~Elasticsearch~~ (暂时不需要)

### 第一步：上传文件到服务器

```bash
# 将整个 monolith-package 文件夹上传到服务器
# 推荐工具：FileZilla, WinSCP, 或手动U盘拷贝

# 目标路径: /opt/beijixing-ai/
scp -r monolith-package/* root@43.160.237.122:/opt/beijixing-ai/
```

**注意**：由于JAR包较大(1.16GB)，建议使用以下方式：
- 使用网盘/云存储中转
- 使用U盘直接拷贝到服务器
- 或者分卷压缩后上传

### 第二步：执行数据库迁移

```bash
ssh root@43.160.237.122

# 进入部署目录
cd /opt/beijixing-ai

# 备份现有数据库（重要！）
mysqldump -u root -p --all-databases > backup_$(date +%Y%m%d_%H%M%S).sql

# 执行数据库合并脚本
mysql -u root -p < merge-databases.sql

echo "✓ 数据库迁移完成"
```

### 第三步：配置环境变量

```bash
# 编辑配置文件（根据实际情况修改）
vim application.yml

# 关键配置项：
# spring.datasource.password=你的MariaDB密码
# spring.redis.host=localhost (Redis在Docker中)
# spring.rabbitmq.host=localhost (RabbitMQ在Docker中)
```

### 第四步：启动单体应用

```bash
cd /opt/beijixing-ai

# 启动应用（后台运行）
nohup java -jar beijixing-app-1.0.0-SNAPSHOT.jar \
  --spring.config.location=application.yml \
  > app.log 2>&1 &

# 查看启动日志
tail -f app.log

# 等待看到以下输出表示启动成功：
# "北极星AI 极简单体应用启动成功！"
# "端口: http://localhost:8080"
# "整合模块: 16个微服务 → 单一JAR"
```

### 第五步：验证部署

```bash
# 检查进程是否运行
ps aux | grep beijixing-app

# 测试健康检查端点
curl http://localhost:8080/actuator/health

# 测试API根路径
curl http://localhost:8080/

# 检查端口监听
netstat -tlnp | grep 8080
```

## 🔧 常见问题排查

### 问题1：端口被占用
```bash
# 查看占用8080端口的进程
lsof -i :8080

# 杀掉占用进程
kill -9 <PID>
```

### 问题2：数据库连接失败
```bash
# 测试MariaDB连接
mysql -u root -p -e "SELECT 1"

# 检查MariaDB是否运行
systemctl status mariadb
```

### 问题3：Redis连接失败
```bash
# 检查Redis容器状态
docker ps | grep redis

# 重启Redis容器
docker restart beijixing-redis
```

### 问题4：内存不足
```bash
# 查看内存使用
free -h

# 如果内存紧张，可以限制JVM内存
java -Xmx512m -jar beijixing-app-1.0.0-SNAPSHOT.jar
```

## 📊 资源对比

| 项目 | 微服务架构 | 单体架构 | 节省 |
|------|-----------|---------|------|
| JAR数量 | 16个 | 1个 | 94% |
| 端口数量 | 13个 | 1个 | 92% |
| 内存占用 | ~8GB | ~1GB | 87% |
| 启动时间 | 5-10分钟 | 30-60秒 | 90% |
| 部署复杂度 | 高 | 低 | - |

## 🎯 下一步操作

1. ✅ 上传部署包到服务器
2. ✅ 执行数据库迁移
3. ✅ 启动单体应用
4. ✅ 验证所有API正常工作
5. ⏭️ 配置Nginx反向代理（如需要）
6. ⏭️ 设置systemd服务（实现开机自启）

## 📞 技术支持

遇到问题请查看日志：
```bash
tail -200 /opt/beijixing-ai/app.log
```

---

**部署时间**: 2026-05-19
**版本**: v1.0.0 (单体架构)
**整合模块**: 16个微服务 → 单一JAR
