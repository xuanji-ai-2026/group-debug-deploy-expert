# web-pc PC端Web

## 模块说明
北极星AI商机获客系统PC端Web应用，面向租户的操作后台。

## 目录结构
```
web-pc/
├── public/
├── src/
│   ├── main.js
│   ├── App.vue
│   ├── api/              # API接口
│   ├── assets/           # 静态资源
│   ├── components/       # 公共组件
│   ├── composables/      # 组合式函数
│   ├── layouts/          # 布局组件
│   ├── router/           # 路由配置
│   ├── store/            # 状态管理
│   ├── utils/            # 工具函数
│   └── views/            # 页面视图
│       ├── login/
│       ├── dashboard/
│       ├── user/
│       ├── tenant/
│       ├── content/
│       ├── lead/
│       ├── account/
│       ├── billing/
│       └── settings/
├── .env
├── vite.config.js
└── package.json
```

## 技术栈
- Vue 3
- TypeScript
- Vite
- Pinia
- Vue Router
- Axios
- Element Plus

## 启动命令
```bash
npm install
npm run dev
```
