# 北极星AI商机获客系统 - 部署指南

## 1. 环境要求

### 1.1 硬件要求

| 组件 | 最低配置 | 推荐配置 |
|------|----------|----------|
| CPU | 4核 | 8核 |
| 内存 | 8GB | 16GB |
| 硬盘 | 100GB | 200GB |

### 1.2 软件要求

| 软件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | Java运行环境 |
| Maven | 3.8+ | 构建工具 |
| Docker | 20.10+ | 容器环境 |
| Kubernetes | 1.20+ | 容器编排 |
| MySQL | 8.0+ | 数据库 |
| Redis | 7.0+ | 缓存 |
| Nginx | 1.20+ | 反向代理 |

## 2. 部署架构

### 2.1 开发环境

```
本地开发环境
├── 本地数据库
├── 本地Redis
├── 本地代码仓库
└── IDE开发工具
```

### 2.2 测试环境

```
Docker环境
├── MySQL容器
├── Redis容器
├── 应用容器
└── Nginx容器
```

### 2.3 生产环境

```
Kubernetes集群
├── 多副本部署
├── 负载均衡
├── 自动扩缩容
└── 高可用架构
```

## 3. 部署步骤

### 3.1 使用Docker Compose部署（开发/测试环境）

#### 3.1.1 克隆代码
```bash
git clone https://github.com/your-org/beijixing-ai.git
cd beijixing-ai
```

#### 3.1.2 构建镜像
```bash
docker-compose build
```

#### 3.1.3 启动服务
```bash
docker-compose up -d
```

#### 3.1.4 查看日志
```bash
docker-compose logs -f
```

#### 3.1.5 停止服务
```bash
docker-compose down
```

### 3.2 使用Kubernetes部署（生产环境）

#### 3.2.1 创建命名空间
```bash
kubectl create namespace beijixing-ai
```

#### 3.2.2 创建ConfigMap
```bash
kubectl apply -f deploy/k8s/configmap.yaml -n beijixing-ai
```

#### 3.2.3 创建Secret
```bash
kubectl apply -f deploy/k8s/secret.yaml -n beijixing-ai
```

#### 3.2.4 部署服务
```bash
kubectl apply -f deploy/k8s/services/ -n beijixing-ai
```

#### 3.2.5 配置Ingress
```bash
kubectl apply -f deploy/k8s/ingress/ -n beijixing-ai
```

#### 3.2.6 查看部署状态
```bash
kubectl get pods -n beijixing-ai
kubectl get svc -n beijixing-ai
```

## 4. 配置说明

### 4.1 数据库配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/beijixing_ai?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

### 4.2 Redis配置

```yaml
spring:
  redis:
    host: redis
    port: 6379
    password: your_password
    database: 0
```

### 4.3 AI服务配置

```yaml
ai:
  volcano:
    api-key: your_api_key
    api-secret: your_api_secret
```

## 5. 监控告警

### 5.1 Prometheus监控

访问地址: `http://your-domain:9090`

监控指标:
- 系统资源使用率
- 服务QPS
- 响应时间
- 错误率

### 5.2 Grafana看板

访问地址: `http://your-domain:3000`

默认账号: `admin/admin`

看板:
- 系统概览
- 服务监控
- 业务指标

## 6. 备份恢复

### 6.1 数据库备份

```bash
# 备份
docker exec mysql mysqldump -uroot -p beijixing_ai > backup.sql

# 恢复
docker exec -i mysql mysql -uroot -p beijixing_ai < backup.sql
```

### 6.2 配置备份

```bash
# 备份ConfigMap
kubectl get configmap -n beijixing-ai -o yaml > configmap-backup.yaml

# 恢复ConfigMap
kubectl apply -f configmap-backup.yaml -n beijixing-ai
```

## 7. 故障排查

### 7.1 常见问题

#### 问题1: 服务无法启动
```bash
# 查看Pod日志
kubectl logs -f <pod-name> -n beijixing-ai

# 查看Pod状态
kubectl describe pod <pod-name> -n beijixing-ai
```

#### 问题2: 数据库连接失败
```bash
# 检查数据库服务
kubectl exec -it <mysql-pod> -n beijixing-ai -- mysql -uroot -p

# 检查网络连接
kubectl exec -it <app-pod> -n beijixing-ai -- ping mysql
```

#### 问题3: Redis连接超时
```bash
# 检查Redis服务
kubectl exec -it <redis-pod> -n beijixing-ai -- redis-cli

# 测试连接
kubectl exec -it <app-pod> -n beijixing-ai -- redis-cli -h redis ping
```

## 8. 性能优化

### 8.1 数据库优化

- 创建合适的索引
- 定期清理过期数据
- 启用查询缓存

### 8.2 应用优化

- 启用连接池
- 配置合理的线程池
- 使用缓存减少数据库访问

### 8.3 网络优化

- 启用HTTP/2
- 配置CDN加速
- 使用负载均衡

## 9. 安全加固

### 9.1 网络安全

- 配置防火墙规则
- 启用HTTPS
- 限制IP访问

### 9.2 应用安全

- 定期更新依赖
- 启用安全审计
- 配置访问控制

### 9.3 数据安全

- 启用数据加密
- 定期备份数据
- 配置数据脱敏

## 10. 升级回滚

### 10.1 滚动升级

```bash
# 更新镜像
kubectl set image deployment/<deployment-name> <container-name>=<new-image> -n beijixing-ai

# 查看升级状态
kubectl rollout status deployment/<deployment-name> -n beijixing-ai
```

### 10.2 快速回滚

```bash
# 回滚到上一个版本
kubectl rollout undo deployment/<deployment-name> -n beijixing-ai

# 回滚到指定版本
kubectl rollout undo deployment/<deployment-name> --to-revision=<revision> -n beijixing-ai
```

## 11. 附录

### 11.1 端口说明

| 服务 | 端口 | 说明 |
|------|------|------|
| 网关 | 8080 | API入口 |
| 用户服务 | 8081 | 用户管理 |
| 商机服务 | 8082 | 商机管理 |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| Nginx | 80 | Web服务 |

### 11.2 目录说明

| 目录 | 说明 |
|------|------|
| deploy/docker/ | Docker配置 |
| deploy/k8s/ | Kubernetes配置 |
| deploy/nginx/ | Nginx配置 |
| scripts/ | 部署脚本 |
