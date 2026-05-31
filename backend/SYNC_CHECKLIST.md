# ==============================================
# 北极星AI - 文件同步清单 v1.0
# 日期: 2026-05-20
# 用途: 记录所有需要从本地同步到远程服务器的文件
# 目标服务器: /opt/beijixing-ai/
# ==============================================

## 📋 同步概览

| 类别 | 文件数量 | 优先级 | 状态 |
|------|---------|--------|------|
| 配置文件 | 4个 | 🔴 高 | ✅ 就绪 |
| 部署文档/脚本 | 4个 | 🔴 高 | ✅ 就绪 |
| 测试工具 | 1个 | 🟡 中 | ✅ 就绪 |
| 源代码 (可选) | 2+个 | 🟢 低 | ✅ 就绪 |
| **总计** | **11+个** | - | **✅ 全部就绪** |

---

## 🔴 第一优先级：必须同步（生产运行必需）

### 1. 配置文件 → `/opt/beijixing-ai/config/`

这些文件控制应用的核心行为，**必须在服务器上外部化配置**以便灵活调整：

#### 1.1 `application.yml` (主应用统一配置)
- **本地路径**: `beijixing-app/src/main/resources/application.yml`
- **远程路径**: `/opt/beijixing-ai/config/application.yml`
- **用途**: 
  - 统一数据库连接（MariaDB）
  - Redis缓存配置
  - MyBatis-Plus ORM设置
  - JWT认证密钥
  - AI模型提供商配置（火山引擎/DeepSeek）
  - 邮件服务配置
  - 文件上传限制（100MB）
- **关键参数**:
  ```yaml
  server.port: 8080
  spring.datasource.url: jdbc:mariadb://...
  spring.data.redis.host: ...
  jwt.secret: ${JWT_SECRET}
  ai.provider.type: volcano
  ```
- **⚠️ 注意**: 包含环境变量占位符（`${DB_HOST}`, `${REDIS_PASSWORD}`等），需配合`env.sh`使用

#### 1.2 `application-prod.yml` (生产环境专用配置)
- **本地路径**: `beijixing-app/src/main/resources/application-prod.yml`
- **远程路径**: `/opt/beijixing-ai/config/application-prod.yml`
- **用途**: 
  - 生产级性能调优
  - 日志输出到`/opt/beijixing-ai/logs/application.log`
  - HikariCP连接池优化（10-50连接）
  - Tomcat线程池配置（200最大线程）
  - 日志轮转策略（100MB/文件，30天保留，3GB上限）

#### 1.3 `bootstrap.yml` (Spring Boot启动配置)
- **本地路径**: `beijixing-app/src/main/resources/bootstrap.yml`
- **远程路径**: `/opt/beijixing-ai/config/bootstrap.yml`
- **用途**: 
  - 禁用Spring Cloud Bootstrap（单体架构不需要）
  - 应用名称定义

#### 1.4 `logback-spring.xml` (日志框架配置)
- **本地路径**: `beijixing-app/src/main/resources/logback-spring.xml`
- **远程路径**: `/opt/beijixing-ai/config/logback-spring.xml`
- **用途**: 
  - 日志格式化（JSON/文本可切换）
  - 控制台彩色输出
  - 文件滚动策略
  - 异步日志提升性能

---

### 2. 部署文档与脚本 → `/opt/beijixing-ai/`

运维团队必备的部署参考资料：

#### 2.1 `QUICK_DEPLOY.md` (快速部署指南) ⭐ 新创建
- **版本**: v2.0 (2026-05-20)
- **内容**:
  - 前置检查清单（Java 17, MariaDB, Redis）
  - 5分钟快速部署步骤
  - 目录结构创建命令
  - 环境变量配置模板 (`config/env.sh`)
  - 启动脚本 (`start.sh`) - 含JVM优化参数
  - 停止脚本 (`stop.sh`)
  - 部署验证方法（健康检查、进程确认、端口监听）
  - 常用运维命令（日志查看、错误搜索、服务重启）
  - 故障排查指南
