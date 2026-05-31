# ============================================================
#  北极星AI - 本地 vs 远程服务器 一致性校验系统
#  生成时间: 2026-05-15
#  用途: 确保D:\BeijiXing-AI (本地) 与远程服务器完全一致
# ============================================================

## 📋 服务器连接信息

| 参数 | 值 |
|------|-----|
| **远程IP** | 43.160.237.122 |
| **用户名** | root |
| **SSH密钥** | D:\BeijiXing-AI\singapore.pem |
| **远程根目录** | /opt/beijixing-ai |
| **后端目录** | /opt/beijixing-ai/backend |
| **前端目录** | /opt/beijixing-ai/frontend |
| **移动端目录** | /opt/beijixing-ai/mobile |
| **配置目录** | /opt/beijixing-ai/config |
| **部署目录** | /opt/beijixing-ai/deploy |

---

## 🎯 部署关键文件分类 (必须一致)

### ✅ **必须同步的文件** (Tier 1 - 核心业务代码)

#### 1️⃣ 后端源码 (backend/)

```
必须同步:
├── pom.xml                          # 根POM文件
├── bx-gateway/
│   ├── pom.xml
│   └── src/main/resources/
│       └── application.yml          # 网关配置
├── bx-user/
│   ├── pom.xml
│   └── src/main/resources/
│       └── application.yml          # 用户服务配置
├── bx-tenant/
│   ├── pom.xml
│   └── src/main/resources/
│       └── application.yml          # 租户服务配置
├── bx-content/
│   ├── pom.xml
│   └── src/main/resources/
│       └── application.yml          # 内容服务配置
├── bx-lead/
│   ├── pom.xml
│   └── src/main/resources/
│       └── application.yml          # 线索服务配置
├── bx-ai/
│   ├── pom.xml
│   └── src/main/resources/
│       ├── application.yml          # AI服务配置
│       └── bootstrap.yml            # Nacos引导配置 ⚠️ 重要
├── bx-risk/
│   ├── pom.xml
│   └── src/main/resources/
│       └── application.yml          # 风控服务配置
├── bx-message/
│   ├── pom.xml
│   └── src/main/resources/
│       └── application.yml          # 消息服务配置
├── bx-storage/
│   ├── pom.xml
│   └── src/main/resources/
│       └── application.yml          # 存储服务配置
├── bx-system/
│   ├── pom.xml
│   └── src/main/resources/
│       └── application.yml          # 系统服务配置
├── bx-data/
│   ├── pom.xml
│   └── src/main/resources/
│       └── application.yml          # 数据服务配置
├── bx-social/
│   ├── pom.xml
│   └── src/main/resources/
│       └── application.yml          # 社交服务配置
├── bx-schedule/
│   ├── pom.xml
│   └── src/main/resources/
│       └── application.yml          # 调度服务配置
├── bx-monitor/
│   ├── pom.xml
│   └── src/main/resources/
│       └── application.yml          # 监控服务配置
└── bx-billing/
    ├── pom.xml
    └── src/main/resources/
        └── application.yml          # 计费服务配置
```

**总计**: 17个POM + 16个application.yml + 1个bootstrap.yml = **34个核心配置文件**

#### 2️⃣ 前端构建产物 (frontend/)

```
必须同步:
├── web-admin/
│   ├── dist/                        # 构建产物 (HTML/CSS/JS)
│   │   ├── index.html
│   │   ├── assets/
│   │   └── ...
│   ├── package.json                 # 依赖声明
│   └── .env.production              # 生产环境变量
└── web-pc/
    ├── dist/                        # 构建产物
    │   ├── index.html
    │   ├── assets/
    │   └── ...
    ├── package.json
    └── .env.production
```

#### 3️⃣ 移动端安装包 (mobile/)

```
必须同步:
├── android/
│   └── app/build/outputs/apk/
│       ├── debug/app-debug.apk      # 调试包
│       └── release/app-release.apk  # 正式包
└── ios/
    └── build/                       # Xcode构建产物 (可选)
```

