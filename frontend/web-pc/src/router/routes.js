/**
 * @fileoverview 路由表配置文件
 * @description 定义所有页面路由及其元信息
 * @author EMP-FE-001 张婷
 */

// ============================================================
// 路由名称常量
// ============================================================
export const RouteName = {
  LOGIN: 'login',
  DASHBOARD: 'dashboard',
  USER_LIST: 'user-list',
  USER_DETAIL: 'user-detail',
  CONTENT_LIST: 'content-list',
  CONTENT_CREATE: 'content-create',
  CONTENT_EDIT: 'content-edit',
  CONTENT_PUBLISH: 'content-publish',
  LEAD_LIST: 'lead-list',
  LEAD_DETAIL: 'lead-detail',
  LEAD_TASK: 'lead-task',
  CRAWL_MANAGEMENT: 'crawl-management',
  BILLING_BALANCE: 'billing-balance',
  BILLING_RECHARGE: 'billing-recharge',
  BILLING_ORDER: 'billing-order',
  BILLING_INVOICE: 'billing-invoice',
  ACCOUNT_LIST: 'account-list',
  ACCOUNT_BIND: 'account-bind',
  ACCOUNT_DEVICE: 'account-device',
  ACCOUNT_NURTURING: 'account-nurturing',
  SETTINGS_PROFILE: 'settings-profile',
  SETTINGS_SECURITY: 'settings-security',
  NOT_FOUND: 'not-found',
  FORBIDDEN: 'forbidden',
}

// ============================================================
// 页面组件懒加载
// ============================================================
const Login = () => import('@/views/login/index.vue')
const Error404 = () => import('@/views/error/404.vue')
const Error403 = () => import('@/views/error/403.vue')
const Dashboard = () => import('@/views/dashboard/index.vue')
const UserList = () => import('@/views/user/list.vue')
const UserDetail = () => import('@/views/user/detail.vue')
const ContentList = () => import('@/views/content/list.vue')
const ContentCreate = () => import('@/views/content/create.vue')
const ContentEdit = () => import('@/views/content/edit.vue')
const ContentPublish = () => import('@/views/content/publish.vue')
const LeadList = () => import('@/views/lead/list.vue')
const LeadDetail = () => import('@/views/lead/detail.vue')
const LeadTask = () => import('@/views/lead/task.vue')
const CrawlManagement = () => import('@/views/crawl/index.vue')
const AccountList = () => import('@/views/account/list.vue')
const AccountBind = () => import('@/views/account/bind.vue')
const AccountDevice = () => import('@/views/account/device.vue')
const AccountNurturing = () => import('@/views/account/nurturing.vue')
const BillingBalance = () => import('@/views/billing/balance.vue')
const BillingRecharge = () => import('@/views/billing/recharge.vue')
const BillingOrder = () => import('@/views/billing/order.vue')
const BillingInvoice = () => import('@/views/billing/invoice.vue')
const TenantInfo = () => import('@/views/tenant/info.vue')
const TenantConfig = () => import('@/views/tenant/config.vue')
const SettingsProfile = () => import('@/views/settings/profile.vue')
const SettingsSecurity = () => import('@/views/settings/security.vue')

// ============================================================
// 静态路由（无需权限）
// ============================================================
const staticRoutes = [
  {
    // 登录页
    path: '/login',
    name: RouteName.LOGIN,
    component: Login,
    meta: {
      title: '登录',
      requiresAuth: false,
      layout: 'blank',
    },
  },

  {
    // 404 页面
    path: '/404',
    name: RouteName.NOT_FOUND,
    component: Error404,
    meta: {
      title: '页面不存在',
      requiresAuth: false,
      layout: 'blank',
    },
  },

  {
    // 403 禁止访问
    path: '/403',
    name: RouteName.FORBIDDEN,
    component: Error403,
    meta: {
      title: '无权访问',
      requiresAuth: false,
      layout: 'blank',
    },
  },
]

