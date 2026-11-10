import { createRouter, createWebHashHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/store/user'

const Layout = () => import('@/layout/index.vue')

export const menuRoutes = [
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/index.vue'),
    meta: { title: '工作台', icon: 'Odometer', permission: 'dashboard:view' }
  },
  {
    path: '/customers',
    name: 'Customer',
    component: () => import('@/views/customer/index.vue'),
    meta: { title: '客户管理', icon: 'OfficeBuilding', permission: 'crm:customer:list' }
  },
  {
    path: '/contacts',
    name: 'Contact',
    component: () => import('@/views/contact/index.vue'),
    meta: { title: '联系人', icon: 'User', permission: 'crm:contact:list' }
  },
  {
    path: '/leads',
    name: 'Lead',
    component: () => import('@/views/lead/index.vue'),
    meta: { title: '线索管理', icon: 'Aim', permission: 'crm:lead:list' }
  },
  {
    path: '/opportunities',
    name: 'Opportunity',
    component: () => import('@/views/opportunity/index.vue'),
    meta: { title: '商机管理', icon: 'TrendCharts', permission: 'crm:opportunity:list' }
  },
  {
    path: '/trade',
    name: 'Trade',
    meta: { title: '交易管理', icon: 'ShoppingCart' },
    children: [
      {
        path: '/trade/overview',
        name: 'TradeOverview',
        component: () => import('@/views/trade/index.vue'),
        meta: { title: '交易总览', icon: 'DataAnalysis', permission: 'crm:trade:view' }
      },
      {
        path: '/contracts',
        name: 'Contract',
        component: () => import('@/views/contract/index.vue'),
        meta: { title: '合同管理', icon: 'Document', permission: 'crm:contract:list' }
      },
      {
        path: '/orders',
        name: 'Order',
        component: () => import('@/views/order/index.vue'),
        meta: { title: '订单管理', icon: 'List', permission: 'crm:order:list' }
      },
      {
        path: '/delivery-plans',
        name: 'DeliveryPlan',
        component: () => import('@/views/plan/index.vue'),
        meta: { title: '发货计划', icon: 'Calendar', permission: 'crm:plan:list' }
      },
      {
        path: '/deliveries',
        name: 'Delivery',
        component: () => import('@/views/delivery/index.vue'),
        meta: { title: '出库管理', icon: 'Van', permission: 'crm:delivery:list' }
      },
      {
        path: '/invoices',
        name: 'Invoice',
        component: () => import('@/views/invoice/index.vue'),
        meta: { title: '开票管理', icon: 'Tickets', permission: 'crm:invoice:list' }
      },
      {
        path: '/receivables',
        name: 'Receivable',
        component: () => import('@/views/receivable/index.vue'),
        meta: { title: '应收管理', icon: 'Money', permission: 'crm:receivable:list' }
      },
      {
        path: '/payments',
        name: 'Payment',
        component: () => import('@/views/payment/index.vue'),
        meta: { title: '收款核销', icon: 'Wallet', permission: 'crm:payment:list' }
      },
      {
        path: '/returns',
        name: 'ReturnOrder',
        component: () => import('@/views/returns/index.vue'),
        meta: { title: '退货退款', icon: 'RefreshLeft', permission: 'crm:return:list' }
      }
    ]
  },
  {
    path: '/products',
    name: 'Product',
    component: () => import('@/views/product/index.vue'),
    meta: { title: '产品管理', icon: 'Goods', permission: 'crm:product:list' }
  },
  {
    path: '/product-categories',
    name: 'ProductCategory',
    component: () => import('@/views/product/CategoryPage.vue'),
    meta: { title: '产品目录', icon: 'FolderOpened', permission: 'crm:product:list' }
  },
  {
    path: '/activities',
    name: 'Activity',
    component: () => import('@/views/activity/index.vue'),
    meta: { title: '跟进记录', icon: 'ChatDotSquare', permission: 'crm:activity:list' }
  },
  {
    path: '/system',
    name: 'System',
    meta: { title: '系统管理', icon: 'Setting' },
    children: [
      {
        path: '/system/users',
        name: 'SysUser',
        component: () => import('@/views/system/user.vue'),
        meta: { title: '用户管理', icon: 'UserFilled', permission: 'system:user:list' }
      },
      {
        path: '/system/roles',
        name: 'SysRole',
        component: () => import('@/views/system/role.vue'),
        meta: { title: '角色权限', icon: 'Key', permission: 'system:role:list' }
      },
      {
        path: '/system/depts',
        name: 'SysDept',
        component: () => import('@/views/system/dept.vue'),
        meta: { title: '部门管理', icon: 'Grid', permission: 'system:dept:list' }
      },
      {
        path: '/system/dicts',
        name: 'SysDict',
        component: () => import('@/views/system/dict.vue'),
        meta: { title: '数据字典', icon: 'Notebook', permission: 'system:dict:list' }
      },
      {
        path: '/system/logs',
        name: 'SysLog',
        component: () => import('@/views/system/log.vue'),
        meta: { title: '操作日志', icon: 'Tickets', permission: 'system:log:list' }
      },
      {
        path: '/system/change-logs',
        name: 'ChangeLogCenter',
        component: () => import('@/views/system/changeLog.vue'),
        meta: { title: '修改记录', icon: 'Clock', permission: 'system:log:list' }
      },
      {
        path: '/system/data-export',
        name: 'DataExport',
        component: () => import('@/views/system/dataExport.vue'),
        meta: { title: '数据导出', icon: 'Download', permission: 'crm:customer:list' }
      }
    ]
  }
]

// 展开二级菜单，全部挂载到 Layout 下（子路由使用绝对路径）
const flatRoutes = menuRoutes.flatMap((route) => (route.children ? route.children : [route]))

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      ...flatRoutes,
      {
        path: '/403',
        name: 'Forbidden',
        component: () => import('@/views/error/403.vue'),
        meta: { title: '无权访问' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { public: true, title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 路由切换后兜底：清理可能残留的弹层遮罩，避免整页点击失效
router.afterEach(() => {
  document.body.classList.remove('el-popup-parent--hidden')
  document.querySelectorAll('.el-overlay').forEach((el) => {
    const visible = getComputedStyle(el).display !== 'none'
    const hasContent = el.querySelector('.el-dialog, .el-message-box, .el-drawer')
    if (visible && !hasContent) {
      el.style.display = 'none'
    }
  })
})

router.beforeEach(async (to, from, next) => {
  const token = getToken()
  document.title = to.meta.title ? `${to.meta.title} · 商序云` : '商序云 · 销售运营一体化平台'

  if (to.meta.public) {
    if (to.path === '/login' && token) {
      return next('/')
    }
    return next()
  }

  if (!token) {
    return next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
  }

  const userStore = useUserStore()
  if (!userStore.user) {
    try {
      await userStore.fetchInfo()
    } catch (e) {
      userStore.reset()
      return next('/login')
    }
  }

  if (to.meta.permission && !userStore.hasPerm(to.meta.permission)) {
    ElMessage.warning('您没有访问该页面的权限')
    return next('/403')
  }
  next()
})

export default router