#### 4️⃣ 数据库脚本 (scripts/sql/ & deploy/docker/mysql/)

```
必须同步:
├── init_database.sql                # 数据库初始化
├── init_all_database.sql            # 全量初始化
├── init_data.sql                    # 初始数据
├── bx-user.sql                      # 用户表结构
├── bx-lead.sql                      # 线索表结构
├── bx-billing.sql                   # 计费表结构
├── bx-risk.sql                      # 风控表结构
├── tenant_init.sql                  # 租户初始数据
├── lead_init.sql                    # 线索初始数据
├── alter_add_foreign_keys.sql       # 外键约束
└── bx_risk_init.sql                 # 风控初始数据
```

#### 5️⃣ 部署配置 (deploy/)

```
必须同步:
├── docker/
│   ├── docker-compose.yml           # Docker编排 (开发环境)
│   ├── docker-compose.prod.yml      # Docker编排 (生产环境) ⚠️ 关键
│   ├── docker-compose.dev.yml       # Docker编排 (开发环境)
│   ├── services/*/Dockerfile        # 各服务Dockerfile (9个)
│   ├── config/mysql/my.cnf          # MySQL配置
│   ├── config/redis/redis.conf      # Redis配置
│   ├── nginx/*.conf                 # Nginx配置
│   └── mysql/init.sql               # MySQL初始化
├── k8s/                             # Kubernetes配置 (生产环境)
│   ├── services/*-deployment.yaml   # 服务部署 (7个)
│   ├── ingress/ingress.yaml         # Ingress入口
│   ├── monitoring/prometheus.yaml   # Prometheus
│   ├── monitoring/grafana.yaml      # Grafana
│   ├── configmap.yaml               # 配置映射
│   ├── secret.yaml                  # 密钥管理
│   └── namespace.yaml               # 命名空间
├── nginx/
│   ├── nginx.conf                   # 主配置
│   ├── beijixing-ai.conf            # 站点配置 ⚠️ 关键
│   └── conf.d/default.conf          # 默认配置
└── scripts/                         # 部署脚本
    ├── backend/build.sh
    ├── backend/deploy.sh
    ├── frontend/build.sh
    ├── mobile/android/build-apk.sh
    ├── mobile/ios/build-ipa.sh
    ├── ops/health-check.sh
    └── ops/rollback.sh
```

#### 6️⃣ Nacos配置中心 (nacos-config/)

```
必须同步:
├── ai-model-config.yml              # AI模型配置 ⚠️ 关键
├── common-datasource.yml            # 数据源配置 ⚠️ 关键
├── common-jwt.yml                   # JWT配置
├── common-logging.yml               # 日志配置
└── common-redis.yml                 # Redis配置
```

---

### ❌ **不需要同步的文件** (仅本地开发使用)

```
不同步:
├── .trae/                           # Trae IDE运行时文件
├── _audit/                          # 审计临时文件
├── skill-dev/                       # 技能开发文件
├── temp-docs-extract/               # 文档提取临时文件
├── tools/                           # 本地工具 (Maven等)
├── *.xml (UI dump文件)              # UI测试临时文件
├── target/                          # Maven编译缓存
├── node_modules/                    # Node.js依赖
├── .gradle/                         # Gradle缓存
├── .idea/                           # IntelliJ IDEA配置
├── screenshots/                     # 测试截图
├── test-reports/                    # 测试报告
├── *.log                            # 日志文件
└── docs/ (需求文档副本)             # 文档文件 (可选)
```

---

## 🔍 校验方法

### 方法A: 自动化校验脚本 (推荐)

在**本地Windows PowerShell**中执行:

```powershell
# ============================================================
#  校验脚本: check-consistency.ps1
#  用途: 对比本地与远程服务器的关键文件
# ============================================================

param(
    [string]$RemoteHost = "43.160.237.122",
    [string]$RemoteUser = "root",
    [string]$RemoteKey = "D:\BeijiXing-AI\singapore.pem",
    [string]$LocalBase = "D:\BeijiXing-AI",
    [string]$RemoteBase = "/opt/beijixing-ai"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  北极星AI - 本地 vs 远程 一致性校验" -ForegroundColor Cyan
Write-Host "  时间: $(Get-Date)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$checkResults = @()

# 1. 检查POM文件一致性
Write-Host "`n[1] 检查POM文件..." -ForegroundColor Yellow
$pomFiles = @("pom.xml")
Get-ChildItem "$LocalBase\backend\bx-*" -Directory | ForEach-Object {
    $pomFiles += "$($_.Name)\pom.xml"
}

foreach ($pom in $pomFiles) {
    $localPath = "$LocalBase\backend\$pom"
    $remotePath = "$RemoteBase/backend/$pom"
    
    if (Test-Path $localPath) {
        $localHash = (Get-FileHash $localPath -Algorithm SHA256).Hash
        
        # 远程文件哈希 (需要SSH连接)
        # $remoteHash = ssh -i $RemoteKey $RemoteUser@$RemoteHost "sha256sum $remotePath | cut -d' ' -f1"
        
        $checkResults += [PSCustomObject]@{
            Type = "POM"
            File = $pom
            LocalHash = $localHash
            RemoteHash = "待检查"
            Status = "需SSH连接"
        }
    }
}

# 2. 检查配置文件一致性
Write-Host "`n[2] 检查配置文件..." -ForegroundColor Yellow
$configFiles = @()
Get-ChildItem "$LocalBase\backend\bx-*" -Directory | ForEach-Object {
    $svc = $_.Name
    $ymlPath = "$LocalBase\backend\$svc\src\main\application.yml"
    if (Test-Path $ymlPath) {
        $configFiles += "$svc/src/main/application.yml"
    }
    
    # 特殊处理bx-ai的bootstrap.yml
    if ($svc -eq 'bx-ai') {
        $bootPath = "$LocalBase\backend\$svc\src\main\bootstrap.yml"
        if (Test-Path $bootPath) {
            $configFiles += "$svc/src/main/bootstrap.yml"
        }
    }
}

foreach ($cfg in $configFiles) {
    $localPath = "$LocalBase\backend\$cfg"
    
    if (Test-Path $localPath) {
        $localHash = (Get-FileHash $localPath -Algorithm SHA256).Hash
        
        $checkResults += [PSCustomObject]@{
            Type = "Config"
            File = $cfg
            LocalHash = $localHash
            RemoteHash = "待检查"
            Status = "需SSH连接"
        }
    }
}

# 3. 输出结果
Write-Host "`n========================================" -ForegroundColor Green
Write-Host "  校验结果汇总" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
$checkResults | Format-Table -AutoSize

Write-Host "`n✅ 本地文件总数: $($checkResults.Count)" -ForegroundColor Cyan
Write-Host "⚠️  需要SSH连接到远程服务器完成对比" -ForegroundColor Yellow
Write-Host "`n下一步: 在支持SSH的环境中运行此脚本" -ForegroundColor Green
```

### 方法B: 手动校验步骤

#### 步骤1: 连接远程服务器

```bash
# Windows (Git Bash / WSL / PowerShell 7+)
ssh -i "D:\BeijiXing-AI\singapore.pem" root@43.160.237.122

# macOS / Linux
ssh -i singapore.pem root@43.160.237.122
```

#### 步骤2: 检查远程目录结构

```bash
# 查看远程根目录
ls -la /opt/beijixing-ai/

# 查看后端服务列表
ls -la /opt/beijixing-ai/backend/

# 查看前端构建产物
ls -la /opt/beijixing-ai/frontend/web-admin/dist/

# 查看JAR文件
find /opt/beijixing-ai/backend -name "*.jar" -type f | head -20
```

#### 步骤3: 关键文件哈希对比

```bash
# 在远程服务器上生成关键文件的SHA256哈希
cd /opt/beijixing-ai/backend

