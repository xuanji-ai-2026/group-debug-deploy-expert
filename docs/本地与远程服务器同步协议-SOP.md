# 北极星AI - 本地与远程服务器同步协议 (Synchronization SOP)

**协议版本**: v1.0  
**生效日期**: 2026-05-15  
**适用范围**: 所有参与北极星AI开发、运维的人员  
**核心原则**: **任何对部署相关文件的修改，必须同时更新本地和远程**  

---

## 🎯 协议目标

### 主要目标
1. ✅ 确保 **D:\BeijiXing-AI** (本地) 与 **/opt/beijixing-ai** (远程) 的关键文件 **100%一致**
2. ✅ 消除"配置漂移"风险 (即本地改了但远程没更新，或反之)
3. ✅ 建立可追溯的修改记录，支持快速回滚
4. ✅ 将人为失误率降低到 <1%

### 非目标 (明确排除)
- ❌ 不要求IDE配置文件 (.idea/, .vscode/) 一致
- ❌ 不要求临时文件 (_audit/, temp-docs-extract/) 一致
- ❌ 不要求依赖缓存 (target/, node_modules/, .gradle/) 一致
- ❌ 不要求日志文件 (*.log) 一致

---

## 📋 同步范围定义 (Sync Scope)

### ✅ Tier 0: 必须实时同步的文件 (P0 - 立即生效)

这些文件的修改会导致**生产环境行为变化**，必须在修改后 **5分钟内** 同步到远程。

| 文件类别 | 路径模式 | 示例 | 触发条件 |
|----------|----------|------|----------|
| **微服务配置** | `backend/bx-*/src/main/resources/application.yml` | bx-user的数据库连接 | 任何参数变更 |
| **Nacos引导配置** | `backend/bx-ai/src/main/resources/bootstrap.yml` | Nacos地址/命名空间 | Nacos迁移或重配 |
| **根POM** | `backend/pom.xml` | 依赖版本升级 | 库版本更新 |
| **模块POM** | `backend/bx-*/pom.xml` | 新增依赖 | 功能开发 |
| **Nacos配置中心** | `nacos-config/*.yml` | AI模型参数调整 | 运营调优 |
| **Nginx站点配置** | `deploy/nginx/beijixing-ai.conf` | 域名/SSL证书 | 网站发布 |
| **Docker Compose生产版** | `deploy/docker/docker-compose.prod.yml` | 服务编排变更 | 扩缩容 |
| **K8s Deployment** | `deploy/k8s/services/*-deployment.yaml` | 镜像版本/资源限制 | 发布新版本 |
| **数据库初始化SQL** | `scripts/sql/*.sql`, `deploy/docker/mysql/init.sql` | 表结构变更 | 数据库迭代 |
| **环境变量模板** | `*.env.example`, `.env.production` | 敏感信息外置 | 安全加固 |

**总计**: 约 **50-60个** 文件属于Tier 0

---

### ⚡ Tier 1: 应在构建后同步的文件 (P1 - 当日完成)

这些文件在**重新构建**后才需要同步，可在当天完成。

| 文件类别 | 路径模式 | 构建命令 | 同步时机 |
|----------|----------|----------|----------|
| **前端Admin构建产物** | `frontend/web-admin/dist/**` | `npm run build` | 每次发版前 |
| **前端PC构建产物** | `frontend/web-pc/dist/**` | `npm run build` | 每次发版前 |
| **Android APK** | `mobile/android/app/build/outputs/apk/*.apk` | `./gradlew assembleRelease` | 每次发版前 |
| **iOS IPA** | `mobile ios/build/**/*.ipa` | xcodebuild | 每次发版前 |
| **后端JAR包** | `backend/bx-*/target/*.jar` | `mvn clean package` | 每次发版前 |

**注意**: 这些通常是**二进制产物**，通过CI/CD流水线自动上传更佳

---

### 📦 Tier 2: 变更时同步即可的文件 (P2 - 本周内)

这些文件不直接影响运行时行为，但在下次部署时会用到。

| 文件类别 | 路径模式 | 说明 |
|----------|----------|------|
| **Dockerfile** | `deploy/docker/services/*/Dockerfile` | 容器构建逻辑 |
| **K8s Service/Ingress** | `deploy/k8s/services/*-service.yaml` | 网络路由规则 |
| **监控配置** | `deploy/k8s/monitoring/*.yaml` | Prometheus/Grafana |
| **部署脚本** | `deploy/scripts/**/*.sh` | 构建/部署/回滚脚本 |
| **辅助SQL** | `scripts/sql/alter_*.sql` | 增量迁移脚本 |

