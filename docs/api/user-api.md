# 用户管理API文档

## 1. 用户认证

### 1.1 用户登录

**接口地址**: `POST /api/v1/auth/login`

**请求参数**:
```json
{
  "username": "admin",
  "password": "123456",
  "captcha": "abc123"
}
```

**响应结果**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userInfo": {
      "id": 1,
      "username": "admin",
      "nickname": "管理员",
      "avatar": "https://example.com/avatar.jpg"
    }
  }
}
```

### 1.2 用户注册

**接口地址**: `POST /api/v1/auth/register`

**请求参数**:
```json
{
  "username": "test",
  "password": "123456",
  "phone": "13800138000",
  "code": "123456"
}
```

**响应结果**:
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": 2
  }
}
```

### 1.3 刷新Token

**接口地址**: `POST /api/v1/auth/refresh`

**请求参数**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**响应结果**:
```json
{
  "code": 200,
  "message": "刷新成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

### 1.4 用户登出

**接口地址**: `POST /api/v1/auth/logout`

**请求头**:
```
Authorization: Bearer {token}
```

**响应结果**:
```json
{
  "code": 200,
  "message": "登出成功"
}
```

## 2. 用户管理

### 2.1 获取用户列表

**接口地址**: `GET /api/v1/users`

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页数量，默认10 |
| keyword | string | 否 | 搜索关键词 |

**响应结果**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 100,
    "list": [
      {
        "id": 1,
        "username": "admin",
        "nickname": "管理员",
        "phone": "13800138000",
        "email": "admin@example.com",
        "avatar": "https://example.com/avatar.jpg",
        "status": 1,
        "createdAt": "2024-01-01 10:00:00"
      }
    ]
  }
}
```

### 2.2 获取用户详情

**接口地址**: `GET /api/v1/users/{id}`

**响应结果**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "id": 1,
    "username": "admin",
    "nickname": "管理员",
    "phone": "13800138000",
    "email": "admin@example.com",
    "avatar": "https://example.com/avatar.jpg",
    "status": 1,
    "roles": [
      {
        "id": 1,
        "roleName": "超级管理员",
        "roleKey": "admin"
      }
    ],
    "createdAt": "2024-01-01 10:00:00"
  }
}
```

### 2.3 创建用户

**接口地址**: `POST /api/v1/users`

**请求参数**:
```json
{
  "username": "test",
  "password": "123456",
  "nickname": "测试用户",
  "phone": "13800138000",
  "email": "test@example.com",
  "roleIds": [2]
}
```

**响应结果**:
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 2
  }
}
```

### 2.4 更新用户

**接口地址**: `PUT /api/v1/users/{id}`

**请求参数**:
```json
{
  "nickname": "测试用户2",
  "phone": "13800138001",
  "email": "test2@example.com",
  "roleIds": [2, 3]
}
```

**响应结果**:
```json
{
  "code": 200,
  "message": "更新成功"
}
```

### 2.5 删除用户

**接口地址**: `DELETE /api/v1/users/{id}`

**响应结果**:
```json
{
  "code": 200,
  "message": "删除成功"
}
```

## 3. 角色管理

### 3.1 获取角色列表

**接口地址**: `GET /api/v1/roles`

**响应结果**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "id": 1,
      "roleName": "超级管理员",
      "roleKey": "admin",
      "description": "系统最高权限",
      "status": 1
    }
  ]
}
```

### 3.2 创建角色

**接口地址**: `POST /api/v1/roles`

**请求参数**:
```json
{
  "roleName": "销售经理",
  "roleKey": "sales_manager",
  "description": "销售团队管理",
  "permissionIds": [1, 2, 3, 4, 5]
}
```

**响应结果**:
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 3
  }
}
```

## 4. 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 5. 注意事项

1. 所有接口需要携带Token进行认证
2. Token有效期为2小时，RefreshToken有效期为7天
3. 密码必须加密传输
4. 敏感操作需要二次验证
