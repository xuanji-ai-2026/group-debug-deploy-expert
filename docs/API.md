# 北极星AI商机获客系统 - 完整API接口文档

## 📋 概述

本文档详细描述北极星AI商机获客系统的所有API接口规范。

## 🌐 服务地址

| 环境 | 地址 |
|------|------|
| 开发环境 | http://localhost:8070 (网关) |
| 用户服务 | http://localhost:8081/bx-user |
| 租户服务 | http://localhost:8082/bx-tenant |
| 商机服务 | http://localhost:8084/bx-lead |
| 内容服务 | http://localhost:8083/bx-content |
| AI服务 | http://localhost:8085/bx-ai |
| 消息服务 | http://localhost:8086/bx-message |
| 存储服务 | http://localhost:8087/bx-storage |
| 风控服务 | http://localhost:8088/bx-risk |
| 计费服务 | http://localhost:8089/bx-billing |

## 🔐 认证方式

### 1. 用户名密码登录
```
POST /bx-user/api/auth/login
Content-Type: application/json

{
    "username": "手机号",
    "password": "密码"
}
```

**响应示例：**
```json
{
    "code": 0,
    "message": "success",
    "data": {
        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "expiresIn": 86400,
        "userInfo": {
            "id": 1,
            "username": "13800138000",
            "nickName": "张三",
            "tenantId": 1
        }
    }
}
```

### 2. 刷新Token
```
POST /bx-user/api/auth/refresh
Content-Type: application/json

{
    "refreshToken": "刷新Token"
}
```

### 3. 退出登录
```
POST /bx-user/api/auth/logout
Authorization: Bearer {token}
```

## 📝 统一响应格式

```json
{
    "code": 0,           // 0=成功，其他=失败
    "message": "success",
    "data": {},          // 返回数据（可选）
    "timestamp": 1704969600000
}
```

## ❌ 统一错误码

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 40000 | 参数错误 |
| 40100 | 未授权/Token过期 |
| 40300 | 权限不足 |
| 40400 | 资源不存在 |
| 50000 | 系统错误 |

---

## 📦 用户服务 (bx-user:8081)

### 基础信息
- **基础路径**: `/bx-user`
- **认证方式**: Bearer Token

### 1. 用户注册
```
POST /bx-user/api/auth/register
Content-Type: application/json

{
    "phone": "13800138000",
    "password": "Aa123456",
    "verifyCode": "123456"
}
```

### 2. 发送验证码
```
POST /bx-user/api/auth/send-verify-code
Content-Type: application/json

{
    "phone": "13800138000",
    "type": "REGISTER"  // REGISTER | LOGIN | RESET_PASSWORD
}
```

### 3. 获取用户信息
```
GET /bx-user/api/user/info
Authorization: Bearer {token}
```

**响应：**
```json
{
    "code": 0,
    "data": {
        "id": 1,
        "phone": "13800138000",
        "nickName": "张三",
        "avatar": "https://xxx.com/avatar.jpg",
        "tenantId": 1,
        "tenantName": "默认租户",
        "roles": ["ADMIN"],
        "permissions": ["user:view", "user:edit"]
    }
}
```

### 4. 更新用户信息
```
PUT /bx-user/api/user/info
Authorization: Bearer {token}
Content-Type: application/json

{
    "nickName": "张三",
    "avatar": "https://xxx.com/new-avatar.jpg"
}
```

### 5. 修改密码
```
POST /bx-user/api/user/change-password
Authorization: Bearer {token}
Content-Type: application/json

{
    "oldPassword": "OldPass123",
    "newPassword": "NewPass123"
}
```

---

## 📦 租户服务 (bx-tenant:8082)

### 基础信息
- **基础路径**: `/bx-tenant`
- **认证方式**: Bearer Token

### 1. 创建租户
```
POST /bx-tenant/api/tenant/create
Authorization: Bearer {token}
Content-Type: application/json

{
    "tenantName": "深圳市xxx公司",
    "contactPerson": "张三",
    "contactPhone": "13800138000",
    "packageType": "PROFESSIONAL"  // BASIC | PROFESSIONAL | ENTERPRISE
}
```