---

### 🚫 不同步的文件 (Exclusions)

以下文件**永远不需要**同步到远程服务器:

```
# 开发工具配置
.trae/                          # Trae IDE运行时
.idea/                          # IntelliJ IDEA
.vscode/                        # VS Code
*.iml                           # IntelliJ模块文件

# 缓存与构建中间产物
target/                         # Maven编译输出
node_modules/                   # Node.js依赖
.gradle/                        # Gradle缓存
*.class                         # Java字节码
dist/ (未压缩)                  # 未优化的前端产物

# 临时与审计文件
_audit/                         # 审计临时目录
temp-docs-extract/              # 文档提取缓存
skill-dev/                      # 技能开发文件 (独立项目)
screenshots/                    # 测试截图
test-reports/                   # 测试报告
*.log                           # 日志文件
*.tmp, *.bak                    # 临时备份

# 文档 (可选)
docs/ (需求文档副本)            # 本地参考用

# 操作系统特定
.DS_Store                       # macOS
Thumbs.db                       # Windows
desktop.ini                     # Windows

# 版本控制
.git/                           # Git仓库数据
.gitignore                      # Git忽略规则 (远程可能不同)
```

---

## 🔧 同步工作流 (Synchronization Workflow)

### 工作流概览图

```
┌─────────────────────────────────────────────────────────────┐
│                     修改触发                                 │
│  (开发者修改了某个Tier 0/1/2文件)                            │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│              Step 1: 本地验证 (Local Validation)             │
│  • 语法检查 (YAML/JSON/XML格式)                              │
│  • 编译测试 (Java/Kotlin/Swift)                              │
│  • 单元测试通过                                              │
│  • 代码审查 (Code Review)                                   │
│                                                              │
│  ⏱️ 耗时: 5-30分钟                                           │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│           Step 2: 备份当前状态 (Backup Current State)        │
│  • 记录修改前的文件哈希                                      │
│  • Git提交 (如果使用Git管理)                                 │
│  • 远程服务器快照 (如果是高危变更)                            │
│                                                              │
│  ⏱️ 耗时: 2-5分钟                                            │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│          Step 3: 上传到远程服务器 (Upload to Remote)         │
│                                                              │
│  选择合适的方法:                                             │
│  A. 自动化脚本 (推荐)                                        │
│     .\sync-configs-from-local.bat  # 配置文件                │
│     .\sync-code-to-server.bat      # 代码文件               │
│                                                              │
│  B. 手动SCP (单个文件)                                       │
│     scp -i singapore.pem local-file root@server:/remote/path │
│                                                              │
│  C. rsync增量同步 (批量文件)                                  │
│     rsync -avz --delete local/ remote:/path/                 │
│                                                              │
│  ⏱️ 耗时: 1-10分钟 (取决于网络)                               │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│         Step 4: 远程验证 (Remote Verification)               │
│  • 检查文件是否成功上传 (ls -la)                             │
│  • 对比文件哈希 (sha256sum)                                  │
│  • 重启受影响的服务 (如有必要)                                │
│  • 检查服务健康状态 (/actuator/health)                       │
│  • 检查应用日志是否有错误                                     │
│                                                              │
│  ⏱️ 耗时: 5-15分钟                                           │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│          Step 5: 记录与通知 (Record & Notify)                │
│  • 更新变更日志 (CHANGELOG.md)                               │
│  • 发送通知给相关人员 (邮件/钉钉/Slack)                       │
│  • 更新监控系统 (如有指标变化)                                │
│                                                              │
│  ⏱️ 耗时: 2-3分钟                                            │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                    完成 ✓                                    │
│  文件已同步, 远程服务器运行正常                               │
└─────────────────────────────────────────────────────────────┘
```

---

## 📝 详细操作指南 (Detailed Procedures)

### 场景A: 修改了application.yml (最常见场景)

**示例**: 修改bx-user服务的数据库连接池大小

#### Step 1: 本地编辑

```bash
# 编辑本地文件
notepad++ D:\BeijiXing-AI\backend\bx-user\src\main\application.yml
```

```yaml
# 修改前:
spring:
  datasource:
    hikari:
      maximum-pool-size: 10

# 修改后:
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # ← 增加连接池大小
```