- **适用场景**: 首次部署、版本升级、紧急恢复

#### 2.2 `DEPLOYMENT.md` (完整部署文档)
- **内容**: 详细的环境搭建步骤、依赖安装、安全加固建议
- **用途**: 完整的生产环境部署参考

#### 2.3 `deploy.sh` (Linux部署脚本)
- **功能**: 
  - 本地Maven构建
  - SCP上传JAR包
  - 远程目录创建
  - 自动备份旧版本
  - 环境变量配置
  - 后台启动应用
- **用法**: `./deploy.sh prod`

#### 2.4 `build-monolith.bat` (Windows构建脚本)
- **功能**: Windows环境下的一键Maven构建命令
- **用法**: 双击或在CMD中执行

---

## 🟡 第二优先级：推荐同步（测试与验证）

### 3. 测试工具 → `/opt/beijixing-ai/`

#### 3.1 `BeijixingAI_Postman_Collection.json` ⭐ 新创建
- **内容**: Postman API测试集合
- **包含接口**:
  - ✅ 健康检查 (`GET /actuator/health`)
  - ✅ 用户认证 (`POST /api/auth/login`, `/refresh`)
  - ✅ 用户管理 (`GET/POST /api/users`)
  - ✅ 合规服务 (`POST /api/compliance/check-sensitive-word`)
  - ✅ 评论抓取 (`POST /api/crawl/tasks/create`, `/targets/add`)
  - ✅ 移动端API (`/mobile/crawl/quick-create`)
- **特性**:
  - 环境变量支持（`{{base_url}}`, `{{token}}`）
  - 自动Token管理（登录后自动获取）
  - 智能断言（状态码200、响应时间<3000ms）
  - 详细测试报告生成
- **用途**: 
  - 部署后功能验证
  - 回归测试
  - 性能基准测试
  - 团队协作测试标准

---

## 🟢 第三优先级：可选同步（调试与开发）

### 4. Phase 3 核心源代码 → `/opt/beijixing-ai/src/` (可选)

**说明**: 这些代码已打包在JAR包中，同步到服务器主要用于：
- 在线调试（如需修改后重新编译）
- 代码审查参考
- 问题排查时查看业务逻辑

#### 4.1 `CommentCrawlController.java` (评论抓取控制器)
- **本地路径**: `bx-social/src/main/java/com/beijixing/social/controller/CommentCrawlController.java`
- **最新修改**: 2026-05-20
- **新增功能**:
  - 目标池管理API（Phase 3核心功能）
    - `POST /targets/add` - 添加抓取目标（支持URL自动解析）
    - `POST /targets/batch-add` - 批量添加目标
    - `GET /targets/list` - 查询目标池列表
    - `DELETE /targets/{id}` - 删除目标
    - `GET /targets/{id}` - 获取目标详情
    - `GET /targets/stats` - 获取统计信息
  - 支持平台：抖音、小红书、快手、微博、B站
  - URL自动识别和解析
- **关键代码片段**:
  ```java
  @PostMapping("/targets/add")
  public ResponseEntity<ApiResponse<CrawlTarget>> addTarget(@RequestBody AddTargetRequest request) {
      CrawlTarget target = targetPoolService.addTarget(request.getInput(), request.getUserId());
      return ResponseEntity.ok(ApiResponse.success(target));
  }
  ```

#### 4.2 `MobileCrawlController.java` (移动端爬虫控制器)
- **本地路径**: `bx-social/src/main/java/com/beijixing/social/crawl/controller/MobileCrawlController.java`
- **设计理念**: 移动优先（Mobile-First）
- **核心功能**:
  - `POST /quick-create` - 一键创建任务（仅需3个参数）
  - `GET /task-progress/{taskId}` - 进度查询
  - `POST /quick-generate-leads` - 快速商机生成
  - 离线支持优化
  - 响应速度优化（精简返回字段）
