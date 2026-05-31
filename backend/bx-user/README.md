# 北极星AI商机获客系统 - 用户认证模块 (bx-user)

## 模块说明

用户认证模块（MOD-001）负责系统的用户身份认证相关功能，包括用户注册、登录、Token管理、微信OAuth、双因素认证等。

## 技术栈

- **框架**: Spring Boot 3.2.0
- **JDK**: Java 17
- **数据库**: MySQL 8.0
- **缓存**: Redis
- **安全**: Spring Security + JWT
- **构建工具**: Maven

## 功能特性

| 功能编号 | 功能描述 | 状态 |
|---------|---------|------|
| UM-001 | 用户注册API（手机号+验证码） | ✅ 已完成 |
| UM-002 | 用户登录API（JWT，含Token刷新） | ✅ 已完成 |
| UM-003 | 账号锁定机制（5次错误后锁定1小时） | ✅ 已完成 |
| UM-004 | 微信OAuth登录 | ✅ 已完成 |
| UM-005 | Token管理（Access 2小时，Refresh 7天） | ✅ 已完成 |
| UM-006 | 验证码发送与校验（60秒过期） | ✅ 已完成 |
| UM-007 | 设备指纹验证 | ✅ 已完成 |
| UM-008 | 异地登录检测 | ✅ 已完成 |
| UM-009 | 双因素认证(2FA) | ✅ 已完成 |
| UM-010 | 密码强度校验 | ✅ 已完成 |
| UM-011 | 用户登出API | ✅ 已完成 |
| UM-012 | 短信网关集成 | ⚠️ 待配置 |

## 快速开始

### 1. 环境准备

- JDK 17+
- MySQL 8.0
- Redis 6.0+

### 2. 数据库初始化

```bash
mysql -u root -p < src/main/resources/db/init.sql
```

### 3. 配置文件

编辑 `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/beijixing_user
    username: your_username
    password: your_password
  
  redis:
    host: localhost
    port: 6379

jwt:
  secret: your_secret_key_here

wechat:
  oauth:
    app-id: your_wechat_app_id
    app-secret: your_wechat_app_secret
```

### 4. 编译运行

```bash
# 编译
mvn clean package

# 运行
java -jar target/bx-user-1.0.0.jar

# 或直接运行
mvn spring-boot:run
```

服务启动后，访问: http://localhost:8081

## API接口文档

### 用户认证接口

| 方法 | 接口 | 描述 |
|-----|------|-----|
| POST | /api/auth/register | 用户注册 |
| POST | /api/auth/login | 用户登录 |
| POST | /api/auth/refresh | Token刷新 |
| POST | /api/auth/logout | 用户登出 |
| POST | /api/auth/verification-code | 发送验证码 |
| GET | /api/auth/wechat/authorize | 微信授权URL |
| GET | /api/auth/wechat/callback | 微信回调 |

### 请求示例

#### 用户注册
```json
POST /api/auth/register
{
    "phone": "13800138000",
    "verificationCode": "123456",
    "password": "Pass@1234",
    "nickName": "张三"
}
```

#### 用户登录
```json
POST /api/auth/login
{
    "phone": "13800138000",
    "password": "Pass@1234",
    "latitude": 39.9042,
    "longitude": 116.4074
}
```

#### 发送验证码
```json
POST /api/auth/verification-code
{
    "phone": "13800138000",
    "purpose": "REGISTER"
}
```

## 密码强度规则

- 最小长度8位
- 必须包含至少1个大写字母
- 必须包含至少1个小写字母
- 必须包含至少1个数字
- 必须包含至少1个特殊字符(!@#$%^&*等)

## 账号安全机制

1. **账号锁定**: 连续5次登录失败后锁定1小时
2. **异地登录检测**: 登录位置与上次相差500公里以上触发提醒
3. **设备指纹**: 记录设备信息，新设备登录触发提醒
4. **双因素认证**: 可选开启，支持TOTP

## 项目结构

```
bx-user/
├── src/main/java/com/beijixing/bxuser/
│   ├── BxUserApplication.java      # 主类
│   ├── config/                      # 配置类
│   ├── controller/                  # 控制器
│   ├── service/                     # 业务层
│   ├── repository/                  # 数据访问层
│   ├── entity/                      # 实体类
│   ├── dto/                         # 数据传输对象
│   ├── security/                    # 安全相关
│   ├── util/                        # 工具类
│   └── exception/                   # 异常处理
├── src/main/resources/
│   ├── application.yml              # 应用配置
│   └── db/init.sql                  # 数据库脚本
└── src/test/                        # 单元测试
```

## 开发团队

- **技术负责人**: 苏波 (EMP-BE-001)
- **所属模块**: MOD-001 用户认证模块
- **所属项目**: 北极星AI商机获客系统

## 更新日志

### v1.0.0 (2024-04-08)
- 初始版本发布
- 完成所有核心功能开发