#### Step 2: 本地验证 (重要!)

```powershell
# 2.1 检查YAML语法
python -c "import yaml; yaml.safe_load(open(r'D:\BeijiXing-AI\backend\bx-user\src\main\application.yml'))"
Write-Host "✅ YAML语法正确"

# 2.2 如果有本地开发环境,启动服务测试
cd D:\BeijiXing-AI\backend\bx-user
mvn spring-boot:run -Dspring.profiles.active=dev
```

#### Step 3: 使用自动化脚本同步

```bash
# 进入部署脚本目录
cd D:\BeijiXing-AI\deploy

# 执行配置同步脚本 (会自动同步所有16个服务的application.yml)
.\sync-configs-from-local.bat

# 输出示例:
# ============================================
#   北极星AI - 配置同步 (本地 → 远程)
# ============================================
#
# [1/17] 同步 pom.xml ... OK
# [2/17] 同步 bx-gateway/application.yml ... OK
# [3/17] 同步 bx-user/application.yml ... OK (UPDATED)
# ...
# [16/17] 同步 bx-billing/application.yml ... OK
# [17/17] 同步 bx-ai/bootstrap.yml ... OK
#
# ✅ 同步完成! 共更新 1 个文件
# ⚠️  请重启 bx-user 服务使配置生效
```

#### Step 4: 远程验证

```bash
# 连接服务器
ssh -i "D:\BeijiXing-AI\singapore.pem" root@43.160.237.122

# 4.1 检查文件是否存在且最新
ls -lh /opt/beijixing-ai/backend/bx-user/src/main/resources/application.yml

# 4.2 对比文件内容 (确认修改已生效)
grep "maximum-pool-size" /opt/beijixing-ai/backend/bx-user/src/main/resources/application.yml
# 应该显示: maximum-pool-size: 20

# 4.3 对比文件哈希 (确保完全一致)
sha256sum /opt/beijixing-ai/backend/bx-user/src/main/resources/application.yml
```

#### Step 5: 重启服务 (如需要)

```bash
# 如果是热加载配置 (Spring Cloud Config + @RefreshScope),无需重启
# 否则需要重启对应服务

# 方式1: 使用systemctl (推荐)
sudo systemctl restart bx-user

# 方式2: 使用Docker (如果容器化部署)
docker restart bx-user-container

# 方式3: 使用K8s (如果K8s部署)
kubectl rollout restart deployment/bx-user -n beijixing
```

#### Step 6: 健康检查

```bash
# 等待30秒让服务启动完成
sleep 30

# 检查健康端点
curl http://localhost:8081/actuator/health
# 预期返回: {"status":"UP",...}

# 查看日志确认无错误
sudo journalctl -u bx-user --since "1 minute ago" | tail -20
```

---

### 场景B: 修改了多个文件 (批量变更)

**示例**: 同时修改了POM文件(新增依赖) + application.yml(新功能配置)

#### 推荐方法: 使用sync-code-to-server.bat

```bash
cd D:\BeijiXing-AI\deploy
.\sync-code-to-server.bat

# 该脚本会自动同步:
# ✅ backend/pom.xml (根POM)
# ✅ backend/bx-*/pom.xml (所有模块POM)
# ✅ nacos-config/*.yml (Nacos配置模板)
# ✅ deploy/scripts/ (部署脚本)
```

#### 或者手动执行rsync (更灵活)

```bash
#!/bin/bash
# batch-sync.sh - 自定义批量同步

LOCAL="D:/BeijiXing-AI"
REMOTE="root@43.160.237.122:/opt/beijixing-ai"
KEY="D:/BeijiXing-AI/singapore.pem"

# 同步整个backend目录 (排除target/)
rsync -avz --delete \
    -e "ssh -i $KEY -o StrictHostKeyChecking=no" \
    --exclude 'target/' \
    --exclude '.idea/' \
    --exclude '*.class' \
    "$LOCAL/backend/" \
    "$REMOTE/backend/"

echo "✅ 后端代码同步完成"
```

---

### 场景C: 前端重新构建并部署

#### Step 1: 本地构建

```bash
cd D:\BeijiXing-AI\frontend\web-admin

# 安装依赖 (首次或依赖变更时)
npm install

# 构建
npm run build

# 检查构建产物
ls -lh dist/
# 应该看到 index.html 和 assets/ 目录
```

#### Step 2: 上传dist到服务器

