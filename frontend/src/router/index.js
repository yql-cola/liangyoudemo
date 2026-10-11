import Vue from 'vue'
import Router from 'vue-router'
import Layout from '@/layout'

Vue.use(Router)

export const constantRoutes = [
  {
    path: '/login',
    component: () => import('@/views/login'),
    hidden: true
  },
  {
    path: '/401',
    component: () => import('@/views/error/401'),
    hidden: true
  },
  {
    path: '/404',
    component: () => import('@/views/error/404'),
    hidden: true
  },
  {
    path: '/',
    component: Layout,
    redirect: '/index',
    children: [
      {
        path: 'index',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index'),
        meta: { title: '首页', icon: 'el-icon-s-home' }
      }
    ]
  },
  {
    path: '/system',
    component: Layout,
    redirect: '/system/user',
    meta: { title: '系统管理', icon: 'el-icon-setting' },
    children: [
      {
        path: 'user',
        name: 'SystemUser',
        component: () => import('@/views/system/user/index'),
        meta: { title: '用户管理', permission: 'system:user:list' }
      },
      {
        path: 'role',
        name: 'SystemRole',
        component: () => import('@/views/system/role/index'),
        meta: { title: '角色管理', permission: 'system:role:list' }
      },
      {
        path: 'menu',
        name: 'SystemMenu',
        component: () => import('@/views/system/menu/index'),
        meta: { title: '菜单权限', permission: 'system:menu:view' }
      }
    ]
  },
  {
    path: '/purchase',
    component: Layout,
    redirect: '/purchase/in',
    meta: { title: '采购管理', icon: 'el-icon-shopping-cart-2' },
    children: [
      {
        path: 'in',
        name: 'PurchaseIn',
        component: () => import('@/views/purchase/in/index'),
        meta: { title: '采购入库', permission: 'purchase:order:list' }
      },
      {
        path: 'in/detail/:id',
        name: 'PurchaseInDetail',
        component: () => import('@/views/purchase/in/detail'),
        hidden: true,
        meta: { title: '采购详情', permission: 'purchase:order:view' }
      }
    ]
  },
  {
    path: '/sale',
    component: Layout,
    redirect: '/sale/out',
    meta: { title: '销售管理', icon: 'el-icon-sell' },
    children: [
      {
        path: 'out',
        name: 'SaleOut',
        component: () => import('@/views/sale/out/index'),
        meta: { title: '销售出库', permission: 'sale:order:list' }
      },
      {
        path: 'out/detail/:id',
        name: 'SaleOutDetail',
        component: () => import('@/views/sale/out/detail'),
        hidden: true,
        meta: { title: '销售详情', permission: 'sale:order:view' }
      }
    ]
  },
  {
    path: '/inventory',
    component: Layout,
    redirect: '/inventory/stock',
    meta: { title: '库存管理', icon: 'el-icon-box' },
    children: [
      {
        path: 'stock',
        name: 'InventoryStock',
        component: () => import('@/views/inventory/stock/index'),
        meta: { title: '库存查询', permission: 'inventory:stock:list' }
      },
      {
        path: 'batch',
        name: 'InventoryBatch',
        component: () => import('@/views/inventory/batch/index'),
        meta: { title: '批次库存', permission: 'inventory:batch:list' }
      },
      {
        path: 'flow',
        name: 'InventoryFlow',
        component: () => import('@/views/inventory/flow/index'),
        meta: { title: '库存流水', permission: 'inventory:flow:list' }
      }
    ]
  },
  {
    path: '/statistics',
    component: Layout,
    redirect: '/statistics/daily',
    meta: { title: '统计报表', icon: 'el-icon-data-analysis' },
    children: [
      {
        path: 'daily',
        name: 'StatisticsDaily',
        component: () => import('@/views/statistics/daily'),
        meta: { title: '日进销统计', permission: 'statistics:daily:view' }
      },
      {
        path: 'monthly',
        name: 'StatisticsMonthly',
        component: () => import('@/views/statistics/monthly'),
        meta: { title: '月进销统计', permission: 'statistics:monthly:view' }
      },
      {
        path: 'customer',
        name: 'StatisticsCustomer',
        component: () => import('@/views/statistics/customer'),
        meta: { title: '客户销售统计', permission: 'statistics:customer:view' }
      },
      {
        path: 'supplier',
        name: 'StatisticsSupplier',
        component: () => import('@/views/statistics/supplier'),
        meta: { title: '供应商采购统计', permission: 'statistics:supplier:view' }
      },
      {
        path: 'category',
        name: 'StatisticsCategory',
        component: () => import('@/views/statistics/category'),
        meta: { title: '分类金额统计', permission: 'statistics:category:view' }
      }
    ]
  },
  {
    path: '/basic',
    component: Layout,
    redirect: '/basic/product',
    meta: { title: '基础资料', icon: 'el-icon-folder' },
    children: [
      { path: 'product', name: 'BasicProduct', component: () => import('@/views/basic/placeholder'), meta: { title: '商品管理' } },
      { path: 'warehouse', name: 'BasicWarehouse', component: () => import('@/views/basic/placeholder'), meta: { title: '仓库管理' } },
      { path: 'supplier', name: 'BasicSupplier', component: () => import('@/views/basic/placeholder'), meta: { title: '供应商管理' } },
      { path: 'customer', name: 'BasicCustomer', component: () => import('@/views/basic/placeholder'), meta: { title: '客户管理' } }
    ]
  },
  { path: '*', redirect: '/404', hidden: true }
]

const createRouter = () => new Router({
  mode: 'history',
  scrollBehavior: () => ({ y: 0 }),
  routes: constantRoutes
})

const router = createRouter()

export default router
