# 北极星AI管理后台 (Beijixing AI Admin)

北极星AI商机获客系统的管理端 Web 应用，为超级管理员和运维管理员提供系统配置、租户管理、计费管理等核心功能。

## 技术栈

- **Vue 3** - 渐进式前端框架
- **Vite** - 下一代前端构建工具
- **Pinia** - Vue 状态管理
- **Vue Router 4** - Vue 官方路由管理
- **Axios** - HTTP 请求库
- **Element Plus** - Vue 3 UI 组件库
- **SCSS** - CSS 预处理器

## 项目结构

```
web-admin/
├── public/                  # 静态公共资源
│   ├── index.html           # HTML 入口
│   └── favicon.ico          # 网站图标
├── src/
│   ├── main.js              # 应用入口
│   ├── App.vue              # 根组件
│   ├── api/                 # API 接口层
│   │   ├── index.js         # API 统一导出
│   │   ├── tenant.js        # 租户管理 API
│   │   ├── billing.js        # 计费管理 API
│   │   └── system.js         # 系统管理 API
│   ├── assets/
│   │   ├── images/          # 图片资源
│   │   └── styles/          # 样式文件
│   │       ├── variables.scss # 全局 SCSS 变量
│   │       └── global.scss   # 全局样式
│   ├── components/          # 公共组件
│   │   ├── common/           # 通用组件
│   │   │   ├── AdminTable.vue  # 通用表格
│   │   │   └── AdminSearch.vue # 通用搜索栏
│   │   └── layout/           # 布局组件
│   │       ├── AdminLayout.vue  # 主布局
│   │       ├── AdminHeader.vue  # 顶部导航
│   │       ├── AdminSidebar.vue # 侧边栏
│   │       └── AdminContent.vue# 内容区
│   ├── router/              # 路由配置
│   │   ├── index.js          # 路由实例 + 守卫
│   │   └── routes.js         # 路由定义
│   ├── store/               # Pinia 状态管理
│   │   └── modules/
│   │       └── admin.js     # 管理员状态模块
│   ├── utils/               # 工具函数
│   │   ├── request.js       # Axios 封装
│   │   └── adminAuth.js      # 认证工具
│   └── views/               # 页面视图
│       ├── login/            # 登录页
│       ├── dashboard/        # 控制台
│       ├── tenant/           # 租户管理
│       │   ├── list.vue      # 租户列表
│       │   └── audit.vue     # 租户审核
│       ├── billing/          # 计费管理
│       │   └── overview.vue  # 计费概览
│       ├── system/           # 系统设置
│       │   └── settings.vue  # 系统配置
│       └── error/            # 错误页面
│           └── 404.vue       # 404 页面
├── .env                     # 环境变量
├── vite.config.js           # Vite 配置
└── package.json             # 项目依赖
```

## 功能模块

| 模块 | 功能说明 |
|------|----------|
| 登录 | 管理员账号密码登录，Token 认证 |
| 控制台 | 核心数据统计、快捷入口、待办事项 |
| 租户列表 | 租户信息管理、状态切换、导出 |
| 租户审核 | 入驻申请审核，通过/拒绝操作 |
| 计费概览 | 收支统计、趋势图表、消费排行 |
| 系统设置 | 基础配置、社媒平台、计费规则、风控规则、支付配置、操作日志 |

## 开发

### 安装依赖

```bash
npm install
```

### 开发模式启动

```bash
npm run dev
```

启动后访问 `http://localhost:3000`

### 生产构建

```bash
npm run build
```

### 代码检查

```bash
npm run lint
```

## 环境变量 (.env)

```env
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_TITLE=北极星AI管理后台
VITE_USE_MOCK=false
```

## API 代理

开发环境下，Vite 会将 `/api` 开头的请求代理到 `http://localhost:8080`，解决跨域问题。

## 路由权限

- `super_admin` - 超级管理员，拥有所有权限
- `ops_admin` - 运维管理员，查看监控和部分管理功能

## 维护

- 开发者：王强 (EMP-FE-002)
- 创建时间：2024-01