```bash
# 方法1: SCP (简单直接)
scp -r -i "D:\BeijiXing-AI\singapore.pem" \
    D:\BeijiXing-AI\frontend\web-admin\dist\* \
    root@43.160.237.122:/opt/beijixing-ai/frontend/web-admin/dist/

# 方法2: rsync (增量同步,更快)
rsync -avz --delete \
    -e "ssh -i D:/BeijiXing-AI/singapore.pem -o StrictHostKeyChecking=no" \
    D:/BeijiXing-AI/frontend/web-admin/dist/ \
    root@43.160.237.122:/opt/beijixing-ai/frontend/web-admin/dist/
```

#### Step 3: 清理浏览器缓存 (用户侧)

提示用户清除缓存或强制刷新 (Ctrl+F5), 因为前端资源通常会被CDN/浏览器缓存。

---

### 场景D: 高危变更 (数据库结构变更)

**⚠️ 警告**: 数据库变更可能导致**数据丢失**, 必须格外谨慎!

#### Step 1: 备份当前数据库 (必须!)

```bash
# 在远程服务器上执行
ssh -i "D:\BeijiXing-AI\singapore.pem" root@43.160.237.122 << 'EOF'
# 创建带时间戳的备份
BACKUP_FILE="/opt/backups/mysql/beijixing-$(date +%Y%m%d-%H%M%S).sql"
mysqldump -u root -p'YOUR_PASSWORD' --all-databases > $BACKUP_FILE
echo "✅ 数据库已备份至: $BACKUP_FILE"
EOF
```

#### Step 2: 在测试环境先验证SQL

```bash
# 先在开发/测试数据库执行,确认无报错
mysql -u test_user -p test_database < alter_add_new_column.sql
```

#### Step 3: 同步SQL文件到远程

```bash
scp -i "D:\BeijiXing-AI\singapore.pem" \
    D:\BeijiXing-AI\scripts\sql\alter_add_new_column.sql \
    root@43.160.237.122:/opt/beijixing-ai/scripts/sql/
```

#### Step 4: 在生产数据库执行 (维护窗口期)

```bash
ssh -i "D:\BeijiXing-AI\singapore.pem" root@43.160.237.122 << 'EOF'
# 在低峰期执行 (如凌晨3点)
mysql -u root -p'YOUR_PASSWORD' beijixing_prod < /opt/beijixing-ai/scripts/sql/alter_add_new_column.sql

# 验证表结构
DESCRIBE beijixing_prod.target_table;
EOF
```

#### Step 5: 验证应用功能

- 启动相关微服务
- 执行涉及该表的业务操作
- 检查日志有无SQL错误

---

## 🛠️ 自动化工具集 (Automation Toolkit)

### 工具1: 一键同步脚本 (已有)

位置: `D:\BeijiXing-AI\deploy\sync-configs-from-local.bat`

**用途**: 快速同步所有配置文件

**使用场景**: 
- 修改了application.yml后
- 日常运维配置调整
- 发布前最后一次配置校验

**增强建议** (可自行改进):
```batch
@echo off
:: 增强版 sync-configs-from-local.bat
:: 增加: 哈希校验 + 日志记录 + 回滚能力

set LOG_FILE=D:\BeijiXing-AI\logs\sync-config-%date:~0,4%%date:~5,2%%date:~8,2%.log

echo [%time%] 开始同步配置 >> %LOG_FILE%

:: 同步前记录当前哈希
for %%S in (bx-gateway bx-user bx-tenant bx-content bx-lead bx-ai bx-risk bx-message bx-storage bx-system bx-data bx-social bx-schedule bx-monitor bx-billing) do (
    if exist "D:\BeijiXing-AI\backend\%%S\src\main\application.yml" (
        for /f "tokens=*" %%H in ('certutil -hashfile "D:\BeijiXing-AI\backend\%%S\src\main\application.yml" SHA256 ^| find /v ":" ^| find /v "CertUtil"') do (
            echo [BEFORE] %%S: %%H >> %LOG_FILE%
        )
    )
)

:: ... 执行原有同步逻辑 ...

:: 同步后再次记录哈希 (用于对比)
echo [%time%] 同步完成, 开始验证 >> %LOG_FILE%
:: (调用SSH获取远程哈希进行对比)
```

---

### 工具2: 一致性校验脚本 (新建)

位置: `D:\BeijiXing-AI\deploy\scripts\check-server-consistency.ps1`

**用途**: 定期检查本地与远程的一致性

