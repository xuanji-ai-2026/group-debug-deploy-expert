# Postman API测试验证指南

## 📌 当前状态
- ✅ **日志清理完成**: 1.1GB → 6.3MB (释放1.09GB空间)
- ⚠️ **服务状态**: 应用未运行（需要先启动服务）
- ✅ **测试集合已就绪**: BeijixingAI_Postman_Collection.json

---

## 🚀 快速开始

### 1️⃣ 导入Postman测试集合

**步骤**:
1. 打开Postman应用
2. 点击 **Import** 按钮
3. 选择文件: `D:\BeijiXing-AI\backend\BeijixingAI_Postman_Collection.json`
4. 点击 **Import** 完成导入

### 2️⃣ 配置环境变量

在Postman中创建环境变量：

```
变量名          值                          说明
─────────────   ──────────────────────────   ────────────
base_url        http://43.160.237.122:8080   服务器地址
username        admin                       用户名（默认）
password        your-password               密码（需修改）
jwt_token       （自动填充）                  登录后自动获取
```

**操作步骤**:
1. 点击右上角 **眼睛图标** → **Add Environment**
2. 输入环境名称: `BeijixingAI-Production`
3. 添加上述变量
4. 保存并选择该环境

### 3️⃣ 启动服务（必需）

**登录服务器启动应用**:
```bash
ssh -i "D:\BeijiXing-AI\singapore.pem" root@43.160.237.122

# 检查JAR包是否存在
ls -lh /opt/beijixing-ai/backend/*.jar

# 启动服务（示例）
cd /opt/beijixing-ai/backend
nohup java -Xms512m -Xmx1024m -XX:+UseG1GC \
     -jar beijixing-app.jar \
     --spring.config.location=classpath:/application.yml,/opt/beijixing-ai/config/application.yml \
     > /opt/beijixing-ai/logs/app.log 2>&1 &

# 验证启动
sleep 10 && curl http://localhost:8080/actuator/health
```

### 4️⃣ 执行测试用例

#### 推荐执行顺序：

**Phase 1: 基础验证** (必须通过)
```
✅ 1.1 应用健康检查         GET  /actuator/health
✅ 1.2 服务信息查询         GET  /actuator/info
```

**Phase 2: 用户认证** (获取Token)
```
✅ 2.1 管理员登录           POST /api/v1/auth/login
   → 自动保存Token到环境变量 jwt_token
```

**Phase 3: 核心业务API**
```
✅ 3.1 获取当前用户信息      GET  /api/v1/user/info
✅ 3.2 敏感词检测           POST /api/v1/compliance/sensitive-word/check
✅ 3.3 频率限制检查         POST /api/v1/compliance/rate-limit/check
✅ 3.4 查询抓取目标池       GET  /api/v1/social/crawl/targets
✅ 3.5 发布内容            POST /api/v1/content/publish
✅ 3.6 系统指标查询         GET  /api/v1/monitor/metrics
```

**Phase 4: 评论抓取API (Phase 3新增)**
```
✅ 4.1 创建评论抓取任务     POST /api/v1/social/crawl/comments
✅ 4.2 移动端快速创建任务    POST /api/mobile/crawl/quick-create
✅ 4.3 查询任务进度         GET  /api/mobile/crawl/task/{taskId}/progress
✅ 4.4 快速生成商机         POST /api/mobile/crawl/generate-leads
```

---

## 📊 测试结果记录模板

| 测试时间 | 执行人 | 通过/总数 | 失败接口 | 备注 |
|---------|--------|----------|---------|------|
| ____/__/__ | ______ | __/20 | ______ | ______ |

### 预期结果

**全部通过的标志**:
- ✅ 健康检查返回 `{"status":"UP"}`
- ✅ 登录成功返回JWT Token（200状态码）
- ✅ 所有业务接口返回200或预期状态码
- ✅ Postman Tests选项卡显示 **All tests passed**

---

## 🔧 故障排查

### 问题1: 健康检查失败 (503/404)
**原因**: 服务未启动或端口错误
**解决**:
```bash
# 检查进程
ps aux | grep java

# 检查端口
netstat -tlnp | grep 8080

# 查看日志
tail -100 /opt/beijixing-ai/logs/app.log
```

### 问题2: 登录失败 (401/403)
**原因**: 用户名密码错误或数据库未连接
**解决**:
```bash
# 检查数据库连接
curl -s http://localhost:8080/actuator/health | jq '.components.db'

# 验证用户存在（需查看数据库）
mysql -u root -p beijixing_ai -e "SELECT id, username FROM bx_user LIMIT 5;"
```

### 问题3: Token无效 (401)
**原因**: Token过期或未正确保存
**解决**:
1. 重新执行 **2.1 管理员登录** 测试
2. 检查环境变量 `jwt_token` 是否已更新
3. 确认Tests脚本中有 `pm.environment.set("jwt_token", ...)` 

### 问题4: Phase 3接口404
**原因**: 新功能未部署或路径错误
**解决**:
1. 确认使用最新JAR包（包含Phase 3代码）
2. 检查MobileCrawlController路径映射:
   - `/api/mobile/crawl/*` (移动端专用)
   - `/api/v1/social/crawl/*` (标准REST)

---

## 📈 性能基准参考

| 接口 | 预期响应时间 | 最大 acceptable |
|------|------------|----------------|
| /actuator/health | < 100ms | < 500ms |
| /api/v1/auth/login | < 500ms | < 2s |
| /api/v1/user/info | < 200ms | < 1s |
| /api/v1/social/crawl/comments | < 2s | < 10s |
| /api/mobile/crawl/quick-create | < 300ms | < 2s |

**测试环境**: 生产服务器 (43.160.237.122)  
**并发数**: 1 (单用户顺序测试)

---

## ✅ 完成标准

测试通过后，请确认以下项目：

- [ ] 所有基础检查接口返回200
- [ ] 成功获取JWT Token
- [ ] 核心业务API响应正常
- [ ] Phase 3移动端接口可用
- [ ] 响应时间在可接受范围内
- [ ] 无控制台错误日志（ERROR级别）

---

## 📞 下一步行动

测试完成后：
1. **记录结果**: 截图保存Postman测试报告
2. **问题反馈**: 将失败接口整理成Issue
3. **性能数据**: 收集响应时间数据用于JVM调优
4. **继续任务3**: 根据QPS调整JVM参数

---

**文档版本**: v1.0  
**最后更新**: 2026-05-20  
**维护者**: 北极星AI团队
