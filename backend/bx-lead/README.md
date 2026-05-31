# bx-lead 商机获客服务

北极星AI商机获客系统 - 商机获客模块后端服务

## 项目信息

- **模块ID**: MOD-007
- **模块名称**: 商机获客模块
- **负责人**: 朱怡 (EMP-DATA-001) - 数据总监
- **项目**: 北极星AI商机获客系统 (BEIJIXING)
- **版本**: 1.0.0-SNAPSHOT

## 核心功能

| 功能ID | 功能描述 | 状态 |
|--------|----------|------|
| LG-001 | 同业截客逻辑（竞品关键词监控） | ✅ 已实现 |
| LG-002 | AI意向分析（商机评分） | ✅ 已实现 |
| LG-003 | 商机自动生成 | ✅ 已实现 |
| LG-004 | 商机列表API（筛选、排序、分页） | ✅ 已实现 |
| LG-005 | 商机详情API | ✅ 已实现 |
| LG-006 | 商机分配逻辑（按规则/手动分配） | ✅ 已实现 |
| LG-007 | 商机跟进记录 | ✅ 已实现 |
| LG-008 | 商机状态机（新建/跟进/成交/失败） | ✅ 已实现 |
| LG-009 | 商机数据导出（Excel/CSV） | ✅ 已实现 |
| LG-010 | 商机漏斗分析 | ✅ 已实现 |

## 技术栈

- **框架**: Spring Boot 2.7.18
- **ORM**: MyBatis Plus 3.5.5
- **数据库**: MySQL 8.0
- **缓存**: Redis + Redisson
- **API文档**: Knife4j 4.4.0
- **定时任务**: XXL-Job 2.4.0
- **导出**: EasyExcel 3.3.4
- **JDK**: Java 11

## 项目结构

```
bx-lead/
├── src/main/java/com/beijixing/bxlead/
│   ├── BxLeadApplication.java         # 启动类
│   ├── config/                         # 配置类
│   ├── controller/                     # 控制器层
│   ├── service/                        # 服务层
│   │   └── impl/                       # 服务实现
│   ├── mapper/                         # Mapper接口
│   ├── entity/                         # 实体类
│   ├── dto/                            # 数据传输对象
│   ├── vo/                             # 视图对象
│   ├── enums/                          # 枚举类
│   ├── job/                            # 定时任务
│   └── utils/                          # 工具类
├── src/main/resources/
│   ├── application.yml                 # 主配置文件
│   └── mapper/                         # MyBatis XML映射
├── src/test/                           # 测试代码
└── sql/
    └── init.sql                        # 数据库初始化脚本
```

## 核心实体

### 1. 商机 (bx_lead)
- 商机主表，存储所有商机信息
- 包含客户信息、意向评分、状态、负责人等

### 2. 跟进记录 (bx_lead_follow_up)
- 记录商机的每次跟进情况
- 支持附件和下次提醒

### 3. 截客来源 (bx_intercept_source)
- 存储竞品关键词监控数据
- 自动分析并生成商机

### 4. 状态历史 (bx_lead_status_history)
- 记录商机状态的变更历史

## API接口

### 商机管理
- `POST /api/lead/lead/list` - 分页查询商机列表
- `GET /api/lead/lead/{id}` - 获取商机详情
- `POST /api/lead/lead` - 创建商机
- `PUT /api/lead/lead/{id}` - 更新商机
- `DELETE /api/lead/lead/{id}` - 删除商机
- `POST /api/lead/lead/{id}/assign` - 分配商机
- `POST /api/lead/lead/{id}/status` - 变更状态
- `POST /api/lead/lead/{id}/auto-assign` - 自动分配

### 商机跟进
- `POST /api/lead/follow-up` - 创建跟进记录
- `GET /api/lead/follow-up/list/{leadId}` - 获取跟进列表

### 同业截客
- `POST /api/lead/intercept/generate-lead` - 从截客来源生成商机
- `POST /api/lead/intercept/batch-generate` - 批量生成商机

### 分析报表
- `GET /api/lead/analysis/funnel` - 商机漏斗分析
- `GET /api/lead/analysis/count-by-status` - 按状态统计
- `GET /api/lead/analysis/count-by-level` - 按等级统计
- `GET /api/lead/analysis/count-by-source` - 按来源统计
- `POST /api/lead/analysis/export` - 导出商机数据

## 数据库初始化

```bash
# 执行SQL脚本
mysql -u root -p < sql/init.sql
```

## 启动服务

```bash
# 本地开发
mvn spring-boot:run

# 打包
mvn clean package

# 运行
java -jar target/bx-lead-1.0.0-SNAPSHOT.jar
```

## 访问API文档

启动后访问：http://localhost:8090/api/lead/doc.html

## 配置说明

### 环境变量
- `MYSQL_HOST` - MySQL主机地址
- `MYSQL_PORT` - MySQL端口
- `MYSQL_USER` - MySQL用户名
- `MYSQL_PASSWORD` - MySQL密码
- `REDIS_HOST` - Redis主机地址
- `REDIS_PORT` - Redis端口
- `REDIS_PASSWORD` - Redis密码
- `AI_SERVICE_URL` - AI服务地址
- `AI_API_KEY` - AI服务API密钥
- `XXL_JOB_ADMIN` - XXL-Job管理地址

## 定时任务

| 任务名称 | 执行周期 | 说明 |
|---------|---------|------|
| processInterceptSources | 每分钟 | 扫描并处理截客来源 |
| leadDataCleanupJob | 自定义 | 数据清理（XXL-Job） |
| leadScoreRecalculateJob | 自定义 | 重新计算意向评分（XXL-Job） |
| leadAutoAssignJob | 自定义 | 自动分配商机（XXL-Job） |

## 团队

- **数据总监**: 朱怡 (EMP-DATA-001)
- **高级数据分析师**: 何玲 (EMP-DATA-002)
- **后端架构师**: 苏波 (EMP-BE-001)
- **前端架构师**: 孙勇 (EMP-FE-001)
- **移动端负责人**: 潘桂英 (EMP-MOBILE-001)

## 更新日志

### v1.0.0 (2025-04-08)
- 初始版本发布
- 实现全部10个核心功能点
- 完成同业截客、AI意向分析、商机管理、漏斗分析等功能