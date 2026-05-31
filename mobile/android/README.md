# 北极星AI商机获客系统 - Android客户端

## 项目概述

北极星AI商机获客系统的Android原生客户端，基于`产品需求文档 v2.0`开发实现。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Kotlin | 1.9.20 | 主开发语言 |
| Jetpack Compose | BOM 2023.10.01 | UI框架（部分XML布局） |
| MVVM | - | 架构模式 |
| Retrofit | 2.9.0 | HTTP客户端 |
| Hilt | 2.48.1 | 依赖注入 |
| Coroutines | 1.7.3 | 异步编程 |
| DataStore | 1.0.0 | 本地存储 |
| Material Design 3 | - | UI设计规范 |

## 项目结构

```
android/
├── app/
│   ├── src/main/
│   │   ├── java/com/beijixing/app/
│   │   │   ├── BxApplication.kt          # Application入口
│   │   │   ├── di/                       # Hilt依赖注入模块
│   │   │   ├── data/
│   │   │   │   ├── model/                # 数据模型（User/Lead/Task）
│   │   │   │   ├── repository/           # 数据仓库层
│   │   │   │   └── remote/               # API客户端
│   │   │   ├── service/                  # 后台服务
│   │   │   ├── ui/                       # UI层（Activity+ViewModel）
│   │   │   │   ├── main/                 # 首页
│   │   │   │   ├── login/                # 登录
│   │   │   │   ├── lead/                 # 商机
│   │   │   │   ├── task/                 # 任务
│   │   │   │   └── components/           # 通用组件
│   │   │   └── util/                     # 工具类
│   │   └── res/                          # 资源文件
│   └── build.gradle.kts
├── build.gradle.kts                       # 根级构建配置
├── settings.gradle.kts                    # 项目设置
├── gradle.properties                     # Gradle属性
└── gradle/wrapper/                      # Gradle Wrapper
```

## 功能模块

### 1. 登录模块
- 手机号+验证码登录
- 手机号+密码登录（Tab切换）
- 微信一键登录（预留）
- Token自动管理
- 设备指纹上报

### 2. 首页模块
- 积分余额展示
- 语音输入入口
- 快捷功能入口（截客/获客）
- 商机预警卡片
- 任务进度卡片
- 最新商机列表

### 3. 商机模块
- 商机列表（分页+筛选+搜索）
- 商机详情
- 添加跟进记录
- 商机状态管理

### 4. 任务模块
- 任务列表（分页）
- 任务进度展示
- 暂停/恢复/取消操作

### 5. 后台服务
- KeepAliveService：后台保活
- NotificationService：通知管理

## 编译构建

### 环境要求
- JDK 17+
- Android Studio Hedgehog (2023.1.1) 或更高
- Android SDK API 34
- Gradle 8.4

### 构建命令

```bash
# 开发调试构建
./gradlew assembleDebug

# 发布构建
./gradlew assembleRelease

# 清理构建
./gradlew clean

# 依赖检查
./gradlew dependencies
```

### 签名配置
Release构建需要配置签名信息，在`~/.gradle/gradle.properties`中添加：

```properties
# 签名配置
RELEASE_STORE_FILE=/path/to/keystore.jks
RELEASE_STORE_PASSWORD=your_password
RELEASE_KEY_ALIAS=your_alias
RELEASE_KEY_PASSWORD=your_key_password
```

## API对接

### 基础地址
- 生产环境：`https://api.beijixing.com/api/v1/`
- 开发环境：`https://api-dev.beijixing.com/api/v1/`

### 认证方式
Bearer Token（JWT），自动注入到所有HTTP请求Header中。

### 主要接口
- `POST /auth/login` - 登录
- `POST /auth/code/send` - 发送验证码
- `GET /lead/list` - 商机列表
- `GET /lead/detail/{id}` - 商机详情
- `GET /task/list` - 任务列表
- `GET /account/info` - 账户信息

## 产品需求映射

| 产品需求章节 | Android实现 |
|------------|------------|
| 5.1节 权限申请清单 | AndroidManifest.xml + PermissionUtil.kt |
| 5.4节 APP核心页面 | LoginActivity / MainActivity / LeadListActivity / LeadDetailActivity / TaskActivity |
| 6.5节 自动发布与运营 | KeepAliveService / TaskRepository |
| 7.3节 商机管理 | LeadRepository / LeadViewModel |
| 4.3节 账号安全 | TokenManager / PreferencesManager |

## 注意事项

1. **Android 8.0+兼容**：minSdk设置为26，满足产品需求
2. **多租户支持**：所有API请求自动携带租户隔离参数
3. **设备指纹**：登录时上报设备ID，用于4.3.2节设备管理
4. **后台保活**：KeepAliveService确保任务持续执行
5. **通知渠道**：Android 8.0+必须创建通知渠道

## License

Proprietary - 北极星AI