**使用频率**: 
- 每天一次 (自动定时任务)
- 每次发布前后 (手动执行)
- 怀疑配置问题时 (故障排查)

**详细用法**: 见上一节文档

---

### 工具3: 定时同步任务 (Cron Job)

在远程服务器上设置定时拉取/检查任务:

```bash
# 编辑crontab
crontab -e

# 添加以下任务:

# 每天凌晨3点执行一致性检查 (结果发送邮件)
0 3 * * * /opt/beijixing-ai/deploy/scripts/check-consistency-cron.sh 2>&1 | mail -s "每日一致性报告" ops@beijixing.com

# 每6小时同步一次Git仓库 (如果使用Git管理代码)
0 */6 * * * cd /opt/beijixing-ai && git pull origin main 2>&1 | logger -t sync-job

# 每小时检查关键配置文件哈希 (检测配置漂移)
0 * * * * /opt/beijixing-ai/deploy/scripts/check-config-drift.sh
```

---

### 工具4: Git Hooks (预提交检查)

如果项目使用Git管理,可以添加pre-commit钩子防止提交不一致的代码:

```bash
# .git/hooks/pre-commit

#!/bin/bash
echo "🔍 检查待提交文件是否需要同步..."

# 获取本次修改的文件列表
CHANGED_FILES=$(git diff --cached --name-only)

# 检查是否包含需要同步的关键文件
SYNC_REQUIRED=false
for FILE in $CHANGED_FILES; do
    if [[ $FILE == *"application.yml"* ]] || \
       [[ $FILE == *"pom.xml"* ]] || \
       [[ $FILE == *"bootstrap.yml"* ]]; then
        echo "⚠️  检测到关键配置文件变更: $FILE"
        SYNC_REQUIRED=true
    fi
done

if [ "$SYNC_REQUIRED" = true ]; then
    echo ""
    echo "📢 提醒: 您修改了关键配置文件!"
    echo "   请在提交后立即执行: ./deploy/sync-configs-from-local.bat"
    echo "   或手动SCP到远程服务器"
    echo ""
    read -p "确认已了解同步要求? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "❌ 已取消提交"
        exit 1
    fi
fi

exit 0
```

---

## 🚨 异常处理流程 (Exception Handling)

### 异常1: 同步失败 (网络中断/权限不足)

**症状**: SCP/rsync命令报错 `Connection refused` 或 `Permission denied`

**处理步骤**:

```bash
# 1. 检查网络连通性
ping 43.160.237.122

# 2. 检查SSH服务
ssh -v -i "D:\BeijiXing-AI\singapore.pem" root@43.160.237.122 "echo OK"
# 注意: -v 会显示详细调试信息

# 3. 检查密钥权限 (Linux/Mac要求600或400)
chmod 400 D:\BeijiXing-AI\singapore.pem  # 如果在WSL中

# 4. 检查磁盘空间
ssh root@43.160.237.122 "df -h /opt/beijixing-ai"

# 5. 如果仍然失败,联系网络管理员或云服务商
```

---

### 异常2: 配置漂移 (远程被意外修改)

**症状**: 校验脚本发现本地和远程哈希不一致,但你近期没有修改过

**可能原因**:
- 其他运维人员手动修改了远程文件
- 自动化脚本/Job意外修改了配置
- 黑客入侵或恶意篡改
- 磁盘损坏导致文件损坏

**处理步骤**:

```bash
# 1. 立即查看哪个文件不一致
ssh root@43.160.237.122 "sha256sum /opt/beijixing-ai/backend/bx-user/src/main/resources/application.yml"

# 2. 对比差异 (找出具体哪里不同)
diff <(ssh root@43.160.237.122 "cat /opt/beijixing-ai/backend/bx-user/src/main/resources/application.yml") \
     "D:\BeijiXing-AI\backend\bx-user\src\main\application.yml"

# 3. 判断哪边是正确的
#    - 如果本地是你最后修改的 → 用本地覆盖远程
#    - 如果远程是别人有意修改的 → 合并两边变更
#    - 如果不知道原因 → 先备份两端,再调查原因

# 4. 强制同步 (以本地为准)
scp -i "D:\BeijiXing-AI\singapore.pem" \
    "D:\BeijiXing-AI\backend\bx-user\src\main\application.yml" \
    root@43.160.237.122:/opt/beijixing-ai/backend/bx-user/src/main/resources/

# 5. 记录事件 (用于审计)
echo "$(date): 检测到配置漂移 - bx-user/application.yml, 已强制同步" >> /var/log/config-drift.log
```