**套餐类型说明：**
- `BASIC`: 基础版 - 5用户，1000条商机/月
- `PROFESSIONAL`: 专业版 - 20用户，10000条商机/月
- `ENTERPRISE`: 企业版 - 不限用户，不限商机

### 2. 租户列表
```
GET /bx-tenant/api/tenant/list
Authorization: Bearer {token}
Query参数:
  - page: 页码（默认1）
  - size: 每页数量（默认10）
  - keyword: 搜索关键词（可选）
```

### 3. 租户详情
```
GET /bx-tenant/api/tenant/{id}
Authorization: Bearer {token}
```

### 4. 更新租户
```
PUT /bx-tenant/api/tenant/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
    "tenantName": "新公司名",
    "contactPerson": "李四",
    "status": "ACTIVE"  // ACTIVE | SUSPENDED | CANCELLED
}
```

### 5. 套餐变更
```
POST /bx-tenant/api/tenant/{id}/change-package
Authorization: Bearer {token}
Content-Type: application/json

{
    "newPackageType": "ENTERPRISE",
    "effectiveTime": "IMMEDIATE"  // IMMEDIATE | NEXT_PERIOD
}
```

---

## 📦 商机服务 (bx-lead:8084)

### 基础信息
- **基础路径**: `/bx-lead`
- **认证方式**: Bearer Token

### 1. 创建商机
```
POST /bx-lead/api/lead/create
Authorization: Bearer {token}
Content-Type: application/json

{
    "companyName": "深圳市xxx科技有限公司",
    "contactName": "张三",
    "contactPhone": "13800138000",
    "contactEmail": "zhangsan@company.com",
    "industry": "互联网",
    "city": "深圳市",
    "source": "AI_DISCOVERY",  // AI_DISCOVERY | MANUAL | IMPORT | REFERRAL
    "intentionLevel": "A",    // A(B级意向) | B(有意向) | C(需培育) | D(无意向)
    "remark": "通过AI分析发现的企业"
}
```

### 2. 商机列表
```
GET /bx-lead/api/lead/list
Authorization: Bearer {token}
Query参数:
  - page: 页码
  - size: 每页数量
  - keyword: 公司名搜索
  - intentionLevel: 意向等级(A/B/C/D)
  - status: 商机状态(NEW/FOLLOWING/CONVERTED/INVALID)
  - startDate: 开始日期
  - endDate: 结束日期
```

**响应示例：**
```json
{
    "code": 0,
    "data": {
        "records": [
            {
                "id": 1,
                "companyName": "深圳市xxx科技有限公司",
                "contactName": "张三",
                "contactPhone": "138****8000",
                "intentionLevel": "A",
                "status": "NEW",
                "createdAt": "2026-04-11 10:00:00"
            }
        ],
        "total": 100,
        "page": 1,
        "size": 10
    }
}
```

### 3. 商机详情
```
GET /bx-lead/api/lead/{id}
Authorization: Bearer {token}
```

### 4. 商机跟进
```
POST /bx-lead/api/lead/{id}/follow-up
Authorization: Bearer {token}
Content-Type: application/json

{
    "followUpType": "CALL",  // CALL | VISIT | WECHAT | EMAIL | OTHER
    "content": "电话沟通，客户表示有兴趣",
    "nextFollowUpTime": "2026-04-15 10:00:00",
    "intentionLevel": "A"
}
```

### 5. 商机转化
```
POST /bx-lead/api/lead/{id}/convert
Authorization: Bearer {token}
Content-Type: application/json

{
    "customerName": "客户公司名",
    "contactName": "张三",
    "contactPhone": "13800138000",
    "dealAmount": 50000
}
```

### 6. AI智能获客
```
POST /bx-lead/api/lead/ai-discover
Authorization: Bearer {token}
Content-Type: application/json

{
    "industry": "互联网",
    "city": "深圳市",
    "keywords": ["AI", "SaaS", "云计算"],
    "targetCount": 50
}
```

---

## 📦 内容服务 (bx-content:8083)

### 基础信息
- **基础路径**: `/bx-content`
- **认证方式**: Bearer Token