# POM文件哈希
find . -name "pom.xml" -exec sha256sum {} \; > /tmp/remote-pom-hashes.txt

# 配置文件哈希
find . -path "*/resources/application.yml" -exec sha256sum {} \; > /tmp/remote-config-hashes.txt
find . -name "bootstrap.yml" -exec sha256sum {} \; >> /tmp/remote-config-hashes.txt

# 查看结果
cat /tmp/remote-pom-hashes.txt
cat /tmp/remote-config-hashes.txt
```

#### 步骤4: 本地生成哈希并对比

```powershell
# 在本地PowerShell中执行
cd D:\BeijiXing-AI\backend

# 生成POM文件哈希
Get-ChildItem -Recurse -Filter "pom.xml" | Get-FileHash -Algorithm SHA256 | 
    Select-Object @{N='Path';E={$_.Path -replace '.*\\beijixing-ai\\',''}}, Hash | 
    Export-Csv -Path "C:\temp\local-pom-hashes.csv" -NoTypeInformation

# 生成配置文件哈希
Get-ChildItem -Recurse -Filter "application.yml" | Get-FileHash -Algorithm SHA256 | 
    Select-Object @{N='Path';E={$_.Path -replace '.*\\beijixing-ai\\',''}}, Hash | 
    Export-Csv -Path "C:\temp\local-config-hashes.csv" -NoTypeInformation
```

#### 步骤5: 对比差异

将本地CSV与远程的hash文件进行对比，找出不一致的文件。

---

## 📊 校验清单模板

### Tier 1: 必须完全一致的核心文件 (共100+个)

| 类别 | 文件数量 | 优先级 | 校验频率 |
|------|----------|--------|----------|
| **后端POM** | 17个 | 🔴 P0 | 每次发布前 |
| **应用配置** | 16个 yml | 🔴 P0 | 每次修改后 |
| **Bootstrap配置** | 1个 yml | 🔴 P0 | 每次修改后 |
| **前端dist** | 2个目录 | 🟠 P1 | 每次构建后 |
| **数据库SQL** | 11个 | 🟠 P1 | 每次变更后 |
| **Docker配置** | 15+个 | 🟠 P1 | 每次部署前 |
| **K8s配置** | 10+个 | 🟡 P2 | K8s部署时 |
| **Nacos配置** | 5个 | 🔴 P0 | 配置变更时 |
| **Nginx配置** | 3个 | 🟠 P1 | 配置变更时 |
| **部署脚本** | 10+个 | 🟡 P2 | 脚本更新时 |

**总计**: 约 **100-120个** 关键文件需要保持一致

---

## 🚨 常见不一致场景及修复

### 场景1: 本地修改了application.yml但未同步

**症状**: 远程服务行为异常，配置未生效

**修复**:
```bash
# 执行现有同步脚本
cd D:\BeijiXing-AI\deploy
.\sync-configs-from-local.bat

# 或手动同步单个文件
scp -i "D:\BeijiXing-AI\singapore.pem" \
    "D:\BeijiXing-AI\backend\bx-user\src\main\resources\application.yml" \
    root@43.160.237.122:/opt/beijixing-ai/backend/bx-user/src/main/resources/
```

### 场景2: 前端重新构建但未上传dist

**症状**: 用户看到旧版本UI

**修复**:
```bash
# 本地构建前端
cd D:\BeijiXing-AI\frontend\web-admin
npm run build

# 上传dist到服务器
scp -r -i "D:\BeijiXing-AI\singapore.pem" \
    D:\BeijiXing-AI\frontend\web-admin\dist\* \
    root@43.160.237.122:/opt/beijixing-ai/frontend/web-admin/dist/
```

### 场景3: 新增了微服务模块

** symptoms**: 新服务无法启动，缺少POM或配置

**修复**:
```bash
# 同步新模块的所有文件
scp -r -i "D:\BeijiXing-AI\singapore.pem" \
    D:\BeijiXing-AI\backend\bx-new-service\ \
    root@43.160.237.122:/opt/beijixing-ai/backend/bx-new-service/
