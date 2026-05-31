# bx-tenant 租户服务

## 模块说明
租户管理服务，提供租户注册、审核、状态管理、套餐管理等功能。

## 目录结构
```
bx-tenant/
├── src/main/java/com/beijixing/tenant/
│   ├── BxTenantApplication.java
│   ├── config/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── vo/
│   ├── dto/
│   └── enums/
├── src/main/resources/
│   ├── application.yml
│   └── mapper/
└── pom.xml
```

## 核心功能
- TM-001: 租户注册流程
- TM-002: 租户审核
- TM-003: 批量审核
- TM-004: 租户状态管理
- TM-005: 套餐管理

## 端口
- 服务端口: 8082