### 1. 创建内容
```
POST /bx-content/api/content/create
Authorization: Bearer {token}
Content-Type: application/json

{
    "title": "AI如何改变企业营销",
    "content": "正文内容...",
    "summary": "简短摘要",
    "contentType": "ARTICLE",  // ARTICLE | VIDEO | IMAGE | AUDIO
    "tags": ["AI", "营销", "科技"],
    "coverImage": "https://xxx.com/cover.jpg",
    "publishTime": "2026-04-15 10:00:00"
}
```

### 2. 内容列表
```
GET /bx-content/api/content/list
Authorization: Bearer {token}
Query参数:
  - page: 页码
  - size: 每页数量
  - contentType: 内容类型
  - status: 状态(DRAFT/PUBLISHED/SCHEDULED/ARCHIVED)
```

### 3. 发布内容
```
POST /bx-content/api/content/{id}/publish
Authorization: Bearer {token}
```

### 4. 定时发布
```
POST /bx-content/api/content/{id}/schedule
Authorization: Bearer {token}
Content-Type: application/json

{
    "publishTime": "2026-04-15 10:00:00",
    "platforms": ["WECHAT", "WECHAT_WORK", "DINGTALK"]
}
```

---

## 📦 AI服务 (bx-ai:8085)

### 基础信息
- **基础路径**: `/bx-ai`
- **认证方式**: Bearer Token

### 1. AI对话
```
POST /bx-ai/api/chat
Authorization: Bearer {token}
Content-Type: application/json

{
    "message": "帮我分析一下这些潜在客户的价值",
    "sessionId": "session-uuid-123",
    "context": {
        "leadIds": [1, 2, 3, 4, 5]
    }
}
```

### 2. 客户分析
```
POST /bx-ai/api/analyze/customer
Authorization: Bearer {token}
Content-Type: application/json

{
    "companyName": "深圳市xxx科技有限公司",
    "industry": "互联网",
    "website": "https://xxx.com",
    "description": "公司描述..."
}
```

**响应示例：**
```json
{
    "code": 0,
    "data": {
        "companyName": "深圳市xxx科技有限公司",
        "industry": "互联网",
        "employeeScale": "50-100人",
        "annualRevenue": "1000-5000万",
        "intentionLevel": "A",
        "score": 85,
        "tags": ["高新技术企业", "快速发展期", "数字化需求强烈"],
        "recommendations": [
            "推荐产品: 企业版SaaS套件",
            "营销策略: 强调效率提升和成本节约"
        ]
    }
}
```

### 3. 内容生成
```
POST /bx-ai/api/generate/content
Authorization: Bearer {token}
Content-Type: application/json

{
    "type": "MARKETING_ARTICLE",
    "topic": "AI在企业营销中的应用",
    "keywords": ["AI", "营销自动化", "精准获客"],
    "length": "MEDIUM",  // SHORT | MEDIUM | LONG
    "tone": "PROFESSIONAL"  // CASUAL | PROFESSIONAL | TECHNICAL
}
```

---

## 📦 消息服务 (bx-message:8086)

### 基础信息
- **基础路径**: `/bx-message`
- **认证方式**: Bearer Token

### 1. 发送消息
```
POST /bx-message/api/message/send
Authorization: Bearer {token}
Content-Type: application/json

{
    "receiverId": 123,
    "messageType": "TEXT",  // TEXT | IMAGE | FILE | SYSTEM
    "content": "消息内容",
    "priority": "NORMAL"  // LOW | NORMAL | HIGH | URGENT
}
```

### 2. 消息列表
```
GET /bx-message/api/message/list
Authorization: Bearer {token}
Query参数:
  - type: 消息类型
  - startDate: 开始日期
  - endDate: 结束日期
```

### 3. 标记已读
```
POST /bx-message/api/message/read
Authorization: Bearer {token}
Content-Type: application/json

{
    "messageIds": [1, 2, 3]
}
```

### 4. 获取未读数
```
GET /bx-message/api/message/unread-count
Authorization: Bearer {token}
```

---

## 📦 存储服务 (bx-storage:8087)

### 基础信息
- **基础路径**: `/bx-storage`
- **认证方式**: Bearer Token