```

---

## 🔄 自动化同步工作流 (推荐)

### 方案A: Git-based同步 (最佳实践)

```bash
# 1. 本地提交代码
git add .
git commit -m "更新配置: xxx"
git push origin main

# 2. 服务器拉取代码
ssh -i "D:\BeijiXing-AI\singapore.pem" root@43.160.237.122 << 'EOF'
cd /opt/beijixing-ai
git pull origin main
EOF
```

### 方案B: rsync增量同步 (高效)

```bash
#!/bin/bash
# sync-to-remote.sh - 增量同步脚本

LOCAL="D:/BeijiXing-AI"
REMOTE="root@43.160.237.122:/opt/beijixing-ai"
KEY="D:/BeijiXing-AI/singapore.pem"

# 同步后端源码 (排除target/)
rsync -avz --delete \
    -e "ssh -i $KEY -o StrictHostKeyChecking=no" \
    --exclude 'target/' \
    --exclude '.idea/' \
    --exclude '*.class' \
    "$LOCAL/backend/" \
    "$REMOTE/backend/"

# 同步前端dist
rsync -avz --delete \
    -e "ssh -i $KEY -o StrictHostKeyChecking=no" \
    --exclude 'node_modules/' \
    --exclude '.cache/' \
    "$LOCAL/frontend/web-admin/dist/" \
    "$REMOTE/frontend/web-admin/dist/"

# 同步配置文件
rsync -avz \
    -e "ssh -i $KEY -o StrictHostKeyChecking=no" \
    "$LOCAL/nacos-config/" \
    "$REMOTE/nacos-config/"

echo "✅ 同步完成!"
```

### 方案C: 现有脚本增强 (立即可用)

基于现有的 `sync-code-to-server.bat` 和 `sync-configs-from-local.bat`，建议增加以下功能：

1. **哈希校验**: 同步前后自动计算文件哈希
2. **差异报告**: 记录哪些文件被更新
3. **回滚机制**: 保留历史版本，支持一键回滚
4. **定时任务**: 设置cron定时同步 (如每小时)

---

## 📈 监控与告警

### 建议实施的监控项

1. **文件完整性监控**
   ```bash
   # 定期检查关键文件哈希
   */5 * * * * /opt/scripts/check-file-integrity.sh
   ```

2. **配置漂移检测**
   ```bash
   # 检测配置是否被意外修改
   */30 * * * * /opt/scripts/detect-config-drift.sh
   ```

3. **同步状态报告**
   ```
   每日发送同步日志摘要给运维团队
   ```

---

## ✅ 下一步行动

### 立即执行 (今天)

1. ✅ 安装SSH客户端 (如果尚未安装)
   - Windows 10+: 已内置OpenSSH客户端
   - 启用方式: 设置 → 应用 → 可选功能 → OpenSSH客户端

2. ✅ 运行首次全量校验
   ```bash
   # 连接服务器并检查目录结构
   ssh -i "D:\BeijiXing-AI\singapore.pem" root@43.160.237.122 "ls -la /opt/beijixing-ai/"
   ```

3. ✅ 执行现有的同步脚本
   ```bash
   cd D:\BeijiXing-AI\deploy
   .\sync-configs-from-local.bat
   ```

### 短期优化 (本周)

1. 创建自动化校验脚本 (上面的 `check-consistency.ps1`)
2. 配置定时同步任务 (每日凌晨3点)
3. 建立同步日志审查机制

### 长期目标 (本月)

1. 引入Git作为版本控制 (替代手动SCP)
2. 实施CI/CD流水线 (GitHub Actions / Jenkins)
3. 建立配置管理中心 (Nacos Console)

---

**文档版本**: v1.0
**最后更新**: 2026-05-15
**适用范围**: 北极星AI商机获客系统 - 生产环境
**维护责任**: DevOps团队