- **移动端特有优化**:
  - 默认值自动填充（maxComments=500, includeReply=false）
  - 返回精简响应格式（MobileTaskResponse）
  - 错误信息友好化（MobileErrorResponse）

---

## 📊 同步状态追踪表

| 序号 | 文件名 | 类别 | 大小 | 最后修改 | 同步状态 | 备注 |
|------|--------|------|------|----------|----------|------|
| 1 | application.yml | 配置 | ~5KB | 2026-05-20 | ⏳ 待同步 | 核心配置 |
| 2 | application-prod.yml | 配置 | ~3KB | 2026-05-20 | ⏳ 待同步 | 生产优化 |
| 3 | bootstrap.yml | 配置 | ~0.5KB | 2026-05-20 | ⏳ 待同步 | 启动配置 |
| 4 | logback-spring.xml | 配置 | ~4KB | 2026-05-20 | ⏳ 待同步 | 日志配置 |
| 5 | QUICK_DEPLOY.md | 文档 | ~8KB | 2026-05-20 | ⏳ 待同步 | **新创建v2.0** |
| 6 | DEPLOYMENT.md | 文档 | ~15KB | 2026-05-19 | ⏳ 待同步 | 完整指南 |
| 7 | deploy.sh | 脚本 | ~3KB | 2026-05-19 | ⏳ 待同步 | Linux部署 |
| 8 | build-monolith.bat | 脚本 | ~1KB | 2026-05-19 | ⏳ 待同步 | Windows构建 |
| 9 | BeijixingAI_Postman_Collection.json | 测试 | ~12KB | 2026-05-20 | ⏳ 待同步 | **新创建** |
| 10 | CommentCrawlController.java | 源码 | ~25KB | 2026-05-20 | ⏳ 待同步 | Phase 3新增API |
| 11 | MobileCrawlController.java | 源码 | ~18KB | 2026-05-20 | ⏳ 待同步 | 移动端控制器 |

---

## 🚀 快速同步命令

### 方式一：使用一键同步脚本（推荐）

#### Windows用户:
```batch
REM 1. 编辑 sync-to-server.bat，修改服务器IP
REM 2. 双击执行或命令行运行:
cd d:\BeijiXing-AI\backend
sync-to-server.bat
```

#### Linux/Mac用户:
```bash
# 1. 添加执行权限
chmod +x sync-to-server.sh

# 2. 设置服务器IP并执行
export REMOTE_HOST=your-server-ip
./sync-to-server.sh

# 仅预览（不实际传输）
./sync-to-server.sh --dry-run

# 同时同步源代码
./sync-to-server.sh --source-code
```

---

### 方式二：手动SCP命令（逐个文件）

```bash
# ====== 配置文件 ======
scp d:/BeijiXing-AI/backend/beijixing-app/src/main/resources/application.yml root@server-ip:/opt/beijixing-ai/config/
scp d:/BeijiXing-AI/backend/beijixing-app/src/main/resources/application-prod.yml root@server-ip:/opt/beijixing-ai/config/
scp d:/BeijiXing-AI/backend/beijixing-app/src/main/resources/bootstrap.yml root@server-ip:/opt/beijixing-ai/config/
scp d:/BeijiXing-AI/backend/beijixing-app/src/main/resources/logback-spring.xml root@server-ip:/opt/beijixing-ai/config/

# ====== 部署文档 ======
scp d:/BeijiXing-AI/backend/QUICK_DEPLOY.md root@server-ip:/opt/beijixing-ai/
scp d:/BeijiXing-AI/backend/DEPLOYMENT.md root@server-ip:/opt/beijixing-ai/
scp d:/BeijiXing-AI/backend/deploy.sh root@server-ip:/opt/beijixing-ai/
scp d:/BeijiXing-AI/backend/build-monolith.bat root@server-ip:/opt/beijixing-ai/

# ====== 测试工具 ======
scp d:/BeijiXing-AI/backend/BeijixingAI_Postman_Collection.json root@server-ip:/opt/beijixing-ai/

# ====== 设置权限 ======
ssh root@server-ip "cd /opt/beijixing-ai && chmod +x deploy.sh && chmod -R 755 config/"
```

