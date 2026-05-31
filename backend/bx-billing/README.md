# bx-billing 计费管理服务

北极星AI商机获客系统 - 计费管理模块（MOD-008）

## 模块功能

| 功能点 | 说明 |
|--------|------|
| BL-001 | 积分账户管理（账户余额、冻结、解冻） |
| BL-002 | 在线充值（微信/支付宝支付集成） |
| BL-003 | Token消耗计算（Token×10 + 资源占用费） |
| BL-004 | 消费明细记录（精确到每次AI调用） |
| BL-005 | 套餐购买（basic/advanced/annual/lifetime） |
| BL-006 | 充值优惠规则（阶梯优惠、赠送比例） |
| BL-007 | 订单管理（充值订单、消费订单） |
| BL-008 | 发票申请 |
| BL-009 | 财务对账API |
| BL-010 | 扣点标准配置API |

## 技术栈

- Spring Boot 3.2.0
- MyBatis Plus 3.5.5
- MySQL 8.0
- Redis + Redisson（分布式锁）
- 支付宝 SDK
- 微信支付 SDK

## 项目结构

```
bx-billing/
├── src/main/java/com/beijixing/billing/
│   ├── BxBillingApplication.java
│   ├── config/           # 配置类
│   ├── constants/        # 常量
│   ├── controller/       # 控制器
│   ├── dto/              # 数据传输对象
│   ├── entity/           # 实体类
│   ├── mapper/           # MyBatis Mapper
│   ├── service/          # 服务层
│   └── vo/               # 视图对象
├── src/main/resources/
│   ├── application.yml   # 应用配置
│   └── schema.sql        # 数据库脚本
└── pom.xml
```

## API 接口

### 积分账户
- GET `/api/billing/account/{tenantId}/{userId}` - 获取账户信息
- POST `/api/billing/account/{accountId}/freeze` - 冻结账户
- POST `/api/billing/account/{accountId}/unfreeze` - 解冻账户

### 订单与支付
- POST `/api/billing/order/recharge` - 创建充值订单
- GET `/api/billing/order/bonus` - 计算充值优惠
- GET `/api/billing/order/{orderNo}/alipay-qr` - 支付宝支付二维码
- GET `/api/billing/order/{orderNo}/wechatpay-qr` - 微信支付二维码

### Token消耗
- POST `/api/billing/consumption/calculate` - 计算消费金额
- POST `/api/billing/consumption/consume` - 执行消费扣费

### 套餐
- GET `/api/billing/package/configs` - 获取套餐配置
- POST `/api/billing/package/order` - 创建套餐订单

### 发票
- POST `/api/billing/invoice/apply` - 申请发票

### 财务
- GET `/api/billing/finance/reconciliation` - 财务对账
- GET `/api/billing/finance/configs` - 计费配置管理

## 启动方式

```bash
# 1. 创建数据库并执行 schema.sql
mysql -u root -p beijixing_billing < src/main/resources/schema.sql

# 2. 配置环境变量（或修改 application.yml）
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_USER=root
export MYSQL_PASSWORD=your_password
export REDIS_HOST=localhost
export REDIS_PORT=6379

# 3. 启动服务
mvn spring-boot:run
```

## 费用计算规则

```
总费用 = Token数量 × 10（分）+ 资源使用分钟数 × 1（分）

充值优惠阶梯：
- 充值100元送5元
- 充值500元送50元
- 充值1000元送150元
- 充值5000元送1000元
```

## 负责人

- 业务负责人：刘美玲（CFO）
- 技术负责人：苏波（后端架构师）
