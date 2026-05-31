# 北极星AI商机获客系统 - Docker部署配置

**运维总监**: 叶宇 (EMP-OPS-001)

## 文件说明

| 文件 | 用途 | 环境 |
|------|------|------|
| `docker-compose.yml` | 基础配置 | 通用/测试 |
| `docker-compose.dev.yml` | 开发环境 | 本地开发 |
| `docker-compose.prod.yml` | 生产环境 | 生产部署 |

## 服务架构

### 基础设施层
- **MySQL 8.0** - 主数据库 (端口: 3306)
- **Redis 6.0** - 缓存与会话 (端口: 6379)
- **MongoDB 6.0** - 文档数据库 (端口: 27017)
- **RabbitMQ 3.11** - 消息队列 (端口: 5672/15672)
- **Elasticsearch 8.x** - 搜索引擎 (端口: 9200/9300)

### 业务服务层
| 服务 | 端口 | 说明 |
|------|------|------|
| Gateway | 8080 | API网关 |
| User | 8081 | 用户服务 |
| Tenant | 8082 | 租户服务 |
| Content | 8083 | 内容服务 |
| Lead | 8084 | 商机服务 |
| Risk | 8085 | 风控服务 |
| Billing | 8086 | 计费服务 |
| AI | 8087 | AI服务 |
| Message | 8088 | 消息服务 |
| Storage | 8089 | 存储服务 |

## 快速开始

### 1. 基础环境启动
```bash
# 使用基础配置启动
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f [service_name]
```

### 2. 开发环境启动
```bash
# 启动开发环境
docker-compose -f docker-compose.dev.yml up -d

# 特点:
# - 开启调试端口 (5005-5014)
# - 热重载支持
# - 简化密码配置
# - 包含Swagger UI文档
```

### 3. 生产环境启动
```bash
# 1. 准备secrets文件
mkdir -p secrets
echo "your_root_password" > secrets/mysql_root_password.txt
echo "your_password" > secrets/mysql_password.txt
# ... 其他secret文件

# 2. 设置应用版本 (可选)
export APP_VERSION=v1.0.0

# 3. 启动生产环境
docker-compose -f docker-compose.prod.yml up -d

# 特点:
# - 资源限制配置
# - 日志轮转
# - Secrets管理
# - 多副本支持
# - 监控集成
```

## 网络配置

| 环境 | 网络名称 | 子网 |
|------|----------|------|
| 基础 | beijixing-network | 172.20.0.0/16 |
| 开发 | beijixing-network-dev | 172.21.0.0/16 |
| 生产 | beijixing-network-prod | 172.22.0.0/16 |

## 健康检查

所有服务均配置了健康检查:
- **间隔**: 30秒
- **超时**: 10秒
- **重试**: 5次
- **启动宽限期**: 30-60秒

## 数据持久化

数据卷挂载位置:
```
/var/lib/docker/volumes/
├── beijixing-ai_mysql_data[_dev|_prod]
├── beijixing-ai_redis_data[_dev|_prod]
├── beijixing-ai_mongodb_data[_dev|_prod]
├── beijixing-ai_rabbitmq_data[_dev|_prod]
├── beijixing-ai_elasticsearch_data[_dev|_prod]
├── beijixing-ai_storage_data[_dev|_prod]
└── beijixing-ai_ai_models[_dev|_prod]
```

## 常用命令

```bash
# 启动所有服务
docker-compose up -d

# 停止所有服务
docker-compose down

# 停止并删除数据卷 (慎用!)
docker-compose down -v

# 重启单个服务
docker-compose restart [service_name]

# 查看服务日志
docker-compose logs -f [service_name]

# 进入容器
docker-compose exec [service_name] /bin/sh

# 扩展服务副本 (仅生产环境)
docker-compose -f docker-compose.prod.yml up -d --scale lead=3
```

## 安全注意事项

1. **生产环境务必修改默认密码**
2. **使用Docker Secrets管理敏感信息**
3. **限制对外暴露的端口**
4. **定期备份数据卷**
5. **启用防火墙规则**

## 故障排查

```bash
# 检查服务状态
docker-compose ps

# 检查资源使用
docker stats

# 检查网络连接
docker network inspect beijixing-network

# 查看容器详情
docker-compose config
```

## 联系信息

如有问题请联系运维团队: **叶宇 (EMP-OPS-001)**