**预防措施**:
- 设置文件属性为只读 (chmod 444) (仅允许root修改)
- 启用文件审计日志 (auditd in Linux)
- 限制SSH访问IP白名单

---

### 异常3: 同步后服务崩溃

**症状**: 修改配置并同步后,服务无法启动或频繁报错

**紧急回滚流程**:

```bash
# 1. 立即恢复备份 (如果有)
ssh root@43.160.237.122 << 'EOF'
# 从备份恢复 (假设之前有备份)
cp /opt/backups/configs/bx-user-application.yml.bak \
   /opt/beijixing-ai/backend/bx-user/src/main/resources/application.yml

# 重启服务
sudo systemctl restart bx-user

# 检查状态
sudo systemctl status bx-user
EOF

# 2. 如果没有备份,从本地Git历史恢复
git checkout HEAD~1 -- backend/bx-user/src/main/resources/application.yml
scp -i "D:\BeijiXing-AI\singapore.pem" \
    "D:\BeijiXing-AI\backend\bx-user\src\main\application.yml" \
    root@43.160.237.122:/opt/beijixing-ai/backend/bx-user/src/main/resources/

# 3. 分析错误原因
ssh root@43.160.237.122 "journalctl -u bx-user --since '5 minutes ago' | tail -100"

# 4. 修正配置后重新同步 (这次要先在本地充分测试!)
```

---

### 异常4: 文件冲突 (多人同时修改同一文件)

**症状**: Git合并冲突,或者两个人同时修改了同一个application.yml

**解决方案**:

1. **沟通优先**: 立即联系另一位修改者,协调变更
2. **使用锁机制**: 对于关键文件,引入文件锁 (如使用Git LFS或专用锁服务)
3. **分支策略**: 强制通过Pull Request合并,由Code Review发现冲突
4. **配置拆分**: 将大的application.yml拆分为多个小文件 (如db.yml, redis.yml, app.yml)

**最佳实践**: 
- 避免直接修改共享配置文件
- 优先使用Nacos配置中心的**动态配置**功能 (支持灰度发布和回滚)
- 重要变更走Change Request流程

---

## 📊 监控与审计 (Monitoring & Auditing)

### 监控指标

建议实施以下监控项,及时发现同步问题:

| 指标名称 | 采集方式 | 告警阈值 | 处理方式 |
|----------|----------|----------|----------|
| **文件一致性率** | 定时脚本对比哈希 | <100% 时告警 | 立即排查 |
| **同步延迟时间** | 记录文件修改时间vs同步时间 | >30分钟 告警 | 检查同步任务 |
| **同步失败次数** | 统计SCP/rsync退出码非0的次数 | >3次/天 告警 | 检查网络/权限 |
| **配置漂移次数** | 检测非预期的文件修改 | >0次/天 告警 | 审计调查 |
| **服务重启频率** | 统计systemctl restart调用 | >5次/天 告警 | 检查配置稳定性 |

### 审计日志

保留所有同步操作的完整日志,便于事后追溯:

```bash
# 日志格式示例:
[2026-05-15 14:30:15] [INFO] [user=zhangsan] [action=SYNC_CONFIG] [file=bx-user/application.yml] [source=LOCAL] [target=REMOTE] [result=SUCCESS] [hash=a1b2c3d4...]
[2026-05-15 14:35:22] [WARN] [user=lisi] [action=VERIFY] [file=bx-ai/bootstrap.yml] [status=MISMATCH] [local_hash=e5f6g7h8...] [remote_hash=i9j0k1l2...]
[2026-05-15 15:00:00] [ERROR] [system=cron] [action=SYNC_CODE] [error=Connection timed out] [retry=3/3]
```

---

## 👥 责任分工 (RACI Matrix)

| 任务角色 | R (执行) | A (负责) | C (咨询) | I (知情) |
|----------|----------|----------|----------|----------|
| **修改本地代码** | 开发者 | 开发者本人 | Tech Lead | 项目经理 |
| **本地测试验证** | 开发者/QA | Tech Lead | - | - |
| **执行同步操作** | DevOps工程师 | DevOps负责人 | 开发者 | 全团队 |
| **远程验证检查** | DevOps工程师 | DevOps负责人 | SRE | - |
| **紧急回滚处理** | 值班工程师 | DevOps负责人 | 开发者 | 项目经理 |
| **审计日志审查** | 安全工程师 | 安全负责人 | - | 管理层 |
| **同步流程优化** | DevOps工程师 | 架构师 | 全团队 | - |