// ============================================================
// 主布局路由（需要登录）
// ============================================================
const mainRoutes = [
  {
    // 首页 / 仪表盘
    path: '/',
    redirect: '/dashboard',
  },

  {
    // 仪表盘
    path: '/dashboard',
    name: RouteName.DASHBOARD,
    component: Dashboard,
    meta: {
      title: '工作台',
      icon: 'dashboard',
      requiresAuth: true,
      layout: 'default',
    },
  },

  // ============================================================
  // 用户管理
  // ============================================================
  {
    path: '/user',
    redirect: '/user/list',
  },
  {
    path: '/user/list',
    name: RouteName.USER_LIST,
    component: UserList,
    meta: {
      title: '用户列表',
      icon: 'user',
      requiresAuth: true,
      roles: [1, 2, 3], // 超级管理员、运维管理员、租户管理员可见
      layout: 'default',
    },
  },
  {
    path: '/user/detail/:id',
    name: RouteName.USER_DETAIL,
    component: UserDetail,
    meta: {
      title: '用户详情',
      icon: 'user',
      requiresAuth: true,
      roles: [1, 2, 3],
      layout: 'default',
      hidden: true, // 不显示在菜单中
      activeMenu: '/user/list', // 激活菜单
    },
  },

  // ============================================================
  // 内容管理
  // ============================================================
  {
    path: '/content',
    redirect: '/content/list',
  },
  {
    path: '/content/list',
    name: RouteName.CONTENT_LIST,
    component: ContentList,
    meta: {
      title: '内容列表',
      icon: 'content',
      requiresAuth: true,
      layout: 'default',
    },
  },
  {
    path: '/content/create',
    name: RouteName.CONTENT_CREATE,
    component: ContentCreate,
    meta: {
      title: '创建内容',
      icon: 'content',
      requiresAuth: true,
      layout: 'default',
      hidden: true,
      activeMenu: '/content/list',
      breadcrumbTitle: '创建内容',
    },
  },
  {
    path: '/content/edit/:id',
    name: RouteName.CONTENT_EDIT,
    component: ContentEdit,
    meta: {
      title: '编辑内容',
      icon: 'content',
      requiresAuth: true,
      layout: 'default',
      hidden: true,
      activeMenu: '/content/list',
      breadcrumbTitle: '编辑内容',
    },
  },
  {
    path: '/content/publish',
    name: RouteName.CONTENT_PUBLISH,
    component: ContentPublish,
    meta: {
      title: '内容发布',
      icon: 'content',
      requiresAuth: true,
      layout: 'default',
      hidden: true,
      activeMenu: '/content/list',
      breadcrumbTitle: '内容发布',
    },
  },

  // ============================================================
  // 商机管理
  // ============================================================
  {
    path: '/lead',
    redirect: '/lead/list',
  },
  {
    path: '/lead/list',
    name: RouteName.LEAD_LIST,
    component: LeadList,
    meta: {
      title: '商机列表',
      icon: 'lead',
      requiresAuth: true,
      layout: 'default',
    },
  },
  {
    path: '/lead/detail/:id',
    name: RouteName.LEAD_DETAIL,
    component: LeadDetail,
    meta: {
      title: '商机详情',
      icon: 'lead',
      requiresAuth: true,
      layout: 'default',
      hidden: true,
      activeMenu: '/lead/list',
      breadcrumbTitle: '商机详情',
    },
  },
  {
    path: '/lead/task',
    name: RouteName.LEAD_TASK,
    component: LeadTask,
    meta: {
      title: '获客任务',
      icon: 'task',
      requiresAuth: true,
      layout: 'default',
    },
  },

  // ============================================================
  // 评论抓取与商机挖掘
  // ============================================================
  {
    path: '/crawl',
    name: RouteName.CRAWL_MANAGEMENT,
    component: CrawlManagement,
    meta: {
      title: '评论抓取与商机挖掘',
      icon: 'crawl',
      requiresAuth: true,
      layout: 'default',
    },
  },

  // ============================================================
  // 社媒账号管理
  // ============================================================
  {
    path: '/account',
    redirect: '/account/list',
  },
  {
    path: '/account/list',
    name: RouteName.ACCOUNT_LIST,
    component: AccountList,
    meta: {
      title: '账号管理',
      icon: 'account',
      requiresAuth: true,
      layout: 'default',
    },
  },
  {
    path: '/account/bind',
    name: RouteName.ACCOUNT_BIND,
    component: AccountBind,
    meta: {
      title: '绑定账号',
      icon: 'account',
      requiresAuth: true,
      layout: 'default',
      hidden: true,
      activeMenu: '/account/list',
      breadcrumbTitle: '绑定账号',
    },
  },
  {
    path: '/account/device',
    name: RouteName.ACCOUNT_DEVICE,
    component: AccountDevice,
    meta: {
      title: '设备管理',
      icon: 'device',
      requiresAuth: true,
      layout: 'default',
    },
  },
  {
    path: '/account/nurturing',
    name: RouteName.ACCOUNT_NURTURING,
    component: AccountNurturing,
    meta: {
      title: '养号策略',
      icon: 'heart',
      requiresAuth: true,
      layout: 'default',
    },
  },

  // ============================================================
  // 计费管理
  // ============================================================
  {
    path: '/billing',
    redirect: '/billing/balance',
  },
  {
    path: '/billing/balance',
    name: RouteName.BILLING_BALANCE,
    component: BillingBalance,
    meta: {
      title: '账户余额',
      icon: 'billing',
      requiresAuth: true,
      layout: 'default',
    },
  },
  {
    path: '/billing/recharge',
    name: RouteName.BILLING_RECHARGE,
    component: BillingRecharge,
    meta: {
      title: '充值',
      icon: 'billing',
      requiresAuth: true,
      layout: 'default',
      hidden: true,
      activeMenu: '/billing/balance',
      breadcrumbTitle: '充值',
    },
  },
  {
    path: '/billing/order',
    name: RouteName.BILLING_ORDER,
    component: BillingOrder,
    meta: {
      title: '订单记录',
      icon: 'billing',
      requiresAuth: true,
      layout: 'default',
    },
  },
  {
    path: '/billing/invoice',
    name: RouteName.BILLING_INVOICE,
    component: BillingInvoice,
    meta: {
      title: '发票管理',
      icon: 'billing',
      requiresAuth: true,
      layout: 'default',
    },
  },

  // ============================================================
  // 租户管理
  // ============================================================
  {
    path: '/tenant',
    redirect: '/tenant/info',
  },
  {
    path: '/tenant/info',
    name: 'tenant-info',
    component: TenantInfo,
    meta: {
      title: '租户信息',
      icon: 'tenant',
      requiresAuth: true,
      layout: 'default',
    },
  },
  {
    path: '/tenant/config',
    name: 'tenant-config',
    component: TenantConfig,
    meta: {
      title: '租户配置',
      icon: 'setting',
      requiresAuth: true,
      layout: 'default',
    },
  },

  // ============================================================
  // 系统设置
  // ============================================================
  {
    path: '/settings',
    redirect: '/settings/profile',
  },
  {
    path: '/settings/profile',
    name: RouteName.SETTINGS_PROFILE,
    component: SettingsProfile,
    meta: {
      title: '个人信息',
      icon: 'settings',
      requiresAuth: true,
      layout: 'default',
    },
  },
  {
    path: '/settings/security',
    name: RouteName.SETTINGS_SECURITY,
    component: SettingsSecurity,
    meta: {
      title: '安全设置',
      icon: 'settings',
      requiresAuth: true,
      layout: 'default',
    },
  },
]

// ============================================================
// 404 路由（必须放在最后）
// ============================================================
const catchAllRoute = {
  // 所有未匹配的路由
  path: '/:pathMatch(.*)*',
  redirect: '/404',
}

// ============================================================
// 获取完整路由表
// ============================================================

/**
 * 获取所有路由
 * @returns {array}
 */
export function getRoutes() {
  return [...staticRoutes, ...mainRoutes, catchAllRoute]
}

/**
 * 获取需要注册到菜单的路由
 * （过滤掉 hidden 的路由）
 * @returns {array}
 */
export function getMenuRoutes() {
  return mainRoutes.filter((route) => !route.meta?.hidden)
}

// ============================================================
// 默认导出
// ============================================================
export default getRoutes
