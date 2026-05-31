# 北极星AI商机获客系统 - K8s部署配置

**作者**: 周杰 (EMP-K8S-001)  
**版本**: v1.0.0  
**日期**: 2026-04-08

---

## 📁 目录结构

```
deploy/k8s/
├── namespace.yaml              # 命名空间配置
├── configmap.yaml              # 应用配置
├── secret.yaml                 # 密钥配置（需修改真实值）
├── deploy.sh                   # 一键部署脚本
├── services/                   # 服务部署配置
│   ├── gateway-deployment.yaml # API网关
│   ├── user-deployment.yaml    # 用户服务
│   ├── tenant-deployment.yaml  # 租户服务
│   ├── content-deployment.yaml # 内容服务
│   ├── lead-deployment.yaml    # 线索服务
│   ├── risk-deployment.yaml    # 风控服务
│   ├── billing-deployment.yaml # 计费服务
│   ├── ai-deployment.yaml      # AI服务（GPU）
│   ├── message-deployment.yaml # 消息服务
│   ├── storage-deployment.yaml # 存储服务
│   └── web-deployment.yaml     # 前端Web
├── ingress/                    # Ingress配置
│   └── ingress.yaml
└── monitoring/                 # 监控配置
    ├── prometheus.yaml         # ServiceMonitor + 告警规则
    └── grafana.yaml            # Grafana Dashboard
```

---

## 🚀 快速部署

### 1. 准备工作

确保已安装：
- kubectl
- 可访问的Kubernetes集群（>=1.20）
- Ingress Controller（如Nginx Ingress）
- cert-manager（用于TLS证书管理）

### 2. 修改配置

**⚠️ 重要**: 在部署前必须修改 `secret.yaml` 中的敏感信息！

```bash
# 编辑密钥文件
vim secret.yaml

# 需要修改的字段：
# - DB_PASSWORD, DB_ROOT_PASSWORD
# - REDIS_PASSWORD
# - JWT_SECRET, JWT_REFRESH_SECRET
# - API_KEY_SECRET
# - 第三方服务密钥（阿里云、腾讯云等）
```

### 3. 执行部署

```bash
# 使用部署脚本
chmod +x deploy.sh
./deploy.sh

# 或手动部署
kubectl apply -f namespace.yaml
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml
kubectl apply -f services/
```

### 4. 部署Ingress（可选）

```bash
kubectl apply -f ingress/ingress.yaml
```

### 5. 部署监控（可选）

需要预先安装 Prometheus Operator：

```bash
kubectl apply -f monitoring/
```

---

## 📋 服务清单

| 服务 | 类型 | 端口 | 副本 | 资源限制 |
|------|------|------|------|----------|
| gateway | LoadBalancer | 80/8081 | 3 | 2CPU/2Gi |
| user-service | ClusterIP | 8001/9001 | 3 | 1CPU/1Gi |
| tenant-service | ClusterIP | 8002/9002 | 3 | 1CPU/1Gi |
| content-service | ClusterIP | 8003/9003 | 3 | 1.5CPU/2Gi |
| lead-service | ClusterIP | 8004/9004 | 3 | 1.5CPU/2Gi |
| risk-service | ClusterIP | 8005/9005 | 3 | 2CPU/2Gi |
| billing-service | ClusterIP | 8006/9006 | 3 | 1CPU/1Gi |
| ai-service | ClusterIP | 8007/9007 | 3 | 4CPU/8Gi + GPU |
| message-service | ClusterIP | 8008/9008 | 3 | 1CPU/1Gi |
| storage-service | ClusterIP | 8009/9009 | 3 | 1CPU/1Gi |
| web | LoadBalancer | 80/443 | 3 | 0.5CPU/512Mi |

---

## 🔧 配置说明

### 环境变量

所有服务都挂载以下环境变量：

```yaml
# 来自ConfigMap
- DB_HOST, DB_PORT, DB_NAME
- REDIS_HOST, REDIS_PORT
- 其他应用配置...

# 来自Secret
- DB_USER, DB_PASSWORD
- JWT_SECRET
- API_KEY_SECRET
- 其他密钥...
```

### 健康检查

每个服务都配置了：
- **livenessProbe**: `/health` - 存活检查
- **readinessProbe**: `/ready` - 就绪检查

### 滚动更新策略

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 25%
    maxUnavailable: 25%
```

---

## 🌐 域名配置

Ingress 配置了以下域名：

| 域名 | 服务 | 用途 |
|------|------|------|
| api.beijixing.ai | gateway | API接口 |
| app.beijixing.ai | web | 用户端 |
| admin.beijixing.ai | web | 管理后台 |

---

## 📊 监控告警

### Prometheus告警规则

- **ServiceDown**: 服务不可用超过5分钟
- **HighErrorRate**: 错误率超过5%
- **HighLatency**: P95延迟超过1秒
- **PodCrashLooping**: Pod频繁重启
- **HighMemoryUsage**: 内存使用率超过85%
- **HighCPUUsage**: CPU使用率超过80%

### Grafana Dashboards

1. **系统概览**: 服务状态、请求量、资源使用
2. **AI服务监控**: 模型调用、GPU利用率
3. **业务指标**: 线索数、活跃租户等

---

## 🔐 安全建议

1. **修改默认密钥**: 务必修改 `secret.yaml` 中的所有密钥
2. **启用TLS**: 配置有效的SSL证书
3. **网络策略**: 建议配置NetworkPolicy限制服务间通信
4. **RBAC**: 为服务账户配置最小权限

---

## 📝 常用命令

```bash
# 查看Pod状态
kubectl get pods -n beijixing

# 查看服务
kubectl get svc -n beijixing

# 查看日志
kubectl logs -n beijixing -l app.kubernetes.io/name=gateway

# 进入容器
kubectl exec -it -n beijixing deployment/gateway -- /bin/sh

# 扩缩容
kubectl scale deployment -n beijixing gateway --replicas=5

# 查看Ingress
kubectl get ingress -n beijixing
```

---

## 🐛 故障排查

### Pod无法启动
```bash
# 查看事件
kubectl describe pod -n beijixing <pod-name>

# 查看日志
kubectl logs -n beijixing <pod-name> --previous
```

### 服务无法访问
```bash
# 检查服务状态
kubectl get endpoints -n beijixing

# 测试服务连通性
kubectl run -it --rm debug --image=curlimages/curl -n beijixing -- curl http://user-service:8001/health
```

---

## 📌 注意事项

1. AI服务需要GPU节点，已配置nodeSelector和toleration
2. 首次部署可能需要拉取镜像，耗时较长
3. 确保集群有足够的资源（建议：20CPU + 32Gi内存）
4. 生产环境建议使用外部数据库和Redis

---

## 📞 联系

如有问题请联系运维团队：
- 负责人: 周杰 (EMP-K8S-001)
- 邮箱: zhoujie@beijixing.ai