---

## ✅ 同步后验证清单

完成文件同步后，**必须在服务器上执行以下验证**：

### 1. 文件完整性检查
```bash
ssh root@server-ip << 'EOF'
cd /opt/beijixing-ai

echo "===== 配置文件检查 ====="
ls -lh config/
echo ""
echo "===== 文档文件检查 ====="
ls -lh *.md
echo ""
echo "===== 脚本权限检查 ====="
ls -lh deploy.sh *.bat
echo ""
echo "===== Postman集合检查 ====="
ls -lh *.json
EOF
```

**预期输出**:
```
===== 配置文件检查 =====
-rw-r--r-- 1 root root 5.2K May 20 10:30 application.yml
-rw-r--r-- 1 root root 3.1K May 20 10:30 application-prod.yml
-rw-r--r-- 1 root root 512B May 20 10:30 bootstrap.yml
-rw-r--r-- 1 root root 4.0K May 20 10:30 logback-spring.xml

===== 文档文件检查 =====
-rw-r--r-- 1 root root 8.5K May 20 10:30 QUICK_DEPLOY.md
...
```

### 2. 配置文件语法验证
```bash
# 检查YAML语法（需要安装yq工具或Python）
ssh root@server-ip "python3 -c \"import yaml; yaml.safe_load(open('/opt/beijixing-ai/config/application.yml'))\" && echo '✅ YAML语法正确'"
```

### 3. JAR包存在性确认
```bash
ssh root@server-ip "ls -lh /opt/beijixing-ai/beijixing-app.jar"
# 应显示: -rwxr-xr-x 1 root root 245M ... beijixing-app.jar
```

---

## 📝 同步历史记录

| 日期 | 操作 | 执行人 | 文件数 | 备注 |
|------|------|--------|--------|------|
| 2026-05-20 10:30 | 首次完整同步 | Assistant | 11个 | 包含Phase 3新增功能 |

---

## ⚠️ 注意事项

1. **安全性**:
   - `application-prod.yml` 和 `env.sh` 包含数据库密码等敏感信息
   - 确保文件权限设置为 `640` 或 `644`（不要777）
   - 不要将包含真实密码的配置文件提交到Git仓库

2. **配置一致性**:
   - 本地和服务器的配置文件保持同步
   - 修改本地配置后，记得重新同步到服务器
   - 使用版本号或日期标记配置文件变更

3. **备份策略**:
   - 同步前先备份服务器上的旧配置文件
   - `deploy.sh` 脚本已包含自动备份逻辑
   - 重要配置变更建议手动额外备份

4. **环境变量**:
   - `application.yml` 中使用了大量环境变量占位符
   - 必须在服务器上正确配置 `config/env.sh` 或系统环境变量
   - 参考 `QUICK_DEPLOY.md` 第2步的配置示例

---

## 🔄 后续维护

### 定期同步时机
- ✅ 每次代码发布前
- ✅ 修改配置文件后
- ✅ 更新部署文档后
- ✅ 新增测试用例后

### 不需要同步的文件
- ❌ `target/` 目录（编译产物，已在JAR中）
- ❌ `.class` 文件（编译产物）
- ❌ `*.log` 日志文件（本地调试日志）
- ❌ `.idea/`, `.vscode/` IDE配置
- ❌ `node_modules/`, `__pycache__/` 依赖缓存

---

## 📞 技术支持

如有问题，请参考：
- 📖 [QUICK_DEPLOY.md](./QUICK_DEPLOY.md) - 快速部署指南
- 📖 [DEPLOYMENT.md](./DEPLOYMENT.md) - 完整部署文档
- 🧪 [Postman Collection](./BeijixingAI_Postman_Collection.json) - API测试集合

---

**文档版本**: v1.0  
**最后更新**: 2026-05-20 10:30  
**维护者**: 北极星AI开发团队