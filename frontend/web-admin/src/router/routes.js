/**
 * routes.js - 路由配置
 * 功能：定义所有页面路由及路由元信息
 * 
 * @author 王强 (EMP-FE-002)
 * @date 2024-01
 * @update 张前端 (EMP-FE-001) - 补充数据看板、系统管理路由
 */

// 页面组件懒加载：避免首屏加载过多代码
const Login = () => import('@/views/login/index.vue')
const Dashboard = () => import('@/views/dashboard/index.vue')
const TenantList = () => import('@/views/tenant/list.vue')
const TenantAudit = () => import('@/views/tenant/audit.vue')
const BillingOverview = () => import('@/views/billing/overview.vue')
const SystemSettings = () => import('@/views/system/settings.vue')
// 新增路由组件
const DataDashboard = () => import('@/views/data/dashboard.vue')
const SystemDict = () => import('@/views/system/dict.vue')
const SystemJob = () => import('@/views/system/job.vue')
const SystemConfig = () => import('@/views/system/config.vue')
const SystemMonitor = () => import('@/views/system/monitor.vue')
const SystemLog = () => import('@/views/system/log.vue')
const SystemNurturing = () => import('@/views/system/nurturing.vue')

/**
 * 路由配置数组
 * 
 * 路由元信息（meta）说明：
 *   - title: 页面标题（显示在浏览器标签和面包屑中）
 *   - requireAuth: 是否需要登录认证
 *   - roles: 允许访问的角色数组（空数组表示所有已登录角色可访问）
 */
const routes = [
  // ============================================================
  // 重定向
  // ============================================================
  {
    path: '/',
    redirect: '/dashboard'
  },

  // ============================================================
  // 登录页（无需认证）
  // ============================================================
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: {
      title: '管理员登录',
      requireAuth: false
    }
  },

  // ============================================================
  // 管理后台布局页（含侧边栏和顶部栏）
  // ============================================================
  {
    path: '/',
    component: () => import('@/components/layout/AdminLayout.vue'),
    redirect: '/dashboard',
    meta: { requireAuth: true },
    children: [
      // 首页/仪表盘
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: Dashboard,
        meta: {
          title: '控制台',
          icon: 'el-icon-monitor'
        }
      },

      // ----------------------------------------
      // 租户管理模块
      // ----------------------------------------
      {
        path: 'tenant/list',
        name: 'TenantList',
        component: TenantList,
        meta: {
          title: '租户列表',
          icon: 'el-icon-office-building',
          roles: ['super_admin', 'ops_admin']
        }
      },
      {
        path: 'tenant/audit',
        name: 'TenantAudit',
        component: TenantAudit,
        meta: {
          title: '租户审核',
          icon: 'el-icon-document-checked',
          roles: ['super_admin']
        }
      },

      // ----------------------------------------
      // 计费管理模块
      // ----------------------------------------
      {
        path: 'billing/overview',
        name: 'BillingOverview',
        component: BillingOverview,
        meta: {
          title: '计费概览',
          icon: 'el-icon-coin',
          roles: ['super_admin', 'ops_admin']
        }
      },

      // ----------------------------------------
      // 系统设置模块
      // ----------------------------------------
      {
        path: 'system/settings',
        name: 'SystemSettings',
        component: SystemSettings,
        meta: {
          title: '系统设置',
          icon: 'Setting',
          roles: ['super_admin']
        }
      },

      // ----------------------------------------
      // 数据看板模块（EMP-FE-001 新增）
      // ----------------------------------------
      {
        path: 'data/dashboard',
        name: 'DataDashboard',
        component: DataDashboard,
        meta: {
          title: '数据看板',
          icon: 'el-icon-data-board',
          roles: ['super_admin', 'ops_admin']
        }
      },

      // ----------------------------------------
      // 系统管理模块（EMP-FE-001 新增）
      // ----------------------------------------
      {
        path: 'system/dict',
        name: 'SystemDict',
        component: SystemDict,
        meta: {
          title: '字典管理',
          icon: 'Notebook',
          roles: ['super_admin']
        }
      },
      {
        path: 'system/job',
        name: 'SystemJob',
        component: SystemJob,
        meta: {
          title: '定时任务',
          icon: 'Timer',
          roles: ['super_admin']
        }
      },
      {
        path: 'system/config',
        name: 'SystemConfig',
        component: SystemConfig,
        meta: {
          title: '系统配置',
          icon: 'el-icon-coin',
          roles: ['super_admin']
        }
      },
      {
        path: 'system/monitor',
        name: 'SystemMonitor',
        component: SystemMonitor,
        meta: {
          title: '系统监控',
          icon: 'el-icon-monitor',
          roles: ['super_admin']
        }
      },
      {
        path: 'system/log',
        name: 'SystemLog',
        component: SystemLog,
        meta: {
          title: '操作日志',
          icon: 'el-icon-document',
          roles: ['super_admin', 'ops_admin']
        }
      },
      {
        path: 'system/nurturing',
        name: 'SystemNurturing',
        component: SystemNurturing,
        meta: {
          title: '养号策略管理',
          icon: 'FirstAidKit',
          roles: ['super_admin', 'ops_admin']
        }
      }
    ]
  },

  // ============================================================
  // 404 页面
  // ============================================================
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', requireAuth: false }
  }
]

export default routes