### 1. 上传文件
```
POST /bx-storage/api/v1/storage/upload
Authorization: Bearer {token}
Content-Type: multipart/form-data

file: [文件]
category: "attachment"  // attachment | image | video | document
```

**响应：**
```json
{
    "code": 0,
    "data": {
        "fileId": "file-uuid-123",
        "fileName": "document.pdf",
        "fileUrl": "https://cos.xxx.com/attachment/file-uuid-123.pdf",
        "fileSize": 1024000
    }
}
```

### 2. 批量上传
```
POST /bx-storage/api/v1/storage/upload/batch
Authorization: Bearer {token}
Content-Type: multipart/form-data

files[]: [多个文件]
category: "attachment"
```

### 3. 获取文件URL
```
GET /bx-storage/api/v1/storage/{fileId}/url
Authorization: Bearer {token}
```

### 4. 删除文件
```
DELETE /bx-storage/api/v1/storage/{fileId}
Authorization: Bearer {token}
```

---

## 📦 风控服务 (bx-risk:8088)

### 基础信息
- **基础路径**: `/bx-risk`
- **认证方式**: Bearer Token

### 1. 内容风控检测
```
POST /bx-risk/api/risk/check
Authorization: Bearer {token}
Content-Type: application/json

{
    "content": "待检测内容",
    "riskType": "CONTENT"
}
```

### 2. 批量风控检测
```
POST /bx-risk/api/risk/check/batch
Authorization: Bearer {token}
Content-Type: application/json

{
    "contents": ["内容1", "内容2", "内容3"],
    "riskType": "CONTENT"
}
```

---

## 📦 计费服务 (bx-billing:8089)

### 基础信息
- **基础路径**: `/bx-billing`
- **认证方式**: Bearer Token

### 1. 账户余额
```
GET /bx-billing/api/billing/balance
Authorization: Bearer {token}
```

**响应：**
```json
{
    "code": 0,
    "data": {
        "balance": 10000.00,
        "currency": "CNY",
        "credits": 5000
    }
}
```

### 2. 充值
```
POST /bx-billing/api/billing/recharge
Authorization: Bearer {token}
Content-Type: application/json

{
    "amount": 1000.00,
    "paymentMethod": "WECHAT"  // WECHAT | ALIPAY | BANK_CARD
}
```

### 3. 消费记录
```
GET /bx-billing/api/billing/records
Authorization: Bearer {token}
Query参数:
  - page: 页码
  - size: 每页数量
  - type: CONSUMPTION | RECHARGE | REFUND
```

### 4. 套餐购买
```
POST /bx-billing/api/billing/package/purchase
Authorization: Bearer {token}
Content-Type: application/json

{
    "packageType": "PROFESSIONAL",
    "duration": 12,  // 月数
    "paymentMethod": "ALIPAY"
}
```

---

## 📦 通用接口

### 1. 获取当前租户信息
```
GET /bx-tenant/api/tenant/current
Authorization: Bearer {token}
```

### 2. 获取数据统计
```
GET /bx-lead/api/statistics/summary
Authorization: Bearer {token}
```

### 3. 获取操作日志
```
GET /bx-system/api/logs
Authorization: Bearer {token}
Query参数:
  - page: 页码
  - size: 每页数量
  - operationType: 操作类型
```

---

## 🔔 WebSocket通知

### 连接方式
```
ws://localhost:8086/ws?token={BearerToken}
```

### 消息格式
```json
{
    "type": "MESSAGE|LEAD_FOLLOW_UP|SYSTEM_NOTIFICATION",
    "data": {},
    "timestamp": 1704969600000
}
```

---

## 📄 注意事项

1. 所有POST/PUT请求的Content-Type为application/json
2. 所有需要认证的接口都需要在Header中携带Token: `Authorization: Bearer {token}`
3. Token有效期为24小时，请及时刷新
4. 分页查询的默认页码为1，每页默认10条
5. 所有时间格式为yyyy-MM-dd HH:mm:ss
6. 金额单位为元（CNY）
7. 文件上传大小限制：普通文件100MB，图片10MB
