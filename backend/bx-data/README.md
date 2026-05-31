# bx-data 数据分析服务

北极星AI商机获客系统 - 数据分析服务

## 功能特性

### DA-001 运营数据看板
- 发布统计（发布数量）
- 互动统计（浏览、点赞、评论、分享、收藏）
- 互动率分析

### DA-002 获客数据看板
- 商机统计（新增、跟进中、已转化）
- 转化率分析
- 线索有效性分析

### DA-003 账号数据看板
- 账号状态监控
- 评分趋势分析
- 粉丝数/获客数统计

### DA-004 消费数据看板
- 积分消费统计
- 金额消耗分析
- 获客成本分析

### DA-005 报表导出
- Excel 格式导出（EasyExcel）
- CSV 格式导出

### DA-006 趋势分析
- 数据趋势展示
- 同比分析（YoY）
- 环比分析（MoM）

## 技术栈

- Spring Boot 3.2
- Elasticsearch 8.11（数据聚合）
- EasyExcel 3.3（报表导出）
- MyBatis Plus 3.5（数据库访问）
- Redis（缓存）
- MySQL 8.0

## 端口

**8092**

## 接口列表

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/v1/data/dashboard/operation` | GET | 运营数据看板 |
| `/api/v1/data/dashboard/lead` | GET | 商机数据看板 |
| `/api/v1/data/dashboard/account` | GET | 账号数据看板 |
| `/api/v1/data/dashboard/billing` | GET | 消费数据看板 |
| `/api/v1/data/report/export` | GET | 报表导出 |
| `/api/v1/data/trend/{type}` | GET | 趋势分析 |

## 请求示例

### 获取运营数据看板
```bash
curl -X GET "http://localhost:8092/api/v1/data/dashboard/operation?startDate=2024-01-01&endDate=2024-01-31" \
  -H "X-Tenant-Id: 1"
```

### 导出Excel报表
```bash
curl -X GET "http://localhost:8092/api/v1/data/report/export?reportType=OPERATION&format=EXCEL" \
  -H "X-Tenant-Id: 1" \
  -o report.xlsx
```

### 获取趋势分析
```bash
curl -X GET "http://localhost:8092/api/v1/data/trend/LEAD?compareType=YOY" \
  -H "X-Tenant-Id: 1"
```

## 配置

配置文件：`src/main/resources/application.yml`

主要配置项：
- `server.port`: 服务端口（默认8092）
- `spring.datasource.*`: 数据库连接配置
- `spring.data.redis.*`: Redis连接配置
- `elasticsearch.*`: Elasticsearch连接配置

## 数据库初始化

运行 SQL 脚本初始化数据库：
```bash
mysql -u root -p < src/main/resources/db/init.sql
```

## 编译运行

```bash
# 编译
mvn clean package

# 运行
java -jar target/bx-data-1.0.0-SNAPSHOT.jar

# 或使用 Maven
mvn spring-boot:run
```

## 环境变量

| 变量名 | 描述 | 默认值 |
|--------|------|--------|
| DB_HOST | 数据库主机 | localhost |
| DB_PORT | 数据库端口 | 3306 |
| DB_NAME | 数据库名称 | bx_data |
| DB_USER | 数据库用户 | root |
| DB_PASSWORD | 数据库密码 | password |
| REDIS_HOST | Redis主机 | localhost |
| REDIS_PORT | Redis端口 | 6379 |
| ES_HOST | Elasticsearch主机 | localhost |
| ES_PORT | Elasticsearch端口 | 9200 |

## 性能优化

- 响应时间目标：<500ms
- 使用 Redis 缓存热点数据
- 使用 Elasticsearch 做数据聚合分析
- 数据库索引优化

## 作者

- **陈工（EMP-DATA-001）** - 数据分析师

## 版本

- **1.0.0** - 初始版本
