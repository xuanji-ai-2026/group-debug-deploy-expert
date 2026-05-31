# BxApp - 北极星AI商机获客系统 iOS客户端

iOS原生客户端项目，基于SwiftUI + MVVM架构开发。

## 技术栈

- **Swift 5.9** - 编程语言
- **SwiftUI** - 用户界面框架
- **MVVM** - 架构模式
- **Combine** - 响应式编程
- **Alamofire** - 网络请求库

## 项目结构

```
BxApp/
├── App/                    # App入口和生命周期
├── Models/                 # 数据模型
├── ViewModels/             # 视图模型
├── Views/                  # SwiftUI视图
│   ├── Login/             # 登录模块
│   ├── Main/              # 主页面
│   ├── Lead/              # 商机模块
│   ├── Task/              # 任务模块
│   └── Components/         # 通用组件
├── Services/               # 服务层
├── Repositories/           # 数据仓库
├── Utils/                  # 工具类
├── Extensions/             # Swift扩展
└── Resources/              # 资源文件
```

## 功能模块

1. **登录模块** - 支持手机号验证码登录、账号密码登录
2. **商机列表** - 展示商机列表，支持搜索和筛选
3. **商机详情** - 查看商机详细信息，跟进记录
4. **任务列表** - 展示任务列表，支持状态筛选
5. **任务进度** - 查看任务执行进度和结果

## 环境要求

- **Xcode**: 15.0+
- **iOS**: 12.0+
- **CocoaPods**: 1.12.0+

## 开发指南

### 1. 生成Xcode项目

```bash
# 安装XcodeGen（如果未安装）
brew install xcodegen

# 生成项目
cd ios
xcodegen generate
```

### 2. 安装依赖

```bash
# 安装CocoaPods依赖
pod install
```

### 3. 打开项目

```bash
open BxApp.xcworkspace
```

### 4. 运行项目

在Xcode中选择模拟器，点击Run按钮（或按Cmd+R）

## API配置

项目使用环境变量配置API地址，默认为：

- 开发环境: `http://localhost:8080/api/v1`
- 生产环境: `https://api.beijixing-ai.com/api/v1`

## 许可证

Proprietary - All Rights Reserved