**R = Responsible (执行者)**  
**A = Accountable (最终负责人)**  
**C = Consulted (需咨询)**  
**I = Informed (需知情)**  

---

## 📚 培训与考核 (Training & Assessment)

### 新员工入职培训 (必读)

1. **阅读本协议** (30分钟)
2. **观看演示视频** (15分钟) - 如有录制
3. **模拟练习** (45分钟) - 在测试环境演练完整流程
4. **通过在线测验** (15分钟) - 80分以上及格

### 定期复训 (每季度)

- 回顾最近发生的同步事故案例
- 更新最佳实践和工具使用方法
- 考核实际操作能力

### 违规处理

对于违反本协议导致生产事故的行为:

| 严重程度 | 后果 | 示例 |
|----------|------|------|
| **轻微** | 口头警告 + 补充培训 | 首次忘记同步,未造成影响 |
| **中等** | 书面警告 + 绩效扣分 | 多次忘记同步,导致短暂异常 |
| **严重** | 行政处分 + 经济赔偿 | 故意跳过验证,导致数据丢失或长时间宕机 |

---

## 🔄 协议演进 (Protocol Evolution)

### 版本历史

| 版本 | 日期 | 作者 | 变更内容 |
|------|------|------|----------|
| v1.0 | 2026-05-15 | Group Debug & Deploy Expert | 初始版本,基于21 Iron Principles制定 |

### 未来规划

- **v1.1** (预计下月): 引入Git-based全自动同步 (GitHub Actions → SSH Deploy)
- **v2.0** (预计下季度): 引入配置即代码 (Infrastructure as Code) + Terraform管理
- **v3.0** (预计明年): 引入AI驱动的智能同步预测 (根据变更类型自动判断影响范围)

---

## ✅ Checklist: 每次修改后的必做事项

打印此清单,贴在显示器旁,每次修改后逐项勾选:

### 修改完成后立即检查:

- [ ] **1. 我修改的是否属于Tier 0/1/2文件?**
  - 如果是 → 必须执行同步流程
  - 如果否 → 无需同步 (如仅修改了.java源码,等下次构建时再同步)

- [ ] **2. 我已经在本地验证过修改的正确性了吗?**
  - YAML/JSON语法检查 ✓
  - 编译通过 ✓
  - 相关单元测试通过 ✓

- [ ] **3. 我已经备份了修改前的版本吗?**
  - Git commit ✓
  - 或复制了一份.bak文件 ✓

- [ ] **4. 我已经执行同步操作了吗?**
  - 运行了 `sync-configs-from-local.bat` ✓
  - 或手动SCP/rsync ✓

- [ ] **5. 我已经在远程服务器上验证过了吗?**
  - 文件存在且大小正确 ✓
  - 文件哈希一致 ✓
  - 受影响的服务正常运行 ✓
  - 健康检查端点返回UP ✓

- [ ] **6. 我已经记录这次变更了吗?**
  - 更新了CHANGELOG.md ✓
  - 或发送了通知给团队成员 ✓

- [ ] **7. 如果是高危变更 (数据库/安全/计费),我是否:**
  - 通知了相关负责人? ✓
  - 在维护窗口期执行? ✓
  - 准备好了回滚方案? ✓
  - 有同事在线协助? ✓

**全部勾选完毕 → ✅ 可以安心下班了!**

**有任何一项未完成 → ⚠️ 请立即补全,不要抱侥幸心理!**

---

**协议批准人**: _________________ (技术负责人签名)  
**生效日期**: 2026-05-15  
**下次评审日期**: 2026-08-15 (3个月后)  
**分发清单**: 
- [x] 全体开发人员
- [x] DevOps团队
- [x] QA测试团队
- [x] 项目经理
- [x] 技术委员会归档

---

*本协议遵循21 Iron Principles,特别是:*
-*P1 (Absolute Truth)*: *永远基于事实,不猜测*
-*P2 (Entropy Reduction)*: *减少系统熵增,保持有序*
-*P3 (Minimal Change)*: *最小化变更范围*
-*P20 (Anti-Goal-Drift)*: *始终聚焦同步这一核心目标*

**记住: 同步不是负担,而是保障生产环境稳定的基石! 💪**